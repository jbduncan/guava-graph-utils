package me.jbduncan.guavagraphutils;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import java.io.StringReader;
import org.jgrapht.graph.guava.MutableGraphAdapter;
import org.jgrapht.nio.dot.DOTImporter;

public final class DotImporter {

  public static ImmutableGraph<String> importGraph(String dotString) {
    MutableGraph<String> importedGraph = GraphBuilder.directed().build();
    var dotImporter = new DOTImporter<String, EndpointPair<String>>();
    dotImporter.setVertexFactory(vertex -> vertex);
    dotImporter.setVertexWithAttributesFactory((vertex, attributes) -> vertex);
    dotImporter.importGraph(new MutableGraphAdapter<>(importedGraph), new StringReader(dotString));
    return ImmutableGraph.copyOf(importedGraph);
  }

  private DotImporter() {}
}
