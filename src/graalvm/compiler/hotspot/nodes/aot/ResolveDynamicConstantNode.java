package graalvm.compiler.hotspot.nodes.aot;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_4;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_16;

import org.graalvm.word.LocationIdentity;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

@NodeInfo(cycles = CYCLES_4, size = SIZE_16, allowedUsageTypes = {InputType.Memory})
public class ResolveDynamicConstantNode extends DeoptimizingFixedWithNextNode implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<ResolveDynamicConstantNode> TYPE = NodeClass.create(ResolveDynamicConstantNode.class);

    @Input ValueNode value;

    public ResolveDynamicConstantNode(Stamp valueStamp, ValueNode value)
    {
        super(TYPE, valueStamp);
        this.value = value;
    }

    public ValueNode value()
    {
        return value;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }
}
