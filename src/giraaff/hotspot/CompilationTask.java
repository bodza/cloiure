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
    private final HotSpotGraalCompiler compiler;
    private final HotSpotCompilationRequest request;
    private final boolean useProfilingInfo;
    /**
     * Specifies whether the compilation result is installed as the
     * {@linkplain HotSpotNmethod#isDefault() default} nmethod for the compiled method.
     */
    private final boolean installAsDefault;

    private HotSpotInstalledCode installedCode;

    // @cons
    public CompilationTask(HotSpotGraalCompiler compiler, HotSpotCompilationRequest request, boolean useProfilingInfo, boolean installAsDefault)
    {
        super();
        this.compiler = compiler;
        this.request = request;
        this.useProfilingInfo = useProfilingInfo;
        this.installAsDefault = installAsDefault;
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
        HotSpotResolvedJavaMethod method = getMethod();

        if (installAsDefault)
        {
            // If there is already compiled code for this method on our level we simply return.
            // JVMCI compiles are always at the highest compile level, even in non-tiered mode,
            // so we only need to check for that value.
            if (method.hasCodeAtLevel(getEntryBCI(), HotSpotRuntime.compilationLevelFullOptimization))
            {
                return HotSpotCompilationRequestResult.failure("Already compiled", false);
            }
        }

        try
        {
            CompilationResult result = compiler.compile(method, getEntryBCI(), useProfilingInfo);
            if (result != null)
            {
                HotSpotBackend backend = compiler.getGraalRuntime().getBackend();
                installedCode = (HotSpotInstalledCode) backend.createInstalledCode(method, request, result, method.getSpeculationLog(), null, installAsDefault);
                return HotSpotCompilationRequestResult.success(result.getBytecodeSize() - method.getCodeSize());
            }
            return null;
        }
        catch (Throwable t)
        {
            if (t instanceof BailoutException)
            {
                BailoutException bailout = (BailoutException) t;
                /*
                 * Handling of permanent bailouts: Permanent bailouts that can happen for example
                 * due to unsupported unstructured control flow in the bytecodes of a method must
                 * not be retried. Hotspot compile broker will ensure that no recompilation at the
                 * given tier will happen if retry is false.
                 */
                return HotSpotCompilationRequestResult.failure(bailout.getMessage(), !bailout.isPermanent());
            }

            /*
             * Treat random exceptions from the compiler as indicating a problem compiling this
             * method. Report the result of toString instead of getMessage to ensure that the
             * exception type is included in the output in case there's no detail mesage.
             */
            return HotSpotCompilationRequestResult.failure(t.toString(), false);
        }
    }
}
