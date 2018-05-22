package graalvm.compiler.core;

import java.util.Collection;

import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.LIRGenerationPhase.LIRGenerationContext;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.alloc.ComputeBlockOrder;
import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.core.target.Backend;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.alloc.OutOfRegistersException;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.FrameMapBuilder;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import graalvm.compiler.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.LowTierContext;
import graalvm.compiler.phases.tiers.MidTierContext;
import graalvm.compiler.phases.tiers.Suites;
import graalvm.compiler.phases.tiers.TargetProvider;
import graalvm.compiler.phases.util.Providers;

/**
 * Static methods for orchestrating the compilation of a {@linkplain StructuredGraph graph}.
 */
public class GraalCompiler
{
    /**
     * Encapsulates all the inputs to a {@linkplain GraalCompiler#compile(Request) compilation}.
     */
    public static class Request<T extends CompilationResult>
    {
        public final StructuredGraph graph;
        public final ResolvedJavaMethod installedCodeOwner;
        public final Providers providers;
        public final Backend backend;
        public final PhaseSuite<HighTierContext> graphBuilderSuite;
        public final OptimisticOptimizations optimisticOpts;
        public final ProfilingInfo profilingInfo;
        public final Suites suites;
        public final LIRSuites lirSuites;
        public final T compilationResult;
        public final CompilationResultBuilderFactory factory;

        /**
         * @param graph the graph to be compiled
         * @param installedCodeOwner the method the compiled code will be associated with once
         *            installed. This argument can be null.
         */
        public Request(StructuredGraph graph, ResolvedJavaMethod installedCodeOwner, Providers providers, Backend backend, PhaseSuite<HighTierContext> graphBuilderSuite, OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo, Suites suites, LIRSuites lirSuites, T compilationResult, CompilationResultBuilderFactory factory)
        {
            this.graph = graph;
            this.installedCodeOwner = installedCodeOwner;
            this.providers = providers;
            this.backend = backend;
            this.graphBuilderSuite = graphBuilderSuite;
            this.optimisticOpts = optimisticOpts;
            this.profilingInfo = profilingInfo;
            this.suites = suites;
            this.lirSuites = lirSuites;
            this.compilationResult = compilationResult;
            this.factory = factory;
        }

        /**
         * Executes this compilation request.
         *
         * @return the result of the compilation
         */
        public T execute()
        {
            return GraalCompiler.compile(this);
        }
    }

    /**
     * Requests compilation of a given graph.
     *
     * @param graph the graph to be compiled
     * @param installedCodeOwner the method the compiled code will be associated with once
     *            installed. This argument can be null.
     * @return the result of the compilation
     */
    public static <T extends CompilationResult> T compileGraph(StructuredGraph graph, ResolvedJavaMethod installedCodeOwner, Providers providers, Backend backend, PhaseSuite<HighTierContext> graphBuilderSuite, OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo, Suites suites, LIRSuites lirSuites, T compilationResult, CompilationResultBuilderFactory factory)
    {
        return compile(new Request<>(graph, installedCodeOwner, providers, backend, graphBuilderSuite, optimisticOpts, profilingInfo, suites, lirSuites, compilationResult, factory));
    }

    /**
     * Services a given compilation request.
     *
     * @return the result of the compilation
     */
    public static <T extends CompilationResult> T compile(Request<T> r)
    {
        emitFrontEnd(r.providers, r.backend, r.graph, r.graphBuilderSuite, r.optimisticOpts, r.profilingInfo, r.suites);
        emitBackEnd(r.graph, null, r.installedCodeOwner, r.backend, r.compilationResult, r.factory, null, r.lirSuites);
        return r.compilationResult;
    }

    /**
     * Builds the graph, optimizes it.
     */
    public static void emitFrontEnd(Providers providers, TargetProvider target, StructuredGraph graph, PhaseSuite<HighTierContext> graphBuilderSuite, OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo, Suites suites)
    {
        HighTierContext highTierContext = new HighTierContext(providers, graphBuilderSuite, optimisticOpts);
        if (graph.start().next() == null)
        {
            graphBuilderSuite.apply(graph, highTierContext);
            new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Optional).apply(graph);
        }

        suites.getHighTier().apply(graph, highTierContext);
        graph.maybeCompress();

        MidTierContext midTierContext = new MidTierContext(providers, target, optimisticOpts, profilingInfo);
        suites.getMidTier().apply(graph, midTierContext);
        graph.maybeCompress();

        LowTierContext lowTierContext = new LowTierContext(providers, target);
        suites.getLowTier().apply(graph, lowTierContext);
    }

    public static <T extends CompilationResult> void emitBackEnd(StructuredGraph graph, Object stub, ResolvedJavaMethod installedCodeOwner, Backend backend, T compilationResult, CompilationResultBuilderFactory factory, RegisterConfig registerConfig, LIRSuites lirSuites)
    {
        LIRGenerationResult lirGen = null;
        lirGen = emitLIR(backend, graph, stub, registerConfig, lirSuites);
        int bytecodeSize = graph.method() == null ? 0 : graph.getBytecodeSize();
        compilationResult.setHasUnsafeAccess(graph.hasUnsafeAccess());
        emitCode(backend, graph.getAssumptions(), graph.method(), graph.getMethods(), graph.getFields(), bytecodeSize, lirGen, compilationResult, installedCodeOwner, factory);
    }

    public static LIRGenerationResult emitLIR(Backend backend, StructuredGraph graph, Object stub, RegisterConfig registerConfig, LIRSuites lirSuites)
    {
        String registerPressure = GraalOptions.RegisterPressure.getValue(graph.getOptions());
        String[] allocationRestrictedTo = registerPressure == null ? null : registerPressure.split(",");
        try
        {
            return emitLIR0(backend, graph, stub, registerConfig, lirSuites, allocationRestrictedTo);
        }
        catch (OutOfRegistersException e)
        {
            if (allocationRestrictedTo != null)
            {
                allocationRestrictedTo = null;
                return emitLIR0(backend, graph, stub, registerConfig, lirSuites, allocationRestrictedTo);
            }
            /* If the re-execution fails we convert the exception into a "hard" failure */
            throw new GraalError(e);
        }
    }

    private static LIRGenerationResult emitLIR0(Backend backend, StructuredGraph graph, Object stub, RegisterConfig registerConfig, LIRSuites lirSuites, String[] allocationRestrictedTo)
    {
        ScheduleResult schedule = graph.getLastSchedule();
        Block[] blocks = schedule.getCFG().getBlocks();
        Block startBlock = schedule.getCFG().getStartBlock();

        AbstractBlockBase<?>[] codeEmittingOrder = ComputeBlockOrder.computeCodeEmittingOrder(blocks.length, startBlock);
        AbstractBlockBase<?>[] linearScanOrder = ComputeBlockOrder.computeLinearScanOrder(blocks.length, startBlock);
        LIR lir = new LIR(schedule.getCFG(), linearScanOrder, codeEmittingOrder, graph.getOptions());

        FrameMapBuilder frameMapBuilder = backend.newFrameMapBuilder(registerConfig);
        LIRGenerationResult lirGenRes = backend.newLIRGenerationResult(graph.compilationId(), lir, frameMapBuilder, graph, stub);
        LIRGeneratorTool lirGen = backend.newLIRGenerator(lirGenRes);
        NodeLIRBuilderTool nodeLirGen = backend.newNodeLIRBuilder(graph, lirGen);

        // LIR generation
        LIRGenerationContext context = new LIRGenerationContext(lirGen, nodeLirGen, graph, schedule);
        new LIRGenerationPhase().apply(backend.getTarget(), lirGenRes, context);

        return emitLowLevel(backend.getTarget(), lirGenRes, lirGen, lirSuites, backend.newRegisterAllocationConfig(registerConfig, allocationRestrictedTo));
    }

    protected static <T extends CompilationResult> String getCompilationUnitName(StructuredGraph graph, T compilationResult)
    {
        if (compilationResult != null && compilationResult.getName() != null)
        {
            return compilationResult.getName();
        }
        ResolvedJavaMethod method = graph.method();
        if (method == null)
        {
            return "<unknown>";
        }
        return method.format("%H.%n(%p)");
    }

    public static LIRGenerationResult emitLowLevel(TargetDescription target, LIRGenerationResult lirGenRes, LIRGeneratorTool lirGen, LIRSuites lirSuites, RegisterAllocationConfig registerAllocationConfig)
    {
        PreAllocationOptimizationContext preAllocOptContext = new PreAllocationOptimizationContext(lirGen);
        lirSuites.getPreAllocationOptimizationStage().apply(target, lirGenRes, preAllocOptContext);

        AllocationContext allocContext = new AllocationContext(lirGen.getSpillMoveFactory(), registerAllocationConfig);
        lirSuites.getAllocationStage().apply(target, lirGenRes, allocContext);

        PostAllocationOptimizationContext postAllocOptContext = new PostAllocationOptimizationContext(lirGen);
        lirSuites.getPostAllocationOptimizationStage().apply(target, lirGenRes, postAllocOptContext);

        return lirGenRes;
    }

    public static void emitCode(Backend backend, Assumptions assumptions, ResolvedJavaMethod rootMethod, Collection<ResolvedJavaMethod> inlinedMethods, EconomicSet<ResolvedJavaField> accessedFields, int bytecodeSize, LIRGenerationResult lirGenRes, CompilationResult compilationResult, ResolvedJavaMethod installedCodeOwner, CompilationResultBuilderFactory factory)
    {
        FrameMap frameMap = lirGenRes.getFrameMap();
        CompilationResultBuilder crb = backend.newCompilationResultBuilder(lirGenRes, frameMap, compilationResult, factory);
        backend.emitCode(crb, lirGenRes.getLIR(), installedCodeOwner);
        if (assumptions != null && !assumptions.isEmpty())
        {
            compilationResult.setAssumptions(assumptions.toArray());
        }
        if (rootMethod != null)
        {
            compilationResult.setMethods(rootMethod, inlinedMethods);
            compilationResult.setFields(accessedFields);
            compilationResult.setBytecodeSize(bytecodeSize);
        }
        crb.finish();
    }
}
