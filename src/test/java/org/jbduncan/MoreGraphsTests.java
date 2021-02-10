package org.jbduncan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

// We test methods that purposefully extend an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreGraphsTests {

  private static final String A_ROW_KEY = "aRowKey";
  private static final String A_COLUMN_KEY = "aColumnKey";
  private static final String A_CELL_VALUE = "aCellValue";
  private static final String ANOTHER_ROW_KEY = "anotherRowKey";
  private static final String ANOTHER_COLUMN_KEY = "anotherColumnKey";
  private static final String A_KEY_NOT_IN_TABLE = "aKeyNotInTable";
  private static final String THE_DEFAULT_EDGE_VALUE = "theDefaultEdgeValue";

  private static ImmutableTable<String, String, String> singleCellTable() {
    return ImmutableTable.of(A_ROW_KEY, A_COLUMN_KEY, A_CELL_VALUE);
  }

  private HashBasedTable<String, String, String> mutableSingleCellTable() {
    return HashBasedTable.create(singleCellTable());
  }

  private static <R, C, V> ImmutableTable<R, C, V> emptyTable() {
    return ImmutableTable.of();
  }

  private static <R, C, V> HashBasedTable<R, C, V> emptyMutableTable() {
    return HashBasedTable.create(emptyTable());
  }

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
            node -> (node * 2) <= 4 ? ImmutableList.of(node * 2) : ImmutableList.of(1));

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
            ImmutableList.of(1),
            node -> {
              if (node == 1) {
                return ImmutableList.of(2, 3);
              }
              return ImmutableList.of();
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
    var table = emptyTable();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.isDirected()).as("graph.isDirected() expected to be true").isTrue();
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenAllowsSelfLoopsIsFalse() {
    // given
    var table = emptyTable();

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
    var table = emptyTable();

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
    var table = emptyTable();

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
    var table = emptyTable();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodes()).as("graph.nodes() expected to be empty").isEmpty();
  }

  @Test
  void givenTableWithOneCell_whenViewingAsValueGraph_thenNodesEqualsTheRowAndColumnKeysOfTheCell() {
    // given
    var table = singleCellTable();

    // when
    var result = MoreGraphs.asValueGraph(table);

    // then
    assertThat(result.nodes())
        .as("graph.nodes() expected to contain row key and column key")
        .containsExactlyInAnyOrder(A_ROW_KEY, A_COLUMN_KEY);
  }

  @Test
  void givenMutableTableAsValueGraph_whenTableIsMutated_thenNodesIsMutatedToo() {
    // given
    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);
    var result = graph.nodes();

    // when
    mutableTable.put(ANOTHER_ROW_KEY, A_COLUMN_KEY, A_CELL_VALUE);

    // then
    assertThat(result)
        .as("graph.nodes() to be mutated")
        .containsExactlyInAnyOrder(A_ROW_KEY, ANOTHER_ROW_KEY, A_COLUMN_KEY);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingNodes_thenItIsUnmodifiable() {
    // given
    var mutableTable = emptyMutableTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var nodes = graph.nodes();

    // then
    assertThatCode(nodes::clear)
        .as("graph.nodes() to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfNullNode_thenThrowsNpe() {
    // given
    var table = emptyTable();
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
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.successors(A_ROW_KEY);

    // then
    assertThat(result)
        .as("graph.successors(aRowKey) expected to aColumnKey")
        .containsExactly(A_COLUMN_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfColumnKey_thenIsEmpty() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.successors(A_COLUMN_KEY);

    // then
    assertThat(result).as("graph.successors(aColumnKey) expected to be empty").isEmpty();
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.successors(A_KEY_NOT_IN_TABLE);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingSuccessorsOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var successors = graph.successors(A_ROW_KEY);

    // then
    assertThatCode(successors::clear)
        .as("graph.successors(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfNullNode_thenThrowsNpe() {
    // given
    var table = emptyTable();
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
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.predecessors(A_COLUMN_KEY);

    // then
    assertThat(result)
        .as("graph.predecessors(aColumnKey) expected to aRowKey")
        .containsExactly(A_ROW_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfRowKey_thenIsEmpty() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.predecessors(A_ROW_KEY);

    // then
    assertThat(result).as("graph.predecessors(aRowKey) expected to be empty").isEmpty();
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.predecessors(A_KEY_NOT_IN_TABLE);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.predecessors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingPredecessorsOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var successors = graph.predecessors(A_COLUMN_KEY);

    // then
    assertThatCode(successors::clear)
        .as("graph.predecessors(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfNullNode_thenThrowsNpe() {
    // given
    var table = emptyTable();
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
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.adjacentNodes(A_ROW_KEY);

    // then
    assertThat(result)
        .as("graph.adjacentNode(aRowKey) expected to contain aColumnKey")
        .containsExactly(A_COLUMN_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfColumnKey_thenContainsRowKey() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.adjacentNodes(A_COLUMN_KEY);

    // then
    assertThat(result)
        .as("graph.adjacentNode(aColumnKey) expected to aRowKey")
        .containsExactly(A_ROW_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfKeyNotInTable_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.predecessors(A_KEY_NOT_IN_TABLE);

    // then
    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingAdjacentNodesOfAnyNode_thenItIsUnmodifiable() {
    // given
    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    // when
    var result = graph.adjacentNodes(A_ROW_KEY);

    // then
    assertThatCode(result::clear)
        .as("graph.adjacentNodes(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfRowAndColumnKeys_thenIsCellValue() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.edgeValueOrDefault(A_ROW_KEY, A_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    // then
    assertThat(result)
        .as(
            "graph.edgeValueOrDefault(aRowKey, aColumnKey, theDefaultEdgeValue) "
                + "expected to return aCellValue")
        .isEqualTo(A_CELL_VALUE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfUnrelatedRowAndColumnKeys_thenIsDefaultEdgeValue() {
    // given
    var table =
        ImmutableTable.builder() //
            .put(A_ROW_KEY, A_COLUMN_KEY, A_CELL_VALUE)
            .put(ANOTHER_ROW_KEY, ANOTHER_COLUMN_KEY, A_CELL_VALUE)
            .build();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result = graph.edgeValueOrDefault(A_ROW_KEY, ANOTHER_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    // then
    assertThat(result)
        .as(
            "graph.edgeValueOrDefault(aRowKey, unrelatedColumnKey, theDefaultEdgeValue) "
                + "expected to return theDefaultEdgeValue")
        .isEqualTo(THE_DEFAULT_EDGE_VALUE);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereRowKeyIsNull_thenThrowsNpe() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(null, A_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.adjacentNodes(null, aColumnKey, theDefaultEdgeValue) "
                + "expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("nodeU");
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereColumnKeyIsNull_thenThrowsNpe() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(A_ROW_KEY, null, THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.adjacentNodes(aRowKey, null, theDefaultEdgeValue) "
                + "expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("nodeV");
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereRowKeyNotInTable_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(A_KEY_NOT_IN_TABLE, A_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.edgeValueOrDefault(aKeyNotInTable, aColumnKey, theDefaultEdgeValue) "
                + "expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereColumnKeyNotInTable_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(A_ROW_KEY, A_KEY_NOT_IN_TABLE, THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.edgeValueOrDefault(aRowKey, keyNotInThisTable, theDefaultEdgeValue) "
                + "expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfEdgeEndpoints_thenIsCellValue() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result =
        graph.edgeValueOrDefault(
            EndpointPair.ordered(A_ROW_KEY, A_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    // then
    assertThat(result)
        .as(
            "graph.edgeValueOfDefault(endpoints(aRowKey, aColumnKey), theDefaultEdgeValue) "
                + "expected to return aCellValue")
        .isEqualTo(A_CELL_VALUE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfAbsentEndpointsEdge_thenIsDefaultEdgeValue() {
    // given
    var table =
        ImmutableTable.builder() //
            .put(A_ROW_KEY, A_COLUMN_KEY, A_CELL_VALUE)
            .put(ANOTHER_ROW_KEY, ANOTHER_COLUMN_KEY, A_CELL_VALUE)
            .build();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var result =
        graph.edgeValueOrDefault(
            EndpointPair.ordered(A_ROW_KEY, ANOTHER_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    // then
    assertThat(result)
        .as(
            "graph.edgeValueOrDefault(endpoints(aRowKey, unrelatedColumnKey), theDefaultEdgeValue) "
                + "expected to return theDefaultEdgeValue")
        .isEqualTo(THE_DEFAULT_EDGE_VALUE);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfNullEndpoints_thenThrowsNpe() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest = () -> graph.edgeValueOrDefault(null, THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.adjacentNodes(nullEndpoints, theDefaultEdgeValue) "
                + "expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("endpoints");
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfUnorderedEndpoints_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest =
        () ->
            graph.edgeValueOrDefault(
                EndpointPair.unordered(A_ROW_KEY, A_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.adjacentNodes(unorderedEndpoints, theDefaultEdgeValue) "
                + "expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ordered");
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereFirstEndpointNotInTable_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest =
        () ->
            graph.edgeValueOrDefault(
                EndpointPair.ordered(A_KEY_NOT_IN_TABLE, A_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.adjacentNodes(endpoints(aKeyNotInTable, aColumnKey), theDefaultEdgeValue) "
                + "expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereLastEndpointNotInTable_thenThrowsIae() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    ThrowingCallable codeUnderTest =
        () ->
            graph.edgeValueOrDefault(
                EndpointPair.ordered(A_ROW_KEY, A_KEY_NOT_IN_TABLE), THE_DEFAULT_EDGE_VALUE);

    // then
    assertThatCode(codeUnderTest)
        .as(
            "graph.adjacentNodes(endpoints(aRowKey, aKeyNotInTable), theDefaultEdgeValue) "
                + "expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenEmptyTableAsValueGraph_whenGettingEdges_thenNumElementsIsEqualToZero() {
    // given
    var table = emptyTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var edges = graph.edges();

    // then
    assertThat(edges).size().as("graph.edges().size() expected to be 0").isZero();
  }

  @Test
  void givenSingleCellTableAsValueGraph_whenGettingEdges_thenNumElementsIsEqualToOne() {
    // given
    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    // when
    var edges = graph.edges();

    // then
    assertThat(edges).size().as("graph.edges().size() expected to be 1").isOne();
  }
}
