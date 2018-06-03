package giraaff.hotspot.amd64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LabelRef;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.amd64.AMD64ControlFlow;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64HotSpotStrategySwitchOp
final class AMD64HotSpotStrategySwitchOp extends AMD64ControlFlow.StrategySwitchOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotStrategySwitchOp> TYPE = LIRInstructionClass.create(AMD64HotSpotStrategySwitchOp.class);

    // @cons
    AMD64HotSpotStrategySwitchOp(SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
    {
        super(TYPE, __strategy, __keyTargets, __defaultTarget, __key, __scratch);
    }

    @Override
    public void emitCode(final CompilationResultBuilder __crb, final AMD64MacroAssembler __masm)
    {
        strategy.run(new HotSpotSwitchClosure(ValueUtil.asRegister(key), __crb, __masm));
    }

    // @class AMD64HotSpotStrategySwitchOp.HotSpotSwitchClosure
    // @closure
    public final class HotSpotSwitchClosure extends SwitchClosure
    {
        // @cons
        protected HotSpotSwitchClosure(Register __keyRegister, CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            super(__keyRegister, __crb, __masm);
        }

        @Override
        protected void emitComparison(Constant __c)
        {
            if (__c instanceof HotSpotMetaspaceConstant)
            {
                HotSpotMetaspaceConstant __meta = (HotSpotMetaspaceConstant) __c;
                if (__meta.isCompressed())
                {
                    crb.recordInlineDataInCode(__meta);
                    masm.cmpl(keyRegister, 0xDEADDEAD);
                }
                else
                {
                    AMD64Address __addr = (AMD64Address) crb.recordDataReferenceInCode(__meta, 8);
                    masm.cmpq(keyRegister, __addr);
                }
            }
            else
            {
                super.emitComparison(__c);
            }
        }
    }
}
