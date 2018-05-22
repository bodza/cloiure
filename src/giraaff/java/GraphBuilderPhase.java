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
import giraaff.phases.tiers.HighTierContext;

/**
 * Parses the bytecodes of a method and builds the IR graph.
 */
public class GraphBuilderPhase extends BasePhase<HighTierContext>
{
    private final GraphBuilderConfiguration graphBuilderConfig;

    public GraphBuilderPhase(GraphBuilderConfiguration config)
    {
        this.graphBuilderConfig = config;
    }

    @Override
    protected void run(StructuredGraph graph, HighTierContext context)
    {
        new Instance(context.getMetaAccess(), context.getStampProvider(), context.getConstantReflection(), context.getConstantFieldProvider(), graphBuilderConfig, context.getOptimisticOptimizations(), null).run(graph);
    }

    public GraphBuilderConfiguration getGraphBuilderConfig()
    {
        return graphBuilderConfig;
    }

    // Fully qualified name is a workaround for JDK-8056066
    public static class Instance extends giraaff.phases.Phase
    {
        protected final MetaAccessProvider metaAccess;
        protected final StampProvider stampProvider;
        protected final ConstantReflectionProvider constantReflection;
        protected final ConstantFieldProvider constantFieldProvider;
        protected final GraphBuilderConfiguration graphBuilderConfig;
        protected final OptimisticOptimizations optimisticOpts;
        private final IntrinsicContext initialIntrinsicContext;

        public Instance(MetaAccessProvider metaAccess, StampProvider stampProvider, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, GraphBuilderConfiguration graphBuilderConfig, OptimisticOptimizations optimisticOpts, IntrinsicContext initialIntrinsicContext)
        {
            this.graphBuilderConfig = graphBuilderConfig;
            this.optimisticOpts = optimisticOpts;
            this.metaAccess = metaAccess;
            this.stampProvider = stampProvider;
            this.constantReflection = constantReflection;
            this.constantFieldProvider = constantFieldProvider;
            this.initialIntrinsicContext = initialIntrinsicContext;
        }

        @Override
        protected void run(StructuredGraph graph)
        {
            createBytecodeParser(graph, null, graph.method(), graph.getEntryBCI(), initialIntrinsicContext).buildRootMethod();
        }

        /* Hook for subclasses of Instance to provide a subclass of BytecodeParser. */
        protected BytecodeParser createBytecodeParser(StructuredGraph graph, BytecodeParser parent, ResolvedJavaMethod method, int entryBCI, IntrinsicContext intrinsicContext)
        {
            return new BytecodeParser(this, graph, parent, method, entryBCI, intrinsicContext);
        }
    }
}
