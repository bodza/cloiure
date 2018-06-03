package giraaff.nodes.extended;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.FixedAccessNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * The ValueAnchor instruction keeps non-CFG (floating) nodes above a certain point in the graph.
 */
// @NodeInfo.allowedUsageTypes "Anchor, Guard"
// @class ValueAnchorNode
public final class ValueAnchorNode extends FixedWithNextNode implements LIRLowerable, Simplifiable, Virtualizable, AnchoringNode, GuardingNode
{
    // @def
    public static final NodeClass<ValueAnchorNode> TYPE = NodeClass.create(ValueAnchorNode.class);

    @OptionalInput(InputType.Guard)
    // @field
    ValueNode anchored;

    // @cons
    public ValueAnchorNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.anchored = __value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // Nothing to emit, since this node is used for structural purposes only.
    }

    public ValueNode getAnchoredNode()
    {
        return anchored;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        while (next() instanceof ValueAnchorNode)
        {
            ValueAnchorNode __nextAnchor = (ValueAnchorNode) next();
            if (__nextAnchor.anchored == anchored || __nextAnchor.anchored == null)
            {
                // two anchors for the same anchored -> coalesce
                // nothing anchored on the next anchor -> coalesce
                __nextAnchor.replaceAtUsages(this);
                GraphUtil.removeFixedWithUnusedInputs(__nextAnchor);
            }
            else
            {
                break;
            }
        }
        if (__tool.allUsagesAvailable() && hasNoUsages() && next() instanceof FixedAccessNode)
        {
            FixedAccessNode __currentNext = (FixedAccessNode) next();
            if (__currentNext.getGuard() == anchored)
            {
                GraphUtil.removeFixedWithUnusedInputs(this);
                return;
            }
        }

        if (anchored != null && (anchored.isConstant() || anchored instanceof FixedNode))
        {
            // anchoring fixed nodes and constants is useless
            removeAnchoredNode();
        }

        if (anchored == null && hasNoUsages())
        {
            // anchor is not necessary any more => remove.
            GraphUtil.removeFixedWithUnusedInputs(this);
        }
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        if (anchored == null || anchored instanceof AbstractBeginNode)
        {
            __tool.delete();
        }
        else
        {
            ValueNode __alias = __tool.getAlias(anchored);
            if (__alias instanceof VirtualObjectNode)
            {
                __tool.delete();
            }
        }
    }

    public void removeAnchoredNode()
    {
        this.updateUsages(anchored, null);
        this.anchored = null;
    }
}
