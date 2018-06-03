package giraaff.java;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaField;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.JavaTypeProfile.ProfiledType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.RawConstant;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;
import jdk.vm.ci.meta.TriState;
import jdk.vm.ci.runtime.JVMCICompiler;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.word.LocationIdentity;

import giraaff.api.replacements.Snippet;
import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeLookupSwitch;
import giraaff.bytecode.BytecodeProvider;
import giraaff.bytecode.BytecodeStream;
import giraaff.bytecode.BytecodeSwitch;
import giraaff.bytecode.BytecodeTableSwitch;
import giraaff.bytecode.Bytecodes;
import giraaff.bytecode.Bytes;
import giraaff.bytecode.ResolvedJavaMethodBytecode;
import giraaff.bytecode.ResolvedJavaMethodBytecodeProvider;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.calc.Condition.CanonicalizedCondition;
import giraaff.core.common.calc.FloatConvert;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Graph.Mark;
import giraaff.graph.Node;
import giraaff.java.BciBlockMapping.BciBlock;
import giraaff.java.BciBlockMapping.ExceptionDispatchBlock;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.EntryMarkerNode;
import giraaff.nodes.EntryProxyNode;
import giraaff.nodes.FieldLocationIdentity;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.IfNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.KillingBeginNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNegationNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.UnwindNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.AndNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.FloatConvertNode;
import giraaff.nodes.calc.FloatDivNode;
import giraaff.nodes.calc.IntegerBelowNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.calc.MulNode;
import giraaff.nodes.calc.NarrowNode;
import giraaff.nodes.calc.NegateNode;
import giraaff.nodes.calc.NormalizeCompareNode;
import giraaff.nodes.calc.ObjectEqualsNode;
import giraaff.nodes.calc.OrNode;
import giraaff.nodes.calc.RemNode;
import giraaff.nodes.calc.RightShiftNode;
import giraaff.nodes.calc.SignExtendNode;
import giraaff.nodes.calc.SignedDivNode;
import giraaff.nodes.calc.SignedRemNode;
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.calc.UnsignedRightShiftNode;
import giraaff.nodes.calc.XorNode;
import giraaff.nodes.calc.ZeroExtendNode;
import giraaff.nodes.extended.AnchoringNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.BytecodeExceptionNode;
import giraaff.nodes.extended.IntegerSwitchNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.extended.LoadMethodNode;
import giraaff.nodes.extended.MembarNode;
import giraaff.nodes.extended.StateSplitProxyNode;
import giraaff.nodes.extended.ValueAnchorNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.BytecodeExceptionMode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin.InlineInfo;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.IntrinsicContext.CompilationContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugins.InvocationPluginReceiver;
import giraaff.nodes.graphbuilderconf.InvokeDynamicPlugin;
import giraaff.nodes.graphbuilderconf.NodePlugin;
import giraaff.nodes.java.ArrayLengthNode;
import giraaff.nodes.java.ExceptionObjectNode;
import giraaff.nodes.java.FinalFieldBarrierNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.java.MonitorEnterNode;
import giraaff.nodes.java.MonitorExitNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.java.NewArrayNode;
import giraaff.nodes.java.NewInstanceNode;
import giraaff.nodes.java.NewMultiArrayNode;
import giraaff.nodes.java.RegisterFinalizerNode;
import giraaff.nodes.java.StoreFieldNode;
import giraaff.nodes.java.StoreIndexedNode;
import giraaff.nodes.spi.StampProvider;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.util.ValueMergeUtil;
import giraaff.util.GraalError;

/**
 * The {@code GraphBuilder} class parses the bytecode of a method and builds the IR graph.
 */
// @class BytecodeParser
public final class BytecodeParser implements GraphBuilderContext
{
    /**
     * A scoped object for tasks to be performed after parsing an intrinsic such as processing
     * {@linkplain BytecodeFrame#isPlaceholderBci(int) placeholder} frames states.
     */
    // @class BytecodeParser.IntrinsicScope
    static final class IntrinsicScope implements AutoCloseable
    {
        // @field
        FrameState stateBefore;
        // @field
        final Mark mark;
        // @field
        final BytecodeParser parser;
        // @field
        List<ReturnToCallerData> returnDataList;

        /**
         * Creates a scope for root parsing an intrinsic.
         *
         * @param parser the parsing context of the intrinsic
         */
        // @cons
        IntrinsicScope(BytecodeParser __parser)
        {
            super();
            this.parser = __parser;
            mark = null;
        }

        /**
         * Creates a scope for parsing an intrinsic during graph builder inlining.
         *
         * @param parser the parsing context of the (non-intrinsic) method calling the intrinsic
         * @param args the arguments to the call
         */
        // @cons
        IntrinsicScope(BytecodeParser __parser, JavaKind[] __argSlotKinds, ValueNode[] __args)
        {
            super();
            this.parser = __parser;
            mark = __parser.getGraph().getMark();
            stateBefore = __parser.frameState.create(__parser.bci(), __parser.getNonIntrinsicAncestor(), false, __argSlotKinds, __args);
        }

        @Override
        public void close()
        {
            IntrinsicContext __intrinsic = parser.intrinsicContext;
            if (__intrinsic != null && __intrinsic.isPostParseInlined())
            {
                return;
            }

            processPlaceholderFrameStates(__intrinsic);
        }

        /**
         * Fixes up the {@linkplain BytecodeFrame#isPlaceholderBci(int) placeholder} frame states
         * added to the graph while parsing/inlining the intrinsic for which this object exists.
         */
        private void processPlaceholderFrameStates(IntrinsicContext __intrinsic)
        {
            StructuredGraph __graph = parser.getGraph();
            boolean __sawInvalidFrameState = false;
            for (Node __node : __graph.getNewNodes(mark))
            {
                if (__node instanceof FrameState)
                {
                    FrameState __frameState = (FrameState) __node;
                    if (BytecodeFrame.isPlaceholderBci(__frameState.bci))
                    {
                        if (__frameState.bci == BytecodeFrame.AFTER_BCI)
                        {
                            if (parser.getInvokeReturnType() == null)
                            {
                                // A frame state in a root compiled intrinsic.
                                FrameState __newFrameState = __graph.add(new FrameState(BytecodeFrame.INVALID_FRAMESTATE_BCI));
                                __frameState.replaceAndDelete(__newFrameState);
                            }
                            else
                            {
                                JavaKind __returnKind = parser.getInvokeReturnType().getJavaKind();
                                FrameStateBuilder __frameStateBuilder = parser.frameState;
                                if (__frameState.stackSize() != 0)
                                {
                                    ValueNode __returnVal = __frameState.stackAt(0);
                                    if (!ReturnToCallerData.containsReturnValue(returnDataList, __returnVal))
                                    {
                                        throw new GraalError("AFTER_BCI frame state within an intrinsic has a non-return value on the stack: %s", __returnVal);
                                    }

                                    // Swap the top-of-stack value with the return value.
                                    ValueNode __tos = __frameStateBuilder.pop(__returnKind);
                                    FrameState __newFrameState = __frameStateBuilder.create(parser.stream.nextBCI(), parser.getNonIntrinsicAncestor(), false, new JavaKind[] { __returnKind }, new ValueNode[] { __returnVal });
                                    __frameState.replaceAndDelete(__newFrameState);
                                    __frameStateBuilder.push(__returnKind, __tos);
                                }
                                else if (__returnKind != JavaKind.Void)
                                {
                                    // If the intrinsic returns a non-void value, then any frame state with an empty stack
                                    // is invalid as it cannot be used to deoptimize to just after the call returns.
                                    // These invalid frame states are expected to be removed by later compilation stages.
                                    FrameState __newFrameState = __graph.add(new FrameState(BytecodeFrame.INVALID_FRAMESTATE_BCI));
                                    __frameState.replaceAndDelete(__newFrameState);
                                    __sawInvalidFrameState = true;
                                }
                                else
                                {
                                    // An intrinsic for a void method.
                                    FrameState __newFrameState = __frameStateBuilder.create(parser.stream.nextBCI(), null);
                                    __frameState.replaceAndDelete(__newFrameState);
                                }
                            }
                        }
                        else if (__frameState.bci == BytecodeFrame.BEFORE_BCI)
                        {
                            if (stateBefore == null)
                            {
                                stateBefore = __graph.start().stateAfter();
                            }
                            if (stateBefore != __frameState)
                            {
                                __frameState.replaceAndDelete(stateBefore);
                            }
                        }
                        else if (__frameState.bci == BytecodeFrame.AFTER_EXCEPTION_BCI)
                        {
                            // This is a frame state for the entry point to an exception dispatcher in an intrinsic.
                            // For example, the invoke denoting a partial intrinsic exit will have an edge to such a
                            // dispatcher if the profile for the original invoke being intrinsified indicates an
                            // exception was seen. As per JVM bytecode semantics, the interpreter expects a single
                            // value on the stack on entry to an exception handler, namely the exception object.
                            ValueNode __exceptionValue = __frameState.stackAt(0);
                            ExceptionObjectNode __exceptionObject = (ExceptionObjectNode) GraphUtil.unproxify(__exceptionValue);
                            FrameStateBuilder __dispatchState = parser.frameState.copy();
                            __dispatchState.clearStack();
                            __dispatchState.push(JavaKind.Object, __exceptionValue);
                            __dispatchState.setRethrowException(true);
                            FrameState __newFrameState = __dispatchState.create(parser.bci(), __exceptionObject);
                            __frameState.replaceAndDelete(__newFrameState);
                        }
                    }
                }
            }
            if (__sawInvalidFrameState)
            {
                JavaKind __returnKind = parser.getInvokeReturnType().getJavaKind();
                FrameStateBuilder __frameStateBuilder = parser.frameState;
                ValueNode __returnValue = __frameStateBuilder.pop(__returnKind);
                StateSplitProxyNode __proxy = __graph.add(new StateSplitProxyNode(__returnValue));
                parser.lastInstr.setNext(__proxy);
                __frameStateBuilder.push(__returnKind, __proxy);
                __proxy.setStateAfter(parser.createFrameState(parser.stream.nextBCI(), __proxy));
                parser.lastInstr = __proxy;
            }
        }
    }

    // @class BytecodeParser.Target
    private static final class Target
    {
        // @field
        FixedNode fixed;
        // @field
        FrameStateBuilder state;

        // @cons
        Target(FixedNode __fixed, FrameStateBuilder __state)
        {
            super();
            this.fixed = __fixed;
            this.state = __state;
        }
    }

    // @class BytecodeParser.ReturnToCallerData
    protected static final class ReturnToCallerData
    {
        // @field
        protected final ValueNode returnValue;
        // @field
        protected final FixedWithNextNode beforeReturnNode;

        // @cons
        protected ReturnToCallerData(ValueNode __returnValue, FixedWithNextNode __beforeReturnNode)
        {
            super();
            this.returnValue = __returnValue;
            this.beforeReturnNode = __beforeReturnNode;
        }

        static boolean containsReturnValue(List<ReturnToCallerData> __list, ValueNode __value)
        {
            for (ReturnToCallerData __e : __list)
            {
                if (__e.returnValue == __value)
                {
                    return true;
                }
            }
            return false;
        }
    }

    // @field
    private final GraphBuilderPhase.Instance graphBuilderInstance;
    // @field
    protected final StructuredGraph graph;

    // @field
    private BciBlockMapping blockMap;
    // @field
    private LocalLiveness liveness;
    // @field
    protected final int entryBCI;
    // @field
    private final BytecodeParser parent;

    // @field
    private ValueNode methodSynchronizedObject;

    // @field
    private List<ReturnToCallerData> returnDataList;
    // @field
    private ValueNode unwindValue;
    // @field
    private FixedWithNextNode beforeUnwindNode;

    // @field
    protected FixedWithNextNode lastInstr; // the last instruction added
    // @field
    private boolean controlFlowSplit;
    // @field
    private final InvocationPluginReceiver invocationPluginReceiver = new InvocationPluginReceiver(this);

    // @field
    private FixedWithNextNode[] firstInstructionArray;
    // @field
    private FrameStateBuilder[] entryStateArray;

    // @field
    private boolean finalBarrierRequired;
    // @field
    private ValueNode originalReceiver;
    // @field
    private final boolean eagerInitializing;
    // @field
    private final boolean uninitializedIsError;

    // @cons
    protected BytecodeParser(GraphBuilderPhase.Instance __graphBuilderInstance, StructuredGraph __graph, BytecodeParser __parent, ResolvedJavaMethod __method, int __entryBCI, IntrinsicContext __intrinsicContext)
    {
        super();
        this.bytecodeProvider = __intrinsicContext == null ? ResolvedJavaMethodBytecodeProvider.INSTANCE : __intrinsicContext.getBytecodeProvider();
        this.code = bytecodeProvider.getBytecode(__method);
        this.method = code.getMethod();
        this.graphBuilderInstance = __graphBuilderInstance;
        this.graph = __graph;
        this.graphBuilderConfig = __graphBuilderInstance.graphBuilderConfig;
        this.optimisticOpts = __graphBuilderInstance.optimisticOpts;
        this.metaAccess = __graphBuilderInstance.metaAccess;
        this.stampProvider = __graphBuilderInstance.stampProvider;
        this.constantReflection = __graphBuilderInstance.constantReflection;
        this.constantFieldProvider = __graphBuilderInstance.constantFieldProvider;
        this.stream = new BytecodeStream(code.getCode());
        this.profilingInfo = __graph.useProfilingInfo() ? code.getProfilingInfo() : null;
        this.constantPool = code.getConstantPool();
        this.intrinsicContext = __intrinsicContext;
        this.entryBCI = __entryBCI;
        this.parent = __parent;

        eagerInitializing = graphBuilderConfig.eagerResolving();
        uninitializedIsError = graphBuilderConfig.unresolvedIsError();
    }

    protected GraphBuilderPhase.Instance getGraphBuilderInstance()
    {
        return graphBuilderInstance;
    }

    public ValueNode getUnwindValue()
    {
        return unwindValue;
    }

    public FixedWithNextNode getBeforeUnwindNode()
    {
        return this.beforeUnwindNode;
    }

    @SuppressWarnings("try")
    protected void buildRootMethod()
    {
        FrameStateBuilder __startFrameState = new FrameStateBuilder(this, code, graph);
        __startFrameState.initializeForMethodStart(graph.getAssumptions(), graphBuilderConfig.eagerResolving() || intrinsicContext != null, graphBuilderConfig.getPlugins());

        try (IntrinsicScope __s = intrinsicContext != null ? new IntrinsicScope(this) : null)
        {
            build(graph.start(), __startFrameState);
        }

        cleanupFinalGraph();
        ComputeLoopFrequenciesClosure.compute(graph);
    }

    protected void build(FixedWithNextNode __startInstruction, FrameStateBuilder __startFrameState)
    {
        if (bytecodeProvider.shouldRecordMethodDependencies())
        {
            // record method dependency in the graph
            graph.recordMethod(method);
        }

        // compute the block map, setup exception handlers and get the entrypoint(s)
        BciBlockMapping __newMapping = BciBlockMapping.create(stream, code);
        this.blockMap = __newMapping;
        this.firstInstructionArray = new FixedWithNextNode[blockMap.getBlockCount()];
        this.entryStateArray = new FrameStateBuilder[blockMap.getBlockCount()];
        if (!method.isStatic())
        {
            originalReceiver = __startFrameState.loadLocal(0, JavaKind.Object);
        }

        liveness = LocalLiveness.compute(stream, blockMap.getBlocks(), method.getMaxLocals(), blockMap.getLoopCount());

        lastInstr = __startInstruction;
        this.setCurrentFrameState(__startFrameState);
        stream.setBCI(0);

        BciBlock __startBlock = blockMap.getStartBlock();
        if (this.parent == null)
        {
            StartNode __startNode = graph.start();
            if (method.isSynchronized())
            {
                __startNode.setStateAfter(createFrameState(BytecodeFrame.BEFORE_BCI, __startNode));
            }
            else
            {
                if (!parsingIntrinsic())
                {
                    if (graph.method() != null && graph.method().isJavaLangObjectInit())
                    {
                        /*
                         * Don't clear the receiver when Object.<init> is the compilation root.
                         * The receiver is needed as input to RegisterFinalizerNode.
                         */
                    }
                    else
                    {
                        frameState.clearNonLiveLocals(__startBlock, liveness, true);
                    }
                    __startNode.setStateAfter(createFrameState(bci(), __startNode));
                }
                else
                {
                    if (__startNode.stateAfter() == null)
                    {
                        FrameState __stateAfterStart = createStateAfterStartOfReplacementGraph();
                        __startNode.setStateAfter(__stateAfterStart);
                    }
                }
            }
        }

        if (method.isSynchronized())
        {
            finishPrepare(lastInstr, BytecodeFrame.BEFORE_BCI);

            // add a monitor enter to the start block
            methodSynchronizedObject = synchronizedObject(frameState, method);
            frameState.clearNonLiveLocals(__startBlock, liveness, true);
            genMonitorEnter(methodSynchronizedObject, bci());
        }

        finishPrepare(lastInstr, 0);

        currentBlock = blockMap.getStartBlock();
        setEntryState(__startBlock, frameState);
        if (__startBlock.isLoopHeader())
        {
            appendGoto(__startBlock);
        }
        else
        {
            setFirstInstruction(__startBlock, lastInstr);
        }

        BciBlock[] __blocks = blockMap.getBlocks();
        for (BciBlock __block : __blocks)
        {
            processBlock(__block);
        }
    }

    /**
     * Hook for subclasses to modify synthetic code (start nodes and unwind nodes).
     *
     * @param instruction the current last instruction
     * @param bci the current bci
     */
    protected void finishPrepare(FixedWithNextNode __instruction, int __bci)
    {
    }

    protected void cleanupFinalGraph()
    {
        GraphUtil.normalizeLoops(graph);

        // Remove dead parameters.
        for (ParameterNode __param : graph.getNodes(ParameterNode.TYPE))
        {
            if (__param.hasNoUsages())
            {
                __param.safeDelete();
            }
        }

        // Remove redundant begin nodes.
        for (BeginNode __beginNode : graph.getNodes(BeginNode.TYPE))
        {
            Node __predecessor = __beginNode.predecessor();
            if (__predecessor instanceof ControlSplitNode)
            {
                // The begin node is necessary.
            }
            else if (!__beginNode.hasUsages())
            {
                GraphUtil.unlinkFixedNode(__beginNode);
                __beginNode.safeDelete();
            }
        }
    }

    /**
     * Creates the frame state after the start node of a graph for an {@link IntrinsicContext
     * intrinsic} that is the parse root (either for root compiling or for post-parse inlining).
     */
    private FrameState createStateAfterStartOfReplacementGraph()
    {
        FrameState __stateAfterStart;
        if (intrinsicContext.isPostParseInlined())
        {
            __stateAfterStart = graph.add(new FrameState(BytecodeFrame.BEFORE_BCI));
        }
        else
        {
            ResolvedJavaMethod __original = intrinsicContext.getOriginalMethod();
            ValueNode[] __locals;
            if (__original.getMaxLocals() == frameState.localsSize() || __original.isNative())
            {
                __locals = new ValueNode[__original.getMaxLocals()];
                for (int __i = 0; __i < __locals.length; __i++)
                {
                    ValueNode __node = frameState.locals[__i];
                    if (__node == FrameState.TWO_SLOT_MARKER)
                    {
                        __node = null;
                    }
                    __locals[__i] = __node;
                }
            }
            else
            {
                __locals = new ValueNode[__original.getMaxLocals()];
                int __parameterCount = __original.getSignature().getParameterCount(!__original.isStatic());
                for (int __i = 0; __i < __parameterCount; __i++)
                {
                    ValueNode __param = frameState.locals[__i];
                    if (__param == FrameState.TWO_SLOT_MARKER)
                    {
                        __param = null;
                    }
                    __locals[__i] = __param;
                }
            }
            ValueNode[] __stack = {};
            int __stackSize = 0;
            ValueNode[] __locks = {};
            List<MonitorIdNode> __monitorIds = Collections.emptyList();
            __stateAfterStart = graph.add(new FrameState(null, new ResolvedJavaMethodBytecode(__original), 0, __locals, __stack, __stackSize, __locks, __monitorIds, false, false));
        }
        return __stateAfterStart;
    }

    /**
     * @param type the unresolved type of the constant
     */
    protected void handleUnresolvedLoadConstant(JavaType __type)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param type the unresolved type of the type check
     * @param object the object value whose type is being checked against {@code type}
     */
    protected void handleUnresolvedCheckCast(JavaType __type, ValueNode __object)
    {
        append(new FixedGuardNode(graph.addOrUniqueWithInputs(IsNullNode.create(__object)), DeoptimizationReason.Unresolved, DeoptimizationAction.InvalidateRecompile));
        frameState.push(JavaKind.Object, appendConstant(JavaConstant.NULL_POINTER));
    }

    /**
     * @param type the unresolved type of the type check
     * @param object the object value whose type is being checked against {@code type}
     */
    protected void handleUnresolvedInstanceOf(JavaType __type, ValueNode __object)
    {
        AbstractBeginNode __successor = graph.add(new BeginNode());
        DeoptimizeNode __deopt = graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
        append(new IfNode(graph.addOrUniqueWithInputs(IsNullNode.create(__object)), __successor, __deopt, 1));
        lastInstr = __successor;
        frameState.push(JavaKind.Int, appendConstant(JavaConstant.INT_0));
    }

    /**
     * @param type the type being instantiated
     */
    protected void handleUnresolvedNewInstance(JavaType __type)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param type the type of the array being instantiated
     * @param length the length of the array
     */
    protected void handleUnresolvedNewObjectArray(JavaType __type, ValueNode __length)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param type the type being instantiated
     * @param dims the dimensions for the multi-array
     */
    protected void handleUnresolvedNewMultiArray(JavaType __type, ValueNode[] __dims)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param field the unresolved field
     * @param receiver the object containing the field or {@code null} if {@code field} is static
     */
    protected void handleUnresolvedLoadField(JavaField __field, ValueNode __receiver)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param field the unresolved field
     * @param value the value being stored to the field
     * @param receiver the object containing the field or {@code null} if {@code field} is static
     */
    protected void handleUnresolvedStoreField(JavaField __field, ValueNode __value, ValueNode __receiver)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    protected void handleUnresolvedExceptionType(JavaType __type)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    protected void handleUnresolvedInvoke(JavaMethod __javaMethod, InvokeKind __invokeKind)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    private AbstractBeginNode handleException(ValueNode __exceptionObject, int __bci, boolean __deoptimizeOnly)
    {
        FrameStateBuilder __dispatchState = frameState.copy();
        __dispatchState.clearStack();

        AbstractBeginNode __dispatchBegin;
        if (__exceptionObject == null)
        {
            ExceptionObjectNode __newExceptionObject = graph.add(new ExceptionObjectNode(metaAccess));
            __dispatchBegin = __newExceptionObject;
            __dispatchState.push(JavaKind.Object, __dispatchBegin);
            __dispatchState.setRethrowException(true);
            __newExceptionObject.setStateAfter(__dispatchState.create(__bci, __newExceptionObject));
        }
        else
        {
            __dispatchBegin = graph.add(new BeginNode());
            __dispatchState.push(JavaKind.Object, __exceptionObject);
            __dispatchState.setRethrowException(true);
        }
        this.controlFlowSplit = true;
        FixedWithNextNode __finishedDispatch = finishInstruction(__dispatchBegin, __dispatchState);

        if (__deoptimizeOnly)
        {
            DeoptimizeNode __deoptimizeNode = graph.add(new DeoptimizeNode(DeoptimizationAction.None, DeoptimizationReason.TransferToInterpreter));
            __dispatchBegin.setNext(BeginNode.begin(__deoptimizeNode));
        }
        else
        {
            createHandleExceptionTarget(__finishedDispatch, __bci, __dispatchState);
        }
        return __dispatchBegin;
    }

    protected void createHandleExceptionTarget(FixedWithNextNode __finishedDispatch, int __bci, FrameStateBuilder __dispatchState)
    {
        BciBlock __dispatchBlock = currentBlock.exceptionDispatchBlock();
        /*
         * The exception dispatch block is always for the last bytecode of a block, so if we are not
         * at the endBci yet, there is no exception handler for this bci and we can unwind immediately.
         */
        if (__bci != currentBlock.endBci || __dispatchBlock == null)
        {
            __dispatchBlock = blockMap.getUnwindBlock();
        }

        FixedNode __target = createTarget(__dispatchBlock, __dispatchState);
        __finishedDispatch.setNext(__target);
    }

    protected ValueNode genLoadIndexed(ValueNode __array, ValueNode __index, JavaKind __kind)
    {
        return LoadIndexedNode.create(graph.getAssumptions(), __array, __index, __kind, metaAccess, constantReflection);
    }

    protected void genStoreIndexed(ValueNode __array, ValueNode __index, JavaKind __kind, ValueNode __value)
    {
        add(new StoreIndexedNode(__array, __index, __kind, __value));
    }

    protected ValueNode genIntegerAdd(ValueNode __x, ValueNode __y)
    {
        return AddNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerSub(ValueNode __x, ValueNode __y)
    {
        return SubNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerMul(ValueNode __x, ValueNode __y)
    {
        return MulNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatAdd(ValueNode __x, ValueNode __y)
    {
        return AddNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatSub(ValueNode __x, ValueNode __y)
    {
        return SubNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatMul(ValueNode __x, ValueNode __y)
    {
        return MulNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatDiv(ValueNode __x, ValueNode __y)
    {
        return FloatDivNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatRem(ValueNode __x, ValueNode __y)
    {
        return RemNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerDiv(ValueNode __x, ValueNode __y)
    {
        return SignedDivNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerRem(ValueNode __x, ValueNode __y)
    {
        return SignedRemNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genNegateOp(ValueNode __x)
    {
        return NegateNode.create(__x, NodeView.DEFAULT);
    }

    protected ValueNode genLeftShift(ValueNode __x, ValueNode __y)
    {
        return LeftShiftNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genRightShift(ValueNode __x, ValueNode __y)
    {
        return RightShiftNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genUnsignedRightShift(ValueNode __x, ValueNode __y)
    {
        return UnsignedRightShiftNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genAnd(ValueNode __x, ValueNode __y)
    {
        return AndNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genOr(ValueNode __x, ValueNode __y)
    {
        return OrNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genXor(ValueNode __x, ValueNode __y)
    {
        return XorNode.create(__x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genNormalizeCompare(ValueNode __x, ValueNode __y, boolean __isUnorderedLess)
    {
        return NormalizeCompareNode.create(__x, __y, __isUnorderedLess, JavaKind.Int, constantReflection);
    }

    protected ValueNode genFloatConvert(FloatConvert __op, ValueNode __input)
    {
        return FloatConvertNode.create(__op, __input, NodeView.DEFAULT);
    }

    protected ValueNode genNarrow(ValueNode __input, int __bitCount)
    {
        return NarrowNode.create(__input, __bitCount, NodeView.DEFAULT);
    }

    protected ValueNode genSignExtend(ValueNode __input, int __bitCount)
    {
        return SignExtendNode.create(__input, __bitCount, NodeView.DEFAULT);
    }

    protected ValueNode genZeroExtend(ValueNode __input, int __bitCount)
    {
        return ZeroExtendNode.create(__input, __bitCount, NodeView.DEFAULT);
    }

    protected void genGoto()
    {
        appendGoto(currentBlock.getSuccessor(0));
    }

    protected LogicNode genObjectEquals(ValueNode __x, ValueNode __y)
    {
        return ObjectEqualsNode.create(constantReflection, metaAccess, __x, __y, NodeView.DEFAULT);
    }

    protected LogicNode genIntegerEquals(ValueNode __x, ValueNode __y)
    {
        return IntegerEqualsNode.create(constantReflection, metaAccess, null, __x, __y, NodeView.DEFAULT);
    }

    protected LogicNode genIntegerLessThan(ValueNode __x, ValueNode __y)
    {
        return IntegerLessThanNode.create(constantReflection, metaAccess, null, __x, __y, NodeView.DEFAULT);
    }

    protected ValueNode genUnique(ValueNode __x)
    {
        return graph.addOrUniqueWithInputs(__x);
    }

    protected LogicNode genUnique(LogicNode __x)
    {
        return graph.addOrUniqueWithInputs(__x);
    }

    protected ValueNode genIfNode(LogicNode __condition, FixedNode __falseSuccessor, FixedNode __trueSuccessor, double __d)
    {
        return new IfNode(__condition, __falseSuccessor, __trueSuccessor, __d);
    }

    protected void genThrow()
    {
        ValueNode __exception = frameState.pop(JavaKind.Object);
        FixedGuardNode __nullCheck = append(new FixedGuardNode(graph.addOrUniqueWithInputs(IsNullNode.create(__exception)), DeoptimizationReason.NullCheckException, DeoptimizationAction.InvalidateReprofile, true));
        ValueNode __nonNullException = graph.maybeAddOrUnique(PiNode.create(__exception, __exception.stamp(NodeView.DEFAULT).join(StampFactory.objectNonNull()), __nullCheck));
        lastInstr.setNext(handleException(__nonNullException, bci(), false));
    }

    protected LogicNode createInstanceOf(TypeReference __type, ValueNode __object)
    {
        return InstanceOfNode.create(__type, __object);
    }

    protected AnchoringNode createAnchor(JavaTypeProfile __profile)
    {
        if (__profile == null || __profile.getNotRecordedProbability() > 0.0)
        {
            return null;
        }
        else
        {
            return append(new ValueAnchorNode(null));
        }
    }

    protected LogicNode createInstanceOf(TypeReference __type, ValueNode __object, JavaTypeProfile __profile)
    {
        return InstanceOfNode.create(__type, __object, __profile, createAnchor(__profile));
    }

    protected LogicNode createInstanceOfAllowNull(TypeReference __type, ValueNode __object, JavaTypeProfile __profile)
    {
        return InstanceOfNode.createAllowNull(__type, __object, __profile, createAnchor(__profile));
    }

    protected ValueNode genConditional(ValueNode __x)
    {
        return ConditionalNode.create((LogicNode) __x, NodeView.DEFAULT);
    }

    protected NewInstanceNode createNewInstance(ResolvedJavaType __type, boolean __fillContents)
    {
        return new NewInstanceNode(__type, __fillContents);
    }

    protected NewArrayNode createNewArray(ResolvedJavaType __elementType, ValueNode __length, boolean __fillContents)
    {
        return new NewArrayNode(__elementType, __length, __fillContents);
    }

    protected NewMultiArrayNode createNewMultiArray(ResolvedJavaType __type, ValueNode[] __dimensions)
    {
        return new NewMultiArrayNode(__type, __dimensions);
    }

    protected ValueNode genLoadField(ValueNode __receiver, ResolvedJavaField __field)
    {
        StampPair __stamp = graphBuilderConfig.getPlugins().getOverridingStamp(this, __field.getType(), false);
        if (__stamp == null)
        {
            return LoadFieldNode.create(getConstantFieldProvider(), getConstantReflection(), getMetaAccess(), getAssumptions(), __receiver, __field, false, false);
        }
        else
        {
            return LoadFieldNode.createOverrideStamp(getConstantFieldProvider(), getConstantReflection(), getMetaAccess(), __stamp, __receiver, __field, false, false);
        }
    }

    protected StateSplitProxyNode genVolatileFieldReadProxy(ValueNode __fieldRead)
    {
        return new StateSplitProxyNode(__fieldRead);
    }

    protected ValueNode emitExplicitNullCheck(ValueNode __receiver)
    {
        if (StampTool.isPointerNonNull(__receiver.stamp(NodeView.DEFAULT)))
        {
            return __receiver;
        }
        BytecodeExceptionNode __exception = graph.add(new BytecodeExceptionNode(metaAccess, NullPointerException.class));
        AbstractBeginNode __falseSucc = graph.add(new BeginNode());
        ValueNode __nonNullReceiver = graph.addOrUniqueWithInputs(PiNode.create(__receiver, StampFactory.objectNonNull(), __falseSucc));
        append(new IfNode(graph.addOrUniqueWithInputs(IsNullNode.create(__receiver)), __exception, __falseSucc, BranchProbabilityNode.SLOW_PATH_PROBABILITY));
        lastInstr = __falseSucc;

        __exception.setStateAfter(createFrameState(bci(), __exception));
        __exception.setNext(handleException(__exception, bci(), false));
        return __nonNullReceiver;
    }

    protected void emitExplicitBoundsCheck(ValueNode __index, ValueNode __length)
    {
        AbstractBeginNode __trueSucc = graph.add(new BeginNode());
        BytecodeExceptionNode __exception = graph.add(new BytecodeExceptionNode(metaAccess, ArrayIndexOutOfBoundsException.class, __index));
        append(new IfNode(genUnique(IntegerBelowNode.create(constantReflection, metaAccess, null, __index, __length, NodeView.DEFAULT)), __trueSucc, __exception, BranchProbabilityNode.FAST_PATH_PROBABILITY));
        lastInstr = __trueSucc;

        __exception.setStateAfter(createFrameState(bci(), __exception));
        __exception.setNext(handleException(__exception, bci(), false));
    }

    protected ValueNode genArrayLength(ValueNode __x)
    {
        return ArrayLengthNode.create(__x, constantReflection);
    }

    protected void genStoreField(ValueNode __receiver, ResolvedJavaField __field, ValueNode __value)
    {
        StoreFieldNode __storeFieldNode = new StoreFieldNode(__receiver, __field, __value);
        append(__storeFieldNode);
        __storeFieldNode.setStateAfter(this.createFrameState(stream.nextBCI(), __storeFieldNode));
    }

    /**
     * Ensure that concrete classes are at least linked before generating an invoke. Interfaces may
     * never be linked so simply return true for them.
     *
     * @return true if the declared holder is an interface or is linked
     */
    private static boolean callTargetIsResolved(JavaMethod __target)
    {
        if (__target instanceof ResolvedJavaMethod)
        {
            ResolvedJavaMethod __resolvedTarget = (ResolvedJavaMethod) __target;
            ResolvedJavaType __resolvedType = __resolvedTarget.getDeclaringClass();
            return __resolvedType.isInterface() || __resolvedType.isLinked();
        }
        return false;
    }

    protected void genInvokeStatic(int __cpi, int __opcode)
    {
        JavaMethod __target = lookupMethod(__cpi, __opcode);
        genInvokeStatic(__target);
    }

    void genInvokeStatic(JavaMethod __target)
    {
        if (callTargetIsResolved(__target))
        {
            ResolvedJavaMethod __resolvedTarget = (ResolvedJavaMethod) __target;
            ResolvedJavaType __holder = __resolvedTarget.getDeclaringClass();
            if (!__holder.isInitialized() && GraalOptions.resolveClassBeforeStaticInvoke)
            {
                handleUnresolvedInvoke(__target, InvokeKind.Static);
            }
            else
            {
                ValueNode[] __args = frameState.popArguments(__resolvedTarget.getSignature().getParameterCount(false));
                Invoke __invoke = appendInvoke(InvokeKind.Static, __resolvedTarget, __args);
                if (__invoke != null)
                {
                    __invoke.setClassInit(null);
                }
            }
        }
        else
        {
            handleUnresolvedInvoke(__target, InvokeKind.Static);
        }
    }

    protected void genInvokeInterface(int __cpi, int __opcode)
    {
        JavaMethod __target = lookupMethod(__cpi, __opcode);
        genInvokeInterface(__target);
    }

    protected void genInvokeInterface(JavaMethod __target)
    {
        if (callTargetIsResolved(__target))
        {
            ValueNode[] __args = frameState.popArguments(__target.getSignature().getParameterCount(true));
            appendInvoke(InvokeKind.Interface, (ResolvedJavaMethod) __target, __args);
        }
        else
        {
            handleUnresolvedInvoke(__target, InvokeKind.Interface);
        }
    }

    protected void genInvokeDynamic(int __cpi, int __opcode)
    {
        JavaMethod __target = lookupMethod(__cpi, __opcode);
        genInvokeDynamic(__target);
    }

    void genInvokeDynamic(JavaMethod __target)
    {
        if (!(__target instanceof ResolvedJavaMethod) || !genDynamicInvokeHelper((ResolvedJavaMethod) __target, stream.readCPI4(), Bytecodes.INVOKEDYNAMIC))
        {
            handleUnresolvedInvoke(__target, InvokeKind.Static);
        }
    }

    protected void genInvokeVirtual(int __cpi, int __opcode)
    {
        JavaMethod __target = lookupMethod(__cpi, __opcode);
        genInvokeVirtual(__target);
    }

    private boolean genDynamicInvokeHelper(ResolvedJavaMethod __target, int __cpi, int __opcode)
    {
        InvokeDynamicPlugin __invokeDynamicPlugin = graphBuilderConfig.getPlugins().getInvokeDynamicPlugin();

        if (__opcode == Bytecodes.INVOKEVIRTUAL && __invokeDynamicPlugin != null && !__invokeDynamicPlugin.isResolvedDynamicInvoke(this, __cpi, __opcode))
        {
            // regular invokevirtual, let caller handle it
            return false;
        }

        JavaConstant __appendix = constantPool.lookupAppendix(__cpi, __opcode);
        if (__appendix != null)
        {
            ValueNode __appendixNode;

            if (__invokeDynamicPlugin != null)
            {
                __invokeDynamicPlugin.recordDynamicMethod(this, __cpi, __opcode, __target);

                // will perform runtime type checks and static initialization
                FrameState __stateBefore = frameState.create(bci(), getNonIntrinsicAncestor(), false, null, null);
                __appendixNode = __invokeDynamicPlugin.genAppendixNode(this, __cpi, __opcode, __appendix, __stateBefore);
            }
            else
            {
                __appendixNode = ConstantNode.forConstant(__appendix, metaAccess, graph);
            }

            frameState.push(JavaKind.Object, __appendixNode);
        }

        boolean __hasReceiver = (__opcode == Bytecodes.INVOKEDYNAMIC) ? false : !__target.isStatic();
        ValueNode[] __args = frameState.popArguments(__target.getSignature().getParameterCount(__hasReceiver));
        if (__hasReceiver)
        {
            appendInvoke(InvokeKind.Virtual, __target, __args);
        }
        else
        {
            appendInvoke(InvokeKind.Static, __target, __args);
        }

        return true;
    }

    void genInvokeVirtual(JavaMethod __target)
    {
        if (!genInvokeVirtualHelper(__target))
        {
            handleUnresolvedInvoke(__target, InvokeKind.Virtual);
        }
    }

    private boolean genInvokeVirtualHelper(JavaMethod __target)
    {
        if (!callTargetIsResolved(__target))
        {
            return false;
        }

        ResolvedJavaMethod __resolvedTarget = (ResolvedJavaMethod) __target;
        int __cpi = stream.readCPI();

        /*
         * Special handling for runtimes that rewrite an invocation of MethodHandle.invoke(...) or
         * MethodHandle.invokeExact(...) to a static adapter. HotSpot does this - see
         * https://wiki.openjdk.java.net/display/HotSpot/Method+handles+and+invokedynamic
         */

        if (genDynamicInvokeHelper(__resolvedTarget, __cpi, Bytecodes.INVOKEVIRTUAL))
        {
            return true;
        }

        ValueNode[] __args = frameState.popArguments(__target.getSignature().getParameterCount(true));
        appendInvoke(InvokeKind.Virtual, (ResolvedJavaMethod) __target, __args);

        return true;
    }

    protected void genInvokeSpecial(int __cpi, int __opcode)
    {
        JavaMethod __target = lookupMethod(__cpi, __opcode);
        genInvokeSpecial(__target);
    }

    void genInvokeSpecial(JavaMethod __target)
    {
        if (callTargetIsResolved(__target))
        {
            ValueNode[] __args = frameState.popArguments(__target.getSignature().getParameterCount(true));
            appendInvoke(InvokeKind.Special, (ResolvedJavaMethod) __target, __args);
        }
        else
        {
            handleUnresolvedInvoke(__target, InvokeKind.Special);
        }
    }

    // @class BytecodeParser.CurrentInvoke
    static final class CurrentInvoke
    {
        // @field
        final ValueNode[] args;
        // @field
        final InvokeKind kind;
        // @field
        final JavaType returnType;

        // @cons
        CurrentInvoke(ValueNode[] __args, InvokeKind __kind, JavaType __returnType)
        {
            super();
            this.args = __args;
            this.kind = __kind;
            this.returnType = __returnType;
        }
    }

    // @field
    private CurrentInvoke currentInvoke;
    // @field
    protected FrameStateBuilder frameState;
    // @field
    protected BciBlock currentBlock;
    // @field
    protected final BytecodeStream stream;
    // @field
    protected final GraphBuilderConfiguration graphBuilderConfig;
    // @field
    protected final ResolvedJavaMethod method;
    // @field
    protected final Bytecode code;
    // @field
    protected final BytecodeProvider bytecodeProvider;
    // @field
    protected final ProfilingInfo profilingInfo;
    // @field
    protected final OptimisticOptimizations optimisticOpts;
    // @field
    protected final ConstantPool constantPool;
    // @field
    protected final MetaAccessProvider metaAccess;
    // @field
    private final ConstantReflectionProvider constantReflection;
    // @field
    private final ConstantFieldProvider constantFieldProvider;
    // @field
    private final StampProvider stampProvider;
    // @field
    protected final IntrinsicContext intrinsicContext;

    @Override
    public InvokeKind getInvokeKind()
    {
        return currentInvoke == null ? null : currentInvoke.kind;
    }

    @Override
    public JavaType getInvokeReturnType()
    {
        return currentInvoke == null ? null : currentInvoke.returnType;
    }

    // @field
    private boolean forceInliningEverything;

    @Override
    public void handleReplacedInvoke(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __args, boolean __inlineEverything)
    {
        boolean __previous = forceInliningEverything;
        forceInliningEverything = __previous || __inlineEverything;
        try
        {
            appendInvoke(__invokeKind, __targetMethod, __args);
        }
        finally
        {
            forceInliningEverything = __previous;
        }
    }

    @Override
    public void handleReplacedInvoke(CallTargetNode __callTarget, JavaKind __resultType)
    {
        BytecodeParser __intrinsicCallSiteParser = getNonIntrinsicAncestor();
        ExceptionEdgeAction __exceptionEdgeAction = __intrinsicCallSiteParser == null ? getActionForInvokeExceptionEdge(null) : __intrinsicCallSiteParser.getActionForInvokeExceptionEdge(null);
        createNonInlinedInvoke(__exceptionEdgeAction, bci(), __callTarget, __resultType);
    }

    protected Invoke appendInvoke(InvokeKind __initialInvokeKind, ResolvedJavaMethod __initialTargetMethod, ValueNode[] __args)
    {
        ResolvedJavaMethod __targetMethod = __initialTargetMethod;
        InvokeKind __invokeKind = __initialInvokeKind;
        if (__initialInvokeKind.isIndirect())
        {
            ResolvedJavaType __contextType = this.frameState.getMethod().getDeclaringClass();
            ResolvedJavaMethod __specialCallTarget = MethodCallTargetNode.findSpecialCallTarget(__initialInvokeKind, __args[0], __initialTargetMethod, __contextType);
            if (__specialCallTarget != null)
            {
                __invokeKind = InvokeKind.Special;
                __targetMethod = __specialCallTarget;
            }
        }

        JavaKind __resultType = __targetMethod.getSignature().getReturnKind();
        if (!parsingIntrinsic() && GraalOptions.deoptALot)
        {
            append(new DeoptimizeNode(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint));
            frameState.pushReturn(__resultType, ConstantNode.defaultForKind(__resultType, graph));
            return null;
        }

        JavaType __returnType = __targetMethod.getSignature().getReturnType(method.getDeclaringClass());
        if (graphBuilderConfig.eagerResolving() || parsingIntrinsic())
        {
            __returnType = __returnType.resolve(__targetMethod.getDeclaringClass());
        }
        if (__invokeKind.hasReceiver())
        {
            __args[0] = emitExplicitExceptions(__args[0]);
        }

        if (__initialInvokeKind == InvokeKind.Special && !__targetMethod.isConstructor())
        {
            emitCheckForInvokeSuperSpecial(__args);
        }

        InlineInfo __inlineInfo = null;
        try
        {
            currentInvoke = new CurrentInvoke(__args, __invokeKind, __returnType);
            if (tryNodePluginForInvocation(__args, __targetMethod))
            {
                return null;
            }

            if (__invokeKind.hasReceiver() && __args[0].isNullConstant())
            {
                append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NullCheckException));
                return null;
            }

            if (!__invokeKind.isIndirect() || GraalOptions.useGuardedIntrinsics)
            {
                if (tryInvocationPlugin(__invokeKind, __args, __targetMethod, __resultType, __returnType))
                {
                    return null;
                }
            }
            if (__invokeKind.isDirect())
            {
                __inlineInfo = tryInline(__args, __targetMethod);
                if (__inlineInfo == SUCCESSFULLY_INLINED)
                {
                    return null;
                }
            }
        }
        finally
        {
            currentInvoke = null;
        }

        int __invokeBci = bci();
        JavaTypeProfile __profile = getProfileForInvoke(__invokeKind);
        ExceptionEdgeAction __edgeAction = getActionForInvokeExceptionEdge(__inlineInfo);
        boolean __partialIntrinsicExit = false;
        if (intrinsicContext != null && intrinsicContext.isCallToOriginal(__targetMethod))
        {
            __partialIntrinsicExit = true;
            ResolvedJavaMethod __originalMethod = intrinsicContext.getOriginalMethod();
            BytecodeParser __intrinsicCallSiteParser = getNonIntrinsicAncestor();
            if (__intrinsicCallSiteParser != null)
            {
                // When exiting a partial intrinsic, the invoke to the original
                // must use the same context as the call to the intrinsic.
                __invokeBci = __intrinsicCallSiteParser.bci();
                __profile = __intrinsicCallSiteParser.getProfileForInvoke(__invokeKind);
                __edgeAction = __intrinsicCallSiteParser.getActionForInvokeExceptionEdge(__inlineInfo);
            }
            else
            {
                // We are parsing the intrinsic for the root compilation or for inlining.
                // This call is a partial intrinsic exit, and we do not have profile information
                // for this callsite. We also have to assume that the call needs an exception
                // edge. Finally, we know that this intrinsic is parsed for late inlining,
                // so the bci must be set to unknown, so that the inliner patches it later.
                __invokeBci = BytecodeFrame.UNKNOWN_BCI;
                __profile = null;
                __edgeAction = graph.method().getAnnotation(Snippet.class) == null ? ExceptionEdgeAction.INCLUDE_AND_HANDLE : ExceptionEdgeAction.OMIT;
            }

            if (__originalMethod.isStatic())
            {
                __invokeKind = InvokeKind.Static;
            }
            else
            {
                // The original call to the intrinsic must have been devirtualized,
                // otherwise we wouldn't be here.
                __invokeKind = InvokeKind.Special;
            }
            Signature __sig = __originalMethod.getSignature();
            __returnType = __sig.getReturnType(method.getDeclaringClass());
            __resultType = __sig.getReturnKind();
            __targetMethod = __originalMethod;
        }
        Invoke __invoke = createNonInlinedInvoke(__edgeAction, __invokeBci, __args, __targetMethod, __invokeKind, __resultType, __returnType, __profile);
        if (__partialIntrinsicExit)
        {
            // This invoke must never be later inlined as it might select the intrinsic graph.
            // Until there is a mechanism to guarantee that any late inlining will not select
            // the intrinsic graph, prevent this invoke from being inlined.
            __invoke.setUseForInlining(false);
        }
        return __invoke;
    }

    /**
     * Checks that the class of the receiver of an {@link Bytecodes#INVOKESPECIAL} in a method
     * declared in an interface (i.e., a default method) is assignable to the interface. If not,
     * then deoptimize so that the interpreter can throw an {@link IllegalAccessError}.
     *
     * This is a check not performed by the verifier and so must be performed at runtime.
     *
     * @param args arguments to an {@link Bytecodes#INVOKESPECIAL} implementing a direct call to a
     *            method in a super class
     */
    protected void emitCheckForInvokeSuperSpecial(ValueNode[] __args)
    {
        ResolvedJavaType __callingClass = method.getDeclaringClass();
        if (__callingClass.getHostClass() != null)
        {
            __callingClass = __callingClass.getHostClass();
        }
        if (__callingClass.isInterface())
        {
            ValueNode __receiver = __args[0];
            TypeReference __checkedType = TypeReference.createTrusted(graph.getAssumptions(), __callingClass);
            LogicNode __condition = genUnique(createInstanceOf(__checkedType, __receiver, null));
            FixedGuardNode __fixedGuard = append(new FixedGuardNode(__condition, DeoptimizationReason.ClassCastException, DeoptimizationAction.None, false));
            __args[0] = append(PiNode.create(__receiver, StampFactory.object(__checkedType, true), __fixedGuard));
        }
    }

    protected JavaTypeProfile getProfileForInvoke(InvokeKind __invokeKind)
    {
        if (__invokeKind.isIndirect() && profilingInfo != null && this.optimisticOpts.useTypeCheckHints())
        {
            return profilingInfo.getTypeProfile(bci());
        }
        return null;
    }

    /**
     * A partial intrinsic exits by (effectively) calling the intrinsified method. This call must
     * use exactly the arguments to the call being intrinsified.
     *
     * @param originalArgs arguments of original call to intrinsified method
     * @param recursiveArgs arguments of recursive call to intrinsified method
     */
    private static boolean checkPartialIntrinsicExit(ValueNode[] __originalArgs, ValueNode[] __recursiveArgs)
    {
        if (__originalArgs != null)
        {
            for (int __i = 0; __i < __originalArgs.length; __i++)
            {
                ValueNode __arg = GraphUtil.unproxify(__recursiveArgs[__i]);
                ValueNode __icArg = GraphUtil.unproxify(__originalArgs[__i]);
            }
        }
        else
        {
            for (int __i = 0; __i < __recursiveArgs.length; __i++)
            {
                ValueNode __arg = GraphUtil.unproxify(__recursiveArgs[__i]);
            }
        }
        return true;
    }

    protected Invoke createNonInlinedInvoke(ExceptionEdgeAction __exceptionEdge, int __invokeBci, ValueNode[] __invokeArgs, ResolvedJavaMethod __targetMethod, InvokeKind __invokeKind, JavaKind __resultType, JavaType __returnType, JavaTypeProfile __profile)
    {
        StampPair __returnStamp = graphBuilderConfig.getPlugins().getOverridingStamp(this, __returnType, false);
        if (__returnStamp == null)
        {
            __returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), __returnType, false);
        }

        MethodCallTargetNode __callTarget = graph.add(createMethodCallTarget(__invokeKind, __targetMethod, __invokeArgs, __returnStamp, __profile));
        Invoke __invoke = createNonInlinedInvoke(__exceptionEdge, __invokeBci, __callTarget, __resultType);

        for (InlineInvokePlugin __plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            __plugin.notifyNotInlined(this, __targetMethod, __invoke);
        }

        return __invoke;
    }

    protected Invoke createNonInlinedInvoke(ExceptionEdgeAction __exceptionEdge, int __invokeBci, CallTargetNode __callTarget, JavaKind __resultType)
    {
        if (__exceptionEdge == ExceptionEdgeAction.OMIT)
        {
            return createInvoke(__invokeBci, __callTarget, __resultType);
        }
        else
        {
            Invoke __invoke = createInvokeWithException(__invokeBci, __callTarget, __resultType, __exceptionEdge);
            AbstractBeginNode __beginNode = graph.add(KillingBeginNode.create(LocationIdentity.any()));
            __invoke.setNext(__beginNode);
            lastInstr = __beginNode;
            return __invoke;
        }
    }

    /**
     * Describes what should be done with the exception edge of an invocation. The edge can be
     * omitted or included. An included edge can handle the exception or transfer execution to the
     * interpreter for handling (deoptimize).
     */
    // @enum BytecodeParser.ExceptionEdgeAction
    protected enum ExceptionEdgeAction
    {
        OMIT,
        INCLUDE_AND_HANDLE,
        INCLUDE_AND_DEOPTIMIZE
    }

    protected ExceptionEdgeAction getActionForInvokeExceptionEdge(InlineInfo __lastInlineInfo)
    {
        if (__lastInlineInfo == InlineInfo.DO_NOT_INLINE_WITH_EXCEPTION)
        {
            return ExceptionEdgeAction.INCLUDE_AND_HANDLE;
        }
        else if (__lastInlineInfo == InlineInfo.DO_NOT_INLINE_NO_EXCEPTION)
        {
            return ExceptionEdgeAction.OMIT;
        }
        else if (__lastInlineInfo == InlineInfo.DO_NOT_INLINE_DEOPTIMIZE_ON_EXCEPTION)
        {
            return ExceptionEdgeAction.INCLUDE_AND_DEOPTIMIZE;
        }
        else if (graphBuilderConfig.getBytecodeExceptionMode() == BytecodeExceptionMode.CheckAll)
        {
            return ExceptionEdgeAction.INCLUDE_AND_HANDLE;
        }
        else if (graphBuilderConfig.getBytecodeExceptionMode() == BytecodeExceptionMode.ExplicitOnly)
        {
            return ExceptionEdgeAction.INCLUDE_AND_HANDLE;
        }
        else if (graphBuilderConfig.getBytecodeExceptionMode() == BytecodeExceptionMode.OmitAll)
        {
            return ExceptionEdgeAction.OMIT;
        }
        else
        {
            // be conservative if information was not recorded (could result in endless
            // recompiles otherwise)
            if (!GraalOptions.stressInvokeWithExceptionNode)
            {
                if (optimisticOpts.useExceptionProbability())
                {
                    if (profilingInfo != null)
                    {
                        TriState __exceptionSeen = profilingInfo.getExceptionSeen(bci());
                        if (__exceptionSeen == TriState.FALSE)
                        {
                            return ExceptionEdgeAction.OMIT;
                        }
                    }
                }
            }
            return ExceptionEdgeAction.INCLUDE_AND_HANDLE;
        }
    }

    // @class BytecodeParser.IntrinsicGuard
    protected static final class IntrinsicGuard
    {
        // @field
        final FixedWithNextNode lastInstr;
        // @field
        final Mark mark;
        // @field
        final AbstractBeginNode nonIntrinsicBranch;
        // @field
        final ValueNode receiver;
        // @field
        final JavaTypeProfile profile;

        // @cons
        public IntrinsicGuard(FixedWithNextNode __lastInstr, ValueNode __receiver, Mark __mark, AbstractBeginNode __nonIntrinsicBranch, JavaTypeProfile __profile)
        {
            super();
            this.lastInstr = __lastInstr;
            this.receiver = __receiver;
            this.mark = __mark;
            this.nonIntrinsicBranch = __nonIntrinsicBranch;
            this.profile = __profile;
        }
    }

    /**
     * Weaves a test of the receiver type to ensure the dispatch will select {@code targetMethod}
     * and not another method that overrides it. This should only be called if there is an
     * {@link InvocationPlugin} for {@code targetMethod} and the invocation is indirect.
     *
     * The control flow woven around the intrinsic is as follows:
     *
     * <pre>
     *  if (LoadMethod(LoadHub(receiver)) == targetMethod) {
     *       <intrinsic for targetMethod>
     *  } else {
     *       <virtual call to targetMethod>
     *  }
     * </pre>
     *
     * The {@code else} branch is woven by {@link #afterInvocationPluginExecution}.
     *
     * @return {@code null} if the intrinsic cannot be used otherwise an object to be used by
     *         {@link #afterInvocationPluginExecution} to weave code for the non-intrinsic branch
     */
    protected IntrinsicGuard guardIntrinsic(ValueNode[] __args, ResolvedJavaMethod __targetMethod, InvocationPluginReceiver __pluginReceiver)
    {
        ValueNode __intrinsicReceiver = __args[0];
        ResolvedJavaType __receiverType = StampTool.typeOrNull(__intrinsicReceiver);
        if (__receiverType == null)
        {
            // the verifier guarantees it to be at least type declaring targetMethod
            __receiverType = __targetMethod.getDeclaringClass();
        }
        ResolvedJavaMethod __resolvedMethod = __receiverType.resolveMethod(__targetMethod, method.getDeclaringClass());
        if (__resolvedMethod == null || __resolvedMethod.equals(__targetMethod))
        {
            Mark __mark = graph.getMark();
            FixedWithNextNode __currentLastInstr = lastInstr;
            ValueNode __nonNullReceiver = __pluginReceiver.get();
            Stamp __methodStamp = stampProvider.createMethodStamp();
            LoadHubNode __hub = graph.unique(new LoadHubNode(stampProvider, __nonNullReceiver));
            LoadMethodNode __actual = append(new LoadMethodNode(__methodStamp, __targetMethod, __receiverType, method.getDeclaringClass(), __hub));
            ConstantNode __expected = graph.unique(ConstantNode.forConstant(__methodStamp, __targetMethod.getEncoding(), getMetaAccess()));
            LogicNode __compare = graph.addOrUniqueWithInputs(CompareNode.createCompareNode(constantReflection, metaAccess, null, CanonicalCondition.EQ, __actual, __expected, NodeView.DEFAULT));

            JavaTypeProfile __profile = null;
            if (profilingInfo != null && this.optimisticOpts.useTypeCheckHints())
            {
                __profile = profilingInfo.getTypeProfile(bci());
                if (__profile != null)
                {
                    JavaTypeProfile __newProfile = adjustProfileForInvocationPlugin(__profile, __targetMethod);
                    if (__newProfile != __profile)
                    {
                        if (__newProfile.getTypes().length == 0)
                        {
                            // all profiled types select the intrinsic, so emit a fixed guard instead of an if-then-else
                            lastInstr = append(new FixedGuardNode(__compare, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile, false));
                            return new IntrinsicGuard(__currentLastInstr, __intrinsicReceiver, __mark, null, null);
                        }
                    }
                    else
                    {
                        // no profiled types select the intrinsic, so emit a virtual call
                        return null;
                    }
                    __profile = __newProfile;
                }
            }

            AbstractBeginNode __intrinsicBranch = graph.add(new BeginNode());
            AbstractBeginNode __nonIntrinsicBranch = graph.add(new BeginNode());
            append(new IfNode(__compare, __intrinsicBranch, __nonIntrinsicBranch, BranchProbabilityNode.FAST_PATH_PROBABILITY));
            lastInstr = __intrinsicBranch;
            return new IntrinsicGuard(__currentLastInstr, __intrinsicReceiver, __mark, __nonIntrinsicBranch, __profile);
        }
        else
        {
            // receiver selects an overriding method, so emit a virtual call
            return null;
        }
    }

    /**
     * Adjusts the profile for an indirect invocation of a virtual method for which there is an
     * intrinsic. The adjustment made by this method is to remove all types from the profile that do
     * not override {@code targetMethod}.
     *
     * @param profile the profile to adjust
     * @param targetMethod the virtual method for which there is an intrinsic
     * @return the adjusted profile or the original {@code profile} object if no adjustment was made
     */
    protected JavaTypeProfile adjustProfileForInvocationPlugin(JavaTypeProfile __profile, ResolvedJavaMethod __targetMethod)
    {
        if (__profile.getTypes().length > 0)
        {
            List<ProfiledType> __retained = new ArrayList<>();
            double __notRecordedProbability = __profile.getNotRecordedProbability();
            for (ProfiledType __ptype : __profile.getTypes())
            {
                if (!__ptype.getType().resolveMethod(__targetMethod, method.getDeclaringClass()).equals(__targetMethod))
                {
                    __retained.add(__ptype);
                }
                else
                {
                    __notRecordedProbability += __ptype.getProbability();
                }
            }
            if (!__retained.isEmpty())
            {
                if (__retained.size() != __profile.getTypes().length)
                {
                    return new JavaTypeProfile(__profile.getNullSeen(), __notRecordedProbability, __retained.toArray(new ProfiledType[__retained.size()]));
                }
            }
            else
            {
                return new JavaTypeProfile(__profile.getNullSeen(), __notRecordedProbability, new ProfiledType[0]);
            }
        }
        return __profile;
    }

    /**
     * Performs any action required after execution of an invocation plugin.
     * This includes checking invocation plugin invariants as well as weaving the {@code else}
     * branch of the code woven by {@link #guardIntrinsic} if {@code guard != null}.
     */
    protected void afterInvocationPluginExecution(boolean __pluginHandledInvoke, IntrinsicGuard __intrinsicGuard, InvokeKind __invokeKind, ValueNode[] __args, ResolvedJavaMethod __targetMethod, JavaKind __resultType, JavaType __returnType)
    {
        if (__intrinsicGuard != null)
        {
            if (__pluginHandledInvoke)
            {
                if (__intrinsicGuard.nonIntrinsicBranch != null)
                {
                    // Intrinsic emitted: emit a virtual call to the target method and
                    // merge it with the intrinsic branch.
                    EndNode __intrinsicEnd = append(new EndNode());

                    FrameStateBuilder __intrinsicState = null;
                    FrameStateBuilder __nonIntrinisicState = null;
                    if (__resultType != JavaKind.Void)
                    {
                        __intrinsicState = frameState.copy();
                        frameState.pop(__resultType);
                        __nonIntrinisicState = frameState;
                    }

                    lastInstr = __intrinsicGuard.nonIntrinsicBranch;
                    createNonInlinedInvoke(getActionForInvokeExceptionEdge(null), bci(), __args, __targetMethod, __invokeKind, __resultType, __returnType, __intrinsicGuard.profile);

                    EndNode __nonIntrinsicEnd = append(new EndNode());
                    AbstractMergeNode __mergeNode = graph.add(new MergeNode());

                    __mergeNode.addForwardEnd(__intrinsicEnd);
                    if (__intrinsicState != null)
                    {
                        __intrinsicState.merge(__mergeNode, __nonIntrinisicState);
                        frameState = __intrinsicState;
                    }
                    __mergeNode.addForwardEnd(__nonIntrinsicEnd);
                    __mergeNode.setStateAfter(frameState.create(stream.nextBCI(), __mergeNode));

                    lastInstr = __mergeNode;
                }
            }
            else
            {
                // Intrinsic was not applied: remove intrinsic guard
                // and restore the original receiver node in the arguments array.
                __intrinsicGuard.lastInstr.setNext(null);
                GraphUtil.removeNewNodes(graph, __intrinsicGuard.mark);
                lastInstr = __intrinsicGuard.lastInstr;
                __args[0] = __intrinsicGuard.receiver;
            }
        }
    }

    protected boolean tryInvocationPlugin(InvokeKind __invokeKind, ValueNode[] __args, ResolvedJavaMethod __targetMethod, JavaKind __resultType, JavaType __returnType)
    {
        InvocationPlugin __plugin = graphBuilderConfig.getPlugins().getInvocationPlugins().lookupInvocation(__targetMethod);
        if (__plugin != null)
        {
            if (intrinsicContext != null && intrinsicContext.isCallToOriginal(__targetMethod))
            {
                // Self recursive intrinsic means the original method should be called.
                return false;
            }

            InvocationPluginReceiver __pluginReceiver = invocationPluginReceiver.init(__targetMethod, __args);

            IntrinsicGuard __intrinsicGuard = null;
            if (__invokeKind.isIndirect())
            {
                __intrinsicGuard = guardIntrinsic(__args, __targetMethod, __pluginReceiver);
                if (__intrinsicGuard == null)
                {
                    return false;
                }
            }

            if (__plugin.execute(this, __targetMethod, __pluginReceiver, __args))
            {
                afterInvocationPluginExecution(true, __intrinsicGuard, __invokeKind, __args, __targetMethod, __resultType, __returnType);
                return !__plugin.isDecorator();
            }
            else
            {
                afterInvocationPluginExecution(false, __intrinsicGuard, __invokeKind, __args, __targetMethod, __resultType, __returnType);
            }
        }
        return false;
    }

    private boolean tryNodePluginForInvocation(ValueNode[] __args, ResolvedJavaMethod __targetMethod)
    {
        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleInvoke(this, __targetMethod, __args))
            {
                return true;
            }
        }
        return false;
    }

    // @def
    private static final InlineInfo SUCCESSFULLY_INLINED = InlineInfo.createStandardInlineInfo(null);

    /**
     * Try to inline a method. If the method was inlined, returns {@link #SUCCESSFULLY_INLINED}.
     * Otherwise, it returns the {@link InlineInfo} that lead to the decision to not inline it, or
     * {@code null} if there is no {@link InlineInfo} for this method.
     */
    private InlineInfo tryInline(ValueNode[] __args, ResolvedJavaMethod __targetMethod)
    {
        boolean __canBeInlined = forceInliningEverything || parsingIntrinsic() || __targetMethod.canBeInlined();
        if (!__canBeInlined)
        {
            return null;
        }

        if (forceInliningEverything)
        {
            if (inline(__targetMethod, __targetMethod, null, __args))
            {
                return SUCCESSFULLY_INLINED;
            }
            else
            {
                return null;
            }
        }

        for (InlineInvokePlugin __plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            InlineInfo __inlineInfo = __plugin.shouldInlineInvoke(this, __targetMethod, __args);
            if (__inlineInfo != null)
            {
                if (__inlineInfo.getMethodToInline() != null)
                {
                    if (inline(__targetMethod, __inlineInfo.getMethodToInline(), __inlineInfo.getIntrinsicBytecodeProvider(), __args))
                    {
                        return SUCCESSFULLY_INLINED;
                    }
                    __inlineInfo = null;
                }
                // Do not inline, and do not ask the remaining plugins.
                return __inlineInfo;
            }
        }

        // There was no inline plugin with a definite answer to whether or not to inline.
        // If we're parsing an intrinsic, then we need to enforce the invariant here
        // that methods are always force inlined in intrinsics/snippets.
        if (parsingIntrinsic())
        {
            if (inline(__targetMethod, __targetMethod, this.bytecodeProvider, __args))
            {
                return SUCCESSFULLY_INLINED;
            }
        }
        return null;
    }

    // @def
    private static final int ACCESSOR_BYTECODE_LENGTH = 5;

    /**
     * Tries to inline {@code targetMethod} if it is an instance field accessor. This avoids the
     * overhead of creating and using a nested {@link BytecodeParser} object.
     */
    private boolean tryFastInlineAccessor(ValueNode[] __args, ResolvedJavaMethod __targetMethod)
    {
        byte[] __bytecode = __targetMethod.getCode();
        if (__bytecode != null && __bytecode.length == ACCESSOR_BYTECODE_LENGTH && Bytes.beU1(__bytecode, 0) == Bytecodes.ALOAD_0 && Bytes.beU1(__bytecode, 1) == Bytecodes.GETFIELD)
        {
            int __b4 = Bytes.beU1(__bytecode, 4);
            if (__b4 >= Bytecodes.IRETURN && __b4 <= Bytecodes.ARETURN)
            {
                int __cpi = Bytes.beU2(__bytecode, 2);
                JavaField __field = __targetMethod.getConstantPool().lookupField(__cpi, __targetMethod, Bytecodes.GETFIELD);
                if (__field instanceof ResolvedJavaField)
                {
                    ValueNode __receiver = invocationPluginReceiver.init(__targetMethod, __args).get();
                    ResolvedJavaField __resolvedField = (ResolvedJavaField) __field;
                    genGetField(__resolvedField, __receiver);
                    notifyBeforeInline(__targetMethod);
                    notifyAfterInline(__targetMethod);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean intrinsify(BytecodeProvider __intrinsicBytecodeProvider, ResolvedJavaMethod __targetMethod, ResolvedJavaMethod __substitute, InvocationPlugin.Receiver __receiver, ValueNode[] __args)
    {
        if (__receiver != null)
        {
            __receiver.get();
        }
        return inline(__targetMethod, __substitute, __intrinsicBytecodeProvider, __args);
    }

    private boolean inline(ResolvedJavaMethod __targetMethod, ResolvedJavaMethod __inlinedMethod, BytecodeProvider __intrinsicBytecodeProvider, ValueNode[] __args)
    {
        IntrinsicContext __intrinsic = this.intrinsicContext;

        if (__intrinsic == null && __targetMethod.equals(__inlinedMethod) && (__targetMethod.getModifiers() & (Modifier.STATIC | Modifier.SYNCHRONIZED)) == 0 && tryFastInlineAccessor(__args, __targetMethod))
        {
            return true;
        }

        if (__intrinsic != null && __intrinsic.isCallToOriginal(__targetMethod))
        {
            if (__intrinsic.isCompilationRoot())
            {
                // A root compiled intrinsic needs to deoptimize if the slow path is taken. During frame state
                // assignment, the deopt node will get its stateBefore from the start node of the intrinsic.
                append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.RuntimeConstraint));
                return true;
            }
            else
            {
                if (__intrinsic.getOriginalMethod().isNative())
                {
                    return false;
                }
                if (canInlinePartialIntrinsicExit() && GraalOptions.inlinePartialIntrinsicExitDuringParsing)
                {
                    // Otherwise inline the original method. Any frame state created during the inlining
                    // will exclude frame(s) in the intrinsic method (see FrameStateBuilder.create(int bci)).
                    notifyBeforeInline(__inlinedMethod);
                    parseAndInlineCallee(__intrinsic.getOriginalMethod(), __args, null);
                    notifyAfterInline(__inlinedMethod);
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            boolean __isIntrinsic = __intrinsicBytecodeProvider != null;
            if (__intrinsic == null && __isIntrinsic)
            {
                __intrinsic = new IntrinsicContext(__targetMethod, __inlinedMethod, __intrinsicBytecodeProvider, CompilationContext.INLINE_DURING_PARSING);
            }
            if (__inlinedMethod.hasBytecodes())
            {
                notifyBeforeInline(__inlinedMethod);
                parseAndInlineCallee(__inlinedMethod, __args, __intrinsic);
                notifyAfterInline(__inlinedMethod);
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    protected void notifyBeforeInline(ResolvedJavaMethod __inlinedMethod)
    {
        for (InlineInvokePlugin __plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            __plugin.notifyBeforeInline(__inlinedMethod);
        }
    }

    protected void notifyAfterInline(ResolvedJavaMethod __inlinedMethod)
    {
        for (InlineInvokePlugin __plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            __plugin.notifyAfterInline(__inlinedMethod);
        }
    }

    /**
     * Determines if a partial intrinsic exit (i.e., a call to the original method within an
     * intrinsic) can be inlined.
     */
    protected boolean canInlinePartialIntrinsicExit()
    {
        return true;
    }

    protected void parseAndInlineCallee(ResolvedJavaMethod __targetMethod, ValueNode[] __args, IntrinsicContext __calleeIntrinsicContext)
    {
        FixedWithNextNode __calleeBeforeUnwindNode = null;
        ValueNode __calleeUnwindValue = null;

        try (IntrinsicScope __s = __calleeIntrinsicContext != null && !parsingIntrinsic() ? new IntrinsicScope(this, __targetMethod.getSignature().toParameterKinds(!__targetMethod.isStatic()), __args) : null)
        {
            BytecodeParser __parser = graphBuilderInstance.createBytecodeParser(graph, this, __targetMethod, JVMCICompiler.INVOCATION_ENTRY_BCI, __calleeIntrinsicContext);
            FrameStateBuilder __startFrameState = new FrameStateBuilder(__parser, __parser.code, graph);
            if (!__targetMethod.isStatic())
            {
                __args[0] = nullCheckedValue(__args[0]);
            }
            __startFrameState.initializeFromArgumentsArray(__args);
            __parser.build(this.lastInstr, __startFrameState);

            if (__parser.returnDataList == null)
            {
                // Callee does not return.
                lastInstr = null;
            }
            else
            {
                ValueNode __calleeReturnValue;
                MergeNode __returnMergeNode = null;
                if (__s != null)
                {
                    __s.returnDataList = __parser.returnDataList;
                }
                if (__parser.returnDataList.size() == 1)
                {
                    // Callee has a single return, we can continue parsing at that point.
                    ReturnToCallerData __singleReturnData = __parser.returnDataList.get(0);
                    lastInstr = __singleReturnData.beforeReturnNode;
                    __calleeReturnValue = __singleReturnData.returnValue;
                }
                else
                {
                    // Callee has multiple returns, we need to insert a control flow merge.
                    __returnMergeNode = graph.add(new MergeNode());
                    __calleeReturnValue = ValueMergeUtil.mergeValueProducers(__returnMergeNode, __parser.returnDataList, __returnData -> __returnData.beforeReturnNode, __returnData -> __returnData.returnValue);
                }

                if (__calleeReturnValue != null)
                {
                    frameState.push(__targetMethod.getSignature().getReturnKind().getStackKind(), __calleeReturnValue);
                }
                if (__returnMergeNode != null)
                {
                    __returnMergeNode.setStateAfter(createFrameState(stream.nextBCI(), __returnMergeNode));
                    lastInstr = finishInstruction(__returnMergeNode, frameState);
                }
            }
            // Propagate any side effects into the caller when parsing intrinsics.
            if (__parser.frameState.isAfterSideEffect() && parsingIntrinsic())
            {
                for (StateSplit __sideEffect : __parser.frameState.sideEffects())
                {
                    frameState.addSideEffect(__sideEffect);
                }
            }

            __calleeBeforeUnwindNode = __parser.getBeforeUnwindNode();
            if (__calleeBeforeUnwindNode != null)
            {
                __calleeUnwindValue = __parser.getUnwindValue();
            }
        }

        /*
         * Method handleException will call createTarget, which wires this exception edge to the
         * corresponding exception dispatch block in the caller. In the case where it wires to the
         * caller's unwind block, any FrameState created meanwhile, e.g. FrameState for LoopExitNode,
         * would be instantiated with AFTER_EXCEPTION_BCI. Such frame states should not be fixed by
         * IntrinsicScope.close, as they denote the states of the caller. Thus, the following code
         * should be placed outside the IntrinsicScope, so that correctly created FrameStates are not replaced.
         */
        if (__calleeBeforeUnwindNode != null)
        {
            __calleeBeforeUnwindNode.setNext(handleException(__calleeUnwindValue, bci(), false));
        }
    }

    public MethodCallTargetNode createMethodCallTarget(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __args, StampPair __returnStamp, JavaTypeProfile __profile)
    {
        return new MethodCallTargetNode(__invokeKind, __targetMethod, __args, __returnStamp, __profile);
    }

    protected InvokeNode createInvoke(int __invokeBci, CallTargetNode __callTarget, JavaKind __resultType)
    {
        InvokeNode __invoke = append(new InvokeNode(__callTarget, __invokeBci));
        frameState.pushReturn(__resultType, __invoke);
        __invoke.setStateAfter(createFrameState(stream.nextBCI(), __invoke));
        return __invoke;
    }

    protected InvokeWithExceptionNode createInvokeWithException(int __invokeBci, CallTargetNode __callTarget, JavaKind __resultType, ExceptionEdgeAction __exceptionEdgeAction)
    {
        if (currentBlock != null && stream.nextBCI() > currentBlock.endBci)
        {
            // Clear non-live locals early so that the exception handler entry gets the cleared state.
            frameState.clearNonLiveLocals(currentBlock, liveness, false);
        }

        AbstractBeginNode __exceptionEdge = handleException(null, bci(), __exceptionEdgeAction == ExceptionEdgeAction.INCLUDE_AND_DEOPTIMIZE);
        InvokeWithExceptionNode __invoke = append(new InvokeWithExceptionNode(__callTarget, __exceptionEdge, __invokeBci));
        frameState.pushReturn(__resultType, __invoke);
        __invoke.setStateAfter(createFrameState(stream.nextBCI(), __invoke));
        return __invoke;
    }

    protected void genReturn(ValueNode __returnVal, JavaKind __returnKind)
    {
        if (parsingIntrinsic() && __returnVal != null)
        {
            if (__returnVal instanceof StateSplit)
            {
                StateSplit __stateSplit = (StateSplit) __returnVal;
                FrameState __stateAfter = __stateSplit.stateAfter();
                if (__stateSplit.hasSideEffect())
                {
                    if (__stateAfter.bci == BytecodeFrame.AFTER_BCI)
                    {
                        __stateAfter.replaceAtUsages(graph.add(new FrameState(BytecodeFrame.AFTER_BCI, __returnVal)));
                        GraphUtil.killWithUnusedFloatingInputs(__stateAfter);
                    }
                    else
                    {
                        // This must be the return value from within a partial intrinsification.
                    }
                }
            }
        }

        ValueNode __realReturnVal = processReturnValue(__returnVal, __returnKind);

        frameState.setRethrowException(false);
        frameState.clearStack();
        beforeReturn(__realReturnVal, __returnKind);
        if (parent == null)
        {
            append(new ReturnNode(__realReturnVal));
        }
        else
        {
            if (returnDataList == null)
            {
                returnDataList = new ArrayList<>();
            }
            returnDataList.add(new ReturnToCallerData(__realReturnVal, lastInstr));
            lastInstr = null;
        }
    }

    private ValueNode processReturnValue(ValueNode __value, JavaKind __kind)
    {
        JavaKind __returnKind = method.getSignature().getReturnKind();
        if (__kind != __returnKind)
        {
            // sub-word integer
            IntegerStamp __stamp = (IntegerStamp) __value.stamp(NodeView.DEFAULT);

            // the bytecode verifier doesn't check that the value is in the correct range
            if (__stamp.lowerBound() < __returnKind.getMinValue() || __returnKind.getMaxValue() < __stamp.upperBound())
            {
                ValueNode __narrow = append(genNarrow(__value, __returnKind.getBitCount()));
                if (__returnKind.isUnsigned())
                {
                    return append(genZeroExtend(__narrow, 32));
                }
                else
                {
                    return append(genSignExtend(__narrow, 32));
                }
            }
        }

        return __value;
    }

    private void beforeReturn(ValueNode __x, JavaKind __kind)
    {
        if (graph.method() != null && graph.method().isJavaLangObjectInit())
        {
            /*
             * Get the receiver from the initial state since bytecode rewriting could do arbitrary
             * things to the state of the locals.
             */
            ValueNode __receiver = graph.start().stateAfter().localAt(0);
            if (RegisterFinalizerNode.mayHaveFinalizer(__receiver, graph.getAssumptions()))
            {
                append(new RegisterFinalizerNode(__receiver));
            }
        }
        if (finalBarrierRequired)
        {
            /*
             * When compiling an OSR with a final field store, don't bother tracking the original
             * receiver since the receiver cannot be EA'ed.
             */
            append(new FinalFieldBarrierNode(entryBCI == JVMCICompiler.INVOCATION_ENTRY_BCI ? originalReceiver : null));
        }
        synchronizedEpilogue(BytecodeFrame.AFTER_BCI, __x, __kind);
    }

    protected MonitorEnterNode createMonitorEnterNode(ValueNode __x, MonitorIdNode __monitorId)
    {
        return new MonitorEnterNode(__x, __monitorId);
    }

    protected void genMonitorEnter(ValueNode __x, int __bci)
    {
        MonitorIdNode __monitorId = graph.add(new MonitorIdNode(frameState.lockDepth(true)));
        MonitorEnterNode __monitorEnter = append(createMonitorEnterNode(__x, __monitorId));
        frameState.pushLock(__x, __monitorId);
        __monitorEnter.setStateAfter(createFrameState(__bci, __monitorEnter));
    }

    protected void genMonitorExit(ValueNode __x, ValueNode __escapedReturnValue, int __bci)
    {
        if (frameState.lockDepth(false) == 0)
        {
            throw bailout("unbalanced monitors: too many exits");
        }
        MonitorIdNode __monitorId = frameState.peekMonitorId();
        ValueNode __lockedObject = frameState.popLock();
        if (GraphUtil.originalValue(__lockedObject) != GraphUtil.originalValue(__x))
        {
            throw bailout("unbalanced monitors: mismatch at monitorexit, " + GraphUtil.originalValue(__x) + " != " + GraphUtil.originalValue(__lockedObject));
        }
        MonitorExitNode __monitorExit = append(new MonitorExitNode(__lockedObject, __monitorId, __escapedReturnValue));
        __monitorExit.setStateAfter(createFrameState(__bci, __monitorExit));
    }

    protected void genJsr(int __dest)
    {
        BciBlock __successor = currentBlock.getJsrSuccessor();
        JsrScope __scope = currentBlock.getJsrScope();
        int __nextBci = getStream().nextBCI();
        if (!__successor.getJsrScope().pop().equals(__scope))
        {
            throw new BailoutException("unstructured control flow (internal limitation)");
        }
        if (__successor.getJsrScope().nextReturnAddress() != __nextBci)
        {
            throw new BailoutException("unstructured control flow (internal limitation)");
        }
        ConstantNode __nextBciNode = getJsrConstant(__nextBci);
        frameState.push(JavaKind.Object, __nextBciNode);
        appendGoto(__successor);
    }

    protected void genRet(int __localIndex)
    {
        BciBlock __successor = currentBlock.getRetSuccessor();
        ValueNode __local = frameState.loadLocal(__localIndex, JavaKind.Object);
        JsrScope __scope = currentBlock.getJsrScope();
        int __retAddress = __scope.nextReturnAddress();
        ConstantNode __returnBciNode = getJsrConstant(__retAddress);
        LogicNode __guard = IntegerEqualsNode.create(constantReflection, metaAccess, null, __local, __returnBciNode, NodeView.DEFAULT);
        __guard = graph.addOrUniqueWithInputs(__guard);
        append(new FixedGuardNode(__guard, DeoptimizationReason.JavaSubroutineMismatch, DeoptimizationAction.InvalidateReprofile));
        if (!__successor.getJsrScope().equals(__scope.pop()))
        {
            throw new BailoutException("unstructured control flow (ret leaves more than one scope)");
        }
        appendGoto(__successor);
    }

    private ConstantNode getJsrConstant(long __bci)
    {
        JavaConstant __nextBciConstant = new RawConstant(__bci);
        Stamp __nextBciStamp = StampFactory.forConstant(__nextBciConstant);
        ConstantNode __nextBciNode = new ConstantNode(__nextBciConstant, __nextBciStamp);
        return graph.unique(__nextBciNode);
    }

    protected void genIntegerSwitch(ValueNode __value, ArrayList<BciBlock> __actualSuccessors, int[] __keys, double[] __keyProbabilities, int[] __keySuccessors)
    {
        if (__value.isConstant())
        {
            JavaConstant __constant = (JavaConstant) __value.asConstant();
            int __constantValue = __constant.asInt();
            for (int __i = 0; __i < __keys.length; ++__i)
            {
                if (__keys[__i] == __constantValue)
                {
                    appendGoto(__actualSuccessors.get(__keySuccessors[__i]));
                    return;
                }
            }
            appendGoto(__actualSuccessors.get(__keySuccessors[__keys.length]));
        }
        else
        {
            this.controlFlowSplit = true;
            double[] __successorProbabilities = successorProbabilites(__actualSuccessors.size(), __keySuccessors, __keyProbabilities);
            IntegerSwitchNode __switchNode = append(new IntegerSwitchNode(__value, __actualSuccessors.size(), __keys, __keyProbabilities, __keySuccessors));
            for (int __i = 0; __i < __actualSuccessors.size(); __i++)
            {
                __switchNode.setBlockSuccessor(__i, createBlockTarget(__successorProbabilities[__i], __actualSuccessors.get(__i), frameState));
            }
        }
    }

    /**
     * Helper function that sums up the probabilities of all keys that lead to a specific successor.
     *
     * @return an array of size successorCount with the accumulated probability for each successor.
     */
    private static double[] successorProbabilites(int __successorCount, int[] __keySuccessors, double[] __keyProbabilities)
    {
        double[] __probability = new double[__successorCount];
        for (int __i = 0; __i < __keySuccessors.length; __i++)
        {
            __probability[__keySuccessors[__i]] += __keyProbabilities[__i];
        }
        return __probability;
    }

    protected ConstantNode appendConstant(JavaConstant __constant)
    {
        return ConstantNode.forConstant(__constant, metaAccess, graph);
    }

    @Override
    public <T extends ValueNode> T append(T __v)
    {
        if (__v.graph() != null)
        {
            return __v;
        }
        T __added = graph.addOrUniqueWithInputs(__v);
        if (__added == __v)
        {
            updateLastInstruction(__v);
        }
        return __added;
    }

    private <T extends ValueNode> void updateLastInstruction(T __v)
    {
        if (__v instanceof FixedNode)
        {
            FixedNode __fixedNode = (FixedNode) __v;
            lastInstr.setNext(__fixedNode);
            if (__fixedNode instanceof FixedWithNextNode)
            {
                FixedWithNextNode __fixedWithNextNode = (FixedWithNextNode) __fixedNode;
                lastInstr = __fixedWithNextNode;
            }
            else
            {
                lastInstr = null;
            }
        }
    }

    private Target checkLoopExit(FixedNode __target, BciBlock __targetBlock, FrameStateBuilder __state)
    {
        if (currentBlock != null)
        {
            long __exits = currentBlock.loops & ~__targetBlock.loops;
            if (__exits != 0)
            {
                LoopExitNode __firstLoopExit = null;
                LoopExitNode __lastLoopExit = null;

                int __pos = 0;
                ArrayList<BciBlock> __exitLoops = new ArrayList<>(Long.bitCount(__exits));
                do
                {
                    long __lMask = 1L << __pos;
                    if ((__exits & __lMask) != 0)
                    {
                        __exitLoops.add(blockMap.getLoopHeader(__pos));
                        __exits &= ~__lMask;
                    }
                    __pos++;
                } while (__exits != 0);

                // @closure
                Collections.sort(__exitLoops, new Comparator<BciBlock>()
                {
                    @Override
                    public int compare(BciBlock __o1, BciBlock __o2)
                    {
                        return Long.bitCount(__o2.loops) - Long.bitCount(__o1.loops);
                    }
                });

                int __bci = __targetBlock.startBci;
                if (__targetBlock instanceof ExceptionDispatchBlock)
                {
                    __bci = ((ExceptionDispatchBlock) __targetBlock).deoptBci;
                }
                FrameStateBuilder __newState = __state.copy();
                for (BciBlock __loop : __exitLoops)
                {
                    LoopBeginNode __loopBegin = (LoopBeginNode) getFirstInstruction(__loop);
                    LoopExitNode __loopExit = graph.add(new LoopExitNode(__loopBegin));
                    if (__lastLoopExit != null)
                    {
                        __lastLoopExit.setNext(__loopExit);
                    }
                    if (__firstLoopExit == null)
                    {
                        __firstLoopExit = __loopExit;
                    }
                    __lastLoopExit = __loopExit;
                    __newState.clearNonLiveLocals(__targetBlock, liveness, true);
                    __newState.insertLoopProxies(__loopExit, getEntryState(__loop));
                    __loopExit.setStateAfter(__newState.create(__bci, __loopExit));
                }

                __lastLoopExit.setNext(__target);
                return new Target(__firstLoopExit, __newState);
            }
        }
        return new Target(__target, __state);
    }

    private FrameStateBuilder getEntryState(BciBlock __block)
    {
        return entryStateArray[__block.id];
    }

    private void setEntryState(BciBlock __block, FrameStateBuilder __entryState)
    {
        this.entryStateArray[__block.id] = __entryState;
    }

    private void setFirstInstruction(BciBlock __block, FixedWithNextNode __firstInstruction)
    {
        this.firstInstructionArray[__block.id] = __firstInstruction;
    }

    private FixedWithNextNode getFirstInstruction(BciBlock __block)
    {
        return firstInstructionArray[__block.id];
    }

    private FixedNode createTarget(double __probability, BciBlock __block, FrameStateBuilder __stateAfter)
    {
        if (isNeverExecutedCode(__probability))
        {
            return graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.UnreachedCode));
        }
        else
        {
            return createTarget(__block, __stateAfter);
        }
    }

    private FixedNode createTarget(BciBlock __block, FrameStateBuilder __state)
    {
        return createTarget(__block, __state, false, false);
    }

    private FixedNode createTarget(BciBlock __block, FrameStateBuilder __state, boolean __canReuseInstruction, boolean __canReuseState)
    {
        if (getFirstInstruction(__block) == null)
        {
            /*
             * This is the first time we see this block as a branch target. Create and return a
             * placeholder that later can be replaced with a MergeNode when we see this block again.
             */
            FixedNode __targetNode;
            if (__canReuseInstruction && (__block.getPredecessorCount() == 1 || !controlFlowSplit) && !__block.isLoopHeader() && (currentBlock.loops & ~__block.loops) == 0)
            {
                setFirstInstruction(__block, lastInstr);
                lastInstr = null;
            }
            else
            {
                setFirstInstruction(__block, graph.add(new BeginNode()));
            }
            __targetNode = getFirstInstruction(__block);
            Target __target = checkLoopExit(__targetNode, __block, __state);
            FixedNode __result = __target.fixed;
            FrameStateBuilder __currentEntryState = __target.state == __state ? (__canReuseState ? __state : __state.copy()) : __target.state;
            setEntryState(__block, __currentEntryState);
            __currentEntryState.clearNonLiveLocals(__block, liveness, true);

            return __result;
        }

        // We already saw this block before, so we have to merge states.
        if (!getEntryState(__block).isCompatibleWith(__state))
        {
            throw bailout(String.format("stacks do not match on merge from %d into %s; bytecodes would not verify:%nexpect: %s%nactual: %s", bci(), __block, getEntryState(__block), __state));
        }

        if (getFirstInstruction(__block) instanceof LoopBeginNode)
        {
            /*
             * Backward loop edge. We need to create a special LoopEndNode and merge with the
             * loop begin node created before.
             */
            LoopBeginNode __loopBegin = (LoopBeginNode) getFirstInstruction(__block);
            LoopEndNode __loopEnd = graph.add(new LoopEndNode(__loopBegin));
            Target __target = checkLoopExit(__loopEnd, __block, __state);
            FixedNode __result = __target.fixed;
            getEntryState(__block).merge(__loopBegin, __target.state);

            return __result;
        }

        if (getFirstInstruction(__block) instanceof AbstractBeginNode && !(getFirstInstruction(__block) instanceof AbstractMergeNode))
        {
            /*
             * This is the second time we see this block. Create the actual MergeNode and the
             * End Node for the already existing edge.
             */
            AbstractBeginNode __beginNode = (AbstractBeginNode) getFirstInstruction(__block);

            // The EndNode for the already existing edge.
            EndNode __end = graph.add(new EndNode());
            // The MergeNode that replaces the placeholder.
            AbstractMergeNode __mergeNode = graph.add(new MergeNode());
            FixedNode __next = __beginNode.next();

            if (__beginNode.predecessor() instanceof ControlSplitNode)
            {
                __beginNode.setNext(__end);
            }
            else
            {
                __beginNode.replaceAtPredecessor(__end);
                __beginNode.safeDelete();
            }

            __mergeNode.addForwardEnd(__end);
            __mergeNode.setNext(__next);

            setFirstInstruction(__block, __mergeNode);
        }

        AbstractMergeNode __mergeNode = (AbstractMergeNode) getFirstInstruction(__block);

        // The EndNode for the newly merged edge.
        EndNode __newEnd = graph.add(new EndNode());
        Target __target = checkLoopExit(__newEnd, __block, __state);
        FixedNode __result = __target.fixed;
        getEntryState(__block).merge(__mergeNode, __target.state);
        __mergeNode.addForwardEnd(__newEnd);

        return __result;
    }

    /**
     * Returns a block begin node with the specified state. If the specified probability is 0, the
     * block deoptimizes immediately.
     */
    private AbstractBeginNode createBlockTarget(double __probability, BciBlock __block, FrameStateBuilder __stateAfter)
    {
        FixedNode __target = createTarget(__probability, __block, __stateAfter);
        return BeginNode.begin(__target);
    }

    private ValueNode synchronizedObject(FrameStateBuilder __state, ResolvedJavaMethod __target)
    {
        if (__target.isStatic())
        {
            return appendConstant(getConstantReflection().asJavaClass(__target.getDeclaringClass()));
        }
        else
        {
            return __state.loadLocal(0, JavaKind.Object);
        }
    }

    protected void processBlock(BciBlock __block)
    {
        // ignore blocks that have no predecessors by the time their bytecodes are parsed
        FixedWithNextNode __firstInstruction = getFirstInstruction(__block);
        if (__firstInstruction == null)
        {
            return;
        }

        lastInstr = __firstInstruction;
        frameState = getEntryState(__block);
        setCurrentFrameState(frameState);
        currentBlock = __block;

        if (__block != blockMap.getUnwindBlock() && !(__block instanceof ExceptionDispatchBlock))
        {
            frameState.setRethrowException(false);
        }

        if (__firstInstruction instanceof AbstractMergeNode)
        {
            setMergeStateAfter(__block, __firstInstruction);
        }

        if (__block == blockMap.getUnwindBlock())
        {
            handleUnwindBlock((ExceptionDispatchBlock) __block);
        }
        else if (__block instanceof ExceptionDispatchBlock)
        {
            createExceptionDispatch((ExceptionDispatchBlock) __block);
        }
        else
        {
            iterateBytecodesForBlock(__block);
        }
    }

    private void handleUnwindBlock(ExceptionDispatchBlock __block)
    {
        if (parent == null)
        {
            finishPrepare(lastInstr, __block.deoptBci);
            frameState.setRethrowException(false);
            createUnwind();
        }
        else
        {
            ValueNode __exception = frameState.pop(JavaKind.Object);
            this.unwindValue = __exception;
            this.beforeUnwindNode = this.lastInstr;
        }
    }

    private void setMergeStateAfter(BciBlock __block, FixedWithNextNode __firstInstruction)
    {
        AbstractMergeNode __abstractMergeNode = (AbstractMergeNode) __firstInstruction;
        if (__abstractMergeNode.stateAfter() == null)
        {
            int __bci = __block.startBci;
            if (__block instanceof ExceptionDispatchBlock)
            {
                __bci = ((ExceptionDispatchBlock) __block).deoptBci;
            }
            __abstractMergeNode.setStateAfter(createFrameState(__bci, __abstractMergeNode));
        }
    }

    private void createUnwind()
    {
        synchronizedEpilogue(BytecodeFrame.AFTER_EXCEPTION_BCI, null, null);
        ValueNode __exception = frameState.pop(JavaKind.Object);
        append(new UnwindNode(__exception));
    }

    private void synchronizedEpilogue(int __bci, ValueNode __currentReturnValue, JavaKind __currentReturnValueKind)
    {
        if (method.isSynchronized())
        {
            if (__currentReturnValue != null)
            {
                frameState.push(__currentReturnValueKind, __currentReturnValue);
            }
            genMonitorExit(methodSynchronizedObject, __currentReturnValue, __bci);
            finishPrepare(lastInstr, __bci);
        }
        if (frameState.lockDepth(false) != 0)
        {
            throw bailout("unbalanced monitors: too few exits exiting frame");
        }
    }

    private void createExceptionDispatch(ExceptionDispatchBlock __block)
    {
        lastInstr = finishInstruction(lastInstr, frameState);

        if (__block.handler.isCatchAll())
        {
            appendGoto(__block.getSuccessor(0));
            return;
        }

        JavaType __catchType = __block.handler.getCatchType();
        if (graphBuilderConfig.eagerResolving())
        {
            __catchType = lookupType(__block.handler.catchTypeCPI(), Bytecodes.INSTANCEOF);
        }
        if (__catchType instanceof ResolvedJavaType)
        {
            TypeReference __checkedCatchType = TypeReference.createTrusted(graph.getAssumptions(), (ResolvedJavaType) __catchType);

            if (graphBuilderConfig.getSkippedExceptionTypes() != null)
            {
                for (ResolvedJavaType __skippedType : graphBuilderConfig.getSkippedExceptionTypes())
                {
                    if (__skippedType.isAssignableFrom(__checkedCatchType.getType()))
                    {
                        BciBlock __nextBlock = __block.getSuccessorCount() == 1 ? blockMap.getUnwindBlock() : __block.getSuccessor(1);
                        ValueNode __exception = frameState.stack[0];
                        FixedNode __trueSuccessor = graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.UnreachedCode));
                        FixedNode __nextDispatch = createTarget(__nextBlock, frameState);
                        append(new IfNode(graph.addOrUniqueWithInputs(createInstanceOf(__checkedCatchType, __exception)), __trueSuccessor, __nextDispatch, 0));
                        return;
                    }
                }
            }

            BciBlock __nextBlock = __block.getSuccessorCount() == 1 ? blockMap.getUnwindBlock() : __block.getSuccessor(1);
            ValueNode __exception = frameState.stack[0];
            // Anchor for the piNode, which must be before any LoopExit inserted by createTarget.
            BeginNode __piNodeAnchor = graph.add(new BeginNode());
            ObjectStamp __checkedStamp = StampFactory.objectNonNull(__checkedCatchType);
            PiNode __piNode = graph.addWithoutUnique(new PiNode(__exception, __checkedStamp));
            frameState.pop(JavaKind.Object);
            frameState.push(JavaKind.Object, __piNode);
            FixedNode __catchSuccessor = createTarget(__block.getSuccessor(0), frameState);
            frameState.pop(JavaKind.Object);
            frameState.push(JavaKind.Object, __exception);
            FixedNode __nextDispatch = createTarget(__nextBlock, frameState);
            __piNodeAnchor.setNext(__catchSuccessor);
            IfNode __ifNode = append(new IfNode(graph.unique(createInstanceOf(__checkedCatchType, __exception)), __piNodeAnchor, __nextDispatch, 0.5));
            __piNode.setGuard(__ifNode.trueSuccessor());
        }
        else
        {
            handleUnresolvedExceptionType(__catchType);
        }
    }

    private void appendGoto(BciBlock __successor)
    {
        FixedNode __targetInstr = createTarget(__successor, frameState, true, true);
        if (lastInstr != null && lastInstr != __targetInstr)
        {
            lastInstr.setNext(__targetInstr);
        }
    }

    protected void iterateBytecodesForBlock(BciBlock __block)
    {
        if (__block.isLoopHeader())
        {
            // Create the loop header block, which later will merge the backward branches of the loop.
            controlFlowSplit = true;
            LoopBeginNode __loopBegin = appendLoopBegin(this.lastInstr, __block.startBci);
            lastInstr = __loopBegin;

            // Create phi functions for all local variables and operand stack slots.
            frameState.insertLoopPhis(liveness, __block.loopId, __loopBegin, forceLoopPhis(), stampFromValueForForcedPhis());
            __loopBegin.setStateAfter(createFrameState(__block.startBci, __loopBegin));

            /*
             * We have seen all forward branches. All subsequent backward branches will merge to the
             * loop header. This ensures that the loop header has exactly one non-loop predecessor.
             */
            setFirstInstruction(__block, __loopBegin);
            /*
             * We need to preserve the frame state builder of the loop header so that we can merge
             * values for phi functions, so make a copy of it.
             */
            setEntryState(__block, frameState.copy());
        }
        else if (lastInstr instanceof MergeNode)
        {
            /*
             * All inputs of non-loop phi nodes are known by now. We can infer the stamp for the
             * phi, so that parsing continues with more precise type information.
             */
            frameState.inferPhiStamps((AbstractMergeNode) lastInstr);
        }

        lastInstr = finishInstruction(lastInstr, frameState);

        int __endBCI = stream.endBCI();

        stream.setBCI(__block.startBci);
        int __bci = __block.startBci;

        while (__bci < __endBCI)
        {
            try
            {
                // read the opcode
                int __opcode = stream.currentBC();
                if (parent == null && __bci == entryBCI)
                {
                    if (__block.getJsrScope() != JsrScope.EMPTY_SCOPE)
                    {
                        throw new BailoutException("OSR into a Bytecodes.JSR scope is not supported");
                    }
                    EntryMarkerNode __x = append(new EntryMarkerNode());
                    frameState.insertProxies(__value -> graph.unique(new EntryProxyNode(__value, __x)));
                    __x.setStateAfter(createFrameState(__bci, __x));
                }

                processBytecode(__bci, __opcode);
            }
            catch (BailoutException | GraalError __e)
            {
                // don't wrap bailouts as parser errors
                throw __e;
            }
            catch (Throwable __t)
            {
                throw new GraalError(__t);
            }

            if (lastInstr == null || lastInstr.next() != null)
            {
                break;
            }

            stream.next();
            __bci = stream.currentBCI();

            lastInstr = finishInstruction(lastInstr, frameState);
            if (__bci < __endBCI)
            {
                if (__bci > __block.endBci)
                {
                    // we fell through to the next block, add a goto and break
                    appendGoto(__block.getSuccessor(0));
                    break;
                }
            }
        }
    }

    // Also a hook for subclasses.
    protected boolean forceLoopPhis()
    {
        return graph.isOSR();
    }

    // Hook for subclasses.
    protected boolean stampFromValueForForcedPhis()
    {
        return false;
    }

    // Also a hook for subclasses.
    protected boolean disableLoopSafepoint()
    {
        return parsingIntrinsic();
    }

    private LoopBeginNode appendLoopBegin(FixedWithNextNode __fixedWithNext, int __startBci)
    {
        EndNode __preLoopEnd = graph.add(new EndNode());
        LoopBeginNode __loopBegin = graph.add(new LoopBeginNode());
        if (disableLoopSafepoint())
        {
            __loopBegin.disableSafepoint();
        }
        __fixedWithNext.setNext(__preLoopEnd);
        // Add the single non-loop predecessor of the loop header.
        __loopBegin.addForwardEnd(__preLoopEnd);
        return __loopBegin;
    }

    /**
     * Hook for subclasses to modify the last instruction or add other instructions.
     *
     * @param instr The last instruction (= fixed node) which was added.
     * @param state The current frame state.
     * @return Returns the (new) last instruction.
     */
    protected FixedWithNextNode finishInstruction(FixedWithNextNode __instr, FrameStateBuilder __state)
    {
        return __instr;
    }

    protected void genIf(ValueNode __x, Condition __cond, ValueNode __y)
    {
        BciBlock __trueBlock = currentBlock.getSuccessor(0);
        BciBlock __falseBlock = currentBlock.getSuccessor(1);

        if (__trueBlock == __falseBlock)
        {
            // The target block is the same independent of the condition.
            appendGoto(__trueBlock);
            return;
        }

        ValueNode __a = __x;
        ValueNode __b = __y;
        BciBlock __trueSuccessor = __trueBlock;
        BciBlock __falseSuccessor = __falseBlock;

        CanonicalizedCondition __canonicalizedCondition = __cond.canonicalize();

        // Check whether the condition needs to mirror the operands.
        if (__canonicalizedCondition.mustMirror())
        {
            __a = __y;
            __b = __x;
        }
        if (__canonicalizedCondition.mustNegate())
        {
            __trueSuccessor = __falseBlock;
            __falseSuccessor = __trueBlock;
        }

        // Create the logic node for the condition.
        LogicNode __condition = createLogicNode(__canonicalizedCondition.getCanonicalCondition(), __a, __b);

        double __probability = -1;
        if (__condition instanceof IntegerEqualsNode)
        {
            __probability = extractInjectedProbability((IntegerEqualsNode) __condition);
            // the probability coming from here is about the actual condition
        }

        if (__probability == -1)
        {
            __probability = getProfileProbability(__canonicalizedCondition.mustNegate());
        }

        __probability = clampProbability(__probability);
        genIf(__condition, __trueSuccessor, __falseSuccessor, __probability);
    }

    protected double getProfileProbability(boolean __negate)
    {
        if (profilingInfo == null)
        {
            return 0.5;
        }

        double __probability = profilingInfo.getBranchTakenProbability(bci());

        if (__probability < 0)
        {
            return 0.5;
        }

        if (__negate && shouldComplementProbability())
        {
            // the probability coming from profile is about the original condition
            __probability = 1 - __probability;
        }
        return __probability;
    }

    private static double extractInjectedProbability(IntegerEqualsNode __condition)
    {
        // Propagate injected branch probability if any.
        IntegerEqualsNode __equalsNode = __condition;
        BranchProbabilityNode __probabilityNode = null;
        ValueNode __other = null;
        if (__equalsNode.getX() instanceof BranchProbabilityNode)
        {
            __probabilityNode = (BranchProbabilityNode) __equalsNode.getX();
            __other = __equalsNode.getY();
        }
        else if (__equalsNode.getY() instanceof BranchProbabilityNode)
        {
            __probabilityNode = (BranchProbabilityNode) __equalsNode.getY();
            __other = __equalsNode.getX();
        }

        if (__probabilityNode != null && __probabilityNode.getProbability().isConstant() && __other != null && __other.isConstant())
        {
            double __probabilityValue = __probabilityNode.getProbability().asJavaConstant().asDouble();
            return __other.asJavaConstant().asInt() == 0 ? 1.0 - __probabilityValue : __probabilityValue;
        }
        return -1;
    }

    protected void genIf(LogicNode __conditionInput, BciBlock __trueBlockInput, BciBlock __falseBlockInput, double __probabilityInput)
    {
        BciBlock __trueBlock = __trueBlockInput;
        BciBlock __falseBlock = __falseBlockInput;
        LogicNode __condition = __conditionInput;
        double __probability = __probabilityInput;

        // Remove a logic negation node.
        if (__condition instanceof LogicNegationNode)
        {
            LogicNegationNode __logicNegationNode = (LogicNegationNode) __condition;
            BciBlock __tmpBlock = __trueBlock;
            __trueBlock = __falseBlock;
            __falseBlock = __tmpBlock;
            if (shouldComplementProbability())
            {
                // the probability coming from profile is about the original condition
                __probability = 1 - __probability;
            }
            __condition = __logicNegationNode.getValue();
        }

        if (__condition instanceof LogicConstantNode)
        {
            genConstantTargetIf(__trueBlock, __falseBlock, __condition);
        }
        else
        {
            if (__condition.graph() == null)
            {
                __condition = genUnique(__condition);
            }

            if (isNeverExecutedCode(__probability))
            {
                if (!graph.isOSR() || getParent() != null || graph.getEntryBCI() != __trueBlock.startBci)
                {
                    append(new FixedGuardNode(__condition, DeoptimizationReason.UnreachedCode, DeoptimizationAction.InvalidateReprofile, true));
                    appendGoto(__falseBlock);
                    return;
                }
            }
            else if (isNeverExecutedCode(1 - __probability))
            {
                if (!graph.isOSR() || getParent() != null || graph.getEntryBCI() != __falseBlock.startBci)
                {
                    append(new FixedGuardNode(__condition, DeoptimizationReason.UnreachedCode, DeoptimizationAction.InvalidateReprofile, false));
                    appendGoto(__trueBlock);
                    return;
                }
            }

            int __oldBci = stream.currentBCI();
            int __trueBlockInt = checkPositiveIntConstantPushed(__trueBlock);
            if (__trueBlockInt != -1)
            {
                int __falseBlockInt = checkPositiveIntConstantPushed(__falseBlock);
                if (__falseBlockInt != -1)
                {
                    if (tryGenConditionalForIf(__trueBlock, __falseBlock, __condition, __oldBci, __trueBlockInt, __falseBlockInt))
                    {
                        return;
                    }
                }
            }

            this.controlFlowSplit = true;
            FixedNode __trueSuccessor = createTarget(__trueBlock, frameState, false, false);
            FixedNode __falseSuccessor = createTarget(__falseBlock, frameState, false, true);
            ValueNode __ifNode = genIfNode(__condition, __trueSuccessor, __falseSuccessor, __probability);
            postProcessIfNode(__ifNode);
            append(__ifNode);
        }
    }

    /**
     * Hook for subclasses to decide whether the IfNode probability should be complemented during
     * conversion to Graal IR.
     */
    protected boolean shouldComplementProbability()
    {
        return true;
    }

    /**
     * Hook for subclasses to generate custom nodes before an IfNode.
     */
    @SuppressWarnings("unused")
    protected void postProcessIfNode(ValueNode __node)
    {
    }

    private boolean tryGenConditionalForIf(BciBlock __trueBlock, BciBlock __falseBlock, LogicNode __condition, int __oldBci, int __trueBlockInt, int __falseBlockInt)
    {
        if (gotoOrFallThroughAfterConstant(__trueBlock) && gotoOrFallThroughAfterConstant(__falseBlock) && __trueBlock.getSuccessor(0) == __falseBlock.getSuccessor(0))
        {
            genConditionalForIf(__trueBlock, __condition, __oldBci, __trueBlockInt, __falseBlockInt, false);
            return true;
        }
        else if (this.parent != null && returnAfterConstant(__trueBlock) && returnAfterConstant(__falseBlock))
        {
            genConditionalForIf(__trueBlock, __condition, __oldBci, __trueBlockInt, __falseBlockInt, true);
            return true;
        }
        return false;
    }

    private void genConditionalForIf(BciBlock __trueBlock, LogicNode __condition, int __oldBci, int __trueBlockInt, int __falseBlockInt, boolean __genReturn)
    {
        ConstantNode __trueValue = graph.unique(ConstantNode.forInt(__trueBlockInt));
        ConstantNode __falseValue = graph.unique(ConstantNode.forInt(__falseBlockInt));
        ValueNode __conditionalNode = ConditionalNode.create(__condition, __trueValue, __falseValue, NodeView.DEFAULT);
        if (__conditionalNode.graph() == null)
        {
            __conditionalNode = graph.addOrUniqueWithInputs(__conditionalNode);
        }
        if (__genReturn)
        {
            JavaKind __returnKind = method.getSignature().getReturnKind().getStackKind();
            this.genReturn(__conditionalNode, __returnKind);
        }
        else
        {
            frameState.push(JavaKind.Int, __conditionalNode);
            appendGoto(__trueBlock.getSuccessor(0));
            stream.setBCI(__oldBci);
        }
    }

    private LogicNode createLogicNode(CanonicalCondition __cond, ValueNode __a, ValueNode __b)
    {
        switch (__cond)
        {
            case EQ:
                if (__a.getStackKind() == JavaKind.Object)
                {
                    return genObjectEquals(__a, __b);
                }
                else
                {
                    return genIntegerEquals(__a, __b);
                }
            case LT:
                return genIntegerLessThan(__a, __b);
            default:
                throw GraalError.shouldNotReachHere("Unexpected condition: " + __cond);
        }
    }

    private void genConstantTargetIf(BciBlock __trueBlock, BciBlock __falseBlock, LogicNode __condition)
    {
        LogicConstantNode __constantLogicNode = (LogicConstantNode) __condition;
        boolean __value = __constantLogicNode.getValue();
        BciBlock __nextBlock = __falseBlock;
        if (__value)
        {
            __nextBlock = __trueBlock;
        }
        int __startBci = __nextBlock.startBci;
        int __targetAtStart = stream.readUByte(__startBci);
        if (__targetAtStart == Bytecodes.GOTO && __nextBlock.getPredecessorCount() == 1)
        {
            // This is an empty block. Skip it.
            BciBlock __successorBlock = __nextBlock.successors.get(0);
            appendGoto(__successorBlock);
        }
        else
        {
            appendGoto(__nextBlock);
        }
    }

    private int checkPositiveIntConstantPushed(BciBlock __block)
    {
        stream.setBCI(__block.startBci);
        int __currentBC = stream.currentBC();
        if (__currentBC >= Bytecodes.ICONST_0 && __currentBC <= Bytecodes.ICONST_5)
        {
            return __currentBC - Bytecodes.ICONST_0;
        }
        return -1;
    }

    private boolean gotoOrFallThroughAfterConstant(BciBlock __block)
    {
        stream.setBCI(__block.startBci);
        int __currentBCI = stream.nextBCI();
        stream.setBCI(__currentBCI);
        int __currentBC = stream.currentBC();
        return stream.currentBCI() > __block.endBci || __currentBC == Bytecodes.GOTO || __currentBC == Bytecodes.GOTO_W;
    }

    private boolean returnAfterConstant(BciBlock __block)
    {
        stream.setBCI(__block.startBci);
        int __currentBCI = stream.nextBCI();
        stream.setBCI(__currentBCI);
        int __currentBC = stream.currentBC();
        return __currentBC == Bytecodes.IRETURN;
    }

    @Override
    public StampProvider getStampProvider()
    {
        return stampProvider;
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return metaAccess;
    }

    @Override
    public void push(JavaKind __slotKind, ValueNode __value)
    {
        frameState.push(__slotKind, __value);
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return constantReflection;
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider()
    {
        return constantFieldProvider;
    }

    /**
     * Gets the graph being processed by this builder.
     */
    @Override
    public StructuredGraph getGraph()
    {
        return graph;
    }

    @Override
    public BytecodeParser getParent()
    {
        return parent;
    }

    @Override
    public IntrinsicContext getIntrinsic()
    {
        return intrinsicContext;
    }

    @Override
    public BailoutException bailout(String __msg)
    {
        throw new BailoutException(__msg);
    }

    private FrameState createFrameState(int __bci, StateSplit __forStateSplit)
    {
        if (currentBlock != null && __bci > currentBlock.endBci)
        {
            frameState.clearNonLiveLocals(currentBlock, liveness, false);
        }
        return frameState.create(__bci, __forStateSplit);
    }

    @Override
    public void setStateAfter(StateSplit __sideEffect)
    {
        FrameState __stateAfter = createFrameState(stream.nextBCI(), __sideEffect);
        __sideEffect.setStateAfter(__stateAfter);
    }

    public void setCurrentFrameState(FrameStateBuilder __frameState)
    {
        this.frameState = __frameState;
    }

    protected final BytecodeStream getStream()
    {
        return stream;
    }

    @Override
    public int bci()
    {
        return stream.currentBCI();
    }

    public void loadLocal(int __index, JavaKind __kind)
    {
        ValueNode __value = frameState.loadLocal(__index, __kind);
        frameState.push(__kind, __value);
    }

    public void loadLocalObject(int __index)
    {
        ValueNode __value = frameState.loadLocal(__index, JavaKind.Object);

        int __nextBCI = stream.nextBCI();
        int __nextBC = stream.readUByte(__nextBCI);
        if (__nextBCI <= currentBlock.endBci && __nextBC == Bytecodes.GETFIELD)
        {
            stream.next();
            genGetField(stream.readCPI(), Bytecodes.GETFIELD, __value);
        }
        else
        {
            frameState.push(JavaKind.Object, __value);
        }
    }

    public void storeLocal(JavaKind __kind, int __index)
    {
        ValueNode __value = frameState.pop(__kind);
        frameState.storeLocal(__index, __kind, __value);
    }

    private void genLoadConstant(int __cpi, int __opcode)
    {
        Object __con = lookupConstant(__cpi, __opcode);

        if (__con instanceof JavaType)
        {
            // this is a load of class constant which might be unresolved
            JavaType __type = (JavaType) __con;
            if (__type instanceof ResolvedJavaType)
            {
                frameState.push(JavaKind.Object, appendConstant(getConstantReflection().asJavaClass((ResolvedJavaType) __type)));
            }
            else
            {
                handleUnresolvedLoadConstant(__type);
            }
        }
        else if (__con instanceof JavaConstant)
        {
            JavaConstant __constant = (JavaConstant) __con;
            frameState.push(__constant.getJavaKind(), appendConstant(__constant));
        }
        else
        {
            throw new Error("lookupConstant returned an object of incorrect type");
        }
    }

    private void genLoadIndexed(JavaKind __kind)
    {
        ValueNode __index = frameState.pop(JavaKind.Int);
        ValueNode __array = emitExplicitExceptions(frameState.pop(JavaKind.Object), __index);

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleLoadIndexed(this, __array, __index, __kind))
            {
                return;
            }
        }

        frameState.push(__kind, append(genLoadIndexed(__array, __index, __kind)));
    }

    private void genStoreIndexed(JavaKind __kind)
    {
        ValueNode __value = frameState.pop(__kind);
        ValueNode __index = frameState.pop(JavaKind.Int);
        ValueNode __array = emitExplicitExceptions(frameState.pop(JavaKind.Object), __index);

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleStoreIndexed(this, __array, __index, __kind, __value))
            {
                return;
            }
        }

        genStoreIndexed(__array, __index, __kind, __value);
    }

    private void genArithmeticOp(JavaKind __kind, int __opcode)
    {
        ValueNode __y = frameState.pop(__kind);
        ValueNode __x = frameState.pop(__kind);
        ValueNode __v;
        switch (__opcode)
        {
            case Bytecodes.IADD:
            case Bytecodes.LADD:
                __v = genIntegerAdd(__x, __y);
                break;
            case Bytecodes.FADD:
            case Bytecodes.DADD:
                __v = genFloatAdd(__x, __y);
                break;
            case Bytecodes.ISUB:
            case Bytecodes.LSUB:
                __v = genIntegerSub(__x, __y);
                break;
            case Bytecodes.FSUB:
            case Bytecodes.DSUB:
                __v = genFloatSub(__x, __y);
                break;
            case Bytecodes.IMUL:
            case Bytecodes.LMUL:
                __v = genIntegerMul(__x, __y);
                break;
            case Bytecodes.FMUL:
            case Bytecodes.DMUL:
                __v = genFloatMul(__x, __y);
                break;
            case Bytecodes.FDIV:
            case Bytecodes.DDIV:
                __v = genFloatDiv(__x, __y);
                break;
            case Bytecodes.FREM:
            case Bytecodes.DREM:
                __v = genFloatRem(__x, __y);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(__kind, append(__v));
    }

    private void genIntegerDivOp(JavaKind __kind, int __opcode)
    {
        ValueNode __y = frameState.pop(__kind);
        ValueNode __x = frameState.pop(__kind);
        ValueNode __v;
        switch (__opcode)
        {
            case Bytecodes.IDIV:
            case Bytecodes.LDIV:
                __v = genIntegerDiv(__x, __y);
                break;
            case Bytecodes.IREM:
            case Bytecodes.LREM:
                __v = genIntegerRem(__x, __y);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(__kind, append(__v));
    }

    private void genNegateOp(JavaKind __kind)
    {
        ValueNode __x = frameState.pop(__kind);
        frameState.push(__kind, append(genNegateOp(__x)));
    }

    private void genShiftOp(JavaKind __kind, int __opcode)
    {
        ValueNode __s = frameState.pop(JavaKind.Int);
        ValueNode __x = frameState.pop(__kind);
        ValueNode __v;
        switch (__opcode)
        {
            case Bytecodes.ISHL:
            case Bytecodes.LSHL:
                __v = genLeftShift(__x, __s);
                break;
            case Bytecodes.ISHR:
            case Bytecodes.LSHR:
                __v = genRightShift(__x, __s);
                break;
            case Bytecodes.IUSHR:
            case Bytecodes.LUSHR:
                __v = genUnsignedRightShift(__x, __s);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(__kind, append(__v));
    }

    private void genLogicOp(JavaKind __kind, int __opcode)
    {
        ValueNode __y = frameState.pop(__kind);
        ValueNode __x = frameState.pop(__kind);
        ValueNode __v;
        switch (__opcode)
        {
            case Bytecodes.IAND:
            case Bytecodes.LAND:
                __v = genAnd(__x, __y);
                break;
            case Bytecodes.IOR:
            case Bytecodes.LOR:
                __v = genOr(__x, __y);
                break;
            case Bytecodes.IXOR:
            case Bytecodes.LXOR:
                __v = genXor(__x, __y);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(__kind, append(__v));
    }

    private void genCompareOp(JavaKind __kind, boolean __isUnorderedLess)
    {
        ValueNode __y = frameState.pop(__kind);
        ValueNode __x = frameState.pop(__kind);
        frameState.push(JavaKind.Int, append(genNormalizeCompare(__x, __y, __isUnorderedLess)));
    }

    private void genFloatConvert(FloatConvert __op, JavaKind __from, JavaKind __to)
    {
        ValueNode __input = frameState.pop(__from);
        frameState.push(__to, append(genFloatConvert(__op, __input)));
    }

    private void genSignExtend(JavaKind __from, JavaKind __to)
    {
        ValueNode __input = frameState.pop(__from);
        if (__from != __from.getStackKind())
        {
            __input = append(genNarrow(__input, __from.getBitCount()));
        }
        frameState.push(__to, append(genSignExtend(__input, __to.getBitCount())));
    }

    private void genZeroExtend(JavaKind __from, JavaKind __to)
    {
        ValueNode __input = frameState.pop(__from);
        if (__from != __from.getStackKind())
        {
            __input = append(genNarrow(__input, __from.getBitCount()));
        }
        frameState.push(__to, append(genZeroExtend(__input, __to.getBitCount())));
    }

    private void genNarrow(JavaKind __from, JavaKind __to)
    {
        ValueNode __input = frameState.pop(__from);
        frameState.push(__to, append(genNarrow(__input, __to.getBitCount())));
    }

    private void genIncrement()
    {
        int __index = getStream().readLocalIndex();
        int __delta = getStream().readIncrement();
        ValueNode __x = frameState.loadLocal(__index, JavaKind.Int);
        ValueNode __y = appendConstant(JavaConstant.forInt(__delta));
        frameState.storeLocal(__index, JavaKind.Int, append(genIntegerAdd(__x, __y)));
    }

    private void genIfZero(Condition __cond)
    {
        ValueNode __y = appendConstant(JavaConstant.INT_0);
        ValueNode __x = frameState.pop(JavaKind.Int);
        genIf(__x, __cond, __y);
    }

    private void genIfNull(Condition __cond)
    {
        ValueNode __y = appendConstant(JavaConstant.NULL_POINTER);
        ValueNode __x = frameState.pop(JavaKind.Object);
        genIf(__x, __cond, __y);
    }

    private void genIfSame(JavaKind __kind, Condition __cond)
    {
        ValueNode __y = frameState.pop(__kind);
        ValueNode __x = frameState.pop(__kind);
        genIf(__x, __cond, __y);
    }

    private static void initialize(ResolvedJavaType __resolvedType)
    {
        /*
         * Since we're potentially triggering class initialization here, we need synchronization
         * to mitigate the potential for class initialization related deadlock being caused by
         * the compiler (e.g. https://github.com/graalvm/graal-core/pull/232/files#r90788550).
         */
        synchronized (BytecodeParser.class)
        {
            __resolvedType.initialize();
        }
    }

    protected JavaType lookupType(int __cpi, int __bytecode)
    {
        maybeEagerlyResolve(__cpi, __bytecode);
        return constantPool.lookupType(__cpi, __bytecode);
    }

    private JavaMethod lookupMethod(int __cpi, int __opcode)
    {
        maybeEagerlyResolve(__cpi, __opcode);
        return constantPool.lookupMethod(__cpi, __opcode);
    }

    protected JavaField lookupField(int __cpi, int __opcode)
    {
        maybeEagerlyResolve(__cpi, __opcode);
        JavaField __result = constantPool.lookupField(__cpi, method, __opcode);
        if (parsingIntrinsic() || eagerInitializing)
        {
            if (__result instanceof ResolvedJavaField)
            {
                ResolvedJavaType __declaringClass = ((ResolvedJavaField) __result).getDeclaringClass();
                if (!__declaringClass.isInitialized())
                {
                    // even with eager initialization, superinterfaces are not always initialized (see StaticInterfaceFieldTest)
                    initialize(__declaringClass);
                }
            }
        }
        return __result;
    }

    private Object lookupConstant(int __cpi, int __opcode)
    {
        maybeEagerlyResolve(__cpi, __opcode);
        return constantPool.lookupConstant(__cpi);
    }

    protected void maybeEagerlyResolve(int __cpi, int __bytecode)
    {
        if (intrinsicContext != null)
        {
            constantPool.loadReferencedType(__cpi, __bytecode);
        }
        else if (graphBuilderConfig.eagerResolving())
        {
            /*
             * Since we're potentially triggering class initialization here, we need synchronization
             * to mitigate the potential for class initialization related deadlock being caused by
             * the compiler (e.g. https://github.com/graalvm/graal-core/pull/232/files#r90788550).
             */
            synchronized (BytecodeParser.class)
            {
                constantPool.loadReferencedType(__cpi, __bytecode);
            }
        }
    }

    private JavaTypeProfile getProfileForTypeCheck(TypeReference __type)
    {
        if (parsingIntrinsic() || profilingInfo == null || !optimisticOpts.useTypeCheckHints() || __type.isExact())
        {
            return null;
        }
        else
        {
            return profilingInfo.getTypeProfile(bci());
        }
    }

    private void genCheckCast()
    {
        int __cpi = getStream().readCPI();
        JavaType __type = lookupType(__cpi, Bytecodes.CHECKCAST);
        ValueNode __object = frameState.pop(JavaKind.Object);

        if (!(__type instanceof ResolvedJavaType))
        {
            handleUnresolvedCheckCast(__type, __object);
            return;
        }
        TypeReference __checkedType = TypeReference.createTrusted(graph.getAssumptions(), (ResolvedJavaType) __type);
        JavaTypeProfile __profile = getProfileForTypeCheck(__checkedType);

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleCheckCast(this, __object, __checkedType.getType(), __profile))
            {
                return;
            }
        }

        ValueNode __castNode = null;
        if (__profile != null)
        {
            if (__profile.getNullSeen().isFalse())
            {
                __object = nullCheckedValue(__object);
                ResolvedJavaType __singleType = __profile.asSingleType();
                if (__singleType != null && __checkedType.getType().isAssignableFrom(__singleType))
                {
                    LogicNode __typeCheck = append(createInstanceOf(TypeReference.createExactTrusted(__singleType), __object, __profile));
                    if (__typeCheck.isTautology())
                    {
                        __castNode = __object;
                    }
                    else
                    {
                        FixedGuardNode __fixedGuard = append(new FixedGuardNode(__typeCheck, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile, false));
                        __castNode = append(PiNode.create(__object, StampFactory.objectNonNull(TypeReference.createExactTrusted(__singleType)), __fixedGuard));
                    }
                }
            }
        }

        boolean __nonNull = ((ObjectStamp) __object.stamp(NodeView.DEFAULT)).nonNull();
        if (__castNode == null)
        {
            LogicNode __condition = genUnique(createInstanceOfAllowNull(__checkedType, __object, null));
            if (__condition.isTautology())
            {
                __castNode = __object;
            }
            else
            {
                FixedGuardNode __fixedGuard = append(new FixedGuardNode(__condition, DeoptimizationReason.ClassCastException, DeoptimizationAction.InvalidateReprofile, false));
                __castNode = append(PiNode.create(__object, StampFactory.object(__checkedType, __nonNull), __fixedGuard));
            }
        }
        frameState.push(JavaKind.Object, __castNode);
    }

    private void genInstanceOf()
    {
        int __cpi = getStream().readCPI();
        JavaType __type = lookupType(__cpi, Bytecodes.INSTANCEOF);
        ValueNode __object = frameState.pop(JavaKind.Object);

        if (!(__type instanceof ResolvedJavaType))
        {
            handleUnresolvedInstanceOf(__type, __object);
            return;
        }
        TypeReference __resolvedType = TypeReference.createTrusted(graph.getAssumptions(), (ResolvedJavaType) __type);
        JavaTypeProfile __profile = getProfileForTypeCheck(__resolvedType);

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleInstanceOf(this, __object, __resolvedType.getType(), __profile))
            {
                return;
            }
        }

        LogicNode __instanceOfNode = null;
        if (__profile != null)
        {
            if (__profile.getNullSeen().isFalse())
            {
                __object = nullCheckedValue(__object);
                ResolvedJavaType __singleType = __profile.asSingleType();
                if (__singleType != null)
                {
                    LogicNode __typeCheck = append(createInstanceOf(TypeReference.createExactTrusted(__singleType), __object, __profile));
                    if (!__typeCheck.isTautology())
                    {
                        append(new FixedGuardNode(__typeCheck, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile));
                    }
                    __instanceOfNode = LogicConstantNode.forBoolean(__resolvedType.getType().isAssignableFrom(__singleType));
                }
            }
        }
        if (__instanceOfNode == null)
        {
            __instanceOfNode = createInstanceOf(__resolvedType, __object, null);
        }
        LogicNode __logicNode = genUnique(__instanceOfNode);

        int __next = getStream().nextBCI();
        int __value = getStream().readUByte(__next);
        if (__next <= currentBlock.endBci && (__value == Bytecodes.IFEQ || __value == Bytecodes.IFNE))
        {
            getStream().next();
            BciBlock __firstSucc = currentBlock.getSuccessor(0);
            BciBlock __secondSucc = currentBlock.getSuccessor(1);
            if (__firstSucc != __secondSucc)
            {
                boolean __negate = __value != Bytecodes.IFNE;
                if (__negate)
                {
                    BciBlock __tmp = __firstSucc;
                    __firstSucc = __secondSucc;
                    __secondSucc = __tmp;
                }
                genIf(__instanceOfNode, __firstSucc, __secondSucc, getProfileProbability(__negate));
            }
            else
            {
                appendGoto(__firstSucc);
            }
        }
        else
        {
            // Most frequent for value is IRETURN, followed by ISTORE.
            frameState.push(JavaKind.Int, append(genConditional(__logicNode)));
        }
    }

    protected void genNewInstance(int __cpi)
    {
        JavaType __type = lookupType(__cpi, Bytecodes.NEW);
        genNewInstance(__type);
    }

    void genNewInstance(JavaType __type)
    {
        if (!(__type instanceof ResolvedJavaType))
        {
            handleUnresolvedNewInstance(__type);
            return;
        }
        ResolvedJavaType __resolvedType = (ResolvedJavaType) __type;
        if (!__resolvedType.isInitialized())
        {
            handleUnresolvedNewInstance(__type);
            return;
        }

        ResolvedJavaType[] __skippedExceptionTypes = this.graphBuilderConfig.getSkippedExceptionTypes();
        if (__skippedExceptionTypes != null)
        {
            for (ResolvedJavaType __exceptionType : __skippedExceptionTypes)
            {
                if (__exceptionType.isAssignableFrom(__resolvedType))
                {
                    append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.RuntimeConstraint));
                    return;
                }
            }
        }

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleNewInstance(this, __resolvedType))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewInstance(__resolvedType, true)));
    }

    /**
     * Gets the kind of array elements for the array type code that appears in a
     * {@link Bytecodes#NEWARRAY} bytecode.
     *
     * @param code the array type code
     * @return the kind from the array type code
     */
    private static Class<?> arrayTypeCodeToClass(int __code)
    {
        switch (__code)
        {
            case 4:
                return boolean.class;
            case 5:
                return char.class;
            case 6:
                return float.class;
            case 7:
                return double.class;
            case 8:
                return byte.class;
            case 9:
                return short.class;
            case 10:
                return int.class;
            case 11:
                return long.class;
            default:
                throw new IllegalArgumentException("unknown array type code: " + __code);
        }
    }

    private void genNewPrimitiveArray(int __typeCode)
    {
        ResolvedJavaType __elementType = metaAccess.lookupJavaType(arrayTypeCodeToClass(__typeCode));
        ValueNode __length = frameState.pop(JavaKind.Int);

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleNewArray(this, __elementType, __length))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewArray(__elementType, __length, true)));
    }

    private void genNewObjectArray(int __cpi)
    {
        JavaType __type = lookupType(__cpi, Bytecodes.ANEWARRAY);

        if (!(__type instanceof ResolvedJavaType))
        {
            ValueNode __length = frameState.pop(JavaKind.Int);
            handleUnresolvedNewObjectArray(__type, __length);
            return;
        }

        ResolvedJavaType __resolvedType = (ResolvedJavaType) __type;

        ValueNode __length = frameState.pop(JavaKind.Int);
        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleNewArray(this, __resolvedType, __length))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewArray(__resolvedType, __length, true)));
    }

    private void genNewMultiArray(int __cpi)
    {
        JavaType __type = lookupType(__cpi, Bytecodes.MULTIANEWARRAY);
        int __rank = getStream().readUByte(bci() + 3);
        ValueNode[] __dims = new ValueNode[__rank];

        if (!(__type instanceof ResolvedJavaType))
        {
            for (int __i = __rank - 1; __i >= 0; __i--)
            {
                __dims[__i] = frameState.pop(JavaKind.Int);
            }
            handleUnresolvedNewMultiArray(__type, __dims);
            return;
        }
        ResolvedJavaType __resolvedType = (ResolvedJavaType) __type;

        for (int __i = __rank - 1; __i >= 0; __i--)
        {
            __dims[__i] = frameState.pop(JavaKind.Int);
        }

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleNewMultiArray(this, __resolvedType, __dims))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewMultiArray(__resolvedType, __dims)));
    }

    protected void genGetField(int __cpi, int __opcode)
    {
        genGetField(__cpi, __opcode, frameState.pop(JavaKind.Object));
    }

    protected void genGetField(int __cpi, int __opcode, ValueNode __receiverInput)
    {
        JavaField __field = lookupField(__cpi, __opcode);
        genGetField(__field, __receiverInput);
    }

    private void genGetField(JavaField __field, ValueNode __receiverInput)
    {
        ValueNode __receiver = emitExplicitExceptions(__receiverInput);
        if (__field instanceof ResolvedJavaField)
        {
            ResolvedJavaField __resolvedField = (ResolvedJavaField) __field;
            genGetField(__resolvedField, __receiver);
        }
        else
        {
            handleUnresolvedLoadField(__field, __receiver);
        }
    }

    private void genGetField(ResolvedJavaField __resolvedField, ValueNode __receiver)
    {
        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleLoadField(this, __receiver, __resolvedField))
            {
                return;
            }
        }

        ValueNode __fieldRead = append(genLoadField(__receiver, __resolvedField));

        if (__resolvedField.getDeclaringClass().getName().equals("Ljava/lang/ref/Reference;") && __resolvedField.getName().equals("referent"))
        {
            LocationIdentity __referentIdentity = new FieldLocationIdentity(__resolvedField);
            append(new MembarNode(0, __referentIdentity));
        }

        JavaKind __fieldKind = __resolvedField.getJavaKind();

        if (__resolvedField.isVolatile() && __fieldRead instanceof LoadFieldNode)
        {
            StateSplitProxyNode __readProxy = append(genVolatileFieldReadProxy(__fieldRead));
            frameState.push(__fieldKind, __readProxy);
            __readProxy.setStateAfter(frameState.create(stream.nextBCI(), __readProxy));
        }
        else
        {
            frameState.push(__fieldKind, __fieldRead);
        }
    }

    /**
     * @param receiver the receiver of an object based operation
     * @param index the index of an array based operation that is to be tested for out of bounds.
     *            This is null for a non-array operation.
     * @return the receiver value possibly modified to have a non-null stamp
     */
    protected ValueNode emitExplicitExceptions(ValueNode __receiver, ValueNode __index)
    {
        if (needsExplicitException())
        {
            ValueNode __nonNullReceiver = emitExplicitNullCheck(__receiver);
            ValueNode __length = append(genArrayLength(__nonNullReceiver));
            emitExplicitBoundsCheck(__index, __length);
            return __nonNullReceiver;
        }
        return __receiver;
    }

    protected ValueNode emitExplicitExceptions(ValueNode __receiver)
    {
        if (StampTool.isPointerNonNull(__receiver) || !needsExplicitException())
        {
            return __receiver;
        }
        else
        {
            return emitExplicitNullCheck(__receiver);
        }
    }

    protected boolean needsExplicitException()
    {
        BytecodeExceptionMode __exceptionMode = graphBuilderConfig.getBytecodeExceptionMode();
        if (__exceptionMode == BytecodeExceptionMode.CheckAll || GraalOptions.stressExplicitExceptionCode)
        {
            return true;
        }
        else if (__exceptionMode == BytecodeExceptionMode.Profile && profilingInfo != null)
        {
            return profilingInfo.getExceptionSeen(bci()) == TriState.TRUE;
        }
        return false;
    }

    protected void genPutField(int __cpi, int __opcode)
    {
        JavaField __field = lookupField(__cpi, __opcode);
        genPutField(__field);
    }

    protected void genPutField(JavaField __field)
    {
        genPutField(__field, frameState.pop(__field.getJavaKind()));
    }

    private void genPutField(JavaField __field, ValueNode __value)
    {
        ValueNode __receiver = emitExplicitExceptions(frameState.pop(JavaKind.Object));
        if (__field instanceof ResolvedJavaField)
        {
            ResolvedJavaField __resolvedField = (ResolvedJavaField) __field;

            for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
            {
                if (__plugin.handleStoreField(this, __receiver, __resolvedField, __value))
                {
                    return;
                }
            }

            if (__resolvedField.isFinal() && method.isConstructor())
            {
                finalBarrierRequired = true;
            }
            genStoreField(__receiver, __resolvedField, __value);
        }
        else
        {
            handleUnresolvedStoreField(__field, __value, __receiver);
        }
    }

    protected void genGetStatic(int __cpi, int __opcode)
    {
        JavaField __field = lookupField(__cpi, __opcode);
        genGetStatic(__field);
    }

    private void genGetStatic(JavaField __field)
    {
        ResolvedJavaField __resolvedField = resolveStaticFieldAccess(__field, null);
        if (__resolvedField == null)
        {
            return;
        }

        /*
         * Javac does not allow use of "$assertionsDisabled" for a field name but Eclipse does, in
         * which case a suffix is added to the generated field.
         */
        if (__resolvedField.isSynthetic() && __resolvedField.getName().startsWith("$assertionsDisabled"))
        {
            frameState.push(__field.getJavaKind(), ConstantNode.forBoolean(true, graph));
            return;
        }

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleLoadStaticField(this, __resolvedField))
            {
                return;
            }
        }

        frameState.push(__field.getJavaKind(), append(genLoadField(null, __resolvedField)));
    }

    private ResolvedJavaField resolveStaticFieldAccess(JavaField __field, ValueNode __value)
    {
        if (__field instanceof ResolvedJavaField)
        {
            ResolvedJavaField __resolvedField = (ResolvedJavaField) __field;
            if (__resolvedField.getDeclaringClass().isInitialized())
            {
                return __resolvedField;
            }
            /*
             * Static fields have initialization semantics but may be safely accessed under certain
             * conditions while the class is being initialized. Executing in the clinit or init of
             * classes which are subtypes of the field holder are sure to be running in a context
             * where the access is safe.
             */
            if (__resolvedField.getDeclaringClass().isAssignableFrom(method.getDeclaringClass()))
            {
                if (method.isClassInitializer() || method.isConstructor())
                {
                    return __resolvedField;
                }
            }
        }
        if (__value == null)
        {
            handleUnresolvedLoadField(__field, null);
        }
        else
        {
            handleUnresolvedStoreField(__field, __value, null);
        }
        return null;
    }

    protected void genPutStatic(int __cpi, int __opcode)
    {
        JavaField __field = lookupField(__cpi, __opcode);
        genPutStatic(__field);
    }

    protected void genPutStatic(JavaField __field)
    {
        ValueNode __value = frameState.pop(__field.getJavaKind());
        ResolvedJavaField __resolvedField = resolveStaticFieldAccess(__field, __value);
        if (__resolvedField == null)
        {
            return;
        }

        for (NodePlugin __plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (__plugin.handleStoreStaticField(this, __resolvedField, __value))
            {
                return;
            }
        }

        genStoreField(null, __resolvedField, __value);
    }

    private double[] switchProbability(int __numberOfCases, int __bci)
    {
        double[] __prob = (profilingInfo == null ? null : profilingInfo.getSwitchProbabilities(__bci));
        if (__prob == null)
        {
            __prob = new double[__numberOfCases];
            for (int __i = 0; __i < __numberOfCases; __i++)
            {
                __prob[__i] = 1.0d / __numberOfCases;
            }
        }
        return __prob;
    }

    private static boolean allPositive(double[] __a)
    {
        for (double __d : __a)
        {
            if (__d < 0)
            {
                return false;
            }
        }
        return true;
    }

    // @class BytecodeParser.SuccessorInfo
    static final class SuccessorInfo
    {
        // @field
        final int blockIndex;
        // @field
        int actualIndex;

        // @cons
        SuccessorInfo(int __blockSuccessorIndex)
        {
            super();
            this.blockIndex = __blockSuccessorIndex;
            actualIndex = -1;
        }
    }

    private void genSwitch(BytecodeSwitch __bs)
    {
        int __bci = bci();
        ValueNode __value = frameState.pop(JavaKind.Int);

        int __nofCases = __bs.numberOfCases();
        int __nofCasesPlusDefault = __nofCases + 1;
        double[] __keyProbabilities = switchProbability(__nofCasesPlusDefault, __bci);

        EconomicMap<Integer, SuccessorInfo> __bciToBlockSuccessorIndex = EconomicMap.create(Equivalence.DEFAULT);
        for (int __i = 0; __i < currentBlock.getSuccessorCount(); __i++)
        {
            __bciToBlockSuccessorIndex.put(currentBlock.getSuccessor(__i).startBci, new SuccessorInfo(__i));
        }

        ArrayList<BciBlock> __actualSuccessors = new ArrayList<>();
        int[] __keys = new int[__nofCases];
        int[] __keySuccessors = new int[__nofCasesPlusDefault];
        int __deoptSuccessorIndex = -1;
        int __nextSuccessorIndex = 0;
        boolean __constantValue = __value.isConstant();
        for (int __i = 0; __i < __nofCasesPlusDefault; __i++)
        {
            if (__i < __nofCases)
            {
                __keys[__i] = __bs.keyAt(__i);
            }

            if (!__constantValue && isNeverExecutedCode(__keyProbabilities[__i]))
            {
                if (__deoptSuccessorIndex < 0)
                {
                    __deoptSuccessorIndex = __nextSuccessorIndex++;
                    __actualSuccessors.add(null);
                }
                __keySuccessors[__i] = __deoptSuccessorIndex;
            }
            else
            {
                int __targetBci = __i < __nofCases ? __bs.targetAt(__i) : __bs.defaultTarget();
                SuccessorInfo __info = __bciToBlockSuccessorIndex.get(__targetBci);
                if (__info.actualIndex < 0)
                {
                    __info.actualIndex = __nextSuccessorIndex++;
                    __actualSuccessors.add(currentBlock.getSuccessor(__info.blockIndex));
                }
                __keySuccessors[__i] = __info.actualIndex;
            }
        }
        /*
         * When the profile indicates a case is never taken, the above code will cause the case to
         * deopt should it be subsequently encountered. However, the case may share code with
         * another case that is taken according to the profile.
         *
         * For example:
         *
         * switch (opcode) {
         *     case GOTO:
         *     case GOTO_W: {
         *         // emit goto code
         *         break;
         *     }
         * }
         *
         * The profile may indicate the GOTO_W case is never taken, and thus a deoptimization stub
         * will be emitted. There might be optimization opportunity if additional branching based
         * on opcode is within the case block. Specially, if there is only single case that
         * reaches a target, we have better chance cutting out unused branches. Otherwise,
         * it might be beneficial routing to the same code instead of deopting.
         *
         * The following code rewires deoptimization stub to existing resolved branch target if
         * the target is connected by more than 1 cases.
         */
        if (__deoptSuccessorIndex >= 0)
        {
            int[] __connectedCases = new int[__nextSuccessorIndex];
            for (int __i = 0; __i < __nofCasesPlusDefault; __i++)
            {
                __connectedCases[__keySuccessors[__i]]++;
            }

            for (int __i = 0; __i < __nofCasesPlusDefault; __i++)
            {
                if (__keySuccessors[__i] == __deoptSuccessorIndex)
                {
                    int __targetBci = __i < __nofCases ? __bs.targetAt(__i) : __bs.defaultTarget();
                    SuccessorInfo __info = __bciToBlockSuccessorIndex.get(__targetBci);
                    int __rewiredIndex = __info.actualIndex;
                    if (__rewiredIndex >= 0 && __connectedCases[__rewiredIndex] > 1)
                    {
                        __keySuccessors[__i] = __info.actualIndex;
                    }
                }
            }
        }

        genIntegerSwitch(__value, __actualSuccessors, __keys, __keyProbabilities, __keySuccessors);
    }

    protected boolean isNeverExecutedCode(double __probability)
    {
        return __probability == 0 && optimisticOpts.removeNeverExecutedCode();
    }

    private double clampProbability(double __probability)
    {
        if (!optimisticOpts.removeNeverExecutedCode())
        {
            if (__probability == 0)
            {
                return 0.0000001;
            }
            else if (__probability == 1)
            {
                return 0.999999;
            }
        }
        return __probability;
    }

    private boolean assertAtIfBytecode()
    {
        int __bytecode = stream.currentBC();
        switch (__bytecode)
        {
            case Bytecodes.IFEQ:
            case Bytecodes.IFNE:
            case Bytecodes.IFLT:
            case Bytecodes.IFGE:
            case Bytecodes.IFGT:
            case Bytecodes.IFLE:
            case Bytecodes.IF_ICMPEQ:
            case Bytecodes.IF_ICMPNE:
            case Bytecodes.IF_ICMPLT:
            case Bytecodes.IF_ICMPGE:
            case Bytecodes.IF_ICMPGT:
            case Bytecodes.IF_ICMPLE:
            case Bytecodes.IF_ACMPEQ:
            case Bytecodes.IF_ACMPNE:
            case Bytecodes.IFNULL:
            case Bytecodes.IFNONNULL:
                return true;
        }
        return true;
    }

    public final void processBytecode(int __bci, int __opcode)
    {
        int __cpi;

        switch (__opcode)
        {
            case Bytecodes.NOP:             /* nothing to do */ break;
            case Bytecodes.ACONST_NULL:     frameState.push(JavaKind.Object, appendConstant(JavaConstant.NULL_POINTER)); break;
            case Bytecodes.ICONST_M1:       // fall through
            case Bytecodes.ICONST_0:        // fall through
            case Bytecodes.ICONST_1:        // fall through
            case Bytecodes.ICONST_2:        // fall through
            case Bytecodes.ICONST_3:        // fall through
            case Bytecodes.ICONST_4:        // fall through
            case Bytecodes.ICONST_5:        frameState.push(JavaKind.Int, appendConstant(JavaConstant.forInt(__opcode - Bytecodes.ICONST_0))); break;
            case Bytecodes.LCONST_0:        // fall through
            case Bytecodes.LCONST_1:        frameState.push(JavaKind.Long, appendConstant(JavaConstant.forLong(__opcode - Bytecodes.LCONST_0))); break;
            case Bytecodes.FCONST_0:        // fall through
            case Bytecodes.FCONST_1:        // fall through
            case Bytecodes.FCONST_2:        frameState.push(JavaKind.Float, appendConstant(JavaConstant.forFloat(__opcode - Bytecodes.FCONST_0))); break;
            case Bytecodes.DCONST_0:        // fall through
            case Bytecodes.DCONST_1:        frameState.push(JavaKind.Double, appendConstant(JavaConstant.forDouble(__opcode - Bytecodes.DCONST_0))); break;
            case Bytecodes.BIPUSH:          frameState.push(JavaKind.Int, appendConstant(JavaConstant.forInt(stream.readByte()))); break;
            case Bytecodes.SIPUSH:          frameState.push(JavaKind.Int, appendConstant(JavaConstant.forInt(stream.readShort()))); break;
            case Bytecodes.LDC:             // fall through
            case Bytecodes.LDC_W:           // fall through
            case Bytecodes.LDC2_W:          genLoadConstant(stream.readCPI(), __opcode); break;
            case Bytecodes.ILOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Int); break;
            case Bytecodes.LLOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Long); break;
            case Bytecodes.FLOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Float); break;
            case Bytecodes.DLOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Double); break;
            case Bytecodes.ALOAD:           loadLocalObject(stream.readLocalIndex()); break;
            case Bytecodes.ILOAD_0:         // fall through
            case Bytecodes.ILOAD_1:         // fall through
            case Bytecodes.ILOAD_2:         // fall through
            case Bytecodes.ILOAD_3:         loadLocal(__opcode - Bytecodes.ILOAD_0, JavaKind.Int); break;
            case Bytecodes.LLOAD_0:         // fall through
            case Bytecodes.LLOAD_1:         // fall through
            case Bytecodes.LLOAD_2:         // fall through
            case Bytecodes.LLOAD_3:         loadLocal(__opcode - Bytecodes.LLOAD_0, JavaKind.Long); break;
            case Bytecodes.FLOAD_0:         // fall through
            case Bytecodes.FLOAD_1:         // fall through
            case Bytecodes.FLOAD_2:         // fall through
            case Bytecodes.FLOAD_3:         loadLocal(__opcode - Bytecodes.FLOAD_0, JavaKind.Float); break;
            case Bytecodes.DLOAD_0:         // fall through
            case Bytecodes.DLOAD_1:         // fall through
            case Bytecodes.DLOAD_2:         // fall through
            case Bytecodes.DLOAD_3:         loadLocal(__opcode - Bytecodes.DLOAD_0, JavaKind.Double); break;
            case Bytecodes.ALOAD_0:         // fall through
            case Bytecodes.ALOAD_1:         // fall through
            case Bytecodes.ALOAD_2:         // fall through
            case Bytecodes.ALOAD_3:         loadLocalObject(__opcode - Bytecodes.ALOAD_0); break;
            case Bytecodes.IALOAD:          genLoadIndexed(JavaKind.Int); break;
            case Bytecodes.LALOAD:          genLoadIndexed(JavaKind.Long); break;
            case Bytecodes.FALOAD:          genLoadIndexed(JavaKind.Float); break;
            case Bytecodes.DALOAD:          genLoadIndexed(JavaKind.Double); break;
            case Bytecodes.AALOAD:          genLoadIndexed(JavaKind.Object); break;
            case Bytecodes.BALOAD:          genLoadIndexed(JavaKind.Byte); break;
            case Bytecodes.CALOAD:          genLoadIndexed(JavaKind.Char); break;
            case Bytecodes.SALOAD:          genLoadIndexed(JavaKind.Short); break;
            case Bytecodes.ISTORE:          storeLocal(JavaKind.Int, stream.readLocalIndex()); break;
            case Bytecodes.LSTORE:          storeLocal(JavaKind.Long, stream.readLocalIndex()); break;
            case Bytecodes.FSTORE:          storeLocal(JavaKind.Float, stream.readLocalIndex()); break;
            case Bytecodes.DSTORE:          storeLocal(JavaKind.Double, stream.readLocalIndex()); break;
            case Bytecodes.ASTORE:          storeLocal(JavaKind.Object, stream.readLocalIndex()); break;
            case Bytecodes.ISTORE_0:        // fall through
            case Bytecodes.ISTORE_1:        // fall through
            case Bytecodes.ISTORE_2:        // fall through
            case Bytecodes.ISTORE_3:        storeLocal(JavaKind.Int, __opcode - Bytecodes.ISTORE_0); break;
            case Bytecodes.LSTORE_0:        // fall through
            case Bytecodes.LSTORE_1:        // fall through
            case Bytecodes.LSTORE_2:        // fall through
            case Bytecodes.LSTORE_3:        storeLocal(JavaKind.Long, __opcode - Bytecodes.LSTORE_0); break;
            case Bytecodes.FSTORE_0:        // fall through
            case Bytecodes.FSTORE_1:        // fall through
            case Bytecodes.FSTORE_2:        // fall through
            case Bytecodes.FSTORE_3:        storeLocal(JavaKind.Float, __opcode - Bytecodes.FSTORE_0); break;
            case Bytecodes.DSTORE_0:        // fall through
            case Bytecodes.DSTORE_1:        // fall through
            case Bytecodes.DSTORE_2:        // fall through
            case Bytecodes.DSTORE_3:        storeLocal(JavaKind.Double, __opcode - Bytecodes.DSTORE_0); break;
            case Bytecodes.ASTORE_0:        // fall through
            case Bytecodes.ASTORE_1:        // fall through
            case Bytecodes.ASTORE_2:        // fall through
            case Bytecodes.ASTORE_3:        storeLocal(JavaKind.Object, __opcode - Bytecodes.ASTORE_0); break;
            case Bytecodes.IASTORE:         genStoreIndexed(JavaKind.Int); break;
            case Bytecodes.LASTORE:         genStoreIndexed(JavaKind.Long); break;
            case Bytecodes.FASTORE:         genStoreIndexed(JavaKind.Float); break;
            case Bytecodes.DASTORE:         genStoreIndexed(JavaKind.Double); break;
            case Bytecodes.AASTORE:         genStoreIndexed(JavaKind.Object); break;
            case Bytecodes.BASTORE:         genStoreIndexed(JavaKind.Byte); break;
            case Bytecodes.CASTORE:         genStoreIndexed(JavaKind.Char); break;
            case Bytecodes.SASTORE:         genStoreIndexed(JavaKind.Short); break;
            case Bytecodes.POP:             // fall through
            case Bytecodes.POP2:            // fall through
            case Bytecodes.DUP:             // fall through
            case Bytecodes.DUP_X1:          // fall through
            case Bytecodes.DUP_X2:          // fall through
            case Bytecodes.DUP2:            // fall through
            case Bytecodes.DUP2_X1:         // fall through
            case Bytecodes.DUP2_X2:         // fall through
            case Bytecodes.SWAP:            frameState.stackOp(__opcode); break;
            case Bytecodes.IADD:            // fall through
            case Bytecodes.ISUB:            // fall through
            case Bytecodes.IMUL:            genArithmeticOp(JavaKind.Int, __opcode); break;
            case Bytecodes.IDIV:            // fall through
            case Bytecodes.IREM:            genIntegerDivOp(JavaKind.Int, __opcode); break;
            case Bytecodes.LADD:            // fall through
            case Bytecodes.LSUB:            // fall through
            case Bytecodes.LMUL:            genArithmeticOp(JavaKind.Long, __opcode); break;
            case Bytecodes.LDIV:            // fall through
            case Bytecodes.LREM:            genIntegerDivOp(JavaKind.Long, __opcode); break;
            case Bytecodes.FADD:            // fall through
            case Bytecodes.FSUB:            // fall through
            case Bytecodes.FMUL:            // fall through
            case Bytecodes.FDIV:            // fall through
            case Bytecodes.FREM:            genArithmeticOp(JavaKind.Float, __opcode); break;
            case Bytecodes.DADD:            // fall through
            case Bytecodes.DSUB:            // fall through
            case Bytecodes.DMUL:            // fall through
            case Bytecodes.DDIV:            // fall through
            case Bytecodes.DREM:            genArithmeticOp(JavaKind.Double, __opcode); break;
            case Bytecodes.INEG:            genNegateOp(JavaKind.Int); break;
            case Bytecodes.LNEG:            genNegateOp(JavaKind.Long); break;
            case Bytecodes.FNEG:            genNegateOp(JavaKind.Float); break;
            case Bytecodes.DNEG:            genNegateOp(JavaKind.Double); break;
            case Bytecodes.ISHL:            // fall through
            case Bytecodes.ISHR:            // fall through
            case Bytecodes.IUSHR:           genShiftOp(JavaKind.Int, __opcode); break;
            case Bytecodes.IAND:            // fall through
            case Bytecodes.IOR:             // fall through
            case Bytecodes.IXOR:            genLogicOp(JavaKind.Int, __opcode); break;
            case Bytecodes.LSHL:            // fall through
            case Bytecodes.LSHR:            // fall through
            case Bytecodes.LUSHR:           genShiftOp(JavaKind.Long, __opcode); break;
            case Bytecodes.LAND:            // fall through
            case Bytecodes.LOR:             // fall through
            case Bytecodes.LXOR:            genLogicOp(JavaKind.Long, __opcode); break;
            case Bytecodes.IINC:            genIncrement(); break;
            case Bytecodes.I2F:             genFloatConvert(FloatConvert.I2F, JavaKind.Int, JavaKind.Float); break;
            case Bytecodes.I2D:             genFloatConvert(FloatConvert.I2D, JavaKind.Int, JavaKind.Double); break;
            case Bytecodes.L2F:             genFloatConvert(FloatConvert.L2F, JavaKind.Long, JavaKind.Float); break;
            case Bytecodes.L2D:             genFloatConvert(FloatConvert.L2D, JavaKind.Long, JavaKind.Double); break;
            case Bytecodes.F2I:             genFloatConvert(FloatConvert.F2I, JavaKind.Float, JavaKind.Int); break;
            case Bytecodes.F2L:             genFloatConvert(FloatConvert.F2L, JavaKind.Float, JavaKind.Long); break;
            case Bytecodes.F2D:             genFloatConvert(FloatConvert.F2D, JavaKind.Float, JavaKind.Double); break;
            case Bytecodes.D2I:             genFloatConvert(FloatConvert.D2I, JavaKind.Double, JavaKind.Int); break;
            case Bytecodes.D2L:             genFloatConvert(FloatConvert.D2L, JavaKind.Double, JavaKind.Long); break;
            case Bytecodes.D2F:             genFloatConvert(FloatConvert.D2F, JavaKind.Double, JavaKind.Float); break;
            case Bytecodes.L2I:             genNarrow(JavaKind.Long, JavaKind.Int); break;
            case Bytecodes.I2L:             genSignExtend(JavaKind.Int, JavaKind.Long); break;
            case Bytecodes.I2B:             genSignExtend(JavaKind.Byte, JavaKind.Int); break;
            case Bytecodes.I2S:             genSignExtend(JavaKind.Short, JavaKind.Int); break;
            case Bytecodes.I2C:             genZeroExtend(JavaKind.Char, JavaKind.Int); break;
            case Bytecodes.LCMP:            genCompareOp(JavaKind.Long, false); break;
            case Bytecodes.FCMPL:           genCompareOp(JavaKind.Float, true); break;
            case Bytecodes.FCMPG:           genCompareOp(JavaKind.Float, false); break;
            case Bytecodes.DCMPL:           genCompareOp(JavaKind.Double, true); break;
            case Bytecodes.DCMPG:           genCompareOp(JavaKind.Double, false); break;
            case Bytecodes.IFEQ:            genIfZero(Condition.EQ); break;
            case Bytecodes.IFNE:            genIfZero(Condition.NE); break;
            case Bytecodes.IFLT:            genIfZero(Condition.LT); break;
            case Bytecodes.IFGE:            genIfZero(Condition.GE); break;
            case Bytecodes.IFGT:            genIfZero(Condition.GT); break;
            case Bytecodes.IFLE:            genIfZero(Condition.LE); break;
            case Bytecodes.IF_ICMPEQ:       genIfSame(JavaKind.Int, Condition.EQ); break;
            case Bytecodes.IF_ICMPNE:       genIfSame(JavaKind.Int, Condition.NE); break;
            case Bytecodes.IF_ICMPLT:       genIfSame(JavaKind.Int, Condition.LT); break;
            case Bytecodes.IF_ICMPGE:       genIfSame(JavaKind.Int, Condition.GE); break;
            case Bytecodes.IF_ICMPGT:       genIfSame(JavaKind.Int, Condition.GT); break;
            case Bytecodes.IF_ICMPLE:       genIfSame(JavaKind.Int, Condition.LE); break;
            case Bytecodes.IF_ACMPEQ:       genIfSame(JavaKind.Object, Condition.EQ); break;
            case Bytecodes.IF_ACMPNE:       genIfSame(JavaKind.Object, Condition.NE); break;
            case Bytecodes.GOTO:            genGoto(); break;
            case Bytecodes.JSR:             genJsr(stream.readBranchDest()); break;
            case Bytecodes.RET:             genRet(stream.readLocalIndex()); break;
            case Bytecodes.TABLESWITCH:     genSwitch(new BytecodeTableSwitch(getStream(), bci())); break;
            case Bytecodes.LOOKUPSWITCH:    genSwitch(new BytecodeLookupSwitch(getStream(), bci())); break;
            case Bytecodes.IRETURN:         genReturn(frameState.pop(JavaKind.Int), JavaKind.Int); break;
            case Bytecodes.LRETURN:         genReturn(frameState.pop(JavaKind.Long), JavaKind.Long); break;
            case Bytecodes.FRETURN:         genReturn(frameState.pop(JavaKind.Float), JavaKind.Float); break;
            case Bytecodes.DRETURN:         genReturn(frameState.pop(JavaKind.Double), JavaKind.Double); break;
            case Bytecodes.ARETURN:         genReturn(frameState.pop(JavaKind.Object), JavaKind.Object); break;
            case Bytecodes.RETURN:          genReturn(null, JavaKind.Void); break;
            case Bytecodes.GETSTATIC:       __cpi = stream.readCPI(); genGetStatic(__cpi, __opcode); break;
            case Bytecodes.PUTSTATIC:       __cpi = stream.readCPI(); genPutStatic(__cpi, __opcode); break;
            case Bytecodes.GETFIELD:        __cpi = stream.readCPI(); genGetField(__cpi, __opcode); break;
            case Bytecodes.PUTFIELD:        __cpi = stream.readCPI(); genPutField(__cpi, __opcode); break;
            case Bytecodes.INVOKEVIRTUAL:   __cpi = stream.readCPI(); genInvokeVirtual(__cpi, __opcode); break;
            case Bytecodes.INVOKESPECIAL:   __cpi = stream.readCPI(); genInvokeSpecial(__cpi, __opcode); break;
            case Bytecodes.INVOKESTATIC:    __cpi = stream.readCPI(); genInvokeStatic(__cpi, __opcode); break;
            case Bytecodes.INVOKEINTERFACE: __cpi = stream.readCPI(); genInvokeInterface(__cpi, __opcode); break;
            case Bytecodes.INVOKEDYNAMIC:   __cpi = stream.readCPI4(); genInvokeDynamic(__cpi, __opcode); break;
            case Bytecodes.NEW:             genNewInstance(stream.readCPI()); break;
            case Bytecodes.NEWARRAY:        genNewPrimitiveArray(stream.readLocalIndex()); break;
            case Bytecodes.ANEWARRAY:       genNewObjectArray(stream.readCPI()); break;
            case Bytecodes.ARRAYLENGTH:     genArrayLength(); break;
            case Bytecodes.ATHROW:          genThrow(); break;
            case Bytecodes.CHECKCAST:       genCheckCast(); break;
            case Bytecodes.INSTANCEOF:      genInstanceOf(); break;
            case Bytecodes.MONITORENTER:    genMonitorEnter(frameState.pop(JavaKind.Object), stream.nextBCI()); break;
            case Bytecodes.MONITOREXIT:     genMonitorExit(frameState.pop(JavaKind.Object), null, stream.nextBCI()); break;
            case Bytecodes.MULTIANEWARRAY:  genNewMultiArray(stream.readCPI()); break;
            case Bytecodes.IFNULL:          genIfNull(Condition.EQ); break;
            case Bytecodes.IFNONNULL:       genIfNull(Condition.NE); break;
            case Bytecodes.GOTO_W:          genGoto(); break;
            case Bytecodes.JSR_W:           genJsr(stream.readBranchDest()); break;
            default:                        throw new BailoutException("unsupported opcode %d (%s) [bci=%d]", __opcode, Bytecodes.nameOf(__opcode), __bci);
        }
    }

    private void genArrayLength()
    {
        ValueNode __array = emitExplicitExceptions(frameState.pop(JavaKind.Object));
        frameState.push(JavaKind.Int, append(genArrayLength(__array)));
    }

    @Override
    public ResolvedJavaMethod getMethod()
    {
        return method;
    }

    @Override
    public Bytecode getCode()
    {
        return code;
    }

    public FrameStateBuilder getFrameStateBuilder()
    {
        return frameState;
    }

    @Override
    public boolean parsingIntrinsic()
    {
        return intrinsicContext != null;
    }

    @Override
    public BytecodeParser getNonIntrinsicAncestor()
    {
        BytecodeParser __ancestor = parent;
        while (__ancestor != null && __ancestor.parsingIntrinsic())
        {
            __ancestor = __ancestor.parent;
        }
        return __ancestor;
    }
}
