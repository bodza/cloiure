package giraaff.hotspot;

import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotCompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotInstalledCode;
import jdk.vm.ci.hotspot.HotSpotNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCICompiler;

import org.graalvm.collections.EconomicMap;

import giraaff.code.CompilationResult;
import giraaff.core.common.CompilationIdentifier;
import giraaff.core.phases.HighTier;
import giraaff.java.BytecodeParserOptions;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;

// @class CompilationTask
public final class CompilationTask
{
    private final HotSpotGraalCompiler compiler;
    private final HotSpotCompilationIdentifier compilationId;

    private HotSpotInstalledCode installedCode;

    /**
     * Specifies whether the compilation result is installed as the
     * {@linkplain HotSpotNmethod#isDefault() default} nmethod for the compiled method.
     */
    private final boolean installAsDefault;

    private final boolean useProfilingInfo;
    private final OptionValues options;

    // @cons
    public CompilationTask(HotSpotGraalCompiler compiler, HotSpotCompilationRequest request, boolean useProfilingInfo, boolean installAsDefault, OptionValues options)
    {
        super();
        this.compiler = compiler;
        this.compilationId = new HotSpotCompilationIdentifier(request);
        this.useProfilingInfo = useProfilingInfo;
        this.installAsDefault = installAsDefault;

        // Disable inlining if HotSpot has it disabled unless it's been explicitly set in Graal.
        OptionValues newOptions = options;
        if (!HotSpotRuntime.inline)
        {
            EconomicMap<OptionKey<?>, Object> m = OptionValues.newOptionMap();
            if (HighTier.Options.Inline.getValue(options) && !HighTier.Options.Inline.hasBeenSet(options))
            {
                m.put(HighTier.Options.Inline, false);
            }
            if (BytecodeParserOptions.InlineDuringParsing.getValue(options) && !BytecodeParserOptions.InlineDuringParsing.hasBeenSet(options))
            {
                m.put(BytecodeParserOptions.InlineDuringParsing, false);
            }
            if (!m.isEmpty())
            {
                newOptions = new OptionValues(options, m);
            }
        }
        this.options = newOptions;
    }

    public HotSpotResolvedJavaMethod getMethod()
    {
        return getRequest().getMethod();
    }

    CompilationIdentifier getCompilationIdentifier()
    {
        return compilationId;
    }

    /**
     * Returns the HotSpot id of this compilation.
     *
     * @return HotSpot compile id
     */
    public int getId()
    {
        return getRequest().getId();
    }

    public int getEntryBCI()
    {
        return getRequest().getEntryBCI();
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
            CompilationResult result = compiler.compile(method, getEntryBCI(), useProfilingInfo, compilationId, options);
            if (result != null)
            {
                HotSpotBackend backend = compiler.getGraalRuntime().getBackend();
                installedCode = (HotSpotInstalledCode) backend.createInstalledCode(method, getRequest(), result, method.getSpeculationLog(), null, installAsDefault);
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

    @Override
    public String toString()
    {
        return "Compilation[id=" + getId() + ", " + getMethod().format("%H.%n(%p)") + (getEntryBCI() == JVMCICompiler.INVOCATION_ENTRY_BCI ? "" : "@" + getEntryBCI()) + "]";
    }

    private HotSpotCompilationRequest getRequest()
    {
        return compilationId.getRequest();
    }
}
