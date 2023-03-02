package me.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

// We test methods that purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreGraphsTests {

  @Test
  void whenBuildingGraphWithBftAndEmptyStartingNodes_thenResultIsEmptyGraph() {
    // when
    var result =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(Set.of(), node -> Set.of("any old node"));

    // then
    assertThat(result).isEqualTo(GraphBuilder.directed().allowsSelfLoops(true).immutable().build());
  }

  @Test
  void whenBuildingGraphWithBftAndEmptySuccessorsFunction_thenResultIsEqualToStartingNodes() {
    // when
    var result =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(Set.of("any old node"), node -> Set.of());

    // then
    assertThat(result)
        .isEqualTo(
            GraphBuilder.directed()
                .allowsSelfLoops(true)
                .immutable()
                .addNode("any old node")
                .build());
  }

  @Test
  void whenBuildingGraphWithBftAndCyclicSuccessorsFunction_thenResultTerminatesAndContainsCycle() {
    // when
    var result =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(
            Set.of(1), node -> (node * 2) <= 4 ? Set.of(node * 2) : Set.of(1));

    // then
    assertThat(result)
        .isEqualTo(
            GraphBuilder.directed()
                .allowsSelfLoops(true)
                .immutable()
                .putEdge(1, 2)
                .putEdge(2, 4)
                .putEdge(4, 1)
                .build());
  }

  @Test
  void whenBuildingGraphWithBftAndTreeShapedSuccessorsFunction_thenResultContainsTree() {
    // when
    var result =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(
            Set.of(1),
            node -> {
              if (node == 1) {
                return Set.of(2, 3);
              }
              return Set.of();
            });

    // then
    assertThat(result)
        .isEqualTo(
            GraphBuilder.directed()
                .allowsSelfLoops(true)
                .immutable()
                .putEdge(1, 2)
                .putEdge(1, 3)
                .build());
  }

  @Test
  void whenBuildingGraphWithBftAndDirectedSelfLoopingGuavaGraph_thenResultIsIdentical() {
    // given
    var expectedGraph =
        GraphBuilder.directed().allowsSelfLoops(true).<Integer>immutable().putEdge(1, 1).build();

    // when
    var result = MoreGraphs.buildGraphWithBreadthFirstTraversal(Set.of(1), expectedGraph);

    // then
    assertThat(result).isEqualTo(expectedGraph);
  }

  @Test
  void
      whenBuildingGraphWithBftAndUndirectedGuavaGraph_thenResultHasTwoDirectedEdgesPerUndirectedEdgeInTheInputGraph() {

    // given
    var successorsFunction =
        GraphBuilder.undirected().allowsSelfLoops(false).<Integer>immutable().putEdge(1, 2).build();

    // when
    ImmutableGraph<Integer> result =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(Set.of(1), successorsFunction);

    // then
    assertThat(result)
        .isEqualTo(
            GraphBuilder.directed()
                .allowsSelfLoops(true)
                .immutable()
                .putEdge(1, 2)
                .putEdge(2, 1)
                .build());
  }

  @Test
  void whenBuildingGraphWithBftAndNullSuccessorsFunction_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.buildGraphWithBreadthFirstTraversal(Set.of(), null);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "MoreGraphs.buildGraphWithBreadthFirstTraversal(anyStartingNodes, null) "
                + "expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("successorsFunction");
  }

  @Test
  void whenBuildingGraphWithBftAndNullStartingNodes_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.buildGraphWithBreadthFirstTraversal(null, __ -> Set.of());

    // then
    assertThatCode(codeUnderTest)
        .as(
            "MoreGraphs.buildGraphWithBreadthFirstTraversal(null, anySuccessorsFunction) "
                + "expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("startingNodes");
  }

  // TODO: Change the below unit test into a property-based test with jqwik.net, where the graph
  // inputs are generated with JGraphT's graph generators. Preferably allow jqwik to use any of
  // them, but at the very least allow it to use the following:
  //
  // https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/generate/EmptyGraphGenerator.html
  // https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/generate/GnmRandomGraphGenerator.html
  // https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/generate/GnpRandomGraphGenerator.html

  @Test
  void givenAGraph_whenCalculatingTopologicalOrdering_thenOrderingIsValid() {
    // given
    ImmutableGraph<String> graph =
        DotImporter.importGraph(
            "digraph G {\n"
                + "    one -> two;\n"
                + "    one -> three;\n"
                + "    one -> four;\n"
                + "    one -> five;\n"
                + "    five -> six;\n"
                + "    five -> seven;\n"
                + "}");

    // when
    var topologicalOrdering = MoreGraphs.topologicalOrdering(graph);

    // then
    assertThatTopologicalOrderingIsValid(graph, topologicalOrdering);
  }

  @Test
  void givenNullGraph_whenCalculatingTopologicalOrdering_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.topologicalOrdering(null);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.topologicalOrdering(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("graph");
  }

  @Test
  void givenCyclicGraph_whenCalculatingTopologicalOrdering_thenIaeIsThrown() {
    // given
    ImmutableGraph<String> graph = DotImporter.importGraph("digraph G { a -> b; b -> a; }");

    // when
    Iterable<String> topologicalOrdering = MoreGraphs.topologicalOrdering(graph);
    // Iterate over the topological ordering to force it to be evaluated.
    ThrowingCallable codeUnderTest = () -> topologicalOrdering.forEach(__ -> {});

    // then
    assertThatCode(codeUnderTest)
        .as(
            "Evaluating MoreGraphs.topologicalOrdering(cyclicGraph) expected to throw "
                + "IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("graph")
        .hasMessageContaining("cycle");
  }

  // TODO: Change the below unit test into a property-based test with jqwik.net, where the graph
  // inputs are generated with JGraphT's graph generators. Preferably allow jqwik to use any of
  // them, but at the very least allow it to use the following:
  //
  // https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/generate/EmptyGraphGenerator.html
  // https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/generate/GnmRandomGraphGenerator.html
  // https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/generate/GnpRandomGraphGenerator.html

  @Test
  void
      givenStartingNodesAndSuccessorsFunction_whenCalculatingTopologicalOrdering_thenOrderIsValid() {
    // given
    ImmutableGraph<String> graph =
        DotImporter.importGraph(
            "digraph G {\n"
                + "    one -> two;\n"
                + "    one -> three;\n"
                + "    one -> four;\n"
                + "    one -> five;\n"
                + "    five -> six;\n"
                + "    five -> seven;\n"
                + "}");

    // when
    var topologicalOrdering = MoreGraphs.topologicalOrderingStartingFrom(Set.of("one"), graph);

    // then
    assertThatTopologicalOrderingIsValid(graph, topologicalOrdering);
  }

  private static void assertThatTopologicalOrderingIsValid(
      ImmutableGraph<String> graph, Iterable<String> topologicalOrdering) {
    assertThat(topologicalOrdering).containsExactlyInAnyOrderElementsOf(graph.nodes());
    for (var edge : graph.edges()) {
      assertThat(edge.isOrdered()).isTrue();
      assertThat(topologicalOrdering).containsSubsequence(edge.source(), edge.target());
    }
  }

  @Test
  void givenNullSuccessorsFunction_whenCalculatingTopologicalOrdering_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.topologicalOrderingStartingFrom(Set.of(), null);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.topologicalOrdering(nodes, null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("successorsFunction");
  }

  @Test
  void givenNullStartingNodes_whenCalculatingTopologicalOrdering_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.topologicalOrderingStartingFrom(null, __ -> Set.of());

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.topologicalOrdering(nodes, null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("startingNodes");
  }

  @Test
  void givenCyclicSuccessorsFunction_whenCalculatingTopologicalOrdering_thenIaeIsThrown() {
    // given
    ImmutableGraph<String> graph = DotImporter.importGraph("digraph G { a -> b; b -> a; }");

    // when
    ThrowingCallable codeUnderTest =
        () -> MoreGraphs.topologicalOrderingStartingFrom(Set.of("a"), graph);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "Evaluating MoreGraphs.topologicalOrdering(cyclicGraph) expected to throw "
                + "IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("graph")
        .hasMessageContaining("cycle");
  }
}
