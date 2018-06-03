package giraaff.nodes.virtual;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;

///
// Selects one object from a {@link CommitAllocationNode}. The object is identified by its
// {@link VirtualObjectNode}.
///
// @class AllocatedObjectNode
public final class AllocatedObjectNode extends FloatingNode implements Virtualizable, ArrayLengthProvider
{
    // @def
    public static final NodeClass<AllocatedObjectNode> TYPE = NodeClass.create(AllocatedObjectNode.class);

    @Input
    // @field
    VirtualObjectNode ___virtualObject;
    @Input(InputType.Extension)
    // @field
    CommitAllocationNode ___commit;

    // @cons
    public AllocatedObjectNode(VirtualObjectNode __virtualObject)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(__virtualObject.type())));
        this.___virtualObject = __virtualObject;
    }

    public VirtualObjectNode getVirtualObject()
    {
        return this.___virtualObject;
    }

    public CommitAllocationNode getCommit()
    {
        return this.___commit;
    }

    public void setCommit(CommitAllocationNode __x)
    {
        updateUsages(this.___commit, __x);
        this.___commit = __x;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        __tool.replaceWithVirtual(getVirtualObject());
    }

    @Override
    public ValueNode length()
    {
        if (this.___virtualObject instanceof ArrayLengthProvider)
        {
            return ((ArrayLengthProvider) this.___virtualObject).length();
        }
        return null;
    }
}
