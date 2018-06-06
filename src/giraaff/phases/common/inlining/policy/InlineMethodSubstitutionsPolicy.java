package giraaff.phases.common.inlining.policy;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.CallTargetNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.walker.MethodInvocation;

///
// Inline every method which would be replaced by a substitution. Useful for testing purposes.
///
// @class InlineMethodSubstitutionsPolicy
public final class InlineMethodSubstitutionsPolicy extends InlineEverythingPolicy
{
    @Override
    public InliningPolicy.Decision isWorthInlining(Replacements __replacements, MethodInvocation __invocation, int __inliningDepth, boolean __fullyProcessed)
    {
        CallTargetNode __callTarget = __invocation.callee().invoke().callTarget();
        if (__callTarget instanceof MethodCallTargetNode)
        {
            ResolvedJavaMethod __calleeMethod = ((MethodCallTargetNode) __callTarget).targetMethod();
            if (__replacements.hasSubstitution(__calleeMethod, __invocation.callee().invoke().bci()))
            {
                return InliningPolicy.Decision.YES;
            }
        }
        return InliningPolicy.Decision.NO;
    }
}
