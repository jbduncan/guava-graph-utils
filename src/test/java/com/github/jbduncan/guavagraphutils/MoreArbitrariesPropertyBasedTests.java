package com.github.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.jbduncan.guavagraphutils.MoreArbitraries.TwoElementOrders;
import com.github.jbduncan.guavagraphutils.MoreArbitraries.TwoGraphs;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import java.util.Set;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.statistics.Statistics;

// We purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreArbitrariesPropertyBasedTests {
  @Property
  void arbitraryDagIsValid(
      @ForAll(supplier = MoreArbitraries.DirectedAcyclicGraphs.class) ImmutableGraph<Integer> dag) {
    Statistics.label("nodes count").collect(sizeStats(dag.nodes()));
    Statistics.label("edges count").collect(sizeStats(dag.edges()));

    assertThat(Graphs.hasCycle(dag)).isFalse();
    assertThat(dag.isDirected()).isTrue();
  }

  @Property
  void arbitraryCyclicGraphIsValid(
      @ForAll(supplier = MoreArbitraries.CyclicGraphs.class) ImmutableGraph<Integer> cyclicGraph) {
    Statistics.label("nodes count").collect(sizeStats(cyclicGraph.nodes()));
    Statistics.label("edges count").collect(sizeStats(cyclicGraph.edges()));
    Statistics.label("directed graph").collect(cyclicGraph.isDirected());

    assertThat(Graphs.hasCycle(cyclicGraph)).isTrue();
  }

  @Property
  void arbitraryTwoGraphsWithSameFlagsAreValid(
      @ForAll(supplier = MoreArbitraries.TwoGraphsWithSameFlags.class) TwoGraphs graphs) {
    Statistics.label("first graph: nodes count").collect(sizeStats(graphs.first().nodes()));
    Statistics.label("first graph: edges count").collect(sizeStats(graphs.first().edges()));
    Statistics.label("first graph: node order").collect(graphs.first().nodeOrder());
    Statistics.label("first graph: incident edge order")
        .collect(graphs.first().incidentEdgeOrder());
    Statistics.label("second graph: nodes count").collect(sizeStats(graphs.second().nodes()));
    Statistics.label("second graph: edges count").collect(sizeStats(graphs.second().edges()));
    Statistics.label("second graph: node order").collect(graphs.second().nodeOrder());
    Statistics.label("second graph: incident edge order")
        .collect(graphs.second().incidentEdgeOrder());

    assertThat(graphs.first().isDirected()).isEqualTo(graphs.second().isDirected());
    assertThat(graphs.first().allowsSelfLoops()).isEqualTo(graphs.second().allowsSelfLoops());
    assertThat(graphs.first().nodeOrder()).isEqualTo(graphs.second().nodeOrder());
    assertThat(graphs.first().incidentEdgeOrder()).isEqualTo(graphs.second().incidentEdgeOrder());
  }

  @Property
  void arbitraryTwoDifferentNodeOrdersIsValid(
      @ForAll(supplier = MoreArbitraries.TwoDifferentNodeOrders.class)
          TwoElementOrders twoDifferentNodeOrders) {
    Statistics.collect(twoDifferentNodeOrders);

    assertThat(twoDifferentNodeOrders.first()).isNotEqualTo(twoDifferentNodeOrders.second());
  }

  private static String sizeStats(Set<?> values) {
    if (values.size() < 20) {
      return "0 <= x < 20";
    }
    if (values.size() < 40) {
      return "20 <= x < 40";
    }
    if (values.size() < 60) {
      return "40 <= x < 60";
    }
    if (values.size() < 80) {
      return "60 <= x < 80";
    }
    if (values.size() < 100) {
      return "80 <= x < 100";
    }
    return "100 <= x <= Integer.MAX_VALUE";
  }
}
