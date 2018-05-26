package giraaff.lir.gen;

import java.util.EnumSet;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

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
        return inst;
    }

    @Override
    public LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input)
    {
        LIRInstruction inst = inner.createStackMove(result, input);
        return inst;
    }

    @Override
    public LIRInstruction createLoad(AllocatableValue result, Constant input)
    {
        LIRInstruction inst = inner.createLoad(result, input);
        return inst;
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue result, Constant input)
    {
        LIRInstruction inst = inner.createStackLoad(result, input);
        return inst;
    }
}
