package graalvm.compiler.hotspot.nodes.aot;

import static graalvm.compiler.nodeinfo.InputType.Memory;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.word.LocationIdentity;

public class InitializeKlassNode extends DeoptimizingFixedWithNextNode implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<InitializeKlassNode> TYPE = NodeClass.create(InitializeKlassNode.class);

    @Input ValueNode value;

    public InitializeKlassNode(ValueNode value)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public ValueNode value()
    {
        return value;
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
