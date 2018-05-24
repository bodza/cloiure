package giraaff.nodes.virtual;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.VerificationError;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.StoreFieldNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;

public final class EnsureVirtualizedNode extends FixedWithNextNode implements Virtualizable, Lowerable
{
    public static final NodeClass<EnsureVirtualizedNode> TYPE = NodeClass.create(EnsureVirtualizedNode.class);

    @Input ValueNode object;
    private final boolean localOnly;

    public EnsureVirtualizedNode(ValueNode object, boolean localOnly)
    {
        super(TYPE, StampFactory.forVoid());
        this.object = object;
        this.localOnly = localOnly;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(object);
        if (alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            if (virtual instanceof VirtualBoxingNode)
            {
                Throwable exception = new VerificationError("ensureVirtual is not valid for boxing objects: %s", virtual.type().getName());
                throw GraphUtil.approxSourceException(this, exception);
            }
            if (!localOnly)
            {
                tool.setEnsureVirtualized(virtual, true);
            }
            tool.delete();
        }
    }

    @Override
    public void lower(LoweringTool tool)
    {
        ensureVirtualFailure(this, object.stamp(NodeView.DEFAULT));
    }

    public static void ensureVirtualFailure(Node location, Stamp stamp)
    {
        String additionalReason = "";
        if (location instanceof FixedWithNextNode && !(location instanceof EnsureVirtualizedNode))
        {
            FixedWithNextNode fixedWithNextNode = (FixedWithNextNode) location;
            FixedNode next = fixedWithNextNode.next();
            if (next instanceof StoreFieldNode)
            {
                additionalReason = " (must not store virtual object into a field)";
            }
            else if (next instanceof Invoke)
            {
                additionalReason = " (must not pass virtual object into an invoke that cannot be inlined)";
            }
            else
            {
                additionalReason = " (must not let virtual object escape at node " + next + ")";
            }
        }
        Throwable exception = new VerificationError("Object of type %s should not be materialized%s:", StampTool.typeOrNull(stamp).getName(), additionalReason);

        Node pos;
        if (location instanceof FixedWithNextNode)
        {
            pos = ((FixedWithNextNode) location).next();
        }
        else if (location instanceof AbstractEndNode)
        {
            pos = ((AbstractEndNode) location).merge();
        }
        else
        {
            pos = location;
        }
        throw GraphUtil.approxSourceException(pos, exception);
    }
}