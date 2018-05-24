package giraaff.hotspot.stubs;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.site.Call;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.meta.DefaultProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.TriState;

import org.graalvm.collections.EconomicSet;

import giraaff.code.CompilationResult;
import giraaff.core.GraalCompiler;
import giraaff.core.common.CompilationIdentifier;
import giraaff.core.common.GraalOptions;
import giraaff.core.target.Backend;
import giraaff.hotspot.HotSpotCompiledCodeBuilder;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.StubStartNode;
import giraaff.lir.asm.CompilationResultBuilderFactory;
import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.StructuredGraph;
import giraaff.options.OptionValues;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.Suites;
import giraaff.util.CollectionsUtil;

/**
 * Base class for implementing some low level code providing the out-of-line slow path for a snippet
 * and/or a callee saved call to a HotSpot C/C++ runtime function or even a another compiled Java method.
 */
public abstract class Stub
{
    /**
     * The linkage information for a call to this stub from compiled code.
     */
    protected final HotSpotForeignCallLinkage linkage;

    /**
     * The code installed for the stub.
     */
    protected InstalledCode code;

    /**
     * The registers destroyed by this stub (from the caller's perspective).
     */
    private EconomicSet<Register> destroyedCallerRegisters;

    private static boolean checkRegisterSetEquivalency(EconomicSet<Register> a, EconomicSet<Register> b)
    {
        if (a == b)
        {
            return true;
        }
        if (a.size() != b.size())
        {
            return false;
        }
        return CollectionsUtil.allMatch(a, e -> b.contains(e));
    }

    public void initDestroyedCallerRegisters(EconomicSet<Register> registers)
    {
        destroyedCallerRegisters = registers;
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

    protected final OptionValues options;
    protected final HotSpotProviders providers;

    /**
     * Creates a new stub.
     *
     * @param linkage linkage details for a call to the stub
     */
    public Stub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        this.linkage = linkage;
        this.options = options;
        this.providers = providers;
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
     *
     * @param compilationId unique compilation id for the stub
     */
    protected abstract StructuredGraph getGraph(CompilationIdentifier compilationId);

    @Override
    public String toString()
    {
        return "Stub<" + linkage.getDescriptor() + ">";
    }

    /**
     * Gets the method the stub's code will be associated with once installed. This may be null.
     */
    protected abstract ResolvedJavaMethod getInstalledCodeOwner();

    /**
     * Gets the code for this stub, compiling it first if necessary.
     */
    public synchronized InstalledCode getCode(final Backend backend)
    {
        if (code == null)
        {
            CodeCacheProvider codeCache = providers.getCodeCache();
            CompilationResult compResult = buildCompilationResult(backend);
            HotSpotCompiledCode compiledCode = HotSpotCompiledCodeBuilder.createCompiledCode(codeCache, null, null, compResult);
            code = codeCache.installCode(null, compiledCode, null, null, false);
        }

        return code;
    }

    private CompilationResult buildCompilationResult(final Backend backend)
    {
        CompilationIdentifier compilationId = getStubCompilationId();
        final StructuredGraph graph = getGraph(compilationId);
        CompilationResult compResult = new CompilationResult(compilationId, toString());

        // Stubs cannot be recompiled so they cannot be compiled with assumptions

        if (!(graph.start() instanceof StubStartNode))
        {
            StubStartNode newStart = graph.add(new StubStartNode(Stub.this));
            newStart.setStateAfter(graph.start().stateAfter());
            graph.replaceFixed(graph.start(), newStart);
        }

        Suites suites = createSuites();
        GraalCompiler.emitFrontEnd(providers, backend, graph, providers.getSuites().getDefaultGraphBuilderSuite(), OptimisticOptimizations.ALL, DefaultProfilingInfo.get(TriState.UNKNOWN), suites);
        LIRSuites lirSuites = createLIRSuites();
        GraalCompiler.emitBackEnd(graph, Stub.this, getInstalledCodeOwner(), backend, compResult, CompilationResultBuilderFactory.Default, getRegisterConfig(), lirSuites);
        return compResult;
    }

    /**
     * Gets a {@link CompilationResult} that can be used for code generation. Required for AOT.
     */
    public CompilationResult getCompilationResult(final Backend backend)
    {
        return buildCompilationResult(backend);
    }

    public CompilationIdentifier getStubCompilationId()
    {
        return new StubCompilationIdentifier(this);
    }

    /**
     * Checks the conditions a compilation must satisfy to be installed as a RuntimeStub.
     */
    private boolean checkStubInvariants(CompilationResult compResult)
    {
        // Stubs cannot be recompiled so they cannot be compiled with
        // assumptions and there is no point in recording evol_method dependencies

        for (DataPatch data : compResult.getDataPatches())
        {
            if (data.reference instanceof ConstantReference)
            {
                ConstantReference ref = (ConstantReference) data.reference;
                if (ref.getConstant() instanceof HotSpotMetaspaceConstant)
                {
                    HotSpotMetaspaceConstant c = (HotSpotMetaspaceConstant) ref.getConstant();
                    if (c.asResolvedJavaType() != null && c.asResolvedJavaType().getName().equals("[I"))
                    {
                        // special handling for NewArrayStub
                        // embedding the type '[I' is safe, since it is never unloaded
                        continue;
                    }
                }
            }
        }
        for (Infopoint infopoint : compResult.getInfopoints())
        {
            Call call = (Call) infopoint;
            HotSpotForeignCallLinkage callLinkage = (HotSpotForeignCallLinkage) call.target;
        }
        return true;
    }

    protected Suites createSuites()
    {
        Suites defaultSuites = providers.getSuites().getDefaultSuites(options);
        return new Suites(new PhaseSuite<>(), defaultSuites.getMidTier(), defaultSuites.getLowTier());
    }

    protected LIRSuites createLIRSuites()
    {
        return new LIRSuites(providers.getSuites().getDefaultLIRSuites(options));
    }
}
