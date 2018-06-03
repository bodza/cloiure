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
    protected ValueNode ___x;
    @Input
    // @field
    protected ValueNode ___y;

    @Override
    public ValueNode getX()
    {
        return this.___x;
    }

    @Override
    public ValueNode getY()
    {
        return this.___y;
    }

    // @cons
    public BinaryOpLogicNode(NodeClass<? extends BinaryOpLogicNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c);
        this.___x = __x;
        this.___y = __y;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
    }

    ///
    // Ensure a canonical ordering of inputs for commutative nodes to improve GVN results. Order the
    // inputs by increasing {@link Node#id} and call {@link Graph#findDuplicate(Node)} on the node
    // if it's currently in a graph.
    //
    // @return the original node or another node with the same inputs, ignoring ordering.
    ///
    @SuppressWarnings("deprecation")
    public LogicNode maybeCommuteInputs()
    {
        if (!this.___y.isConstant() && (this.___x.isConstant() || this.___x.getId() > this.___y.getId()))
        {
            ValueNode __tmp = this.___x;
            this.___x = this.___y;
            this.___y = __tmp;
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

    public abstract Stamp getSucceedingStampForX(boolean __negated, Stamp __xStamp, Stamp __yStamp);

    public abstract Stamp getSucceedingStampForY(boolean __negated, Stamp __xStamp, Stamp __yStamp);

    public abstract TriState tryFold(Stamp __xStamp, Stamp __yStamp);
}
