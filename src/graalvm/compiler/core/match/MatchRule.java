package graalvm.compiler.core.match;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import graalvm.compiler.nodes.ConstantNode;

/**
 * This annotation declares a textual pattern for matching an HIR tree. The format is a LISP style
 * s-expression with node types and/or names that are matched against the HIR. Node types are always
 * uppercase and the names of nodes are always lowercase. Named nodes can be used to match trees
 * where a node is used multiple times but only as an input to the full match.
 *
 * <pre>
 *   &lt;node-name&gt;    := [a-z][a-zA-Z0-9]*
 *   &lt;node-type&gt;    := [A-Z][a-zA-Z0-9]*
 *   &lt;node-spec&gt;    := &lt;node-type&gt; { '=' &lt;node-name&gt; }
 *   &lt;node-or-name&gt; := &lt;node-spec&gt; | &lt;node-name&gt;
 *   &lt;argument&gt;     := &lt;node-or-name&gt; | &lt;match-rule&gt;
 *   &lt;match-rule&gt;   := '(' &lt;node-spec&gt; &lt;argument&gt;+ ')'
 * </pre>
 *
 * All matched nodes except the root of the match and {@link ConstantNode}s must have a single user.
 * All matched nodes must be in the same block.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(value = MatchRules.class)
public @interface MatchRule {
    String value();
}
