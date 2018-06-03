package giraaff.hotspot;

import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotCompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotInstalledCode;
import jdk.vm.ci.hotspot.HotSpotNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;

import org.graalvm.collections.EconomicMap;

import giraaff.code.CompilationResult;
import giraaff.core.common.GraalOptions;
import giraaff.core.phases.HighTier;

// @class CompilationTask
public final class CompilationTask
{
    // @field
    private final HotSpotGraalCompiler compiler;
    // @field
    private final HotSpotCompilationRequest request;
    // @field
    private final boolean useProfilingInfo;
    /**
     * Specifies whether the compilation result is installed as the
     * {@linkplain HotSpotNmethod#isDefault() default} nmethod for the compiled method.
     */
    // @field
    private final boolean installAsDefault;

    // @field
    private HotSpotInstalledCode installedCode;

    // @cons
    public CompilationTask(HotSpotGraalCompiler __compiler, HotSpotCompilationRequest __request, boolean __useProfilingInfo, boolean __installAsDefault)
    {
        super();
        this.compiler = __compiler;
        this.request = __request;
        this.useProfilingInfo = __useProfilingInfo;
        this.installAsDefault = __installAsDefault;
    }

    public HotSpotResolvedJavaMethod getMethod()
    {
        return request.getMethod();
    }

    /**
     * Returns the HotSpot id of this compilation.
     *
     * @return HotSpot compile id
     */
    public int getId()
    {
        return request.getId();
    }

    public int getEntryBCI()
    {
        return request.getEntryBCI();
    }

    public HotSpotInstalledCode getInstalledCode()
    {
        return installedCode;
    }

    public HotSpotCompilationRequestResult runCompilation()
    {
        HotSpotResolvedJavaMethod __method = getMethod();

        if (installAsDefault)
        {
            // If there is already compiled code for this method on our level we simply return.
            // JVMCI compiles are always at the highest compile level, even in non-tiered mode,
            // so we only need to check for that value.
            if (__method.hasCodeAtLevel(getEntryBCI(), HotSpotRuntime.compilationLevelFullOptimization))
            {
                return HotSpotCompilationRequestResult.failure("Already compiled", false);
            }
        }

        try
        {
            CompilationResult __result = compiler.compile(__method, getEntryBCI(), useProfilingInfo);
            if (__result != null)
            {
                HotSpotBackend __backend = compiler.getGraalRuntime().getBackend();
                installedCode = (HotSpotInstalledCode) __backend.createInstalledCode(__method, request, __result, __method.getSpeculationLog(), null, installAsDefault);
                return HotSpotCompilationRequestResult.success(__result.getBytecodeSize() - __method.getCodeSize());
            }
            return null;
        }
        catch (Throwable __t)
        {
            if (__t instanceof BailoutException)
            {
                BailoutException __bailout = (BailoutException) __t;
                /*
                 * Handling of permanent bailouts: Permanent bailouts that can happen for example
                 * due to unsupported unstructured control flow in the bytecodes of a method must
                 * not be retried. Hotspot compile broker will ensure that no recompilation at the
                 * given tier will happen if retry is false.
                 */
                return HotSpotCompilationRequestResult.failure(__bailout.getMessage(), !__bailout.isPermanent());
            }

            /*
             * Treat random exceptions from the compiler as indicating a problem compiling this
             * method. Report the result of toString instead of getMessage to ensure that the
             * exception type is included in the output in case there's no detail mesage.
             */
            return HotSpotCompilationRequestResult.failure(__t.toString(), false);
        }
    }
}
