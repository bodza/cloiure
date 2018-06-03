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
    // @def
    public static final LIRInstructionClass<AMD64HotSpotConstantRetrievalOp> TYPE = LIRInstructionClass.create(AMD64HotSpotConstantRetrievalOp.class);

    @Def
    // @field
    protected AllocatableValue ___result;
    // @field
    protected final Constant[] ___constants;
    @Alive
    // @field
    protected AllocatableValue[] ___constantDescriptions;
    @Temp
    // @field
    protected AllocatableValue[] ___gotSlotOffsetParameters;
    @Temp
    // @field
    protected AllocatableValue[] ___descriptionParameters;
    @Temp
    // @field
    protected Value[] ___callTemps;
    // @State
    // @field
    protected LIRFrameState ___state;
    // @field
    private final ForeignCallLinkage ___callLinkage;
    // @field
    private final Object[] ___notes;

    // @class AMD64HotSpotConstantRetrievalOp.CollectTemporaries
    // @closure
    private final class CollectTemporaries implements ValueProcedure
    {
        // @field
        ArrayList<Value> ___values = new ArrayList<>();

        // @cons
        CollectTemporaries()
        {
            super();
            AMD64HotSpotConstantRetrievalOp.this.forEachTemp(this);
        }

        public Value[] asArray()
        {
            return this.___values.toArray(new Value[this.___values.size()]);
        }

        @Override
        public Value doValue(Value __value, OperandMode __mode, EnumSet<OperandFlag> __flags)
        {
            this.___values.add(__value);
            return __value;
        }
    }

    // @cons
    public AMD64HotSpotConstantRetrievalOp(Constant[] __constants, AllocatableValue[] __constantDescriptions, LIRFrameState __state, ForeignCallLinkage __callLinkage, Object[] __notes)
    {
        super(TYPE);
        this.___constantDescriptions = __constantDescriptions;
        this.___constants = __constants;
        this.___state = __state;
        this.___notes = __notes;

        // call arguments
        CallingConvention __callingConvention = __callLinkage.getOutgoingCallingConvention();
        this.___gotSlotOffsetParameters = new AllocatableValue[__constants.length];
        int __argIndex = 0;
        for (int __i = 0; __i < __constants.length; __i++, __argIndex++)
        {
            this.___gotSlotOffsetParameters[__i] = __callingConvention.getArgument(__argIndex);
        }
        this.___descriptionParameters = new AllocatableValue[__constantDescriptions.length];
        for (int __i = 0; __i < __constantDescriptions.length; __i++, __argIndex++)
        {
            this.___descriptionParameters[__i] = __callingConvention.getArgument(__argIndex);
        }
        this.___result = __callingConvention.getReturn();

        this.___callLinkage = __callLinkage;

        // compute registers that are killed by the stub, but are not used as other temps.
        this.___callTemps = new Value[0];
        this.___callTemps = LIRValueUtil.subtractRegisters(__callLinkage.getTemporaries(), new CollectTemporaries().asArray());
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // metadata_adr
        for (int __i = 0; __i < this.___constants.length; __i++)
        {
            __crb.recordInlineDataInCodeWithNote(this.___constants[__i], this.___notes[__i]);
            __masm.leaq(ValueUtil.asRegister(this.___gotSlotOffsetParameters[__i]), __masm.getPlaceholder(-1));
        }

        for (int __i = 0; __i < this.___constantDescriptions.length; __i++)
        {
            __masm.movq(ValueUtil.asRegister(this.___descriptionParameters[__i]), ValueUtil.asRegister(this.___constantDescriptions[__i]));
        }

        __masm.call();
    }
}
