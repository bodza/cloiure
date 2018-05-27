package giraaff.hotspot.nodes.aot;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @NodeInfo.allowedUsageTypes "Memory"
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
