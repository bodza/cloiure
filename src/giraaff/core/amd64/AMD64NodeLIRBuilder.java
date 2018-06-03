package giraaff.core.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.GraalOptions;
import giraaff.core.gen.NodeLIRBuilder;
import giraaff.lir.LIRFrameState;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.IndirectCallTargetNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IntegerDivRemNode;
import giraaff.nodes.cfg.Block;
import giraaff.util.GraalError;

// @class AMD64NodeLIRBuilder
public abstract class AMD64NodeLIRBuilder extends NodeLIRBuilder
{
    // @cons
    public AMD64NodeLIRBuilder(StructuredGraph __graph, LIRGeneratorTool __gen)
    {
        super(__graph, __gen);
    }

    @Override
    protected void emitIndirectCall(IndirectCallTargetNode __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __callState)
    {
        Value __targetAddressSrc = operand(__callTarget.computedAddress());
        AllocatableValue __targetAddress = AMD64.rax.asValue(__targetAddressSrc.getValueKind());
        gen.emitMove(__targetAddress, __targetAddressSrc);
        append(new AMD64Call.IndirectCallOp(__callTarget.targetMethod(), __result, __parameters, __temps, __targetAddress, __callState));
    }

    @Override
    protected boolean peephole(ValueNode __valueNode)
    {
        if (__valueNode instanceof IntegerDivRemNode)
        {
            AMD64ArithmeticLIRGenerator __arithmeticGen = (AMD64ArithmeticLIRGenerator) gen.getArithmetic();
            IntegerDivRemNode __divRem = (IntegerDivRemNode) __valueNode;
            FixedNode __node = __divRem.next();
            while (true)
            {
                if (__node instanceof IfNode)
                {
                    IfNode __ifNode = (IfNode) __node;
                    double __probability = __ifNode.getTrueSuccessorProbability();
                    if (__probability == 1.0)
                    {
                        __node = __ifNode.trueSuccessor();
                    }
                    else if (__probability == 0.0)
                    {
                        __node = __ifNode.falseSuccessor();
                    }
                    else
                    {
                        break;
                    }
                }
                else if (!(__node instanceof FixedWithNextNode))
                {
                    break;
                }

                FixedWithNextNode __fixedWithNextNode = (FixedWithNextNode) __node;
                if (__fixedWithNextNode instanceof IntegerDivRemNode)
                {
                    IntegerDivRemNode __otherDivRem = (IntegerDivRemNode) __fixedWithNextNode;
                    if (__divRem.getOp() != __otherDivRem.getOp() && __divRem.getType() == __otherDivRem.getType())
                    {
                        if (__otherDivRem.getX() == __divRem.getX() && __otherDivRem.getY() == __divRem.getY() && !hasOperand(__otherDivRem))
                        {
                            Value[] __results;
                            switch (__divRem.getType())
                            {
                                case SIGNED:
                                    __results = __arithmeticGen.emitSignedDivRem(operand(__divRem.getX()), operand(__divRem.getY()), state((DeoptimizingNode) __valueNode));
                                    break;
                                case UNSIGNED:
                                    __results = __arithmeticGen.emitUnsignedDivRem(operand(__divRem.getX()), operand(__divRem.getY()), state((DeoptimizingNode) __valueNode));
                                    break;
                                default:
                                    throw GraalError.shouldNotReachHere();
                            }
                            switch (__divRem.getOp())
                            {
                                case DIV:
                                    setResult(__divRem, __results[0]);
                                    setResult(__otherDivRem, __results[1]);
                                    break;
                                case REM:
                                    setResult(__divRem, __results[1]);
                                    setResult(__otherDivRem, __results[0]);
                                    break;
                                default:
                                    throw GraalError.shouldNotReachHere();
                            }
                            return true;
                        }
                    }
                }
                __node = __fixedWithNextNode.next();
            }
        }
        return false;
    }

    @Override
    public AMD64LIRGenerator getLIRGeneratorTool()
    {
        return (AMD64LIRGenerator) gen;
    }

    @Override
    public void doBlockPrologue(Block __block)
    {
        if (GraalOptions.mitigateSpeculativeExecutionAttacks)
        {
            boolean __hasControlSplitPredecessor = false;
            for (Block __b : __block.getPredecessors())
            {
                if (__b.getSuccessorCount() > 1)
                {
                    __hasControlSplitPredecessor = true;
                    break;
                }
            }
            boolean __isStartBlock = __block.getPredecessorCount() == 0;
            if (__hasControlSplitPredecessor || __isStartBlock)
            {
                getLIRGeneratorTool().emitLFence();
            }
        }
    }
}
