# guava-graph-utils
This is a Java library with a small number of utilities for working with Guava `Graph`s. Specifically:
- `MoreGraphs#buildGraphWithBreadthFirstTraversal`: builds an `ImmutableGraph` from a set of starting nodes and a "successors function". The successors function is applied in a breadth-firstmanner to the starting nodes, then their children, then their grand-children, and so on and so forth until all descendants have been traversed.
- `MoreGraphs#asValueGraph(Table)`: wraps a Guava `Table` as a `ValueGraph`.
