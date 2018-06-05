package giraaff.nodes.calc;

import java.util.function.Function;

import jdk.vm.ci.meta.Constant;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodePredicate;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ArithmeticOperation;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeValueMap;
import giraaff.util.GraalError;

// @class BinaryArithmeticNode
public abstract class BinaryArithmeticNode<OP> extends BinaryNode implements ArithmeticOperation, ArithmeticLIRLowerable, Canonicalizable.Binary<ValueNode>
{
    @SuppressWarnings("rawtypes")
    // @def
    public static final NodeClass<BinaryArithmeticNode> TYPE = NodeClass.create(BinaryArithmeticNode.class);

    // @iface BinaryArithmeticNode.SerializableBinaryFunction
    protected interface SerializableBinaryFunction<T> extends Function<ArithmeticOpTable, BinaryOp<T>>
    {
    }

    // @field
    protected final SerializableBinaryFunction<OP> ___getOp;

    // @cons
    protected BinaryArithmeticNode(NodeClass<? extends BinaryArithmeticNode<OP>> __c, SerializableBinaryFunction<OP> __getOp, ValueNode __x, ValueNode __y)
    {
        super(__c, __getOp.apply(ArithmeticOpTable.forStamp(__x.stamp(NodeView.DEFAULT))).foldStamp(__x.stamp(NodeView.DEFAULT), __y.stamp(NodeView.DEFAULT)), __x, __y);
        this.___getOp = __getOp;
    }

    protected final BinaryOp<OP> getOp(ValueNode __forX, ValueNode __forY)
    {
        ArithmeticOpTable __table = ArithmeticOpTable.forStamp(__forX.stamp(NodeView.DEFAULT));
        return this.___getOp.apply(__table);
    }

    @Override
    public final BinaryOp<OP> getArithmeticOp()
    {
        return getOp(getX(), getY());
    }

    public boolean isAssociative()
    {
        return getArithmeticOp().isAssociative();
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __result = tryConstantFold(getOp(__forX, __forY), __forX, __forY, stamp(__view), __view);
        if (__result != null)
        {
            return __result;
        }
        return this;
    }

    @SuppressWarnings("unused")
    public static <OP> ConstantNode tryConstantFold(BinaryOp<OP> __op, ValueNode __forX, ValueNode __forY, Stamp __stamp, NodeView __view)
    {
        if (__forX.isConstant() && __forY.isConstant())
        {
            Constant __ret = __op.foldConstant(__forX.asConstant(), __forY.asConstant());
            if (__ret != null)
            {
                return ConstantNode.forPrimitive(__stamp, __ret);
            }
        }
        return null;
    }

    @Override
    public Stamp foldStamp(Stamp __stampX, Stamp __stampY)
    {
        return getArithmeticOp().foldStamp(__stampX, __stampY);
    }

    public static ValueNode add(StructuredGraph __graph, ValueNode __v1, ValueNode __v2, NodeView __view)
    {
        return __graph.addOrUniqueWithInputs(AddNode.create(__v1, __v2, __view));
    }

    public static ValueNode add(ValueNode __v1, ValueNode __v2, NodeView __view)
    {
        return AddNode.create(__v1, __v2, __view);
    }

    public static ValueNode mul(StructuredGraph __graph, ValueNode __v1, ValueNode __v2, NodeView __view)
    {
        return __graph.addOrUniqueWithInputs(MulNode.create(__v1, __v2, __view));
    }

    public static ValueNode mul(ValueNode __v1, ValueNode __v2, NodeView __view)
    {
        return MulNode.create(__v1, __v2, __view);
    }

    public static ValueNode sub(StructuredGraph __graph, ValueNode __v1, ValueNode __v2, NodeView __view)
    {
        return __graph.addOrUniqueWithInputs(SubNode.create(__v1, __v2, __view));
    }

    public static ValueNode sub(ValueNode __v1, ValueNode __v2, NodeView __view)
    {
        return SubNode.create(__v1, __v2, __view);
    }

    // @enum BinaryArithmeticNode.ReassociateMatch
    private enum ReassociateMatch
    {
        x,
        y;

        public ValueNode getValue(BinaryNode __binary)
        {
            switch (this)
            {
                case x:
                    return __binary.getX();
                case y:
                    return __binary.getY();
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }

        public ValueNode getOtherValue(BinaryNode __binary)
        {
            switch (this)
            {
                case x:
                    return __binary.getY();
                case y:
                    return __binary.getX();
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    private static ReassociateMatch findReassociate(BinaryNode __binary, NodePredicate __criterion)
    {
        boolean __resultX = __criterion.apply(__binary.getX());
        boolean __resultY = __criterion.apply(__binary.getY());
        if (__resultX && !__resultY)
        {
            return ReassociateMatch.x;
        }
        if (!__resultX && __resultY)
        {
            return ReassociateMatch.y;
        }
        return null;
    }

    // In reassociate, complexity comes from the handling of IntegerSub (non commutative) which can
    // be mixed with IntegerAdd. It first tries to find m1, m2 which match the criterion :
    // (a o m2) o m1
    // (m2 o a) o m1
    // m1 o (a o m2)
    // m1 o (m2 o a)
    // It then produces 4 boolean for the -/+ cases:
    // invertA : should the final expression be like *-a (rather than a+*)
    // aSub : should the final expression be like a-* (rather than a+*)
    // invertM1 : should the final expression contain -m1
    // invertM2 : should the final expression contain -m2

    ///
    // Tries to re-associate values which satisfy the criterion. For example with a constantness
    // criterion: {@code (a + 2) + 1 => a + (1 + 2)}.
    //
    // This method accepts only {@linkplain BinaryOp#isAssociative() associative} operations such as
    // +, -, *, &, | and ^.
    ///
    public static ValueNode reassociate(BinaryArithmeticNode<?> __node, NodePredicate __criterion, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        ReassociateMatch __match1 = findReassociate(__node, __criterion);
        if (__match1 == null)
        {
            return __node;
        }
        ValueNode __otherValue = __match1.getOtherValue(__node);
        boolean __addSub = false;
        boolean __subAdd = false;
        if (__otherValue.getClass() != __node.getClass())
        {
            if (__node instanceof AddNode && __otherValue instanceof SubNode)
            {
                __addSub = true;
            }
            else if (__node instanceof SubNode && __otherValue instanceof AddNode)
            {
                __subAdd = true;
            }
            else
            {
                return __node;
            }
        }
        BinaryNode __other = (BinaryNode) __otherValue;
        ReassociateMatch __match2 = findReassociate(__other, __criterion);
        if (__match2 == null)
        {
            return __node;
        }
        boolean __invertA = false;
        boolean __aSub = false;
        boolean __invertM1 = false;
        boolean __invertM2 = false;
        if (__addSub)
        {
            __invertM2 = __match2 == ReassociateMatch.y;
            __invertA = !__invertM2;
        }
        else if (__subAdd)
        {
            __invertA = __invertM2 = __match1 == ReassociateMatch.x;
            __invertM1 = !__invertM2;
        }
        else if (__node instanceof SubNode && __other instanceof SubNode)
        {
            __invertA = __match1 == ReassociateMatch.x ^ __match2 == ReassociateMatch.x;
            __aSub = __match1 == ReassociateMatch.y && __match2 == ReassociateMatch.y;
            __invertM1 = __match1 == ReassociateMatch.y && __match2 == ReassociateMatch.x;
            __invertM2 = __match1 == ReassociateMatch.x && __match2 == ReassociateMatch.x;
        }
        ValueNode __m1 = __match1.getValue(__node);
        ValueNode __m2 = __match2.getValue(__other);
        ValueNode __a = __match2.getOtherValue(__other);
        if (__node instanceof AddNode || __node instanceof SubNode)
        {
            ValueNode __associated;
            if (__invertM1)
            {
                __associated = BinaryArithmeticNode.sub(__m2, __m1, __view);
            }
            else if (__invertM2)
            {
                __associated = BinaryArithmeticNode.sub(__m1, __m2, __view);
            }
            else
            {
                __associated = BinaryArithmeticNode.add(__m1, __m2, __view);
            }
            if (__invertA)
            {
                return BinaryArithmeticNode.sub(__associated, __a, __view);
            }
            if (__aSub)
            {
                return BinaryArithmeticNode.sub(__a, __associated, __view);
            }
            return BinaryArithmeticNode.add(__a, __associated, __view);
        }
        else if (__node instanceof MulNode)
        {
            return BinaryArithmeticNode.mul(__a, AddNode.mul(__m1, __m2, __view), __view);
        }
        else if (__node instanceof AndNode)
        {
            return new AndNode(__a, new AndNode(__m1, __m2));
        }
        else if (__node instanceof OrNode)
        {
            return new OrNode(__a, new OrNode(__m1, __m2));
        }
        else if (__node instanceof XorNode)
        {
            return new XorNode(__a, new XorNode(__m1, __m2));
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
    }

    ///
    // Ensure a canonical ordering of inputs for commutative nodes to improve GVN results. Order the
    // inputs by increasing {@link Node#id} and call {@link Graph#findDuplicate(Node)} on the node
    // if it's currently in a graph. It's assumed that if there was a constant on the left it's been
    // moved to the right by other code and that ordering is left alone.
    //
    // @return the original node or another node with the same input ordering
    ///
    @SuppressWarnings("deprecation")
    public BinaryNode maybeCommuteInputs()
    {
        if (!this.___y.isConstant() && (this.___x.isConstant() || this.___x.getId() > this.___y.getId()))
        {
            ValueNode __tmp = this.___x;
            this.___x = this.___y;
            this.___y = __tmp;
            if (graph() != null)
            {
                // see if this node already exists
                BinaryNode __duplicate = graph().findDuplicate(this);
                if (__duplicate != null)
                {
                    return __duplicate;
                }
            }
        }
        return this;
    }

    ///
    // Determines if it would be better to swap the inputs in order to produce better assembly code.
    // First we try to pick a value which is dead after this use. If both values are dead at this
    // use then we try pick an induction variable phi to encourage the phi to live in a single register.
    //
    // @return true if inputs should be swapped, false otherwise
    ///
    protected boolean shouldSwapInputs(NodeValueMap __nodeValueMap)
    {
        final boolean __xHasOtherUsages = getX().hasUsagesOtherThan(this, __nodeValueMap);
        final boolean __yHasOtherUsages = getY().hasUsagesOtherThan(this, __nodeValueMap);

        if (!getY().isConstant() && !__yHasOtherUsages)
        {
            if (__xHasOtherUsages == __yHasOtherUsages)
            {
                return getY() instanceof ValuePhiNode && getY().inputs().contains(this);
            }
            else
            {
                return true;
            }
        }
        return false;
    }
}
