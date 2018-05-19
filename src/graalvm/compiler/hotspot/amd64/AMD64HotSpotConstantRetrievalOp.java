package graalvm.compiler.hotspot.amd64;

import static jdk.vm.ci.code.ValueUtil.asRegister;

import java.util.ArrayList;
import java.util.EnumSet;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.core.common.spi.ForeignCallLinkage;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.LIRValueUtil;
import graalvm.compiler.lir.ValueProcedure;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

public final class AMD64HotSpotConstantRetrievalOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotConstantRetrievalOp> TYPE = LIRInstructionClass.create(AMD64HotSpotConstantRetrievalOp.class);

    @Def protected AllocatableValue result;
    protected final Constant[] constants;
    @Alive protected AllocatableValue[] constantDescriptions;
    @Temp protected AllocatableValue[] gotSlotOffsetParameters;
    @Temp protected AllocatableValue[] descriptionParameters;
    @Temp protected Value[] callTemps;
    @State protected LIRFrameState frameState;
    private final ForeignCallLinkage callLinkage;
    private final Object[] notes;

    private class CollectTemporaries implements ValueProcedure
    {
        ArrayList<Value> values = new ArrayList<>();

        CollectTemporaries()
        {
            forEachTemp(this);
        }

        public Value[] asArray()
        {
            Value[] copy = new Value[values.size()];
            return values.toArray(copy);
        }

        @Override
        public Value doValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            values.add(value);
            return value;
        }
    }

    public AMD64HotSpotConstantRetrievalOp(Constant[] constants, AllocatableValue[] constantDescriptions, LIRFrameState frameState, ForeignCallLinkage callLinkage, Object[] notes)
    {
        super(TYPE);
        this.constantDescriptions = constantDescriptions;
        this.constants = constants;
        this.frameState = frameState;
        this.notes = notes;

        // call arguments
        CallingConvention callingConvention = callLinkage.getOutgoingCallingConvention();
        this.gotSlotOffsetParameters = new AllocatableValue[constants.length];
        int argIndex = 0;
        for (int i = 0; i < constants.length; i++, argIndex++)
        {
            this.gotSlotOffsetParameters[i] = callingConvention.getArgument(argIndex);
        }
        this.descriptionParameters = new AllocatableValue[constantDescriptions.length];
        for (int i = 0; i < constantDescriptions.length; i++, argIndex++)
        {
            this.descriptionParameters[i] = callingConvention.getArgument(argIndex);
        }
        this.result = callingConvention.getReturn();

        this.callLinkage = callLinkage;

        // compute registers that are killed by the stub, but are not used as other temps.
        this.callTemps = new Value[0];
        this.callTemps = LIRValueUtil.subtractRegisters(callLinkage.getTemporaries(), new CollectTemporaries().asArray());
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        // metadata_adr
        for (int i = 0; i < constants.length; i++)
        {
            crb.recordInlineDataInCodeWithNote(constants[i], notes[i]);
            masm.leaq(asRegister(gotSlotOffsetParameters[i]), masm.getPlaceholder(-1));
        }

        for (int i = 0; i < constantDescriptions.length; i++)
        {
            masm.movq(asRegister(descriptionParameters[i]), asRegister(constantDescriptions[i]));
        }

        final int before = masm.position();
        masm.call();
        final int after = masm.position();
        crb.recordDirectCall(before, after, callLinkage, frameState);
    }
}
