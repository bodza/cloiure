package giraaff.lir.gen;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

///
// Wrapper for {@link MoveFactory} that checks that the instructions created adhere to the contract
// of {@link MoveFactory}.
///
// @class VerifyingMoveFactory
public final class VerifyingMoveFactory implements MoveFactory
{
    // @field
    private final MoveFactory ___inner;

    // @cons
    public VerifyingMoveFactory(MoveFactory __inner)
    {
        super();
        this.___inner = __inner;
    }

    @Override
    public boolean canInlineConstant(Constant __c)
    {
        return this.___inner.canInlineConstant(__c);
    }

    @Override
    public boolean allowConstantToStackMove(Constant __constant)
    {
        return this.___inner.allowConstantToStackMove(__constant);
    }

    @Override
    public LIRInstruction createMove(AllocatableValue __result, Value __input)
    {
        return this.___inner.createMove(__result, __input);
    }

    @Override
    public LIRInstruction createStackMove(AllocatableValue __result, AllocatableValue __input)
    {
        return this.___inner.createStackMove(__result, __input);
    }

    @Override
    public LIRInstruction createLoad(AllocatableValue __result, Constant __input)
    {
        return this.___inner.createLoad(__result, __input);
    }

    @Override
    public LIRInstruction createStackLoad(AllocatableValue __result, Constant __input)
    {
        return this.___inner.createStackLoad(__result, __input);
    }
}
