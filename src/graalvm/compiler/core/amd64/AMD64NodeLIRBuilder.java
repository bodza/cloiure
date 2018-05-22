package graalvm.compiler.core.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.amd64.AMD64NodeLIRBuilder.Options;
import graalvm.compiler.core.gen.NodeLIRBuilder;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.amd64.AMD64Call;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodes.DeoptimizingNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.IndirectCallTargetNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.IntegerDivRemNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;

public abstract class AMD64NodeLIRBuilder extends NodeLIRBuilder
{
    public static class Options
    {
        // Option "AMD64: Emit lfence instructions at the beginning of basic blocks."
        public static final OptionKey<Boolean> MitigateSpeculativeExecutionAttacks = new OptionKey<>(false);
    }

    public AMD64NodeLIRBuilder(StructuredGraph graph, LIRGeneratorTool gen, AMD64NodeMatchRules nodeMatchRules)
    {
        super(graph, gen, nodeMatchRules);
    }

    @Override
    protected void emitIndirectCall(IndirectCallTargetNode callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState callState)
    {
        Value targetAddressSrc = operand(callTarget.computedAddress());
        AllocatableValue targetAddress = AMD64.rax.asValue(targetAddressSrc.getValueKind());
        gen.emitMove(targetAddress, targetAddressSrc);
        append(new AMD64Call.IndirectCallOp(callTarget.targetMethod(), result, parameters, temps, targetAddress, callState));
    }

    @Override
    protected boolean peephole(ValueNode valueNode)
    {
        if (valueNode instanceof IntegerDivRemNode)
        {
            AMD64ArithmeticLIRGenerator arithmeticGen = (AMD64ArithmeticLIRGenerator) gen.getArithmetic();
            IntegerDivRemNode divRem = (IntegerDivRemNode) valueNode;
            FixedNode node = divRem.next();
            while (true)
            {
                if (node instanceof IfNode)
                {
                    IfNode ifNode = (IfNode) node;
                    double probability = ifNode.getTrueSuccessorProbability();
                    if (probability == 1.0)
                    {
                        node = ifNode.trueSuccessor();
                    }
                    else if (probability == 0.0)
                    {
                        node = ifNode.falseSuccessor();
                    }
                    else
                    {
                        break;
                    }
                }
                else if (!(node instanceof FixedWithNextNode))
                {
                    break;
                }

                FixedWithNextNode fixedWithNextNode = (FixedWithNextNode) node;
                if (fixedWithNextNode instanceof IntegerDivRemNode)
                {
                    IntegerDivRemNode otherDivRem = (IntegerDivRemNode) fixedWithNextNode;
                    if (divRem.getOp() != otherDivRem.getOp() && divRem.getType() == otherDivRem.getType())
                    {
                        if (otherDivRem.getX() == divRem.getX() && otherDivRem.getY() == divRem.getY() && !hasOperand(otherDivRem))
                        {
                            Value[] results;
                            switch (divRem.getType())
                            {
                                case SIGNED:
                                    results = arithmeticGen.emitSignedDivRem(operand(divRem.getX()), operand(divRem.getY()), state((DeoptimizingNode) valueNode));
                                    break;
                                case UNSIGNED:
                                    results = arithmeticGen.emitUnsignedDivRem(operand(divRem.getX()), operand(divRem.getY()), state((DeoptimizingNode) valueNode));
                                    break;
                                default:
                                    throw GraalError.shouldNotReachHere();
                            }
                            switch (divRem.getOp())
                            {
                                case DIV:
                                    setResult(divRem, results[0]);
                                    setResult(otherDivRem, results[1]);
                                    break;
                                case REM:
                                    setResult(divRem, results[1]);
                                    setResult(otherDivRem, results[0]);
                                    break;
                                default:
                                    throw GraalError.shouldNotReachHere();
                            }
                            return true;
                        }
                    }
                }
                node = fixedWithNextNode.next();
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
    public void doBlockPrologue(Block block, OptionValues options)
    {
        if (Options.MitigateSpeculativeExecutionAttacks.getValue(options))
        {
            boolean hasControlSplitPredecessor = false;
            for (Block b : block.getPredecessors())
            {
                if (b.getSuccessorCount() > 1)
                {
                    hasControlSplitPredecessor = true;
                    break;
                }
            }
            boolean isStartBlock = block.getPredecessorCount() == 0;
            if (hasControlSplitPredecessor || isStartBlock)
            {
                getLIRGeneratorTool().emitLFence();
            }
        }
    }
}
