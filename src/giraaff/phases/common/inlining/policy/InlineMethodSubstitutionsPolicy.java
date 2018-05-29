package giraaff.phases.common.inlining.policy;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.CallTargetNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.walker.MethodInvocation;

/**
 * Inline every method which would be replaced by a substitution. Useful for testing purposes.
 */
// @class InlineMethodSubstitutionsPolicy
public final class InlineMethodSubstitutionsPolicy extends InlineEverythingPolicy
{
    @Override
    public Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed)
    {
        CallTargetNode callTarget = invocation.callee().invoke().callTarget();
        if (callTarget instanceof MethodCallTargetNode)
        {
            ResolvedJavaMethod calleeMethod = ((MethodCallTargetNode) callTarget).targetMethod();
            if (replacements.hasSubstitution(calleeMethod, invocation.callee().invoke().bci()))
            {
                return Decision.YES;
            }
        }
        return Decision.NO;
    }
}
