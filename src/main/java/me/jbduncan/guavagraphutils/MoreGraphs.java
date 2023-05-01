package me.jbduncan.guavagraphutils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Multisets.toMultiset;
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.graph.AbstractValueGraph;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.ValueGraph;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

// This class purposefully expands upon an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
public final class MoreGraphs {

  private static final String NODE_IS_NOT_IN_THIS_GRAPH = "Node '%s' is not in this graph";
  private static final String GRAPH_HAS_AT_LEAST_ONE_CYCLE = "graph has at least one cycle";
  private static final String SUCCESSORS_FUNCTION_HAS_AT_LEAST_ONE_CYCLE =
      "successors function has at least one cycle";

  /**
   * Returns an immutable directed graph from a given set of starting nodes and a {@linkplain
   * SuccessorsFunction successors function}. The successors function is applied to the starting
   * nodes, then their children, then their grand-children, and so on and so forth in a
   * breadth-first manner until all descendants have been traversed.
   *
   * <p>For example, given the starting node {@code "a"} and the following successors function:
   *
   * <pre>{@code
   * node -> {
   *   if (node.equals("a") {
   *     return ImmutableList.of("b");
   *   }
   *   if (node.equals("b") {
   *     return ImmutableList.of("c");
   *   }
   *   return ImmutableList.of();
   * }
   * }</pre>
   *
   * <p>...then this method will return the graph {@code {("a", "b"), ("b", "c")}}.
   *
   * <p>This method is safe to use with successor functions with cycles. For example, given node "z"
   * and the following successors function:
   *
   * <pre>{@code
   * node -> {
   *   if (nodes.equals("z") {
   *       return ImmutableList.of("z");
   *   }
   *   return ImmutableList.of();
   * }
   * }</pre>
   *
   * <p>...then this method will terminate quickly and return the graph {@code {("z", "z")}}.
   *
   * @param startingNodes the set of nodes to start from
   * @param successorsFunction the function to apply to the starting nodes and their descendants in
   *     a breadth-first manner; can represent any kind of graph, including cyclic graphs
   * @param <N> the type of the nodes
   * @return an immutable directed graph representing the breadth-first traversal of the successors
   *     function with the given starting nodes
   * @see <a href='https://stackoverflow.com/a/58457785/2252930'>this StackOverflow answer for the
   *     origin of this method</a>
   */
  // TODO: Can we make a version of common.graph.Traverser with an API like
  //   "Traverser.forGraph(successorsFunction).toGraphBreadthFirst(startingNodes)"
  //   that replaces this static method?
  public static <N> ImmutableGraph<N> buildGraphWithBreadthFirstTraversal(
      Iterable<N> startingNodes, SuccessorsFunction<N> successorsFunction) {
    checkNotNull(startingNodes, "startingNodes");
    checkNotNull(successorsFunction, "successorsFunction");

    MutableGraph<N> result = GraphBuilder.directed().allowsSelfLoops(true).build();
    startingNodes.forEach(result::addNode);
    Queue<N> nodesRemaining = Queues.newArrayDeque(startingNodes);
    while (!nodesRemaining.isEmpty()) {
      N next = nodesRemaining.remove();
      for (N successor : successorsFunction.successors(next)) {
        if (!result.hasEdgeConnecting(next, successor)) {
          nodesRemaining.add(successor);
          result.putEdge(next, successor);
        }
      }
    }
    return ImmutableGraph.copyOf(result);
  }

  /**
   * Returns an <i>unmodifiable, directed {@linkplain ValueGraph value graph} view</i> of the given
   * table where:
   *
   * <ul>
   *   <li>The row keys and column keys of the table are viewed as the graph's nodes.
   *   <li>Each row key / column key / cell value mapping in the table is viewed in the graph as a
   *       directed edge from the row key to the column key.
   *   <li>Each cell value is viewed as the edge value of its row and column's respective edge in
   *       the graph.
   * </ul>
   *
   * <p>For example, this table...
   *
   * <pre>{@code
   * +---+---+---+---+
   * |   | A | B | C |
   * +---+---+---+---+
   * | 1 | e |   |   |
   * | 2 |   | j | f |
   * | 3 |   | z |   |
   * +---+---+---+---+
   * }</pre>
   *
   * <p>...is viewed as this graph.
   *
   * <pre>{@code
   *     [e]
   * 1 -------> A
   *
   *     [j]
   * 2 -------> B
   *   \
   *    \ [f]
   *     -----> C
   *
   *     [z]
   * 3 -------> B
   * }</pre>
   *
   * <p>The given table must be non-null, otherwise a {@code NullPointerException} will be thrown.
   *
   * <p>The methods of this value graph require all input parameters to be non-null (except for
   * those parameters annotated with {@code @Nullable} in the {@link ValueGraph ValueGraph}
   * interface), otherwise a {@code NullPointerException} will be thrown.
   *
   * <p>If you need this value graph's directed edges to point in the other direction, then pass
   * this value graph to {@link com.google.common.graph.Graphs#transpose(ValueGraph)
   * Graphs.transpose} and use the result of that method instead.
   *
   * <p>The row keys and column keys in the given table must have {@link #equals(Object) equals()}
   * and {@link #hashCode() hashCode()} implementations as described in "<a
   * href='https://github.com/google/guava/wiki/GraphsExplained#graph-elements-nodes-and-edges'>Graphs
   * Explained</a>", otherwise the behaviour of this graph is undefined.
   *
   * <p>This value graph's methods have the following behaviour:
   *
   * <ul>
   *   <li>{@link ValueGraph#isDirected() isDirected()}: {@code true}
   *   <li>{@link ValueGraph#allowsSelfLoops() allowsSelfLoops()}: {@code false}
   *   <li>{@link ValueGraph#nodeOrder() nodeOrder()}: {@link ElementOrder#unordered() unordered()}
   *   <li>{@link ValueGraph#incidentEdgeOrder() incidentEdgeOrder()}: {@link
   *       ElementOrder#unordered() unordered()}
   *   <li>{@link ValueGraph#nodes() nodes()}: returns an unmodifiable set view of the row keys and
   *       column keys in the given table; the order is undefined.
   *   <li>{@link ValueGraph#edges() edges()}: returns an unmodifiable set view of {@linkplain
   *       EndpointPair endpoint pairs}, where each pair corresponds with a cell in the given table,
   *       and the pair's {@linkplain EndpointPair#source() source} and {@linkplain
   *       EndpointPair#target() target} are the cell's row key and column key respectively. The
   *       order of the edges is undefined.
   *   <li>{@link ValueGraph#successors(Object) successors(rowKey)}: returns an unmodifiable set
   *       view of the column keys that share a cell value with the row key; the order is undefined.
   *   <li>{@link ValueGraph#successors(Object) successors(columnKey)}: returns an empty immutable
   *       set.
   *   <li>{@link ValueGraph#successors(Object) successors(otherValue)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#predecessors(Object) predecessors(rowKey)}: returns an empty immutable
   *       set.
   *   <li>{@link ValueGraph#predecessors(Object) predecessors(columnKey)}: returns an unmodifiable
   *       set view of the row keys that share a cell value with the column key; the order is
   *       undefined.
   *   <li>{@link ValueGraph#predecessors(Object) predecessors(otherValue)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#adjacentNodes(Object) adjacentNodes(rowKey)}: returns an unmodifiable
   *       set view of the column keys that share a cell value with the row key; the order is
   *       undefined.
   *   <li>{@link ValueGraph#adjacentNodes(Object) adjacentNodes(columnKey)}: returns an
   *       unmodifiable set view of the row keys that share a cell value with the column key; the
   *       order is undefined.
   *   <li>{@link ValueGraph#adjacentNodes(Object) adjacentNodes(otherValue)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#outDegree(Object) outDegree(rowKey)}: the number of column keys that
   *       share a cell value with the row key.
   *   <li>{@link ValueGraph#outDegree(Object) outDegree(columnKey)}: 0
   *   <li>{@link ValueGraph#outDegree(Object) outDegree(otherValue)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#inDegree(Object) inDegree(rowKey)}: 0
   *   <li>{@link ValueGraph#inDegree(Object) inDegree(columnKey)}: the number of row keys that
   *       share a cell value with the column key.
   *   <li>{@link ValueGraph#inDegree(Object) inDegree(otherValue)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#degree(Object) degree(rowKey)}: the number of column keys that share a
   *       cell value with the row key.
   *   <li>{@link ValueGraph#degree(Object) degree(columnKey)}: the number of row keys that share a
   *       cell value with the column key.
   *   <li>{@link ValueGraph#degree(Object) degree(otherValue)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#edgeValue(Object, Object) edgeValue(firstValue, secondValue)}: if the
   *       first value is not a row key or the second value is not a column key, then this method
   *       throws an {@code IllegalArgumentException}; otherwise if the row key and column key share
   *       a cell value in the table then it returns the cell value as an {@code Optional};
   *       otherwise it returns an empty {@code Optional}.
   *   <li>{@link ValueGraph#edgeValue(EndpointPair) edgeValue(someEndpointPair)}: equivalent to
   *       {@code edgeValue(someEndpointPair.source(), someEndpointPair.target())}. Note that this
   *       throws an {@code IllegalArgumentException} if the endpoint pair is {@linkplain
   *       EndpointPair#unordered(Object, Object) not ordered}.
   *   <li>{@link ValueGraph#edgeValueOrDefault(Object, Object, Object)
   *       edgeValueOrDefault(firstValue, secondValue, defaultValue)}: if the first value is not a
   *       row key or the second value is not a column key, then this method throws an {@code
   *       IllegalArgumentException}; otherwise if the row key and column key share a cell value in
   *       the table then it returns the cell value; otherwise it returns the default value.
   *   <li>{@link ValueGraph#edgeValueOrDefault(EndpointPair, Object)
   *       edgeValueOrDefault(someEndpointPair, default)}: equivalent to {@code
   *       edgeValueOrDefault(someEndpointPair.source(), someEndpointPair.target(), default)}. Note
   *       that this throws an {@code IllegalArgumentException} if the endpoint pair is {@linkplain
   *       EndpointPair#unordered(Object, Object) not ordered}.
   *   <li>{@link ValueGraph#hasEdgeConnecting(Object, Object) hasEdgeConnecting(firstValue,
   *       secondValue)}: returns {@code true} if the first value is a row key, the second value is
   *       a column key, and if the row key and column key share a cell value in the table;
   *       otherwise returns {@code false}.
   *   <li>{@link ValueGraph#hasEdgeConnecting(EndpointPair) hasEdgeConnecting(someEndpointPair)}:
   *       almost equivalent to {@code hasEdgeConnecting(someEndpointPair.source(),
   *       someEndpointPair.target())}, except it does <i>not</i> throw an {@code
   *       IllegalArgumentException} if the endpoint pair is {@linkplain
   *       EndpointPair#unordered(Object, Object) not ordered}.
   *   <li>{@link ValueGraph#asGraph() asGraph()}: returns a {@code Graph} view that contains the
   *       nodes and edge connections of this value graph, but not the edge values.
   *   <li>{@link Object#toString() toString()}: returns a string representation of this value
   *       graph; the value and format is undefined.
   * </ul>
   *
   * @param table the table to adapt into a {@code ValueGraph} view; must not be null
   * @param <N> the node type; must have {@link #equals(Object) equals()} and {@link #hashCode()
   *     hashCode()} implementations as described in "<a
   *     href='https://github.com/google/guava/wiki/GraphsExplained#graph-elements-nodes-and-edges'>
   *     Graphs Explained</a>".
   * @param <E> the edge type; does not have to have {@code equals()} or {@code hashCode()}
   *     implementations
   * @return a {@code ValueGraph} view of the given table; never null
   * @see <a href='https://github.com/google/guava/wiki/GraphsExplained'>Graphs Explained</a>
   */
  public static <N, E> ValueGraph<N, E> asValueGraph(Table<N, N, E> table) {
    checkNotNull(table, "table");

    return new AbstractValueGraph<>() {
      @Override
      public Set<N> nodes() {
        return Sets.union(table.rowKeySet(), table.columnKeySet());
      }

      @Override
      protected long edgeCount() {
        return table.size();
      }

      @Override
      public Set<N> successors(N node) {
        checkNotNull(node, "node");
        checkArgument(nodes().contains(node), NODE_IS_NOT_IN_THIS_GRAPH, node);
        return Collections.unmodifiableSet(table.row(node).keySet());
      }

      @Override
      public Set<N> predecessors(N node) {
        checkNotNull(node, "node");
        checkArgument(nodes().contains(node), NODE_IS_NOT_IN_THIS_GRAPH, node);
        return Collections.unmodifiableSet(table.column(node).keySet());
      }

      @Override
      public Set<N> adjacentNodes(N node) {
        checkNotNull(node, "node");
        checkArgument(nodes().contains(node), NODE_IS_NOT_IN_THIS_GRAPH, node);
        return Collections.unmodifiableSet(
            table.rowKeySet().contains(node)
                ? table.row(node).keySet()
                : table.column(node).keySet());
      }

      @Override
      public boolean isDirected() {
        return true;
      }

      @Override
      public boolean allowsSelfLoops() {
        return false;
      }

      @Override
      public ElementOrder<N> nodeOrder() {
        return ElementOrder.unordered();
      }

      @Override
      public @Nullable E edgeValueOrDefault(N nodeU, N nodeV, @Nullable E defaultValue) {
        checkNotNull(nodeU, "nodeU");
        checkNotNull(nodeV, "nodeV");
        checkArgument(nodes().contains(nodeU), "First node '%s' is not in this graph", nodeU);
        checkArgument(nodes().contains(nodeV), "Second node '%s' is not in this graph", nodeV);
        if (table.contains(nodeU, nodeV)) {
          return table.get(nodeU, nodeV);
        }
        return defaultValue;
      }

      @Override
      public @Nullable E edgeValueOrDefault(EndpointPair<N> endpoints, @Nullable E defaultValue) {
        checkNotNull(endpoints, "endpoints");
        checkArgument(endpoints.isOrdered(), "Endpoints are not ordered");
        checkArgument(
            nodes().contains(endpoints.source()),
            "Source endpoint '%s' is not in this graph",
            endpoints.source());
        checkArgument(
            nodes().contains(endpoints.target()),
            "Target endpoint '%s' is not in this graph",
            endpoints.target());
        if (table.contains(endpoints.source(), endpoints.target())) {
          return table.get(endpoints.source(), endpoints.target());
        }
        return defaultValue;
      }
    };
  }

  /**
   * Returns an iterable representing the topological ordering of the given graph; that is, a
   * traversal of the graph in which each node is visited only after all its predecessors and other
   * ancestors have been visited.
   *
   * <p>The given graph must be non-null, otherwise a {@code NullPointerException} will be thrown.
   *
   * <p>The returned iterable is an <i>unmodifiable, lazy view</i> of the graph's nodes, and each
   * and every iteration recalculates the topological ordering. So if the iterable is used multiple
   * times, turn it into a set immediately, such as with {@link
   * com.google.common.collect.ImmutableSet#copyOf(Iterable) ImmutableSet.copyOf}, and use that set
   * instead.
   *
   * <p>Furthermore, the given graph may have multiple valid topological orderings. This method does
   * not guarantee which topological ordering is returned on any iteration, or even that the same
   * ordering is returned on subsequent iterations.
   *
   * <p>For example, given this graph...
   *
   * <pre>{@code
   * b <--- a ---> d
   * |      |
   * v      v
   * e ---> c ---> f
   * }</pre>
   *
   * <p>...the topological ordering returned from an iteration could be any of:
   *
   * <ul>
   *   <li>{@code [a, b, d, e, c, f]}
   *   <li>{@code [a, b, e, c, d, f]}
   *   <li>{@code [a, b, e, d, c, f]}
   *   <li>{@code [a, b, e, c, f, d]}
   *   <li>{@code [a, d, b, e, c, f]}
   * </ul>
   *
   * <p>This method only works on directed acyclic graphs. If a cycle is discovered when traversing
   * the graph, an {@code IllegalArgumentException} is thrown.
   *
   * <p>Iterations over the returned iterable run in linear time, specifically {@code O(N + E)},
   * where {@code N} is the number of nodes in the graph and {@code E} is the number of edges.
   *
   * @param graph the graph to return a topological ordering for; must not be null
   * @param <N> the node type; must have {@link #equals(Object) equals()} and {@link #hashCode()
   *     hashCode()} implementations as described in "<a
   *     href='https://github.com/google/guava/wiki/GraphsExplained#graph-elements-nodes-and-edges'>
   *     Graphs Explained</a>".
   * @return an unmodifiable, lazy iterable view of a topological ordering of the graph
   * @throws IllegalArgumentException if the graph has a cycle
   * @see <a href='https://en.wikipedia.org/wiki/Topological_sorting'>Wikipedia, "Topological
   *     sorting"</a>
   * @see <a href='https://github.com/google/guava/wiki/GraphsExplained'>Graphs Explained</a>
   */
  public static <N> Iterable<N> lazyTopologicalOrdering(Graph<N> graph) {
    checkNotNull(graph, "graph");

    return () -> {
      /*
       * Kahn's algorithm. Derived from [1], in turn derived from [2].
       *
       * [1] https://web.archive.org/web/20230225053309/https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm
       * [2] https://dl.acm.org/doi/pdf/10.1145/368996.369025
       */

      Queue<N> roots =
          graph.nodes().stream()
              .filter(node -> graph.inDegree(node) == 0)
              .collect(toCollection(ArrayDeque::new));
      Multiset<N> nonRoots =
          graph.nodes().stream()
              .filter(node -> graph.inDegree(node) > 0)
              .collect(toMultiset(node -> node, graph::inDegree, HashMultiset::create));

      return new AbstractIterator<>() {
        @Override
        protected N computeNext() {
          if (!roots.isEmpty()) {
            N next = roots.remove();
            for (N successor : graph.successors(next)) {
              int newInDegree = nonRoots.count(successor) - 1;
              nonRoots.setCount(successor, newInDegree);
              if (newInDegree == 0) {
                roots.add(successor);
              }
            }
            return next;
          }
          checkArgument(nonRoots.isEmpty(), GRAPH_HAS_AT_LEAST_ONE_CYCLE);
          return endOfData();
        }
      };
    };
  }

  // TODO: Make an eager version of lazyTopologicalOrdering

  // TODO: Javadoc
  public static <N> ImmutableList<N> topologicalOrderingStartingFrom(
      Iterable<N> startingNodes, SuccessorsFunction<N> successorsFunction) {
    checkNotNull(startingNodes, "startingNodes");
    checkNotNull(successorsFunction, "successorsFunction");

    /*
     * Depth-first-search-based algorithm. Derived from [1], in turn derived from Introduction to Algorithms (2nd ed.)
     * by Cormen et al.
     *
     * [1] https://web.archive.org/web/20230225053309/https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
     */

    class DeepRecursiveTopo {
      private final Deque<Object> intermediateStack = new ArrayDeque<>();
      private final Deque<Object> frameStack = new ArrayDeque<>();

      private DeepRecursiveTopo() {
        Map<N, VisitState> nodeToVisitState = new HashMap<>();
        for (N startingNode : startingNodes) {
          yieldContinuation(() -> visit(startingNode, successorsFunction, nodeToVisitState));
        }
      }

      private void visit(
          N node, SuccessorsFunction<N> successorsFunction, Map<N, VisitState> nodeToVisitState) {
        var visitState = nodeToVisitState.get(node);
        if (visitState == VisitState.FULLY_VISITED) {
          return;
        }
        if (visitState == VisitState.PARTIALLY_VISITED) {
          throw new IllegalArgumentException(SUCCESSORS_FUNCTION_HAS_AT_LEAST_ONE_CYCLE);
        }

        nodeToVisitState.put(node, VisitState.PARTIALLY_VISITED);

        for (N successor : successorsFunction.successors(node)) {
          yieldContinuation(() -> visit(successor, successorsFunction, nodeToVisitState));
        }

        yieldContinuation(() -> nodeToVisitState.put(node, VisitState.FULLY_VISITED));
        yieldValue(node);
      }

      void yieldContinuation(Continuation continuation) {
        intermediateStack.push(continuation);
      }

      void yieldValue(N value) {
        intermediateStack.push(value);
      }

      ImmutableList<N> recurse() {
        return Stream.generate(this::next)
            .takeWhile(Objects::nonNull)
            .collect(toImmutableList())
            .reverse();
      }

      private N next() {
        while (true) {
          while (!intermediateStack.isEmpty()) {
            frameStack.push(intermediateStack.pop());
          }

          if (!frameStack.isEmpty()) {
            var next = frameStack.pop();
            if (next instanceof Continuation) {
              ((Continuation) next).run();
            } else {
              // pollStack only contains Ns and Continuations, so the type
              // is guaranteed to be N here.
              @SuppressWarnings("unchecked")
              N result = (N) next;
              return result;
            }
          }

          if (intermediateStack.isEmpty() && frameStack.isEmpty()) {
            return null;
          }
        }
      }
    }

    return new DeepRecursiveTopo().recurse();
  }

  private enum VisitState {
    PARTIALLY_VISITED,
    FULLY_VISITED
  }

  @FunctionalInterface
  private interface Continuation {
    void run();
  }

  // TODO: Make a method to merge multiple graphs into one view.
  //   - What about graphs with the same node(s) and/or edge(s)?
  //   - We can use a property-based test: assert that merging a graph
  //     with its complement makes a complete graph (will only work with
  //     graphs with a node count >=5 according to
  //     https://math.stackexchange.com/a/2961132/1067289).
  //   - Assert that merging a graph with an empty graph returns the
  //     first graph.
  //   - Assert that merging a graph with a singleton graph returns the
  //     first graph plus the node from the second graph, if said node
  //     is not in the first graph already.
  //   - Read https://johanneslink.net/how-to-specify-it. The lessons at
  //     the very end recommended the following strategies first:
  //       1. Use model-based properties combined with validity properties.
  //          Perhaps a set of nodes and a multimap of source nodes to
  //          sink nodes will suffice as the model?
  //       2. If model is hard to define, then use many metamorphic
  //          properties instead.

  private MoreGraphs() {}
}
