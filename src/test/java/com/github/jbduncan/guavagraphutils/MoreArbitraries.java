package com.github.jbduncan.guavagraphutils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.function.Predicate.not;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.primitives.Booleans;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
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
        nodes(MIN_NODES_COUNT_FOR_ANY_GRAPH, MAX_NODES_COUNT_FOR_ANY_GRAPH);
    return Combinators.combine(arbitraryNodes, nodeOrders(), Arbitraries.randoms())
        .as(MoreArbitraries::dagWithRandomEdges);
  }

  static Arbitrary<ImmutableGraph<Integer>> cyclicGraphs() {
    return Combinators.combine(
            nodes(MIN_NODES_COUNT_FOR_CYCLIC_GRAPH, MAX_NODES_COUNT_FOR_ANY_GRAPH), nodeOrders())
        .as(MoreArbitraries::ringGraph);
  }

  private static ImmutableGraph<Integer> ringGraph(
      List<Integer> nodes, ElementOrder<Integer> nodeOrder) {
    MutableGraph<Integer> graph =
        GraphBuilder.directed().allowsSelfLoops(true).nodeOrder(nodeOrder).build();
    var graphAdapter = new MutableGraphAdapter<>(graph, nodes.iterator()::next, null);
    var graphGenerator = new RingGraphGenerator<Integer, EndpointPair<Integer>>(nodes.size());
    graphGenerator.generateGraph(graphAdapter);
    return ImmutableGraph.copyOf(graph);
  }

  record TwoGraphs(ImmutableGraph<Integer> first, ImmutableGraph<Integer> second) {}

  static Arbitrary<TwoGraphs> twoGraphsWithSameFlags() {
    Arbitrary<Boolean> arbitraryIsDirected = Arbitraries.of(true, false);
    Arbitrary<Boolean> arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(arbitraryIsDirected, arbitraryAllowsSelfLoops, nodeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodeOrder) -> {
              var arbitraryGraphsWithFixedFlags =
                  graphs(
                      Arbitraries.just(isDirected),
                      Arbitraries.just(allowsSelfLoops),
                      Arbitraries.just(nodeOrder));
              return arbitraryGraphsWithFixedFlags
                  .tuple2()
                  .map(t -> new TwoGraphs(t.get1(), t.get2()));
            });
  }

  static Arbitrary<ImmutableGraph<Integer>> graphs() {
    return Arbitraries.oneOf(directedGraphs(), undirectedGraphs());
  }

  static Arbitrary<ImmutableGraph<Integer>> directedGraphs() {
    return graphs(Arbitraries.just(true), Arbitraries.of(true, false), nodeOrders());
  }

  static Arbitrary<ImmutableGraph<Integer>> undirectedGraphs() {
    return graphs(Arbitraries.just(false), Arbitraries.of(true, false), nodeOrders());
  }

  static Arbitrary<ImmutableGraph<Integer>> graphs(
      Arbitrary<Boolean> arbitraryIsDirected,
      Arbitrary<Boolean> arbitraryAllowsSelfLoops,
      Arbitrary<ElementOrder<Integer>> arbitraryNodeOrder) {

    Arbitrary<MutableGraph<Integer>> arbitraryEmptyGraphs =
        Combinators.combine(arbitraryIsDirected, arbitraryAllowsSelfLoops, arbitraryNodeOrder)
            .as(
                (isDirected, allowsSelfLoops, nodeOrder) -> {
                  GraphBuilder<Object> graphBuilder =
                      isDirected ? GraphBuilder.directed() : GraphBuilder.undirected();
                  return graphBuilder.allowsSelfLoops(allowsSelfLoops).nodeOrder(nodeOrder).build();
                });

    Arbitrary<Double> arbitraryEdgeProbabilities =
        Arbitraries.doubles().between(0d, 1d).withSpecialValue(0d).withSpecialValue(1d);
    return Combinators.combine(
            arbitraryEmptyGraphs,
            nodes(MIN_NODES_COUNT_FOR_ANY_GRAPH, MAX_NODES_COUNT_FOR_ANY_GRAPH),
            arbitraryEdgeProbabilities,
            Arbitraries.randoms())
        .as(MoreArbitraries::populateGraph);
  }

  private static ImmutableGraph<Integer> populateGraph(
      MutableGraph<Integer> graph, List<Integer> nodes, double edgeProbability, Random random) {
    var graphAdapter = new MutableGraphAdapter<>(graph, nodes.iterator()::next, null);
    new GnpRandomGraphGenerator<Integer, EndpointPair<Integer>>(
            nodes.size(), edgeProbability, random, false)
        .generateGraph(graphAdapter);
    return ImmutableGraph.copyOf(graph);
  }

  static ListArbitrary<Integer> nodes(int minNodesCount, int maxNodesCount) {
    return Arbitraries.integers()
        .between(SMALLEST_NODE, LARGEST_NODE)
        .list()
        .uniqueElements()
        .ofMinSize(minNodesCount)
        .ofMaxSize(maxNodesCount);
  }

  static Arbitrary<ElementOrder<Integer>> nodeOrders() {
    return Arbitraries.of(
        ElementOrder.natural(),
        ElementOrder.unordered(),
        ElementOrder.stable(),
        ElementOrder.sorted(Comparator.<Integer>reverseOrder()));
  }

  record TwoNodeOrders(ElementOrder<Integer> first, ElementOrder<Integer> second) {}

  static Arbitrary<TwoNodeOrders> twoDifferentNodeOrders() {
    return MoreArbitraries.nodeOrders()
        .flatMap(
            nodeOrder ->
                Combinators.combine(
                        Arbitraries.just(nodeOrder),
                        MoreArbitraries.nodeOrders().filter(not(nodeOrder::equals)))
                    .as(TwoNodeOrders::new));
  }

  private static ImmutableGraph<Integer> dagWithRandomEdges(
      List<Integer> nodes, ElementOrder<Integer> nodeOrder, Random random) {
    int edgeCount =
        maxEdgesForDag(nodes.size()) == 0 //
            ? 0
            : random.nextInt(0, maxEdgesForDag(nodes.size()));

    // filled with `false` by default
    var randomEdges = new boolean[maxEdgesForDag(nodes.size())];
    for (int i = 0; i < edgeCount; i++) {
      randomEdges[i] = true;
    }
    Collections.shuffle(Booleans.asList(randomEdges), random);

    ImmutableGraph.Builder<Integer> graphBuilder =
        GraphBuilder.directed().allowsSelfLoops(false).nodeOrder(nodeOrder).immutable();

    int index = 0;
    for (int i = 0; i < nodes.size() - 1; i++) {
      for (int j = i + 1; j < nodes.size(); j++) {
        if (randomEdges[index]) {
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
    checkArgument(n >= 0, "n is %s but must be non-negative", n);
    checkArgument(n <= 65_536, "n is %s but must not be greater than 65536", n);
    return (int) ((long) n * ((long) n - 1) / 2);
  }

  private MoreArbitraries() {}
}
