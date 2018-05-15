package graalvm.compiler.hotspot.stubs;

import static graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext.CompilationContext.INLINE_AFTER_PARSING;

import java.lang.reflect.Method;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.api.replacements.Snippet.NonNullParameter;
import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.bytecode.BytecodeProvider;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.java.GraphBuilderPhase;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ParameterNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.GuardsStage;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.common.RemoveValueProxyPhase;
import graalvm.compiler.phases.tiers.PhaseContext;
import graalvm.compiler.replacements.ConstantBindingParameterPlugin;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.Snippets;

import jdk.vm.ci.meta.Local;
import jdk.vm.ci.meta.LocalVariableTable;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Base class for a stub defined by a snippet.
 */
public abstract class SnippetStub extends Stub implements Snippets {

    protected final ResolvedJavaMethod method;

    /**
     * Creates a new snippet stub.
     *
     * @param snippetMethodName name of the single {@link Snippet} annotated method in the class of
     *            this object
     * @param linkage linkage details for a call to the stub
     */
    public SnippetStub(String snippetMethodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage) {
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
    public SnippetStub(Class<? extends Snippets> snippetDeclaringClass, String snippetMethodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage) {
        super(options, providers, linkage);
        Method javaMethod = SnippetTemplate.AbstractTemplates.findMethod(snippetDeclaringClass == null ? getClass() : snippetDeclaringClass, snippetMethodName, null);
        this.method = providers.getMetaAccess().lookupJavaMethod(javaMethod);
    }

    @Override
    @SuppressWarnings("try")
    protected StructuredGraph getGraph(DebugContext debug, CompilationIdentifier compilationId) {
        Plugins defaultPlugins = providers.getGraphBuilderPlugins();
        MetaAccessProvider metaAccess = providers.getMetaAccess();
        SnippetReflectionProvider snippetReflection = providers.getSnippetReflection();

        Plugins plugins = new Plugins(defaultPlugins);
        plugins.prependParameterPlugin(new ConstantBindingParameterPlugin(makeConstArgs(), metaAccess, snippetReflection));
        GraphBuilderConfiguration config = GraphBuilderConfiguration.getSnippetDefault(plugins);

        // Stubs cannot have optimistic assumptions since they have
        // to be valid for the entire run of the VM.
        final StructuredGraph graph = new StructuredGraph.Builder(options, debug).method(method).compilationId(compilationId).build();
        try (DebugContext.Scope outer = debug.scope("SnippetStub", graph)) {
            graph.disableUnsafeAccessTracking();

            IntrinsicContext initialIntrinsicContext = new IntrinsicContext(method, method, getReplacementsBytecodeProvider(), INLINE_AFTER_PARSING);
            GraphBuilderPhase.Instance instance = new GraphBuilderPhase.Instance(metaAccess, providers.getStampProvider(),
                            providers.getConstantReflection(), providers.getConstantFieldProvider(),
                            config, OptimisticOptimizations.NONE,
                            initialIntrinsicContext);
            instance.apply(graph);

            for (ParameterNode param : graph.getNodes(ParameterNode.TYPE)) {
                int index = param.index();
                if (method.getParameterAnnotation(NonNullParameter.class, index) != null) {
                    param.setStamp(param.stamp(NodeView.DEFAULT).join(StampFactory.objectNonNull()));
                }
            }

            new RemoveValueProxyPhase().apply(graph);
            graph.setGuardsStage(GuardsStage.FLOATING_GUARDS);
            CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
            PhaseContext context = new PhaseContext(providers);
            canonicalizer.apply(graph, context);
            new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER).apply(graph, context);
        } catch (Throwable e) {
            throw debug.handle(e);
        }

        return graph;
    }

    protected BytecodeProvider getReplacementsBytecodeProvider() {
        return providers.getReplacements().getDefaultReplacementBytecodeProvider();
    }

    protected boolean checkConstArg(int index, String expectedName) {
        assert method.getParameterAnnotation(ConstantParameter.class, index) != null : String.format("parameter %d of %s is expected to be constant", index, method.format("%H.%n(%p)"));
        LocalVariableTable lvt = method.getLocalVariableTable();
        if (lvt != null) {
            Local local = lvt.getLocal(index, 0);
            assert local != null;
            String actualName = local.getName();
            assert actualName.equals(expectedName) : String.format("parameter %d of %s is expected to be named %s, not %s", index, method.format("%H.%n(%p)"), expectedName, actualName);
        }
        return true;
    }

    protected Object[] makeConstArgs() {
        int count = method.getSignature().getParameterCount(false);
        Object[] args = new Object[count];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameterAnnotation(ConstantParameter.class, i) != null) {
                args[i] = getConstantParameterValue(i, null);
            }
        }
        return args;
    }

    protected Object getConstantParameterValue(int index, String name) {
        throw new GraalError("%s must override getConstantParameterValue() to provide a value for parameter %d%s", getClass().getName(), index, name == null ? "" : " (" + name + ")");
    }

    @Override
    protected Object debugScopeContext() {
        return getInstalledCodeOwner();
    }

    @Override
    public ResolvedJavaMethod getInstalledCodeOwner() {
        return method;
    }

    @Override
    public String toString() {
        return "Stub<" + getInstalledCodeOwner().format("%h.%n") + ">";
    }
}