package giraaff.loop;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import jdk.vm.ci.code.BytecodeFrame;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.calc.Condition;
import giraaff.core.common.cfg.Loop;
import giraaff.core.common.type.IntegerStamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.iterators.NodePredicate;
import giraaff.loop.InductionVariable;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.calc.MulNode;
import giraaff.nodes.calc.NegateNode;
import giraaff.nodes.calc.SignExtendNode;
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.calc.ZeroExtendNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.debug.ControlFlowAnchored;
import giraaff.nodes.extended.ValueAnchorNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

// @class LoopEx
public final class LoopEx
{
    // @field
    private final Loop<Block> ___loop;
    // @field
    private LoopFragmentInside ___inside;
    // @field
    private LoopFragmentWhole ___whole;
    // @field
    private CountedLoopInfo ___counted;
    // @field
    private LoopsData ___data;
    // @field
    private EconomicMap<Node, InductionVariable> ___ivs;

    // @cons LoopEx
    LoopEx(Loop<Block> __loop, LoopsData __data)
    {
        super();
        this.___loop = __loop;
        this.___data = __data;
    }

    public Loop<Block> loop()
    {
        return this.___loop;
    }

    public LoopFragmentInside inside()
    {
        if (this.___inside == null)
        {
            this.___inside = new LoopFragmentInside(this);
        }
        return this.___inside;
    }

    public LoopFragmentWhole whole()
    {
        if (this.___whole == null)
        {
            this.___whole = new LoopFragmentWhole(this);
        }
        return this.___whole;
    }

    public void invalidateFragments()
    {
        this.___inside = null;
        this.___whole = null;
    }

    @SuppressWarnings("unused")
    public LoopFragmentInsideFrom insideFrom(FixedNode __point)
    {
        // TODO
        return null;
    }

    @SuppressWarnings("unused")
    public LoopFragmentInsideBefore insideBefore(FixedNode __point)
    {
        // TODO
        return null;
    }

    public boolean isOutsideLoop(Node __n)
    {
        return !whole().contains(__n);
    }

    public LoopBeginNode loopBegin()
    {
        return (LoopBeginNode) loop().getHeader().getBeginNode();
    }

    public FixedNode predecessor()
    {
        return (FixedNode) loopBegin().forwardEnd().predecessor();
    }

    public FixedNode entryPoint()
    {
        return loopBegin().forwardEnd();
    }

    public boolean isCounted()
    {
        return this.___counted != null;
    }

    public CountedLoopInfo counted()
    {
        return this.___counted;
    }

    public LoopEx parent()
    {
        if (this.___loop.getParent() == null)
        {
            return null;
        }
        return this.___data.loop(this.___loop.getParent());
    }

    public int size()
    {
        return whole().nodes().count();
    }

    // @class LoopEx.InvariantPredicate
    // @closure
    private final class InvariantPredicate implements NodePredicate
    {
        // @field
        private final Graph.NodeMark ___mark;

        // @cons LoopEx.InvariantPredicate
        InvariantPredicate()
        {
            super();
            this.___mark = LoopEx.this.loopBegin().graph().getMark();
        }

        @Override
        public boolean apply(Node __n)
        {
            if (LoopEx.this.loopBegin().graph().isNew(this.___mark, __n))
            {
                // Newly created nodes are unknown.
                return false;
            }
            return LoopEx.this.isOutsideLoop(__n);
        }
    }

    public boolean reassociateInvariants()
    {
        int __count = 0;
        StructuredGraph __graph = loopBegin().graph();
        LoopEx.InvariantPredicate __invariant = new LoopEx.InvariantPredicate();
        for (BinaryArithmeticNode<?> __binary : whole().nodes().filter(BinaryArithmeticNode.class))
        {
            if (!__binary.isAssociative())
            {
                continue;
            }
            ValueNode __result = BinaryArithmeticNode.reassociate(__binary, __invariant, __binary.getX(), __binary.getY(), NodeView.DEFAULT);
            if (__result != __binary)
            {
                if (!__result.isAlive())
                {
                    __result = __graph.addOrUniqueWithInputs(__result);
                }
                __binary.replaceAtUsages(__result);
                GraphUtil.killWithUnusedFloatingInputs(__binary);
                __count++;
            }
        }
        return __count != 0;
    }

    public boolean detectCounted()
    {
        LoopBeginNode __loopBegin = loopBegin();
        FixedNode __next = __loopBegin.next();
        while (__next instanceof FixedGuardNode || __next instanceof ValueAnchorNode)
        {
            __next = ((FixedWithNextNode) __next).next();
        }
        if (__next instanceof IfNode)
        {
            IfNode __ifNode = (IfNode) __next;
            boolean __negated = false;
            if (!__loopBegin.isLoopExit(__ifNode.falseSuccessor()))
            {
                if (!__loopBegin.isLoopExit(__ifNode.trueSuccessor()))
                {
                    return false;
                }
                __negated = true;
            }
            LogicNode __ifTest = __ifNode.condition();
            if (!(__ifTest instanceof IntegerLessThanNode) && !(__ifTest instanceof IntegerEqualsNode))
            {
                return false;
            }
            CompareNode __lessThan = (CompareNode) __ifTest;
            Condition __condition = null;
            InductionVariable __iv = null;
            ValueNode __limit = null;
            if (isOutsideLoop(__lessThan.getX()))
            {
                __iv = getInductionVariables().get(__lessThan.getY());
                if (__iv != null)
                {
                    __condition = __lessThan.condition().asCondition().mirror();
                    __limit = __lessThan.getX();
                }
            }
            else if (isOutsideLoop(__lessThan.getY()))
            {
                __iv = getInductionVariables().get(__lessThan.getX());
                if (__iv != null)
                {
                    __condition = __lessThan.condition().asCondition();
                    __limit = __lessThan.getY();
                }
            }
            if (__condition == null)
            {
                return false;
            }
            if (__negated)
            {
                __condition = __condition.negate();
            }
            boolean __oneOff = false;
            switch (__condition)
            {
                case EQ:
                    return false;
                case NE:
                {
                    if (!__iv.isConstantStride() || Math.abs(__iv.constantStride()) != 1)
                    {
                        return false;
                    }
                    IntegerStamp __initStamp = (IntegerStamp) __iv.initNode().stamp(NodeView.DEFAULT);
                    IntegerStamp __limitStamp = (IntegerStamp) __limit.stamp(NodeView.DEFAULT);
                    if (__iv.direction() == InductionVariable.Direction.Up)
                    {
                        if (__initStamp.upperBound() > __limitStamp.lowerBound())
                        {
                            return false;
                        }
                    }
                    else if (__iv.direction() == InductionVariable.Direction.Down)
                    {
                        if (__initStamp.lowerBound() < __limitStamp.upperBound())
                        {
                            return false;
                        }
                    }
                    else
                    {
                        return false;
                    }
                    break;
                }
                case LE:
                    __oneOff = true;
                    if (__iv.direction() != InductionVariable.Direction.Up)
                    {
                        return false;
                    }
                    break;
                case LT:
                    if (__iv.direction() != InductionVariable.Direction.Up)
                    {
                        return false;
                    }
                    break;
                case GE:
                    __oneOff = true;
                    if (__iv.direction() != InductionVariable.Direction.Down)
                    {
                        return false;
                    }
                    break;
                case GT:
                    if (__iv.direction() != InductionVariable.Direction.Down)
                    {
                        return false;
                    }
                    break;
                default:
                    throw GraalError.shouldNotReachHere();
            }
            this.___counted = new CountedLoopInfo(this, __iv, __ifNode, __limit, __oneOff, __negated ? __ifNode.falseSuccessor() : __ifNode.trueSuccessor());
            return true;
        }
        return false;
    }

    public LoopsData loopsData()
    {
        return this.___data;
    }

    public void nodesInLoopBranch(NodeBitMap __branchNodes, AbstractBeginNode __branch)
    {
        EconomicSet<AbstractBeginNode> __blocks = EconomicSet.create();
        Collection<AbstractBeginNode> __exits = new LinkedList<>();
        Queue<Block> __work = new LinkedList<>();
        ControlFlowGraph __cfg = loopsData().getCFG();
        __work.add(__cfg.blockFor(__branch));
        while (!__work.isEmpty())
        {
            Block __b = __work.remove();
            if (loop().getExits().contains(__b))
            {
                __exits.add(__b.getBeginNode());
            }
            else if (__blocks.add(__b.getBeginNode()))
            {
                Block __d = __b.getDominatedSibling();
                while (__d != null)
                {
                    if (this.___loop.getBlocks().contains(__d))
                    {
                        __work.add(__d);
                    }
                    __d = __d.getDominatedSibling();
                }
            }
        }
        LoopFragment.computeNodes(__branchNodes, __branch.graph(), __blocks, __exits);
    }

    public EconomicMap<Node, InductionVariable> getInductionVariables()
    {
        if (this.___ivs == null)
        {
            this.___ivs = findInductionVariables(this);
        }
        return this.___ivs;
    }

    ///
    // Collect all the basic induction variables for the loop and the find any induction variables
    // which are derived from the basic ones.
    //
    // @return a map from node to induction variable
    ///
    private static EconomicMap<Node, InductionVariable> findInductionVariables(LoopEx __loop)
    {
        EconomicMap<Node, InductionVariable> __ivs = EconomicMap.create(Equivalence.IDENTITY);

        Queue<InductionVariable> __scanQueue = new LinkedList<>();
        LoopBeginNode __loopBegin = __loop.loopBegin();
        AbstractEndNode __forwardEnd = __loopBegin.forwardEnd();
        for (PhiNode __phi : __loopBegin.valuePhis())
        {
            ValueNode __backValue = __phi.singleBackValueOrThis();
            if (__backValue == __phi)
            {
                continue;
            }
            ValueNode __stride = addSub(__loop, __backValue, __phi);
            if (__stride != null)
            {
                BasicInductionVariable __biv = new BasicInductionVariable(__loop, (ValuePhiNode) __phi, __phi.valueAt(__forwardEnd), __stride, (BinaryArithmeticNode<?>) __backValue);
                __ivs.put(__phi, __biv);
                __scanQueue.add(__biv);
            }
        }

        while (!__scanQueue.isEmpty())
        {
            InductionVariable __baseIv = __scanQueue.remove();
            ValueNode __baseIvNode = __baseIv.valueNode();
            for (ValueNode __op : __baseIvNode.usages().filter(ValueNode.class))
            {
                if (__loop.isOutsideLoop(__op))
                {
                    continue;
                }
                if (__op.usages().count() == 1 && __op.usages().first() == __baseIvNode)
                {
                    // This is just the base induction variable increment with no other uses so don't bother reporting it.
                    continue;
                }
                InductionVariable __iv = null;
                ValueNode __offset = addSub(__loop, __op, __baseIvNode);
                ValueNode __scale;
                if (__offset != null)
                {
                    __iv = new DerivedOffsetInductionVariable(__loop, __baseIv, __offset, (BinaryArithmeticNode<?>) __op);
                }
                else if (__op instanceof NegateNode)
                {
                    __iv = new DerivedScaledInductionVariable(__loop, __baseIv, (NegateNode) __op);
                }
                else if ((__scale = mul(__loop, __op, __baseIvNode)) != null)
                {
                    __iv = new DerivedScaledInductionVariable(__loop, __baseIv, __scale, __op);
                }
                else
                {
                    boolean __isValidConvert = __op instanceof PiNode || __op instanceof SignExtendNode;
                    if (!__isValidConvert && __op instanceof ZeroExtendNode)
                    {
                        ZeroExtendNode __zeroExtendNode = (ZeroExtendNode) __op;
                        __isValidConvert = __zeroExtendNode.isInputAlwaysPositive() || ((IntegerStamp) __zeroExtendNode.stamp(NodeView.DEFAULT)).isPositive();
                    }

                    if (__isValidConvert)
                    {
                        __iv = new DerivedConvertedInductionVariable(__loop, __baseIv, __op.stamp(NodeView.DEFAULT), __op);
                    }
                }

                if (__iv != null)
                {
                    __ivs.put(__op, __iv);
                    __scanQueue.offer(__iv);
                }
            }
        }
        return __ivs;
    }

    private static ValueNode addSub(LoopEx __loop, ValueNode __op, ValueNode __base)
    {
        if (__op.stamp(NodeView.DEFAULT) instanceof IntegerStamp && (__op instanceof AddNode || __op instanceof SubNode))
        {
            BinaryArithmeticNode<?> __aritOp = (BinaryArithmeticNode<?>) __op;
            if (__aritOp.getX() == __base && __loop.isOutsideLoop(__aritOp.getY()))
            {
                return __aritOp.getY();
            }
            else if (__aritOp.getY() == __base && __loop.isOutsideLoop(__aritOp.getX()))
            {
                return __aritOp.getX();
            }
        }
        return null;
    }

    private static ValueNode mul(LoopEx __loop, ValueNode __op, ValueNode __base)
    {
        if (__op instanceof MulNode)
        {
            MulNode __mul = (MulNode) __op;
            if (__mul.getX() == __base && __loop.isOutsideLoop(__mul.getY()))
            {
                return __mul.getY();
            }
            else if (__mul.getY() == __base && __loop.isOutsideLoop(__mul.getX()))
            {
                return __mul.getX();
            }
        }
        if (__op instanceof LeftShiftNode)
        {
            LeftShiftNode __shift = (LeftShiftNode) __op;
            if (__shift.getX() == __base && __shift.getY().isConstant())
            {
                return ConstantNode.forIntegerStamp(__base.stamp(NodeView.DEFAULT), 1 << __shift.getY().asJavaConstant().asInt(), __base.graph());
            }
        }
        return null;
    }

    ///
    // Deletes any nodes created within the scope of this object that have no usages.
    ///
    public void deleteUnusedNodes()
    {
        if (this.___ivs != null)
        {
            for (InductionVariable __iv : this.___ivs.getValues())
            {
                __iv.deleteUnusedNodes();
            }
        }
    }

    ///
    // @return true if all nodes in the loop can be duplicated.
    ///
    public boolean canDuplicateLoop()
    {
        for (Node __node : inside().nodes())
        {
            if (__node instanceof ControlFlowAnchored)
            {
                return false;
            }
            if (__node instanceof FrameState)
            {
                FrameState __frameState = (FrameState) __node;
                if (__frameState.___bci == BytecodeFrame.AFTER_EXCEPTION_BCI || __frameState.___bci == BytecodeFrame.UNWIND_BCI)
                {
                    return false;
                }
            }
        }
        return true;
    }
}
