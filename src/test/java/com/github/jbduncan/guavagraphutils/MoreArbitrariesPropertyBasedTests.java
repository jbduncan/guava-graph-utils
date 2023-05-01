package com.github.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Range;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import java.util.Set;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;

// We purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreArbitrariesPropertyBasedTests {
  @Property
  void givenArbitraryDag_whenFindingCycles_thenNoneAreFound(
      @ForAll("dags") ImmutableGraph<Integer> dag) {
    Statistics.label("nodes count").collect(countStats(dag.nodes()));
    Statistics.label("edges count").collect(countStats(dag.edges()));

    assertThat(Graphs.hasCycle(dag)).isFalse();
    assertThat(dag.isDirected()).isTrue();
  }

  @Provide
  Arbitrary<ImmutableGraph<Integer>> dags() {
    return MoreArbitraries.directedAcyclicGraphs();
  }

  @Property
  void givenArbitraryCyclicGraph_whenFindingCycles_thenAtLeastOneIsFound(
      @ForAll("cyclicGraphs") ImmutableGraph<Integer> cyclicGraph) {
    Statistics.label("nodes count").collect(countStats(cyclicGraph.nodes()));
    Statistics.label("edges count").collect(countStats(cyclicGraph.edges()));
    Statistics.label("directed graph").collect(cyclicGraph.isDirected());

    assertThat(Graphs.hasCycle(cyclicGraph)).isTrue();
  }

  @Provide
  Arbitrary<ImmutableGraph<Integer>> cyclicGraphs() {
    return MoreArbitraries.cyclicGraphs();
  }

  private static String countStats(Set<?> values) {
    String nodeCountStats;
    if (Range.closedOpen(0, 20).contains(values.size())) {
      nodeCountStats = "0 <= x < 20";
    } else if (Range.closed(20, 40).contains(values.size())) {
      nodeCountStats = "20 <= x < 40";
    } else if (Range.closed(40, 60).contains(values.size())) {
      nodeCountStats = "40 <= x < 60";
    } else if (Range.closed(60, 80).contains(values.size())) {
      nodeCountStats = "60 <= x < 80";
    } else if (Range.closed(80, 100).contains(values.size())) {
      nodeCountStats = "80 <= x < 100";
    } else {
      nodeCountStats = "100 <= x <= Integer.MAX_VALUE";
    }
    return nodeCountStats;
  }
}
