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
public final class AllocatedObjectNode extends FloatingNode implements Virtualizable, ArrayLengthProvider
{
    public static final NodeClass<AllocatedObjectNode> TYPE = NodeClass.create(AllocatedObjectNode.class);
    @Input VirtualObjectNode virtualObject;
    @Input(InputType.Extension) CommitAllocationNode commit;

    public AllocatedObjectNode(VirtualObjectNode virtualObject)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(virtualObject.type())));
        this.virtualObject = virtualObject;
    }

    public VirtualObjectNode getVirtualObject()
    {
        return virtualObject;
    }

    public CommitAllocationNode getCommit()
    {
        return commit;
    }

    public void setCommit(CommitAllocationNode x)
    {
        updateUsages(commit, x);
        commit = x;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        tool.replaceWithVirtual(getVirtualObject());
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
