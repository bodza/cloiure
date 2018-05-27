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
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Node for a {@linkplain ForeignCallDescriptor foreign} call from within a stub.
 */
// @NodeInfo.allowedUsageTypes "Memory"
public final class StubForeignCallNode extends FixedWithNextNode implements LIRLowerable, MemoryCheckpoint.Multi
{
    public static final NodeClass<StubForeignCallNode> TYPE = NodeClass.create(StubForeignCallNode.class);
    @Input NodeInputList<ValueNode> arguments;
    protected final ForeignCallsProvider foreignCalls;

    protected final ForeignCallDescriptor descriptor;

    public StubForeignCallNode(@InjectedNodeParameter ForeignCallsProvider foreignCalls, @InjectedNodeParameter Stamp stamp, ForeignCallDescriptor descriptor, ValueNode... arguments)
    {
        super(TYPE, stamp);
        this.arguments = new NodeInputList<>(this, arguments);
        this.descriptor = descriptor;
        this.foreignCalls = foreignCalls;
    }

    public ForeignCallDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public LocationIdentity[] getLocationIdentities()
    {
        LocationIdentity[] killedLocations = foreignCalls.getKilledLocations(descriptor);
        killedLocations = Arrays.copyOf(killedLocations, killedLocations.length + 1);
        killedLocations[killedLocations.length - 1] = HotSpotReplacementsUtil.PENDING_EXCEPTION_LOCATION;
        return killedLocations;
    }

    protected Value[] operands(NodeLIRBuilderTool gen)
    {
        Value[] operands = new Value[arguments.size()];
        for (int i = 0; i < operands.length; i++)
        {
            operands[i] = gen.operand(arguments.get(i));
        }
        return operands;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        ForeignCallLinkage linkage = foreignCalls.lookupForeignCall(descriptor);
        Value[] operands = operands(gen);
        Value result = gen.getLIRGeneratorTool().emitForeignCall(linkage, null, operands);
        if (result != null)
        {
            gen.setResult(this, result);
        }
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Name)
        {
            return super.toString(verbosity) + "#" + descriptor;
        }
        return super.toString(verbosity);
    }
}
