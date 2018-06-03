package giraaff.hotspot;

import java.util.EnumSet;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompiledCode;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterSaveLayout;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import org.graalvm.word.Pointer;

import giraaff.code.CompilationResult;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.target.Backend;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.aot.ResolveConstantStubCall;
import giraaff.hotspot.replacements.AESCryptSubstitutions;
import giraaff.hotspot.replacements.BigIntegerSubstitutions;
import giraaff.hotspot.replacements.CipherBlockChainingSubstitutions;
import giraaff.hotspot.replacements.SHA2Substitutions;
import giraaff.hotspot.replacements.SHA5Substitutions;
import giraaff.hotspot.stubs.ExceptionHandlerStub;
import giraaff.hotspot.stubs.Stub;
import giraaff.hotspot.stubs.UnwindExceptionToCallerStub;
import giraaff.hotspot.word.KlassPointer;
import giraaff.hotspot.word.MethodCountersPointer;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.StandardOp.SaveRegistersOp;
import giraaff.lir.ValueConsumer;
import giraaff.lir.framemap.FrameMap;
import giraaff.nodes.UnwindNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.phases.tiers.SuitesProvider;
import giraaff.word.Word;

/**
 * HotSpot specific backend.
 */
// @class HotSpotBackend
public abstract class HotSpotBackend extends Backend
{
    /**
     * Descriptor for {@link ExceptionHandlerStub}. This stub is called by the
     * {@linkplain HotSpotRuntime#exceptionHandlerEntryMark exception handler} in a
     * compiled method.
     */
    // @def
    public static final ForeignCallDescriptor EXCEPTION_HANDLER = new ForeignCallDescriptor("exceptionHandler", void.class, Object.class, Word.class);

    /**
     * Descriptor for SharedRuntime::get_ic_miss_stub().
     */
    // @def
    public static final ForeignCallDescriptor IC_MISS_HANDLER = new ForeignCallDescriptor("icMissHandler", void.class);

    /**
     * Descriptor for {@link UnwindExceptionToCallerStub}. This stub is called by code generated
     * from {@link UnwindNode}.
     */
    // @def
    public static final ForeignCallDescriptor UNWIND_EXCEPTION_TO_CALLER = new ForeignCallDescriptor("unwindExceptionToCaller", void.class, Object.class, Word.class);

    /**
     * Descriptor for the arguments when unwinding to an exception handler in a caller.
     */
    // @def
    public static final ForeignCallDescriptor EXCEPTION_HANDLER_IN_CALLER = new ForeignCallDescriptor("exceptionHandlerInCaller", void.class, Object.class, Word.class);

    /**
     * @see AESCryptSubstitutions#encryptBlockStub(ForeignCallDescriptor, Word, Word, Pointer)
     */
    // @def
    public static final ForeignCallDescriptor ENCRYPT_BLOCK = new ForeignCallDescriptor("encrypt_block", void.class, Word.class, Word.class, Pointer.class);

    /**
     * @see AESCryptSubstitutions#decryptBlockStub(ForeignCallDescriptor, Word, Word, Pointer)
     */
    // @def
    public static final ForeignCallDescriptor DECRYPT_BLOCK = new ForeignCallDescriptor("decrypt_block", void.class, Word.class, Word.class, Pointer.class);

    /**
     * @see AESCryptSubstitutions#decryptBlockStub(ForeignCallDescriptor, Word, Word, Pointer)
     */
    // @def
    public static final ForeignCallDescriptor DECRYPT_BLOCK_WITH_ORIGINAL_KEY = new ForeignCallDescriptor("decrypt_block_with_original_key", void.class, Word.class, Word.class, Pointer.class, Pointer.class);

    /**
     * @see CipherBlockChainingSubstitutions#crypt
     */
    // @def
    public static final ForeignCallDescriptor ENCRYPT = new ForeignCallDescriptor("encrypt", void.class, Word.class, Word.class, Pointer.class, Pointer.class, int.class);

    /**
     * @see CipherBlockChainingSubstitutions#crypt
     */
    // @def
    public static final ForeignCallDescriptor DECRYPT = new ForeignCallDescriptor("decrypt", void.class, Word.class, Word.class, Pointer.class, Pointer.class, int.class);

    /**
     * @see CipherBlockChainingSubstitutions#crypt
     */
    // @def
    public static final ForeignCallDescriptor DECRYPT_WITH_ORIGINAL_KEY = new ForeignCallDescriptor("decrypt_with_original_key", void.class, Word.class, Word.class, Pointer.class, Pointer.class, int.class, Pointer.class);

    /**
     * @see BigIntegerSubstitutions#multiplyToLen
     */
    // @def
    public static final ForeignCallDescriptor MULTIPLY_TO_LEN = new ForeignCallDescriptor("multiplyToLen", void.class, Word.class, int.class, Word.class, int.class, Word.class, int.class);

    public static void multiplyToLenStub(Word __xAddr, int __xlen, Word __yAddr, int __ylen, Word __zAddr, int __zLen)
    {
        multiplyToLenStub(HotSpotBackend.MULTIPLY_TO_LEN, __xAddr, __xlen, __yAddr, __ylen, __zAddr, __zLen);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void multiplyToLenStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word xIn, int xLen, Word yIn, int yLen, Word zIn, int zLen);

    /**
     * @see BigIntegerSubstitutions#mulAdd
     */
    // @def
    public static final ForeignCallDescriptor MUL_ADD = new ForeignCallDescriptor("mulAdd", int.class, Word.class, Word.class, int.class, int.class, int.class);

    public static int mulAddStub(Word __inAddr, Word __outAddr, int __newOffset, int __len, int __k)
    {
        return mulAddStub(HotSpotBackend.MUL_ADD, __inAddr, __outAddr, __newOffset, __len, __k);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native int mulAddStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word inAddr, Word outAddr, int newOffset, int len, int k);

    /**
     * @see BigIntegerSubstitutions#implMontgomeryMultiply
     */
    // @def
    public static final ForeignCallDescriptor MONTGOMERY_MULTIPLY = new ForeignCallDescriptor("implMontgomeryMultiply", void.class, Word.class, Word.class, Word.class, int.class, long.class, Word.class);

    public static void implMontgomeryMultiply(Word __aAddr, Word __bAddr, Word __nAddr, int __len, long __inv, Word __productAddr)
    {
        implMontgomeryMultiply(HotSpotBackend.MONTGOMERY_MULTIPLY, __aAddr, __bAddr, __nAddr, __len, __inv, __productAddr);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void implMontgomeryMultiply(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word aAddr, Word bAddr, Word nAddr, int len, long inv, Word productAddr);

    /**
     * @see BigIntegerSubstitutions#implMontgomerySquare
     */
    // @def
    public static final ForeignCallDescriptor MONTGOMERY_SQUARE = new ForeignCallDescriptor("implMontgomerySquare", void.class, Word.class, Word.class, int.class, long.class, Word.class);

    public static void implMontgomerySquare(Word __aAddr, Word __nAddr, int __len, long __inv, Word __productAddr)
    {
        implMontgomerySquare(HotSpotBackend.MONTGOMERY_SQUARE, __aAddr, __nAddr, __len, __inv, __productAddr);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void implMontgomerySquare(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word aAddr, Word nAddr, int len, long inv, Word productAddr);

    /**
     * @see BigIntegerSubstitutions#implSquareToLen
     */
    // @def
    public static final ForeignCallDescriptor SQUARE_TO_LEN = new ForeignCallDescriptor("implSquareToLen", void.class, Word.class, int.class, Word.class, int.class);

    public static void implSquareToLen(Word __xAddr, int __len, Word __zAddr, int __zLen)
    {
        implSquareToLen(SQUARE_TO_LEN, __xAddr, __len, __zAddr, __zLen);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void implSquareToLen(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word xAddr, int len, Word zAddr, int zLen);

    /**
     * @see SHA2Substitutions#implCompress0
     */
    // @def
    public static final ForeignCallDescriptor SHA2_IMPL_COMPRESS = new ForeignCallDescriptor("sha2ImplCompress", void.class, Word.class, Object.class);

    public static void sha2ImplCompressStub(Word __bufAddr, Object __state)
    {
        sha2ImplCompressStub(HotSpotBackend.SHA2_IMPL_COMPRESS, __bufAddr, __state);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void sha2ImplCompressStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word bufAddr, Object state);

    /**
     * @see SHA5Substitutions#implCompress0
     */
    // @def
    public static final ForeignCallDescriptor SHA5_IMPL_COMPRESS = new ForeignCallDescriptor("sha5ImplCompress", void.class, Word.class, Object.class);

    public static void sha5ImplCompressStub(Word __bufAddr, Object __state)
    {
        sha5ImplCompressStub(HotSpotBackend.SHA5_IMPL_COMPRESS, __bufAddr, __state);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void sha5ImplCompressStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word bufAddr, Object state);

    /**
     * @see giraaff.hotspot.meta.HotSpotUnsafeSubstitutions#copyMemory
     */
    // @def
    public static final ForeignCallDescriptor UNSAFE_ARRAYCOPY = new ForeignCallDescriptor("unsafe_arraycopy", void.class, Word.class, Word.class, Word.class);

    public static void unsafeArraycopy(Word __srcAddr, Word __dstAddr, Word __size)
    {
        unsafeArraycopyStub(HotSpotBackend.UNSAFE_ARRAYCOPY, __srcAddr, __dstAddr, __size);
    }

    // @def
    public static final ForeignCallDescriptor GENERIC_ARRAYCOPY = new ForeignCallDescriptor("generic_arraycopy", int.class, Word.class, int.class, Word.class, int.class, int.class);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void unsafeArraycopyStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word srcAddr, Word dstAddr, Word size);

    /**
     * New multi array stub call.
     */
    // @def
    public static final ForeignCallDescriptor NEW_MULTI_ARRAY = new ForeignCallDescriptor("new_multi_array", Object.class, KlassPointer.class, int.class, Word.class);

    /**
     * New array stub.
     */
    // @def
    public static final ForeignCallDescriptor NEW_ARRAY = new ForeignCallDescriptor("new_array", Object.class, KlassPointer.class, int.class, boolean.class);

    /**
     * New instance stub.
     */
    // @def
    public static final ForeignCallDescriptor NEW_INSTANCE = new ForeignCallDescriptor("new_instance", Object.class, KlassPointer.class);

    /**
     * @see ResolveConstantStubCall
     */
    // @def
    public static final ForeignCallDescriptor RESOLVE_STRING_BY_SYMBOL = new ForeignCallDescriptor("resolve_string_by_symbol", Object.class, Word.class, Word.class);

    /**
     * @see ResolveConstantStubCall
     */
    // @def
    public static final ForeignCallDescriptor RESOLVE_DYNAMIC_INVOKE = new ForeignCallDescriptor("resolve_dynamic_invoke", Object.class, Word.class);

    /**
     * @see ResolveConstantStubCall
     */
    // @def
    public static final ForeignCallDescriptor RESOLVE_KLASS_BY_SYMBOL = new ForeignCallDescriptor("resolve_klass_by_symbol", Word.class, Word.class, Word.class);

    /**
     * @see ResolveConstantStubCall
     */
    // @def
    public static final ForeignCallDescriptor INITIALIZE_KLASS_BY_SYMBOL = new ForeignCallDescriptor("initialize_klass_by_symbol", Word.class, Word.class, Word.class);

    /**
     * @see ResolveConstantStubCall
     */
    // @def
    public static final ForeignCallDescriptor RESOLVE_METHOD_BY_SYMBOL_AND_LOAD_COUNTERS = new ForeignCallDescriptor("resolve_method_by_symbol_and_load_counters", Word.class, Word.class, Word.class, Word.class);

    /**
     * Tiered support.
     */
    // @def
    public static final ForeignCallDescriptor INVOCATION_EVENT = new ForeignCallDescriptor("invocation_event", void.class, MethodCountersPointer.class);
    // @def
    public static final ForeignCallDescriptor BACKEDGE_EVENT = new ForeignCallDescriptor("backedge_event", void.class, MethodCountersPointer.class, int.class, int.class);

    // @field
    private final HotSpotGraalRuntime runtime;

    // @cons
    public HotSpotBackend(HotSpotGraalRuntime __runtime, HotSpotProviders __providers)
    {
        super(__providers);
        this.runtime = __runtime;
    }

    public HotSpotGraalRuntime getRuntime()
    {
        return runtime;
    }

    /**
     * Performs any remaining initialization that was deferred until the {@linkplain #getRuntime()
     * runtime} object was initialized and this backend was registered with it.
     */
    public void completeInitialization()
    {
    }

    /**
     * Finds all the registers that are defined by some given LIR.
     *
     * @param lir the LIR to examine
     * @return the registers that are defined by or used as temps for any instruction in {@code lir}
     */
    protected final EconomicSet<Register> gatherDestroyedCallerRegisters(LIR __lir)
    {
        final EconomicSet<Register> __destroyedRegisters = EconomicSet.create(Equivalence.IDENTITY);
        // @closure
        ValueConsumer defConsumer = new ValueConsumer()
        {
            @Override
            public void visitValue(Value __value, OperandMode __mode, EnumSet<OperandFlag> __flags)
            {
                if (ValueUtil.isRegister(__value))
                {
                    final Register __reg = ValueUtil.asRegister(__value);
                    __destroyedRegisters.add(__reg);
                }
            }
        };
        for (AbstractBlockBase<?> __block : __lir.codeEmittingOrder())
        {
            if (__block == null)
            {
                continue;
            }
            for (LIRInstruction __op : __lir.getLIRforBlock(__block))
            {
                if (__op instanceof LabelOp)
                {
                    // don't consider this as a definition
                }
                else
                {
                    __op.visitEachTemp(defConsumer);
                    __op.visitEachOutput(defConsumer);
                }
            }
        }
        return translateToCallerRegisters(__destroyedRegisters);
    }

    /**
     * Updates a given stub with respect to the registers it destroys.
     *
     * Any entry in {@code calleeSaveInfo} that {@linkplain SaveRegistersOp#supportsRemove()
     * supports} pruning will have {@code destroyedRegisters}
     * {@linkplain SaveRegistersOp#remove(EconomicSet) removed} as these registers are declared as
     * temporaries in the stub's {@linkplain ForeignCallLinkage linkage} (and thus will be saved by
     * the stub's caller).
     *
     * @param stub the stub to update
     * @param destroyedRegisters the registers destroyed by the stub
     * @param calleeSaveInfo a map from debug infos to the operations that provide their
     *            {@linkplain RegisterSaveLayout callee-save information}
     * @param frameMap used to {@linkplain FrameMap#offsetForStackSlot(StackSlot) convert} a virtual
     *            slot to a frame slot index
     */
    protected void updateStub(Stub __stub, EconomicSet<Register> __destroyedRegisters, EconomicMap<LIRFrameState, SaveRegistersOp> __calleeSaveInfo, FrameMap __frameMap)
    {
        __stub.initDestroyedCallerRegisters(__destroyedRegisters);

        MapCursor<LIRFrameState, SaveRegistersOp> __cursor = __calleeSaveInfo.getEntries();
        while (__cursor.advance())
        {
            SaveRegistersOp __save = __cursor.getValue();
            if (__save.supportsRemove())
            {
                __save.remove(__destroyedRegisters);
            }
        }
    }

    @Override
    public HotSpotProviders getProviders()
    {
        return (HotSpotProviders) super.getProviders();
    }

    @Override
    public SuitesProvider getSuites()
    {
        return getProviders().getSuites();
    }

    @Override
    public CompiledCode createCompiledCode(ResolvedJavaMethod __method, CompilationRequest __compilationRequest, CompilationResult __compResult)
    {
        HotSpotCompilationRequest __compRequest = __compilationRequest instanceof HotSpotCompilationRequest ? (HotSpotCompilationRequest) __compilationRequest : null;
        return HotSpotCompiledCodeBuilder.createCompiledCode(getCodeCache(), __method, __compRequest, __compResult);
    }
}
