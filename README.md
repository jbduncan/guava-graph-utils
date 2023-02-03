# guava-graph-utils
This is a Java library with a few utilities for working with Guava graphs. Specifically:
- [`MoreGraphs.buildGraphWithBreadthFirstTraversal`](src/main/java/me/jbduncan/guavagraphutils/MoreGraphs.java):
  builds an `ImmutableGraph` from a set of starting nodes and a "successors function". The
  successors function is applied in a breadth-first manner to the starting nodes, then their
  children, then their grand-children, and so on and so forth until all descendants have been
  traversed.
- [`MoreGraphs.asValueGraph`](src/main/java/me/jbduncan/guavagraphutils/MoreGraphs.java): wraps a Guava `Table` as
  a `ValueGraph`.
  
See these methods' javadocs for more information.
