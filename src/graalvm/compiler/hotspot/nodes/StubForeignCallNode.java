package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.InputType.Memory;

import java.util.Arrays;

import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.common.spi.ForeignCallLinkage;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.Value;

/**
 * Node for a {@linkplain ForeignCallDescriptor foreign} call from within a stub.
 */
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
