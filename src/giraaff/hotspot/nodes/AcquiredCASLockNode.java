package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Marks the control flow path where an object acquired a lightweight lock based on an atomic
// compare-and-swap (CAS) of the mark word in the object's header.
///
// @class AcquiredCASLockNode
public final class AcquiredCASLockNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<AcquiredCASLockNode> TYPE = NodeClass.create(AcquiredCASLockNode.class);

    @Input
    // @field
    ValueNode ___object;

    // @cons
    public AcquiredCASLockNode(ValueNode __object)
    {
        super(TYPE, StampFactory.forVoid());
        this.___object = __object;
    }

    public ValueNode object()
    {
        return this.___object;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // this is just a marker node, so it generates nothing
    }

    @NodeIntrinsic
    public static native void mark(Object __object);
}
