package giraaff.core;

import java.util.Collection;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.code.CompilationResult;
import giraaff.core.LIRGenerationPhase.LIRGenerationContext;
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
        // @field
        public final StructuredGraph graph;
        // @field
        public final ResolvedJavaMethod installedCodeOwner;
        // @field
        public final Providers providers;
        // @field
        public final Backend backend;
        // @field
        public final PhaseSuite<HighTierContext> graphBuilderSuite;
        // @field
        public final OptimisticOptimizations optimisticOpts;
        // @field
        public final ProfilingInfo profilingInfo;
        // @field
        public final Suites suites;
        // @field
        public final LIRSuites lirSuites;
        // @field
        public final CompilationResult compilationResult;
        // @field
        public final CompilationResultBuilderFactory factory;

        /**
         * @param graph the graph to be compiled
         * @param installedCodeOwner the method the compiled code will be associated with once installed. This argument can be null.
         */
        // @cons
        public Request(StructuredGraph __graph, ResolvedJavaMethod __installedCodeOwner, Providers __providers, Backend __backend, PhaseSuite<HighTierContext> __graphBuilderSuite, OptimisticOptimizations __optimisticOpts, ProfilingInfo __profilingInfo, Suites __suites, LIRSuites __lirSuites, CompilationResult __compilationResult, CompilationResultBuilderFactory __factory)
        {
            super();
            this.graph = __graph;
            this.installedCodeOwner = __installedCodeOwner;
            this.providers = __providers;
            this.backend = __backend;
            this.graphBuilderSuite = __graphBuilderSuite;
            this.optimisticOpts = __optimisticOpts;
            this.profilingInfo = __profilingInfo;
            this.suites = __suites;
            this.lirSuites = __lirSuites;
            this.compilationResult = __compilationResult;
            this.factory = __factory;
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
    public static CompilationResult compileGraph(StructuredGraph __graph, ResolvedJavaMethod __installedCodeOwner, Providers __providers, Backend __backend, PhaseSuite<HighTierContext> __graphBuilderSuite, OptimisticOptimizations __optimisticOpts, ProfilingInfo __profilingInfo, Suites __suites, LIRSuites __lirSuites, CompilationResult __compilationResult, CompilationResultBuilderFactory __factory)
    {
        return compile(new Request(__graph, __installedCodeOwner, __providers, __backend, __graphBuilderSuite, __optimisticOpts, __profilingInfo, __suites, __lirSuites, __compilationResult, __factory));
    }

    /**
     * Services a given compilation request.
     *
     * @return the result of the compilation
     */
    public static CompilationResult compile(Request __r)
    {
        emitFrontEnd(__r.providers, __r.backend, __r.graph, __r.graphBuilderSuite, __r.optimisticOpts, __r.profilingInfo, __r.suites);
        emitBackEnd(__r.graph, null, __r.installedCodeOwner, __r.backend, __r.compilationResult, __r.factory, null, __r.lirSuites);
        return __r.compilationResult;
    }

    /**
     * Builds the graph, optimizes it.
     */
    public static void emitFrontEnd(Providers __providers, TargetProvider __target, StructuredGraph __graph, PhaseSuite<HighTierContext> __graphBuilderSuite, OptimisticOptimizations __optimisticOpts, ProfilingInfo __profilingInfo, Suites __suites)
    {
        HighTierContext __highTierContext = new HighTierContext(__providers, __graphBuilderSuite, __optimisticOpts);
        if (__graph.start().next() == null)
        {
            __graphBuilderSuite.apply(__graph, __highTierContext);
            new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Optional).apply(__graph);
        }

        __suites.getHighTier().apply(__graph, __highTierContext);
        __graph.maybeCompress();

        MidTierContext __midTierContext = new MidTierContext(__providers, __target, __optimisticOpts, __profilingInfo);
        __suites.getMidTier().apply(__graph, __midTierContext);
        __graph.maybeCompress();

        LowTierContext __lowTierContext = new LowTierContext(__providers, __target);
        __suites.getLowTier().apply(__graph, __lowTierContext);
    }

    public static void emitBackEnd(StructuredGraph __graph, Object __stub, ResolvedJavaMethod __installedCodeOwner, Backend __backend, CompilationResult __compilationResult, CompilationResultBuilderFactory __factory, RegisterConfig __registerConfig, LIRSuites __lirSuites)
    {
        LIRGenerationResult __lirGen = emitLIR(__backend, __graph, __stub, __registerConfig, __lirSuites);
        __compilationResult.setHasUnsafeAccess(__graph.hasUnsafeAccess());
        emitCode(__backend, __graph.getAssumptions(), __graph.method(), __graph.getMethods(), __graph.getFields(), (__graph.method() != null) ? __graph.getBytecodeSize() : 0, __lirGen, __compilationResult, __installedCodeOwner, __factory);
    }

    public static LIRGenerationResult emitLIR(Backend __backend, StructuredGraph __graph, Object __stub, RegisterConfig __registerConfig, LIRSuites __lirSuites)
    {
        try
        {
            return emitLIR0(__backend, __graph, __stub, __registerConfig, __lirSuites);
        }
        catch (/*OutOfRegistersException*/ BailoutException __e)
        {
            throw new GraalError(__e);
        }
    }

    private static LIRGenerationResult emitLIR0(Backend __backend, StructuredGraph __graph, Object __stub, RegisterConfig __registerConfig, LIRSuites __lirSuites)
    {
        ScheduleResult __schedule = __graph.getLastSchedule();
        Block[] __blocks = __schedule.getCFG().getBlocks();
        Block __startBlock = __schedule.getCFG().getStartBlock();

        AbstractBlockBase<?>[] __codeEmittingOrder = ComputeBlockOrder.computeCodeEmittingOrder(__blocks.length, __startBlock);
        AbstractBlockBase<?>[] __linearScanOrder = ComputeBlockOrder.computeLinearScanOrder(__blocks.length, __startBlock);
        LIR __lir = new LIR(__schedule.getCFG(), __linearScanOrder, __codeEmittingOrder);

        FrameMapBuilder __frameMapBuilder = __backend.newFrameMapBuilder(__registerConfig);
        LIRGenerationResult __lirGenRes = __backend.newLIRGenerationResult(__lir, __frameMapBuilder, __graph, __stub);
        LIRGeneratorTool __lirGen = __backend.newLIRGenerator(__lirGenRes);
        NodeLIRBuilderTool __nodeLirGen = __backend.newNodeLIRBuilder(__graph, __lirGen);

        // LIR generation
        LIRGenerationContext __context = new LIRGenerationContext(__lirGen, __nodeLirGen, __graph, __schedule);
        new LIRGenerationPhase().apply(__backend.getTarget(), __lirGenRes, __context);

        return emitLowLevel(__backend.getTarget(), __lirGenRes, __lirGen, __lirSuites, __backend.newRegisterAllocationConfig(__registerConfig));
    }

    public static LIRGenerationResult emitLowLevel(TargetDescription __target, LIRGenerationResult __lirGenRes, LIRGeneratorTool __lirGen, LIRSuites __lirSuites, RegisterAllocationConfig __registerAllocationConfig)
    {
        PreAllocationOptimizationContext __preAllocOptContext = new PreAllocationOptimizationContext(__lirGen);
        __lirSuites.getPreAllocationOptimizationStage().apply(__target, __lirGenRes, __preAllocOptContext);

        AllocationContext __allocContext = new AllocationContext(__lirGen.getSpillMoveFactory(), __registerAllocationConfig);
        __lirSuites.getAllocationStage().apply(__target, __lirGenRes, __allocContext);

        PostAllocationOptimizationContext __postAllocOptContext = new PostAllocationOptimizationContext(__lirGen);
        __lirSuites.getPostAllocationOptimizationStage().apply(__target, __lirGenRes, __postAllocOptContext);

        return __lirGenRes;
    }

    public static void emitCode(Backend __backend, Assumptions __assumptions, ResolvedJavaMethod __rootMethod, Collection<ResolvedJavaMethod> __inlinedMethods, EconomicSet<ResolvedJavaField> __accessedFields, int __bytecodeSize, LIRGenerationResult __lirGenRes, CompilationResult __compilationResult, ResolvedJavaMethod __installedCodeOwner, CompilationResultBuilderFactory __factory)
    {
        FrameMap __frameMap = __lirGenRes.getFrameMap();
        CompilationResultBuilder __crb = __backend.newCompilationResultBuilder(__lirGenRes, __frameMap, __compilationResult, __factory);
        __backend.emitCode(__crb, __lirGenRes.getLIR(), __installedCodeOwner);
        if (__assumptions != null && !__assumptions.isEmpty())
        {
            __compilationResult.setAssumptions(__assumptions.toArray());
        }
        if (__rootMethod != null)
        {
            __compilationResult.setMethods(__rootMethod, __inlinedMethods);
            __compilationResult.setFields(__accessedFields);
            __compilationResult.setBytecodeSize(__bytecodeSize);
        }
        __crb.finish();
    }
}
