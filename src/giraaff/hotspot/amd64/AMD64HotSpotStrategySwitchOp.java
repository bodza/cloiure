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

final class AMD64HotSpotStrategySwitchOp extends AMD64ControlFlow.StrategySwitchOp
{
    public static final LIRInstructionClass<AMD64HotSpotStrategySwitchOp> TYPE = LIRInstructionClass.create(AMD64HotSpotStrategySwitchOp.class);

    AMD64HotSpotStrategySwitchOp(SwitchStrategy strategy, LabelRef[] keyTargets, LabelRef defaultTarget, Value key, Value scratch)
    {
        super(TYPE, strategy, keyTargets, defaultTarget, key, scratch);
    }

    @Override
    public void emitCode(final CompilationResultBuilder crb, final AMD64MacroAssembler masm)
    {
        strategy.run(new HotSpotSwitchClosure(ValueUtil.asRegister(key), crb, masm));
    }

    public class HotSpotSwitchClosure extends SwitchClosure
    {
        protected HotSpotSwitchClosure(Register keyRegister, CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            super(keyRegister, crb, masm);
        }

        @Override
        protected void emitComparison(Constant c)
        {
            if (c instanceof HotSpotMetaspaceConstant)
            {
                HotSpotMetaspaceConstant meta = (HotSpotMetaspaceConstant) c;
                if (meta.isCompressed())
                {
                    crb.recordInlineDataInCode(meta);
                    masm.cmpl(keyRegister, 0xDEADDEAD);
                }
                else
                {
                    AMD64Address addr = (AMD64Address) crb.recordDataReferenceInCode(meta, 8);
                    masm.cmpq(keyRegister, addr);
                }
            }
            else
            {
                super.emitComparison(c);
            }
        }
    }
}
