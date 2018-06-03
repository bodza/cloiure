package giraaff.hotspot.stubs;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.meta.DefaultProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.TriState;

import org.graalvm.collections.EconomicSet;

import giraaff.code.CompilationResult;
import giraaff.core.GraalCompiler;
import giraaff.core.target.Backend;
import giraaff.hotspot.HotSpotCompiledCodeBuilder;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.StubStartNode;
import giraaff.lir.asm.CompilationResultBuilderFactory;
import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.Suites;

/**
 * Base class for implementing some low level code providing the out-of-line slow path for a snippet
 * and/or a callee saved call to a HotSpot C/C++ runtime function or even a another compiled Java method.
 */
// @class Stub
public abstract class Stub
{
    /**
     * The linkage information for a call to this stub from compiled code.
     */
    // @field
    protected final HotSpotForeignCallLinkage linkage;

    /**
     * The code installed for the stub.
     */
    // @field
    protected InstalledCode code;

    /**
     * The registers destroyed by this stub (from the caller's perspective).
     */
    // @field
    private EconomicSet<Register> destroyedCallerRegisters;

    public void initDestroyedCallerRegisters(EconomicSet<Register> __registers)
    {
        destroyedCallerRegisters = __registers;
    }

    /**
     * Gets the registers destroyed by this stub from a caller's perspective. These are the
     * temporaries of this stub and must thus be caller saved by a callers of this stub.
     */
    public EconomicSet<Register> getDestroyedCallerRegisters()
    {
        return destroyedCallerRegisters;
    }

    /**
     * Determines if this stub preserves all registers apart from those it
     * {@linkplain #getDestroyedCallerRegisters() destroys}.
     */
    public boolean preservesRegisters()
    {
        return true;
    }

    // @field
    protected final HotSpotProviders providers;

    /**
     * Creates a new stub.
     *
     * @param linkage linkage details for a call to the stub
     */
    // @cons
    public Stub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super();
        this.linkage = __linkage;
        this.providers = __providers;
    }

    /**
     * Gets the linkage for a call to this stub from compiled code.
     */
    public HotSpotForeignCallLinkage getLinkage()
    {
        return linkage;
    }

    public RegisterConfig getRegisterConfig()
    {
        return null;
    }

    /**
     * Gets the graph that from which the code for this stub will be compiled.
     */
    protected abstract StructuredGraph getStubGraph();

    /**
     * Gets the method the stub's code will be associated with once installed. This may be null.
     */
    protected abstract ResolvedJavaMethod getInstalledCodeOwner();

    /**
     * Gets the code for this stub, compiling it first if necessary.
     */
    public synchronized InstalledCode getCode(final Backend __backend)
    {
        if (this.code == null)
        {
            CodeCacheProvider __codeCache = providers.getCodeCache();
            CompilationResult __compResult = buildCompilationResult(__backend);
            HotSpotCompiledCode __compiledCode = HotSpotCompiledCodeBuilder.createCompiledCode(__codeCache, null, null, __compResult);
            this.code = __codeCache.installCode(null, __compiledCode, null, null, false);
        }

        return this.code;
    }

    private final CompilationResult buildCompilationResult(final Backend __backend)
    {
        final StructuredGraph __graph = getStubGraph();

        // stubs cannot be recompiled, so they cannot be compiled with assumptions
        if (!(__graph.start() instanceof StubStartNode))
        {
            StubStartNode __newStart = __graph.add(new StubStartNode(Stub.this));
            __newStart.setStateAfter(__graph.start().stateAfter());
            __graph.replaceFixed(__graph.start(), __newStart);
        }

        Suites __suites = providers.getSuites().getDefaultSuites();
        __suites = new Suites(new PhaseSuite<>(), __suites.getMidTier(), __suites.getLowTier());
        GraalCompiler.emitFrontEnd(providers, __backend, __graph, providers.getSuites().getDefaultGraphBuilderSuite(), OptimisticOptimizations.ALL, DefaultProfilingInfo.get(TriState.UNKNOWN), __suites);
        CompilationResult __result = new CompilationResult();
        LIRSuites __lirSuites = new LIRSuites(providers.getSuites().getDefaultLIRSuites());
        GraalCompiler.emitBackEnd(__graph, Stub.this, getInstalledCodeOwner(), __backend, __result, CompilationResultBuilderFactory.DEFAULT, getRegisterConfig(), __lirSuites);
        return __result;
    }
}
