package giraaff.hotspot.nodes.aot;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @NodeInfo.allowedUsageTypes "Memory"
// @class InitializeKlassNode
public final class InitializeKlassNode extends DeoptimizingFixedWithNextNode implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<InitializeKlassNode> TYPE = NodeClass.create(InitializeKlassNode.class);

    @Input
    // @field
    ValueNode ___value;

    // @cons
    public InitializeKlassNode(ValueNode __value)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.___value = __value;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public ValueNode value()
    {
        return this.___value;
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
