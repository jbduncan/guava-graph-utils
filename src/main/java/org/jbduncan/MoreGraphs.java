package org.jbduncan;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.graph.AbstractValueGraph;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.ValueGraph;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class MoreGraphs {

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
  public static <N> ImmutableGraph<N> buildGraphWithBreadthFirstTraversal(
      Iterable<N> startingNodes, SuccessorsFunction<N> successorsFunction) {
    MutableGraph<N> result = GraphBuilder.directed().allowsSelfLoops(true).build();
    startingNodes.forEach(result::addNode);
    Queue<N> nodesRemaining = Queues.newArrayDeque(startingNodes);
    while (!nodesRemaining.isEmpty()) {
      N next = nodesRemaining.remove();
      for (N successor : successorsFunction.successors(next)) {
        if (!result.edges().contains(EndpointPair.ordered(next, successor))) {
          nodesRemaining.add(successor);
          result.putEdge(next, successor);
        }
      }
    }
    return ImmutableGraph.copyOf(result);
  }

  /**
   * Returns an unmodifiable directed {@linkplain ValueGraph value graph} view of the given table
   * where:
   *
   * <ul>
   *   <li>The row keys and column keys of the table are viewed as the graph's nodes.
   *   <li>Every row-and-column mapping in the table is viewed as a directed edge from the row key
   *       to the column key in the graph.
   *   <li>Every cell value is viewed as the edge value of its row and column's respective edge in
   *       the graph.
   * </ul>
   *
   * <p>For example, the following table is viewed as the value graph beneath it.
   *
   * <h2>Table</h2>
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
   * <h2>Graph</h2>
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
   *   <li>{@link ValueGraph#predecessors(Object) predecessors(row key)}: returns an unmodifiable
   *       set view of the column keys associated with the row key; the order is undefined.
   *   <li>{@link ValueGraph#predecessors(Object) predecessors(column key)}: returns an empty
   *       immutable set.
   *   <li>{@link ValueGraph#predecessors(Object) predecessors(other value)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#successors(Object) successors(row key)}: returns an empty immutable
   *       set.
   *   <li>{@link ValueGraph#successors(Object) successors(column key)}: returns an unmodifiable set
   *       view of the row keys associated with the column key; the order is undefined.
   *   <li>{@link ValueGraph#successors(Object) predecessors(other value)}: throws an {@code
   *       IllegalArgumentException}.
   *   <li>{@link ValueGraph#adjacentNodes(Object) adjacentNodes(some value)}: returns an
   *       unmodifiable set union view of {@link ValueGraph#predecessors(Object) predecessors(some
   *       value)} and {@link ValueGraph#successors(Object) successors(some value)}; the order is
   *       undefined.
   *   <li>TODO: Describe the behaviour of the other methods of this value graph.
   * </ul>
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
  // TODO: Rewrite the implementation of this method using TDD
  public static <N, E> ValueGraph<N, E> asValueGraph(Table<N, N, E> table) {
    requireNonNull(table);

    return new AbstractValueGraph<>() {
      @Override
      public @Nullable E edgeValueOrDefault(N nodeU, N nodeV, @Nullable E defaultValue) {
        requireNonNull(nodeU);
        requireNonNull(nodeV);
        checkArgument(table.containsRow(nodeU));
        checkArgument(table.containsColumn(nodeV));
        return table.contains(nodeU, nodeV)
            ? requireNonNull(table.get(nodeU, nodeV))
            : defaultValue;
      }

      @Override
      public @Nullable E edgeValueOrDefault(EndpointPair<N> endpoints, @Nullable E defaultValue) {
        requireNonNull(endpoints);
        return edgeValueOrDefault(endpoints.nodeU(), endpoints.nodeV(), defaultValue);
      }

      @Override
      public Set<N> nodes() {
        return Sets.union(table.rowKeySet(), table.columnKeySet());
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
      public Set<N> adjacentNodes(N node) {
        return Sets.union(predecessors(node), successors(node));
      }

      @Override
      public Set<N> predecessors(N node) {
        requireNonNull(node);
        checkArgument(nodes().contains(node));
        return table.containsColumn(node)
            ? Collections.unmodifiableSet(table.column(node).keySet())
            : ImmutableSet.of();
      }

      @Override
      public Set<N> successors(N node) {
        requireNonNull(node);
        checkArgument(nodes().contains(node));
        return table.containsRow(node)
            ? Collections.unmodifiableSet(table.row(node).keySet())
            : ImmutableSet.of();
      }
    };
  }
}
