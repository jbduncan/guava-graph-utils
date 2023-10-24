package com.github.jbduncan.guavagraphutils;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.primitives.Booleans;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Tuple;
import net.jqwik.api.arbitraries.SetArbitrary;
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
    return Combinators.combine(nodes(), nodeOrders(), Arbitraries.randoms())
        .as(MoreArbitraries::dagWithRandomEdges);
  }

  static Arbitrary<ImmutableGraph<Integer>> cyclicGraphs() {
    return Combinators.combine(
            nodes(MIN_NODES_COUNT_FOR_CYCLIC_GRAPH, MAX_NODES_COUNT_FOR_ANY_GRAPH), nodeOrders())
        .as(MoreArbitraries::ringGraph);
  }

  private static ImmutableGraph<Integer> ringGraph(
      Collection<Integer> nodes, ElementOrder<Integer> nodeOrder) {
    MutableGraph<Integer> graph =
        GraphBuilder.directed().allowsSelfLoops(true).nodeOrder(nodeOrder).build();
    var graphAdapter = new MutableGraphAdapter<>(graph, nodes.iterator()::next, null);
    var graphGenerator = new RingGraphGenerator<Integer, EndpointPair<Integer>>(nodes.size());
    graphGenerator.generateGraph(graphAdapter);
    return ImmutableGraph.copyOf(graph);
  }

  record TwoGraphs(Graph<Integer> first, Graph<Integer> second) {}

  static Arbitrary<TwoGraphs> twoGraphsWithSameFlags() {
    Arbitrary<Boolean> arbitraryIsDirected = Arbitraries.of(true, false);
    Arbitrary<Boolean> arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected, arbitraryAllowsSelfLoops, nodeOrders(), incidentEdgeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodeOrder, incidentEdgeOrder) -> {
              var arbitraryGraphsWithFixedFlags =
                  graphs(
                      Arbitraries.just(isDirected),
                      Arbitraries.just(allowsSelfLoops),
                      MoreArbitraries.nodes(),
                      Arbitraries.just(nodeOrder),
                      Arbitraries.just(incidentEdgeOrder));
              return arbitraryGraphsWithFixedFlags
                  .tuple2()
                  .map(t -> new TwoGraphs(t.get1(), t.get2()));
            });
  }

  static Arbitrary<TwoGraphs> twoGraphsWithSameFlagsAndDisjointedNodes() {
    Arbitrary<Boolean> arbitraryIsDirected = Arbitraries.of(true, false);
    Arbitrary<Boolean> arbitraryAllowsSelfLoops = Arbitraries.of(true, false);

    return Combinators.combine(
            arbitraryIsDirected,
            arbitraryAllowsSelfLoops,
            nodes(),
            nodeOrders(),
            incidentEdgeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodes, nodeOrder, incidentEdgeOrder) -> {
              var nodesASize = Arbitraries.integers().between(0, nodes.size());
              var splitNodes = nodesASize.map(n -> splitAtIndex(nodes, n));
              return splitNodes.flatMap(
                  sets ->
                      Combinators.combine(
                              graphs(
                                  Arbitraries.just(isDirected),
                                  Arbitraries.just(allowsSelfLoops),
                                  Arbitraries.just(sets.get1()),
                                  Arbitraries.just(nodeOrder),
                                  Arbitraries.just(incidentEdgeOrder)),
                              graphs(
                                  Arbitraries.just(isDirected),
                                  Arbitraries.just(allowsSelfLoops),
                                  Arbitraries.just(sets.get2()),
                                  Arbitraries.just(nodeOrder),
                                  Arbitraries.just(incidentEdgeOrder)))
                          .as(TwoGraphs::new));
            });
  }

  private static <T> Tuple.Tuple2<Set<T>, Set<T>> splitAtIndex(Set<T> set, int index) {
    var list = new ArrayList<>(set);
    var nodesA = new ArrayList<T>(index);
    for (int i = 0; i < index; i++) {
      nodesA.add(list.get(i));
    }
    var nodesB = new ArrayList<T>();
    for (int i = index; i < list.size(); i++) {
      nodesB.add(list.get(i));
    }
    return Tuple.of(ImmutableSet.copyOf(nodesA), ImmutableSet.copyOf(nodesB));
  }

  static Arbitrary<Graph<Integer>> graphs() {
    return graphs(
        Arbitraries.of(true, false),
        Arbitraries.of(true, false),
        nodes(),
        nodeOrders(),
        incidentEdgeOrders());
  }

  static Arbitrary<Graph<Integer>> directedGraphs() {
    return graphs(
        Arbitraries.just(true),
        Arbitraries.of(true, false),
        nodes(),
        nodeOrders(),
        incidentEdgeOrders());
  }

  static Arbitrary<Graph<Integer>> undirectedGraphs() {
    return graphs(
        Arbitraries.just(false),
        Arbitraries.of(true, false),
        nodes(),
        nodeOrders(),
        incidentEdgeOrders());
  }

  static Arbitrary<Graph<Integer>> graphs(
      Arbitrary<Boolean> arbitraryIsDirected,
      Arbitrary<Boolean> arbitraryAllowsSelfLoops,
      Arbitrary<Set<Integer>> arbitraryNodes,
      Arbitrary<ElementOrder<Integer>> arbitraryNodeOrder,
      Arbitrary<ElementOrder<Integer>> arbitraryIncidentEdgeOrder) {
    Arbitrary<MutableGraph<Integer>> arbitraryEmptyGraphs =
        Combinators.combine(
                arbitraryIsDirected,
                arbitraryAllowsSelfLoops,
                arbitraryNodeOrder,
                arbitraryIncidentEdgeOrder)
            .as(
                (isDirected, allowsSelfLoops, nodeOrder, incidentEdgeOrder) -> {
                  GraphBuilder<Object> graphBuilder =
                      isDirected ? GraphBuilder.directed() : GraphBuilder.undirected();
                  return graphBuilder
                      .allowsSelfLoops(allowsSelfLoops)
                      .nodeOrder(nodeOrder)
                      .incidentEdgeOrder(incidentEdgeOrder)
                      .build();
                });

    Arbitrary<Double> arbitraryEdgeProbabilities =
        Arbitraries.doubles().between(0d, 1d).withSpecialValue(0d).withSpecialValue(1d);
    return Combinators.combine(
            arbitraryEmptyGraphs, arbitraryNodes, arbitraryEdgeProbabilities, Arbitraries.randoms())
        .as(MoreArbitraries::populateGraph);
  }

  private static Graph<Integer> populateGraph(
      MutableGraph<Integer> graph,
      Collection<Integer> nodes,
      double edgeProbability,
      Random random) {
    var graphAdapter = new MutableGraphAdapter<>(graph, nodes.iterator()::next, null);
    new GnpRandomGraphGenerator<Integer, EndpointPair<Integer>>(
            nodes.size(), edgeProbability, random, false)
        .generateGraph(graphAdapter);
    return graph; // avoid returning ImmutableGraph as it always has a stable .incidentEdgeOrder()
  }

  static SetArbitrary<Integer> nodes() {
    return nodes(MIN_NODES_COUNT_FOR_ANY_GRAPH, MAX_NODES_COUNT_FOR_ANY_GRAPH);
  }

  static SetArbitrary<Integer> nodes(int minNodesCount, int maxNodesCount) {
    return Arbitraries.integers()
        .between(SMALLEST_NODE, LARGEST_NODE)
        .set()
        .ofMinSize(minNodesCount)
        .ofMaxSize(maxNodesCount);
  }

  record TwoElementOrders(ElementOrder<Integer> first, ElementOrder<Integer> second) {}

  private static final Set<ElementOrder<Integer>> NODE_ORDERS =
      Set.of(
          ElementOrder.natural(),
          ElementOrder.unordered(),
          ElementOrder.stable(),
          ElementOrder.sorted(Comparator.<Integer>reverseOrder()));

  static Arbitrary<ElementOrder<Integer>> nodeOrders() {
    return Arbitraries.of(NODE_ORDERS);
  }

  static Arbitrary<TwoElementOrders> twoDifferentNodeOrders() {
    return MoreArbitraries.nodeOrders()
        .flatMap(
            elementOrder ->
                Combinators.combine(
                        Arbitraries.just(elementOrder),
                        Arbitraries.of(Sets.difference(NODE_ORDERS, Set.of(elementOrder))))
                    .as(TwoElementOrders::new));
  }

  static Arbitrary<ElementOrder<Integer>> incidentEdgeOrders() {
    return Arbitraries.of(ElementOrder.stable(), ElementOrder.unordered());
  }

  static Arbitrary<TwoElementOrders> twoDifferentIncidentEdgeOrders() {
    return Arbitraries.of(true, false)
        .map(
            unorderedFirst -> {
              if (!unorderedFirst) {
                return new TwoElementOrders(ElementOrder.stable(), ElementOrder.unordered());
              } else {
                return new TwoElementOrders(ElementOrder.unordered(), ElementOrder.stable());
              }
            });
  }

  private static ImmutableGraph<Integer> dagWithRandomEdges(
      Collection<Integer> nodes, ElementOrder<Integer> nodeOrder, Random random) {
    Integer[] nodesArray = nodes.toArray(Integer[]::new);
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
          graphBuilder.putEdge(nodesArray[i], nodesArray[j]);
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
