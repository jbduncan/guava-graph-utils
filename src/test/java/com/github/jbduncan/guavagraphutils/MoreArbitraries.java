package com.github.jbduncan.guavagraphutils;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.arbitraries.DoubleArbitrary;
import net.jqwik.api.arbitraries.ListArbitrary;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.RingGraphGenerator;
import org.jgrapht.graph.guava.MutableGraphAdapter;

// We purposefully use an unstable Guava API
@SuppressWarnings("UnstableApiUsage")
final class MoreArbitraries {
  private static final int MIN_NODES_COUNT_FOR_CYCLIC_GRAPH = 1;
  private static final int MIN_NODES_COUNT_FOR_ANY_GRAPH = 0;
  private static final int MAX_NODES_COUNT_FOR_ANY_GRAPH = 100;

  private static final int SMALLEST_NODE = 0;
  private static final int LARGEST_NODE = Integer.MAX_VALUE;

  static Arbitrary<ImmutableGraph<Integer>> directedAcyclicGraphs() {
    ListArbitrary<Integer> arbitraryNodes =
        arbitraryNodes(MIN_NODES_COUNT_FOR_ANY_GRAPH, MIN_NODES_COUNT_FOR_ANY_GRAPH);
    Arbitrary<Integer> arbitraryEdgeCount =
        arbitraryNodes.flatMap(
            nodes -> Arbitraries.integers().between(0, maxEdgesForDag(nodes.size())));
    return Combinators.combine(arbitraryNodes, arbitraryEdgeCount, Arbitraries.randoms())
        .as(MoreArbitraries::dagWithRandomEdges);
  }

  static Arbitrary<ImmutableGraph<Integer>> cyclicGraphs() {
    return arbitraryNodes(MIN_NODES_COUNT_FOR_CYCLIC_GRAPH, MAX_NODES_COUNT_FOR_ANY_GRAPH)
        .map(MoreArbitraries::ringGraph);
  }

  private static ImmutableGraph<Integer> ringGraph(List<Integer> nodes) {
    MutableGraph<Integer> graph = GraphBuilder.directed().allowsSelfLoops(true).build();
    var graphAdapter = new MutableGraphAdapter<>(graph, nodes.iterator()::next, null);
    var graphGenerator = new RingGraphGenerator<Integer, EndpointPair<Integer>>(nodes.size());
    graphGenerator.generateGraph(graphAdapter);
    return ImmutableGraph.copyOf(graph);
  }

  static Arbitrary<ImmutableGraph<Integer>> graphs() {
    Arbitrary<MutableGraph<Integer>> arbitraryGraphs =
        Arbitraries.ofSuppliers(
            () -> GraphBuilder.directed().allowsSelfLoops(true).build(),
            () -> GraphBuilder.directed().allowsSelfLoops(false).build(),
            () -> GraphBuilder.undirected().allowsSelfLoops(true).build(),
            () -> GraphBuilder.undirected().allowsSelfLoops(false).build());
    DoubleArbitrary arbitraryEdgeProbabilities = Arbitraries.doubles().between(0d, 1d);
    return Combinators.combine(
            arbitraryGraphs,
            arbitraryNodes(MIN_NODES_COUNT_FOR_ANY_GRAPH, MAX_NODES_COUNT_FOR_ANY_GRAPH),
            arbitraryEdgeProbabilities,
            Arbitraries.randoms())
        .as(MoreArbitraries::populateGraph);
  }

  private static ImmutableGraph<Integer> populateGraph(
      MutableGraph<Integer> graph, List<Integer> nodes, Double edgeProbability, Random random) {
    var graphAdapter = new MutableGraphAdapter<>(graph, nodes.iterator()::next, null);
    new GnpRandomGraphGenerator<Integer, EndpointPair<Integer>>(
            nodes.size(), edgeProbability, random, false)
        .generateGraph(graphAdapter);
    return ImmutableGraph.copyOf(graph);
  }

  static ListArbitrary<Integer> arbitraryNodes(int minNodesCount, int maxNodesCount) {
    return Arbitraries.integers()
        .between(SMALLEST_NODE, LARGEST_NODE)
        .list()
        .uniqueElements()
        .ofMinSize(minNodesCount)
        .ofMaxSize(maxNodesCount);
  }

  private static ImmutableGraph<Integer> dagWithRandomEdges(
      List<Integer> nodes, int edgeCount, Random random) {
    var randomEdges = new ArrayList<Boolean>(maxEdgesForDag(nodes.size()));
    for (int i = 0; i < edgeCount; i++) {
      randomEdges.add(true);
    }
    for (int i = edgeCount; i < maxEdgesForDag(nodes.size()); i++) {
      randomEdges.add(false);
    }
    Collections.shuffle(randomEdges, random);

    ImmutableGraph.Builder<Integer> graphBuilder =
        GraphBuilder.directed().allowsSelfLoops(false).immutable();

    int index = 0;
    for (int i = 0; i < nodes.size() - 1; i++) {
      for (int j = i + 1; j < nodes.size(); j++) {
        if (randomEdges.get(index)) {
          graphBuilder.putEdge(nodes.get(i), nodes.get(j));
        }
        index++;
      }
    }

    return graphBuilder.build();
  }

  // For any directed acyclic graph, the maximum number of edges is equal to (n * (n - 1) / 2),
  // where n is the number of nodes in the graph.
  private static int maxEdgesForDag(int n) {
    if (n > 65_536) {
      throw new IllegalArgumentException("n is %d but must not be greater than 65536".formatted(n));
    }

    return (int) ((long) n * ((long) n - 1) / 2);
  }

  private MoreArbitraries() {}
}
