package com.github.jbduncan.guavagraphutils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Multisets.toMultiset;
import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.graph.AbstractGraph;
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
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

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
  public static <N> ImmutableGraph<N> buildGraph(
      Iterable<N> startingNodes, SuccessorsFunction<N> successorsFunction) {
    requireNonNull(startingNodes, "startingNodes");
    requireNonNull(successorsFunction, "successorsFunction");

    MutableGraph<N> result = GraphBuilder.directed().allowsSelfLoops(true).build();
    startingNodes.forEach(result::addNode);
    var nodesRemaining = Queues.newArrayDeque(startingNodes);
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
    requireNonNull(table, "table");

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
        requireNonNull(node, "node");
        checkArgument(nodes().contains(node), NODE_IS_NOT_IN_THIS_GRAPH, node);
        return Collections.unmodifiableSet(table.row(node).keySet());
      }

      @Override
      public Set<N> predecessors(N node) {
        requireNonNull(node, "node");
        checkArgument(nodes().contains(node), NODE_IS_NOT_IN_THIS_GRAPH, node);
        return Collections.unmodifiableSet(table.column(node).keySet());
      }

      @Override
      public Set<N> adjacentNodes(N node) {
        requireNonNull(node, "node");
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
        requireNonNull(nodeU, "nodeU");
        requireNonNull(nodeV, "nodeV");
        checkArgument(nodes().contains(nodeU), "First node '%s' is not in this graph", nodeU);
        checkArgument(nodes().contains(nodeV), "Second node '%s' is not in this graph", nodeV);
        if (table.contains(nodeU, nodeV)) {
          return table.get(nodeU, nodeV);
        }
        return defaultValue;
      }

      @Override
      public @Nullable E edgeValueOrDefault(EndpointPair<N> endpoints, @Nullable E defaultValue) {
        requireNonNull(endpoints, "endpoints");
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
   * traversal of the graph in which each node is visited only after all its {@linkplain
   * Graph#predecessors(Object) predecessors} and other ancestors have been visited.
   *
   * <p>This method is preferable to {@link MoreGraphs#topologicalOrdering(Graph)
   * MoreGraphs.topologicalOrdering} if the topological ordering will be iterated on only once, as
   * it will avoid putting the result into an intermediate list. Also, it is preferable to {@link
   * MoreGraphs#topologicalOrderingStartingFrom(Iterable, SuccessorsFunction)}
   * topologicalOrderingStartingFrom} when the given graph can be represented as a {@code Graph} and
   * a topological ordering over the entire graph is needed.
   *
   * <p>The given graph must be non-null, otherwise a {@code NullPointerException} will be thrown.
   *
   * <p>The returned iterable is an <i>unmodifiable, lazy view</i> of the graph's nodes, and each
   * and every iteration recalculates the topological ordering. So if the iterable is used multiple
   * times, use {@link MoreGraphs#topologicalOrdering(Graph) MoreGraphs.topologicalOrdering} and use
   * that list instead.
   *
   * <p>The given graph may have multiple valid topological orderings. This method does not
   * guarantee which topological ordering is returned on any iteration, or even that the same
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
   * <p>...the topological ordering returned from an iteration can be any of:
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
   * the graph, an {@code IllegalArgumentException} will be thrown.
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
    requireNonNull(graph, "graph");

    return () -> {
      /*
       * Kahn's algorithm. Derived from [1], in turn derived from [2].
       *
       * [1] https://web.archive.org/web/20230225053309/https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm
       * [2] https://dl.acm.org/doi/pdf/10.1145/368996.369025
       */

      Queue<N> roots = rootsOf(graph);
      Multiset<N> nonRoots = nonRootsOf(graph);

      return new AbstractIterator<>() {
        @Override
        protected N computeNext() {
          if (!roots.isEmpty()) {
            N next = roots.remove();
            for (N successor : graph.successors(next)) {
              nonRoots.remove(successor, 1);
              if (!nonRoots.contains(successor)) {
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

  /**
   * Returns an immutable list representing the topological ordering of the given graph; that is, a
   * traversal of the graph in which each node is visited only after all its {@linkplain
   * Graph#predecessors(Object) predecessors} and other ancestors have been visited.
   *
   * <p>This method is preferable to {@link MoreGraphs#lazyTopologicalOrdering(Graph)
   * MoreGraphs.lazyTopologicalOrdering} if the topological ordering will be iterated on multiple
   * times, as it will avoid recalculating the topological ordering each time. Also, it is
   * preferable to {@link MoreGraphs#topologicalOrderingStartingFrom(Iterable, SuccessorsFunction)}
   * topologicalOrderingStartingFrom} when the given graph can be represented as a {@code Graph} and
   * a topological ordering over the entire graph is needed.
   *
   * <p>The given graph must be non-null, otherwise a {@code NullPointerException} will be thrown.
   *
   * <p>The given graph may have multiple valid topological orderings. This method does not
   * guarantee which topological ordering is returned, even on subsequent calls.
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
   * <p>...the topological ordering returned can be any of:
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
   * the graph, an {@code IllegalArgumentException} will be thrown.
   *
   * <p>This method runs in linear time, specifically {@code O(N + E)}, where {@code N} is the
   * number of nodes in the graph and {@code E} is the number of edges.
   *
   * @param graph the graph to return a topological ordering for; must not be null
   * @param <N> the node type; must have {@link #equals(Object) equals()} and {@link #hashCode()
   *     hashCode()} implementations as described in "<a
   *     href='https://github.com/google/guava/wiki/GraphsExplained#graph-elements-nodes-and-edges'>
   *     Graphs Explained</a>".
   * @return an immutable list representing a topological ordering of the graph
   * @throws IllegalArgumentException if the graph has a cycle
   * @see <a href='https://en.wikipedia.org/wiki/Topological_sorting'>Wikipedia, "Topological
   *     sorting"</a>
   * @see <a href='https://github.com/google/guava/wiki/GraphsExplained'>Graphs Explained</a>
   */
  public static <N> ImmutableList<N> topologicalOrdering(Graph<N> graph) {
    requireNonNull(graph, "graph");

    Queue<N> roots = rootsOf(graph);
    Multiset<N> nonRoots = nonRootsOf(graph);

    ImmutableList.Builder<N> result = ImmutableList.builder();
    while (!roots.isEmpty()) {
      N next = roots.remove();
      result.add(next);
      for (N successor : graph.successors(next)) {
        nonRoots.remove(successor, 1);
        if (!nonRoots.contains(successor)) {
          roots.add(successor);
        }
      }
    }
    checkArgument(nonRoots.isEmpty(), GRAPH_HAS_AT_LEAST_ONE_CYCLE);
    return result.build();
  }

  private static <N> Multiset<N> nonRootsOf(Graph<N> graph) {
    return graph.nodes().stream()
        .filter(node -> graph.inDegree(node) > 0)
        .collect(toMultiset(node -> node, graph::inDegree, HashMultiset::create));
  }

  private static <N> Queue<N> rootsOf(Graph<N> graph) {
    return graph.nodes().stream()
        .filter(node -> graph.inDegree(node) == 0)
        .collect(toCollection(ArrayDeque::new));
  }

  /**
   * Returns an immutable list representing the topological ordering of the graph, specifically the
   * subgraph that is {@linkplain com.google.common.graph.Graphs#reachableNodes(Graph, Object)
   * reachable} from the given starting nodes. A topological ordering is a traversal of the subgraph
   * in which each node is visited only after all its {@linkplain Graph#predecessors(Object)
   * predecessors} and other ancestors have been visited.
   *
   * <p>This method is preferable to {@link MoreGraphs#lazyTopologicalOrdering(Graph)
   * lazyTopologicalOrdering} and {@link MoreGraphs#topologicalOrdering(Graph) topologicalOrdering}
   * when the graph can only be represented as a successors function or when only the subgraph
   * reachable from the starting nodes is needed.
   *
   * <p>The given starting nodes iterable, its elements, and the graph must all be non-null,
   * otherwise a {@code NullPointerException} will be thrown.
   *
   * <p>The given graph may have multiple valid topological orderings. This method does not
   * guarantee which topological ordering is returned, even on subsequent calls.
   *
   * <p>For example, given this graph and starting node {@code a}...
   *
   * <pre>{@code
   * b <--- a ---> d   g ---> h
   * |      |                 |
   * v      v                 v
   * e ---> c ---> f          i
   * }</pre>
   *
   * <p>...the topological ordering returned can be any of:
   *
   * <ul>
   *   <li>{@code [a, b, d, e, c, f]}
   *   <li>{@code [a, b, e, c, d, f]}
   *   <li>{@code [a, b, e, d, c, f]}
   *   <li>{@code [a, b, e, c, f, d]}
   *   <li>{@code [a, d, b, e, c, f]}
   * </ul>
   *
   * <p>Note that the subgraph {@code {g, h, i}} is not reachable from node {@code a}. Thus, its
   * nodes are not included in the topological ordering.
   *
   * <p>This method only works on directed acyclic graphs. If a cycle is discovered when traversing
   * the graph, an {@code IllegalArgumentException} will be thrown.
   *
   * <p>This method runs in linear time, specifically {@code O(N + E)}, where {@code N} is the
   * number of nodes in the graph and {@code E} is the number of edges.
   *
   * @param startingNodes the nodes to start traversing the graph from; the iterable and the nodes
   *     themselves must not be null
   * @param successorsFunction the graph to return a topological ordering for; must not be null
   * @param <N> the node type; must have {@link #equals(Object) equals()} and {@link #hashCode()
   *     hashCode()} implementations as described in "<a
   *     href='https://github.com/google/guava/wiki/GraphsExplained#graph-elements-nodes-and-edges'>
   *     Graphs Explained</a>".
   * @return an immutable list representing a topological ordering of the graph
   * @throws IllegalArgumentException if the graph has a cycle
   * @see <a href='https://en.wikipedia.org/wiki/Topological_sorting'>Wikipedia, "Topological
   *     sorting"</a>
   * @see <a href='https://github.com/google/guava/wiki/GraphsExplained'>Graphs Explained</a>
   */
  public static <N> ImmutableList<N> topologicalOrderingStartingFrom(
      Iterable<N> startingNodes, SuccessorsFunction<N> successorsFunction) {
    requireNonNull(startingNodes, "startingNodes");
    for (N startingNode : startingNodes) {
      requireNonNull(startingNode, "startingNodes has at least one null node");
    }
    requireNonNull(successorsFunction, "successorsFunction");

    /*
     * Depth-first-search-based algorithm. Derived from [1], in turn derived from Introduction to Algorithms (2nd ed.)
     * by Cormen et al.
     *
     * The original algorithm by Cormen et al. is recursive, which may throw StackOverflowErrors on large inputs, so
     * this algorithm uses an iterative "deep recursion" technique to simulate the stack and avoid SOEs.
     *
     * [1] https://web.archive.org/web/20230225053309/https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
     */

    return new DeepRecursiveTopo<>(startingNodes, successorsFunction).recurse();
  }

  abstract static class DeepRecursion<T> {
    @FunctionalInterface
    interface Closure {
      void run();
    }

    private final Deque<Object> intermediateStack = new ArrayDeque<>();
    private final Deque<Object> frameStack = new ArrayDeque<>();

    void yieldClosure(Closure closure) {
      intermediateStack.push(closure);
    }

    void yieldValue(T value) {
      intermediateStack.push(value);
    }

    ImmutableList<T> recurse() {
      //noinspection ConstantValue
      return Stream.generate(this::next)
          .takeWhile(Objects::nonNull) // null signals end of data
          .collect(toImmutableList())
          .reverse();
    }

    private @Nullable T next() {
      while (true) {
        while (!intermediateStack.isEmpty()) {
          frameStack.push(intermediateStack.pop());
        }

        if (!frameStack.isEmpty()) {
          var next = frameStack.pop();
          if (next instanceof Closure closure) {
            closure.run();
          } else {
            // frameStack only contains Ns and Closures, so the type is
            // guaranteed to be N here.
            @SuppressWarnings("unchecked")
            T result = (T) next;
            return result;
          }
        }

        if (intermediateStack.isEmpty() && frameStack.isEmpty()) {
          return null; // end of data
        }
      }
    }
  }

  static final class DeepRecursiveTopo<N> extends DeepRecursion<N> {
    private enum VisitState {
      PARTIALLY_VISITED,
      FULLY_VISITED
    }

    private final SuccessorsFunction<N> successorsFunction;
    private final Map<N, VisitState> nodeToVisitState = new HashMap<>();

    DeepRecursiveTopo(Iterable<N> startingNodes, SuccessorsFunction<N> successorsFunction) {
      this.successorsFunction = successorsFunction;
      for (N startingNode : startingNodes) {
        yieldClosure(() -> visit(startingNode));
      }
    }

    private void visit(N node) {
      var visitState = nodeToVisitState.get(node);
      if (visitState == VisitState.FULLY_VISITED) {
        return;
      }
      if (visitState == VisitState.PARTIALLY_VISITED) {
        throw new IllegalArgumentException(SUCCESSORS_FUNCTION_HAS_AT_LEAST_ONE_CYCLE);
      }

      nodeToVisitState.put(node, VisitState.PARTIALLY_VISITED);

      for (N successor : successorsFunction.successors(node)) {
        yieldClosure(() -> visit(successor));
      }

      yieldClosure(() -> nodeToVisitState.put(node, VisitState.FULLY_VISITED));
      yieldValue(node);
    }
  }

  // TODO: Javadoc
  public static <N> Graph<N> union(Graph<N> first, Graph<N> second) {
    requireNonNull(first, "first");
    requireNonNull(second, "second");
    checkArgument(
        first.isDirected() == second.isDirected(),
        "Graph.isDirected() is not consistent for both graphs");
    checkArgument(
        first.allowsSelfLoops() == second.allowsSelfLoops(),
        "Graph.allowsSelfLoops() is not consistent for both graphs");
    checkArgument(
        first.nodeOrder().equals(second.nodeOrder()),
        "Graph.nodeOrder() is not consistent for both graphs");
    checkArgument(
        first.incidentEdgeOrder().equals(second.incidentEdgeOrder()),
        "Graph.incidentEdgeOrder() is not consistent for both graphs");

    return new AbstractGraph<>() {
      @Override
      public Set<N> nodes() {
        return Sets.union(first.nodes(), second.nodes());
      }

      @Override
      public boolean isDirected() {
        return first.isDirected();
      }

      @Override
      public boolean allowsSelfLoops() {
        return first.allowsSelfLoops();
      }

      @Override
      public ElementOrder<N> nodeOrder() {
        return first.nodeOrder();
      }

      @Override
      public Set<N> adjacentNodes(N node) {
        return neighbours(node, Graph::adjacentNodes);
      }

      @Override
      public Set<N> predecessors(N node) {
        return neighbours(node, Graph::predecessors);
      }

      @Override
      public Set<N> successors(N node) {
        return neighbours(node, Graph::successors);
      }

      private Set<N> neighbours(N node, BiFunction<Graph<N>, N, Set<N>> neighbours) {
        checkArgument(
            first.nodes().contains(node) || second.nodes().contains(node),
            "Node %s is not an element of this graph.",
            node);

        // A ForwardingSet is used to re-evaluate the set every time it is used for two reasons:
        // - "neighbours" throws IAE if "first" or "second" do not contain "node", so Sets.union
        //   cannot always be used.
        // - The neighbours of "node" can change if "first" or "second" are ever mutated, so if
        //   only "first" or "second"'s neighbours were returned, the result could become
        //   outdated.
        return new ForwardingSet<>() {
          @Override
          protected Set<N> delegate() {
            if (!second.nodes().contains(node)) {
              return neighbours.apply(first, node);
            }
            if (!first.nodes().contains(node)) {
              return neighbours.apply(second, node);
            }
            return Sets.union(neighbours.apply(first, node), neighbours.apply(second, node));
          }
        };
      }

      @Override
      public ElementOrder<N> incidentEdgeOrder() {
        return first.incidentEdgeOrder();
      }
    };
  }

  private static final double DAMPING_FACTOR = 0.85;
  private static final int DEFAULT_ITERATIONS = 10_000;

  // TODO: Change into a builder that accepts dampingFactor, maxIterations and tolerance.
  // TODO: Javadoc
  public static <N> ImmutableMap<N, Double> pageRanks(Graph<N> graph) {
    int n = graph.nodes().size();
    Map<N, Double> currentPageRanks = Maps.newHashMapWithExpectedSize(n);
    for (N node : graph.nodes()) {
      currentPageRanks.put(node, 1.0 / n);
    }
    Map<N, Double> nextPageRanks = Maps.newHashMapWithExpectedSize(n);

    for (int i = 0; i < DEFAULT_ITERATIONS; i++) {
      double left = 0.0;
      for (N node : graph.nodes()) {
        left +=
            graph.successors(node).isEmpty()
                ? requireNonNull(currentPageRanks.get(node))
                : (1 - DAMPING_FACTOR) * requireNonNull(currentPageRanks.get(node));
      }
      left /= n;

      for (N node : graph.nodes()) {
        double sum = 0.0;
        for (N predecessor : graph.predecessors(node)) {
          sum +=
              requireNonNull(currentPageRanks.get(predecessor))
                  / graph.successors(predecessor).size();
        }
        double right = DAMPING_FACTOR * sum;

        double pr = left + right;
        nextPageRanks.put(node, pr);
      }

      var tmp = currentPageRanks;
      currentPageRanks = nextPageRanks;
      nextPageRanks = tmp;
    }

    return currentPageRanks.entrySet().stream()
        .sorted(comparingByValue(reverseOrder()))
        .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private MoreGraphs() {}
}
