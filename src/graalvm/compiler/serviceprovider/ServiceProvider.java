package graalvm.compiler.serviceprovider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a service provider that will have a provider file generated by an annotation processor.
 * For a service defined by {@code S} and a class {@code P} implementing the service, the processor
 * generates the file {@code META-INF/providers/P} whose contents are a single line containing the
 * fully qualified name of {@code S}.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ServiceProvider {

    /**
     * The interface or class defining the service implemented by the annotated class.
     */
    Class<?> value();
}