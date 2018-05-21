package graalvm.compiler.nodes.virtual;

import static graalvm.compiler.nodeinfo.InputType.Extension;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.ArrayLengthProvider;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;

/**
 * Selects one object from a {@link CommitAllocationNode}. The object is identified by its
 * {@link VirtualObjectNode}.
 */
public final class AllocatedObjectNode extends FloatingNode implements Virtualizable, ArrayLengthProvider
{
    public static final NodeClass<AllocatedObjectNode> TYPE = NodeClass.create(AllocatedObjectNode.class);
    @Input VirtualObjectNode virtualObject;
    @Input(Extension) CommitAllocationNode commit;

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
