package com.github.jbduncan.guavagraphutils;

import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import org.junit.jupiter.api.Test;

// We test a method that purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
class MoreGraphsPageRanksTests {
  @Test
  void whenCalculatingPageRanksOfWikipediaExampleGraph_thenExpectedRanksAreReturned() {
    // given
    var graph = wikipediaPageRanksExampleGraph();

    // when
    var pageRanks = MoreGraphs.pageRanks(graph).execute();

    // then
    assertAll(
        "page ranks",
        () -> assertThat(pageRanks.get("a")).isCloseTo(0.0327815, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("b")).isCloseTo(0.3844010, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("c")).isCloseTo(0.3429103, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("d")).isCloseTo(0.0390871, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("e")).isCloseTo(0.0808857, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("f")).isCloseTo(0.0390871, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("g")).isCloseTo(0.0161695, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("h")).isCloseTo(0.0161695, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("i")).isCloseTo(0.0161695, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("j")).isCloseTo(0.0161695, offset(1.0e-7)),
        () -> assertThat(pageRanks.get("k")).isCloseTo(0.0161695, offset(1.0e-7)));
  }

  @Test
  void whenCalculatingPageRanks_thenResultIsUnmodifiable() {
    // given
    var graph = wikipediaPageRanksExampleGraph();

    // when
    var pageRanks = MoreGraphs.pageRanks(graph).execute();

    // then
    assertThat(pageRanks).isUnmodifiable();
  }

  @Test
  void whenCalculatingPageRanks_thenRanksAreSorted() {
    // given
    var graph = wikipediaPageRanksExampleGraph();

    // when
    var pageRanks = MoreGraphs.pageRanks(graph).execute();

    // then
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
