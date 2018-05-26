package giraaff.hotspot;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.meta.JavaKind;
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
import giraaff.options.OptionValues;

/**
 * Common functionality of HotSpot host backends.
 */
public abstract class HotSpotHostBackend extends HotSpotBackend
{
    /**
     * Descriptor for {@code SharedRuntime::deopt_blob()->unpack()}.
     */
    public static final ForeignCallDescriptor DEOPTIMIZATION_HANDLER = new ForeignCallDescriptor("deoptHandler", void.class);

    /**
     * Descriptor for {@code SharedRuntime::deopt_blob()->uncommon_trap()}.
     */
    public static final ForeignCallDescriptor UNCOMMON_TRAP_HANDLER = new ForeignCallDescriptor("uncommonTrapHandler", void.class);

    protected final GraalHotSpotVMConfig config;

    public HotSpotHostBackend(GraalHotSpotVMConfig config, HotSpotGraalRuntimeProvider runtime, HotSpotProviders providers)
    {
        super(runtime, providers);
        this.config = config;
    }

    @Override
    public void completeInitialization(HotSpotJVMCIRuntime jvmciRuntime, OptionValues options)
    {
        final HotSpotProviders providers = getProviders();
        HotSpotHostForeignCallsProvider foreignCalls = (HotSpotHostForeignCallsProvider) providers.getForeignCalls();
        final HotSpotLoweringProvider lowerer = (HotSpotLoweringProvider) providers.getLowerer();

        foreignCalls.initialize(providers, options);
        lowerer.initialize(options, providers, config);
    }

    protected CallingConvention makeCallingConvention(StructuredGraph graph, Stub stub)
    {
        if (stub != null)
        {
            return stub.getLinkage().getIncomingCallingConvention();
        }

        CallingConvention cc = CodeUtil.getCallingConvention(getCodeCache(), HotSpotCallingConventionType.JavaCallee, graph.method(), this);
        if (graph.getEntryBCI() != JVMCICompiler.INVOCATION_ENTRY_BCI)
        {
            // for OSR, only a pointer is passed to the method.
            JavaType[] parameterTypes = new JavaType[] { getMetaAccess().lookupJavaType(long.class) };
            CallingConvention tmp = getCodeCache().getRegisterConfig().getCallingConvention(HotSpotCallingConventionType.JavaCallee, getMetaAccess().lookupJavaType(void.class), parameterTypes, this);
            cc = new CallingConvention(cc.getStackSize(), cc.getReturn(), tmp.getArgument(0));
        }
        return cc;
    }

    public void emitStackOverflowCheck(CompilationResultBuilder crb)
    {
        if (config.useStackBanging)
        {
            // Each code entry causes one stack bang n pages down the stack where n is configurable
            // by StackShadowPages. The setting depends on the maximum depth of VM call stack or native
            // before going back into java code, since only java code can raise a stack overflow exception
            // using the stack banging mechanism. The VM and native code does not detect stack overflow.
            // The code in JavaCalls::call() checks that there is at least n pages available, so all
            // entry code needs to do is bang once for the end of this shadow zone.
            // The entry code may need to bang additional pages if the framesize is greater than a page.

            int pageSize = config.vmPageSize;
            int bangEnd = NumUtil.roundUp(config.stackShadowPages * 4 * CodeUtil.K, pageSize);

            // This is how far the previous frame's stack banging extended.
            int bangEndSafe = bangEnd;

            int frameSize = crb.frameMap.frameSize();
            if (frameSize > pageSize)
            {
                bangEnd += frameSize;
            }

            int bangOffset = bangEndSafe;
            while (bangOffset <= bangEnd)
            {
                // Need at least one stack bang at end of shadow zone.
                bangStackWithOffset(crb, bangOffset);
                bangOffset += pageSize;
            }
        }
    }

    protected abstract void bangStackWithOffset(CompilationResultBuilder crb, int bangOffset);
}
