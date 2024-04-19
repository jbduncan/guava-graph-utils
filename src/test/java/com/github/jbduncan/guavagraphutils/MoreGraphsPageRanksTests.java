package com.github.jbduncan.guavagraphutils;

import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// We test a method that purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreGraphsPageRanksTests {
  @ParameterizedTest
  @MethodSource("pageRanksAlgorithmsWithDampingFactorOf0Point85")
  void wikipediaGraphWithDampingFactor0Point85(
      MoreGraphs.PageRanksAlgorithm<String> pageRanksAlgorithm) {
    var pageRanks = pageRanksAlgorithm.execute();

    assertAll(
        "page ranks",
        () -> assertThat(pageRanks.get("a")).isCloseTo(0.032782, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("b")).isCloseTo(0.384401, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("c")).isCloseTo(0.342910, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("d")).isCloseTo(0.039087, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("e")).isCloseTo(0.080886, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("f")).isCloseTo(0.039087, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("g")).isCloseTo(0.016170, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("h")).isCloseTo(0.016170, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("i")).isCloseTo(0.016170, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("j")).isCloseTo(0.016170, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("k")).isCloseTo(0.016170, offset(1.0e-6)));
  }

  private static Stream<MoreGraphs.PageRanksAlgorithm<String>>
      pageRanksAlgorithmsWithDampingFactorOf0Point85() {
    var graph = wikipediaPageRanksExampleGraph();
    return Stream.of(
        MoreGraphs.pageRanks(graph), MoreGraphs.pageRanks(graph).withDampingFactor(0.85));
  }

  @Test
  void wikipediaGraphWithDampingFactor0Point9() {
    var graph = wikipediaPageRanksExampleGraph();

    var pageRanks = MoreGraphs.pageRanks(graph).withDampingFactor(0.9).execute();

    assertAll(
        "page ranks",
        () -> assertThat(pageRanks.get("a")).isCloseTo(0.023958, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("b")).isCloseTo(0.417685, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("c")).isCloseTo(0.386968, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("d")).isCloseTo(0.028682, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("e")).isCloseTo(0.058769, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("f")).isCloseTo(0.028682, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("g")).isCloseTo(0.011051, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("h")).isCloseTo(0.011051, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("i")).isCloseTo(0.011051, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("j")).isCloseTo(0.011051, offset(1.0e-6)),
        () -> assertThat(pageRanks.get("k")).isCloseTo(0.011051, offset(1.0e-6)));
  }

  @Test
  void resultIsUnmodifiable() {
    var graph = wikipediaPageRanksExampleGraph();

    var pageRanks = MoreGraphs.pageRanks(graph).execute();

    assertThat(pageRanks).isUnmodifiable();
  }

  @Test
  void resultRanksAreSorted() {
    var graph = wikipediaPageRanksExampleGraph();

    var pageRanks = MoreGraphs.pageRanks(graph).execute();

    assertSorted(pageRanks.values());
  }

  // Based on https://commons.wikimedia.org/wiki/File:PageRanks-Example.svg
  private static ImmutableGraph<String> wikipediaPageRanksExampleGraph() {
    return GraphBuilder.directed()
        .<String>immutable()
        .putEdge("b", "c")
        .putEdge("c", "b")
        .putEdge("d", "a")
        .putEdge("d", "b")
        .putEdge("e", "b")
        .putEdge("e", "d")
        .putEdge("e", "f")
        .putEdge("f", "b")
        .putEdge("f", "e")
        .putEdge("g", "b")
        .putEdge("g", "e")
        .putEdge("h", "b")
        .putEdge("h", "e")
        .putEdge("i", "b")
        .putEdge("i", "e")
        .putEdge("j", "e")
        .putEdge("k", "e")
        .build();
  }

  private static void assertSorted(Iterable<Double> values) {
    assertThat(values)
        .containsExactlyElementsOf(Streams.stream(values).sorted(reverseOrder()).toList());
  }
}
