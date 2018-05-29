package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * A node that results in a platform dependent pause instruction being emitted.
 */
// @class PauseNode
public final class PauseNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<PauseNode> TYPE = NodeClass.create(PauseNode.class);

    // @cons
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
