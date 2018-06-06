package giraaff.hotspot.nodes.aot;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class ResolveDynamicConstantNode
public final class ResolveDynamicConstantNode extends DeoptimizingFixedWithNextNode implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<ResolveDynamicConstantNode> TYPE = NodeClass.create(ResolveDynamicConstantNode.class);

    @Node.Input
    // @field
    ValueNode ___value;

    // @cons ResolveDynamicConstantNode
    public ResolveDynamicConstantNode(Stamp __valueStamp, ValueNode __value)
    {
        super(TYPE, __valueStamp);
        this.___value = __value;
    }

    public ValueNode value()
    {
        return this.___value;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
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
