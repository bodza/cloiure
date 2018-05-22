package giraaff.core.match;

import jdk.vm.ci.meta.Value;

import giraaff.core.gen.NodeLIRBuilder;

/**
 * A closure that can be evaluated to produce the LIR for some complex match. Using a closure allows
 * normal evaluation in NodeLIRBuilder for all the simple nodes with the complex nodes evaluated at
 * the proper time.
 */
public interface ComplexMatchResult
{
    Value evaluate(NodeLIRBuilder gen);
}
