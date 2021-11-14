package in.bytehue.osgifx.console.smartgraph.graphview;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method annotation to override an element's label provider.
 *
 * The annotated method must return a value, otherwise the a reflection
 * exception will be thrown.
 *
 * By default the text label is obtained from the toString method if this
 * annotation is not present in any other class method; this is also the case
 * with String and other boxed-types, e.g., Integer, Double, etc.
 *
 * If multiple annotations exist, the behavior is undefined.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SmartLabelSource {

}
