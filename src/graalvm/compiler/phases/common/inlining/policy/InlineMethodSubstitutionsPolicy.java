package graalvm.compiler.phases.common.inlining.policy;

import graalvm.compiler.nodes.CallTargetNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.phases.common.inlining.walker.MethodInvocation;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Inline every method which would be replaced by a substitution. Useful for testing purposes.
 */
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
