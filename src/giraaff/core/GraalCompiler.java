package giraaff.core;

import java.util.Collection;

import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.code.CompilationResult;
import giraaff.core.LIRGenerationPhase.LIRGenerationContext;
import giraaff.core.common.PermanentBailoutException;
import giraaff.core.common.alloc.ComputeBlockOrder;
import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.target.Backend;
import giraaff.lir.LIR;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.asm.CompilationResultBuilderFactory;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.LIRSuites;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.LowTierContext;
import giraaff.phases.tiers.MidTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.TargetProvider;
import giraaff.phases.util.Providers;
import giraaff.util.GraalError;

/**
 * Static methods for orchestrating the compilation of a {@linkplain StructuredGraph graph}.
 */
// @class GraalCompiler
public final class GraalCompiler
{
    // @cons
    private GraalCompiler()
    {
        super();
    }

    /**
     * Encapsulates all the inputs to a {@linkplain GraalCompiler#compile(Request) compilation}.
     */
    // @class GraalCompiler.Request
    public static final class Request
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
        public final CompilationResult compilationResult;
        public final CompilationResultBuilderFactory factory;

        /**
         * @param graph the graph to be compiled
         * @param installedCodeOwner the method the compiled code will be associated with once installed. This argument can be null.
         */
        // @cons
        public Request(StructuredGraph graph, ResolvedJavaMethod installedCodeOwner, Providers providers, Backend backend, PhaseSuite<HighTierContext> graphBuilderSuite, OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo, Suites suites, LIRSuites lirSuites, CompilationResult compilationResult, CompilationResultBuilderFactory factory)
        {
            super();
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
    }

    /**
     * Requests compilation of a given graph.
     *
     * @param graph the graph to be compiled
     * @param installedCodeOwner the method the compiled code will be associated with once
     *            installed. This argument can be null.
     * @return the result of the compilation
     */
    public static CompilationResult compileGraph(StructuredGraph graph, ResolvedJavaMethod installedCodeOwner, Providers providers, Backend backend, PhaseSuite<HighTierContext> graphBuilderSuite, OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo, Suites suites, LIRSuites lirSuites, CompilationResult compilationResult, CompilationResultBuilderFactory factory)
    {
        return compile(new Request(graph, installedCodeOwner, providers, backend, graphBuilderSuite, optimisticOpts, profilingInfo, suites, lirSuites, compilationResult, factory));
    }

    /**
     * Services a given compilation request.
     *
     * @return the result of the compilation
     */
    public static CompilationResult compile(Request r)
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

    public static void emitBackEnd(StructuredGraph graph, Object stub, ResolvedJavaMethod installedCodeOwner, Backend backend, CompilationResult compilationResult, CompilationResultBuilderFactory factory, RegisterConfig registerConfig, LIRSuites lirSuites)
    {
        LIRGenerationResult lirGen = emitLIR(backend, graph, stub, registerConfig, lirSuites);
        compilationResult.setHasUnsafeAccess(graph.hasUnsafeAccess());
        emitCode(backend, graph.getAssumptions(), graph.method(), graph.getMethods(), graph.getFields(), (graph.method() != null) ? graph.getBytecodeSize() : 0, lirGen, compilationResult, installedCodeOwner, factory);
    }

    public static LIRGenerationResult emitLIR(Backend backend, StructuredGraph graph, Object stub, RegisterConfig registerConfig, LIRSuites lirSuites)
    {
        try
        {
            return emitLIR0(backend, graph, stub, registerConfig, lirSuites);
        }
        catch (/*OutOfRegistersException*/ PermanentBailoutException e)
        {
            throw new GraalError(e);
        }
    }

    private static LIRGenerationResult emitLIR0(Backend backend, StructuredGraph graph, Object stub, RegisterConfig registerConfig, LIRSuites lirSuites)
    {
        ScheduleResult schedule = graph.getLastSchedule();
        Block[] blocks = schedule.getCFG().getBlocks();
        Block startBlock = schedule.getCFG().getStartBlock();

        AbstractBlockBase<?>[] codeEmittingOrder = ComputeBlockOrder.computeCodeEmittingOrder(blocks.length, startBlock);
        AbstractBlockBase<?>[] linearScanOrder = ComputeBlockOrder.computeLinearScanOrder(blocks.length, startBlock);
        LIR lir = new LIR(schedule.getCFG(), linearScanOrder, codeEmittingOrder, graph.getOptions());

        FrameMapBuilder frameMapBuilder = backend.newFrameMapBuilder(registerConfig);
        LIRGenerationResult lirGenRes = backend.newLIRGenerationResult(lir, frameMapBuilder, graph, stub);
        LIRGeneratorTool lirGen = backend.newLIRGenerator(lirGenRes);
        NodeLIRBuilderTool nodeLirGen = backend.newNodeLIRBuilder(graph, lirGen);

        // LIR generation
        LIRGenerationContext context = new LIRGenerationContext(lirGen, nodeLirGen, graph, schedule);
        new LIRGenerationPhase().apply(backend.getTarget(), lirGenRes, context);

        return emitLowLevel(backend.getTarget(), lirGenRes, lirGen, lirSuites, backend.newRegisterAllocationConfig(registerConfig));
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
