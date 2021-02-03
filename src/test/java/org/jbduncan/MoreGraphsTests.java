package org.jbduncan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MoreGraphsTests {

  private static final String UNUSED = "unused-cell-value";

  @Test
  void whenBuildingGraphWithBftAndEmptyStartingNodes_thenResultIsEmptyGraph() {
    // when
    var result =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(
            ImmutableList.of(), node -> ImmutableList.of("any old node"));

    // then
    assertThat(result).isEqualTo(GraphBuilder.directed().allowsSelfLoops(true).immutable().build());
  }

  @Test
  void whenBuildingGraphWithBftAndEmptySuccessorsFunction_thenResultIsEqualToStartingNodes() {
    // when
    var result =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(
            ImmutableList.of("any old node"), node -> ImmutableList.of());

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
            ImmutableList.of(1),
            node -> {
              int nextNode = node * 2;
              return nextNode <= 4 ? ImmutableList.of(nextNode) : ImmutableList.of(1);
            });

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
  void whenBuildingGraphWithBftAndDirectedSelfLoopingGuavaGraph_thenResultIsIdentical() {
    // given
    var expectedGraph =
        GraphBuilder.directed().allowsSelfLoops(true).<Integer>immutable().putEdge(1, 1).build();

    // when
    var result = MoreGraphs.buildGraphWithBreadthFirstTraversal(ImmutableList.of(1), expectedGraph);

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
        MoreGraphs.buildGraphWithBreadthFirstTraversal(ImmutableList.of(1), successorsFunction);

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
        () -> MoreGraphs.buildGraphWithBreadthFirstTraversal(null, element -> Set.of());

    // then
    assertThatCode(codeUnderTest)
        .as(
            "MoreGraphs.buildGraphWithBreadthFirstTraversal(null, anySuccessorsFunction) "
                + "expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("startingNodes");
  }

  @Test
  void whenViewingNullTableAsValueGraph_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.asValueGraph(null);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.asValueGraph(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("table");
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenIsDirectedIsTrue() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.isDirected()).as("graph.isDirected() expected to be true").isTrue();
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenAllowsSelfLoopsIsFalse() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.allowsSelfLoops())
        .as("graph.allowsSelfLoops() expected to be false")
        .isFalse();
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenNodeOrderIsUnordered() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodeOrder())
        .as("graph.nodeOrder() expected to be unordered")
        .isEqualTo(ElementOrder.unordered());
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenIncidentEdgeOrderIsUnordered() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.incidentEdgeOrder())
        .as("graph.incidentEdgeOrder() expected to be unordered")
        .isEqualTo(ElementOrder.unordered());
  }

  @Test
  void givenEmptyTable_whenViewingAsValueGraph_thenNodesIsEmpty() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodes()).as("graph.nodes() expected to be empty").isEmpty();
  }

  @Test
  void givenTableWithOneCell_whenViewingAsValueGraph_thenNodesContainsTheRowAndColumnKeysOfCell() {
    // given
    var table = ImmutableTable.of("1", "A", "cell-value");

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodes())
        .as("graph.nodes() expected to contain row key and column key")
        .containsExactlyInAnyOrder("1", "A");
  }

  @Test
  void givenMutableTable_whenViewingAsValueGraph_andTableIsMutated_thenNodesIsMutatedToo() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));

    // when
    var result = MoreGraphs.asValueGraph(mutableTable).nodes();
    mutableTable.put("2", "A", UNUSED);

    // then
    assertThat(result).as("graph.nodes() to be mutated").containsExactlyInAnyOrder("1", "2", "A");
  }

  @Test
  void givenMutableTable_whenViewingAsValueGraph_andGettingNodes_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of());

    // when
    var nodes = MoreGraphs.asValueGraph(mutableTable).nodes();
    ThrowingCallable codeUnderTest = nodes::clear;

    // then
    assertThatCode(codeUnderTest)
        .as("graph.nodes() to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenEmptyTable_whenViewingAsValueGraph_thenEdgesIsEmpty() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.edges()).as("graph.edges() expected to be empty").isEmpty();
  }

  @Test
  void givenTableWithOneCell_whenViewingAsValueGraph_thenEdgesHasOneRespectiveEdge() {
    // given
    var table = ImmutableTable.of("1", "A", "cell-value");

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.edges())
        .as("graph.edges() expected to have one edge")
        .containsExactly(EndpointPair.ordered("1", "A"));
  }

  @Test
  void givenMutableTableAsValueGraph_whenTableIsMutated_thenEdgesIsMutatedToo() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));
    var result = MoreGraphs.asValueGraph(mutableTable).edges();

    // when
    mutableTable.put("2", "A", UNUSED);

    // then
    assertThat(result)
        .as("graph.edges() expected to be mutated")
        .containsExactlyInAnyOrder(EndpointPair.ordered("1", "A"), EndpointPair.ordered("2", "A"));
  }

  @Test
  void givenEmptyTableAsValueGraph_whenGettingEdgesSize_thenItIsEqualToZero() {
    // given
    var table = ImmutableTable.of();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.edges().size();

    // then
    assertThat(result).as("graph.edges().size() expected to be 0").isZero();
  }

  @Test
  void givenTableWithOneCellAsValueGraph_whenGettingEdgesSize_thenItIsEqualToOne() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.edges().size();

    // then
    assertThat(result).as("graph.edges().size() expected to be 1").isOne();
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingEdgesIterator_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var edgesIterator = graph.edges().iterator();

    // then
    assertThatCode(edgesIterator::remove)
        .as("graph.edges() expected to have unmodifiable iterator")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.successors(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfRowKey_thenContainsColumnKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.successors("1");

    // then
    assertThat(result)
        .as("graph.successors(aRowKey) expected to contain associated column key")
        .containsExactly("A");
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfColumnKey_thenIsEmpty() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.successors("A");

    // then
    assertThat(result).as("graph.successors(aColumnKey) expected to be empty").isEmpty();
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.successors("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingSuccessorsOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var successors = graph.successors("1");

    // then
    assertThatCode(successors::clear)
        .as("graph.successors(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.predecessors(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.predecessors(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfColumnKey_thenContainsRowKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.predecessors("A");

    // then
    assertThat(result)
        .as("graph.predecessors(aColumnKey) expected to contain associated row key")
        .containsExactly("1");
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfRowKey_thenIsEmpty() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.predecessors("1");

    // then
    assertThat(result).as("graph.predecessors(aRowKey) expected to be empty").isEmpty();
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.predecessors("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.predecessors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingPredecessorsOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var successors = graph.predecessors("A");

    // then
    assertThatCode(successors::clear)
        .as("graph.predecessors(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.adjacentNodes(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.adjacentNodes(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfRowKey_thenContainsColumnKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.adjacentNodes("1");

    // then
    assertThat(result)
        .as("graph.adjacentNode(aRowKey) expected to contain associated column key")
        .containsExactly("A");
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfColumnKey_thenContainsRowKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.adjacentNodes("A");

    // then
    assertThat(result)
        .as("graph.adjacentNode(aColumnKey) expected to contain associated row key")
        .containsExactly("1");
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.predecessors("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingAdjacentNodesOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var successors = graph.adjacentNodes("1");

    // then
    assertThatCode(successors::clear)
        .as("graph.adjacentNodes(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingOutDegreeOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.outDegree(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.outDegree(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @ParameterizedTest
  @MethodSource("tablesAndOutDegrees")
  void givenTableAsValueGraph_whenGettingOutDegreeOfRowKey_thenEqualToNumAssociatedColumnKeys(
      Table<String, String, String> table, int expectedOutDegree) {

    // given
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.outDegree("1");

    // then
    assertThat(result)
        .as("graph.outDegree(aRowKey) expected to be %s", expectedOutDegree)
        .isEqualTo(expectedOutDegree);
  }

  private static Stream<Arguments> tablesAndOutDegrees() {
    return Stream.of(
        arguments(ImmutableTable.of("1", "A", UNUSED), 1),
        arguments(ImmutableTable.builder().put("1", "A", UNUSED).put("1", "B", UNUSED).build(), 2));
  }

  @Test
  void givenTableAsValueGraph_whenGettingOutDegreeOfColumnKey_thenItIsZero() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.outDegree("A");

    // then
    assertThat(result).as("graph.outDegree(aColumnKey) expected to be zero").isZero();
  }

  @Test
  void givenTableAsValueGraph_whenGettingOutDegreeOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.outDegree("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.outDegree(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
