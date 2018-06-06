package giraaff.hotspot;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.runtime.JVMCICompiler;

import giraaff.core.common.NumUtil;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotLoweringProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.stubs.Stub;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.nodes.StructuredGraph;

///
// Common functionality of HotSpot host backends.
///
// @class HotSpotHostBackend
public abstract class HotSpotHostBackend extends HotSpotBackend
{
    ///
    // Descriptor for {@code SharedRuntime::deopt_blob()->unpack()}.
    ///
    // @def
    public static final ForeignCallDescriptor DEOPTIMIZATION_HANDLER = new ForeignCallDescriptor("deoptHandler", void.class);

    ///
    // Descriptor for {@code SharedRuntime::deopt_blob()->uncommon_trap()}.
    ///
    // @def
    public static final ForeignCallDescriptor UNCOMMON_TRAP_HANDLER = new ForeignCallDescriptor("uncommonTrapHandler", void.class);

    // @cons HotSpotHostBackend
    public HotSpotHostBackend(HotSpotGraalRuntime __runtime, HotSpotProviders __providers)
    {
        super(__runtime, __providers);
    }

    @Override
    public void completeInitialization()
    {
        final HotSpotProviders __providers = getProviders();
        HotSpotHostForeignCallsProvider __foreignCalls = (HotSpotHostForeignCallsProvider) __providers.getForeignCalls();
        final HotSpotLoweringProvider __lowerer = (HotSpotLoweringProvider) __providers.getLowerer();

        __foreignCalls.initialize(__providers);
        __lowerer.initialize(__providers);
    }

    protected CallingConvention makeCallingConvention(StructuredGraph __graph, Stub __stub)
    {
        if (__stub != null)
        {
            return __stub.getLinkage().getIncomingCallingConvention();
        }

        CallingConvention __cc = CodeUtil.getCallingConvention(getCodeCache(), HotSpotCallingConventionType.JavaCallee, __graph.method(), this);
        if (__graph.getEntryBCI() != JVMCICompiler.INVOCATION_ENTRY_BCI)
        {
            // for OSR, only a pointer is passed to the method.
            JavaType[] __parameterTypes = new JavaType[] { getMetaAccess().lookupJavaType(long.class) };
            CallingConvention __tmp = getCodeCache().getRegisterConfig().getCallingConvention(HotSpotCallingConventionType.JavaCallee, getMetaAccess().lookupJavaType(void.class), __parameterTypes, this);
            __cc = new CallingConvention(__cc.getStackSize(), __cc.getReturn(), __tmp.getArgument(0));
        }
        return __cc;
    }

    public void emitStackOverflowCheck(CompilationResultBuilder __crb)
    {
        if (HotSpotRuntime.useStackBanging)
        {
            // Each code entry causes one stack bang n pages down the stack where n is configurable
            // by StackShadowPages. The setting depends on the maximum depth of VM call stack or native
            // before going back into java code, since only java code can raise a stack overflow exception
            // using the stack banging mechanism. The VM and native code does not detect stack overflow.
            // The code in JavaCalls::call() checks that there is at least n pages available, so all
            // entry code needs to do is bang once for the end of this shadow zone.
            // The entry code may need to bang additional pages if the framesize is greater than a page.

            int __pageSize = HotSpotRuntime.vmPageSize;
            int __bangEnd = NumUtil.roundUp(HotSpotRuntime.stackShadowPages * 4 * CodeUtil.K, __pageSize);

            // This is how far the previous frame's stack banging extended.
            int __bangEndSafe = __bangEnd;

            int __frameSize = __crb.___frameMap.frameSize();
            if (__frameSize > __pageSize)
            {
                __bangEnd += __frameSize;
            }

            int __bangOffset = __bangEndSafe;
            while (__bangOffset <= __bangEnd)
            {
                // Need at least one stack bang at end of shadow zone.
                bangStackWithOffset(__crb, __bangOffset);
                __bangOffset += __pageSize;
            }
        }
    }

    protected abstract void bangStackWithOffset(CompilationResultBuilder __crb, int __bangOffset);
}
