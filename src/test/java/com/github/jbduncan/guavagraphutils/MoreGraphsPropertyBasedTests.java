package com.github.jbduncan.guavagraphutils;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.Traverser;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

// We test methods that purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
public class MoreGraphsPropertyBasedTests {

  private static final int MIN_STARTING_NODES_COUNT_FOR_CYCLIC_GRAPH = 1;

  @Property
  void givenADag_whenCalculatingLazyTopologicalOrdering_thenOrderingIsValid(
      // given
      @ForAll(supplier = MoreArbitraries.DirectedAcyclicGraphs.class)
          ImmutableGraph<Integer> graph) {
    // when
    var topologicalOrdering = MoreGraphs.lazyTopologicalOrdering(graph);

    // then
    assertThatTopologicalOrderingIsValid(graph, topologicalOrdering);
  }

  @Property
  void givenADag_whenCalculatingLazyTopologicalOrdering_thenSizeIsNumNodesOfDag(
      // given
      @ForAll(supplier = MoreArbitraries.DirectedAcyclicGraphs.class)
          ImmutableGraph<Integer> graph) {
    // when
    var topologicalOrdering = MoreGraphs.lazyTopologicalOrdering(graph);

    // then
    assertThat(topologicalOrdering)
        .as(
            """
            MoreGraphs.lazyTopologicalOrdering(graph) expected to have \
            size equal to graph.nodes().size: %s\
            """,
            graph.nodes().size())
        .hasSize(graph.nodes().size());
  }

  @Example
  void givenNullGraph_whenCalculatingLazyTopologicalOrdering_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.lazyTopologicalOrdering(null);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.lazyTopologicalOrdering(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("graph");
  }

  @Example
  void givenSelfLoopingDag_whenCalculatingLazyTopologicalOrdering_thenIaeIsThrown() {
    // given
    ImmutableGraph<Integer> graph =
        GraphBuilder.directed().allowsSelfLoops(true).<Integer>immutable().putEdge(0, 0).build();

    // when
    ThrowingCallable codeUnderTest =
        () -> {
          // Force the topological ordering to be evaluated.
          MoreGraphs.lazyTopologicalOrdering(graph).forEach(__ -> {});
        };

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.lazyTopologicalOrdering(cyclicGraph) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("graph")
        .hasMessageContaining("cycle");
  }

  @Property
  void givenCyclicGraph_whenCalculatingLazyTopologicalOrdering_thenIaeIsThrown(
      // given
      @ForAll(supplier = MoreArbitraries.CyclicGraphs.class) ImmutableGraph<Integer> graph) {
    // when
    ThrowingCallable codeUnderTest =
        () -> {
          // Force the topological ordering to be evaluated.
          MoreGraphs.lazyTopologicalOrdering(graph).forEach(__ -> {});
        };

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.lazyTopologicalOrdering(cyclicGraph) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("graph")
        .hasMessageContaining("cycle");
  }

  @Property
  void givenStartingNodesAndDag_whenCalculatingTopologicalOrdering_thenOrderIsValid(
      @ForAll(supplier = DirectedAcyclicGraphsAndStartingNodes.class) GraphAndNodes graphAndNodes) {
    // given
    var graph = graphAndNodes.graph();
    var startingNodes = graphAndNodes.nodes();

    // when
    var topologicalOrdering = MoreGraphs.topologicalOrderingStartingFrom(startingNodes, graph);

    // then
    assertThatTopologicalOrderingStartingWithIsValid(startingNodes, graph, topologicalOrdering);
  }

  @Property
  void givenStartingNodesAndCyclicGraph_whenCalculatingTopologicalOrdering_thenIaeIsThrown(
      @ForAll(supplier = CyclicGraphsAndStartingNodes.class) GraphAndNodes graphAndNodes) {
    // given
    var cyclicGraph = graphAndNodes.graph();
    var startingNodes = graphAndNodes.nodes();

    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.topologicalOrderingStartingFrom(startingNodes, cyclicGraph);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.topologicalOrderingStartingFrom(nodes, cyclicGraph) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("successors function")
        .hasMessageContaining("cycle");
  }

  @Property
  void givenNullSuccessorsFunction_whenCalculatingTopologicalOrdering_thenNpeIsThrown(
      // given
      @ForAll List<Integer> startingNodes) {
    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.topologicalOrderingStartingFrom(startingNodes, null);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.topologicalOrderingStartingFrom(nodes, null) expected to throw \
            NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("successorsFunction");
  }

  @Property
  void givenNullStartingNodes_whenCalculatingTopologicalOrdering_thenNpeIsThrown(
      // given
      @ForAll(supplier = MoreArbitraries.Graphs.class) Graph<Integer> graph) {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.topologicalOrderingStartingFrom(null, graph);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.topologicalOrderingStartingFrom(nodes, null) expected to throw \
            NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("startingNodes");
  }

  @Property
  void givenStartingNodesWithNullNode_whenCalculatingTopologicalOrdering_thenNpeIsThrown(
      // given
      @ForAll(supplier = MoreArbitraries.Graphs.class) Graph<Integer> graph) {
    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.topologicalOrderingStartingFrom(singleton(null), graph);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.topologicalOrderingStartingFrom(nodes, null) expected to throw \
            NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("startingNodes has at least one null node");
  }

  @Example
  void givenNullGraph_whenCalculatingTopologicalOrdering_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.topologicalOrdering(null);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.topologicalOrdering(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("graph");
  }

  @Property
  void givenADag_whenCalculatingTopologicalOrdering_thenOrderingIsValid(
      // given
      @ForAll(supplier = MoreArbitraries.DirectedAcyclicGraphs.class)
          ImmutableGraph<Integer> graph) {
    // when
    var topologicalOrdering = MoreGraphs.topologicalOrdering(graph);

    // then
    assertThatTopologicalOrderingIsValid(graph, topologicalOrdering);
  }

  @Example
  void givenSelfLoopingDag_whenCalculatingTopologicalOrdering_thenIaeIsThrown() {
    // given
    ImmutableGraph<Integer> graph =
        GraphBuilder.directed().allowsSelfLoops(true).<Integer>immutable().putEdge(0, 0).build();

    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.topologicalOrdering(graph);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.topologicalOrdering(cyclicGraph) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("graph")
        .hasMessageContaining("cycle");
  }

  @Property
  void givenCyclicGraph_whenCalculatingTopologicalOrdering_thenIaeIsThrown(
      // given
      @ForAll(supplier = MoreArbitraries.CyclicGraphs.class) ImmutableGraph<Integer> graph) {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.topologicalOrdering(graph);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.lazyTopologicalOrdering(cyclicGraph) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("graph")
        .hasMessageContaining("cycle");
  }

  private static <N> void assertThatTopologicalOrderingIsValid(
      Graph<N> graph, Iterable<N> topologicalOrdering) {
    // copy into list for good performance
    topologicalOrdering = ImmutableList.copyOf(topologicalOrdering);

    assertThat(topologicalOrdering).containsExactlyInAnyOrderElementsOf(graph.nodes());
    assertThat(topologicalOrdering).doesNotHaveDuplicates();

    for (var edge : graph.edges()) {
      assertThat(edge.isOrdered()).isTrue();
      assertThat(topologicalOrdering).containsSubsequence(edge.source(), edge.target());
    }
  }

  private static <N> void assertThatTopologicalOrderingStartingWithIsValid(
      Iterable<N> startingNodes, Graph<N> graph, List<N> topologicalOrdering) {
    Iterable<N> reachableNodes = Traverser.forGraph(graph).breadthFirst(startingNodes);
    assertThat(topologicalOrdering).containsExactlyInAnyOrderElementsOf(reachableNodes);
    assertThat(topologicalOrdering).doesNotHaveDuplicates();

    Graph<N> reachableSubgraph = Graphs.inducedSubgraph(graph, reachableNodes);
    for (var edge : reachableSubgraph.edges()) {
      assertThat(edge.isOrdered()).isTrue();
      assertThat(topologicalOrdering).containsSubsequence(edge.source(), edge.target());
    }
  }

  record GraphAndNodes(Graph<Integer> graph, Set<Integer> nodes) {}

  static class DirectedAcyclicGraphsAndStartingNodes implements ArbitrarySupplier<GraphAndNodes> {
    @Override
    public Arbitrary<GraphAndNodes> get() {
      return new MoreArbitraries.DirectedAcyclicGraphs()
          .get()
          .flatMap(
              graph ->
                  Combinators.combine(Arbitraries.just(graph), Arbitraries.subsetOf(graph.nodes()))
                      .as(GraphAndNodes::new));
    }
  }

  static class CyclicGraphsAndStartingNodes implements ArbitrarySupplier<GraphAndNodes> {
    @Override
    public Arbitrary<GraphAndNodes> get() {
      return new MoreArbitraries.CyclicGraphs()
          .get()
          .flatMap(
              graph ->
                  Combinators.combine(
                          Arbitraries.just(graph),
                          Arbitraries.subsetOf(graph.nodes())
                              .ofMinSize(MIN_STARTING_NODES_COUNT_FOR_CYCLIC_GRAPH))
                      .as(GraphAndNodes::new));
    }
  }
}
