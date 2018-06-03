package giraaff.java;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.spi.StampProvider;
import giraaff.phases.BasePhase;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.Phase;
import giraaff.phases.tiers.HighTierContext;

/**
 * Parses the bytecodes of a method and builds the IR graph.
 */
// @class GraphBuilderPhase
public final class GraphBuilderPhase extends BasePhase<HighTierContext>
{
    // @field
    private final GraphBuilderConfiguration graphBuilderConfig;

    // @cons
    public GraphBuilderPhase(GraphBuilderConfiguration __config)
    {
        super();
        this.graphBuilderConfig = __config;
    }

    @Override
    protected void run(StructuredGraph __graph, HighTierContext __context)
    {
        new Instance(__context.getMetaAccess(), __context.getStampProvider(), __context.getConstantReflection(), __context.getConstantFieldProvider(), graphBuilderConfig, __context.getOptimisticOptimizations(), null).run(__graph);
    }

    public GraphBuilderConfiguration getGraphBuilderConfig()
    {
        return graphBuilderConfig;
    }

    // @class GraphBuilderPhase.Instance
    public static final class Instance extends Phase
    {
        // @field
        protected final MetaAccessProvider metaAccess;
        // @field
        protected final StampProvider stampProvider;
        // @field
        protected final ConstantReflectionProvider constantReflection;
        // @field
        protected final ConstantFieldProvider constantFieldProvider;
        // @field
        protected final GraphBuilderConfiguration graphBuilderConfig;
        // @field
        protected final OptimisticOptimizations optimisticOpts;
        // @field
        private final IntrinsicContext initialIntrinsicContext;

        // @cons
        public Instance(MetaAccessProvider __metaAccess, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, GraphBuilderConfiguration __graphBuilderConfig, OptimisticOptimizations __optimisticOpts, IntrinsicContext __initialIntrinsicContext)
        {
            super();
            this.graphBuilderConfig = __graphBuilderConfig;
            this.optimisticOpts = __optimisticOpts;
            this.metaAccess = __metaAccess;
            this.stampProvider = __stampProvider;
            this.constantReflection = __constantReflection;
            this.constantFieldProvider = __constantFieldProvider;
            this.initialIntrinsicContext = __initialIntrinsicContext;
        }

        @Override
        protected void run(StructuredGraph __graph)
        {
            createBytecodeParser(__graph, null, __graph.method(), __graph.getEntryBCI(), initialIntrinsicContext).buildRootMethod();
        }

        // Hook for subclasses of Instance to provide a subclass of BytecodeParser.
        protected BytecodeParser createBytecodeParser(StructuredGraph __graph, BytecodeParser __parent, ResolvedJavaMethod __method, int __entryBCI, IntrinsicContext __intrinsicContext)
        {
            return new BytecodeParser(this, __graph, __parent, __method, __entryBCI, __intrinsicContext);
        }
    }
}
