package giraaff.nodes.virtual;

import jdk.vm.ci.code.BailoutException;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
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

// @class EnsureVirtualizedNode
public final class EnsureVirtualizedNode extends FixedWithNextNode implements Virtualizable, Lowerable
{
    // @def
    public static final NodeClass<EnsureVirtualizedNode> TYPE = NodeClass.create(EnsureVirtualizedNode.class);

    @Input
    // @field
    ValueNode ___object;
    // @field
    private final boolean ___localOnly;

    // @cons
    public EnsureVirtualizedNode(ValueNode __object, boolean __localOnly)
    {
        super(TYPE, StampFactory.forVoid());
        this.___object = __object;
        this.___localOnly = __localOnly;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(this.___object);
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
            if (__virtual instanceof VirtualBoxingNode)
            {
                throw new BailoutException("ensureVirtual is not valid for boxing objects: %s", __virtual.type().getName());
            }
            if (!this.___localOnly)
            {
                __tool.setEnsureVirtualized(__virtual, true);
            }
            __tool.delete();
        }
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        ensureVirtualFailure(this, this.___object.stamp(NodeView.DEFAULT));
    }

    public static void ensureVirtualFailure(Node __location, Stamp __stamp)
    {
        String __additionalReason = "";
        if (__location instanceof FixedWithNextNode && !(__location instanceof EnsureVirtualizedNode))
        {
            FixedWithNextNode __fixedWithNextNode = (FixedWithNextNode) __location;
            FixedNode __next = __fixedWithNextNode.next();
            if (__next instanceof StoreFieldNode)
            {
                __additionalReason = " (must not store virtual object into a field)";
            }
            else if (__next instanceof Invoke)
            {
                __additionalReason = " (must not pass virtual object into an invoke that cannot be inlined)";
            }
            else
            {
                __additionalReason = " (must not let virtual object escape at node " + __next + ")";
            }
        }
        throw new BailoutException("instance of type %s should not be materialized%s", StampTool.typeOrNull(__stamp).getName(), __additionalReason);
    }
}
