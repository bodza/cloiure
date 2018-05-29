package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.ForeignCallNode;

/**
 * {@link InvocationPlugin} for converting a method call directly to a foreign call.
 */
// @class ForeignCallPlugin
public final class ForeignCallPlugin implements InvocationPlugin
{
    private final ForeignCallsProvider foreignCalls;
    private final ForeignCallDescriptor descriptor;

    // @cons
    public ForeignCallPlugin(ForeignCallsProvider foreignCalls, ForeignCallDescriptor descriptor)
    {
        super();
        this.foreignCalls = foreignCalls;
        this.descriptor = descriptor;
    }

    @Override
    public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args)
    {
        ForeignCallNode foreignCall = new ForeignCallNode(foreignCalls, descriptor, args);
        foreignCall.setBci(b.bci());
        b.addPush(targetMethod.getSignature().getReturnKind(), foreignCall);
        return true;
    }
}
