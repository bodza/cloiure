package graalvm.compiler.lir.gen;

import static graalvm.compiler.lir.LIRValueUtil.isJavaConstant;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;

import java.util.EnumSet;

import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.StandardOp.LoadConstantOp;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

/**
 * Wrapper for {@link MoveFactory} that checks that the instructions created adhere to the contract
 * of {@link MoveFactory}.
 */
public final class VerifyingMoveFactory implements MoveFactory
{
    private final MoveFactory inner;

    public VerifyingMoveFactory(MoveFactory inner)
    {
        this.inner = inner;
    }

    @Override
    public boolean canInlineConstant(Constant c)
    {
        return inner.canInlineConstant(c);
    }

    @Override
    public boolean allowConstantToStackMove(Constant constant)
    {
        return inner.allowConstantToStackMove(constant);
    }

    @Override
    public LIRInstruction createMove(AllocatableValue result, Value input)
    {
        LIRInstruction inst = inner.createMove(result, input);
        assert checkResult(inst, result, input);
        return inst;
    }

    @Override
    public LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input)
    {
        LIRInstruction inst = inner.createStackMove(result, input);
        assert checkResult(inst, result, input);
        return inst;
    }

    @Override
    public LIRInstruction createLoad(AllocatableValue result, Constant input)
    {
        LIRInstruction inst = inner.createLoad(result, input);
        assert LoadConstantOp.isLoadConstantOp(inst) && checkResult(inst, result, null);
        return inst;
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue result, Constant input)
    {
        LIRInstruction inst = inner.createStackLoad(result, input);
        assert LoadConstantOp.isLoadConstantOp(inst) && checkResult(inst, result, null);
        return inst;
    }

    /** Closure for {@link VerifyingMoveFactory#checkResult}. */
    @SuppressWarnings("unused")
    private static class CheckClosure
    {
        private final AllocatableValue result;
        private final Value input;

        private int tempCount = 0;
        private int aliveCount = 0;
        private int stateCount = 0;
        private int inputCount = 0;
        private int outputCount = 0;

        CheckClosure(AllocatableValue result, Value input)
        {
            this.result = result;
            this.input = input;
        }

        void tempProc(LIRInstruction op, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            assert false : String.format("SpillMoveFactory: Instruction %s is not allowed to contain operand %s of mode %s", op, value, mode);
            tempCount++;
        }

        void stateProc(LIRInstruction op, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            assert false : String.format("SpillMoveFactory: Instruction %s is not allowed to contain operand %s of mode %s", op, value, mode);
            stateCount++;
        }

        void aliveProc(LIRInstruction op, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            assert !isVariable(value) && flags.contains(OperandFlag.UNINITIALIZED) : String.format("SpillMoveFactory: Instruction %s is not allowed to contain operand %s of mode %s", op, value, mode);
            aliveCount++;
        }

        void inputProc(LIRInstruction op, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            assert value.equals(input) || isJavaConstant(value) : String.format("SpillMoveFactory: Instruction %s can only have %s as input, got %s", op, input, value);
            inputCount++;
        }

        void outputProc(LIRInstruction op, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            assert value.equals(result) : String.format("SpillMoveFactory: Instruction %s can only have %s as input, got %s", op, input, value);
            outputCount++;
        }
    }

    /**
     * Checks that the instructions adheres to the contract of {@link MoveFactory}.
     */
    private static boolean checkResult(LIRInstruction inst, AllocatableValue result, Value input)
    {
        VerifyingMoveFactory.CheckClosure c = new CheckClosure(result, input);
        inst.visitEachInput(c::inputProc);
        inst.visitEachOutput(c::outputProc);
        inst.visitEachAlive(c::aliveProc);
        inst.visitEachTemp(c::tempProc);
        inst.visitEachState(c::stateProc);

        assert c.outputCount >= 1 : "no output produced" + inst;
        assert c.stateCount == 0 : "SpillMoveFactory: instruction must not have a state: " + inst;
        return true;
    }
}
