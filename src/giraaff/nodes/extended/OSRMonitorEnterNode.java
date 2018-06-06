package giraaff.nodes.extended;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MonitorEnterNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.VirtualizerTool;

// @class OSRMonitorEnterNode
public final class OSRMonitorEnterNode extends MonitorEnterNode implements LIRLowerable
{
    // @def
    public static final NodeClass<OSRMonitorEnterNode> TYPE = NodeClass.create(OSRMonitorEnterNode.class);

    // @cons OSRMonitorEnterNode
    public OSRMonitorEnterNode(ValueNode __object, MonitorIdNode __monitorId)
    {
        super(TYPE, __object, __monitorId);
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        // OSR Entry cannot be virtualized
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // nothing to do
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        // Nothing to do for OSR compilations with locks the monitor enter operation already
        // happened when we do the compilation.
    }
}
