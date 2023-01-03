package me.jbduncan.guavagraphutils;

import com.google.common.graph.ImmutableGraph;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;

final class DotToStringImmutableGraphArgumentConverter extends SimpleArgumentConverter {
  @Override
  protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
    if (!String.class.isAssignableFrom(source.getClass())) {
      throw new ArgumentConversionException(
          "Can only convert objects of type "
              + String.class
              + ", but was given object of type "
              + source.getClass());
    }
    String dotString = (String) source;

    if (!ImmutableGraph.class.isAssignableFrom(targetType)) {
      throw new ArgumentConversionException(
          "Can only convert to "
              + ImmutableGraph.class
              + " but was given object of type "
              + targetType);
    }

    return DotImporter.importGraph(dotString);
  }
}
