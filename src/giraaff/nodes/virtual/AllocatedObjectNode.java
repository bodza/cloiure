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

/**
 * Selects one object from a {@link CommitAllocationNode}. The object is identified by its
 * {@link VirtualObjectNode}.
 */
// @class AllocatedObjectNode
public final class AllocatedObjectNode extends FloatingNode implements Virtualizable, ArrayLengthProvider
{
    // @def
    public static final NodeClass<AllocatedObjectNode> TYPE = NodeClass.create(AllocatedObjectNode.class);

    @Input
    // @field
    VirtualObjectNode virtualObject;
    @Input(InputType.Extension)
    // @field
    CommitAllocationNode commit;

    // @cons
    public AllocatedObjectNode(VirtualObjectNode __virtualObject)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(__virtualObject.type())));
        this.virtualObject = __virtualObject;
    }

    public VirtualObjectNode getVirtualObject()
    {
        return virtualObject;
    }

    public CommitAllocationNode getCommit()
    {
        return commit;
    }

    public void setCommit(CommitAllocationNode __x)
    {
        updateUsages(commit, __x);
        commit = __x;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        __tool.replaceWithVirtual(getVirtualObject());
    }

    @Override
    public ValueNode length()
    {
        if (virtualObject instanceof ArrayLengthProvider)
        {
            return ((ArrayLengthProvider) virtualObject).length();
        }
        return null;
    }
}
