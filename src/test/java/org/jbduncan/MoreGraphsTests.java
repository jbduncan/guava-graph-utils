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
    assertThat(result).isEqualTo(GraphBuilder.directed().allowsSelfLoops(true).build());
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
  void whenViewingNullTableAsValueGraph_thenNpeIsThrown() {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.asValueGraph(null);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.asValueGraph(null) throws NullPointerException")
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
    assertThat(result.isDirected()).as("graph.isDirected() is true").isTrue();
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenAllowsSelfLoopsIsFalse() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.allowsSelfLoops()).as("graph.allowsSelfLoops() is false").isFalse();
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenNodeOrderIsUnordered() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodeOrder())
        .as("graph.nodeOrder() is unordered")
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
        .as("graph.incidentEdgeOrder() is unordered")
        .isEqualTo(ElementOrder.unordered());
  }

  @Test
  void givenEmptyTable_whenViewingAsValueGraph_thenNodesIsEmpty() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodes()).as("graph.nodes() is empty").isEmpty();
  }

  @Test
  void givenTableWithOneCell_whenViewingAsValueGraph_thenNodesContainsTheRowAndColumnKeysOfCell() {
    // given
    var table = ImmutableTable.of("1", "A", "cell-value");

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodes())
        .as("graph.nodes() contains row key and column key")
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
    assertThat(result).as("graph.nodes() is mutated").containsExactlyInAnyOrder("1", "2", "A");
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
        .as("graph.nodes() is unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenEmptyTable_whenViewingAsValueGraph_thenEdgesIsEmpty() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.edges()).as("graph.edges() is empty").isEmpty();
  }

  @Test
  void givenTableWithOneCell_whenViewingAsValueGraph_thenEdgesHasOneRespectiveEdge() {
    // given
    var table = ImmutableTable.of("1", "A", "cell-value");

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.edges())
        .as("graph.edges() has one edge")
        .containsExactly(EndpointPair.ordered("1", "A"));
  }

  @Test
  void givenMutableTable_whenViewingAsValueGraph_andTableIsMutated_thenEdgesIsMutatedToo() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));

    // when
    var result = MoreGraphs.asValueGraph(mutableTable).edges();
    mutableTable.put("2", "A", UNUSED);

    // then
    assertThat(result)
        .as("graph.edges() is mutated")
        .containsExactlyInAnyOrder(EndpointPair.ordered("1", "A"), EndpointPair.ordered("2", "A"));
  }

  @Test
  void givenEmptyTable_whenViewingAsValueGraph_andGettingEdgesSize_thenItIsEqualToZero() {
    // given
    var table = ImmutableTable.of();

    // when
    var result = MoreGraphs.asValueGraph(table).edges().size();

    // then
    assertThat(result).as("graph.edges().size() is 0").isEqualTo(0);
  }

  @Test
  void givenTableWithOneCell_whenViewingAsValueGraph_andGettingEdgesSize_thenItIsEqualToOne() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).edges().size();

    // then
    assertThat(result).as("graph.edges().size() is 1").isEqualTo(1);
  }

  @Test
  void givenMutableTable_whenViewingAsValueGraph_andGettingEdgesIterator_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create();

    // when
    var edgesIterator = MoreGraphs.asValueGraph(mutableTable).edges().iterator();
    ThrowingCallable codeUnderTest = edgesIterator::remove;

    // then
    assertThatCode(codeUnderTest)
        .as("graph.edges() has unmodifiable iterator")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingSuccessorsOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.successors(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(null) throws NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingSuccessorsOfRowKey_thenContainsColumnKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).successors("1");

    // then
    assertThat(result)
        .as("graph.successors(aRowKey) contains associated column key")
        .containsExactly("A");
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingSuccessorsOfColumnKey_thenIsEmpty() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).successors("A");

    // then
    assertThat(result).as("graph.successors(aColumnKey) is empty").isEmpty();
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingSuccessorsOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.successors("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) throws IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void
      givenMutableTable_whenViewingAsValueGraph_andGettingSuccessorsOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));

    // when
    var successors = MoreGraphs.asValueGraph(mutableTable).successors("1");
    ThrowingCallable codeUnderTest = successors::clear;

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(aNode) is unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingPredecessorsOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.predecessors(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.predecessors(null) throws NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingPredecessorsOfColumnKey_thenContainsRowKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).predecessors("A");

    // then
    assertThat(result)
        .as("graph.predecessors(aColumnKey) contains associated row key")
        .containsExactly("1");
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingPredecessorsOfRowKey_thenIsEmpty() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).predecessors("1");

    // then
    assertThat(result).as("graph.predecessors(aRowKey) is empty").isEmpty();
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingPredecessorsOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.predecessors("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.predecessors(aKeyNotInTable) throws IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void
      givenMutableTable_whenViewingAsValueGraph_andGettingPredecessorsOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));

    // when
    var successors = MoreGraphs.asValueGraph(mutableTable).predecessors("A");
    ThrowingCallable codeUnderTest = successors::clear;

    // then
    assertThatCode(codeUnderTest)
        .as("graph.predecessors(aNode) is unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingAdjacentNodesOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.adjacentNodes(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.adjacentNodes(null) throws NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingAdjacentNodesOfRowKey_thenContainsColumnKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).adjacentNodes("1");

    // then
    assertThat(result)
        .as("graph.adjacentNode(aRowKey) contains associated column key")
        .containsExactly("A");
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingAdjacentNodesOfColumnKey_thenContainsRowKey() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).adjacentNodes("A");

    // then
    assertThat(result)
        .as("graph.adjacentNode(aColumnKey) contains associated row key")
        .containsExactly("1");
  }

  @Test
  void
      givenTable_whenViewingAsValueGraph_andGettingAdjacentNodesOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.predecessors("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) throws IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void
      givenMutableTable_whenViewingAsValueGraph_andGettingAdjacentNodesOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = HashBasedTable.create(ImmutableTable.of("1", "A", UNUSED));

    // when
    var successors = MoreGraphs.asValueGraph(mutableTable).adjacentNodes("1");
    ThrowingCallable codeUnderTest = successors::clear;

    // then
    assertThatCode(codeUnderTest)
        .as("graph.adjacentNodes(aNode) is unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingOutDegreeOfNullNode_thenThrowsNpe() {
    // given
    var table = ImmutableTable.of();

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.outDegree(null);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.outDegree(null) throws NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @ParameterizedTest
  @MethodSource("tablesAndOutDegrees")
  void
      givenTable_whenViewingAsValueGraph_andGettingOutDegreeOfRowKey_thenEqualToNumAssociatedColumnKeys(
          Table<String, String, String> table, int expectedOutDegree) {
    // when
    var result = MoreGraphs.asValueGraph(table).outDegree("1");

    // then
    assertThat(result)
        .as("graph.outDegree(aRowKey) is %s", expectedOutDegree)
        .isEqualTo(expectedOutDegree);
  }

  private static Stream<Arguments> tablesAndOutDegrees() {
    return Stream.of(
        arguments(ImmutableTable.of("1", "A", UNUSED), 1),
        arguments(ImmutableTable.builder().put("1", "A", UNUSED).put("1", "B", UNUSED).build(), 2));
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingOutDegreeOfColumnKey_thenItIsZero() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var result = MoreGraphs.asValueGraph(table).outDegree("A");

    // then
    assertThat(result).as("graph.outDegree(aColumnKey) is zero").isZero();
  }

  @Test
  void givenTable_whenViewingAsValueGraph_andGettingOutDegreeOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = ImmutableTable.of("1", "A", UNUSED);

    // when
    var graph = MoreGraphs.asValueGraph(table);
    ThrowingCallable codeUnderTest = () -> graph.outDegree("other");

    // then
    assertThatCode(codeUnderTest)
        .as("graph.outDegree(aKeyNotInTable) throws IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
