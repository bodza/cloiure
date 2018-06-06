package giraaff.hotspot.amd64;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.core.amd64.AMD64ArithmeticLIRGenerator;
import giraaff.core.amd64.AMD64LIRGenerator;
import giraaff.core.amd64.AMD64MoveFactoryBase;
import giraaff.core.common.CompressEncoding;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotLIRGenerationResult;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.hotspot.HotSpotLockStack;
import giraaff.hotspot.HotSpotLockStackHolder;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.stubs.Stub;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LabelRef;
import giraaff.lir.StandardOp;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.Variable;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.amd64.AMD64AddressValue;
import giraaff.lir.amd64.AMD64CCall;
import giraaff.lir.amd64.AMD64ControlFlow;
import giraaff.lir.amd64.AMD64FrameMapBuilder;
import giraaff.lir.amd64.AMD64Move;
import giraaff.lir.amd64.AMD64PrefetchOp;
import giraaff.lir.amd64.AMD64ReadTimestampCounter;
import giraaff.lir.amd64.AMD64RestoreRegistersOp;
import giraaff.lir.amd64.AMD64SaveRegistersOp;
import giraaff.lir.amd64.AMD64VZeroUpper;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.util.GraalError;

///
// LIR generator specialized for AMD64 HotSpot.
///
// @class AMD64HotSpotLIRGenerator
public final class AMD64HotSpotLIRGenerator extends AMD64LIRGenerator implements HotSpotLIRGenerator
{
    // @field
    private HotSpotLockStackHolder ___lockStackHolder;

    // @cons AMD64HotSpotLIRGenerator
    protected AMD64HotSpotLIRGenerator(HotSpotProviders __providers, LIRGenerationResult __lirGenRes)
    {
        this(__providers, __lirGenRes, new AMD64MoveFactoryBase.BackupSlotProvider(__lirGenRes.getFrameMapBuilder()));
    }

    // @cons AMD64HotSpotLIRGenerator
    private AMD64HotSpotLIRGenerator(HotSpotProviders __providers, LIRGenerationResult __lirGenRes, AMD64MoveFactoryBase.BackupSlotProvider __backupSlotProvider)
    {
        this(new AMD64HotSpotLIRKindTool(), new AMD64ArithmeticLIRGenerator(), new AMD64HotSpotMoveFactory(__backupSlotProvider), __providers, __lirGenRes);
    }

    // @cons AMD64HotSpotLIRGenerator
    protected AMD64HotSpotLIRGenerator(LIRKindTool __lirKindTool, AMD64ArithmeticLIRGenerator __arithmeticLIRGen, LIRGeneratorTool.MoveFactory __moveFactory, HotSpotProviders __providers, LIRGenerationResult __lirGenRes)
    {
        super(__lirKindTool, __arithmeticLIRGen, __moveFactory, __providers, __lirGenRes);
    }

    @Override
    public HotSpotProviders getProviders()
    {
        return (HotSpotProviders) super.getProviders();
    }

    ///
    // Utility for emitting the instruction to save RBP.
    ///
    // @class AMD64HotSpotLIRGenerator.SaveRbp
    // @closure
    final class SaveRbp
    {
        // @field
        final StandardOp.NoOp ___placeholder;

        ///
        // The slot reserved for saving RBP.
        ///
        // @field
        final StackSlot ___reservedSlot;

        // @cons AMD64HotSpotLIRGenerator.SaveRbp
        SaveRbp(StandardOp.NoOp __placeholder)
        {
            super();
            this.___placeholder = __placeholder;
            AMD64FrameMapBuilder __frameMapBuilder = (AMD64FrameMapBuilder) AMD64HotSpotLIRGenerator.this.getResult().getFrameMapBuilder();
            this.___reservedSlot = __frameMapBuilder.allocateRBPSpillSlot();
        }

        ///
        // Replaces this operation with the appropriate move for saving rbp.
        //
        // @param useStack specifies if rbp must be saved to the stack
        ///
        public AllocatableValue finalize(boolean __useStack)
        {
            AllocatableValue __dst;
            if (__useStack)
            {
                __dst = this.___reservedSlot;
            }
            else
            {
                ((AMD64FrameMapBuilder) AMD64HotSpotLIRGenerator.this.getResult().getFrameMapBuilder()).freeRBPSpillSlot();
                __dst = AMD64HotSpotLIRGenerator.this.newVariable(LIRKind.value(AMD64Kind.QWORD));
            }

            this.___placeholder.replace(AMD64HotSpotLIRGenerator.this.getResult().getLIR(), new AMD64Move.MoveFromRegOp(AMD64Kind.QWORD, __dst, AMD64.rbp.asValue(LIRKind.value(AMD64Kind.QWORD))));
            return __dst;
        }
    }

    // @field
    private AMD64HotSpotLIRGenerator.SaveRbp ___saveRbp;

    protected void emitSaveRbp()
    {
        StandardOp.NoOp __placeholder = new StandardOp.NoOp(getCurrentBlock(), getResult().getLIR().getLIRforBlock(getCurrentBlock()).size());
        append(__placeholder);
        this.___saveRbp = new AMD64HotSpotLIRGenerator.SaveRbp(__placeholder);
    }

    protected AMD64HotSpotLIRGenerator.SaveRbp getSaveRbp()
    {
        return this.___saveRbp;
    }

    ///
    // Helper instruction to reserve a stack slot for the whole method. Note that the actual users
    // of the stack slot might be inserted after stack slot allocation. This dummy instruction
    // ensures that the stack slot is alive and gets a real stack slot assigned.
    ///
    // @class AMD64HotSpotLIRGenerator.RescueSlotDummyOp
    private static final class RescueSlotDummyOp extends LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64HotSpotLIRGenerator.RescueSlotDummyOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLIRGenerator.RescueSlotDummyOp.class);

        @LIRInstruction.Alive({LIRInstruction.OperandFlag.STACK, LIRInstruction.OperandFlag.UNINITIALIZED})
        // @field
        private AllocatableValue ___slot;

        // @cons AMD64HotSpotLIRGenerator.RescueSlotDummyOp
        RescueSlotDummyOp(FrameMapBuilder __frameMapBuilder, LIRKind __kind)
        {
            super(TYPE);
            this.___slot = __frameMapBuilder.allocateSpillSlot(__kind);
        }

        public AllocatableValue getSlot()
        {
            return this.___slot;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
        }
    }

    // @field
    private AMD64HotSpotLIRGenerator.RescueSlotDummyOp ___rescueSlotOp;

    private AllocatableValue getOrInitRescueSlot()
    {
        return getOrInitRescueSlotOp().getSlot();
    }

    private AMD64HotSpotLIRGenerator.RescueSlotDummyOp getOrInitRescueSlotOp()
    {
        if (this.___rescueSlotOp == null)
        {
            // create dummy instruction to keep the rescue slot alive
            this.___rescueSlotOp = new AMD64HotSpotLIRGenerator.RescueSlotDummyOp(getResult().getFrameMapBuilder(), getLIRKindTool().getWordKind());
        }
        return this.___rescueSlotOp;
    }

    ///
    // List of epilogue operations that need to restore RBP.
    ///
    // @field
    List<AMD64HotSpotRestoreRbpOp> ___epilogueOps = new ArrayList<>(2);

    @Override
    public <I extends LIRInstruction> I append(I __op)
    {
        I __ret = super.append(__op);
        if (__op instanceof AMD64HotSpotRestoreRbpOp)
        {
            this.___epilogueOps.add((AMD64HotSpotRestoreRbpOp) __op);
        }
        return __ret;
    }

    @Override
    public VirtualStackSlot getLockSlot(int __lockDepth)
    {
        return getLockStack().makeLockSlot(__lockDepth);
    }

    private HotSpotLockStack getLockStack()
    {
        return this.___lockStackHolder.lockStack();
    }

    private Register findPollOnReturnScratchRegister()
    {
        RegisterConfig __regConfig = getProviders().getCodeCache().getRegisterConfig();
        for (Register __r : __regConfig.getAllocatableRegisters())
        {
            if (!__r.equals(__regConfig.getReturnRegister(JavaKind.Long)) && !__r.equals(AMD64.rbp))
            {
                return __r;
            }
        }
        throw GraalError.shouldNotReachHere();
    }

    // @field
    private Register ___pollOnReturnScratchRegister;

    @Override
    public void emitReturn(JavaKind __kind, Value __input)
    {
        AllocatableValue __operand = Value.ILLEGAL;
        if (__input != null)
        {
            __operand = resultOperandFor(__kind, __input.getValueKind());
            emitMove(__operand, __input);
        }
        if (this.___pollOnReturnScratchRegister == null)
        {
            this.___pollOnReturnScratchRegister = findPollOnReturnScratchRegister();
        }
        Register __thread = getProviders().getRegisters().getThreadRegister();
        append(new AMD64HotSpotReturnOp(__operand, getStub() != null, __thread, this.___pollOnReturnScratchRegister));
    }

    @Override
    public boolean needOnlyOopMaps()
    {
        // stubs only need oop maps
        return getResult().getStub() != null;
    }

    // @field
    private LIRFrameState ___currentRuntimeCallInfo;

    @Override
    protected void emitForeignCallOp(ForeignCallLinkage __linkage, Value __result, Value[] __arguments, Value[] __temps, LIRFrameState __info)
    {
        this.___currentRuntimeCallInfo = __info;
        HotSpotForeignCallLinkage __hsLinkage = (HotSpotForeignCallLinkage) __linkage;
        AMD64 __arch = (AMD64) target().arch;
        if (__arch.getFeatures().contains(AMD64.CPUFeature.AVX) && __hsLinkage.mayContainFP() && !__hsLinkage.isCompiledStub())
        {
            // If the target may contain FP ops, and it is not compiled by us, we may have an
            // AVX-SSE transition.
            //
            // We exclude the argument registers from the zeroing LIR instruction since it violates
            // the LIR semantics of @LIRInstruction.Temp that values must not be live. Note that the
            // emitted machine instruction actually zeros _all_ XMM registers which is fine since we
            // know that their upper half is not used.
            append(new AMD64VZeroUpper(__arguments));
        }
        super.emitForeignCallOp(__linkage, __result, __arguments, __temps, __info);
    }

    ///
    // @param savedRegisters the registers saved by this operation which may be subject to pruning
    // @param savedRegisterLocations the slots to which the registers are saved
    // @param supportsRemove determines if registers can be pruned
    ///
    protected AMD64SaveRegistersOp emitSaveRegisters(Register[] __savedRegisters, AllocatableValue[] __savedRegisterLocations, boolean __supportsRemove)
    {
        AMD64SaveRegistersOp __save = new AMD64SaveRegistersOp(__savedRegisters, __savedRegisterLocations, __supportsRemove);
        append(__save);
        return __save;
    }

    ///
    // Allocate a stack slot for saving a register.
    ///
    protected VirtualStackSlot allocateSaveRegisterLocation(Register __register)
    {
        PlatformKind __kind = target().arch.getLargestStorableKind(__register.getRegisterCategory());
        if (__kind.getVectorLength() > 1)
        {
            // we don't use vector registers, so there is no need to save them
            __kind = AMD64Kind.DOUBLE;
        }
        return getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(__kind));
    }

    ///
    // Adds a node to the graph that saves all allocatable registers to the stack.
    //
    // @param supportsRemove determines if registers can be pruned
    // @return the register save node
    ///
    private AMD64SaveRegistersOp emitSaveAllRegisters(Register[] __savedRegisters, boolean __supportsRemove)
    {
        AllocatableValue[] __savedRegisterLocations = new AllocatableValue[__savedRegisters.length];
        for (int __i = 0; __i < __savedRegisters.length; __i++)
        {
            __savedRegisterLocations[__i] = allocateSaveRegisterLocation(__savedRegisters[__i]);
        }
        return emitSaveRegisters(__savedRegisters, __savedRegisterLocations, __supportsRemove);
    }

    protected void emitRestoreRegisters(AMD64SaveRegistersOp __save)
    {
        append(new AMD64RestoreRegistersOp(__save.getSlots().clone(), __save));
    }

    ///
    // Gets the {@link Stub} this generator is generating code for or {@code null}
    // if a stub is not being generated.
    ///
    public Stub getStub()
    {
        return getResult().getStub();
    }

    @Override
    public HotSpotLIRGenerationResult getResult()
    {
        return ((HotSpotLIRGenerationResult) super.getResult());
    }

    public void setLockStackHolder(HotSpotLockStackHolder __lockStackHolder)
    {
        this.___lockStackHolder = __lockStackHolder;
    }

    @Override
    public Variable emitForeignCall(ForeignCallLinkage __linkage, LIRFrameState __state, Value... __args)
    {
        HotSpotForeignCallLinkage __hotspotLinkage = (HotSpotForeignCallLinkage) __linkage;
        boolean __destroysRegisters = __hotspotLinkage.destroysRegisters();

        AMD64SaveRegistersOp __save = null;
        Stub __stub = getStub();
        if (__destroysRegisters)
        {
            if (__stub != null && __stub.preservesRegisters())
            {
                Register[] __savedRegisters = getRegisterConfig().getAllocatableRegisters().toArray();
                __save = emitSaveAllRegisters(__savedRegisters, true);
            }
        }

        LIRFrameState __debugInfo = null;
        if (__hotspotLinkage.needsDebugInfo())
        {
            __debugInfo = __state;
        }

        Variable __result;
        if (__hotspotLinkage.needsJavaFrameAnchor())
        {
            Register __thread = getProviders().getRegisters().getThreadRegister();
            append(new AMD64HotSpotCRuntimeCallPrologueOp(HotSpotRuntime.threadLastJavaSpOffset, __thread));
            __result = super.emitForeignCall(__hotspotLinkage, __debugInfo, __args);
            append(new AMD64HotSpotCRuntimeCallEpilogueOp(HotSpotRuntime.threadLastJavaSpOffset, HotSpotRuntime.threadLastJavaFpOffset, HotSpotRuntime.threadLastJavaPcOffset, __thread));
        }
        else
        {
            __result = super.emitForeignCall(__hotspotLinkage, __debugInfo, __args);
        }

        if (__destroysRegisters)
        {
            if (__stub != null)
            {
                if (__stub.preservesRegisters())
                {
                    HotSpotLIRGenerationResult __generationResult = getResult();
                    LIRFrameState __key = this.___currentRuntimeCallInfo;
                    if (__key == null)
                    {
                        __key = LIRFrameState.NO_STATE;
                    }
                    __generationResult.getCalleeSaveInfo().put(__key, __save);
                    emitRestoreRegisters(__save);
                }
            }
        }

        return __result;
    }

    @Override
    public Value emitLoadObjectAddress(Constant __constant)
    {
        HotSpotObjectConstant __objectConstant = (HotSpotObjectConstant) __constant;
        LIRKind __kind = __objectConstant.isCompressed() ? getLIRKindTool().getNarrowOopKind() : getLIRKindTool().getObjectKind();
        Variable __result = newVariable(__kind);
        append(new AMD64HotSpotLoadAddressOp(__result, __constant, HotSpotConstantLoadAction.RESOLVE));
        return __result;
    }

    @Override
    public Value emitLoadMetaspaceAddress(Constant __constant, HotSpotConstantLoadAction __action)
    {
        HotSpotMetaspaceConstant __metaspaceConstant = (HotSpotMetaspaceConstant) __constant;
        LIRKind __kind = __metaspaceConstant.isCompressed() ? getLIRKindTool().getNarrowPointerKind() : getLIRKindTool().getWordKind();
        Variable __result = newVariable(__kind);
        append(new AMD64HotSpotLoadAddressOp(__result, __constant, __action));
        return __result;
    }

    private Value emitConstantRetrieval(ForeignCallDescriptor __foreignCall, Object[] __notes, Constant[] __constants, AllocatableValue[] __constantDescriptions, LIRFrameState __frameState)
    {
        ForeignCallLinkage __linkage = getForeignCalls().lookupForeignCall(__foreignCall);
        append(new AMD64HotSpotConstantRetrievalOp(__constants, __constantDescriptions, __frameState, __linkage, __notes));
        AllocatableValue __result = __linkage.getOutgoingCallingConvention().getReturn();
        return emitMove(__result);
    }

    private Value emitConstantRetrieval(ForeignCallDescriptor __foreignCall, HotSpotConstantLoadAction __action, Constant __constant, AllocatableValue[] __constantDescriptions, LIRFrameState __frameState)
    {
        Constant[] __constants = new Constant[] { __constant };
        Object[] __notes = new Object[] { __action };
        return emitConstantRetrieval(__foreignCall, __notes, __constants, __constantDescriptions, __frameState);
    }

    private Value emitConstantRetrieval(ForeignCallDescriptor __foreignCall, HotSpotConstantLoadAction __action, Constant __constant, Value __constantDescription, LIRFrameState __frameState)
    {
        AllocatableValue[] __constantDescriptions = new AllocatableValue[] { asAllocatable(__constantDescription) };
        return emitConstantRetrieval(__foreignCall, __action, __constant, __constantDescriptions, __frameState);
    }

    @Override
    public Value emitObjectConstantRetrieval(Constant __constant, Value __constantDescription, LIRFrameState __frameState)
    {
        return emitConstantRetrieval(HotSpotBackend.RESOLVE_STRING_BY_SYMBOL, HotSpotConstantLoadAction.RESOLVE, __constant, __constantDescription, __frameState);
    }

    @Override
    public Value emitMetaspaceConstantRetrieval(Constant __constant, Value __constantDescription, LIRFrameState __frameState)
    {
        return emitConstantRetrieval(HotSpotBackend.RESOLVE_KLASS_BY_SYMBOL, HotSpotConstantLoadAction.RESOLVE, __constant, __constantDescription, __frameState);
    }

    @Override
    public Value emitKlassInitializationAndRetrieval(Constant __constant, Value __constantDescription, LIRFrameState __frameState)
    {
        return emitConstantRetrieval(HotSpotBackend.INITIALIZE_KLASS_BY_SYMBOL, HotSpotConstantLoadAction.INITIALIZE, __constant, __constantDescription, __frameState);
    }

    @Override
    public Value emitResolveMethodAndLoadCounters(Constant __method, Value __klassHint, Value __methodDescription, LIRFrameState __frameState)
    {
        AllocatableValue[] __constantDescriptions = new AllocatableValue[] { asAllocatable(__klassHint), asAllocatable(__methodDescription) };
        return emitConstantRetrieval(HotSpotBackend.RESOLVE_METHOD_BY_SYMBOL_AND_LOAD_COUNTERS, HotSpotConstantLoadAction.LOAD_COUNTERS, __method, __constantDescriptions, __frameState);
    }

    @Override
    public Value emitResolveDynamicInvoke(Constant __appendix, LIRFrameState __frameState)
    {
        AllocatableValue[] __constantDescriptions = new AllocatableValue[0];
        return emitConstantRetrieval(HotSpotBackend.RESOLVE_DYNAMIC_INVOKE, HotSpotConstantLoadAction.INITIALIZE, __appendix, __constantDescriptions, __frameState);
    }

    @Override
    public Value emitLoadConfigValue(int __markId, LIRKind __kind)
    {
        Variable __result = newVariable(__kind);
        append(new AMD64HotSpotLoadConfigValueOp(__markId, __result));
        return __result;
    }

    @Override
    public Value emitRandomSeed()
    {
        AMD64ReadTimestampCounter __timestamp = new AMD64ReadTimestampCounter();
        append(__timestamp);
        return emitMove(__timestamp.getLowResult());
    }

    @Override
    public void emitTailcall(Value[] __args, Value __address)
    {
        append(new AMD64TailcallOp(__args, __address));
    }

    @Override
    public void emitCCall(long __address, CallingConvention __nativeCallingConvention, Value[] __args, int __numberOfFloatingPointArguments)
    {
        Value[] __argLocations = new Value[__args.length];
        getResult().getFrameMapBuilder().callsMethod(__nativeCallingConvention);
        // TODO in case a native function uses floating point varargs, the ABI requires that RAX contains the length of the varargs
        PrimitiveConstant __intConst = JavaConstant.forInt(__numberOfFloatingPointArguments);
        AllocatableValue __numberOfFloatingPointArgumentsRegister = AMD64.rax.asValue(LIRKind.value(AMD64Kind.DWORD));
        emitMoveConstant(__numberOfFloatingPointArgumentsRegister, __intConst);
        for (int __i = 0; __i < __args.length; __i++)
        {
            Value __arg = __args[__i];
            AllocatableValue __loc = __nativeCallingConvention.getArgument(__i);
            emitMove(__loc, __arg);
            __argLocations[__i] = __loc;
        }
        Value __ptr = emitLoadConstant(LIRKind.value(AMD64Kind.QWORD), JavaConstant.forLong(__address));
        append(new AMD64CCall(__nativeCallingConvention.getReturn(), __ptr, __numberOfFloatingPointArgumentsRegister, __argLocations));
    }

    @Override
    public void emitUnwind(Value __exception)
    {
        ForeignCallLinkage __linkage = getForeignCalls().lookupForeignCall(HotSpotBackend.UNWIND_EXCEPTION_TO_CALLER);
        CallingConvention __outgoingCc = __linkage.getOutgoingCallingConvention();
        RegisterValue __exceptionParameter = (RegisterValue) __outgoingCc.getArgument(0);
        emitMove(__exceptionParameter, __exception);
        append(new AMD64HotSpotUnwindOp(__exceptionParameter));
    }

    private void moveDeoptValuesToThread(Value __actionAndReason, Value __speculation)
    {
        moveValueToThread(__actionAndReason, HotSpotRuntime.pendingDeoptimizationOffset);
        moveValueToThread(__speculation, HotSpotRuntime.pendingFailedSpeculationOffset);
    }

    private void moveValueToThread(Value __v, int __offset)
    {
        LIRKind __wordKind = LIRKind.value(target().arch.getWordKind());
        RegisterValue __thread = getProviders().getRegisters().getThreadRegister().asValue(__wordKind);
        AMD64AddressValue __address = new AMD64AddressValue(__wordKind, __thread, __offset);
        this.___arithmeticLIRGen.emitStore(__v.getValueKind(), __address, __v, null);
    }

    @Override
    public void emitDeoptimize(Value __actionAndReason, Value __speculation, LIRFrameState __state)
    {
        moveDeoptValuesToThread(__actionAndReason, __speculation);
        append(new AMD64DeoptimizeOp(__state));
    }

    @Override
    public void emitDeoptimizeCaller(DeoptimizationAction __action, DeoptimizationReason __reason)
    {
        Value __actionAndReason = emitJavaConstant(getMetaAccess().encodeDeoptActionAndReason(__action, __reason, 0));
        Value __nullValue = emitConstant(LIRKind.reference(AMD64Kind.QWORD), JavaConstant.NULL_POINTER);
        moveDeoptValuesToThread(__actionAndReason, __nullValue);
        append(new AMD64HotSpotDeoptimizeCallerOp());
    }

    @Override
    public void beforeRegisterAllocation()
    {
        super.beforeRegisterAllocation();
        AllocatableValue __savedRbp = this.___saveRbp.finalize(false);

        for (AMD64HotSpotRestoreRbpOp __op : this.___epilogueOps)
        {
            __op.setSavedRbp(__savedRbp);
        }
    }

    @Override
    public Value emitCompress(Value __pointer, CompressEncoding __encoding, boolean __nonNull)
    {
        LIRKind __inputKind = __pointer.getValueKind(LIRKind.class);
        LIRKindTool __lirKindTool = getLIRKindTool();
        if (__inputKind.isReference(0))
        {
            // oop
            Variable __result = newVariable(__lirKindTool.getNarrowOopKind());
            append(new AMD64Move.CompressPointerOp(__result, asAllocatable(__pointer), getProviders().getRegisters().getHeapBaseRegister().asValue(), __encoding, __nonNull, getLIRKindTool()));
            return __result;
        }
        else
        {
            // metaspace pointer
            Variable __result = newVariable(__lirKindTool.getNarrowPointerKind());
            AllocatableValue __base = Value.ILLEGAL;
            if (__encoding.hasBase())
            {
                __base = emitLoadConstant(__lirKindTool.getWordKind(), JavaConstant.forLong(__encoding.getBase()));
            }
            append(new AMD64Move.CompressPointerOp(__result, asAllocatable(__pointer), __base, __encoding, __nonNull, getLIRKindTool()));
            return __result;
        }
    }

    @Override
    public Value emitUncompress(Value __pointer, CompressEncoding __encoding, boolean __nonNull)
    {
        LIRKind __inputKind = __pointer.getValueKind(LIRKind.class);
        LIRKindTool __lirKindTool = getLIRKindTool();
        if (__inputKind.isReference(0))
        {
            // oop
            Variable __result = newVariable(__lirKindTool.getObjectKind());
            append(new AMD64Move.UncompressPointerOp(__result, asAllocatable(__pointer), getProviders().getRegisters().getHeapBaseRegister().asValue(), __encoding, __nonNull, __lirKindTool));
            return __result;
        }
        else
        {
            // metaspace pointer
            LIRKind __uncompressedKind = __lirKindTool.getWordKind();
            Variable __result = newVariable(__uncompressedKind);
            AllocatableValue __base = Value.ILLEGAL;
            if (__encoding.hasBase())
            {
                __base = emitLoadConstant(__uncompressedKind, JavaConstant.forLong(__encoding.getBase()));
            }
            append(new AMD64Move.UncompressPointerOp(__result, asAllocatable(__pointer), __base, __encoding, __nonNull, __lirKindTool));
            return __result;
        }
    }

    @Override
    public void emitNullCheck(Value __address, LIRFrameState __state)
    {
        if (__address.getValueKind().getPlatformKind() == getLIRKindTool().getNarrowOopKind().getPlatformKind())
        {
            CompressEncoding __encoding = HotSpotRuntime.oopEncoding;
            Value __uncompressed;
            if (__encoding.getShift() <= 3)
            {
                LIRKind __wordKind = LIRKind.unknownReference(target().arch.getWordKind());
                __uncompressed = new AMD64AddressValue(__wordKind, getProviders().getRegisters().getHeapBaseRegister().asValue(__wordKind), asAllocatable(__address), AMD64Address.Scale.fromInt(1 << __encoding.getShift()), 0);
            }
            else
            {
                __uncompressed = emitUncompress(__address, __encoding, false);
            }
            append(new AMD64Move.NullCheckOp(asAddressValue(__uncompressed), __state));
            return;
        }
        super.emitNullCheck(__address, __state);
    }

    @Override
    public void emitPrefetchAllocate(Value __address)
    {
        append(new AMD64PrefetchOp(asAddressValue(__address), HotSpotRuntime.allocatePrefetchInstr));
    }

    @Override
    protected AMD64ControlFlow.StrategySwitchOp createStrategySwitchOp(SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Variable __key, AllocatableValue __temp)
    {
        return new AMD64HotSpotStrategySwitchOp(__strategy, __keyTargets, __defaultTarget, __key, __temp);
    }
}
