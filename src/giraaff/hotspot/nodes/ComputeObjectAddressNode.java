package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.debug.ControlFlowAnchored;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// A high-level intrinsic for getting an address inside of an object. During lowering it will be
// moved next to any uses to avoid creating a derived pointer that is live across a safepoint.
///
// @class ComputeObjectAddressNode
public final class ComputeObjectAddressNode extends FixedWithNextNode implements Lowerable, ControlFlowAnchored
{
    // @def
    public static final NodeClass<ComputeObjectAddressNode> TYPE = NodeClass.create(ComputeObjectAddressNode.class);

    @Input
    // @field
    ValueNode ___object;
    @Input
    // @field
    ValueNode ___offset;

    // @cons
    public ComputeObjectAddressNode(ValueNode __obj, ValueNode __offset)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Long));
        this.___object = __obj;
        this.___offset = __offset;
    }

    @NodeIntrinsic
    public static native long get(Object __array, long __offset);

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public ValueNode getObject()
    {
        return this.___object;
    }

    public ValueNode getOffset()
    {
        return this.___offset;
    }
}
