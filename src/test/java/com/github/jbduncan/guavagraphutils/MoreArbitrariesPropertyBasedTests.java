package com.github.jbduncan.guavagraphutils;

import static org.assertj.core.api.Assertions.assertThat;

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
