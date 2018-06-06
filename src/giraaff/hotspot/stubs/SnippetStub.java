package giraaff.hotspot.stubs;

import java.lang.reflect.Method;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.api.replacements.Snippet.NonNullParameter;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.type.StampFactory;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.java.GraphBuilderPhase;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.spi.LoweringTool;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.phases.tiers.PhaseContext;
import giraaff.replacements.ConstantBindingParameterPlugin;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.Snippets;
import giraaff.util.GraalError;

///
// Base class for a stub defined by a snippet.
///
// @class SnippetStub
public abstract class SnippetStub extends Stub implements Snippets
{
    // @field
    protected final ResolvedJavaMethod ___method;

    ///
    // Creates a new snippet stub.
    //
    // @param snippetMethodName name of the single {@link Snippet} annotated method in the class of this object
    // @param linkage linkage details for a call to the stub
    ///
    // @cons SnippetStub
    public SnippetStub(String __snippetMethodName, HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        this(null, __snippetMethodName, __providers, __linkage);
    }

    ///
    // Creates a new snippet stub.
    //
    // @param snippetDeclaringClass this class in which the {@link Snippet} annotated method is
    //            declared. If {@code null}, this the class of this object is used.
    // @param snippetMethodName name of the single {@link Snippet} annotated method in
    //            {@code snippetDeclaringClass}
    // @param linkage linkage details for a call to the stub
    ///
    // @cons SnippetStub
    public SnippetStub(Class<? extends Snippets> __snippetDeclaringClass, String __snippetMethodName, HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super(__providers, __linkage);
        Method __javaMethod = SnippetTemplate.AbstractTemplates.findMethod(__snippetDeclaringClass == null ? getClass() : __snippetDeclaringClass, __snippetMethodName, null);
        this.___method = __providers.getMetaAccess().lookupJavaMethod(__javaMethod);
    }

    @Override
    protected StructuredGraph getStubGraph()
    {
        GraphBuilderConfiguration.Plugins __defaultPlugins = this.___providers.getGraphBuilderPlugins();
        MetaAccessProvider __metaAccess = this.___providers.getMetaAccess();
        SnippetReflectionProvider __snippetReflection = this.___providers.getSnippetReflection();

        GraphBuilderConfiguration.Plugins __plugins = new GraphBuilderConfiguration.Plugins(__defaultPlugins);
        __plugins.prependParameterPlugin(new ConstantBindingParameterPlugin(makeConstArgs(), __metaAccess, __snippetReflection));
        GraphBuilderConfiguration __config = GraphBuilderConfiguration.getSnippetDefault(__plugins);

        // Stubs cannot have optimistic assumptions, since they have to be valid for the entire run of the VM.
        final StructuredGraph __graph = new StructuredGraph.GraphBuilder().method(this.___method).build();
        __graph.disableUnsafeAccessTracking();

        IntrinsicContext __initialIntrinsicContext = new IntrinsicContext(this.___method, this.___method, getReplacementsBytecodeProvider(), IntrinsicContext.CompilationContext.INLINE_AFTER_PARSING);
        GraphBuilderPhase.GraphBuilderInstance __instance = new GraphBuilderPhase.GraphBuilderInstance(__metaAccess, this.___providers.getStampProvider(), this.___providers.getConstantReflection(), this.___providers.getConstantFieldProvider(), __config, OptimisticOptimizations.NONE, __initialIntrinsicContext);
        __instance.apply(__graph);

        for (ParameterNode __param : __graph.getNodes(ParameterNode.TYPE))
        {
            int __index = __param.index();
            if (this.___method.getParameterAnnotation(Snippet.NonNullParameter.class, __index) != null)
            {
                __param.setStamp(__param.stamp(NodeView.DEFAULT).join(StampFactory.objectNonNull()));
            }
        }

        new RemoveValueProxyPhase().apply(__graph);
        __graph.setGuardsStage(StructuredGraph.GuardsStage.FLOATING_GUARDS);
        CanonicalizerPhase __canonicalizer = new CanonicalizerPhase();
        PhaseContext __context = new PhaseContext(this.___providers);
        __canonicalizer.apply(__graph, __context);
        new LoweringPhase(__canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER).apply(__graph, __context);

        return __graph;
    }

    protected BytecodeProvider getReplacementsBytecodeProvider()
    {
        return this.___providers.getReplacements().getDefaultReplacementBytecodeProvider();
    }

    protected Object[] makeConstArgs()
    {
        int __count = this.___method.getSignature().getParameterCount(false);
        Object[] __args = new Object[__count];
        for (int __i = 0; __i < __args.length; __i++)
        {
            if (this.___method.getParameterAnnotation(Snippet.ConstantParameter.class, __i) != null)
            {
                __args[__i] = getConstantParameterValue(__i, null);
            }
        }
        return __args;
    }

    protected Object getConstantParameterValue(int __index, String __name)
    {
        throw new GraalError("%s must override getConstantParameterValue() to provide a value for parameter %d%s", getClass().getName(), __index, __name == null ? "" : " (" + __name + ")");
    }

    @Override
    public ResolvedJavaMethod getInstalledCodeOwner()
    {
        return this.___method;
    }
}
