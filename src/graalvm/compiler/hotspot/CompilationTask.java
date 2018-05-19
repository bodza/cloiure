package graalvm.compiler.hotspot;

import static graalvm.compiler.core.phases.HighTier.Options.Inline;
import static graalvm.compiler.java.BytecodeParserOptions.InlineDuringParsing;

import java.util.List;

import org.graalvm.collections.EconomicMap;
import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.CompilationPrinter;
import graalvm.compiler.core.CompilationWrapper;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.options.EnumOptionKey;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.hotspot.EventProvider;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotCompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotInstalledCode;
import jdk.vm.ci.hotspot.HotSpotJVMCICompilerFactory;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCICompiler;
import jdk.vm.ci.services.JVMCIServiceLocator;

public class CompilationTask
{
    private static final EventProvider eventProvider;

    static
    {
        List<EventProvider> providers = JVMCIServiceLocator.getProviders(EventProvider.class);
        if (providers.size() > 1)
        {
            throw new GraalError("Multiple %s providers found: %s", EventProvider.class.getName(), providers);
        }
        else if (providers.isEmpty())
        {
            eventProvider = EventProvider.createEmptyEventProvider();
        }
        else
        {
            eventProvider = providers.get(0);
        }
    }

    private final HotSpotJVMCIRuntimeProvider jvmciRuntime;

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

    final class HotSpotCompilationWrapper extends CompilationWrapper<HotSpotCompilationRequestResult>
    {
        private final EventProvider.CompilationEvent compilationEvent;
        CompilationResult result;

        HotSpotCompilationWrapper(EventProvider.CompilationEvent compilationEvent)
        {
            this.compilationEvent = compilationEvent;
        }

        @Override
        public String toString()
        {
            return getMethod().format("%H.%n(%p)");
        }

        @Override
        protected HotSpotCompilationRequestResult handleException(Throwable t)
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
            // Log a failure event.
            EventProvider.CompilerFailureEvent event = eventProvider.newCompilerFailureEvent();
            if (event.shouldWrite())
            {
                event.setCompileId(getId());
                event.setMessage(t.getMessage());
                event.commit();
            }

            /*
             * Treat random exceptions from the compiler as indicating a problem compiling this
             * method. Report the result of toString instead of getMessage to ensure that the
             * exception type is included in the output in case there's no detail mesage.
             */
            return HotSpotCompilationRequestResult.failure(t.toString(), false);
        }

        @Override
        protected HotSpotCompilationRequestResult performCompilation()
        {
            HotSpotResolvedJavaMethod method = getMethod();
            int entryBCI = getEntryBCI();

            final CompilationPrinter printer = CompilationPrinter.begin(options, compilationId, method, entryBCI);

            try
            {
                // Begin the compilation event.
                compilationEvent.begin();
                result = compiler.compile(method, entryBCI, useProfilingInfo, compilationId, options);
            }
            finally
            {
                // End the compilation event.
                compilationEvent.end();
            }

            if (result != null)
            {
                installMethod(result);
                // Installation is included in compilation time and memory usage reported by printer
                printer.finish(result);
                return HotSpotCompilationRequestResult.success(result.getBytecodeSize() - method.getCodeSize());
            }
            return null;
        }
    }

    public CompilationTask(HotSpotJVMCIRuntimeProvider jvmciRuntime, HotSpotGraalCompiler compiler, HotSpotCompilationRequest request, boolean useProfilingInfo, boolean installAsDefault, OptionValues options)
    {
        this.jvmciRuntime = jvmciRuntime;
        this.compiler = compiler;
        this.compilationId = new HotSpotCompilationIdentifier(request);
        this.useProfilingInfo = useProfilingInfo;
        this.installAsDefault = installAsDefault;

        /*
         * Disable inlining if HotSpot has it disabled unless it's been explicitly set in Graal.
         */
        HotSpotGraalRuntimeProvider graalRuntime = compiler.getGraalRuntime();
        GraalHotSpotVMConfig config = graalRuntime.getVMConfig();
        OptionValues newOptions = options;
        if (!config.inline)
        {
            EconomicMap<OptionKey<?>, Object> m = OptionValues.newOptionMap();
            if (Inline.getValue(options) && !Inline.hasBeenSet(options))
            {
                m.put(Inline, false);
            }
            if (InlineDuringParsing.getValue(options) && !InlineDuringParsing.hasBeenSet(options))
            {
                m.put(InlineDuringParsing, false);
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

    /**
     * @return the compilation id plus a trailing '%' if the compilation is an OSR to match
     *         PrintCompilation style output
     */
    public String getIdString()
    {
        if (getEntryBCI() != JVMCICompiler.INVOCATION_ENTRY_BCI)
        {
            return getId() + "%";
        }
        else
        {
            return Integer.toString(getId());
        }
    }

    public HotSpotInstalledCode getInstalledCode()
    {
        return installedCode;
    }

    public HotSpotCompilationRequestResult runCompilation()
    {
        HotSpotGraalRuntimeProvider graalRuntime = compiler.getGraalRuntime();
        GraalHotSpotVMConfig config = graalRuntime.getVMConfig();
        int entryBCI = getEntryBCI();
        boolean isOSR = entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;
        HotSpotResolvedJavaMethod method = getMethod();

        // Log a compilation event.
        EventProvider.CompilationEvent compilationEvent = eventProvider.newCompilationEvent();

        if (installAsDefault)
        {
            // If there is already compiled code for this method on our level we simply return.
            // JVMCI compiles are always at the highest compile level, even in non-tiered mode so we
            // only need to check for that value.
            if (method.hasCodeAtLevel(entryBCI, config.compilationLevelFullOptimization))
            {
                return HotSpotCompilationRequestResult.failure("Already compiled", false);
            }
        }

        HotSpotCompilationWrapper compilation = new HotSpotCompilationWrapper(compilationEvent);
        try
        {
            return compilation.run();
        }
        finally
        {
            try
            {
                int compiledBytecodes = 0;
                int codeSize = 0;

                if (compilation.result != null)
                {
                    compiledBytecodes = compilation.result.getBytecodeSize();
                    if (installedCode != null)
                    {
                        codeSize = installedCode.getSize();
                    }
                }

                // Log a compilation event.
                if (compilationEvent.shouldWrite())
                {
                    compilationEvent.setMethod(method.format("%H.%n(%p)"));
                    compilationEvent.setCompileId(getId());
                    compilationEvent.setCompileLevel(config.compilationLevelFullOptimization);
                    compilationEvent.setSucceeded(compilation.result != null && installedCode != null);
                    compilationEvent.setIsOsr(isOSR);
                    compilationEvent.setCodeSize(codeSize);
                    compilationEvent.setInlinedBytes(compiledBytecodes);
                    compilationEvent.commit();
                }
            }
            catch (Throwable t)
            {
                return compilation.handleException(t);
            }
        }
    }

    @SuppressWarnings("try")
    private void installMethod(final CompilationResult compResult)
    {
        final CodeCacheProvider codeCache = jvmciRuntime.getHostJVMCIBackend().getCodeCache();
        HotSpotBackend backend = compiler.getGraalRuntime().getHostBackend();
        installedCode = (HotSpotInstalledCode) backend.createInstalledCode(getRequest().getMethod(), getRequest(), compResult, getRequest().getMethod().getSpeculationLog(), null, installAsDefault);
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
