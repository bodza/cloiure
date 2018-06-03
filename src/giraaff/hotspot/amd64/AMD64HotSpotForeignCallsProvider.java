package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.HotSpotForeignCallLinkageImpl;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.CRC32CSubstitutions;
import giraaff.hotspot.replacements.CRC32Substitutions;
import giraaff.word.WordTypes;

// @class AMD64HotSpotForeignCallsProvider
public final class AMD64HotSpotForeignCallsProvider extends HotSpotHostForeignCallsProvider
{
    // @field
    private final Value[] ___nativeABICallerSaveRegisters;

    // @cons
    public AMD64HotSpotForeignCallsProvider(HotSpotGraalRuntime __runtime, MetaAccessProvider __metaAccess, CodeCacheProvider __codeCache, WordTypes __wordTypes, Value[] __nativeABICallerSaveRegisters)
    {
        super(__runtime, __metaAccess, __codeCache, __wordTypes);
        this.___nativeABICallerSaveRegisters = __nativeABICallerSaveRegisters;
    }

    @Override
    public void initialize(HotSpotProviders __providers)
    {
        TargetDescription __target = __providers.getCodeCache().getTarget();
        PlatformKind __word = __target.arch.getWordKind();

        // The calling convention for the exception handler stub is (only?)
        // defined in TemplateInterpreterGenerator::generate_throw_exception()
        // in templateInterpreter_x86_64.cpp around line 1923.
        RegisterValue __exception = AMD64.rax.asValue(LIRKind.reference(__word));
        RegisterValue __exceptionPc = AMD64.rdx.asValue(LIRKind.value(__word));
        CallingConvention __exceptionCc = new CallingConvention(0, Value.ILLEGAL, __exception, __exceptionPc);
        register(new HotSpotForeignCallLinkageImpl(HotSpotBackend.EXCEPTION_HANDLER, 0L, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, __exceptionCc, null, NOT_REEXECUTABLE, LocationIdentity.any()));
        register(new HotSpotForeignCallLinkageImpl(HotSpotBackend.EXCEPTION_HANDLER_IN_CALLER, HotSpotForeignCallLinkage.JUMP_ADDRESS, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, __exceptionCc, null, NOT_REEXECUTABLE, LocationIdentity.any()));

        if (HotSpotRuntime.useCRC32Intrinsics)
        {
            registerForeignCall(CRC32Substitutions.UPDATE_BYTES_CRC32, HotSpotRuntime.updateBytesCRC32Stub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, LocationIdentity.any());
        }
        if (HotSpotRuntime.useCRC32CIntrinsics)
        {
            registerForeignCall(CRC32CSubstitutions.UPDATE_BYTES_CRC32C, HotSpotRuntime.updateBytesCRC32C, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, LocationIdentity.any());
        }

        super.initialize(__providers);
    }

    @Override
    public Value[] getNativeABICallerSaveRegisters()
    {
        return this.___nativeABICallerSaveRegisters;
    }
}
