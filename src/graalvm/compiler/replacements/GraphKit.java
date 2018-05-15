package graalvm.compiler.replacements;

import static graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext.CompilationContext.INLINE_AFTER_PARSING;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Node.ValueNumberable;
import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.java.FrameStateBuilder;
import graalvm.compiler.java.GraphBuilderPhase;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.BeginNode;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.EndNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.InvokeWithExceptionNode;
import graalvm.compiler.nodes.KillingBeginNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.MergeNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.UnwindNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderTool;
import graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext;
import graalvm.compiler.nodes.java.ExceptionObjectNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.spi.StampProvider;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase.Optionality;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.word.WordTypes;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Signature;

/**
 * A utility for manually creating a graph. This will be expanded as necessary to support all
 * subsystems that employ manual graph creation (as opposed to {@linkplain GraphBuilderPhase
 * bytecode parsing} based graph creation).
 */
public class GraphKit implements GraphBuilderTool {

    protected final Providers providers;
    protected final StructuredGraph graph;
    protected final WordTypes wordTypes;
    protected final GraphBuilderConfiguration.Plugins graphBuilderPlugins;
    protected FixedWithNextNode lastFixedNode;

    private final List<Structure> structures;

    protected abstract static class Structure {
    }

    public GraphKit(DebugContext debug, ResolvedJavaMethod stubMethod, Providers providers, WordTypes wordTypes, Plugins graphBuilderPlugins, CompilationIdentifier compilationId, String name) {
        this.providers = providers;
        StructuredGraph.Builder builder = new StructuredGraph.Builder(debug.getOptions(), debug).compilationId(compilationId);
        if (name != null) {
            builder.name(name);
        } else {
            builder.method(stubMethod);
        }
        this.graph = builder.build();
        graph.disableUnsafeAccessTracking();
        if (graph.trackNodeSourcePosition()) {
            // Set up a default value that everything constructed by GraphKit will use.
            graph.withNodeSourcePosition(NodeSourcePosition.substitution(stubMethod));
        }
        this.wordTypes = wordTypes;
        this.graphBuilderPlugins = graphBuilderPlugins;
        this.lastFixedNode = graph.start();

        structures = new ArrayList<>();
        /*
         * Add a dummy element, so that the access of the last element never leads to an exception.
         */
        structures.add(new Structure() {
        });
    }

    @Override
    public StructuredGraph getGraph() {
        return graph;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection() {
        return providers.getConstantReflection();
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider() {
        return providers.getConstantFieldProvider();
    }

    @Override
    public MetaAccessProvider getMetaAccess() {
        return providers.getMetaAccess();
    }

    @Override
    public StampProvider getStampProvider() {
        return providers.getStampProvider();
    }

    @Override
    public boolean parsingIntrinsic() {
        return true;
    }

    /**
     * Ensures a floating node is added to or already present in the graph via {@link Graph#unique}.
     *
     * @return a node similar to {@code node} if one exists, otherwise {@code node}
     */
    public <T extends FloatingNode & ValueNumberable> T unique(T node) {
        return graph.unique(changeToWord(node));
    }

    public <T extends ValueNode> T add(T node) {
        return graph.add(changeToWord(node));
    }

    public <T extends ValueNode> T changeToWord(T node) {
        if (wordTypes != null && wordTypes.isWord(node)) {
            node.setStamp(wordTypes.getWordStamp(StampTool.typeOrNull(node)));
        }
        return node;
    }

    @Override
    public <T extends ValueNode> T append(T node) {
        T result = graph.addOrUniqueWithInputs(changeToWord(node));
        if (result instanceof FixedNode) {
            updateLastFixed((FixedNode) result);
        }
        return result;
    }

    private void updateLastFixed(FixedNode result) {
        assert lastFixedNode != null;
        assert result.predecessor() == null;
        graph.addAfterFixed(lastFixedNode, result);
        if (result instanceof FixedWithNextNode) {
            lastFixedNode = (FixedWithNextNode) result;
        } else {
            lastFixedNode = null;
        }
    }

    public InvokeNode createInvoke(Class<?> declaringClass, String name, ValueNode... args) {
        return createInvoke(declaringClass, name, InvokeKind.Static, null, BytecodeFrame.UNKNOWN_BCI, args);
    }

    /**
     * Creates and appends an {@link InvokeNode} for a call to a given method with a given set of
     * arguments. The method is looked up via reflection based on the declaring class and name.
     *
     * @param declaringClass the class declaring the invoked method
     * @param name the name of the invoked method
     * @param args the arguments to the invocation
     */
    public InvokeNode createInvoke(Class<?> declaringClass, String name, InvokeKind invokeKind, FrameStateBuilder frameStateBuilder, int bci, ValueNode... args) {
        boolean isStatic = invokeKind == InvokeKind.Static;
        ResolvedJavaMethod method = findMethod(declaringClass, name, isStatic);
        return createInvoke(method, invokeKind, frameStateBuilder, bci, args);
    }

    public ResolvedJavaMethod findMethod(Class<?> declaringClass, String name, boolean isStatic) {
        ResolvedJavaMethod method = null;
        for (Method m : declaringClass.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) == isStatic && m.getName().equals(name)) {
                assert method == null : "found more than one method in " + declaringClass + " named " + name;
                method = providers.getMetaAccess().lookupJavaMethod(m);
            }
        }
        GraalError.guarantee(method != null, "Could not find %s.%s (%s)", declaringClass, name, isStatic ? "static" : "non-static");
        return method;
    }

    public ResolvedJavaMethod findMethod(Class<?> declaringClass, String name, Class<?>... parameterTypes) {
        try {
            Method m = declaringClass.getDeclaredMethod(name, parameterTypes);
            return providers.getMetaAccess().lookupJavaMethod(m);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Creates and appends an {@link InvokeNode} for a call to a given method with a given set of
     * arguments.
     */
    @SuppressWarnings("try")
    public InvokeNode createInvoke(ResolvedJavaMethod method, InvokeKind invokeKind, FrameStateBuilder frameStateBuilder, int bci, ValueNode... args) {
        try (DebugCloseable context = graph.withNodeSourcePosition(NodeSourcePosition.substitution(graph.currentNodeSourcePosition(), method))) {
            assert method.isStatic() == (invokeKind == InvokeKind.Static);
            Signature signature = method.getSignature();
            JavaType returnType = signature.getReturnType(null);
            assert checkArgs(method, args);
            StampPair returnStamp = graphBuilderPlugins.getOverridingStamp(this, returnType, false);
            if (returnStamp == null) {
                returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), returnType, false);
            }
            MethodCallTargetNode callTarget = graph.add(createMethodCallTarget(invokeKind, method, args, returnStamp, bci));
            InvokeNode invoke = append(new InvokeNode(callTarget, bci));

            if (frameStateBuilder != null) {
                if (invoke.getStackKind() != JavaKind.Void) {
                    frameStateBuilder.push(invoke.getStackKind(), invoke);
                }
                invoke.setStateAfter(frameStateBuilder.create(bci, invoke));
                if (invoke.getStackKind() != JavaKind.Void) {
                    frameStateBuilder.pop(invoke.getStackKind());
                }
            }
            return invoke;
        }
    }

    @SuppressWarnings("try")
    public InvokeWithExceptionNode createInvokeWithExceptionAndUnwind(ResolvedJavaMethod method, InvokeKind invokeKind,
                    FrameStateBuilder frameStateBuilder, int invokeBci, int exceptionEdgeBci, ValueNode... args) {
        try (DebugCloseable context = graph.withNodeSourcePosition(NodeSourcePosition.substitution(graph.currentNodeSourcePosition(), method))) {
            InvokeWithExceptionNode result = startInvokeWithException(method, invokeKind, frameStateBuilder, invokeBci, exceptionEdgeBci, args);
            exceptionPart();
            ExceptionObjectNode exception = exceptionObject();
            append(new UnwindNode(exception));
            endInvokeWithException();
            return result;
        }
    }

    @SuppressWarnings("try")
    public InvokeWithExceptionNode createInvokeWithExceptionAndUnwind(MethodCallTargetNode callTarget, FrameStateBuilder frameStateBuilder, int invokeBci, int exceptionEdgeBci) {
        try (DebugCloseable context = graph.withNodeSourcePosition(NodeSourcePosition.substitution(graph.currentNodeSourcePosition(), callTarget.targetMethod()))) {
            InvokeWithExceptionNode result = startInvokeWithException(callTarget, frameStateBuilder, invokeBci, exceptionEdgeBci);
            exceptionPart();
            ExceptionObjectNode exception = exceptionObject();
            append(new UnwindNode(exception));
            endInvokeWithException();
            return result;
        }
    }

    protected MethodCallTargetNode createMethodCallTarget(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] args, StampPair returnStamp, @SuppressWarnings("unused") int bci) {
        return new MethodCallTargetNode(invokeKind, targetMethod, args, returnStamp, null);
    }

    protected final JavaKind asKind(JavaType type) {
        return wordTypes != null ? wordTypes.asKind(type) : type.getJavaKind();
    }

    /**
     * Determines if a given set of arguments is compatible with the signature of a given method.
     *
     * @return true if {@code args} are compatible with the signature if {@code method}
     * @throws AssertionError if {@code args} are not compatible with the signature if
     *             {@code method}
     */
    public boolean checkArgs(ResolvedJavaMethod method, ValueNode... args) {
        Signature signature = method.getSignature();
        boolean isStatic = method.isStatic();
        if (signature.getParameterCount(!isStatic) != args.length) {
            throw new AssertionError(graph + ": wrong number of arguments to " + method);
        }
        int argIndex = 0;
        if (!isStatic) {
            JavaKind expected = asKind(method.getDeclaringClass());
            JavaKind actual = args[argIndex++].stamp(NodeView.DEFAULT).getStackKind();
            assert expected == actual : graph + ": wrong kind of value for receiver argument of call to " + method + " [" + actual + " != " + expected + "]";
        }
        for (int i = 0; i != signature.getParameterCount(false); i++) {
            JavaKind expected = asKind(signature.getParameterType(i, method.getDeclaringClass())).getStackKind();
            JavaKind actual = args[argIndex++].stamp(NodeView.DEFAULT).getStackKind();
            if (expected != actual) {
                throw new AssertionError(graph + ": wrong kind of value for argument " + i + " of call to " + method + " [" + actual + " != " + expected + "]");
            }
        }
        return true;
    }

    /**
     * Recursively {@linkplain #inline inlines} all invocations currently in the graph.
     */
    public void inlineInvokes(String reason, String phase) {
        while (!graph.getNodes().filter(InvokeNode.class).isEmpty()) {
            for (InvokeNode invoke : graph.getNodes().filter(InvokeNode.class).snapshot()) {
                inline(invoke, reason, phase);
            }
        }

        // Clean up all code that is now dead after inlining.
        new DeadCodeEliminationPhase().apply(graph);
    }

    /**
     * Inlines a given invocation to a method. The graph of the inlined method is processed in the
     * same manner as for snippets and method substitutions.
     */
    public void inline(InvokeNode invoke, String reason, String phase) {
        ResolvedJavaMethod method = ((MethodCallTargetNode) invoke.callTarget()).targetMethod();

        MetaAccessProvider metaAccess = providers.getMetaAccess();
        Plugins plugins = new Plugins(graphBuilderPlugins);
        GraphBuilderConfiguration config = GraphBuilderConfiguration.getSnippetDefault(plugins);

        StructuredGraph calleeGraph = new StructuredGraph.Builder(invoke.getOptions(), invoke.getDebug()).method(method).build();
        if (invoke.graph().trackNodeSourcePosition()) {
            calleeGraph.setTrackNodeSourcePosition();
        }
        IntrinsicContext initialReplacementContext = new IntrinsicContext(method, method, providers.getReplacements().getDefaultReplacementBytecodeProvider(), INLINE_AFTER_PARSING);
        GraphBuilderPhase.Instance instance = createGraphBuilderInstance(metaAccess, providers.getStampProvider(), providers.getConstantReflection(), providers.getConstantFieldProvider(), config,
                        OptimisticOptimizations.NONE,
                        initialReplacementContext);
        instance.apply(calleeGraph);

        // Remove all frame states from inlinee
        calleeGraph.clearAllStateAfter();
        new DeadCodeEliminationPhase(Optionality.Required).apply(calleeGraph);

        InliningUtil.inline(invoke, calleeGraph, false, method, reason, phase);
    }

    protected GraphBuilderPhase.Instance createGraphBuilderInstance(MetaAccessProvider metaAccess, StampProvider stampProvider, ConstantReflectionProvider constantReflection,
                    ConstantFieldProvider constantFieldProvider, GraphBuilderConfiguration graphBuilderConfig, OptimisticOptimizations optimisticOpts, IntrinsicContext initialIntrinsicContext) {
        return new GraphBuilderPhase.Instance(metaAccess, stampProvider, constantReflection, constantFieldProvider, graphBuilderConfig, optimisticOpts, initialIntrinsicContext);
    }

    protected void pushStructure(Structure structure) {
        structures.add(structure);
    }

    protected <T extends Structure> T getTopStructure(Class<T> expectedClass) {
        return expectedClass.cast(structures.get(structures.size() - 1));
    }

    protected void popStructure() {
        structures.remove(structures.size() - 1);
    }

    protected enum IfState {
        CONDITION,
        THEN_PART,
        ELSE_PART,
        FINISHED
    }

    static class IfStructure extends Structure {
        protected IfState state;
        protected FixedNode thenPart;
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
    public IfNode startIf(LogicNode condition, double trueProbability) {
        AbstractBeginNode thenSuccessor = graph.add(new BeginNode());
        AbstractBeginNode elseSuccessor = graph.add(new BeginNode());
        IfNode node = append(new IfNode(condition, thenSuccessor, elseSuccessor, trueProbability));
        lastFixedNode = null;

        IfStructure s = new IfStructure();
        s.state = IfState.CONDITION;
        s.thenPart = thenSuccessor;
        s.elsePart = elseSuccessor;
        pushStructure(s);
        return node;
    }

    private IfStructure saveLastIfNode() {
        IfStructure s = getTopStructure(IfStructure.class);
        switch (s.state) {
            case CONDITION:
                assert lastFixedNode == null;
                break;
            case THEN_PART:
                s.thenPart = lastFixedNode;
                break;
            case ELSE_PART:
                s.elsePart = lastFixedNode;
                break;
            case FINISHED:
                assert false;
                break;
        }
        lastFixedNode = null;
        return s;
    }

    public void thenPart() {
        IfStructure s = saveLastIfNode();
        lastFixedNode = (FixedWithNextNode) s.thenPart;
        s.state = IfState.THEN_PART;
    }

    public void elsePart() {
        IfStructure s = saveLastIfNode();
        lastFixedNode = (FixedWithNextNode) s.elsePart;
        s.state = IfState.ELSE_PART;
    }

    /**
     * Ends an if block started with {@link #startIf(LogicNode, double)}.
     *
     * @return the created merge node, or {@code null} if no merge node was required (for example,
     *         when one part ended with a control sink).
     */
    public AbstractMergeNode endIf() {
        IfStructure s = saveLastIfNode();

        FixedWithNextNode thenPart = s.thenPart instanceof FixedWithNextNode ? (FixedWithNextNode) s.thenPart : null;
        FixedWithNextNode elsePart = s.elsePart instanceof FixedWithNextNode ? (FixedWithNextNode) s.elsePart : null;
        AbstractMergeNode merge = null;

        if (thenPart != null && elsePart != null) {
            /* Both parts are alive, we need a real merge. */
            EndNode thenEnd = graph.add(new EndNode());
            graph.addAfterFixed(thenPart, thenEnd);
            EndNode elseEnd = graph.add(new EndNode());
            graph.addAfterFixed(elsePart, elseEnd);

            merge = graph.add(new MergeNode());
            merge.addForwardEnd(thenEnd);
            merge.addForwardEnd(elseEnd);

            lastFixedNode = merge;

        } else if (thenPart != null) {
            /* elsePart ended with a control sink, so we can continue with thenPart. */
            lastFixedNode = thenPart;

        } else if (elsePart != null) {
            /* thenPart ended with a control sink, so we can continue with elsePart. */
            lastFixedNode = elsePart;

        } else {
            /* Both parts ended with a control sink, so no nodes can be added after the if. */
            assert lastFixedNode == null;
        }
        s.state = IfState.FINISHED;
        popStructure();
        return merge;
    }

    static class InvokeWithExceptionStructure extends Structure {
        protected enum State {
            INVOKE,
            NO_EXCEPTION_EDGE,
            EXCEPTION_EDGE,
            FINISHED
        }

        protected State state;
        protected ExceptionObjectNode exceptionObject;
        protected FixedNode noExceptionEdge;
        protected FixedNode exceptionEdge;
    }

    public InvokeWithExceptionNode startInvokeWithException(ResolvedJavaMethod method, InvokeKind invokeKind,
                    FrameStateBuilder frameStateBuilder, int invokeBci, int exceptionEdgeBci, ValueNode... args) {

        assert method.isStatic() == (invokeKind == InvokeKind.Static);
        Signature signature = method.getSignature();
        JavaType returnType = signature.getReturnType(null);
        assert checkArgs(method, args);
        StampPair returnStamp = graphBuilderPlugins.getOverridingStamp(this, returnType, false);
        if (returnStamp == null) {
            returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), returnType, false);
        }
        MethodCallTargetNode callTarget = graph.add(createMethodCallTarget(invokeKind, method, args, returnStamp, invokeBci));
        return startInvokeWithException(callTarget, frameStateBuilder, invokeBci, exceptionEdgeBci);
    }

    public InvokeWithExceptionNode startInvokeWithException(MethodCallTargetNode callTarget, FrameStateBuilder frameStateBuilder, int invokeBci, int exceptionEdgeBci) {
        ExceptionObjectNode exceptionObject = add(new ExceptionObjectNode(getMetaAccess()));
        if (frameStateBuilder != null) {
            FrameStateBuilder exceptionState = frameStateBuilder.copy();
            exceptionState.clearStack();
            exceptionState.push(JavaKind.Object, exceptionObject);
            exceptionState.setRethrowException(false);
            exceptionObject.setStateAfter(exceptionState.create(exceptionEdgeBci, exceptionObject));
        }
        InvokeWithExceptionNode invoke = append(new InvokeWithExceptionNode(callTarget, exceptionObject, invokeBci));
        AbstractBeginNode noExceptionEdge = graph.add(KillingBeginNode.create(LocationIdentity.any()));
        invoke.setNext(noExceptionEdge);
        if (frameStateBuilder != null) {
            if (invoke.getStackKind() != JavaKind.Void) {
                frameStateBuilder.push(invoke.getStackKind(), invoke);
            }
            invoke.setStateAfter(frameStateBuilder.create(invokeBci, invoke));
            if (invoke.getStackKind() != JavaKind.Void) {
                frameStateBuilder.pop(invoke.getStackKind());
            }
        }
        lastFixedNode = null;

        InvokeWithExceptionStructure s = new InvokeWithExceptionStructure();
        s.state = InvokeWithExceptionStructure.State.INVOKE;
        s.noExceptionEdge = noExceptionEdge;
        s.exceptionEdge = exceptionObject;
        s.exceptionObject = exceptionObject;
        pushStructure(s);

        return invoke;
    }

    private InvokeWithExceptionStructure saveLastInvokeWithExceptionNode() {
        InvokeWithExceptionStructure s = getTopStructure(InvokeWithExceptionStructure.class);
        switch (s.state) {
            case INVOKE:
                assert lastFixedNode == null;
                break;
            case NO_EXCEPTION_EDGE:
                s.noExceptionEdge = lastFixedNode;
                break;
            case EXCEPTION_EDGE:
                s.exceptionEdge = lastFixedNode;
                break;
            case FINISHED:
                assert false;
                break;
        }
        lastFixedNode = null;
        return s;
    }

    public void noExceptionPart() {
        InvokeWithExceptionStructure s = saveLastInvokeWithExceptionNode();
        lastFixedNode = (FixedWithNextNode) s.noExceptionEdge;
        s.state = InvokeWithExceptionStructure.State.NO_EXCEPTION_EDGE;
    }

    public void exceptionPart() {
        InvokeWithExceptionStructure s = saveLastInvokeWithExceptionNode();
        lastFixedNode = (FixedWithNextNode) s.exceptionEdge;
        s.state = InvokeWithExceptionStructure.State.EXCEPTION_EDGE;
    }

    public ExceptionObjectNode exceptionObject() {
        InvokeWithExceptionStructure s = getTopStructure(InvokeWithExceptionStructure.class);
        return s.exceptionObject;
    }

    /**
     * Finishes a control flow started with {@link #startInvokeWithException}. If necessary, creates
     * a merge of the non-exception and exception edges. The merge node is returned and the
     * non-exception edge is the first forward end of the merge, the exception edge is the second
     * forward end (relevant for phi nodes).
     */
    public AbstractMergeNode endInvokeWithException() {
        InvokeWithExceptionStructure s = saveLastInvokeWithExceptionNode();
        FixedWithNextNode noExceptionEdge = s.noExceptionEdge instanceof FixedWithNextNode ? (FixedWithNextNode) s.noExceptionEdge : null;
        FixedWithNextNode exceptionEdge = s.exceptionEdge instanceof FixedWithNextNode ? (FixedWithNextNode) s.exceptionEdge : null;
        AbstractMergeNode merge = null;
        if (noExceptionEdge != null && exceptionEdge != null) {
            EndNode noExceptionEnd = graph.add(new EndNode());
            graph.addAfterFixed(noExceptionEdge, noExceptionEnd);
            EndNode exceptionEnd = graph.add(new EndNode());
            graph.addAfterFixed(exceptionEdge, exceptionEnd);
            merge = graph.add(new MergeNode());
            merge.addForwardEnd(noExceptionEnd);
            merge.addForwardEnd(exceptionEnd);
            lastFixedNode = merge;
        } else if (noExceptionEdge != null) {
            lastFixedNode = noExceptionEdge;
        } else if (exceptionEdge != null) {
            lastFixedNode = exceptionEdge;
        } else {
            assert lastFixedNode == null;
        }
        s.state = InvokeWithExceptionStructure.State.FINISHED;
        popStructure();
        return merge;
    }
}
