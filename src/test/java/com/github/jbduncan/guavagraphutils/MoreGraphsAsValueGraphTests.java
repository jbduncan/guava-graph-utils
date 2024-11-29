package com.github.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  // We test a method that purposefully uses an unstable Guava API.
  "UnstableApiUsage",
  // We also test that the method reacts gracefully in the face of nulls.
  "ConstantConditions"
})
class MoreGraphsAsValueGraphTests {

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
  void whenViewingNullTableAsValueGraph_thenNpeIsThrown() {

    ThrowingCallable codeUnderTest = () -> MoreGraphs.asValueGraph(null);

    assertThatCode(codeUnderTest)
        .as("MoreGraphs.asValueGraph(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("table");
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenIsDirectedIsTrue() {

    var table = emptyTable();

    var result = MoreGraphs.asValueGraph(table);

    assertThat(result.isDirected()).as("graph.isDirected() expected to be true").isTrue();
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenAllowsSelfLoopsIsFalse() {

    var table = emptyTable();

    var result = MoreGraphs.asValueGraph(table);

    assertThat(result.allowsSelfLoops())
        .as("graph.allowsSelfLoops() expected to be false")
        .isFalse();
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenNodeOrderIsUnordered() {

    var table = emptyTable();

    var result = MoreGraphs.asValueGraph(table);

    assertThat(result.nodeOrder())
        .as("graph.nodeOrder() expected to be unordered")
        .isEqualTo(ElementOrder.unordered());
  }

  @Test
  void givenAnyTable_whenViewingAsValueGraph_thenIncidentEdgeOrderIsUnordered() {

    var table = emptyTable();

    var result = MoreGraphs.asValueGraph(table);

    assertThat(result.incidentEdgeOrder())
        .as("graph.incidentEdgeOrder() expected to be unordered")
        .isEqualTo(ElementOrder.unordered());
  }

  @Test
  void givenEmptyTable_whenViewingAsValueGraph_thenNodesIsEmpty() {

    var table = emptyTable();

    var result = MoreGraphs.asValueGraph(table);

    assertThat(result.nodes()).as("graph.nodes() expected to be empty").isEmpty();
  }

  @Test
  void givenTableWithOneCell_whenViewingAsValueGraph_thenNodesEqualsTheRowAndColumnKeysOfTheCell() {

    var table = singleCellTable();

    var result = MoreGraphs.asValueGraph(table);

    assertThat(result.nodes())
        .as("graph.nodes() expected to contain row key and column key")
        .containsExactlyInAnyOrder(A_ROW_KEY, A_COLUMN_KEY);
  }

  @Test
  void givenMutableTableAsValueGraph_whenTableIsMutated_thenNodesIsMutatedToo() {

    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);
    var result = graph.nodes();

    mutableTable.put(ANOTHER_ROW_KEY, A_COLUMN_KEY, A_CELL_VALUE);

    assertThat(result)
        .as("graph.nodes() to be mutated")
        .containsExactlyInAnyOrder(A_ROW_KEY, ANOTHER_ROW_KEY, A_COLUMN_KEY);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingNodes_thenItIsUnmodifiable() {

    var mutableTable = emptyMutableTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    var nodes = graph.nodes();

    assertThatCode(nodes::clear)
        .as("graph.nodes() to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfNullNode_thenThrowsNpe() {

    var table = emptyTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // successors should throw, not return
    ThrowingCallable codeUnderTest = () -> graph.successors(null);

    assertThatCode(codeUnderTest)
        .as("graph.successors(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfRowKey_thenContainsColumnKey() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.successors(A_ROW_KEY);

    assertThat(result)
        .as("graph.successors(aRowKey) expected to aColumnKey")
        .containsExactly(A_COLUMN_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfColumnKey_thenIsEmpty() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.successors(A_COLUMN_KEY);

    assertThat(result).as("graph.successors(aColumnKey) expected to be empty").isEmpty();
  }

  @Test
  void givenTableAsValueGraph_whenGettingSuccessorsOfKeyNotInTable_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // successors should throw, not return
    ThrowingCallable codeUnderTest = () -> graph.successors(A_KEY_NOT_IN_TABLE);

    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingSuccessorsOfAnyNode_thenItIsUnmodifiable() {

    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    var successors = graph.successors(A_ROW_KEY);

    assertThatCode(successors::clear)
        .as("graph.successors(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfNullNode_thenThrowsNpe() {

    var table = emptyTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // predecessors should throw, not return
    ThrowingCallable codeUnderTest = () -> graph.predecessors(null);

    assertThatCode(codeUnderTest)
        .as("graph.predecessors(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfColumnKey_thenContainsRowKey() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.predecessors(A_COLUMN_KEY);

    assertThat(result)
        .as("graph.predecessors(aColumnKey) expected to aRowKey")
        .containsExactly(A_ROW_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfRowKey_thenIsEmpty() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.predecessors(A_ROW_KEY);

    assertThat(result).as("graph.predecessors(aRowKey) expected to be empty").isEmpty();
  }

  @Test
  void givenTableAsValueGraph_whenGettingPredecessorsOfKeyNotInTable_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // predecessors should throw, not return
    ThrowingCallable codeUnderTest = () -> graph.predecessors(A_KEY_NOT_IN_TABLE);

    assertThatCode(codeUnderTest)
        .as("graph.predecessors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingPredecessorsOfAnyNode_thenItIsUnmodifiable() {

    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    var successors = graph.predecessors(A_COLUMN_KEY);

    assertThatCode(successors::clear)
        .as("graph.predecessors(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfNullNode_thenThrowsNpe() {

    var table = emptyTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // adjacentNodes should throw, not return
    ThrowingCallable codeUnderTest = () -> graph.adjacentNodes(null);

    assertThatCode(codeUnderTest)
        .as("graph.adjacentNodes(null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("node");
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfRowKey_thenContainsColumnKey() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.adjacentNodes(A_ROW_KEY);

    assertThat(result)
        .as("graph.adjacentNode(aRowKey) expected to contain aColumnKey")
        .containsExactly(A_COLUMN_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfColumnKey_thenContainsRowKey() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.adjacentNodes(A_COLUMN_KEY);

    assertThat(result)
        .as("graph.adjacentNode(aColumnKey) expected to aRowKey")
        .containsExactly(A_ROW_KEY);
  }

  @Test
  void givenTableAsValueGraph_whenGettingAdjacentNodesOfKeyNotInTable_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // predecessors should throw, not return
    ThrowingCallable codeUnderTest = () -> graph.predecessors(A_KEY_NOT_IN_TABLE);

    assertThatCode(codeUnderTest)
        .as("graph.successors(aKeyNotInTable) expected to throw IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenMutableTableAsValueGraph_whenGettingAdjacentNodesOfAnyNode_thenItIsUnmodifiable() {

    var mutableTable = mutableSingleCellTable();
    var graph = MoreGraphs.asValueGraph(mutableTable);

    var result = graph.adjacentNodes(A_ROW_KEY);

    assertThatCode(result::clear)
        .as("graph.adjacentNodes(aNode) expected to be unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfRowAndColumnKeys_thenIsCellValue() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.edgeValueOrDefault(A_ROW_KEY, A_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    assertThat(result)
        .as(
            """
            graph.edgeValueOrDefault(aRowKey, aColumnKey, theDefaultEdgeValue) \
            expected to return aCellValue\
            """)
        .isEqualTo(A_CELL_VALUE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfUnrelatedRowAndColumnKeys_thenIsDefaultEdgeValue() {

    var table =
        ImmutableTable.builder() //
            .put(A_ROW_KEY, A_COLUMN_KEY, A_CELL_VALUE)
            .put(ANOTHER_ROW_KEY, ANOTHER_COLUMN_KEY, A_CELL_VALUE)
            .build();
    var graph = MoreGraphs.asValueGraph(table);

    var result = graph.edgeValueOrDefault(A_ROW_KEY, ANOTHER_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    assertThat(result)
        .as(
            """
            graph.edgeValueOrDefault(aRowKey, unrelatedColumnKey, theDefaultEdgeValue) \
            expected to return theDefaultEdgeValue\
            """)
        .isEqualTo(THE_DEFAULT_EDGE_VALUE);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereRowKeyIsNull_thenThrowsNpe() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(null, A_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.adjacentNodes(null, aColumnKey, theDefaultEdgeValue) \
            expected to throw NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("nodeU");
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereColumnKeyIsNull_thenThrowsNpe() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(A_ROW_KEY, null, THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.adjacentNodes(aRowKey, null, theDefaultEdgeValue) \
            expected to throw NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("nodeV");
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereRowKeyNotInTable_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(A_KEY_NOT_IN_TABLE, A_COLUMN_KEY, THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.edgeValueOrDefault(aKeyNotInTable, aColumnKey, theDefaultEdgeValue) \
            expected to throw IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereColumnKeyNotInTable_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest =
        () -> graph.edgeValueOrDefault(A_ROW_KEY, A_KEY_NOT_IN_TABLE, THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.edgeValueOrDefault(aRowKey, keyNotInThisTable, theDefaultEdgeValue) \
            expected to throw IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfEdgeEndpoints_thenIsCellValue() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var result =
        graph.edgeValueOrDefault(
            EndpointPair.ordered(A_ROW_KEY, A_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    assertThat(result)
        .as(
            """
            graph.edgeValueOfDefault(endpoints(aRowKey, aColumnKey), theDefaultEdgeValue) \
            expected to return aCellValue\
            """)
        .isEqualTo(A_CELL_VALUE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfAbsentEndpointsEdge_thenIsDefaultEdgeValue() {

    var table =
        ImmutableTable.builder() //
            .put(A_ROW_KEY, A_COLUMN_KEY, A_CELL_VALUE)
            .put(ANOTHER_ROW_KEY, ANOTHER_COLUMN_KEY, A_CELL_VALUE)
            .build();
    var graph = MoreGraphs.asValueGraph(table);

    var result =
        graph.edgeValueOrDefault(
            EndpointPair.ordered(A_ROW_KEY, ANOTHER_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    assertThat(result)
        .as(
            """
            graph.edgeValueOrDefault(endpoints(aRowKey, unrelatedColumnKey), theDefaultEdgeValue) \
            expected to return theDefaultEdgeValue\
            """)
        .isEqualTo(THE_DEFAULT_EDGE_VALUE);
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfNullEndpoints_thenThrowsNpe() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest = () -> graph.edgeValueOrDefault(null, THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.adjacentNodes(nullEndpoints, theDefaultEdgeValue) \
            expected to throw NullPointerException\
            """)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("endpoints");
  }

  @Test
  void givenTableAsValueGraph_whenGettingEdgeValueOrDefaultOfUnorderedEndpoints_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest =
        () ->
            graph.edgeValueOrDefault(
                EndpointPair.unordered(A_ROW_KEY, A_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.adjacentNodes(unorderedEndpoints, theDefaultEdgeValue) \
            expected to throw IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ordered");
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereFirstEndpointNotInTable_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest =
        () ->
            graph.edgeValueOrDefault(
                EndpointPair.ordered(A_KEY_NOT_IN_TABLE, A_COLUMN_KEY), THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.adjacentNodes(endpoints(aKeyNotInTable, aColumnKey), theDefaultEdgeValue) \
            expected to throw IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void
      givenTableAsValueGraph_whenGettingEdgeValueOrDefaultWhereLastEndpointNotInTable_thenThrowsIae() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    @SuppressWarnings("ResultOfMethodCallIgnored") // edgeValueOrDefault should throw, not return
    ThrowingCallable codeUnderTest =
        () ->
            graph.edgeValueOrDefault(
                EndpointPair.ordered(A_ROW_KEY, A_KEY_NOT_IN_TABLE), THE_DEFAULT_EDGE_VALUE);

    assertThatCode(codeUnderTest)
        .as(
            """
            graph.adjacentNodes(endpoints(aRowKey, aKeyNotInTable), theDefaultEdgeValue) \
            expected to throw IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(A_KEY_NOT_IN_TABLE);
  }

  @Test
  void givenEmptyTableAsValueGraph_whenGettingEdges_thenNumElementsIsEqualToZero() {

    var table = emptyTable();
    var graph = MoreGraphs.asValueGraph(table);

    var edges = graph.edges();

    assertThat(edges).size().as("graph.edges().size() expected to be 0").isZero();
  }

  @Test
  void givenSingleCellTableAsValueGraph_whenGettingEdges_thenNumElementsIsEqualToOne() {

    var table = singleCellTable();
    var graph = MoreGraphs.asValueGraph(table);

    var edges = graph.edges();

    assertThat(edges).size().as("graph.edges().size() expected to be 1").isOne();
  }
}
