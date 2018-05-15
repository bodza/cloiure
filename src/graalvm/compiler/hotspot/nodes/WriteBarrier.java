package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

@NodeInfo
public abstract class WriteBarrier extends FixedWithNextNode implements Lowerable {

    public static final NodeClass<WriteBarrier> TYPE = NodeClass.create(WriteBarrier.class);

    protected WriteBarrier(NodeClass<? extends WriteBarrier> c) {
        super(c, StampFactory.forVoid());
    }

    @Override
    public void lower(LoweringTool tool) {
        assert graph().getGuardsStage().areFrameStatesAtDeopts();
        tool.getLowerer().lower(this, tool);
    }
}
