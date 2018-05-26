package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.asm.Assembler;
import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.ConditionFlag;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.code.CompilationResult;
import giraaff.core.common.CompilationIdentifier;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.LIRKind;
import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.target.Backend;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotDataBuilder;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.HotSpotHostBackend;
import giraaff.hotspot.HotSpotLIRGenerationResult;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.hotspot.meta.HotSpotForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.stubs.Stub;
import giraaff.lir.LIR;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.amd64.AMD64FrameMap;
import giraaff.lir.amd64.AMD64FrameMapBuilder;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.asm.CompilationResultBuilderFactory;
import giraaff.lir.asm.DataBuilder;
import giraaff.lir.asm.FrameContext;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.options.OptionValues;

/**
 * HotSpot AMD64 specific backend.
 */
public class AMD64HotSpotBackend extends HotSpotHostBackend
{
    public AMD64HotSpotBackend(GraalHotSpotVMConfig config, HotSpotGraalRuntimeProvider runtime, HotSpotProviders providers)
    {
        super(config, runtime, providers);
    }

    @Override
    public FrameMapBuilder newFrameMapBuilder(RegisterConfig registerConfig)
    {
        RegisterConfig registerConfigNonNull = registerConfig == null ? getCodeCache().getRegisterConfig() : registerConfig;
        return new AMD64FrameMapBuilder(newFrameMap(registerConfigNonNull), getCodeCache(), registerConfigNonNull);
    }

    @Override
    public FrameMap newFrameMap(RegisterConfig registerConfig)
    {
        return new AMD64FrameMap(getCodeCache(), registerConfig);
    }

    @Override
    public LIRGeneratorTool newLIRGenerator(LIRGenerationResult lirGenRes)
    {
        return new AMD64HotSpotLIRGenerator(getProviders(), config, lirGenRes);
    }

    @Override
    public LIRGenerationResult newLIRGenerationResult(CompilationIdentifier compilationId, LIR lir, FrameMapBuilder frameMapBuilder, StructuredGraph graph, Object stub)
    {
        return new HotSpotLIRGenerationResult(compilationId, lir, frameMapBuilder, makeCallingConvention(graph, (Stub) stub), stub);
    }

    @Override
    public NodeLIRBuilderTool newNodeLIRBuilder(StructuredGraph graph, LIRGeneratorTool lirGen)
    {
        return new AMD64HotSpotNodeLIRBuilder(graph, lirGen);
    }

    @Override
    protected void bangStackWithOffset(CompilationResultBuilder crb, int bangOffset)
    {
        AMD64MacroAssembler asm = (AMD64MacroAssembler) crb.asm;
        int pos = asm.position();
        asm.movl(new AMD64Address(AMD64.rsp, -bangOffset), AMD64.rax);
    }

    /**
     * The size of the instruction used to patch the verified entry point of an nmethod when the
     * nmethod is made non-entrant or a zombie (e.g. during deopt or class unloading). The first
     * instruction emitted at an nmethod's verified entry point must be at least this length to
     * ensure mt-safe patching.
     */
    public static final int PATCHED_VERIFIED_ENTRY_POINT_INSTRUCTION_SIZE = 5;

    /**
     * Emits code at the verified entry point and return point(s) of a method.
     */
    class HotSpotFrameContext implements FrameContext
    {
        final boolean isStub;
        final boolean omitFrame;

        HotSpotFrameContext(boolean isStub, boolean omitFrame)
        {
            this.isStub = isStub;
            this.omitFrame = omitFrame;
        }

        @Override
        public boolean hasFrame()
        {
            return !omitFrame;
        }

        @Override
        public void enter(CompilationResultBuilder crb)
        {
            FrameMap frameMap = crb.frameMap;
            int frameSize = frameMap.frameSize();
            AMD64MacroAssembler asm = (AMD64MacroAssembler) crb.asm;
            if (omitFrame)
            {
                if (!isStub)
                {
                    asm.nop(PATCHED_VERIFIED_ENTRY_POINT_INSTRUCTION_SIZE);
                }
            }
            else
            {
                int verifiedEntryPointOffset = asm.position();
                if (!isStub)
                {
                    emitStackOverflowCheck(crb);
                    // assert asm.position() - verifiedEntryPointOffset >=
                    // PATCHED_VERIFIED_ENTRY_POINT_INSTRUCTION_SIZE;
                }
                if (!isStub && asm.position() == verifiedEntryPointOffset)
                {
                    asm.subqWide(AMD64.rsp, frameSize);
                }
                else
                {
                    asm.decrementq(AMD64.rsp, frameSize);
                }
                if (GraalOptions.ZapStackOnMethodEntry.getValue(crb.getOptions()))
                {
                    final int intSize = 4;
                    for (int i = 0; i < frameSize / intSize; ++i)
                    {
                        asm.movl(new AMD64Address(AMD64.rsp, i * intSize), 0xC1C1C1C1);
                    }
                }
            }
        }

        @Override
        public void leave(CompilationResultBuilder crb)
        {
            if (!omitFrame)
            {
                AMD64MacroAssembler asm = (AMD64MacroAssembler) crb.asm;

                int frameSize = crb.frameMap.frameSize();
                asm.incrementq(AMD64.rsp, frameSize);
            }
        }
    }

    @Override
    protected Assembler createAssembler(FrameMap frameMap)
    {
        return new AMD64MacroAssembler(getTarget());
    }

    @Override
    public CompilationResultBuilder newCompilationResultBuilder(LIRGenerationResult lirGenRen, FrameMap frameMap, CompilationResult compilationResult, CompilationResultBuilderFactory factory)
    {
        // Omit the frame if the method:
        // - has no spill slots or other slots allocated during register allocation
        // - has no callee-saved registers
        // - has no incoming arguments passed on the stack
        // - has no deoptimization points
        // - makes no foreign calls (which require an aligned stack)
        HotSpotLIRGenerationResult gen = (HotSpotLIRGenerationResult) lirGenRen;
        LIR lir = gen.getLIR();
        OptionValues options = lir.getOptions();
        boolean omitFrame = GraalOptions.CanOmitFrame.getValue(options) && !frameMap.frameNeedsAllocating() && !lir.hasArgInCallerFrame() && !gen.hasForeignCall();

        Stub stub = gen.getStub();
        Assembler masm = createAssembler(frameMap);
        HotSpotFrameContext frameContext = new HotSpotFrameContext(stub != null, omitFrame);
        DataBuilder dataBuilder = new HotSpotDataBuilder(getCodeCache().getTarget());
        CompilationResultBuilder crb = factory.createBuilder(getCodeCache(), getForeignCalls(), frameMap, masm, dataBuilder, frameContext, options, compilationResult);
        crb.setTotalFrameSize(frameMap.totalFrameSize());

        if (stub != null)
        {
            EconomicSet<Register> destroyedCallerRegisters = gatherDestroyedCallerRegisters(lir);
            updateStub(stub, destroyedCallerRegisters, gen.getCalleeSaveInfo(), frameMap);
        }

        return crb;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, LIR lir, ResolvedJavaMethod installedCodeOwner)
    {
        AMD64MacroAssembler asm = (AMD64MacroAssembler) crb.asm;
        FrameMap frameMap = crb.frameMap;
        RegisterConfig regConfig = frameMap.getRegisterConfig();
        Label verifiedEntry = new Label();

        // Emit the prefix
        emitCodePrefix(installedCodeOwner, crb, asm, regConfig, verifiedEntry);

        // Emit code for the LIR
        emitCodeBody(installedCodeOwner, crb, lir);

        // Emit the suffix
        emitCodeSuffix(installedCodeOwner, crb, asm, frameMap);
    }

    /**
     * Emits the code prior to the verified entry point.
     *
     * @param installedCodeOwner see {@link Backend#emitCode}
     */
    public void emitCodePrefix(ResolvedJavaMethod installedCodeOwner, CompilationResultBuilder crb, AMD64MacroAssembler asm, RegisterConfig regConfig, Label verifiedEntry)
    {
        HotSpotProviders providers = getProviders();
        if (installedCodeOwner != null && !installedCodeOwner.isStatic())
        {
            crb.recordMark(config.MARKID_UNVERIFIED_ENTRY);
            CallingConvention cc = regConfig.getCallingConvention(HotSpotCallingConventionType.JavaCallee, null, new JavaType[] { providers.getMetaAccess().lookupJavaType(Object.class) }, this);
            Register inlineCacheKlass = AMD64.rax; // see definition of IC_Klass in c1_LIRAssembler_x86.cpp
            Register receiver = ValueUtil.asRegister(cc.getArgument(0));
            AMD64Address src = new AMD64Address(receiver, config.hubOffset);

            if (config.useCompressedClassPointers)
            {
                Register register = AMD64.r10;
                AMD64HotSpotMove.decodeKlassPointer(crb, asm, register, providers.getRegisters().getHeapBaseRegister(), src, config);
                if (config.narrowKlassBase != 0)
                {
                    // The heap base register was destroyed above, so restore it
                    asm.movq(providers.getRegisters().getHeapBaseRegister(), config.narrowOopBase);
                }
                asm.cmpq(inlineCacheKlass, register);
            }
            else
            {
                asm.cmpq(inlineCacheKlass, src);
            }
            AMD64Call.directConditionalJmp(crb, asm, getForeignCalls().lookupForeignCall(IC_MISS_HANDLER), ConditionFlag.NotEqual);
        }

        asm.align(config.codeEntryAlignment);
        crb.recordMark(config.MARKID_OSR_ENTRY);
        asm.bind(verifiedEntry);
        crb.recordMark(config.MARKID_VERIFIED_ENTRY);
    }

    /**
     * Emits the code which starts at the verified entry point.
     *
     * @param installedCodeOwner see {@link Backend#emitCode}
     */
    public void emitCodeBody(ResolvedJavaMethod installedCodeOwner, CompilationResultBuilder crb, LIR lir)
    {
        crb.emit(lir);
    }

    /**
     * @param installedCodeOwner see {@link Backend#emitCode}
     */
    public void emitCodeSuffix(ResolvedJavaMethod installedCodeOwner, CompilationResultBuilder crb, AMD64MacroAssembler asm, FrameMap frameMap)
    {
        HotSpotProviders providers = getProviders();
        HotSpotFrameContext frameContext = (HotSpotFrameContext) crb.frameContext;
        if (!frameContext.isStub)
        {
            HotSpotForeignCallsProvider foreignCalls = providers.getForeignCalls();
            crb.recordMark(config.MARKID_EXCEPTION_HANDLER_ENTRY);
            AMD64Call.directCall(crb, asm, foreignCalls.lookupForeignCall(EXCEPTION_HANDLER), null, false, null);
            crb.recordMark(config.MARKID_DEOPT_HANDLER_ENTRY);
            AMD64Call.directCall(crb, asm, foreignCalls.lookupForeignCall(DEOPTIMIZATION_HANDLER), null, false, null);
        }
        else
        {
            // No need to emit the stubs for entries back into the method since
            // it has no calls that can cause such "return" entries
        }
    }

    @Override
    public RegisterAllocationConfig newRegisterAllocationConfig(RegisterConfig registerConfig)
    {
        RegisterConfig registerConfigNonNull = (registerConfig != null) ? registerConfig : getCodeCache().getRegisterConfig();
        return new AMD64HotSpotRegisterAllocationConfig(registerConfigNonNull);
    }

    @Override
    public EconomicSet<Register> translateToCallerRegisters(EconomicSet<Register> calleeRegisters)
    {
        return calleeRegisters;
    }
}
