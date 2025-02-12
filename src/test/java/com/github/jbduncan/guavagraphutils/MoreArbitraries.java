package com.github.jbduncan.guavagraphutils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.shuffle;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static net.jqwik.api.Arbitraries.just;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.primitives.Booleans;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
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

  static class Graphs implements ArbitrarySupplier<Graph<Integer>> {
    @Override
    public Arbitrary<Graph<Integer>> get() {
      return graphs(booleans(), booleans(), nodes(), nodeOrders(), incidentEdgeOrders());
    }
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
                (isDirected, allowsSelfLoops, nodeOrder, incidentEdgeOrder) ->
                    emptyMutableGraph(
                        requireNonNull(isDirected),
                        requireNonNull(allowsSelfLoops),
                        requireNonNull(nodeOrder),
                        requireNonNull(incidentEdgeOrder)));

    Arbitrary<Double> arbitraryEdgeProbabilities =
        Arbitraries.doubles().between(0d, 1d).withSpecialValue(0d).withSpecialValue(1d);
    return Combinators.combine(
            arbitraryEmptyGraphs, arbitraryNodes, arbitraryEdgeProbabilities, Arbitraries.randoms())
        .as(
            (graph, nodes, edgeProbability, random) ->
                populateGraph(
                    requireNonNull(graph),
                    requireNonNull(nodes),
                    requireNonNull(edgeProbability),
                    requireNonNull(random)));
  }

  static Arbitrary<Graph<Integer>> graphs(
      boolean isDirected,
      boolean allowsSelfLoops,
      Set<Integer> nodes,
      ElementOrder<Integer> nodeOrder,
      ElementOrder<Integer> incidentEdgeOrder) {
    return graphs(
        just(isDirected),
        just(allowsSelfLoops),
        just(nodes),
        just(nodeOrder),
        just(incidentEdgeOrder));
  }

  private static MutableGraph<Integer> emptyMutableGraph(
      boolean isDirected,
      boolean allowsSelfLoops,
      ElementOrder<Integer> nodeOrder,
      ElementOrder<Integer> incidentEdgeOrder) {
    var graphBuilder = isDirected ? GraphBuilder.directed() : GraphBuilder.undirected();
    return graphBuilder
        .allowsSelfLoops(allowsSelfLoops)
        .nodeOrder(nodeOrder)
        .incidentEdgeOrder(incidentEdgeOrder)
        .build();
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

  static class DirectedAcyclicGraphs implements ArbitrarySupplier<ImmutableGraph<Integer>> {
    @Override
    public Arbitrary<ImmutableGraph<Integer>> get() {
      return Combinators.combine(nodes(), nodeOrders(), Arbitraries.randoms())
          .as(
              (nodes, nodeOrder, random) ->
                  dagWithRandomEdges(
                      requireNonNull(nodes), requireNonNull(nodeOrder), requireNonNull(random)));
    }
  }

  private static ImmutableGraph<Integer> dagWithRandomEdges(
      Set<Integer> nodes, ElementOrder<Integer> nodeOrder, Random random) {
    int edgeCount =
        maxEdgesForDag(nodes.size()) == 0 //
            ? 0
            : random.nextInt(0, maxEdgesForDag(nodes.size()));

    // filled with `false` by default
    var randomEdges = new boolean[maxEdgesForDag(nodes.size())];
    for (int i = 0; i < edgeCount; i++) {
      randomEdges[i] = true;
    }
    shuffle(Booleans.asList(randomEdges), random);

    ImmutableGraph.Builder<Integer> graphBuilder =
        GraphBuilder.directed().allowsSelfLoops(false).nodeOrder(nodeOrder).immutable();

    int[] nodesArray = nodes.stream().mapToInt(i -> i).toArray();
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

  static class CyclicGraphs implements ArbitrarySupplier<ImmutableGraph<Integer>> {
    @Override
    public Arbitrary<ImmutableGraph<Integer>> get() {
      return Combinators.combine(nodes(MIN_NODES_COUNT_FOR_CYCLIC_GRAPH), nodeOrders())
          .as((nodes, nodeOrder) -> ringGraph(requireNonNull(nodes), requireNonNull(nodeOrder)));
    }
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

  record GraphAndNode(Graph<Integer> graph, Integer node) {}

  static class GraphsAndGraphNodes implements ArbitrarySupplier<GraphAndNode> {
    @Override
    public Arbitrary<GraphAndNode> get() {
      return graphs(booleans(), booleans(), nodes(), nodeOrders(), incidentEdgeOrders())
          .filter(not(graph -> graph.nodes().isEmpty()))
          .flatMap(
              graph ->
                  Combinators.combine(just(graph), Arbitraries.of(graph.nodes()))
                      .as((g, node) -> new GraphAndNode(requireNonNull(g), requireNonNull(node))));
    }
  }

  record TwoGraphs(Graph<Integer> first, Graph<Integer> second) {}

  static Arbitrary<TwoGraphs> twoGraphs(
      boolean isDirectedA,
      boolean isDirectedB,
      boolean allowsSelfLoopsA,
      boolean allowsSelfLoopsB,
      Set<Integer> nodesA,
      Set<Integer> nodesB,
      ElementOrder<Integer> nodeOrderA,
      ElementOrder<Integer> nodeOrderB,
      ElementOrder<Integer> incidentEdgeOrderA,
      ElementOrder<Integer> incidentEdgeOrderB) {
    var firstGraph =
        MoreArbitraries.graphs(
            isDirectedA, allowsSelfLoopsA, nodesA, nodeOrderA, incidentEdgeOrderA);
    var secondGraph =
        MoreArbitraries.graphs(
            isDirectedB, allowsSelfLoopsB, nodesB, nodeOrderB, incidentEdgeOrderB);
    return Combinators.combine(firstGraph, secondGraph)
        .as((first, second) -> new TwoGraphs(requireNonNull(first), requireNonNull(second)));
  }

  static class TwoGraphsWithSameFlags implements ArbitrarySupplier<TwoGraphs> {
    @Override
    public Arbitrary<TwoGraphs> get() {
      Arbitrary<Boolean> arbitraryIsDirected = booleans();
      Arbitrary<Boolean> arbitraryAllowsSelfLoops = booleans();

      return Combinators.combine(
              arbitraryIsDirected, arbitraryAllowsSelfLoops, nodeOrders(), incidentEdgeOrders())
          .flatAs(
              (isDirected, allowsSelfLoops, nodeOrder, incidentEdgeOrder) -> {
                var arbitraryGraphsWithFixedFlags =
                    graphs(
                        just(isDirected),
                        just(allowsSelfLoops),
                        MoreArbitraries.nodes(),
                        just(nodeOrder),
                        just(incidentEdgeOrder));
                return arbitraryGraphsWithFixedFlags
                    .tuple2()
                    .map(t -> new TwoGraphs(t.get1(), t.get2()));
              });
    }
  }

  record TwoMutableGraphsAndNode(
      MutableGraph<Integer> firstGraph, MutableGraph<Integer> secondGraph, Integer node) {}

  static class TwoMutableGraphsWithSameFlagsAndNodeFromFirst
      implements ArbitrarySupplier<TwoMutableGraphsAndNode> {
    @Override
    public Arbitrary<TwoMutableGraphsAndNode> get() {
      return twoGraphsWithSameFlagsAndDisjointedNodes()
          .filter(not(twoGraphs -> twoGraphs.first().nodes().isEmpty()))
          .flatMap(
              twoGraphs ->
                  Arbitraries.of(twoGraphs.first().nodes())
                      .map(
                          node -> {
                            MutableGraph<Integer> first =
                                com.google.common.graph.Graphs.copyOf(twoGraphs.first());
                            MutableGraph<Integer> second =
                                com.google.common.graph.Graphs.copyOf(twoGraphs.second());
                            return new TwoMutableGraphsAndNode(first, second, node);
                          }));
    }
  }

  static class TwoMutableGraphsWithSameFlagsAndNodeFromSecond
      implements ArbitrarySupplier<TwoMutableGraphsAndNode> {
    @Override
    public Arbitrary<TwoMutableGraphsAndNode> get() {
      return twoGraphsWithSameFlagsAndDisjointedNodes()
          .filter(not(twoGraphs -> twoGraphs.second().nodes().isEmpty()))
          .flatMap(
              twoGraphs ->
                  Arbitraries.of(twoGraphs.second().nodes())
                      .map(
                          node -> {
                            MutableGraph<Integer> first =
                                com.google.common.graph.Graphs.copyOf(twoGraphs.first());
                            MutableGraph<Integer> second =
                                com.google.common.graph.Graphs.copyOf(twoGraphs.second());
                            return new TwoMutableGraphsAndNode(first, second, node);
                          }));
    }
  }

  private static Arbitrary<TwoGraphs> twoGraphsWithSameFlagsAndDisjointedNodes() {
    Arbitrary<Boolean> arbitraryIsDirected = booleans();
    Arbitrary<Boolean> arbitraryAllowsSelfLoops = booleans();

    return Combinators.combine(
            arbitraryIsDirected,
            arbitraryAllowsSelfLoops,
            nodes(),
            nodeOrders(),
            incidentEdgeOrders())
        .flatAs(
            (isDirected, allowsSelfLoops, nodes, nodeOrder, incidentEdgeOrder) -> {
              requireNonNull(isDirected);
              requireNonNull(allowsSelfLoops);
              requireNonNull(nodes);
              requireNonNull(nodeOrder);
              requireNonNull(incidentEdgeOrder);
              var nodesASize = Arbitraries.integers().between(0, nodes.size());
              var splitNodes = nodesASize.map(n -> splitAtIndex(nodes, n));
              return splitNodes.flatMap(
                  sets ->
                      twoGraphs(
                          isDirected,
                          isDirected,
                          allowsSelfLoops,
                          allowsSelfLoops,
                          sets.get1(),
                          sets.get2(),
                          nodeOrder,
                          nodeOrder,
                          incidentEdgeOrder,
                          incidentEdgeOrder));
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
    return Tuple.of(Set.copyOf(nodesA), Set.copyOf(nodesB));
  }

  static class TwoMutableGraphsWithSameFlagsAndCommonNode
      implements ArbitrarySupplier<TwoMutableGraphsAndNode> {
    @Override
    public Arbitrary<TwoMutableGraphsAndNode> get() {
      return Combinators.combine(
              booleans(),
              booleans(),
              MoreArbitraries.nodes(),
              MoreArbitraries.nodes(),
              Arbitraries.integers(),
              MoreArbitraries.nodeOrders(),
              MoreArbitraries.incidentEdgeOrders())
          .flatAs(
              (isDirected,
                  allowsSelfLoops,
                  nodesA,
                  nodesB,
                  commonNode,
                  nodeOrder,
                  incidentEdgeOrder) -> {
                requireNonNull(isDirected);
                requireNonNull(allowsSelfLoops);
                requireNonNull(nodesA);
                requireNonNull(nodesB);
                requireNonNull(commonNode);
                requireNonNull(nodeOrder);
                requireNonNull(incidentEdgeOrder);
                return Combinators.combine(
                        MoreArbitraries.graphs(
                            isDirected,
                            allowsSelfLoops,
                            union(nodesA, Set.of(commonNode)),
                            nodeOrder,
                            incidentEdgeOrder),
                        MoreArbitraries.graphs(
                            isDirected,
                            allowsSelfLoops,
                            union(nodesB, Set.of(commonNode)),
                            nodeOrder,
                            incidentEdgeOrder),
                        just(commonNode))
                    .as(
                        (a, b, node) -> {
                          requireNonNull(a);
                          requireNonNull(b);
                          requireNonNull(node);
                          MutableGraph<Integer> first = com.google.common.graph.Graphs.copyOf(a);
                          MutableGraph<Integer> second = com.google.common.graph.Graphs.copyOf(b);
                          return new TwoMutableGraphsAndNode(first, second, node);
                        });
              });
    }
  }

  static SetArbitrary<Integer> nodes() {
    return nodes(MIN_NODES_COUNT_FOR_ANY_GRAPH);
  }

  static SetArbitrary<Integer> nodes(int minNodesCount) {
    return Arbitraries.integers()
        .between(SMALLEST_NODE, LARGEST_NODE)
        .set()
        .ofMinSize(minNodesCount)
        .ofMaxSize(MoreArbitraries.MAX_NODES_COUNT_FOR_ANY_GRAPH);
  }

  record TwoElementOrders(ElementOrder<Integer> first, ElementOrder<Integer> second) {}

  private static final Set<ElementOrder<Integer>> NODE_ORDERS =
      Set.of(
          ElementOrder.natural(),
          ElementOrder.unordered(),
          ElementOrder.stable(),
          ElementOrder.sorted(Comparator.<Integer>reverseOrder()));

  private static final Arbitrary<ElementOrder<Integer>> ARBITRARY_NODE_ORDERS =
      Arbitraries.of(NODE_ORDERS);

  static Arbitrary<ElementOrder<Integer>> nodeOrders() {
    return ARBITRARY_NODE_ORDERS;
  }

  static Arbitrary<TwoElementOrders> twoDifferentNodeOrders() {
    return MoreArbitraries.nodeOrders()
        .flatMap(
            nodeOrder ->
                Combinators.combine(
                        just(nodeOrder), Arbitraries.of(difference(NODE_ORDERS, Set.of(nodeOrder))))
                    .as(
                        (first, second) ->
                            new TwoElementOrders(requireNonNull(first), requireNonNull(second))));
  }

  static class TwoDifferentNodeOrders implements ArbitrarySupplier<TwoElementOrders> {
    @Override
    public Arbitrary<TwoElementOrders> get() {
      return twoDifferentNodeOrders();
    }
  }

  static Arbitrary<ElementOrder<Integer>> incidentEdgeOrders() {
    return Arbitraries.of(ElementOrder.stable(), ElementOrder.unordered());
  }

  static Arbitrary<TwoElementOrders> twoDifferentIncidentEdgeOrders() {
    return Arbitraries.of(
        new TwoElementOrders(ElementOrder.stable(), ElementOrder.unordered()),
        new TwoElementOrders(ElementOrder.unordered(), ElementOrder.stable()));
  }

  private static final Arbitrary<Boolean> ARBITRARY_BOOLEANS =
      Arbitraries.of(Boolean.TRUE, Boolean.FALSE);

  static Arbitrary<Boolean> booleans() {
    return ARBITRARY_BOOLEANS;
  }

  private MoreArbitraries() {}
}
