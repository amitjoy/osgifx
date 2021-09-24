package in.bytehue.osgifx.console.propertytypes;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * Component Property Type for the {@code main.thread} service property.
 * <p>
 * This annotation can be used on a {@link Component} to declare the value of
 * the {@code main.thread} service property.
 *
 * @see "Component Property Types"
 */
@Target(TYPE)
@Retention(CLASS)
@ComponentPropertyType
public @interface MainThread {
    String value() default "true";
}
