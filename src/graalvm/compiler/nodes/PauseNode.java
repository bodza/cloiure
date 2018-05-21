package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * A node that results in a platform dependent pause instruction being emitted.
 */
public final class PauseNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<PauseNode> TYPE = NodeClass.create(PauseNode.class);

    public PauseNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().emitPause();
    }

    @NodeIntrinsic
    public static native void pause();
}
