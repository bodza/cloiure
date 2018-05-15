package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.ArithmeticOpTable.BinaryOp;
import graalvm.compiler.core.common.type.ArithmeticOpTable.BinaryOp.Mul;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable.BinaryCommutative;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.Value;

@NodeInfo(shortName = "*", cycles = CYCLES_2)
public class MulNode extends BinaryArithmeticNode<Mul> implements NarrowableArithmeticNode, BinaryCommutative<ValueNode> {

    public static final NodeClass<MulNode> TYPE = NodeClass.create(MulNode.class);

    public MulNode(ValueNode x, ValueNode y) {
        this(TYPE, x, y);
    }

    protected MulNode(NodeClass<? extends MulNode> c, ValueNode x, ValueNode y) {
        super(c, ArithmeticOpTable::getMul, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view) {
        BinaryOp<Mul> op = ArithmeticOpTable.forStamp(x.stamp(view)).getMul();
        Stamp stamp = op.foldStamp(x.stamp(view), y.stamp(view));
        ConstantNode tryConstantFold = tryConstantFold(op, x, y, stamp, view);
        if (tryConstantFold != null) {
            return tryConstantFold;
        }
        return canonical(null, op, stamp, x, y, view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY) {
        ValueNode ret = super.canonical(tool, forX, forY);
        if (ret != this) {
            return ret;
        }

        if (forX.isConstant() && !forY.isConstant()) {
            // we try to swap and canonicalize
            ValueNode improvement = canonical(tool, forY, forX);
            if (improvement != this) {
                return improvement;
            }
            // if this fails we only swap
            return new MulNode(forY, forX);
        }
        BinaryOp<Mul> op = getOp(forX, forY);
        NodeView view = NodeView.from(tool);
        return canonical(this, op, stamp(view), forX, forY, view);
    }

    private static ValueNode canonical(MulNode self, BinaryOp<Mul> op, Stamp stamp, ValueNode forX, ValueNode forY, NodeView view) {
        if (forY.isConstant()) {
            Constant c = forY.asConstant();
            if (op.isNeutral(c)) {
                return forX;
            }

            if (c instanceof PrimitiveConstant && ((PrimitiveConstant) c).getJavaKind().isNumericInteger()) {
                long i = ((PrimitiveConstant) c).asLong();
                ValueNode result = canonical(stamp, forX, i, view);
                if (result != null) {
                    return result;
                }
            }

            if (op.isAssociative()) {
                // canonicalize expressions like "(a * 1) * 2"
                return reassociate(self != null ? self : (MulNode) new MulNode(forX, forY).maybeCommuteInputs(), ValueNode.isConstantPredicate(), forX, forY, view);
            }
        }
        return self != null ? self : new MulNode(forX, forY).maybeCommuteInputs();
    }

    public static ValueNode canonical(Stamp stamp, ValueNode forX, long i, NodeView view) {
        if (i == 0) {
            return ConstantNode.forIntegerStamp(stamp, 0);
        } else if (i == 1) {
            return forX;
        } else if (i == -1) {
            return NegateNode.create(forX, view);
        } else if (i > 0) {
            if (CodeUtil.isPowerOf2(i)) {
                return new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(i)));
            } else if (CodeUtil.isPowerOf2(i - 1)) {
                return AddNode.create(new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(i - 1))), forX, view);
            } else if (CodeUtil.isPowerOf2(i + 1)) {
                return SubNode.create(new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(i + 1))), forX, view);
            } else {
                int bitCount = Long.bitCount(i);
                long highestBitValue = Long.highestOneBit(i);
                if (bitCount == 2) {
                    // e.g., 0b1000_0010
                    long lowerBitValue = i - highestBitValue;
                    assert highestBitValue > 0 && lowerBitValue > 0;
                    ValueNode left = new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(highestBitValue)));
                    ValueNode right = lowerBitValue == 1 ? forX : new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(lowerBitValue)));
                    return AddNode.create(left, right, view);
                } else {
                    // e.g., 0b1111_1101
                    int shiftToRoundUpToPowerOf2 = CodeUtil.log2(highestBitValue) + 1;
                    long subValue = (1 << shiftToRoundUpToPowerOf2) - i;
                    if (CodeUtil.isPowerOf2(subValue) && shiftToRoundUpToPowerOf2 < ((IntegerStamp) stamp).getBits()) {
                        assert CodeUtil.log2(subValue) >= 1;
                        ValueNode left = new LeftShiftNode(forX, ConstantNode.forInt(shiftToRoundUpToPowerOf2));
                        ValueNode right = new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(subValue)));
                        return SubNode.create(left, right, view);
                    }
                }
            }
        } else if (i < 0) {
            if (CodeUtil.isPowerOf2(-i)) {
                return NegateNode.create(LeftShiftNode.create(forX, ConstantNode.forInt(CodeUtil.log2(-i)), view), view);
            }
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen) {
        Value op1 = nodeValueMap.operand(getX());
        Value op2 = nodeValueMap.operand(getY());
        if (shouldSwapInputs(nodeValueMap)) {
            Value tmp = op1;
            op1 = op2;
            op2 = tmp;
        }
        nodeValueMap.setResult(this, gen.emitMul(op1, op2, false));
    }
}