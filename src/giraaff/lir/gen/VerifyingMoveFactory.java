package giraaff.lir.gen;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;
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
        return inner.createMove(result, input);
    }

    @Override
    public LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input)
    {
        return inner.createStackMove(result, input);
    }

    @Override
    public LIRInstruction createLoad(AllocatableValue result, Constant input)
    {
        return inner.createLoad(result, input);
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue result, Constant input)
    {
        return inner.createStackLoad(result, input);
    }
}
