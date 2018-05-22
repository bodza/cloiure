package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.debug.ControlFlowAnchored;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * A high-level intrinsic for getting an address inside of an object. During lowering it will be
 * moved next to any uses to avoid creating a derived pointer that is live across a safepoint.
 */
public final class ComputeObjectAddressNode extends FixedWithNextNode implements Lowerable, ControlFlowAnchored
{
    public static final NodeClass<ComputeObjectAddressNode> TYPE = NodeClass.create(ComputeObjectAddressNode.class);

    @Input ValueNode object;
    @Input ValueNode offset;

    public ComputeObjectAddressNode(ValueNode obj, ValueNode offset)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Long));
        this.object = obj;
        this.offset = offset;
    }

    @NodeIntrinsic
    public static native long get(Object array, long offset);

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public ValueNode getObject()
    {
        return object;
    }

    public ValueNode getOffset()
    {
        return offset;
    }
}
