package graalvm.compiler.loop;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.core.common.calc.Condition;
import graalvm.compiler.core.common.cfg.Loop;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeBitMap;
import graalvm.compiler.graph.iterators.NodePredicate;
import graalvm.compiler.loop.InductionVariable.Direction;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.FullInfopointNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.ValuePhiNode;
import graalvm.compiler.nodes.calc.AddNode;
import graalvm.compiler.nodes.calc.BinaryArithmeticNode;
import graalvm.compiler.nodes.calc.CompareNode;
import graalvm.compiler.nodes.calc.IntegerBelowNode;
import graalvm.compiler.nodes.calc.IntegerEqualsNode;
import graalvm.compiler.nodes.calc.IntegerLessThanNode;
import graalvm.compiler.nodes.calc.LeftShiftNode;
import graalvm.compiler.nodes.calc.MulNode;
import graalvm.compiler.nodes.calc.NegateNode;
import graalvm.compiler.nodes.calc.SignExtendNode;
import graalvm.compiler.nodes.calc.SubNode;
import graalvm.compiler.nodes.calc.ZeroExtendNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.debug.ControlFlowAnchored;
import graalvm.compiler.nodes.extended.ValueAnchorNode;
import graalvm.compiler.nodes.util.GraphUtil;

import jdk.vm.ci.code.BytecodeFrame;

public class LoopEx {
    private final Loop<Block> loop;
    private LoopFragmentInside inside;
    private LoopFragmentWhole whole;
    private CountedLoopInfo counted;
    private LoopsData data;
    private EconomicMap<Node, InductionVariable> ivs;

    LoopEx(Loop<Block> loop, LoopsData data) {
        this.loop = loop;
        this.data = data;
    }

    public Loop<Block> loop() {
        return loop;
    }

    public LoopFragmentInside inside() {
        if (inside == null) {
            inside = new LoopFragmentInside(this);
        }
        return inside;
    }

    public LoopFragmentWhole whole() {
        if (whole == null) {
            whole = new LoopFragmentWhole(this);
        }
        return whole;
    }

    public void invalidateFragments() {
        inside = null;
        whole = null;
    }

    @SuppressWarnings("unused")
    public LoopFragmentInsideFrom insideFrom(FixedNode point) {
        // TODO (gd)
        return null;
    }

    @SuppressWarnings("unused")
    public LoopFragmentInsideBefore insideBefore(FixedNode point) {
        // TODO (gd)
        return null;
    }

    public boolean isOutsideLoop(Node n) {
        return !whole().contains(n);
    }

    public LoopBeginNode loopBegin() {
        return (LoopBeginNode) loop().getHeader().getBeginNode();
    }

    public FixedNode predecessor() {
        return (FixedNode) loopBegin().forwardEnd().predecessor();
    }

    public FixedNode entryPoint() {
        return loopBegin().forwardEnd();
    }

    public boolean isCounted() {
        return counted != null;
    }

    public CountedLoopInfo counted() {
        return counted;
    }

    public LoopEx parent() {
        if (loop.getParent() == null) {
            return null;
        }
        return data.loop(loop.getParent());
    }

    public int size() {
        return whole().nodes().count();
    }

    @Override
    public String toString() {
        return (isCounted() ? "CountedLoop [" + counted() + "] " : "Loop ") + "(depth=" + loop().getDepth() + ") " + loopBegin();
    }

    private class InvariantPredicate implements NodePredicate {

        private final Graph.Mark mark;

        InvariantPredicate() {
            this.mark = loopBegin().graph().getMark();
        }

        @Override
        public boolean apply(Node n) {
            if (loopBegin().graph().isNew(mark, n)) {
                // Newly created nodes are unknown.
                return false;
            }
            return isOutsideLoop(n);
        }
    }

    public boolean reassociateInvariants() {
        int count = 0;
        StructuredGraph graph = loopBegin().graph();
        InvariantPredicate invariant = new InvariantPredicate();
        for (BinaryArithmeticNode<?> binary : whole().nodes().filter(BinaryArithmeticNode.class)) {
            if (!binary.isAssociative()) {
                continue;
            }
            ValueNode result = BinaryArithmeticNode.reassociate(binary, invariant, binary.getX(), binary.getY(), NodeView.DEFAULT);
            if (result != binary) {
                if (!result.isAlive()) {
                    assert !result.isDeleted();
                    result = graph.addOrUniqueWithInputs(result);
                }
                DebugContext debug = graph.getDebug();
                if (debug.isLogEnabled()) {
                    debug.log("%s : Reassociated %s into %s", graph.method().format("%H::%n"), binary, result);
                }
                binary.replaceAtUsages(result);
                GraphUtil.killWithUnusedFloatingInputs(binary);
                count++;
            }
        }
        return count != 0;
    }

    public boolean detectCounted() {
        LoopBeginNode loopBegin = loopBegin();
        FixedNode next = loopBegin.next();
        while (next instanceof FixedGuardNode || next instanceof ValueAnchorNode || next instanceof FullInfopointNode) {
            next = ((FixedWithNextNode) next).next();
        }
        if (next instanceof IfNode) {
            IfNode ifNode = (IfNode) next;
            boolean negated = false;
            if (!loopBegin.isLoopExit(ifNode.falseSuccessor())) {
                if (!loopBegin.isLoopExit(ifNode.trueSuccessor())) {
                    return false;
                }
                negated = true;
            }
            LogicNode ifTest = ifNode.condition();
            if (!(ifTest instanceof IntegerLessThanNode) && !(ifTest instanceof IntegerEqualsNode)) {
                if (ifTest instanceof IntegerBelowNode) {
                    ifTest.getDebug().log("Ignored potential Counted loop at %s with |<|", loopBegin);
                }
                return false;
            }
            CompareNode lessThan = (CompareNode) ifTest;
            Condition condition = null;
            InductionVariable iv = null;
            ValueNode limit = null;
            if (isOutsideLoop(lessThan.getX())) {
                iv = getInductionVariables().get(lessThan.getY());
                if (iv != null) {
                    condition = lessThan.condition().asCondition().mirror();
                    limit = lessThan.getX();
                }
            } else if (isOutsideLoop(lessThan.getY())) {
                iv = getInductionVariables().get(lessThan.getX());
                if (iv != null) {
                    condition = lessThan.condition().asCondition();
                    limit = lessThan.getY();
                }
            }
            if (condition == null) {
                return false;
            }
            if (negated) {
                condition = condition.negate();
            }
            boolean oneOff = false;
            switch (condition) {
                case EQ:
                    return false;
                case NE: {
                    if (!iv.isConstantStride() || Math.abs(iv.constantStride()) != 1) {
                        return false;
                    }
                    IntegerStamp initStamp = (IntegerStamp) iv.initNode().stamp(NodeView.DEFAULT);
                    IntegerStamp limitStamp = (IntegerStamp) limit.stamp(NodeView.DEFAULT);
                    if (iv.direction() == Direction.Up) {
                        if (initStamp.upperBound() > limitStamp.lowerBound()) {
                            return false;
                        }
                    } else if (iv.direction() == Direction.Down) {
                        if (initStamp.lowerBound() < limitStamp.upperBound()) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                    break;
                }
                case LE:
                    oneOff = true;
                    if (iv.direction() != Direction.Up) {
                        return false;
                    }
                    break;
                case LT:
                    if (iv.direction() != Direction.Up) {
                        return false;
                    }
                    break;
                case GE:
                    oneOff = true;
                    if (iv.direction() != Direction.Down) {
                        return false;
                    }
                    break;
                case GT:
                    if (iv.direction() != Direction.Down) {
                        return false;
                    }
                    break;
                default:
                    throw GraalError.shouldNotReachHere();
            }
            counted = new CountedLoopInfo(this, iv, ifNode, limit, oneOff, negated ? ifNode.falseSuccessor() : ifNode.trueSuccessor());
            return true;
        }
        return false;
    }

    public LoopsData loopsData() {
        return data;
    }

    public void nodesInLoopBranch(NodeBitMap branchNodes, AbstractBeginNode branch) {
        EconomicSet<AbstractBeginNode> blocks = EconomicSet.create();
        Collection<AbstractBeginNode> exits = new LinkedList<>();
        Queue<Block> work = new LinkedList<>();
        ControlFlowGraph cfg = loopsData().getCFG();
        work.add(cfg.blockFor(branch));
        while (!work.isEmpty()) {
            Block b = work.remove();
            if (loop().getExits().contains(b)) {
                assert !exits.contains(b.getBeginNode());
                exits.add(b.getBeginNode());
            } else if (blocks.add(b.getBeginNode())) {
                Block d = b.getDominatedSibling();
                while (d != null) {
                    if (loop.getBlocks().contains(d)) {
                        work.add(d);
                    }
                    d = d.getDominatedSibling();
                }
            }
        }
        LoopFragment.computeNodes(branchNodes, branch.graph(), blocks, exits);
    }

    public EconomicMap<Node, InductionVariable> getInductionVariables() {
        if (ivs == null) {
            ivs = findInductionVariables(this);
        }
        return ivs;
    }

    /**
     * Collect all the basic induction variables for the loop and the find any induction variables
     * which are derived from the basic ones.
     *
     * @param loop
     * @return a map from node to induction variable
     */
    private static EconomicMap<Node, InductionVariable> findInductionVariables(LoopEx loop) {
        EconomicMap<Node, InductionVariable> ivs = EconomicMap.create(Equivalence.IDENTITY);

        Queue<InductionVariable> scanQueue = new LinkedList<>();
        LoopBeginNode loopBegin = loop.loopBegin();
        AbstractEndNode forwardEnd = loopBegin.forwardEnd();
        for (PhiNode phi : loopBegin.valuePhis()) {
            ValueNode backValue = phi.singleBackValueOrThis();
            if (backValue == phi) {
                continue;
            }
            ValueNode stride = addSub(loop, backValue, phi);
            if (stride != null) {
                BasicInductionVariable biv = new BasicInductionVariable(loop, (ValuePhiNode) phi, phi.valueAt(forwardEnd), stride, (BinaryArithmeticNode<?>) backValue);
                ivs.put(phi, biv);
                scanQueue.add(biv);
            }
        }

        while (!scanQueue.isEmpty()) {
            InductionVariable baseIv = scanQueue.remove();
            ValueNode baseIvNode = baseIv.valueNode();
            for (ValueNode op : baseIvNode.usages().filter(ValueNode.class)) {
                if (loop.isOutsideLoop(op)) {
                    continue;
                }
                if (op.usages().count() == 1 && op.usages().first() == baseIvNode) {
                    /*
                     * This is just the base induction variable increment with no other uses so
                     * don't bother reporting it.
                     */
                    continue;
                }
                InductionVariable iv = null;
                ValueNode offset = addSub(loop, op, baseIvNode);
                ValueNode scale;
                if (offset != null) {
                    iv = new DerivedOffsetInductionVariable(loop, baseIv, offset, (BinaryArithmeticNode<?>) op);
                } else if (op instanceof NegateNode) {
                    iv = new DerivedScaledInductionVariable(loop, baseIv, (NegateNode) op);
                } else if ((scale = mul(loop, op, baseIvNode)) != null) {
                    iv = new DerivedScaledInductionVariable(loop, baseIv, scale, op);
                } else {
                    boolean isValidConvert = op instanceof PiNode || op instanceof SignExtendNode;
                    if (!isValidConvert && op instanceof ZeroExtendNode) {
                        ZeroExtendNode zeroExtendNode = (ZeroExtendNode) op;
                        isValidConvert = zeroExtendNode.isInputAlwaysPositive() || ((IntegerStamp) zeroExtendNode.stamp(NodeView.DEFAULT)).isPositive();
                    }

                    if (isValidConvert) {
                        iv = new DerivedConvertedInductionVariable(loop, baseIv, op.stamp(NodeView.DEFAULT), op);
                    }
                }

                if (iv != null) {
                    ivs.put(op, iv);
                    scanQueue.offer(iv);
                }
            }
        }
        return ivs;
    }

    private static ValueNode addSub(LoopEx loop, ValueNode op, ValueNode base) {
        if (op.stamp(NodeView.DEFAULT) instanceof IntegerStamp && (op instanceof AddNode || op instanceof SubNode)) {
            BinaryArithmeticNode<?> aritOp = (BinaryArithmeticNode<?>) op;
            if (aritOp.getX() == base && loop.isOutsideLoop(aritOp.getY())) {
                return aritOp.getY();
            } else if (aritOp.getY() == base && loop.isOutsideLoop(aritOp.getX())) {
                return aritOp.getX();
            }
        }
        return null;
    }

    private static ValueNode mul(LoopEx loop, ValueNode op, ValueNode base) {
        if (op instanceof MulNode) {
            MulNode mul = (MulNode) op;
            if (mul.getX() == base && loop.isOutsideLoop(mul.getY())) {
                return mul.getY();
            } else if (mul.getY() == base && loop.isOutsideLoop(mul.getX())) {
                return mul.getX();
            }
        }
        if (op instanceof LeftShiftNode) {
            LeftShiftNode shift = (LeftShiftNode) op;
            if (shift.getX() == base && shift.getY().isConstant()) {
                return ConstantNode.forIntegerStamp(base.stamp(NodeView.DEFAULT), 1 << shift.getY().asJavaConstant().asInt(), base.graph());
            }
        }
        return null;
    }

    /**
     * Deletes any nodes created within the scope of this object that have no usages.
     */
    public void deleteUnusedNodes() {
        if (ivs != null) {
            for (InductionVariable iv : ivs.getValues()) {
                iv.deleteUnusedNodes();
            }
        }
    }

    /**
     * @return true if all nodes in the loop can be duplicated.
     */
    public boolean canDuplicateLoop() {
        for (Node node : inside().nodes()) {
            if (node instanceof ControlFlowAnchored) {
                return false;
            }
            if (node instanceof FrameState) {
                FrameState frameState = (FrameState) node;
                if (frameState.bci == BytecodeFrame.AFTER_EXCEPTION_BCI || frameState.bci == BytecodeFrame.UNWIND_BCI) {
                    return false;
                }
            }
        }
        return true;
    }
}
