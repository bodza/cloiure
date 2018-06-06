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

    // @cons AMD64HotSpotStrategySwitchOp
    AMD64HotSpotStrategySwitchOp(SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
    {
        super(TYPE, __strategy, __keyTargets, __defaultTarget, __key, __scratch);
    }

    @Override
    public void emitCode(final CompilationResultBuilder __crb, final AMD64MacroAssembler __masm)
    {
        this.___strategy.run(new AMD64HotSpotStrategySwitchOp.HotSpotSwitchClosure(ValueUtil.asRegister(this.___key), __crb, __masm));
    }

    // @class AMD64HotSpotStrategySwitchOp.HotSpotSwitchClosure
    // @closure
    public final class HotSpotSwitchClosure extends AMD64ControlFlow.StrategySwitchOp.AMD64SwitchClosure
    {
        // @cons AMD64HotSpotStrategySwitchOp.HotSpotSwitchClosure
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
                    this.___crb.recordInlineDataInCode(__meta);
                    this.___masm.cmpl(this.___keyRegister, 0xDEADDEAD);
                }
                else
                {
                    AMD64Address __addr = (AMD64Address) this.___crb.recordDataReferenceInCode(__meta, 8);
                    this.___masm.cmpq(this.___keyRegister, __addr);
                }
            }
            else
            {
                super.emitComparison(__c);
            }
        }
    }
}
