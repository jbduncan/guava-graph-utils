package com.github.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

// We test methods that purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreGraphsBuildGraphTests {

  @Test
  void whenBuildingGraphWithBftAndEmptyStartingNodes_thenResultIsEmptyGraph() {
    // when
    var result = MoreGraphs.buildGraph(Set.of(), node -> Set.of("any old node"));

    // then
    assertThat(result).isEqualTo(GraphBuilder.directed().allowsSelfLoops(true).immutable().build());
  }

  @Test
  void whenBuildingGraphWithBftAndEmptySuccessorsFunction_thenResultIsEqualToStartingNodes() {
    // when
    var result = MoreGraphs.buildGraph(Set.of("any old node"), node -> Set.of());

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
        MoreGraphs.buildGraph(Set.of(1), node -> (node * 2) <= 4 ? Set.of(node * 2) : Set.of(1));

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
        MoreGraphs.buildGraph(
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
    var result = MoreGraphs.buildGraph(Set.of(1), expectedGraph);

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
    ImmutableGraph<Integer> result = MoreGraphs.buildGraph(Set.of(1), successorsFunction);

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
    ThrowingCallable codeUnderTest = () -> MoreGraphs.buildGraph(Set.of(), null);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.buildGraphWithBreadthFirstTraversal(anyStartingNodes, null) \
            expected to throw NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("successorsFunction");
  }

  @Test
  void whenBuildingGraphWithBftAndNullStartingNodes_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.buildGraph(null, __ -> Set.of());

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.buildGraphWithBreadthFirstTraversal(null, anySuccessorsFunction) \
            expected to throw NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("startingNodes");
  }
}
