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

///
// A utility for manually creating a graph. This will be expanded as necessary to support all
// subsystems that employ manual graph creation (as opposed to {@linkplain GraphBuilderPhase
// bytecode parsing} based graph creation).
///
// @class GraphKit
public final class GraphKit implements GraphBuilderTool
{
    // @field
    protected final Providers ___providers;
    // @field
    protected final StructuredGraph ___graph;
    // @field
    protected final WordTypes ___wordTypes;
    // @field
    protected final GraphBuilderConfiguration.Plugins ___graphBuilderPlugins;
    // @field
    protected FixedWithNextNode ___lastFixedNode;

    // @field
    private final List<Structure> ___structures;

    // @class GraphKit.Structure
    protected abstract static class Structure
    {
    }

    // @cons
    public GraphKit(ResolvedJavaMethod __stubMethod, Providers __providers, WordTypes __wordTypes, Plugins __graphBuilderPlugins)
    {
        super();
        this.___providers = __providers;
        this.___graph = new StructuredGraph.Builder().method(__stubMethod).build();
        this.___graph.disableUnsafeAccessTracking();
        this.___wordTypes = __wordTypes;
        this.___graphBuilderPlugins = __graphBuilderPlugins;
        this.___lastFixedNode = this.___graph.start();

        this.___structures = new ArrayList<>();
        // Add a dummy element, so that the access of the last element never leads to an exception.
        this.___structures.add(new Structure() {});
    }

    @Override
    public StructuredGraph getGraph()
    {
        return this.___graph;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return this.___providers.getConstantReflection();
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider()
    {
        return this.___providers.getConstantFieldProvider();
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return this.___providers.getMetaAccess();
    }

    @Override
    public StampProvider getStampProvider()
    {
        return this.___providers.getStampProvider();
    }

    @Override
    public boolean parsingIntrinsic()
    {
        return true;
    }

    ///
    // Ensures a floating node is added to or already present in the graph via {@link Graph#unique}.
    //
    // @return a node similar to {@code node} if one exists, otherwise {@code node}
    ///
    public <T extends FloatingNode & ValueNumberable> T unique(T __node)
    {
        return this.___graph.unique(changeToWord(__node));
    }

    public <T extends ValueNode> T add(T __node)
    {
        return this.___graph.add(changeToWord(__node));
    }

    public <T extends ValueNode> T changeToWord(T __node)
    {
        if (this.___wordTypes != null && this.___wordTypes.isWord(__node))
        {
            __node.setStamp(this.___wordTypes.getWordStamp(StampTool.typeOrNull(__node)));
        }
        return __node;
    }

    @Override
    public <T extends ValueNode> T append(T __node)
    {
        T __result = this.___graph.addOrUniqueWithInputs(changeToWord(__node));
        if (__result instanceof FixedNode)
        {
            updateLastFixed((FixedNode) __result);
        }
        return __result;
    }

    private void updateLastFixed(FixedNode __result)
    {
        this.___graph.addAfterFixed(this.___lastFixedNode, __result);
        if (__result instanceof FixedWithNextNode)
        {
            this.___lastFixedNode = (FixedWithNextNode) __result;
        }
        else
        {
            this.___lastFixedNode = null;
        }
    }

    public InvokeNode createInvoke(Class<?> __declaringClass, String __name, ValueNode... __args)
    {
        return createInvoke(__declaringClass, __name, InvokeKind.Static, null, BytecodeFrame.UNKNOWN_BCI, __args);
    }

    ///
    // Creates and appends an {@link InvokeNode} for a call to a given method with a given set of
    // arguments. The method is looked up via reflection based on the declaring class and name.
    //
    // @param declaringClass the class declaring the invoked method
    // @param name the name of the invoked method
    // @param args the arguments to the invocation
    ///
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
                __method = this.___providers.getMetaAccess().lookupJavaMethod(__m);
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
            return this.___providers.getMetaAccess().lookupJavaMethod(__m);
        }
        catch (NoSuchMethodException | SecurityException __e)
        {
            throw new AssertionError(__e);
        }
    }

    ///
    // Creates and appends an {@link InvokeNode} for a call to a given method with a given set of arguments.
    ///
    public InvokeNode createInvoke(ResolvedJavaMethod __method, InvokeKind __invokeKind, FrameStateBuilder __frameStateBuilder, int __bci, ValueNode... __args)
    {
        Signature __signature = __method.getSignature();
        JavaType __returnType = __signature.getReturnType(null);
        StampPair __returnStamp = this.___graphBuilderPlugins.getOverridingStamp(this, __returnType, false);
        if (__returnStamp == null)
        {
            __returnStamp = StampFactory.forDeclaredType(this.___graph.getAssumptions(), __returnType, false);
        }
        MethodCallTargetNode __callTarget = this.___graph.add(createMethodCallTarget(__invokeKind, __method, __args, __returnStamp, __bci));
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
        return this.___wordTypes != null ? this.___wordTypes.asKind(__type) : __type.getJavaKind();
    }

    ///
    // Determines if a given set of arguments is compatible with the signature of a given method.
    //
    // @return true if {@code args} are compatible with the signature of {@code method}
    // @throws AssertionError if {@code args} are not compatible with the signature of {@code method}
    ///
    public boolean checkArgs(ResolvedJavaMethod __method, ValueNode... __args)
    {
        Signature __signature = __method.getSignature();
        boolean __isStatic = __method.isStatic();
        if (__signature.getParameterCount(!__isStatic) != __args.length)
        {
            throw new AssertionError(this.___graph + ": wrong number of arguments to " + __method);
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
                throw new AssertionError(this.___graph + ": wrong kind of value for argument " + __i + " of call to " + __method + " [" + __actual + " != " + __expected + "]");
            }
        }
        return true;
    }

    ///
    // Recursively {@linkplain #inline inlines} all invocations currently in the graph.
    ///
    public void inlineInvokes(String __reason, String __phase)
    {
        while (!this.___graph.getNodes().filter(InvokeNode.class).isEmpty())
        {
            for (InvokeNode __invoke : this.___graph.getNodes().filter(InvokeNode.class).snapshot())
            {
                inline(__invoke, __reason, __phase);
            }
        }

        // Clean up all code that is now dead after inlining.
        new DeadCodeEliminationPhase().apply(this.___graph);
    }

    ///
    // Inlines a given invocation to a method. The graph of the inlined method is processed in the
    // same manner as for snippets and method substitutions.
    ///
    public void inline(InvokeNode __invoke, String __reason, String __phase)
    {
        ResolvedJavaMethod __method = ((MethodCallTargetNode) __invoke.callTarget()).targetMethod();

        MetaAccessProvider __metaAccess = this.___providers.getMetaAccess();
        Plugins __plugins = new Plugins(this.___graphBuilderPlugins);
        GraphBuilderConfiguration __config = GraphBuilderConfiguration.getSnippetDefault(__plugins);

        StructuredGraph __calleeGraph = new StructuredGraph.Builder().method(__method).build();
        IntrinsicContext __initialReplacementContext = new IntrinsicContext(__method, __method, this.___providers.getReplacements().getDefaultReplacementBytecodeProvider(), CompilationContext.INLINE_AFTER_PARSING);
        GraphBuilderPhase.Instance __instance = createGraphBuilderInstance(__metaAccess, this.___providers.getStampProvider(), this.___providers.getConstantReflection(), this.___providers.getConstantFieldProvider(), __config, OptimisticOptimizations.NONE, __initialReplacementContext);
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
        this.___structures.add(__structure);
    }

    protected <T extends Structure> T getTopStructure(Class<T> __expectedClass)
    {
        return __expectedClass.cast(this.___structures.get(this.___structures.size() - 1));
    }

    protected void popStructure()
    {
        this.___structures.remove(this.___structures.size() - 1);
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
        protected IfState ___state;
        // @field
        protected FixedNode ___thenPart;
        // @field
        protected FixedNode ___elsePart;
    }

    ///
    // Starts an if-block. This call can be followed by a call to {@link #thenPart} to start
    // emitting the code executed when the condition hold; and a call to {@link #elsePart} to start
    // emititng the code when the condition does not hold. It must be followed by a call to
    // {@link #endIf} to close the if-block.
    //
    // @param condition The condition for the if-block
    // @param trueProbability The estimated probability the condition is true
    // @return the created {@link IfNode}.
    ///
    public IfNode startIf(LogicNode __condition, double __trueProbability)
    {
        AbstractBeginNode __thenSuccessor = this.___graph.add(new BeginNode());
        AbstractBeginNode __elseSuccessor = this.___graph.add(new BeginNode());
        IfNode __node = append(new IfNode(__condition, __thenSuccessor, __elseSuccessor, __trueProbability));
        this.___lastFixedNode = null;

        IfStructure __s = new IfStructure();
        __s.___state = IfState.CONDITION;
        __s.___thenPart = __thenSuccessor;
        __s.___elsePart = __elseSuccessor;
        pushStructure(__s);
        return __node;
    }

    private IfStructure saveLastIfNode()
    {
        IfStructure __s = getTopStructure(IfStructure.class);
        switch (__s.___state)
        {
            case CONDITION:
                break;
            case THEN_PART:
            {
                __s.___thenPart = this.___lastFixedNode;
                break;
            }
            case ELSE_PART:
            {
                __s.___elsePart = this.___lastFixedNode;
                break;
            }
            case FINISHED:
                break;
        }
        this.___lastFixedNode = null;
        return __s;
    }

    public void thenPart()
    {
        IfStructure __s = saveLastIfNode();
        this.___lastFixedNode = (FixedWithNextNode) __s.___thenPart;
        __s.___state = IfState.THEN_PART;
    }

    public void elsePart()
    {
        IfStructure __s = saveLastIfNode();
        this.___lastFixedNode = (FixedWithNextNode) __s.___elsePart;
        __s.___state = IfState.ELSE_PART;
    }

    ///
    // Ends an if block started with {@link #startIf(LogicNode, double)}.
    //
    // @return the created merge node, or {@code null} if no merge node was required (for example,
    //         when one part ended with a control sink).
    ///
    public AbstractMergeNode endIf()
    {
        IfStructure __s = saveLastIfNode();

        FixedWithNextNode __thenPart = __s.___thenPart instanceof FixedWithNextNode ? (FixedWithNextNode) __s.___thenPart : null;
        FixedWithNextNode __elsePart = __s.___elsePart instanceof FixedWithNextNode ? (FixedWithNextNode) __s.___elsePart : null;
        AbstractMergeNode __merge = null;

        if (__thenPart != null && __elsePart != null)
        {
            // Both parts are alive, we need a real merge.
            EndNode __thenEnd = this.___graph.add(new EndNode());
            this.___graph.addAfterFixed(__thenPart, __thenEnd);
            EndNode __elseEnd = this.___graph.add(new EndNode());
            this.___graph.addAfterFixed(__elsePart, __elseEnd);

            __merge = this.___graph.add(new MergeNode());
            __merge.addForwardEnd(__thenEnd);
            __merge.addForwardEnd(__elseEnd);

            this.___lastFixedNode = __merge;
        }
        else if (__thenPart != null)
        {
            // elsePart ended with a control sink, so we can continue with thenPart.
            this.___lastFixedNode = __thenPart;
        }
        else if (__elsePart != null)
        {
            // thenPart ended with a control sink, so we can continue with elsePart.
            this.___lastFixedNode = __elsePart;
        }
        else
        {
            // Both parts ended with a control sink, so no nodes can be added after the if.
        }
        __s.___state = IfState.FINISHED;
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
        protected State ___state;
        // @field
        protected ExceptionObjectNode ___exceptionObject;
        // @field
        protected FixedNode ___noExceptionEdge;
        // @field
        protected FixedNode ___exceptionEdge;
    }

    public InvokeWithExceptionNode startInvokeWithException(ResolvedJavaMethod __method, InvokeKind __invokeKind, FrameStateBuilder __frameStateBuilder, int __invokeBci, int __exceptionEdgeBci, ValueNode... __args)
    {
        Signature __signature = __method.getSignature();
        JavaType __returnType = __signature.getReturnType(null);
        StampPair __returnStamp = this.___graphBuilderPlugins.getOverridingStamp(this, __returnType, false);
        if (__returnStamp == null)
        {
            __returnStamp = StampFactory.forDeclaredType(this.___graph.getAssumptions(), __returnType, false);
        }
        MethodCallTargetNode __callTarget = this.___graph.add(createMethodCallTarget(__invokeKind, __method, __args, __returnStamp, __invokeBci));
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
        AbstractBeginNode __noExceptionEdge = this.___graph.add(KillingBeginNode.create(LocationIdentity.any()));
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
        this.___lastFixedNode = null;

        InvokeWithExceptionStructure __s = new InvokeWithExceptionStructure();
        __s.___state = InvokeWithExceptionStructure.State.INVOKE;
        __s.___noExceptionEdge = __noExceptionEdge;
        __s.___exceptionEdge = __exceptionObject;
        __s.___exceptionObject = __exceptionObject;
        pushStructure(__s);

        return __invoke;
    }

    private InvokeWithExceptionStructure saveLastInvokeWithExceptionNode()
    {
        InvokeWithExceptionStructure __s = getTopStructure(InvokeWithExceptionStructure.class);
        switch (__s.___state)
        {
            case INVOKE:
                break;
            case NO_EXCEPTION_EDGE:
            {
                __s.___noExceptionEdge = this.___lastFixedNode;
                break;
            }
            case EXCEPTION_EDGE:
            {
                __s.___exceptionEdge = this.___lastFixedNode;
                break;
            }
            case FINISHED:
                break;
        }
        this.___lastFixedNode = null;
        return __s;
    }

    public void noExceptionPart()
    {
        InvokeWithExceptionStructure __s = saveLastInvokeWithExceptionNode();
        this.___lastFixedNode = (FixedWithNextNode) __s.___noExceptionEdge;
        __s.___state = InvokeWithExceptionStructure.State.NO_EXCEPTION_EDGE;
    }

    public void exceptionPart()
    {
        InvokeWithExceptionStructure __s = saveLastInvokeWithExceptionNode();
        this.___lastFixedNode = (FixedWithNextNode) __s.___exceptionEdge;
        __s.___state = InvokeWithExceptionStructure.State.EXCEPTION_EDGE;
    }

    public ExceptionObjectNode exceptionObject()
    {
        InvokeWithExceptionStructure __s = getTopStructure(InvokeWithExceptionStructure.class);
        return __s.___exceptionObject;
    }

    ///
    // Finishes a control flow started with {@link #startInvokeWithException}. If necessary, creates
    // a merge of the non-exception and exception edges. The merge node is returned and the
    // non-exception edge is the first forward end of the merge, the exception edge is the second
    // forward end (relevant for phi nodes).
    ///
    public AbstractMergeNode endInvokeWithException()
    {
        InvokeWithExceptionStructure __s = saveLastInvokeWithExceptionNode();
        FixedWithNextNode __noExceptionEdge = __s.___noExceptionEdge instanceof FixedWithNextNode ? (FixedWithNextNode) __s.___noExceptionEdge : null;
        FixedWithNextNode __exceptionEdge = __s.___exceptionEdge instanceof FixedWithNextNode ? (FixedWithNextNode) __s.___exceptionEdge : null;
        AbstractMergeNode __merge = null;
        if (__noExceptionEdge != null && __exceptionEdge != null)
        {
            EndNode __noExceptionEnd = this.___graph.add(new EndNode());
            this.___graph.addAfterFixed(__noExceptionEdge, __noExceptionEnd);
            EndNode __exceptionEnd = this.___graph.add(new EndNode());
            this.___graph.addAfterFixed(__exceptionEdge, __exceptionEnd);
            __merge = this.___graph.add(new MergeNode());
            __merge.addForwardEnd(__noExceptionEnd);
            __merge.addForwardEnd(__exceptionEnd);
            this.___lastFixedNode = __merge;
        }
        else if (__noExceptionEdge != null)
        {
            this.___lastFixedNode = __noExceptionEdge;
        }
        else if (__exceptionEdge != null)
        {
            this.___lastFixedNode = __exceptionEdge;
        }
        __s.___state = InvokeWithExceptionStructure.State.FINISHED;
        popStructure();
        return __merge;
    }
}
