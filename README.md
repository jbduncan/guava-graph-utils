# guava-graph-utils

This is a Java library with a few utilities for working with Guava graphs. Specifically:

- [`MoreGraphs.buildGraph`](src/main/java/com/github/jbduncan/guavagraphutils/MoreGraphs.java): builds
  an `ImmutableGraph` from a set of starting nodes and a "successors function". The successors function is applied in a
  breadth-first manner to the starting nodes, then their children, then their grand-children, and so on and so forth
  until all descendants have been traversed.
- [`MoreGraphs.asValueGraph`](src/main/java/com/github/jbduncan/guavagraphutils/MoreGraphs.java): wraps a Guava `Table`
  as a `ValueGraph`.
- [`MoreGraphs.topologicalOrdering`](src/main/java/com/github/jbduncan/guavagraphutils/MoreGraphs.java): returns a
  topological ordering of the given graph; that is, a traversal of the graph in which each node is visited only after
  all its predecessors and other ancestors have been visited.
- [`MoreGraphs.lazyTopologicalOrdering`](src/main/java/com/github/jbduncan/guavagraphutils/MoreGraphs.java): returns a
  lazy view of the topological ordering of the given graph.
- [`MoreGraphs.topologicalOrderingStartingFrom`](src/main/java/com/github/jbduncan/guavagraphutils/MoreGraphs.java):
  returns a topological ordering of the subgraph of the given graph that starts from the given nodes.
- [`MoreGraphs.union`](src/main/java/com/github/jbduncan/guavagraphutils/MoreGraphs.java): returns a view of the union
  of the given graphs.
- [`MoreGraphs.pageRanks`](): returns the page ranks of all nodes of the given graph as per
  the [PageRank](https://en.wikipedia.org/wiki/PageRank) algorithm.

See these methods' javadocs for more information.
