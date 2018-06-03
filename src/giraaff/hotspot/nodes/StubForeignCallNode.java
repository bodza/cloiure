package giraaff.hotspot.nodes;

import java.util.Arrays;

import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Node for a {@linkplain ForeignCallDescriptor foreign} call from within a stub.
///
// @NodeInfo.allowedUsageTypes "Memory"
// @class StubForeignCallNode
public final class StubForeignCallNode extends FixedWithNextNode implements LIRLowerable, MemoryCheckpoint.Multi
{
    // @def
    public static final NodeClass<StubForeignCallNode> TYPE = NodeClass.create(StubForeignCallNode.class);

    @Input
    // @field
    NodeInputList<ValueNode> ___arguments;
    // @field
    protected final ForeignCallsProvider ___foreignCalls;

    // @field
    protected final ForeignCallDescriptor ___descriptor;

    // @cons
    public StubForeignCallNode(@InjectedNodeParameter ForeignCallsProvider __foreignCalls, @InjectedNodeParameter Stamp __stamp, ForeignCallDescriptor __descriptor, ValueNode... __arguments)
    {
        super(TYPE, __stamp);
        this.___arguments = new NodeInputList<>(this, __arguments);
        this.___descriptor = __descriptor;
        this.___foreignCalls = __foreignCalls;
    }

    public ForeignCallDescriptor getDescriptor()
    {
        return this.___descriptor;
    }

    @Override
    public LocationIdentity[] getLocationIdentities()
    {
        LocationIdentity[] __killedLocations = this.___foreignCalls.getKilledLocations(this.___descriptor);
        __killedLocations = Arrays.copyOf(__killedLocations, __killedLocations.length + 1);
        __killedLocations[__killedLocations.length - 1] = HotSpotReplacementsUtil.PENDING_EXCEPTION_LOCATION;
        return __killedLocations;
    }

    protected Value[] operands(NodeLIRBuilderTool __gen)
    {
        Value[] __operands = new Value[this.___arguments.size()];
        for (int __i = 0; __i < __operands.length; __i++)
        {
            __operands[__i] = __gen.operand(this.___arguments.get(__i));
        }
        return __operands;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        ForeignCallLinkage __linkage = this.___foreignCalls.lookupForeignCall(this.___descriptor);
        Value[] __operands = operands(__gen);
        Value __result = __gen.getLIRGeneratorTool().emitForeignCall(__linkage, null, __operands);
        if (__result != null)
        {
            __gen.setResult(this, __result);
        }
    }
}
