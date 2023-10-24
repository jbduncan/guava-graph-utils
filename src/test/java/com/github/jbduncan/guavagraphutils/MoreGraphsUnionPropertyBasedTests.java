package com.github.jbduncan.guavagraphutils;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.jbduncan.guavagraphutils.MoreArbitraries.TwoGraphs;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Group;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

@SuppressWarnings({
  // We test methods that purposefully use an unstable Guava API
  "UnstableApiUsage",
  // The inner classes annotated with @Group are purposefully non-static to work with jqwik
  "ClassCanBeStatic"
})
class MoreGraphsUnionPropertyBasedTests {
  @Group
  class Instantiation {
    @Property
    void givenNullFirstGraph_whenCalculatingUnion_thenItThrowsNpe(
        // given
        @ForAll("graphs") Graph<Integer> second) {
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
        @ForAll("graphs") Graph<Integer> first) {
      // when
      ThrowingCallable codeUnderTest = () -> MoreGraphs.union(first, null);

      // then
      assertThatCode(codeUnderTest)
          .as("MoreGraphs.union(first, null) expected to throw NullPointerException")
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("second");
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
    void givenTwoGraphsWithDifferentIncidentEdgeOrder_whenCalculatingUnion_thenIaeIsThrown(
        // given
        @ForAll("twoGraphsWithDifferentIncidentEdgeOrder") TwoGraphs graphs) {

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
          .hasMessage("Graph.incidentEdgeOrder() is not consistent for both graphs");
    }
  }

  @Group
  class Nodes {
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
  }

  @Group
  class IsDirected {
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
  }

  @Group
  class AllowsSelfLoops {
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
  }

  @Group
  class NodeOrder {
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
  }

  @Group
  class AdjacentNodes {
    @Property
    void givenTwoGraphsAndNodeAbsentFromBoth_whenCalculatingUnionAdjNodes_thenThrowIae(
        // given
        @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs, @ForAll Integer node) {
      Assume.that(
          !graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

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

    // TODO: Make this test faster by making the two graphs have disjointed nodes at the source.
    @Property
    void givenTwoGraphsAndNodeFromFirst_whenCalculatingUnionAdjNodes_thenReturnAdjNodesOfFirst(
        // given
        @ForAll("twoGraphsWithSameFlagsAndNodeFromFirst") TwoGraphsAndNode twoGraphsAndNode) {
      var firstGraph = twoGraphsAndNode.firstGraph();
      var secondGraph = twoGraphsAndNode.secondGraph();
      var node = twoGraphsAndNode.node();

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.adjacentNodes(node))
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(node) expected to \
              be equal to first.adjacentNodes(node)\
              """)
          .isEqualTo(firstGraph.adjacentNodes(node));
    }

    // TODO: Make this test faster by making the two graphs have disjointed nodes at the source.
    @Property
    void givenTwoGraphsAndNodeFromSecond_whenCalculatingUnionAdjNodes_thenReturnAdjNodesOfSecond(
        // given
        @ForAll("twoGraphsWithSameFlagsAndNodeFromSecond") TwoGraphsAndNode twoGraphsAndNode) {
      var firstGraph = twoGraphsAndNode.firstGraph();
      var secondGraph = twoGraphsAndNode.secondGraph();
      var node = twoGraphsAndNode.node();

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.adjacentNodes(node))
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(node) expected to \
              be equal to second.adjacentNodes(node)\
              """)
          .isEqualTo(secondGraph.adjacentNodes(node));
    }

    @Property
    void givenTwoGraphsAndNode_whenCalculatingUnionAdjNodes_thenReturnUnionOfAdjNodesOfBoth(
        // given
        @ForAll("twoGraphsWithSameFlags") TwoGraphs twoGraphs) {
      var firstGraph = twoGraphs.first();
      var secondGraph = twoGraphs.second();

      var commonNode =
          Sets.intersection(firstGraph.nodes(), secondGraph.nodes()).stream().findFirst();
      Assume.that(commonNode.isPresent());

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.adjacentNodes(commonNode.get()))
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(node) expected to \
              be equal to union of first.adjacentNodes(node) and \
              second.adjacentNodes(node)\
              """)
          .isEqualTo(
              Sets.union(
                  firstGraph.adjacentNodes(commonNode.get()),
                  secondGraph.adjacentNodes(commonNode.get())));
    }

    @Property
    void
        givenTwoGraphsAndNodeFromFirst_whenGettingUnionAdjNodes_andSecondGraphMutated_thenReturnAdjNodesOfBoth(
            // given
            @ForAll("graphAndNodePairs") GraphAndNode firstGraphAndNodeU, @ForAll Integer nodeV) {
      var firstGraph = firstGraphAndNodeU.graph();
      Assume.that(!firstGraph.edges().isEmpty());
      Assume.that(!firstGraph.nodes().contains(nodeV));
      var secondGraph = GraphBuilder.from(firstGraph).build();
      var nodeU = firstGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // when
      var adjacentNodes = union.adjacentNodes(nodeU);
      var expected = ImmutableSet.builder().addAll(adjacentNodes).add(nodeV).build();

      // and
      secondGraph.putEdge(nodeU, nodeV);

      // then
      assertThat(adjacentNodes)
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(node) expected to \
              be equal to union of first.adjacentNodes(node) and \
              second.adjacentNodes(node)\
              """)
          .isEqualTo(expected);
    }

    @Property
    void
        givenTwoGraphsAndNodeFromSecond_whenGettingUnionAdjNodes_andFirstGraphMutated_thenReturnAdjNodesOfBoth(
            // given
            @ForAll("graphAndNodePairs") GraphAndNode secondGraphAndNodeU, @ForAll Integer nodeV) {
      var secondGraph = secondGraphAndNodeU.graph();
      Assume.that(!secondGraph.edges().isEmpty());
      Assume.that(!secondGraph.nodes().contains(nodeV));
      var firstGraph = GraphBuilder.from(secondGraph).build();
      var nodeU = secondGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // when
      var adjacentNodes = union.adjacentNodes(nodeU);
      var expected = ImmutableSet.builder().addAll(adjacentNodes).add(nodeV).build();

      // and
      firstGraph.putEdge(nodeU, nodeV);

      // then
      assertThat(adjacentNodes)
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(node) expected to \
              be equal to union of first.adjacentNodes(node) and \
              second.adjacentNodes(node)\
              """)
          .isEqualTo(expected);
    }
  }

  @Group
  class Predecessors {
    @Property
    void givenTwoGraphsAndNodeAbsentFromBoth_whenCalculatingUnionPredNodes_thenThrowIae(
        // given
        @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs, @ForAll Integer node) {
      Assume.that(
          !graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

      // when
      var union = MoreGraphs.union(graphs.first(), graphs.second());
      ThrowingCallable codeUnderTest = () -> union.predecessors(node);

      // then
      assertThatCode(codeUnderTest)
          .as(
              """
              MoreGraphs.union(first, second).predecessors(absentNode) \
              expected to throw IllegalArgumentException\
              """)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Node %s is not an element of this graph.", node);
    }

    // TODO: Make this test faster by making the two graphs have disjointed nodes at the source.
    @Property
    void givenTwoGraphsAndNodeFromFirst_whenCalculatingUnionPredNodes_thenReturnPredNodesOfFirst(
        // given
        @ForAll("twoGraphsWithSameFlagsAndNodeFromFirst") TwoGraphsAndNode twoGraphsAndNode) {
      var firstGraph = twoGraphsAndNode.firstGraph();
      var secondGraph = twoGraphsAndNode.secondGraph();
      var node = twoGraphsAndNode.node();

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.predecessors(node))
          .as(
              """
              MoreGraphs.union(first, second).predecessors(node) expected to \
              be equal to first.predecessors(node)\
              """)
          .isEqualTo(firstGraph.predecessors(node));
    }

    // TODO: Make this test faster by making the two graphs have disjointed nodes at the source.
    @Property
    void givenTwoGraphsAndNodeFromSecond_whenCalculatingUnionPredNodes_thenReturnPredNodesOfSecond(
        // given
        @ForAll("twoGraphsWithSameFlagsAndNodeFromSecond") TwoGraphsAndNode twoGraphsAndNode) {
      var firstGraph = twoGraphsAndNode.firstGraph();
      var secondGraph = twoGraphsAndNode.secondGraph();
      var node = twoGraphsAndNode.node();

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.predecessors(node))
          .as(
              """
              MoreGraphs.union(first, second).predecessors(node) expected to \
              be equal to second.predecessors(node)\
              """)
          .isEqualTo(secondGraph.predecessors(node));
    }

    @Property
    void givenTwoGraphsAndNode_whenCalculatingUnionPredNodes_thenReturnUnionOfPredNodesOfBoth(
        // given
        @ForAll("twoGraphsWithSameFlags") TwoGraphs twoGraphs) {
      var firstGraph = twoGraphs.first();
      var secondGraph = twoGraphs.second();

      var commonNode =
          Sets.intersection(firstGraph.nodes(), secondGraph.nodes()).stream().findFirst();
      Assume.that(commonNode.isPresent());

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.predecessors(commonNode.get()))
          .as(
              """
              MoreGraphs.union(first, second).predecessors(node) expected to \
              be equal to union of first.predecessors(node) and \
              second.predecessors(node)\
              """)
          .isEqualTo(
              Sets.union(
                  firstGraph.predecessors(commonNode.get()),
                  secondGraph.predecessors(commonNode.get())));
    }

    @Property
    void
        givenTwoGraphsAndNodeFromFirst_whenGettingUnionPredNodes_andSecondGraphMutated_thenReturnPredNodesOfBoth(
            // given
            @ForAll("graphAndNodePairs") GraphAndNode firstGraphAndNodeV, @ForAll Integer nodeU) {
      var firstGraph = firstGraphAndNodeV.graph();
      Assume.that(!firstGraph.edges().isEmpty());
      Assume.that(!firstGraph.nodes().contains(nodeU));
      var secondGraph = GraphBuilder.from(firstGraph).build();
      var nodeV = firstGraphAndNodeV.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // when
      var predecessors = union.predecessors(nodeV);
      var expected = ImmutableSet.builder().addAll(predecessors).add(nodeU).build();

      // and
      secondGraph.putEdge(nodeU, nodeV);

      // then
      assertThat(predecessors)
          .as(
              """
              MoreGraphs.union(first, second).predecessors(node) expected to \
              be equal to union of first.predecessors(node) and \
              second.predecessors(node)\
              """)
          .isEqualTo(expected);
    }

    @Property
    void
        givenTwoGraphsAndNodeFromSecond_whenGettingUnionPredNodes_andFirstGraphMutated_thenReturnPredNodesOfBoth(
            // given
            @ForAll("graphAndNodePairs") GraphAndNode secondGraphAndNodeV, @ForAll Integer nodeU) {
      var secondGraph = secondGraphAndNodeV.graph();
      Assume.that(!secondGraph.edges().isEmpty());
      Assume.that(!secondGraph.nodes().contains(nodeU));
      var firstGraph = GraphBuilder.from(secondGraph).build();
      var nodeV = secondGraphAndNodeV.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // when
      var predecessors = union.predecessors(nodeV);
      var expected = ImmutableSet.builder().addAll(predecessors).add(nodeU).build();

      // and
      firstGraph.putEdge(nodeU, nodeV);

      // then
      assertThat(predecessors)
          .as(
              """
              MoreGraphs.union(first, second).predecessors(node) expected to \
              be equal to union of first.predecessors(node) and \
              second.predecessors(node)\
              """)
          .isEqualTo(expected);
    }
  }

  @Group
  class Successors {
    @Property
    void givenTwoGraphsAndNodeAbsentFromBoth_whenCalculatingUnionSuccNodes_thenThrowIae(
        // given
        @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs, @ForAll Integer node) {
      Assume.that(
          !graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

      // when
      var union = MoreGraphs.union(graphs.first(), graphs.second());
      ThrowingCallable codeUnderTest = () -> union.successors(node);

      // then
      assertThatCode(codeUnderTest)
          .as(
              """
              MoreGraphs.union(first, second).successors(absentNode) \
              expected to throw IllegalArgumentException\
              """)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Node %s is not an element of this graph.", node);
    }

    // TODO: Make this test faster by making the two graphs have disjointed nodes at the source.
    @Property
    void givenTwoGraphsAndNodeFromFirst_whenCalculatingUnionSuccNodes_thenReturnSuccNodesOfFirst(
        // given
        @ForAll("twoGraphsWithSameFlagsAndNodeFromFirst") TwoGraphsAndNode twoGraphsAndNode) {
      var firstGraph = twoGraphsAndNode.firstGraph();
      var secondGraph = twoGraphsAndNode.secondGraph();
      var node = twoGraphsAndNode.node();

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.successors(node))
          .as(
              """
              MoreGraphs.union(first, second).successors(node) expected to \
              be equal to first.successors(node)\
              """)
          .isEqualTo(firstGraph.successors(node));
    }

    // TODO: Make this test faster by making the two graphs have disjointed nodes at the source.
    @Property
    void givenTwoGraphsAndNodeFromSecond_whenCalculatingUnionSuccNodes_thenReturnSuccNodesOfSecond(
        // given
        @ForAll("twoGraphsWithSameFlagsAndNodeFromSecond") TwoGraphsAndNode twoGraphsAndNode) {
      var firstGraph = twoGraphsAndNode.firstGraph();
      var secondGraph = twoGraphsAndNode.secondGraph();
      var node = twoGraphsAndNode.node();

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.successors(node))
          .as(
              """
              MoreGraphs.union(first, second).successors(node) expected to \
              be equal to second.successors(node)\
              """)
          .isEqualTo(secondGraph.successors(node));
    }

    @Property
    void givenTwoGraphsAndNode_whenCalculatingUnionSuccNodes_thenReturnUnionOfSuccNodesOfBoth(
        // given
        @ForAll("twoGraphsWithSameFlags") TwoGraphs twoGraphs) {
      var firstGraph = twoGraphs.first();
      var secondGraph = twoGraphs.second();

      var commonNode =
          Sets.intersection(firstGraph.nodes(), secondGraph.nodes()).stream().findFirst();
      Assume.that(commonNode.isPresent());

      // when
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // then
      assertThat(union.successors(commonNode.get()))
          .as(
              """
              MoreGraphs.union(first, second).successors(node) expected to \
              be equal to union of first.successors(node) and \
              second.successors(node)\
              """)
          .isEqualTo(
              Sets.union(
                  firstGraph.successors(commonNode.get()),
                  secondGraph.successors(commonNode.get())));
    }

    @Property
    void
        givenTwoGraphsAndNodeFromFirst_whenGettingUnionSuccNodes_andSecondGraphMutated_thenReturnSuccNodesOfBoth(
            // given
            @ForAll("graphAndNodePairs") GraphAndNode firstGraphAndNodeU, @ForAll Integer nodeV) {
      var firstGraph = firstGraphAndNodeU.graph();
      Assume.that(!firstGraph.edges().isEmpty());
      Assume.that(!firstGraph.nodes().contains(nodeV));
      var secondGraph = GraphBuilder.from(firstGraph).build();
      var nodeU = firstGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // when
      var successors = union.successors(nodeU);
      var expected = ImmutableSet.builder().addAll(successors).add(nodeV).build();

      // and
      secondGraph.putEdge(nodeU, nodeV);

      // then
      assertThat(successors)
          .as(
              """
              MoreGraphs.union(first, second).successors(node) expected to \
              be equal to union of first.successors(node) and \
              second.successors(node)\
              """)
          .isEqualTo(expected);
    }

    @Property
    void
        givenTwoGraphsAndNodeFromSecond_whenGettingUnionSuccNodes_andFirstGraphMutated_thenReturnSuccNodesOfBoth(
            // given
            @ForAll("graphAndNodePairs") GraphAndNode secondGraphAndNodeU, @ForAll Integer nodeV) {
      var secondGraph = secondGraphAndNodeU.graph();
      Assume.that(!secondGraph.edges().isEmpty());
      Assume.that(!secondGraph.nodes().contains(nodeV));
      var firstGraph = GraphBuilder.from(secondGraph).build();
      var nodeU = secondGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      // when
      var successors = union.successors(nodeU);
      var expected = ImmutableSet.builder().addAll(successors).add(nodeV).build();

      // and
      firstGraph.putEdge(nodeU, nodeV);

      // then
      assertThat(successors)
          .as(
              """
              MoreGraphs.union(first, second).successors(node) expected to \
              be equal to union of first.successors(node) and \
              second.successors(node)\
              """)
          .isEqualTo(expected);
    }
  }

  @Group
  class IncidentEdgeOrder {
    @Property
    void givenTwoGraphsWithSameIncidentEdgeOrder_whenCalculatingUnion_thenReturnCommonNodeOrder(
        // given
        @ForAll("twoGraphsWithSameFlags") TwoGraphs graphs) {
      // when
      var union = MoreGraphs.union(graphs.first(), graphs.second());

      // then
      assertThat(union.incidentEdgeOrder())
          .as(
              """
              MoreGraphs.union(first, second).incidentEdgeOrder() expected \
              to be the same as both first's and second's .incidentEdgeOrder()\
              """)
          .isEqualTo(graphs.first().incidentEdgeOrder());
    }
  }

  @Provide
  Arbitrary<Graph<Integer>> graphs() {
    return MoreArbitraries.graphs();
  }

  record GraphAndNode(Graph<Integer> graph, Integer node) {}

  @Provide
  Arbitrary<GraphAndNode> graphAndNodePairs() {
    return MoreArbitraries.graphs()
        .filter(not(graph -> graph.nodes().isEmpty()))
        .flatMap(
            graph ->
                Combinators.combine(Arbitraries.just(graph), Arbitraries.of(graph.nodes()))
                    .as(GraphAndNode::new));
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
            arbitraryIsDirected,
            arbitraryAllowsSelfLoops,
            MoreArbitraries.nodeOrders(),
            MoreArbitraries.incidentEdgeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodeOrder, incidentEdgeOrder) ->
                twoGraphs(
                    isDirected,
                    !isDirected,
                    allowsSelfLoops,
                    allowsSelfLoops,
                    nodeOrder,
                    nodeOrder,
                    incidentEdgeOrder,
                    incidentEdgeOrder));
  }

  @Provide
  Arbitrary<TwoGraphs> twoGraphsWithDifferentAllowsSelfLoops() {
    var arbitraryIsDirected = Arbitraries.of(true, false);
    var arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected,
            arbitraryAllowsSelfLoops,
            MoreArbitraries.nodeOrders(),
            MoreArbitraries.incidentEdgeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodeOrder, incidentEdgeOrder) ->
                twoGraphs(
                    isDirected,
                    isDirected,
                    allowsSelfLoops,
                    !allowsSelfLoops,
                    nodeOrder,
                    nodeOrder,
                    incidentEdgeOrder,
                    incidentEdgeOrder));
  }

  @Provide
  Arbitrary<TwoGraphs> twoGraphsWithDifferentNodeOrder() {
    var arbitraryIsDirected = Arbitraries.of(true, false);
    var arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected,
            arbitraryAllowsSelfLoops,
            MoreArbitraries.twoDifferentNodeOrders(),
            MoreArbitraries.incidentEdgeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, twoDifferentNodeOrders, incidentEdgeOrder) ->
                twoGraphs(
                    isDirected,
                    isDirected,
                    allowsSelfLoops,
                    allowsSelfLoops,
                    twoDifferentNodeOrders.first(),
                    twoDifferentNodeOrders.second(),
                    incidentEdgeOrder,
                    incidentEdgeOrder));
  }

  @Provide
  Arbitrary<TwoGraphs> twoGraphsWithDifferentIncidentEdgeOrder() {
    var arbitraryIsDirected = Arbitraries.of(true, false);
    var arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected,
            arbitraryAllowsSelfLoops,
            MoreArbitraries.nodeOrders(),
            MoreArbitraries.twoDifferentIncidentEdgeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodeOrder, twoDifferentIncidentEdgeOrders) ->
                twoGraphs(
                    isDirected,
                    isDirected,
                    allowsSelfLoops,
                    allowsSelfLoops,
                    nodeOrder,
                    nodeOrder,
                    twoDifferentIncidentEdgeOrders.first(),
                    twoDifferentIncidentEdgeOrders.second()));
  }

  record TwoGraphsAndNode(
      MutableGraph<Integer> firstGraph, MutableGraph<Integer> secondGraph, Integer node) {}

  @Provide
  Arbitrary<TwoGraphsAndNode> twoGraphsWithSameFlagsAndNodeFromFirst() {
    return MoreArbitraries.twoGraphsWithSameFlagsAndDisjointedNodes()
        .filter(not(twoGraphs -> twoGraphs.first().nodes().isEmpty()))
        .flatMap(
            twoGraphs ->
                Arbitraries.of(twoGraphs.first().nodes())
                    .map(
                        node -> {
                          MutableGraph<Integer> first = Graphs.copyOf(twoGraphs.first());
                          MutableGraph<Integer> second = Graphs.copyOf(twoGraphs.second());
                          return new TwoGraphsAndNode(first, second, node);
                        }));
  }

  @Provide
  Arbitrary<TwoGraphsAndNode> twoGraphsWithSameFlagsAndNodeFromSecond() {
    return MoreArbitraries.twoGraphsWithSameFlagsAndDisjointedNodes()
        .filter(not(twoGraphs -> twoGraphs.second().nodes().isEmpty()))
        .flatMap(
            twoGraphs ->
                Arbitraries.of(twoGraphs.second().nodes())
                    .map(
                        node -> {
                          MutableGraph<Integer> first = Graphs.copyOf(twoGraphs.first());
                          MutableGraph<Integer> second = Graphs.copyOf(twoGraphs.second());
                          return new TwoGraphsAndNode(first, second, node);
                        }));
  }

  // TODO: Move to MoreArbitraries and refactor
  private static Arbitrary<TwoGraphs> twoGraphs(
      boolean isDirectedA,
      boolean isDirectedB,
      boolean allowsSelfLoopsA,
      boolean allowsSelfLoopsB,
      ElementOrder<Integer> nodeOrderA,
      ElementOrder<Integer> nodeOrderB,
      ElementOrder<Integer> incidentEdgeOrderA,
      ElementOrder<Integer> incidentEdgeOrderB) {
    var firstGraph =
        MoreArbitraries.graphs(
            Arbitraries.just(isDirectedA),
            Arbitraries.just(allowsSelfLoopsA),
            MoreArbitraries.nodes(),
            Arbitraries.just(nodeOrderA),
            Arbitraries.just(incidentEdgeOrderA));
    var secondGraph =
        MoreArbitraries.graphs(
            Arbitraries.just(isDirectedB),
            Arbitraries.just(allowsSelfLoopsB),
            MoreArbitraries.nodes(),
            Arbitraries.just(nodeOrderB),
            Arbitraries.just(incidentEdgeOrderB));
    return Combinators.combine(firstGraph, secondGraph).as(TwoGraphs::new);
  }
}
