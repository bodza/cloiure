package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Marks the control flow path where an object acquired a biased lock because the lock was already
 * biased to the object on the current thread.
 */
// @class FastAcquireBiasedLockNode
public final class FastAcquireBiasedLockNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<FastAcquireBiasedLockNode> TYPE = NodeClass.create(FastAcquireBiasedLockNode.class);

    @Input ValueNode object;

    // @cons
    public FastAcquireBiasedLockNode(ValueNode object)
    {
        super(TYPE, StampFactory.forVoid());
        this.object = object;
    }

    public ValueNode object()
    {
        return object;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // this is just a marker node, so it generates nothing
    }

    @NodeIntrinsic
    public static native void mark(Object object);
}
