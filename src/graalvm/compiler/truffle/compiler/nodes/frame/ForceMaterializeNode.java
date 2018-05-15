package graalvm.compiler.truffle.compiler.nodes.frame;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class ForceMaterializeNode extends FixedWithNextNode implements LIRLowerable {
    public static final NodeClass<ForceMaterializeNode> TYPE = NodeClass.create(ForceMaterializeNode.class);

    @Input ValueNode object;

    public ForceMaterializeNode(ValueNode object) {
        super(TYPE, StampFactory.forVoid());
        this.object = object;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // nothing to do
    }

    @NodeIntrinsic
    public static native void force(Object object);
}
