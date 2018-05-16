package graalvm.compiler.hotspot.stubs;

import static java.util.Collections.singletonList;
import static graalvm.compiler.core.GraalCompiler.emitBackEnd;
import static graalvm.compiler.core.GraalCompiler.emitFrontEnd;
import static graalvm.compiler.core.common.GraalOptions.GeneratePIC;
import static graalvm.compiler.debug.DebugContext.DEFAULT_LOG_STREAM;
import static graalvm.compiler.debug.DebugOptions.DebugStubsAndSnippets;
import static graalvm.compiler.hotspot.HotSpotHostBackend.UNCOMMON_TRAP_HANDLER;
import static graalvm.util.CollectionsUtil.allMatch;

import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

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
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.target.Backend;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.DebugContext.Description;
import graalvm.compiler.hotspot.HotSpotCompiledCodeBuilder;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.StubStartNode;
import graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import graalvm.compiler.lir.phases.LIRPhase;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import graalvm.compiler.lir.profiling.MoveProfilingPhase;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.Suites;
import graalvm.compiler.printer.GraalDebugHandlersFactory;

/**
 * Base class for implementing some low level code providing the out-of-line slow path for a snippet
 * and/or a callee saved call to a HotSpot C/C++ runtime function or even a another compiled Java
 * method.
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
        return allMatch(a, e -> b.contains(e));
    }

    public void initDestroyedCallerRegisters(EconomicSet<Register> registers)
    {
        assert registers != null;
        assert destroyedCallerRegisters == null || checkRegisterSetEquivalency(registers, destroyedCallerRegisters) : "cannot redefine";
        destroyedCallerRegisters = registers;
    }

    /**
     * Gets the registers destroyed by this stub from a caller's perspective. These are the
     * temporaries of this stub and must thus be caller saved by a callers of this stub.
     */
    public EconomicSet<Register> getDestroyedCallerRegisters()
    {
        assert destroyedCallerRegisters != null : "not yet initialized";
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
        this.options = new OptionValues(options, GraalOptions.TraceInlining, GraalOptions.TraceInliningForStubsAndSnippets.getValue(options));
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
    protected abstract StructuredGraph getGraph(DebugContext debug, CompilationIdentifier compilationId);

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
     * Gets a context object for the debug scope created when producing the code for this stub.
     */
    protected abstract Object debugScopeContext();

    private static final AtomicInteger nextStubId = new AtomicInteger();

    private DebugContext openDebugContext(DebugContext outer)
    {
        if (DebugStubsAndSnippets.getValue(options))
        {
            Description description = new Description(linkage, "Stub_" + nextStubId.incrementAndGet());
            return DebugContext.create(options, description, outer.getGlobalMetrics(), DEFAULT_LOG_STREAM, singletonList(new GraalDebugHandlersFactory(providers.getSnippetReflection())));
        }
        return DebugContext.DISABLED;
    }

    /**
     * Gets the code for this stub, compiling it first if necessary.
     */
    @SuppressWarnings("try")
    public synchronized InstalledCode getCode(final Backend backend)
    {
        if (code == null)
        {
            try (DebugContext debug = openDebugContext(DebugContext.forCurrentThread()))
            {
                try (DebugContext.Scope d = debug.scope("CompilingStub", providers.getCodeCache(), debugScopeContext()))
                {
                    CodeCacheProvider codeCache = providers.getCodeCache();
                    CompilationResult compResult = buildCompilationResult(debug, backend);
                    try (DebugContext.Scope s = debug.scope("CodeInstall", compResult); DebugContext.Activation a = debug.activate())
                    {
                        assert destroyedCallerRegisters != null;
                        // Add a GeneratePIC check here later, we don't want to install
                        // code if we don't have a corresponding VM global symbol.
                        HotSpotCompiledCode compiledCode = HotSpotCompiledCodeBuilder.createCompiledCode(codeCache, null, null, compResult);
                        code = codeCache.installCode(null, compiledCode, null, null, false);
                    }
                    catch (Throwable e)
                    {
                        throw debug.handle(e);
                    }
                }
                catch (Throwable e)
                {
                    throw debug.handle(e);
                }
                assert code != null : "error installing stub " + this;
            }
        }

        return code;
    }

    @SuppressWarnings("try")
    private CompilationResult buildCompilationResult(DebugContext debug, final Backend backend)
    {
        CompilationIdentifier compilationId = getStubCompilationId();
        final StructuredGraph graph = getGraph(debug, compilationId);
        CompilationResult compResult = new CompilationResult(compilationId, toString(), GeneratePIC.getValue(options));

        // Stubs cannot be recompiled so they cannot be compiled with assumptions
        assert graph.getAssumptions() == null;

        if (!(graph.start() instanceof StubStartNode))
        {
            StubStartNode newStart = graph.add(new StubStartNode(Stub.this));
            newStart.setStateAfter(graph.start().stateAfter());
            graph.replaceFixed(graph.start(), newStart);
        }

        try (DebugContext.Scope s0 = debug.scope("StubCompilation", graph, providers.getCodeCache()))
        {
            Suites suites = createSuites();
            emitFrontEnd(providers, backend, graph, providers.getSuites().getDefaultGraphBuilderSuite(), OptimisticOptimizations.ALL, DefaultProfilingInfo.get(TriState.UNKNOWN), suites);
            LIRSuites lirSuites = createLIRSuites();
            emitBackEnd(graph, Stub.this, getInstalledCodeOwner(), backend, compResult, CompilationResultBuilderFactory.Default, getRegisterConfig(), lirSuites);
            assert checkStubInvariants(compResult);
        }
        catch (Throwable e)
        {
            throw debug.handle(e);
        }
        return compResult;
    }

    /**
     * Gets a {@link CompilationResult} that can be used for code generation. Required for AOT.
     */
    @SuppressWarnings("try")
    public CompilationResult getCompilationResult(DebugContext debug, final Backend backend)
    {
        try (DebugContext.Scope d = debug.scope("CompilingStub", providers.getCodeCache(), debugScopeContext()))
        {
            return buildCompilationResult(debug, backend);
        }
        catch (Throwable e)
        {
            throw debug.handle(e);
        }
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
        assert compResult.getExceptionHandlers().isEmpty() : this;

        // Stubs cannot be recompiled so they cannot be compiled with
        // assumptions and there is no point in recording evol_method dependencies
        assert compResult.getAssumptions() == null : "stubs should not use assumptions: " + this;

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

            assert !(data.reference instanceof ConstantReference) : this + " cannot have embedded object or metadata constant: " + data.reference;
        }
        for (Infopoint infopoint : compResult.getInfopoints())
        {
            assert infopoint instanceof Call : this + " cannot have non-call infopoint: " + infopoint;
            Call call = (Call) infopoint;
            assert call.target instanceof HotSpotForeignCallLinkage : this + " cannot have non runtime call: " + call.target;
            HotSpotForeignCallLinkage callLinkage = (HotSpotForeignCallLinkage) call.target;
            assert !callLinkage.isCompiledStub() || callLinkage.getDescriptor().equals(UNCOMMON_TRAP_HANDLER) : this + " cannot call compiled stub " + callLinkage;
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
        LIRSuites lirSuites = new LIRSuites(providers.getSuites().getDefaultLIRSuites(options));
        ListIterator<LIRPhase<PostAllocationOptimizationContext>> moveProfiling = lirSuites.getPostAllocationOptimizationStage().findPhase(MoveProfilingPhase.class);
        if (moveProfiling != null)
        {
            moveProfiling.remove();
        }
        return lirSuites;
    }
}
