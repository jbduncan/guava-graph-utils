package me.jbduncan.guavagraphutils;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.IterableAssert;

public class GraphAssert<N> extends AbstractAssert<GraphAssert<N>, Graph<N>> {
  protected GraphAssert(Graph<N> actual) {
    super(actual, GraphAssert.class);
  }

  public static <N> GraphAssert assertThat(Graph<N> actual) {
    return new GraphAssert<>(actual);
  }

  public GraphAssert<N> hasValidTopologicalOrdering() {
    isNotNull();

    Iterable<N> topologicalOrdering = MoreGraphs.topologicalOrdering(actual);
    assertThatTopologicalOrdering(topologicalOrdering)
        .containsExactlyInAnyOrderElementsOf(actual.nodes()); // test .iterator()

    for (EndpointPair<N> edge : actual.edges()) {
      assertThatEdgeOrder(edge).isTrue();
      assertThatTopologicalOrdering(topologicalOrdering)
          .containsSubsequence(edge.source(), edge.target());
    }

    return this;
  }

  private static <N> BooleanAssert assertThatEdgeOrder(EndpointPair<N> edge) {
    return new BooleanAssert(edge.isOrdered()).as("edge %s is ordered", edge);
  }

  private static <N> IterableAssert<N> assertThatTopologicalOrdering(
      Iterable<N> topologicalOrdering) {
    return new IterableAssert<>(topologicalOrdering).as("topological ordering");
  }
}
