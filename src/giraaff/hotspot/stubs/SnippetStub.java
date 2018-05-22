package giraaff.hotspot.stubs;

import java.lang.reflect.Method;

import jdk.vm.ci.meta.Local;
import jdk.vm.ci.meta.LocalVariableTable;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.api.replacements.Snippet.NonNullParameter;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.CompilationIdentifier;
import giraaff.core.common.type.StampFactory;
import giraaff.debug.GraalError;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.java.GraphBuilderPhase;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.GuardsStage;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.IntrinsicContext.CompilationContext;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.phases.tiers.PhaseContext;
import giraaff.replacements.ConstantBindingParameterPlugin;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.Snippets;

/**
 * Base class for a stub defined by a snippet.
 */
public abstract class SnippetStub extends Stub implements Snippets
{
    protected final ResolvedJavaMethod method;

    /**
     * Creates a new snippet stub.
     *
     * @param snippetMethodName name of the single {@link Snippet} annotated method in the class of
     *            this object
     * @param linkage linkage details for a call to the stub
     */
    public SnippetStub(String snippetMethodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        this(null, snippetMethodName, options, providers, linkage);
    }

    /**
     * Creates a new snippet stub.
     *
     * @param snippetDeclaringClass this class in which the {@link Snippet} annotated method is
     *            declared. If {@code null}, this the class of this object is used.
     * @param snippetMethodName name of the single {@link Snippet} annotated method in
     *            {@code snippetDeclaringClass}
     * @param linkage linkage details for a call to the stub
     */
    public SnippetStub(Class<? extends Snippets> snippetDeclaringClass, String snippetMethodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super(options, providers, linkage);
        Method javaMethod = SnippetTemplate.AbstractTemplates.findMethod(snippetDeclaringClass == null ? getClass() : snippetDeclaringClass, snippetMethodName, null);
        this.method = providers.getMetaAccess().lookupJavaMethod(javaMethod);
    }

    @Override
    protected StructuredGraph getGraph(CompilationIdentifier compilationId)
    {
        Plugins defaultPlugins = providers.getGraphBuilderPlugins();
        MetaAccessProvider metaAccess = providers.getMetaAccess();
        SnippetReflectionProvider snippetReflection = providers.getSnippetReflection();

        Plugins plugins = new Plugins(defaultPlugins);
        plugins.prependParameterPlugin(new ConstantBindingParameterPlugin(makeConstArgs(), metaAccess, snippetReflection));
        GraphBuilderConfiguration config = GraphBuilderConfiguration.getSnippetDefault(plugins);

        // Stubs cannot have optimistic assumptions since they have
        // to be valid for the entire run of the VM.
        final StructuredGraph graph = new StructuredGraph.Builder(options).method(method).compilationId(compilationId).build();
        graph.disableUnsafeAccessTracking();

        IntrinsicContext initialIntrinsicContext = new IntrinsicContext(method, method, getReplacementsBytecodeProvider(), CompilationContext.INLINE_AFTER_PARSING);
        GraphBuilderPhase.Instance instance = new GraphBuilderPhase.Instance(metaAccess, providers.getStampProvider(), providers.getConstantReflection(), providers.getConstantFieldProvider(), config, OptimisticOptimizations.NONE, initialIntrinsicContext);
        instance.apply(graph);

        for (ParameterNode param : graph.getNodes(ParameterNode.TYPE))
        {
            int index = param.index();
            if (method.getParameterAnnotation(NonNullParameter.class, index) != null)
            {
                param.setStamp(param.stamp(NodeView.DEFAULT).join(StampFactory.objectNonNull()));
            }
        }

        new RemoveValueProxyPhase().apply(graph);
        graph.setGuardsStage(GuardsStage.FLOATING_GUARDS);
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        PhaseContext context = new PhaseContext(providers);
        canonicalizer.apply(graph, context);
        new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER).apply(graph, context);

        return graph;
    }

    protected BytecodeProvider getReplacementsBytecodeProvider()
    {
        return providers.getReplacements().getDefaultReplacementBytecodeProvider();
    }

    protected boolean checkConstArg(int index, String expectedName)
    {
        LocalVariableTable lvt = method.getLocalVariableTable();
        if (lvt != null)
        {
            Local local = lvt.getLocal(index, 0);
            String actualName = local.getName();
        }
        return true;
    }

    protected Object[] makeConstArgs()
    {
        int count = method.getSignature().getParameterCount(false);
        Object[] args = new Object[count];
        for (int i = 0; i < args.length; i++)
        {
            if (method.getParameterAnnotation(ConstantParameter.class, i) != null)
            {
                args[i] = getConstantParameterValue(i, null);
            }
        }
        return args;
    }

    protected Object getConstantParameterValue(int index, String name)
    {
        throw new GraalError("%s must override getConstantParameterValue() to provide a value for parameter %d%s", getClass().getName(), index, name == null ? "" : " (" + name + ")");
    }

    @Override
    public ResolvedJavaMethod getInstalledCodeOwner()
    {
        return method;
    }

    @Override
    public String toString()
    {
        return "Stub<" + getInstalledCodeOwner().format("%h.%n") + ">";
    }
}
