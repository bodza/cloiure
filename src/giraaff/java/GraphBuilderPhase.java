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

///
// Parses the bytecodes of a method and builds the IR graph.
///
// @class GraphBuilderPhase
public final class GraphBuilderPhase extends BasePhase<HighTierContext>
{
    // @field
    private final GraphBuilderConfiguration ___graphBuilderConfig;

    // @cons
    public GraphBuilderPhase(GraphBuilderConfiguration __config)
    {
        super();
        this.___graphBuilderConfig = __config;
    }

    @Override
    protected void run(StructuredGraph __graph, HighTierContext __context)
    {
        new Instance(__context.getMetaAccess(), __context.getStampProvider(), __context.getConstantReflection(), __context.getConstantFieldProvider(), this.___graphBuilderConfig, __context.getOptimisticOptimizations(), null).run(__graph);
    }

    public GraphBuilderConfiguration getGraphBuilderConfig()
    {
        return this.___graphBuilderConfig;
    }

    // @class GraphBuilderPhase.Instance
    public static final class Instance extends Phase
    {
        // @field
        protected final MetaAccessProvider ___metaAccess;
        // @field
        protected final StampProvider ___stampProvider;
        // @field
        protected final ConstantReflectionProvider ___constantReflection;
        // @field
        protected final ConstantFieldProvider ___constantFieldProvider;
        // @field
        protected final GraphBuilderConfiguration ___graphBuilderConfig;
        // @field
        protected final OptimisticOptimizations ___optimisticOpts;
        // @field
        private final IntrinsicContext ___initialIntrinsicContext;

        // @cons
        public Instance(MetaAccessProvider __metaAccess, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, GraphBuilderConfiguration __graphBuilderConfig, OptimisticOptimizations __optimisticOpts, IntrinsicContext __initialIntrinsicContext)
        {
            super();
            this.___graphBuilderConfig = __graphBuilderConfig;
            this.___optimisticOpts = __optimisticOpts;
            this.___metaAccess = __metaAccess;
            this.___stampProvider = __stampProvider;
            this.___constantReflection = __constantReflection;
            this.___constantFieldProvider = __constantFieldProvider;
            this.___initialIntrinsicContext = __initialIntrinsicContext;
        }

        @Override
        protected void run(StructuredGraph __graph)
        {
            createBytecodeParser(__graph, null, __graph.method(), __graph.getEntryBCI(), this.___initialIntrinsicContext).buildRootMethod();
        }

        // Hook for subclasses of Instance to provide a subclass of BytecodeParser.
        protected BytecodeParser createBytecodeParser(StructuredGraph __graph, BytecodeParser __parent, ResolvedJavaMethod __method, int __entryBCI, IntrinsicContext __intrinsicContext)
        {
            return new BytecodeParser(this, __graph, __parent, __method, __entryBCI, __intrinsicContext);
        }
    }
}
