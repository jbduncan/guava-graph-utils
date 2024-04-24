package com.github.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.jbduncan.guavagraphutils.MoreArbitraries.GraphAndNode;
import com.github.jbduncan.guavagraphutils.MoreArbitraries.TwoGraphs;
import com.github.jbduncan.guavagraphutils.MoreArbitraries.TwoMutableGraphsAndNode;
import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import java.util.Set;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Group;
import net.jqwik.api.Property;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

@SuppressWarnings({
  // We test methods that purposefully use an unstable Guava API
  "UnstableApiUsage",
  // jqwik's @Group is equivalent to JUnit 5's @Nested, so they can be safely ignored
  "ClassCanBeStatic"
})
class MoreGraphsUnionPropertyBasedTests {
  @Group
  class Instantiation {
    @Property
    void givenNullFirstGraph_whenCalculatingUnion_thenItThrowsNpe(
        @ForAll(supplier = MoreArbitraries.Graphs.class) Graph<Integer> second) {
      ThrowingCallable codeUnderTest = () -> MoreGraphs.union(null, second);

      assertThatCode(codeUnderTest)
          .as("MoreGraphs.union(null, second) expected to throw NullPointerException")
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("first");
    }

    @Property
    void givenNullSecondGraph_whenCalculatingUnion_thenItThrowsNpe(
        @ForAll(supplier = MoreArbitraries.Graphs.class) Graph<Integer> first) {
      ThrowingCallable codeUnderTest = () -> MoreGraphs.union(first, null);

      assertThatCode(codeUnderTest)
          .as("MoreGraphs.union(first, null) expected to throw NullPointerException")
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("second");
    }

    @Property
    void givenTwoGraphsWithDifferentIsDirected_whenCalculatingUnion_thenIaeIsThrown(
        @ForAll(supplier = TwoGraphsWithDifferentIsDirected.class) TwoGraphs graphs) {
      ThrowingCallable codeUnderTest = () -> MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = TwoGraphsWithDifferentAllowsSelfLoops.class) TwoGraphs graphs) {

      ThrowingCallable codeUnderTest = () -> MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = TwoGraphsWithDifferentNodeOrder.class) TwoGraphs graphs) {

      ThrowingCallable codeUnderTest = () -> MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = TwoGraphsWithDifferentIncidentEdgeOrder.class) TwoGraphs graphs) {

      ThrowingCallable codeUnderTest = () -> MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs) {

      var union = MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs) {

      var union = MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs) {

      var union = MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs) {

      var union = MoreGraphs.union(graphs.first(), graphs.second());

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs,
        @ForAll Integer node) {
      Assume.that(
          !graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

      var union = MoreGraphs.union(graphs.first(), graphs.second());
      ThrowingCallable codeUnderTest = () -> union.adjacentNodes(node);

      assertThatCode(codeUnderTest)
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(absentNode) \
              expected to throw IllegalArgumentException\
              """)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Node '%s' is not in this graph", node);
    }

    @Property
    void givenTwoGraphsAndNodeFromFirst_whenCalculatingUnionAdjNodes_thenReturnAdjNodesOfFirst(
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndNodeFromFirst.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndNode) {
      var firstGraph = twoMutableGraphsAndNode.firstGraph();
      var secondGraph = twoMutableGraphsAndNode.secondGraph();
      var node = twoMutableGraphsAndNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

      assertThat(union.adjacentNodes(node))
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(node) expected to \
              be equal to first.adjacentNodes(node)\
              """)
          .isEqualTo(firstGraph.adjacentNodes(node));
    }

    @Property
    void givenTwoGraphsAndNodeFromSecond_whenCalculatingUnionAdjNodes_thenReturnAdjNodesOfSecond(
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndNodeFromSecond.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndNode) {
      var firstGraph = twoMutableGraphsAndNode.firstGraph();
      var secondGraph = twoMutableGraphsAndNode.secondGraph();
      var node = twoMutableGraphsAndNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

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
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndCommonNode.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndCommonNode) {
      var firstGraph = twoMutableGraphsAndCommonNode.firstGraph();
      var secondGraph = twoMutableGraphsAndCommonNode.secondGraph();
      var commonNode = twoMutableGraphsAndCommonNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

      assertThat(union.adjacentNodes(commonNode))
          .as(
              """
              MoreGraphs.union(first, second).adjacentNodes(node) expected to \
              be equal to union of first.adjacentNodes(node) and \
              second.adjacentNodes(node)\
              """)
          .isEqualTo(
              Sets.union(
                  firstGraph.adjacentNodes(commonNode), secondGraph.adjacentNodes(commonNode)));
    }

    @Property
    void
        givenTwoGraphsAndNodeFromFirst_whenGettingUnionAdjNodes_andSecondGraphMutated_thenReturnAdjNodesOfBoth(
            @ForAll(supplier = MoreArbitraries.GraphsAndGraphNodes.class)
                GraphAndNode firstGraphAndNodeU,
            @ForAll Integer nodeV) {
      var firstGraph = firstGraphAndNodeU.graph();
      Assume.that(!firstGraph.edges().isEmpty());
      Assume.that(!firstGraph.nodes().contains(nodeV));
      var secondGraph = GraphBuilder.from(firstGraph).build();
      var nodeU = firstGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      var adjacentNodes = union.adjacentNodes(nodeU);
      var expected = Sets.union(adjacentNodes, Set.of(nodeV)).immutableCopy();

      secondGraph.putEdge(nodeU, nodeV);

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
            @ForAll(supplier = MoreArbitraries.GraphsAndGraphNodes.class)
                GraphAndNode secondGraphAndNodeU,
            @ForAll Integer nodeV) {
      var secondGraph = secondGraphAndNodeU.graph();
      Assume.that(!secondGraph.edges().isEmpty());
      Assume.that(!secondGraph.nodes().contains(nodeV));
      var firstGraph = GraphBuilder.from(secondGraph).build();
      var nodeU = secondGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      var adjacentNodes = union.adjacentNodes(nodeU);
      var expected = Sets.union(adjacentNodes, Set.of(nodeV)).immutableCopy();

      firstGraph.putEdge(nodeU, nodeV);

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs,
        @ForAll Integer node) {
      Assume.that(
          !graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

      var union = MoreGraphs.union(graphs.first(), graphs.second());
      ThrowingCallable codeUnderTest = () -> union.predecessors(node);

      assertThatCode(codeUnderTest)
          .as(
              """
              MoreGraphs.union(first, second).predecessors(absentNode) \
              expected to throw IllegalArgumentException\
              """)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Node '%s' is not in this graph", node);
    }

    @Property
    void givenTwoGraphsAndNodeFromFirst_whenCalculatingUnionPredNodes_thenReturnPredNodesOfFirst(
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndNodeFromFirst.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndNode) {
      var firstGraph = twoMutableGraphsAndNode.firstGraph();
      var secondGraph = twoMutableGraphsAndNode.secondGraph();
      var node = twoMutableGraphsAndNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

      assertThat(union.predecessors(node))
          .as(
              """
              MoreGraphs.union(first, second).predecessors(node) expected to \
              be equal to first.predecessors(node)\
              """)
          .isEqualTo(firstGraph.predecessors(node));
    }

    @Property
    void givenTwoGraphsAndNodeFromSecond_whenCalculatingUnionPredNodes_thenReturnPredNodesOfSecond(
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndNodeFromSecond.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndNode) {
      var firstGraph = twoMutableGraphsAndNode.firstGraph();
      var secondGraph = twoMutableGraphsAndNode.secondGraph();
      var node = twoMutableGraphsAndNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

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
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndCommonNode.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndCommonNode) {
      var firstGraph = twoMutableGraphsAndCommonNode.firstGraph();
      var secondGraph = twoMutableGraphsAndCommonNode.secondGraph();
      var commonNode = twoMutableGraphsAndCommonNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

      assertThat(union.predecessors(commonNode))
          .as(
              """
              MoreGraphs.union(first, second).predecessors(node) expected to \
              be equal to union of first.predecessors(node) and \
              second.predecessors(node)\
              """)
          .isEqualTo(
              Sets.union(
                  firstGraph.predecessors(commonNode), secondGraph.predecessors(commonNode)));
    }

    @Property
    void
        givenTwoGraphsAndNodeFromFirst_whenGettingUnionPredNodes_andSecondGraphMutated_thenReturnPredNodesOfBoth(
            @ForAll(supplier = MoreArbitraries.GraphsAndGraphNodes.class)
                GraphAndNode firstGraphAndNodeV,
            @ForAll Integer nodeU) {
      var firstGraph = firstGraphAndNodeV.graph();
      Assume.that(!firstGraph.edges().isEmpty());
      Assume.that(!firstGraph.nodes().contains(nodeU));
      var secondGraph = GraphBuilder.from(firstGraph).build();
      var nodeV = firstGraphAndNodeV.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      var predecessors = union.predecessors(nodeV);
      var expected = Sets.union(predecessors, Set.of(nodeU)).immutableCopy();

      secondGraph.putEdge(nodeU, nodeV);

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
            @ForAll(supplier = MoreArbitraries.GraphsAndGraphNodes.class)
                GraphAndNode secondGraphAndNodeV,
            @ForAll Integer nodeU) {
      var secondGraph = secondGraphAndNodeV.graph();
      Assume.that(!secondGraph.edges().isEmpty());
      Assume.that(!secondGraph.nodes().contains(nodeU));
      var firstGraph = GraphBuilder.from(secondGraph).build();
      var nodeV = secondGraphAndNodeV.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      var predecessors = union.predecessors(nodeV);
      var expected = Sets.union(predecessors, Set.of(nodeU)).immutableCopy();

      firstGraph.putEdge(nodeU, nodeV);

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs,
        @ForAll Integer node) {
      Assume.that(
          !graphs.first().nodes().contains(node) && !graphs.second().nodes().contains(node));

      var union = MoreGraphs.union(graphs.first(), graphs.second());
      ThrowingCallable codeUnderTest = () -> union.successors(node);

      assertThatCode(codeUnderTest)
          .as(
              """
              MoreGraphs.union(first, second).successors(absentNode) \
              expected to throw IllegalArgumentException\
              """)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Node '%s' is not in this graph", node);
    }

    @Property
    void givenTwoGraphsAndNodeFromFirst_whenCalculatingUnionSuccNodes_thenReturnSuccNodesOfFirst(
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndNodeFromFirst.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndNode) {
      var firstGraph = twoMutableGraphsAndNode.firstGraph();
      var secondGraph = twoMutableGraphsAndNode.secondGraph();
      var node = twoMutableGraphsAndNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

      assertThat(union.successors(node))
          .as(
              """
              MoreGraphs.union(first, second).successors(node) expected to \
              be equal to first.successors(node)\
              """)
          .isEqualTo(firstGraph.successors(node));
    }

    @Property
    void givenTwoGraphsAndNodeFromSecond_whenCalculatingUnionSuccNodes_thenReturnSuccNodesOfSecond(
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndNodeFromSecond.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndNode) {
      var firstGraph = twoMutableGraphsAndNode.firstGraph();
      var secondGraph = twoMutableGraphsAndNode.secondGraph();
      var node = twoMutableGraphsAndNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

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
        @ForAll(supplier = MoreArbitraries.TwoMutableGraphsWithSameFlagsAndCommonNode.class)
            TwoMutableGraphsAndNode twoMutableGraphsAndCommonNode) {
      var firstGraph = twoMutableGraphsAndCommonNode.firstGraph();
      var secondGraph = twoMutableGraphsAndCommonNode.secondGraph();
      var commonNode = twoMutableGraphsAndCommonNode.node();

      var union = MoreGraphs.union(firstGraph, secondGraph);

      assertThat(union.successors(commonNode))
          .as(
              """
              MoreGraphs.union(first, second).successors(node) expected to \
              be equal to union of first.successors(node) and \
              second.successors(node)\
              """)
          .isEqualTo(
              Sets.union(firstGraph.successors(commonNode), secondGraph.successors(commonNode)));
    }

    @Property
    void
        givenTwoGraphsAndNodeFromFirst_whenGettingUnionSuccNodes_andSecondGraphMutated_thenReturnSuccNodesOfBoth(
            @ForAll(supplier = MoreArbitraries.GraphsAndGraphNodes.class)
                GraphAndNode firstGraphAndNodeU,
            @ForAll Integer nodeV) {
      var firstGraph = firstGraphAndNodeU.graph();
      Assume.that(!firstGraph.edges().isEmpty());
      Assume.that(!firstGraph.nodes().contains(nodeV));
      var secondGraph = GraphBuilder.from(firstGraph).build();
      var nodeU = firstGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      var successors = union.successors(nodeU);
      var expected = Sets.union(successors, Set.of(nodeV)).immutableCopy();

      secondGraph.putEdge(nodeU, nodeV);

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
            @ForAll(supplier = MoreArbitraries.GraphsAndGraphNodes.class)
                GraphAndNode secondGraphAndNodeU,
            @ForAll Integer nodeV) {
      var secondGraph = secondGraphAndNodeU.graph();
      Assume.that(!secondGraph.edges().isEmpty());
      Assume.that(!secondGraph.nodes().contains(nodeV));
      var firstGraph = GraphBuilder.from(secondGraph).build();
      var nodeU = secondGraphAndNodeU.node();
      var union = MoreGraphs.union(firstGraph, secondGraph);

      var successors = union.successors(nodeU);
      var expected = Sets.union(successors, Set.of(nodeV)).immutableCopy();

      firstGraph.putEdge(nodeU, nodeV);

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
        @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs) {

      var union = MoreGraphs.union(graphs.first(), graphs.second());

      assertThat(union.incidentEdgeOrder())
          .as(
              """
              MoreGraphs.union(first, second).incidentEdgeOrder() expected \
              to be the same as both first's and second's .incidentEdgeOrder()\
              """)
          .isEqualTo(graphs.first().incidentEdgeOrder());
    }
  }

  static class TwoGraphsWithDifferentIsDirected implements ArbitrarySupplier<TwoGraphs> {
    @Override
    public Arbitrary<TwoGraphs> get() {
      var arbitraryIsDirected = MoreArbitraries.booleans();
      var arbitraryAllowsSelfLoops = MoreArbitraries.booleans();

      return Combinators.combine(
              arbitraryIsDirected,
              arbitraryAllowsSelfLoops,
              MoreArbitraries.nodes(),
              MoreArbitraries.nodeOrders(),
              MoreArbitraries.incidentEdgeOrders())
          .flatAs(
              (isDirected, allowsSelfLoops, nodes, nodeOrder, incidentEdgeOrder) ->
                  MoreArbitraries.twoGraphs(
                      isDirected,
                      !isDirected,
                      allowsSelfLoops,
                      allowsSelfLoops,
                      nodes,
                      nodes,
                      nodeOrder,
                      nodeOrder,
                      incidentEdgeOrder,
                      incidentEdgeOrder));
    }
  }

  static class TwoGraphsWithDifferentAllowsSelfLoops implements ArbitrarySupplier<TwoGraphs> {
    @Override
    public Arbitrary<TwoGraphs> get() {
      var arbitraryIsDirected = MoreArbitraries.booleans();
      var arbitraryAllowsSelfLoops = MoreArbitraries.booleans();

      return Combinators.combine(
              arbitraryIsDirected,
              arbitraryAllowsSelfLoops,
              MoreArbitraries.nodes(),
              MoreArbitraries.nodeOrders(),
              MoreArbitraries.incidentEdgeOrders())
          .flatAs(
              (isDirected, allowsSelfLoops, nodes, nodeOrder, incidentEdgeOrder) ->
                  MoreArbitraries.twoGraphs(
                      isDirected,
                      isDirected,
                      allowsSelfLoops,
                      !allowsSelfLoops,
                      nodes,
                      nodes,
                      nodeOrder,
                      nodeOrder,
                      incidentEdgeOrder,
                      incidentEdgeOrder));
    }
  }

  static class TwoGraphsWithDifferentNodeOrder implements ArbitrarySupplier<TwoGraphs> {
    @Override
    public Arbitrary<TwoGraphs> get() {
      var arbitraryIsDirected = MoreArbitraries.booleans();
      var arbitraryAllowsSelfLoops = MoreArbitraries.booleans();

      return Combinators.combine(
              arbitraryIsDirected,
              arbitraryAllowsSelfLoops,
              MoreArbitraries.nodes(),
              MoreArbitraries.twoDifferentNodeOrders(),
              MoreArbitraries.incidentEdgeOrders())
          .flatAs(
              (isDirected, allowsSelfLoops, nodes, twoDifferentNodeOrders, incidentEdgeOrder) ->
                  MoreArbitraries.twoGraphs(
                      isDirected,
                      isDirected,
                      allowsSelfLoops,
                      allowsSelfLoops,
                      nodes,
                      nodes,
                      twoDifferentNodeOrders.first(),
                      twoDifferentNodeOrders.second(),
                      incidentEdgeOrder,
                      incidentEdgeOrder));
    }
  }

  static class TwoGraphsWithDifferentIncidentEdgeOrder implements ArbitrarySupplier<TwoGraphs> {
    @Override
    public Arbitrary<TwoGraphs> get() {
      var arbitraryIsDirected = MoreArbitraries.booleans();
      var arbitraryAllowsSelfLoops = MoreArbitraries.booleans();

      return Combinators.combine(
              arbitraryIsDirected,
              arbitraryAllowsSelfLoops,
              MoreArbitraries.nodes(),
              MoreArbitraries.nodeOrders(),
              MoreArbitraries.twoDifferentIncidentEdgeOrders())
          .flatAs(
              (isDirected, allowsSelfLoops, nodes, nodeOrder, twoDifferentIncidentEdgeOrders) ->
                  MoreArbitraries.twoGraphs(
                      isDirected,
                      isDirected,
                      allowsSelfLoops,
                      allowsSelfLoops,
                      nodes,
                      nodes,
                      nodeOrder,
                      nodeOrder,
                      twoDifferentIncidentEdgeOrders.first(),
                      twoDifferentIncidentEdgeOrders.second()));
    }
  }
}
