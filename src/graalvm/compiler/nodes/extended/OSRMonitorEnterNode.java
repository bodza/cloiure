package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.MonitorEnterNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.VirtualizerTool;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public class OSRMonitorEnterNode extends MonitorEnterNode implements LIRLowerable {

    public static final NodeClass<OSRMonitorEnterNode> TYPE = NodeClass.create(OSRMonitorEnterNode.class);

    public OSRMonitorEnterNode(ValueNode object, MonitorIdNode monitorId) {
        super(TYPE, object, monitorId);
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        // OSR Entry cannot be virtualized
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // Nothing to do
    }

    @Override
    public void lower(LoweringTool tool) {
        /*
         * Nothing to do for OSR compilations with locks the monitor enter operation already
         * happened when we do the compilation.
         */
    }
}
