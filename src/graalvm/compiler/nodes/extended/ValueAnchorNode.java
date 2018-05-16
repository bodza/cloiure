package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.InputType.Anchor;
import static graalvm.compiler.nodeinfo.InputType.Guard;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.FixedAccessNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

/**
 * The ValueAnchor instruction keeps non-CFG (floating) nodes above a certain point in the graph.
 */
@NodeInfo(allowedUsageTypes = {Anchor, Guard}, cycles = CYCLES_0, size = SIZE_0)
public final class ValueAnchorNode extends FixedWithNextNode implements LIRLowerable, Simplifiable, Virtualizable, AnchoringNode, GuardingNode
{
    public static final NodeClass<ValueAnchorNode> TYPE = NodeClass.create(ValueAnchorNode.class);
    @OptionalInput(Guard) ValueNode anchored;

    public ValueAnchorNode(ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.anchored = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // Nothing to emit, since this node is used for structural purposes only.
    }

    public ValueNode getAnchoredNode()
    {
        return anchored;
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        while (next() instanceof ValueAnchorNode)
        {
            ValueAnchorNode nextAnchor = (ValueAnchorNode) next();
            if (nextAnchor.anchored == anchored || nextAnchor.anchored == null)
            {
                // two anchors for the same anchored -> coalesce
                // nothing anchored on the next anchor -> coalesce
                nextAnchor.replaceAtUsages(this);
                GraphUtil.removeFixedWithUnusedInputs(nextAnchor);
            }
            else
            {
                break;
            }
        }
        if (tool.allUsagesAvailable() && hasNoUsages() && next() instanceof FixedAccessNode)
        {
            FixedAccessNode currentNext = (FixedAccessNode) next();
            if (currentNext.getGuard() == anchored)
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
    public void virtualize(VirtualizerTool tool)
    {
        if (anchored == null || anchored instanceof AbstractBeginNode)
        {
            tool.delete();
        }
        else
        {
            ValueNode alias = tool.getAlias(anchored);
            if (alias instanceof VirtualObjectNode)
            {
                tool.delete();
            }
        }
    }

    public void removeAnchoredNode()
    {
        this.updateUsages(anchored, null);
        this.anchored = null;
    }
}
