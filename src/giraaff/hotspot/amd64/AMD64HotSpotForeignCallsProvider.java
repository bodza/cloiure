package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.HotSpotForeignCallLinkageImpl;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.CRC32CSubstitutions;
import giraaff.hotspot.replacements.CRC32Substitutions;
import giraaff.options.OptionValues;
import giraaff.word.WordTypes;

public class AMD64HotSpotForeignCallsProvider extends HotSpotHostForeignCallsProvider
{
    public static final ForeignCallDescriptor ARITHMETIC_SIN_STUB = new ForeignCallDescriptor("arithmeticSinStub", double.class, double.class);
    public static final ForeignCallDescriptor ARITHMETIC_COS_STUB = new ForeignCallDescriptor("arithmeticCosStub", double.class, double.class);
    public static final ForeignCallDescriptor ARITHMETIC_TAN_STUB = new ForeignCallDescriptor("arithmeticTanStub", double.class, double.class);
    public static final ForeignCallDescriptor ARITHMETIC_EXP_STUB = new ForeignCallDescriptor("arithmeticExpStub", double.class, double.class);
    public static final ForeignCallDescriptor ARITHMETIC_POW_STUB = new ForeignCallDescriptor("arithmeticPowStub", double.class, double.class, double.class);
    public static final ForeignCallDescriptor ARITHMETIC_LOG_STUB = new ForeignCallDescriptor("arithmeticLogStub", double.class, double.class);
    public static final ForeignCallDescriptor ARITHMETIC_LOG10_STUB = new ForeignCallDescriptor("arithmeticLog10Stub", double.class, double.class);

    private final Value[] nativeABICallerSaveRegisters;

    public AMD64HotSpotForeignCallsProvider(HotSpotJVMCIRuntimeProvider jvmciRuntime, HotSpotGraalRuntimeProvider runtime, MetaAccessProvider metaAccess, CodeCacheProvider codeCache, WordTypes wordTypes, Value[] nativeABICallerSaveRegisters)
    {
        super(jvmciRuntime, runtime, metaAccess, codeCache, wordTypes);
        this.nativeABICallerSaveRegisters = nativeABICallerSaveRegisters;
    }

    @Override
    public void initialize(HotSpotProviders providers, OptionValues options)
    {
        GraalHotSpotVMConfig config = runtime.getVMConfig();
        TargetDescription target = providers.getCodeCache().getTarget();
        PlatformKind word = target.arch.getWordKind();

        // The calling convention for the exception handler stub is (only?) defined in
        // TemplateInterpreterGenerator::generate_throw_exception()
        // in templateInterpreter_x86_64.cpp around line 1923
        RegisterValue exception = AMD64.rax.asValue(LIRKind.reference(word));
        RegisterValue exceptionPc = AMD64.rdx.asValue(LIRKind.value(word));
        CallingConvention exceptionCc = new CallingConvention(0, Value.ILLEGAL, exception, exceptionPc);
        register(new HotSpotForeignCallLinkageImpl(HotSpotBackend.EXCEPTION_HANDLER, 0L, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, exceptionCc, null, NOT_REEXECUTABLE, LocationIdentity.any()));
        register(new HotSpotForeignCallLinkageImpl(HotSpotBackend.EXCEPTION_HANDLER_IN_CALLER, HotSpotForeignCallLinkage.JUMP_ADDRESS, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, exceptionCc, null, NOT_REEXECUTABLE, LocationIdentity.any()));

        link(new AMD64MathStub(ARITHMETIC_LOG_STUB, options, providers, registerStubCall(ARITHMETIC_LOG_STUB, REEXECUTABLE, Transition.LEAF, NO_LOCATIONS)));
        link(new AMD64MathStub(ARITHMETIC_LOG10_STUB, options, providers, registerStubCall(ARITHMETIC_LOG10_STUB, REEXECUTABLE, Transition.LEAF, NO_LOCATIONS)));
        link(new AMD64MathStub(ARITHMETIC_SIN_STUB, options, providers, registerStubCall(ARITHMETIC_SIN_STUB, REEXECUTABLE, Transition.LEAF, NO_LOCATIONS)));
        link(new AMD64MathStub(ARITHMETIC_COS_STUB, options, providers, registerStubCall(ARITHMETIC_COS_STUB, REEXECUTABLE, Transition.LEAF, NO_LOCATIONS)));
        link(new AMD64MathStub(ARITHMETIC_TAN_STUB, options, providers, registerStubCall(ARITHMETIC_TAN_STUB, REEXECUTABLE, Transition.LEAF, NO_LOCATIONS)));
        link(new AMD64MathStub(ARITHMETIC_EXP_STUB, options, providers, registerStubCall(ARITHMETIC_EXP_STUB, REEXECUTABLE, Transition.LEAF, NO_LOCATIONS)));
        link(new AMD64MathStub(ARITHMETIC_POW_STUB, options, providers, registerStubCall(ARITHMETIC_POW_STUB, REEXECUTABLE, Transition.LEAF, NO_LOCATIONS)));

        if (config.useCRC32Intrinsics)
        {
            // This stub does callee saving
            registerForeignCall(CRC32Substitutions.UPDATE_BYTES_CRC32, config.updateBytesCRC32Stub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, LocationIdentity.any());
        }
        if (config.useCRC32CIntrinsics)
        {
            registerForeignCall(CRC32CSubstitutions.UPDATE_BYTES_CRC32C, config.updateBytesCRC32C, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, LocationIdentity.any());
        }

        super.initialize(providers, options);
    }

    @Override
    public Value[] getNativeABICallerSaveRegisters()
    {
        return nativeABICallerSaveRegisters;
    }
}
