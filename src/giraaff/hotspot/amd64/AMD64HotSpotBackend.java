package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.asm.Assembler;
import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.code.CompilationResult;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.target.Backend;
import giraaff.hotspot.HotSpotDataBuilder;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.HotSpotHostBackend;
import giraaff.hotspot.HotSpotLIRGenerationResult;
import giraaff.hotspot.HotSpotRuntime;
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

///
// HotSpot AMD64 specific backend.
///
// @class AMD64HotSpotBackend
public final class AMD64HotSpotBackend extends HotSpotHostBackend
{
    // @cons AMD64HotSpotBackend
    public AMD64HotSpotBackend(HotSpotGraalRuntime __runtime, HotSpotProviders __providers)
    {
        super(__runtime, __providers);
    }

    @Override
    public FrameMapBuilder newFrameMapBuilder(RegisterConfig __registerConfig)
    {
        RegisterConfig __registerConfigNonNull = __registerConfig == null ? getCodeCache().getRegisterConfig() : __registerConfig;
        return new AMD64FrameMapBuilder(newFrameMap(__registerConfigNonNull), getCodeCache(), __registerConfigNonNull);
    }

    @Override
    public FrameMap newFrameMap(RegisterConfig __registerConfig)
    {
        return new AMD64FrameMap(getCodeCache(), __registerConfig);
    }

    @Override
    public LIRGeneratorTool newLIRGenerator(LIRGenerationResult __lirGenRes)
    {
        return new AMD64HotSpotLIRGenerator(getProviders(), __lirGenRes);
    }

    @Override
    public LIRGenerationResult newLIRGenerationResult(LIR __lir, FrameMapBuilder __frameMapBuilder, StructuredGraph __graph, Object __stub)
    {
        return new HotSpotLIRGenerationResult(__lir, __frameMapBuilder, makeCallingConvention(__graph, (Stub) __stub), __stub);
    }

    @Override
    public NodeLIRBuilderTool newNodeLIRBuilder(StructuredGraph __graph, LIRGeneratorTool __lirGen)
    {
        return new AMD64HotSpotNodeLIRBuilder(__graph, __lirGen);
    }

    @Override
    protected void bangStackWithOffset(CompilationResultBuilder __crb, int __bangOffset)
    {
        AMD64MacroAssembler __asm = (AMD64MacroAssembler) __crb.___asm;
        int __pos = __asm.position();
        __asm.movl(new AMD64Address(AMD64.rsp, -__bangOffset), AMD64.rax);
    }

    ///
    // The size of the instruction used to patch the verified entry point of an nmethod when the
    // nmethod is made non-entrant or a zombie (e.g. during deopt or class unloading). The first
    // instruction emitted at an nmethod's verified entry point must be at least this length to
    // ensure mt-safe patching.
    ///
    // @def
    public static final int PATCHED_VERIFIED_ENTRY_POINT_INSTRUCTION_SIZE = 5;

    ///
    // Emits code at the verified entry point and return point(s) of a method.
    ///
    // @class AMD64HotSpotBackend.HotSpotFrameContext
    // @closure
    final class HotSpotFrameContext implements FrameContext
    {
        // @field
        final boolean ___isStub;
        // @field
        final boolean ___omitFrame;

        // @cons AMD64HotSpotBackend.HotSpotFrameContext
        HotSpotFrameContext(boolean __isStub, boolean __omitFrame)
        {
            super();
            this.___isStub = __isStub;
            this.___omitFrame = __omitFrame;
        }

        @Override
        public boolean hasFrame()
        {
            return !this.___omitFrame;
        }

        @Override
        public void enter(CompilationResultBuilder __crb)
        {
            FrameMap __frameMap = __crb.___frameMap;
            int __frameSize = __frameMap.frameSize();
            AMD64MacroAssembler __asm = (AMD64MacroAssembler) __crb.___asm;
            if (this.___omitFrame)
            {
                if (!this.___isStub)
                {
                    __asm.nop(PATCHED_VERIFIED_ENTRY_POINT_INSTRUCTION_SIZE);
                }
            }
            else
            {
                int __verifiedEntryPosition = __asm.position();
                if (!this.___isStub)
                {
                    AMD64HotSpotBackend.this.emitStackOverflowCheck(__crb);
                }
                if (!this.___isStub && __asm.position() == __verifiedEntryPosition)
                {
                    __asm.subqWide(AMD64.rsp, __frameSize);
                }
                else
                {
                    __asm.decrementq(AMD64.rsp, __frameSize);
                }
                if (GraalOptions.zapStackOnMethodEntry)
                {
                    final int __intSize = 4;
                    for (int __i = 0; __i < __frameSize / __intSize; ++__i)
                    {
                        __asm.movl(new AMD64Address(AMD64.rsp, __i * __intSize), 0xC1C1C1C1);
                    }
                }
            }
        }

        @Override
        public void leave(CompilationResultBuilder __crb)
        {
            if (!this.___omitFrame)
            {
                AMD64MacroAssembler __asm = (AMD64MacroAssembler) __crb.___asm;

                int __frameSize = __crb.___frameMap.frameSize();
                __asm.incrementq(AMD64.rsp, __frameSize);
            }
        }
    }

    @Override
    protected Assembler createAssembler(FrameMap __frameMap)
    {
        return new AMD64MacroAssembler(getTarget());
    }

    @Override
    public CompilationResultBuilder newCompilationResultBuilder(LIRGenerationResult __lirGenRen, FrameMap __frameMap, CompilationResult __compilationResult, CompilationResultBuilderFactory __factory)
    {
        // Omit the frame if the method:
        // - has no spill slots or other slots allocated during register allocation
        // - has no callee-saved registers
        // - has no incoming arguments passed on the stack
        // - has no deoptimization points
        // - makes no foreign calls (which require an aligned stack)
        HotSpotLIRGenerationResult __gen = (HotSpotLIRGenerationResult) __lirGenRen;
        LIR __lir = __gen.getLIR();
        boolean __omitFrame = GraalOptions.canOmitFrame && !__frameMap.frameNeedsAllocating() && !__lir.hasArgInCallerFrame() && !__gen.hasForeignCall();

        Stub __stub = __gen.getStub();
        Assembler __masm = createAssembler(__frameMap);
        AMD64HotSpotBackend.HotSpotFrameContext __frameContext = new AMD64HotSpotBackend.HotSpotFrameContext(__stub != null, __omitFrame);
        DataBuilder __dataBuilder = new HotSpotDataBuilder(getCodeCache().getTarget());
        CompilationResultBuilder __crb = __factory.createBuilder(getCodeCache(), getForeignCalls(), __frameMap, __masm, __dataBuilder, __frameContext, __compilationResult);
        __crb.setTotalFrameSize(__frameMap.totalFrameSize());

        if (__stub != null)
        {
            EconomicSet<Register> __destroyedCallerRegisters = gatherDestroyedCallerRegisters(__lir);
            updateStub(__stub, __destroyedCallerRegisters, __gen.getCalleeSaveInfo(), __frameMap);
        }

        return __crb;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, LIR __lir, ResolvedJavaMethod __installedCodeOwner)
    {
        AMD64MacroAssembler __asm = (AMD64MacroAssembler) __crb.___asm;
        FrameMap __frameMap = __crb.___frameMap;
        RegisterConfig __regConfig = __frameMap.getRegisterConfig();
        Label __verifiedEntry = new Label();

        // emit the prefix
        emitCodePrefix(__installedCodeOwner, __crb, __asm, __regConfig, __verifiedEntry);

        // emit code for the LIR
        emitCodeBody(__installedCodeOwner, __crb, __lir);

        // emit the suffix
        emitCodeSuffix(__installedCodeOwner, __crb, __asm, __frameMap);
    }

    ///
    // Emits the code prior to the verified entry point.
    //
    // @param installedCodeOwner see {@link Backend#emitCode}
    ///
    public void emitCodePrefix(ResolvedJavaMethod __installedCodeOwner, CompilationResultBuilder __crb, AMD64MacroAssembler __asm, RegisterConfig __regConfig, Label __verifiedEntry)
    {
        HotSpotProviders __providers = getProviders();
        if (__installedCodeOwner != null && !__installedCodeOwner.isStatic())
        {
            __crb.recordMark(HotSpotRuntime.unverifiedEntryMark);
            CallingConvention __cc = __regConfig.getCallingConvention(HotSpotCallingConventionType.JavaCallee, null, new JavaType[] { __providers.getMetaAccess().lookupJavaType(Object.class) }, this);
            Register __inlineCacheKlass = AMD64.rax; // see definition of IC_Klass in c1_LIRAssembler_x86.cpp
            Register __receiver = ValueUtil.asRegister(__cc.getArgument(0));
            AMD64Address __src = new AMD64Address(__receiver, HotSpotRuntime.hubOffset);

            if (HotSpotRuntime.useCompressedClassPointers)
            {
                Register __register = AMD64.r10;
                AMD64HotSpotMove.decodeKlassPointer(__crb, __asm, __register, __providers.getRegisters().getHeapBaseRegister(), __src);
                if (HotSpotRuntime.narrowKlassBase != 0)
                {
                    // the heap base register was destroyed above, so restore it
                    __asm.movq(__providers.getRegisters().getHeapBaseRegister(), HotSpotRuntime.narrowOopBase);
                }
                __asm.cmpq(__inlineCacheKlass, __register);
            }
            else
            {
                __asm.cmpq(__inlineCacheKlass, __src);
            }
            AMD64Call.directConditionalJmp(__crb, __asm, getForeignCalls().lookupForeignCall(IC_MISS_HANDLER), AMD64Assembler.ConditionFlag.NotEqual);
        }

        __asm.align(HotSpotRuntime.codeEntryAlignment);
        __crb.recordMark(HotSpotRuntime.osrEntryMark);
        __asm.bind(__verifiedEntry);
        __crb.recordMark(HotSpotRuntime.verifiedEntryMark);
    }

    ///
    // Emits the code which starts at the verified entry point.
    //
    // @param installedCodeOwner see {@link Backend#emitCode}
    ///
    public void emitCodeBody(ResolvedJavaMethod __installedCodeOwner, CompilationResultBuilder __crb, LIR __lir)
    {
        __crb.emit(__lir);
    }

    ///
    // @param installedCodeOwner see {@link Backend#emitCode}
    ///
    public void emitCodeSuffix(ResolvedJavaMethod __installedCodeOwner, CompilationResultBuilder __crb, AMD64MacroAssembler __asm, FrameMap __frameMap)
    {
        HotSpotProviders __providers = getProviders();
        AMD64HotSpotBackend.HotSpotFrameContext __frameContext = (AMD64HotSpotBackend.HotSpotFrameContext) __crb.___frameContext;
        if (!__frameContext.___isStub)
        {
            HotSpotForeignCallsProvider __foreignCalls = __providers.getForeignCalls();
            __crb.recordMark(HotSpotRuntime.exceptionHandlerEntryMark);
            AMD64Call.directCall(__crb, __asm, __foreignCalls.lookupForeignCall(EXCEPTION_HANDLER), null, false, null);
            __crb.recordMark(HotSpotRuntime.deoptHandlerEntryMark);
            AMD64Call.directCall(__crb, __asm, __foreignCalls.lookupForeignCall(DEOPTIMIZATION_HANDLER), null, false, null);
        }
        else
        {
            // No need to emit the stubs for entries back into the method,
            // since it has no calls that can cause such "return" entries.
        }
    }

    @Override
    public RegisterAllocationConfig newRegisterAllocationConfig(RegisterConfig __registerConfig)
    {
        RegisterConfig __registerConfigNonNull = (__registerConfig != null) ? __registerConfig : getCodeCache().getRegisterConfig();
        return new AMD64HotSpotRegisterAllocationConfig(__registerConfigNonNull);
    }

    @Override
    public EconomicSet<Register> translateToCallerRegisters(EconomicSet<Register> __calleeRegisters)
    {
        return __calleeRegisters;
    }
}
