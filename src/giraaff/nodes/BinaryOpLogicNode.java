package giraaff.nodes;

import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class BinaryOpLogicNode
public abstract class BinaryOpLogicNode extends LogicNode implements LIRLowerable, Canonicalizable.Binary<ValueNode>
{
    // @def
    public static final NodeClass<BinaryOpLogicNode> TYPE = NodeClass.create(BinaryOpLogicNode.class);

    @Input
    // @field
    protected ValueNode x;
    @Input
    // @field
    protected ValueNode y;

    @Override
    public ValueNode getX()
    {
        return x;
    }

    @Override
    public ValueNode getY()
    {
        return y;
    }

    // @cons
    public BinaryOpLogicNode(NodeClass<? extends BinaryOpLogicNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c);
        this.x = __x;
        this.y = __y;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
    }

    /**
     * Ensure a canonical ordering of inputs for commutative nodes to improve GVN results. Order the
     * inputs by increasing {@link Node#id} and call {@link Graph#findDuplicate(Node)} on the node
     * if it's currently in a graph.
     *
     * @return the original node or another node with the same inputs, ignoring ordering.
     */
    @SuppressWarnings("deprecation")
    public LogicNode maybeCommuteInputs()
    {
        if (!y.isConstant() && (x.isConstant() || x.getId() > y.getId()))
        {
            ValueNode __tmp = x;
            x = y;
            y = __tmp;
            if (graph() != null)
            {
                // see if this node already exists
                LogicNode __duplicate = graph().findDuplicate(this);
                if (__duplicate != null)
                {
                    return __duplicate;
                }
            }
        }
        return this;
    }

    public abstract Stamp getSucceedingStampForX(boolean negated, Stamp xStamp, Stamp yStamp);

    public abstract Stamp getSucceedingStampForY(boolean negated, Stamp xStamp, Stamp yStamp);

    public abstract TriState tryFold(Stamp xStamp, Stamp yStamp);
}
