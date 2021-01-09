package org.jbduncan;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MoreGraphsTests {

  @Test
  void buildGraphWithBreadthFirstTraversal() {
    var circularImmutableGraph =
        MoreGraphs.buildGraphWithBreadthFirstTraversal(
            ImmutableList.of(1),
            node -> {
              int result = node * 2;
              return result <= 4 ? ImmutableList.of(result) : ImmutableList.of(1);
            });

    assertAll(
        () -> assertThat(circularImmutableGraph.nodes()).containsExactlyInAnyOrder(1, 2, 4),
        () ->
            assertThat(circularImmutableGraph.edges())
                .containsExactlyInAnyOrder(
                    EndpointPair.ordered(1, 2),
                    EndpointPair.ordered(2, 4)));
  }
}
