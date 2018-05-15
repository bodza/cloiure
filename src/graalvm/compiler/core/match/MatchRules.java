package graalvm.compiler.core.match;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The repeatable representation of {@link MatchRule}. Should never be used directly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MatchRules {
    MatchRule[] value();
}
