package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.TriState;

@NodeInfo
public abstract class BinaryOpLogicNode extends LogicNode implements LIRLowerable, Canonicalizable.Binary<ValueNode> {

    public static final NodeClass<BinaryOpLogicNode> TYPE = NodeClass.create(BinaryOpLogicNode.class);
    @Input protected ValueNode x;
    @Input protected ValueNode y;

    @Override
    public ValueNode getX() {
        return x;
    }

    @Override
    public ValueNode getY() {
        return y;
    }

    public BinaryOpLogicNode(NodeClass<? extends BinaryOpLogicNode> c, ValueNode x, ValueNode y) {
        super(c);
        assert x != null && y != null;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean verify() {
        return super.verify();
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
    }

    /**
     * Ensure a canonical ordering of inputs for commutative nodes to improve GVN results. Order the
     * inputs by increasing {@link Node#id} and call {@link Graph#findDuplicate(Node)} on the node
     * if it's currently in a graph.
     *
     * @return the original node or another node with the same inputs, ignoring ordering.
     */
    @SuppressWarnings("deprecation")
    public LogicNode maybeCommuteInputs() {
        assert this instanceof BinaryCommutative;
        if (!y.isConstant() && (x.isConstant() || x.getId() > y.getId())) {
            ValueNode tmp = x;
            x = y;
            y = tmp;
            if (graph() != null) {
                // See if this node already exists
                LogicNode duplicate = graph().findDuplicate(this);
                if (duplicate != null) {
                    return duplicate;
                }
            }
        }
        return this;
    }

    public abstract Stamp getSucceedingStampForX(boolean negated, Stamp xStamp, Stamp yStamp);

    public abstract Stamp getSucceedingStampForY(boolean negated, Stamp xStamp, Stamp yStamp);

    public abstract TriState tryFold(Stamp xStamp, Stamp yStamp);
}
