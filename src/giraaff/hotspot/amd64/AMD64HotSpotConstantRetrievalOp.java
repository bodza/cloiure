package giraaff.hotspot.amd64;

import java.util.ArrayList;
import java.util.EnumSet;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.ValueProcedure;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64HotSpotConstantRetrievalOp
public final class AMD64HotSpotConstantRetrievalOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotConstantRetrievalOp> TYPE = LIRInstructionClass.create(AMD64HotSpotConstantRetrievalOp.class);

    @Def protected AllocatableValue result;
    protected final Constant[] constants;
    @Alive protected AllocatableValue[] constantDescriptions;
    @Temp protected AllocatableValue[] gotSlotOffsetParameters;
    @Temp protected AllocatableValue[] descriptionParameters;
    @Temp protected Value[] callTemps;
    // @State
    protected LIRFrameState state;
    private final ForeignCallLinkage callLinkage;
    private final Object[] notes;

    // @class AMD64HotSpotConstantRetrievalOp.CollectTemporaries
    // @closure
    private final class CollectTemporaries implements ValueProcedure
    {
        ArrayList<Value> values = new ArrayList<>();

        // @cons
        CollectTemporaries()
        {
            super();
            AMD64HotSpotConstantRetrievalOp.this.forEachTemp(this);
        }

        public Value[] asArray()
        {
            return values.toArray(new Value[values.size()]);
        }

        @Override
        public Value doValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            values.add(value);
            return value;
        }
    }

    // @cons
    public AMD64HotSpotConstantRetrievalOp(Constant[] constants, AllocatableValue[] constantDescriptions, LIRFrameState state, ForeignCallLinkage callLinkage, Object[] notes)
    {
        super(TYPE);
        this.constantDescriptions = constantDescriptions;
        this.constants = constants;
        this.state = state;
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
            masm.leaq(ValueUtil.asRegister(gotSlotOffsetParameters[i]), masm.getPlaceholder(-1));
        }

        for (int i = 0; i < constantDescriptions.length; i++)
        {
            masm.movq(ValueUtil.asRegister(descriptionParameters[i]), ValueUtil.asRegister(constantDescriptions[i]));
        }

        masm.call();
    }
}
