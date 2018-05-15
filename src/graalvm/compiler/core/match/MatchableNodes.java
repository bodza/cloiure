package graalvm.compiler.core.match;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The repeatable representation of {@link MatchableNode}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MatchableNodes {
    MatchableNode[] value() default {};
}
