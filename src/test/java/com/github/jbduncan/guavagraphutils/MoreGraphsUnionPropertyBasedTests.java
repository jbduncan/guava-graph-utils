package com.github.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.jbduncan.guavagraphutils.MoreArbitraries.TwoGraphs;
import com.google.common.collect.Sets;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.ImmutableGraph;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.Disabled;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

// We test methods that purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreGraphsUnionPropertyBasedTests {
  @Property
  void givenNullFirstGraph_whenCalculatingUnion_thenItThrowsNpe(
      // given
      @ForAll("graphs") ImmutableGraph<Integer> second) {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.union(null, second);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.union(null, second) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("first");
  }

  @Property
  void givenNullSecondGraph_whenCalculatingUnion_thenItThrowsNpe(
      // given
      @ForAll("graphs") ImmutableGraph<Integer> first) {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.union(first, null);

    // then
    assertThatCode(codeUnderTest)
        .as("MoreGraphs.union(first, null) expected to throw NullPointerException")
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("second");
  }

  @Property
  void givenTwoGraphs_whenCalculatingUnionNodes_thenReturnUnionOfBothGraphsNodes(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs) {

    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThat(union.nodes())
        .as(
            """
            MoreGraphs.union(first, second).nodes() expected to be union \
            of first's and second's nodes\
            """)
        .isEqualTo(Sets.union(graphs.first().nodes(), graphs.second().nodes()));
  }

  @Property
  void givenTwoGraphsWithDifferentIsDirected_whenCalculatingUnion_thenIaeIsThrown(
      // given
      @ForAll("twoGraphsWithDifferentIsDirected") TwoGraphs graphs) {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.union(first, second) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Graph.isDirected() is not consistent for both graphs");
  }

  @Property
  void givenTwoGraphsWithSameIsDirected_whenCalculatingUnion_thenReturnCommonIsDirected(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs) {
    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThat(union.isDirected())
        .as(
            """
            MoreGraphs.union(first, second).isDirected() expected \
            to be the same as both first's and second's .isDirected()\
            """)
        .isEqualTo(graphs.first().isDirected());
  }

  @Property
  void givenTwoGraphsWithDifferentAllowsSelfLoops_whenCalculatingUnion_thenIaeIsThrown(
      // given
      @ForAll("twoGraphsWithDifferentAllowsSelfLoops") TwoGraphs graphs) {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.union(first, second) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Graph.allowsSelfLoops() is not consistent for both graphs");
  }

  @Property
  void givenTwoGraphsWithSameAllowsSelfLoops_whenCalculatingUnion_thenReturnCommonAllowsSelfLoops(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs) {
    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThat(union.allowsSelfLoops())
        .as(
            """
            MoreGraphs.union(first, second).allowsSelfLoops() expected \
            to be the same as both first's and second's .allowsSelfLoops()\
            """)
        .isEqualTo(graphs.first().allowsSelfLoops());
  }

  @Property
  void givenTwoGraphsWithDifferentNodeOrder_whenCalculatingUnion_thenIaeIsThrown(
      // given
      @ForAll("twoGraphsWithDifferentNodeOrder") TwoGraphs graphs) {
    // when
    ThrowingCallable codeUnderTest = () -> MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.union(first, second) expected to throw \
            IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Graph.nodeOrder() is not consistent for both graphs");
  }

  @Property
  void givenTwoGraphsWithSameNodeOrder_whenCalculatingUnion_thenReturnCommonNodeOrder(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs) {
    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThat(union.nodeOrder())
        .as(
            """
            MoreGraphs.union(first, second).nodeOrder() expected \
            to be the same as both first's and second's .nodeOrder()\
            """)
        .isEqualTo(graphs.first().nodeOrder());
  }

  @Property
  void givenTwoGraphsAndNodeAbsentFromBoth_whenCalculatingUnionAdjNodes_thenThrowIae(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs, @ForAll Integer node) {
    Assume.that(!graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());
    ThrowingCallable codeUnderTest = () -> union.adjacentNodes(node);

    // then
    assertThatCode(codeUnderTest)
        .as(
            """
            MoreGraphs.union(first, second).adjacentNodes(absentNode) \
            expected to throw IllegalArgumentException\
            """)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Node %s is not an element of this graph.", node);
  }

  @Property
  @Disabled // TODO: Fix exhausted generation problem and re-enable
  void givenTwoGraphsAndNodePresentInFirst_whenCalculatingUnionAdjNodes_thenReturnAdjNodesOfFirst(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs, @ForAll Integer node) {
    Assume.that(graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThat(union.adjacentNodes(node))
        .as(
            """
            MoreGraphs.union(first, second).adjacentNodes(node) expected to \
            be equal to first.adjacentNodes(node)\
            """)
        .isEqualTo(graphs.first().adjacentNodes(node));
  }

  @Property
  @Disabled // TODO: Fix exhausted generation problem and re-enable
  void givenTwoGraphsAndNodePresentInSecond_whenCalculatingUnionAdjNodes_thenReturnAdjNodesOfSecond(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs, @ForAll Integer node) {
    Assume.that(!graphs.first().nodes().contains(node) && graphs.second().nodes().contains(node));

    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThat(union.adjacentNodes(node))
        .as(
            """
            MoreGraphs.union(first, second).adjacentNodes(node) expected to \
            be equal to second.adjacentNodes(node)\
            """)
        .isEqualTo(graphs.second().adjacentNodes(node));
  }

  @Property
  @Disabled // TODO: Fix exhausted generation problem and re-enable
  void givenTwoGraphsAndNode_whenCalculatingUnionAdjNodes_thenReturnUnionOfAdjNodesOfBoth(
      // given
      @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs, @ForAll Integer node) {
    Assume.that(graphs.first().nodes().contains(node) && graphs.second().nodes().contains(node));

    // when
    var union = MoreGraphs.union(graphs.first(), graphs.second());

    // then
    assertThat(union.adjacentNodes(node))
        .as(
            """
            MoreGraphs.union(first, second).adjacentNodes(node) expected to \
            be equal to union of first.adjacentNodes(node) and \
            second.adjacentNodes(node)\
            """)
        .isEqualTo(
            Sets.union(graphs.first().adjacentNodes(node), graphs.second().adjacentNodes(node)));
  }

  @Provide
  Arbitrary<ImmutableGraph<Integer>> graphs() {
    return MoreArbitraries.graphs();
  }

  @Provide
  Arbitrary<TwoGraphs> twoGraphsWithSameFlags() {
    return MoreArbitraries.twoGraphsWithSameFlags();
  }

  @Provide
  Arbitrary<TwoGraphs> twoGraphsWithDifferentIsDirected() {
    var arbitraryIsDirected = Arbitraries.of(true, false);
    var arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected, arbitraryAllowsSelfLoops, MoreArbitraries.nodeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodeOrder) ->
                twoGraphs(
                    isDirected,
                    !isDirected,
                    allowsSelfLoops,
                    allowsSelfLoops,
                    nodeOrder,
                    nodeOrder));
  }

  @Provide
  Arbitrary<TwoGraphs> twoGraphsWithDifferentAllowsSelfLoops() {
    var arbitraryIsDirected = Arbitraries.of(true, false);
    var arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected, arbitraryAllowsSelfLoops, MoreArbitraries.nodeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodeOrder) ->
                twoGraphs(
                    isDirected,
                    isDirected,
                    allowsSelfLoops,
                    !allowsSelfLoops,
                    nodeOrder,
                    nodeOrder));
  }

  @Provide
  Arbitrary<TwoGraphs> twoGraphsWithDifferentNodeOrder() {
    var arbitraryIsDirected = Arbitraries.of(true, false);
    var arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected, arbitraryAllowsSelfLoops, MoreArbitraries.twoDifferentNodeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, twoDifferentNodeOrders) ->
                twoGraphs(
                    isDirected,
                    isDirected,
                    allowsSelfLoops,
                    allowsSelfLoops,
                    twoDifferentNodeOrders.first(),
                    twoDifferentNodeOrders.second()));
  }

  private static Arbitrary<TwoGraphs> twoGraphs(
      boolean isDirectedA,
      boolean isDirectedB,
      boolean allowsSelfLoopsA,
      boolean allowsSelfLoopsB,
      ElementOrder<Integer> nodeOrderA,
      ElementOrder<Integer> nodeOrderB) {
    var firstGraph =
        MoreArbitraries.graphs(
            Arbitraries.just(isDirectedA),
            Arbitraries.just(allowsSelfLoopsA),
            Arbitraries.just(nodeOrderA));
    var secondGraph =
        MoreArbitraries.graphs(
            Arbitraries.just(isDirectedB),
            Arbitraries.just(allowsSelfLoopsB),
            Arbitraries.just(nodeOrderB));
    return Combinators.combine(firstGraph, secondGraph).as(TwoGraphs::new);
  }
}
