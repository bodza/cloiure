package giraaff.nodes.extended;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MonitorEnterNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.VirtualizerTool;

public class OSRMonitorEnterNode extends MonitorEnterNode implements LIRLowerable
{
    public static final NodeClass<OSRMonitorEnterNode> TYPE = NodeClass.create(OSRMonitorEnterNode.class);

    public OSRMonitorEnterNode(ValueNode object, MonitorIdNode monitorId)
    {
        super(TYPE, object, monitorId);
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        // OSR Entry cannot be virtualized
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        // Nothing to do
    }

    @Override
    public void lower(LoweringTool tool)
    {
        /*
         * Nothing to do for OSR compilations with locks the monitor enter operation already
         * happened when we do the compilation.
         */
    }
}
