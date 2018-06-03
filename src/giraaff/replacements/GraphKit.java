package giraaff.replacements;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Signature;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.graph.Graph;
import giraaff.graph.Node.ValueNumberable;
import giraaff.java.FrameStateBuilder;
import giraaff.java.GraphBuilderPhase;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.KillingBeginNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.UnwindNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.graphbuilderconf.GraphBuilderTool;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.IntrinsicContext.CompilationContext;
import giraaff.nodes.java.ExceptionObjectNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.StampProvider;
import giraaff.nodes.type.StampTool;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.util.Providers;
import giraaff.util.GraalError;
import giraaff.word.WordTypes;

/**
 * A utility for manually creating a graph. This will be expanded as necessary to support all
 * subsystems that employ manual graph creation (as opposed to {@linkplain GraphBuilderPhase
 * bytecode parsing} based graph creation).
 */
// @class GraphKit
public final class GraphKit implements GraphBuilderTool
{
    // @field
    protected final Providers providers;
    // @field
    protected final StructuredGraph graph;
    // @field
    protected final WordTypes wordTypes;
    // @field
    protected final GraphBuilderConfiguration.Plugins graphBuilderPlugins;
    // @field
    protected FixedWithNextNode lastFixedNode;

    // @field
    private final List<Structure> structures;

    // @class GraphKit.Structure
    protected abstract static class Structure
    {
    }

    // @cons
    public GraphKit(ResolvedJavaMethod __stubMethod, Providers __providers, WordTypes __wordTypes, Plugins __graphBuilderPlugins)
    {
        super();
        this.providers = __providers;
        this.graph = new StructuredGraph.Builder().method(__stubMethod).build();
        this.graph.disableUnsafeAccessTracking();
        this.wordTypes = __wordTypes;
        this.graphBuilderPlugins = __graphBuilderPlugins;
        this.lastFixedNode = graph.start();

        this.structures = new ArrayList<>();
        // Add a dummy element, so that the access of the last element never leads to an exception.
        this.structures.add(new Structure() {});
    }

    @Override
    public StructuredGraph getGraph()
    {
        return graph;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return providers.getConstantReflection();
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider()
    {
        return providers.getConstantFieldProvider();
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return providers.getMetaAccess();
    }

    @Override
    public StampProvider getStampProvider()
    {
        return providers.getStampProvider();
    }

    @Override
    public boolean parsingIntrinsic()
    {
        return true;
    }

    /**
     * Ensures a floating node is added to or already present in the graph via {@link Graph#unique}.
     *
     * @return a node similar to {@code node} if one exists, otherwise {@code node}
     */
    public <T extends FloatingNode & ValueNumberable> T unique(T __node)
    {
        return graph.unique(changeToWord(__node));
    }

    public <T extends ValueNode> T add(T __node)
    {
        return graph.add(changeToWord(__node));
    }

    public <T extends ValueNode> T changeToWord(T __node)
    {
        if (wordTypes != null && wordTypes.isWord(__node))
        {
            __node.setStamp(wordTypes.getWordStamp(StampTool.typeOrNull(__node)));
        }
        return __node;
    }

    @Override
    public <T extends ValueNode> T append(T __node)
    {
        T __result = graph.addOrUniqueWithInputs(changeToWord(__node));
        if (__result instanceof FixedNode)
        {
            updateLastFixed((FixedNode) __result);
        }
        return __result;
    }

    private void updateLastFixed(FixedNode __result)
    {
        graph.addAfterFixed(lastFixedNode, __result);
        if (__result instanceof FixedWithNextNode)
        {
            lastFixedNode = (FixedWithNextNode) __result;
        }
        else
        {
            lastFixedNode = null;
        }
    }

    public InvokeNode createInvoke(Class<?> __declaringClass, String __name, ValueNode... __args)
    {
        return createInvoke(__declaringClass, __name, InvokeKind.Static, null, BytecodeFrame.UNKNOWN_BCI, __args);
    }

    /**
     * Creates and appends an {@link InvokeNode} for a call to a given method with a given set of
     * arguments. The method is looked up via reflection based on the declaring class and name.
     *
     * @param declaringClass the class declaring the invoked method
     * @param name the name of the invoked method
     * @param args the arguments to the invocation
     */
    public InvokeNode createInvoke(Class<?> __declaringClass, String __name, InvokeKind __invokeKind, FrameStateBuilder __frameStateBuilder, int __bci, ValueNode... __args)
    {
        boolean __isStatic = __invokeKind == InvokeKind.Static;
        ResolvedJavaMethod __method = findMethod(__declaringClass, __name, __isStatic);
        return createInvoke(__method, __invokeKind, __frameStateBuilder, __bci, __args);
    }

    public ResolvedJavaMethod findMethod(Class<?> __declaringClass, String __name, boolean __isStatic)
    {
        ResolvedJavaMethod __method = null;
        for (Method __m : __declaringClass.getDeclaredMethods())
        {
            if (Modifier.isStatic(__m.getModifiers()) == __isStatic && __m.getName().equals(__name))
            {
                __method = providers.getMetaAccess().lookupJavaMethod(__m);
            }
        }
        GraalError.guarantee(__method != null, "Could not find %s.%s (%s)", __declaringClass, __name, __isStatic ? "static" : "non-static");
        return __method;
    }

    public ResolvedJavaMethod findMethod(Class<?> __declaringClass, String __name, Class<?>... __parameterTypes)
    {
        try
        {
            Method __m = __declaringClass.getDeclaredMethod(__name, __parameterTypes);
            return providers.getMetaAccess().lookupJavaMethod(__m);
        }
        catch (NoSuchMethodException | SecurityException __e)
        {
            throw new AssertionError(__e);
        }
    }

    /**
     * Creates and appends an {@link InvokeNode} for a call to a given method with a given set of arguments.
     */
    public InvokeNode createInvoke(ResolvedJavaMethod __method, InvokeKind __invokeKind, FrameStateBuilder __frameStateBuilder, int __bci, ValueNode... __args)
    {
        Signature __signature = __method.getSignature();
        JavaType __returnType = __signature.getReturnType(null);
        StampPair __returnStamp = graphBuilderPlugins.getOverridingStamp(this, __returnType, false);
        if (__returnStamp == null)
        {
            __returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), __returnType, false);
        }
        MethodCallTargetNode __callTarget = graph.add(createMethodCallTarget(__invokeKind, __method, __args, __returnStamp, __bci));
        InvokeNode __invoke = append(new InvokeNode(__callTarget, __bci));

        if (__frameStateBuilder != null)
        {
            if (__invoke.getStackKind() != JavaKind.Void)
            {
                __frameStateBuilder.push(__invoke.getStackKind(), __invoke);
            }
            __invoke.setStateAfter(__frameStateBuilder.create(__bci, __invoke));
            if (__invoke.getStackKind() != JavaKind.Void)
            {
                __frameStateBuilder.pop(__invoke.getStackKind());
            }
        }
        return __invoke;
    }

    public InvokeWithExceptionNode createInvokeWithExceptionAndUnwind(ResolvedJavaMethod __method, InvokeKind __invokeKind, FrameStateBuilder __frameStateBuilder, int __invokeBci, int __exceptionEdgeBci, ValueNode... __args)
    {
        InvokeWithExceptionNode __result = startInvokeWithException(__method, __invokeKind, __frameStateBuilder, __invokeBci, __exceptionEdgeBci, __args);
        exceptionPart();
        ExceptionObjectNode __exception = exceptionObject();
        append(new UnwindNode(__exception));
        endInvokeWithException();
        return __result;
    }

    public InvokeWithExceptionNode createInvokeWithExceptionAndUnwind(MethodCallTargetNode __callTarget, FrameStateBuilder __frameStateBuilder, int __invokeBci, int __exceptionEdgeBci)
    {
        InvokeWithExceptionNode __result = startInvokeWithException(__callTarget, __frameStateBuilder, __invokeBci, __exceptionEdgeBci);
        exceptionPart();
        ExceptionObjectNode __exception = exceptionObject();
        append(new UnwindNode(__exception));
        endInvokeWithException();
        return __result;
    }

    protected MethodCallTargetNode createMethodCallTarget(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __args, StampPair __returnStamp, @SuppressWarnings("unused") int __bci)
    {
        return new MethodCallTargetNode(__invokeKind, __targetMethod, __args, __returnStamp, null);
    }

    protected final JavaKind asKind(JavaType __type)
    {
        return wordTypes != null ? wordTypes.asKind(__type) : __type.getJavaKind();
    }

    /**
     * Determines if a given set of arguments is compatible with the signature of a given method.
     *
     * @return true if {@code args} are compatible with the signature of {@code method}
     * @throws AssertionError if {@code args} are not compatible with the signature of {@code method}
     */
    public boolean checkArgs(ResolvedJavaMethod __method, ValueNode... __args)
    {
        Signature __signature = __method.getSignature();
        boolean __isStatic = __method.isStatic();
        if (__signature.getParameterCount(!__isStatic) != __args.length)
        {
            throw new AssertionError(graph + ": wrong number of arguments to " + __method);
        }
        int __argIndex = 0;
        if (!__isStatic)
        {
            JavaKind __expected = asKind(__method.getDeclaringClass());
            JavaKind __actual = __args[__argIndex++].stamp(NodeView.DEFAULT).getStackKind();
        }
        for (int __i = 0; __i != __signature.getParameterCount(false); __i++)
        {
            JavaKind __expected = asKind(__signature.getParameterType(__i, __method.getDeclaringClass())).getStackKind();
            JavaKind __actual = __args[__argIndex++].stamp(NodeView.DEFAULT).getStackKind();
            if (__expected != __actual)
            {
                throw new AssertionError(graph + ": wrong kind of value for argument " + __i + " of call to " + __method + " [" + __actual + " != " + __expected + "]");
            }
        }
        return true;
    }

    /**
     * Recursively {@linkplain #inline inlines} all invocations currently in the graph.
     */
    public void inlineInvokes(String __reason, String __phase)
    {
        while (!graph.getNodes().filter(InvokeNode.class).isEmpty())
        {
            for (InvokeNode __invoke : graph.getNodes().filter(InvokeNode.class).snapshot())
            {
                inline(__invoke, __reason, __phase);
            }
        }

        // Clean up all code that is now dead after inlining.
        new DeadCodeEliminationPhase().apply(graph);
    }

    /**
     * Inlines a given invocation to a method. The graph of the inlined method is processed in the
     * same manner as for snippets and method substitutions.
     */
    public void inline(InvokeNode __invoke, String __reason, String __phase)
    {
        ResolvedJavaMethod __method = ((MethodCallTargetNode) __invoke.callTarget()).targetMethod();

        MetaAccessProvider __metaAccess = providers.getMetaAccess();
        Plugins __plugins = new Plugins(graphBuilderPlugins);
        GraphBuilderConfiguration __config = GraphBuilderConfiguration.getSnippetDefault(__plugins);

        StructuredGraph __calleeGraph = new StructuredGraph.Builder().method(__method).build();
        IntrinsicContext __initialReplacementContext = new IntrinsicContext(__method, __method, providers.getReplacements().getDefaultReplacementBytecodeProvider(), CompilationContext.INLINE_AFTER_PARSING);
        GraphBuilderPhase.Instance __instance = createGraphBuilderInstance(__metaAccess, providers.getStampProvider(), providers.getConstantReflection(), providers.getConstantFieldProvider(), __config, OptimisticOptimizations.NONE, __initialReplacementContext);
        __instance.apply(__calleeGraph);

        // Remove all frame states from inlinee.
        __calleeGraph.clearAllStateAfter();
        new DeadCodeEliminationPhase(Optionality.Required).apply(__calleeGraph);

        InliningUtil.inline(__invoke, __calleeGraph, false, __method, __reason, __phase);
    }

    protected GraphBuilderPhase.Instance createGraphBuilderInstance(MetaAccessProvider __metaAccess, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, GraphBuilderConfiguration __graphBuilderConfig, OptimisticOptimizations __optimisticOpts, IntrinsicContext __initialIntrinsicContext)
    {
        return new GraphBuilderPhase.Instance(__metaAccess, __stampProvider, __constantReflection, __constantFieldProvider, __graphBuilderConfig, __optimisticOpts, __initialIntrinsicContext);
    }

    protected void pushStructure(Structure __structure)
    {
        structures.add(__structure);
    }

    protected <T extends Structure> T getTopStructure(Class<T> __expectedClass)
    {
        return __expectedClass.cast(structures.get(structures.size() - 1));
    }

    protected void popStructure()
    {
        structures.remove(structures.size() - 1);
    }

    // @enum GraphKit.IfState
    protected enum IfState
    {
        CONDITION,
        THEN_PART,
        ELSE_PART,
        FINISHED
    }

    // @class GraphKit.IfStructure
    static final class IfStructure extends Structure
    {
        // @field
        protected IfState state;
        // @field
        protected FixedNode thenPart;
        // @field
        protected FixedNode elsePart;
    }

    /**
     * Starts an if-block. This call can be followed by a call to {@link #thenPart} to start
     * emitting the code executed when the condition hold; and a call to {@link #elsePart} to start
     * emititng the code when the condition does not hold. It must be followed by a call to
     * {@link #endIf} to close the if-block.
     *
     * @param condition The condition for the if-block
     * @param trueProbability The estimated probability the condition is true
     * @return the created {@link IfNode}.
     */
    public IfNode startIf(LogicNode __condition, double __trueProbability)
    {
        AbstractBeginNode __thenSuccessor = graph.add(new BeginNode());
        AbstractBeginNode __elseSuccessor = graph.add(new BeginNode());
        IfNode __node = append(new IfNode(__condition, __thenSuccessor, __elseSuccessor, __trueProbability));
        lastFixedNode = null;

        IfStructure __s = new IfStructure();
        __s.state = IfState.CONDITION;
        __s.thenPart = __thenSuccessor;
        __s.elsePart = __elseSuccessor;
        pushStructure(__s);
        return __node;
    }

    private IfStructure saveLastIfNode()
    {
        IfStructure __s = getTopStructure(IfStructure.class);
        switch (__s.state)
        {
            case CONDITION:
                break;
            case THEN_PART:
                __s.thenPart = lastFixedNode;
                break;
            case ELSE_PART:
                __s.elsePart = lastFixedNode;
                break;
            case FINISHED:
                break;
        }
        lastFixedNode = null;
        return __s;
    }

    public void thenPart()
    {
        IfStructure __s = saveLastIfNode();
        lastFixedNode = (FixedWithNextNode) __s.thenPart;
        __s.state = IfState.THEN_PART;
    }

    public void elsePart()
    {
        IfStructure __s = saveLastIfNode();
        lastFixedNode = (FixedWithNextNode) __s.elsePart;
        __s.state = IfState.ELSE_PART;
    }

    /**
     * Ends an if block started with {@link #startIf(LogicNode, double)}.
     *
     * @return the created merge node, or {@code null} if no merge node was required (for example,
     *         when one part ended with a control sink).
     */
    public AbstractMergeNode endIf()
    {
        IfStructure __s = saveLastIfNode();

        FixedWithNextNode __thenPart = __s.thenPart instanceof FixedWithNextNode ? (FixedWithNextNode) __s.thenPart : null;
        FixedWithNextNode __elsePart = __s.elsePart instanceof FixedWithNextNode ? (FixedWithNextNode) __s.elsePart : null;
        AbstractMergeNode __merge = null;

        if (__thenPart != null && __elsePart != null)
        {
            // Both parts are alive, we need a real merge.
            EndNode __thenEnd = graph.add(new EndNode());
            graph.addAfterFixed(__thenPart, __thenEnd);
            EndNode __elseEnd = graph.add(new EndNode());
            graph.addAfterFixed(__elsePart, __elseEnd);

            __merge = graph.add(new MergeNode());
            __merge.addForwardEnd(__thenEnd);
            __merge.addForwardEnd(__elseEnd);

            lastFixedNode = __merge;
        }
        else if (__thenPart != null)
        {
            // elsePart ended with a control sink, so we can continue with thenPart.
            lastFixedNode = __thenPart;
        }
        else if (__elsePart != null)
        {
            // thenPart ended with a control sink, so we can continue with elsePart.
            lastFixedNode = __elsePart;
        }
        else
        {
            // Both parts ended with a control sink, so no nodes can be added after the if.
        }
        __s.state = IfState.FINISHED;
        popStructure();
        return __merge;
    }

    // @class GraphKit.InvokeWithExceptionStructure
    static final class InvokeWithExceptionStructure extends Structure
    {
        // @enum GraphKit.InvokeWithExceptionStructure.State
        protected enum State
        {
            INVOKE,
            NO_EXCEPTION_EDGE,
            EXCEPTION_EDGE,
            FINISHED
        }

        // @field
        protected State state;
        // @field
        protected ExceptionObjectNode exceptionObject;
        // @field
        protected FixedNode noExceptionEdge;
        // @field
        protected FixedNode exceptionEdge;
    }

    public InvokeWithExceptionNode startInvokeWithException(ResolvedJavaMethod __method, InvokeKind __invokeKind, FrameStateBuilder __frameStateBuilder, int __invokeBci, int __exceptionEdgeBci, ValueNode... __args)
    {
        Signature __signature = __method.getSignature();
        JavaType __returnType = __signature.getReturnType(null);
        StampPair __returnStamp = graphBuilderPlugins.getOverridingStamp(this, __returnType, false);
        if (__returnStamp == null)
        {
            __returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), __returnType, false);
        }
        MethodCallTargetNode __callTarget = graph.add(createMethodCallTarget(__invokeKind, __method, __args, __returnStamp, __invokeBci));
        return startInvokeWithException(__callTarget, __frameStateBuilder, __invokeBci, __exceptionEdgeBci);
    }

    public InvokeWithExceptionNode startInvokeWithException(MethodCallTargetNode __callTarget, FrameStateBuilder __frameStateBuilder, int __invokeBci, int __exceptionEdgeBci)
    {
        ExceptionObjectNode __exceptionObject = add(new ExceptionObjectNode(getMetaAccess()));
        if (__frameStateBuilder != null)
        {
            FrameStateBuilder __exceptionState = __frameStateBuilder.copy();
            __exceptionState.clearStack();
            __exceptionState.push(JavaKind.Object, __exceptionObject);
            __exceptionState.setRethrowException(false);
            __exceptionObject.setStateAfter(__exceptionState.create(__exceptionEdgeBci, __exceptionObject));
        }
        InvokeWithExceptionNode __invoke = append(new InvokeWithExceptionNode(__callTarget, __exceptionObject, __invokeBci));
        AbstractBeginNode __noExceptionEdge = graph.add(KillingBeginNode.create(LocationIdentity.any()));
        __invoke.setNext(__noExceptionEdge);
        if (__frameStateBuilder != null)
        {
            if (__invoke.getStackKind() != JavaKind.Void)
            {
                __frameStateBuilder.push(__invoke.getStackKind(), __invoke);
            }
            __invoke.setStateAfter(__frameStateBuilder.create(__invokeBci, __invoke));
            if (__invoke.getStackKind() != JavaKind.Void)
            {
                __frameStateBuilder.pop(__invoke.getStackKind());
            }
        }
        lastFixedNode = null;

        InvokeWithExceptionStructure __s = new InvokeWithExceptionStructure();
        __s.state = InvokeWithExceptionStructure.State.INVOKE;
        __s.noExceptionEdge = __noExceptionEdge;
        __s.exceptionEdge = __exceptionObject;
        __s.exceptionObject = __exceptionObject;
        pushStructure(__s);

        return __invoke;
    }

    private InvokeWithExceptionStructure saveLastInvokeWithExceptionNode()
    {
        InvokeWithExceptionStructure __s = getTopStructure(InvokeWithExceptionStructure.class);
        switch (__s.state)
        {
            case INVOKE:
                break;
            case NO_EXCEPTION_EDGE:
                __s.noExceptionEdge = lastFixedNode;
                break;
            case EXCEPTION_EDGE:
                __s.exceptionEdge = lastFixedNode;
                break;
            case FINISHED:
                break;
        }
        lastFixedNode = null;
        return __s;
    }

    public void noExceptionPart()
    {
        InvokeWithExceptionStructure __s = saveLastInvokeWithExceptionNode();
        lastFixedNode = (FixedWithNextNode) __s.noExceptionEdge;
        __s.state = InvokeWithExceptionStructure.State.NO_EXCEPTION_EDGE;
    }

    public void exceptionPart()
    {
        InvokeWithExceptionStructure __s = saveLastInvokeWithExceptionNode();
        lastFixedNode = (FixedWithNextNode) __s.exceptionEdge;
        __s.state = InvokeWithExceptionStructure.State.EXCEPTION_EDGE;
    }

    public ExceptionObjectNode exceptionObject()
    {
        InvokeWithExceptionStructure __s = getTopStructure(InvokeWithExceptionStructure.class);
        return __s.exceptionObject;
    }

    /**
     * Finishes a control flow started with {@link #startInvokeWithException}. If necessary, creates
     * a merge of the non-exception and exception edges. The merge node is returned and the
     * non-exception edge is the first forward end of the merge, the exception edge is the second
     * forward end (relevant for phi nodes).
     */
    public AbstractMergeNode endInvokeWithException()
    {
        InvokeWithExceptionStructure __s = saveLastInvokeWithExceptionNode();
        FixedWithNextNode __noExceptionEdge = __s.noExceptionEdge instanceof FixedWithNextNode ? (FixedWithNextNode) __s.noExceptionEdge : null;
        FixedWithNextNode __exceptionEdge = __s.exceptionEdge instanceof FixedWithNextNode ? (FixedWithNextNode) __s.exceptionEdge : null;
        AbstractMergeNode __merge = null;
        if (__noExceptionEdge != null && __exceptionEdge != null)
        {
            EndNode __noExceptionEnd = graph.add(new EndNode());
            graph.addAfterFixed(__noExceptionEdge, __noExceptionEnd);
            EndNode __exceptionEnd = graph.add(new EndNode());
            graph.addAfterFixed(__exceptionEdge, __exceptionEnd);
            __merge = graph.add(new MergeNode());
            __merge.addForwardEnd(__noExceptionEnd);
            __merge.addForwardEnd(__exceptionEnd);
            lastFixedNode = __merge;
        }
        else if (__noExceptionEdge != null)
        {
            lastFixedNode = __noExceptionEdge;
        }
        else if (__exceptionEdge != null)
        {
            lastFixedNode = __exceptionEdge;
        }
        __s.state = InvokeWithExceptionStructure.State.FINISHED;
        popStructure();
        return __merge;
    }
}
