package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.ForeignCallNode;

///
// {@link InvocationPlugin} for converting a method call directly to a foreign call.
///
// @class ForeignCallPlugin
public final class ForeignCallPlugin implements InvocationPlugin
{
    // @field
    private final ForeignCallsProvider ___foreignCalls;
    // @field
    private final ForeignCallDescriptor ___descriptor;

    // @cons
    public ForeignCallPlugin(ForeignCallsProvider __foreignCalls, ForeignCallDescriptor __descriptor)
    {
        super();
        this.___foreignCalls = __foreignCalls;
        this.___descriptor = __descriptor;
    }

    @Override
    public boolean execute(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode[] __args)
    {
        ForeignCallNode __foreignCall = new ForeignCallNode(this.___foreignCalls, this.___descriptor, __args);
        __foreignCall.setBci(__b.bci());
        __b.addPush(__targetMethod.getSignature().getReturnKind(), __foreignCall);
        return true;
    }
}
