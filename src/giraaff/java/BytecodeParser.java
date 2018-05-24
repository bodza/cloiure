package giraaff.java;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.site.InfopointReason;
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
import jdk.vm.ci.meta.LineNumberTable;
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
import giraaff.bytecode.BytecodeDisassembler;
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
import giraaff.core.common.PermanentBailoutException;
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
import giraaff.java.BytecodeParserOptions;
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
import giraaff.nodes.FullInfopointNode;
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
import giraaff.options.OptionValues;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.util.ValueMergeUtil;
import giraaff.util.GraalError;

/**
 * The {@code GraphBuilder} class parses the bytecode of a method and builds the IR graph.
 */
public class BytecodeParser implements GraphBuilderContext
{
    /**
     * A scoped object for tasks to be performed after parsing an intrinsic such as processing
     * {@linkplain BytecodeFrame#isPlaceholderBci(int) placeholder} frames states.
     */
    static class IntrinsicScope implements AutoCloseable
    {
        FrameState stateBefore;
        final Mark mark;
        final BytecodeParser parser;
        List<ReturnToCallerData> returnDataList;

        /**
         * Creates a scope for root parsing an intrinsic.
         *
         * @param parser the parsing context of the intrinsic
         */
        IntrinsicScope(BytecodeParser parser)
        {
            this.parser = parser;
            mark = null;
        }

        /**
         * Creates a scope for parsing an intrinsic during graph builder inlining.
         *
         * @param parser the parsing context of the (non-intrinsic) method calling the intrinsic
         * @param args the arguments to the call
         */
        IntrinsicScope(BytecodeParser parser, JavaKind[] argSlotKinds, ValueNode[] args)
        {
            this.parser = parser;
            mark = parser.getGraph().getMark();
            stateBefore = parser.frameState.create(parser.bci(), parser.getNonIntrinsicAncestor(), false, argSlotKinds, args);
        }

        @Override
        public void close()
        {
            IntrinsicContext intrinsic = parser.intrinsicContext;
            if (intrinsic != null && intrinsic.isPostParseInlined())
            {
                return;
            }

            processPlaceholderFrameStates(intrinsic);
        }

        /**
         * Fixes up the {@linkplain BytecodeFrame#isPlaceholderBci(int) placeholder} frame states
         * added to the graph while parsing/inlining the intrinsic for which this object exists.
         */
        private void processPlaceholderFrameStates(IntrinsicContext intrinsic)
        {
            StructuredGraph graph = parser.getGraph();
            boolean sawInvalidFrameState = false;
            for (Node node : graph.getNewNodes(mark))
            {
                if (node instanceof FrameState)
                {
                    FrameState frameState = (FrameState) node;
                    if (BytecodeFrame.isPlaceholderBci(frameState.bci))
                    {
                        if (frameState.bci == BytecodeFrame.AFTER_BCI)
                        {
                            if (parser.getInvokeReturnType() == null)
                            {
                                // A frame state in a root compiled intrinsic.
                                FrameState newFrameState = graph.add(new FrameState(BytecodeFrame.INVALID_FRAMESTATE_BCI));
                                frameState.replaceAndDelete(newFrameState);
                            }
                            else
                            {
                                JavaKind returnKind = parser.getInvokeReturnType().getJavaKind();
                                FrameStateBuilder frameStateBuilder = parser.frameState;
                                if (frameState.stackSize() != 0)
                                {
                                    ValueNode returnVal = frameState.stackAt(0);
                                    if (!ReturnToCallerData.containsReturnValue(returnDataList, returnVal))
                                    {
                                        throw new GraalError("AFTER_BCI frame state within an intrinsic has a non-return value on the stack: %s", returnVal);
                                    }

                                    // Swap the top-of-stack value with the return value
                                    ValueNode tos = frameStateBuilder.pop(returnKind);
                                    FrameState newFrameState = frameStateBuilder.create(parser.stream.nextBCI(), parser.getNonIntrinsicAncestor(), false, new JavaKind[] { returnKind }, new ValueNode[] { returnVal });
                                    frameState.replaceAndDelete(newFrameState);
                                    frameStateBuilder.push(returnKind, tos);
                                }
                                else if (returnKind != JavaKind.Void)
                                {
                                    // If the intrinsic returns a non-void value, then any frame
                                    // state with an empty stack is invalid as it cannot
                                    // be used to deoptimize to just after the call returns.
                                    // These invalid frame states are expected to be removed
                                    // by later compilation stages.
                                    FrameState newFrameState = graph.add(new FrameState(BytecodeFrame.INVALID_FRAMESTATE_BCI));
                                    frameState.replaceAndDelete(newFrameState);
                                    sawInvalidFrameState = true;
                                }
                                else
                                {
                                    // An intrinsic for a void method.
                                    FrameState newFrameState = frameStateBuilder.create(parser.stream.nextBCI(), null);
                                    frameState.replaceAndDelete(newFrameState);
                                }
                            }
                        }
                        else if (frameState.bci == BytecodeFrame.BEFORE_BCI)
                        {
                            if (stateBefore == null)
                            {
                                stateBefore = graph.start().stateAfter();
                            }
                            if (stateBefore != frameState)
                            {
                                frameState.replaceAndDelete(stateBefore);
                            }
                        }
                        else if (frameState.bci == BytecodeFrame.AFTER_EXCEPTION_BCI)
                        {
                            // This is a frame state for the entry point to an exception
                            // dispatcher in an intrinsic. For example, the invoke denoting
                            // a partial intrinsic exit will have an edge to such a
                            // dispatcher if the profile for the original invoke being
                            // intrinsified indicates an exception was seen. As per JVM
                            // bytecode semantics, the interpreter expects a single
                            // value on the stack on entry to an exception handler,
                            // namely the exception object.
                            ValueNode exceptionValue = frameState.stackAt(0);
                            ExceptionObjectNode exceptionObject = (ExceptionObjectNode) GraphUtil.unproxify(exceptionValue);
                            FrameStateBuilder dispatchState = parser.frameState.copy();
                            dispatchState.clearStack();
                            dispatchState.push(JavaKind.Object, exceptionValue);
                            dispatchState.setRethrowException(true);
                            FrameState newFrameState = dispatchState.create(parser.bci(), exceptionObject);
                            frameState.replaceAndDelete(newFrameState);
                        }
                    }
                }
            }
            if (sawInvalidFrameState)
            {
                JavaKind returnKind = parser.getInvokeReturnType().getJavaKind();
                FrameStateBuilder frameStateBuilder = parser.frameState;
                ValueNode returnValue = frameStateBuilder.pop(returnKind);
                StateSplitProxyNode proxy = graph.add(new StateSplitProxyNode(returnValue));
                parser.lastInstr.setNext(proxy);
                frameStateBuilder.push(returnKind, proxy);
                proxy.setStateAfter(parser.createFrameState(parser.stream.nextBCI(), proxy));
                parser.lastInstr = proxy;
            }
        }
    }

    private static class Target
    {
        FixedNode fixed;
        FrameStateBuilder state;

        Target(FixedNode fixed, FrameStateBuilder state)
        {
            this.fixed = fixed;
            this.state = state;
        }
    }

    @SuppressWarnings("serial")
    public static class BytecodeParserError extends GraalError
    {
        public BytecodeParserError(Throwable cause)
        {
            super(cause);
        }

        public BytecodeParserError(String msg, Object... args)
        {
            super(msg, args);
        }
    }

    protected static class ReturnToCallerData
    {
        protected final ValueNode returnValue;
        protected final FixedWithNextNode beforeReturnNode;

        protected ReturnToCallerData(ValueNode returnValue, FixedWithNextNode beforeReturnNode)
        {
            this.returnValue = returnValue;
            this.beforeReturnNode = beforeReturnNode;
        }

        static boolean containsReturnValue(List<ReturnToCallerData> list, ValueNode value)
        {
            for (ReturnToCallerData e : list)
            {
                if (e.returnValue == value)
                {
                    return true;
                }
            }
            return false;
        }
    }

    private final GraphBuilderPhase.Instance graphBuilderInstance;
    protected final StructuredGraph graph;
    protected final OptionValues options;

    private BciBlockMapping blockMap;
    private LocalLiveness liveness;
    protected final int entryBCI;
    private final BytecodeParser parent;

    private LineNumberTable lnt;
    private int previousLineNumber;
    private int currentLineNumber;

    private ValueNode methodSynchronizedObject;

    private List<ReturnToCallerData> returnDataList;
    private ValueNode unwindValue;
    private FixedWithNextNode beforeUnwindNode;

    protected FixedWithNextNode lastInstr;                 // the last instruction added
    private boolean controlFlowSplit;
    private final InvocationPluginReceiver invocationPluginReceiver = new InvocationPluginReceiver(this);

    private FixedWithNextNode[] firstInstructionArray;
    private FrameStateBuilder[] entryStateArray;

    private boolean finalBarrierRequired;
    private ValueNode originalReceiver;
    private final boolean eagerInitializing;
    private final boolean uninitializedIsError;

    protected BytecodeParser(GraphBuilderPhase.Instance graphBuilderInstance, StructuredGraph graph, BytecodeParser parent, ResolvedJavaMethod method, int entryBCI, IntrinsicContext intrinsicContext)
    {
        this.bytecodeProvider = intrinsicContext == null ? new ResolvedJavaMethodBytecodeProvider() : intrinsicContext.getBytecodeProvider();
        this.code = bytecodeProvider.getBytecode(method);
        this.method = code.getMethod();
        this.graphBuilderInstance = graphBuilderInstance;
        this.graph = graph;
        this.options = graph.getOptions();
        this.graphBuilderConfig = graphBuilderInstance.graphBuilderConfig;
        this.optimisticOpts = graphBuilderInstance.optimisticOpts;
        this.metaAccess = graphBuilderInstance.metaAccess;
        this.stampProvider = graphBuilderInstance.stampProvider;
        this.constantReflection = graphBuilderInstance.constantReflection;
        this.constantFieldProvider = graphBuilderInstance.constantFieldProvider;
        this.stream = new BytecodeStream(code.getCode());
        this.profilingInfo = graph.useProfilingInfo() ? code.getProfilingInfo() : null;
        this.constantPool = code.getConstantPool();
        this.intrinsicContext = intrinsicContext;
        this.entryBCI = entryBCI;
        this.parent = parent;

        eagerInitializing = graphBuilderConfig.eagerResolving();
        uninitializedIsError = graphBuilderConfig.unresolvedIsError();

        if (graphBuilderConfig.insertFullInfopoints() && !parsingIntrinsic())
        {
            lnt = code.getLineNumberTable();
            previousLineNumber = -1;
        }
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
        FrameStateBuilder startFrameState = new FrameStateBuilder(this, code, graph);
        startFrameState.initializeForMethodStart(graph.getAssumptions(), graphBuilderConfig.eagerResolving() || intrinsicContext != null, graphBuilderConfig.getPlugins());

        try (IntrinsicScope s = intrinsicContext != null ? new IntrinsicScope(this) : null)
        {
            build(graph.start(), startFrameState);
        }

        cleanupFinalGraph();
        ComputeLoopFrequenciesClosure.compute(graph);
    }

    protected void build(FixedWithNextNode startInstruction, FrameStateBuilder startFrameState)
    {
        if (bytecodeProvider.shouldRecordMethodDependencies())
        {
            // Record method dependency in the graph
            graph.recordMethod(method);
        }

        // compute the block map, setup exception handlers and get the entrypoint(s)
        BciBlockMapping newMapping = BciBlockMapping.create(stream, code, options);
        this.blockMap = newMapping;
        this.firstInstructionArray = new FixedWithNextNode[blockMap.getBlockCount()];
        this.entryStateArray = new FrameStateBuilder[blockMap.getBlockCount()];
        if (!method.isStatic())
        {
            originalReceiver = startFrameState.loadLocal(0, JavaKind.Object);
        }

        liveness = LocalLiveness.compute(stream, blockMap.getBlocks(), method.getMaxLocals(), blockMap.getLoopCount());

        lastInstr = startInstruction;
        this.setCurrentFrameState(startFrameState);
        stream.setBCI(0);

        BciBlock startBlock = blockMap.getStartBlock();
        if (this.parent == null)
        {
            StartNode startNode = graph.start();
            if (method.isSynchronized())
            {
                startNode.setStateAfter(createFrameState(BytecodeFrame.BEFORE_BCI, startNode));
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
                        frameState.clearNonLiveLocals(startBlock, liveness, true);
                    }
                    startNode.setStateAfter(createFrameState(bci(), startNode));
                }
                else
                {
                    if (startNode.stateAfter() == null)
                    {
                        FrameState stateAfterStart = createStateAfterStartOfReplacementGraph();
                        startNode.setStateAfter(stateAfterStart);
                    }
                }
            }
        }

        if (method.isSynchronized())
        {
            finishPrepare(lastInstr, BytecodeFrame.BEFORE_BCI);

            // add a monitor enter to the start block
            methodSynchronizedObject = synchronizedObject(frameState, method);
            frameState.clearNonLiveLocals(startBlock, liveness, true);
            genMonitorEnter(methodSynchronizedObject, bci());
        }

        finishPrepare(lastInstr, 0);

        genInfoPointNode(InfopointReason.METHOD_START, null);

        currentBlock = blockMap.getStartBlock();
        setEntryState(startBlock, frameState);
        if (startBlock.isLoopHeader())
        {
            appendGoto(startBlock);
        }
        else
        {
            setFirstInstruction(startBlock, lastInstr);
        }

        BciBlock[] blocks = blockMap.getBlocks();
        for (BciBlock block : blocks)
        {
            processBlock(block);
        }
    }

    /**
     * Hook for subclasses to modify synthetic code (start nodes and unwind nodes).
     *
     * @param instruction the current last instruction
     * @param bci the current bci
     */
    protected void finishPrepare(FixedWithNextNode instruction, int bci)
    {
    }

    protected void cleanupFinalGraph()
    {
        GraphUtil.normalizeLoops(graph);

        // Remove dead parameters.
        for (ParameterNode param : graph.getNodes(ParameterNode.TYPE))
        {
            if (param.hasNoUsages())
            {
                param.safeDelete();
            }
        }

        // Remove redundant begin nodes.
        for (BeginNode beginNode : graph.getNodes(BeginNode.TYPE))
        {
            Node predecessor = beginNode.predecessor();
            if (predecessor instanceof ControlSplitNode)
            {
                // The begin node is necessary.
            }
            else if (!beginNode.hasUsages())
            {
                GraphUtil.unlinkFixedNode(beginNode);
                beginNode.safeDelete();
            }
        }
    }

    /**
     * Creates the frame state after the start node of a graph for an {@link IntrinsicContext
     * intrinsic} that is the parse root (either for root compiling or for post-parse inlining).
     */
    private FrameState createStateAfterStartOfReplacementGraph()
    {
        FrameState stateAfterStart;
        if (intrinsicContext.isPostParseInlined())
        {
            stateAfterStart = graph.add(new FrameState(BytecodeFrame.BEFORE_BCI));
        }
        else
        {
            ResolvedJavaMethod original = intrinsicContext.getOriginalMethod();
            ValueNode[] locals;
            if (original.getMaxLocals() == frameState.localsSize() || original.isNative())
            {
                locals = new ValueNode[original.getMaxLocals()];
                for (int i = 0; i < locals.length; i++)
                {
                    ValueNode node = frameState.locals[i];
                    if (node == FrameState.TWO_SLOT_MARKER)
                    {
                        node = null;
                    }
                    locals[i] = node;
                }
            }
            else
            {
                locals = new ValueNode[original.getMaxLocals()];
                int parameterCount = original.getSignature().getParameterCount(!original.isStatic());
                for (int i = 0; i < parameterCount; i++)
                {
                    ValueNode param = frameState.locals[i];
                    if (param == FrameState.TWO_SLOT_MARKER)
                    {
                        param = null;
                    }
                    locals[i] = param;
                }
            }
            ValueNode[] stack = {};
            int stackSize = 0;
            ValueNode[] locks = {};
            List<MonitorIdNode> monitorIds = Collections.emptyList();
            stateAfterStart = graph.add(new FrameState(null, new ResolvedJavaMethodBytecode(original), 0, locals, stack, stackSize, locks, monitorIds, false, false));
        }
        return stateAfterStart;
    }

    /**
     * @param type the unresolved type of the constant
     */
    protected void handleUnresolvedLoadConstant(JavaType type)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param type the unresolved type of the type check
     * @param object the object value whose type is being checked against {@code type}
     */
    protected void handleUnresolvedCheckCast(JavaType type, ValueNode object)
    {
        append(new FixedGuardNode(graph.addOrUniqueWithInputs(IsNullNode.create(object)), DeoptimizationReason.Unresolved, DeoptimizationAction.InvalidateRecompile));
        frameState.push(JavaKind.Object, appendConstant(JavaConstant.NULL_POINTER));
    }

    /**
     * @param type the unresolved type of the type check
     * @param object the object value whose type is being checked against {@code type}
     */
    protected void handleUnresolvedInstanceOf(JavaType type, ValueNode object)
    {
        AbstractBeginNode successor = graph.add(new BeginNode());
        DeoptimizeNode deopt = graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
        append(new IfNode(graph.addOrUniqueWithInputs(IsNullNode.create(object)), successor, deopt, 1));
        lastInstr = successor;
        frameState.push(JavaKind.Int, appendConstant(JavaConstant.INT_0));
    }

    /**
     * @param type the type being instantiated
     */
    protected void handleUnresolvedNewInstance(JavaType type)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param type the type of the array being instantiated
     * @param length the length of the array
     */
    protected void handleUnresolvedNewObjectArray(JavaType type, ValueNode length)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param type the type being instantiated
     * @param dims the dimensions for the multi-array
     */
    protected void handleUnresolvedNewMultiArray(JavaType type, ValueNode[] dims)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param field the unresolved field
     * @param receiver the object containing the field or {@code null} if {@code field} is static
     */
    protected void handleUnresolvedLoadField(JavaField field, ValueNode receiver)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    /**
     * @param field the unresolved field
     * @param value the value being stored to the field
     * @param receiver the object containing the field or {@code null} if {@code field} is static
     */
    protected void handleUnresolvedStoreField(JavaField field, ValueNode value, ValueNode receiver)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    protected void handleUnresolvedExceptionType(JavaType type)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    protected void handleUnresolvedInvoke(JavaMethod javaMethod, InvokeKind invokeKind)
    {
        append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.Unresolved));
    }

    private AbstractBeginNode handleException(ValueNode exceptionObject, int bci, boolean deoptimizeOnly)
    {
        FrameStateBuilder dispatchState = frameState.copy();
        dispatchState.clearStack();

        AbstractBeginNode dispatchBegin;
        if (exceptionObject == null)
        {
            ExceptionObjectNode newExceptionObject = graph.add(new ExceptionObjectNode(metaAccess));
            dispatchBegin = newExceptionObject;
            dispatchState.push(JavaKind.Object, dispatchBegin);
            dispatchState.setRethrowException(true);
            newExceptionObject.setStateAfter(dispatchState.create(bci, newExceptionObject));
        }
        else
        {
            dispatchBegin = graph.add(new BeginNode());
            dispatchState.push(JavaKind.Object, exceptionObject);
            dispatchState.setRethrowException(true);
        }
        this.controlFlowSplit = true;
        FixedWithNextNode finishedDispatch = finishInstruction(dispatchBegin, dispatchState);

        if (deoptimizeOnly)
        {
            DeoptimizeNode deoptimizeNode = graph.add(new DeoptimizeNode(DeoptimizationAction.None, DeoptimizationReason.TransferToInterpreter));
            dispatchBegin.setNext(BeginNode.begin(deoptimizeNode));
        }
        else
        {
            createHandleExceptionTarget(finishedDispatch, bci, dispatchState);
        }
        return dispatchBegin;
    }

    protected void createHandleExceptionTarget(FixedWithNextNode finishedDispatch, int bci, FrameStateBuilder dispatchState)
    {
        BciBlock dispatchBlock = currentBlock.exceptionDispatchBlock();
        /*
         * The exception dispatch block is always for the last bytecode of a block, so if we are not
         * at the endBci yet, there is no exception handler for this bci and we can unwind immediately.
         */
        if (bci != currentBlock.endBci || dispatchBlock == null)
        {
            dispatchBlock = blockMap.getUnwindBlock();
        }

        FixedNode target = createTarget(dispatchBlock, dispatchState);
        finishedDispatch.setNext(target);
    }

    protected ValueNode genLoadIndexed(ValueNode array, ValueNode index, JavaKind kind)
    {
        return LoadIndexedNode.create(graph.getAssumptions(), array, index, kind, metaAccess, constantReflection);
    }

    protected void genStoreIndexed(ValueNode array, ValueNode index, JavaKind kind, ValueNode value)
    {
        add(new StoreIndexedNode(array, index, kind, value));
    }

    protected ValueNode genIntegerAdd(ValueNode x, ValueNode y)
    {
        return AddNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerSub(ValueNode x, ValueNode y)
    {
        return SubNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerMul(ValueNode x, ValueNode y)
    {
        return MulNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatAdd(ValueNode x, ValueNode y)
    {
        return AddNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatSub(ValueNode x, ValueNode y)
    {
        return SubNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatMul(ValueNode x, ValueNode y)
    {
        return MulNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatDiv(ValueNode x, ValueNode y)
    {
        return FloatDivNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genFloatRem(ValueNode x, ValueNode y)
    {
        return RemNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerDiv(ValueNode x, ValueNode y)
    {
        return SignedDivNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genIntegerRem(ValueNode x, ValueNode y)
    {
        return SignedRemNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genNegateOp(ValueNode x)
    {
        return NegateNode.create(x, NodeView.DEFAULT);
    }

    protected ValueNode genLeftShift(ValueNode x, ValueNode y)
    {
        return LeftShiftNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genRightShift(ValueNode x, ValueNode y)
    {
        return RightShiftNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genUnsignedRightShift(ValueNode x, ValueNode y)
    {
        return UnsignedRightShiftNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genAnd(ValueNode x, ValueNode y)
    {
        return AndNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genOr(ValueNode x, ValueNode y)
    {
        return OrNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genXor(ValueNode x, ValueNode y)
    {
        return XorNode.create(x, y, NodeView.DEFAULT);
    }

    protected ValueNode genNormalizeCompare(ValueNode x, ValueNode y, boolean isUnorderedLess)
    {
        return NormalizeCompareNode.create(x, y, isUnorderedLess, JavaKind.Int, constantReflection);
    }

    protected ValueNode genFloatConvert(FloatConvert op, ValueNode input)
    {
        return FloatConvertNode.create(op, input, NodeView.DEFAULT);
    }

    protected ValueNode genNarrow(ValueNode input, int bitCount)
    {
        return NarrowNode.create(input, bitCount, NodeView.DEFAULT);
    }

    protected ValueNode genSignExtend(ValueNode input, int bitCount)
    {
        return SignExtendNode.create(input, bitCount, NodeView.DEFAULT);
    }

    protected ValueNode genZeroExtend(ValueNode input, int bitCount)
    {
        return ZeroExtendNode.create(input, bitCount, NodeView.DEFAULT);
    }

    protected void genGoto()
    {
        appendGoto(currentBlock.getSuccessor(0));
    }

    protected LogicNode genObjectEquals(ValueNode x, ValueNode y)
    {
        return ObjectEqualsNode.create(constantReflection, metaAccess, options, x, y, NodeView.DEFAULT);
    }

    protected LogicNode genIntegerEquals(ValueNode x, ValueNode y)
    {
        return IntegerEqualsNode.create(constantReflection, metaAccess, options, null, x, y, NodeView.DEFAULT);
    }

    protected LogicNode genIntegerLessThan(ValueNode x, ValueNode y)
    {
        return IntegerLessThanNode.create(constantReflection, metaAccess, options, null, x, y, NodeView.DEFAULT);
    }

    protected ValueNode genUnique(ValueNode x)
    {
        return graph.addOrUniqueWithInputs(x);
    }

    protected LogicNode genUnique(LogicNode x)
    {
        return graph.addOrUniqueWithInputs(x);
    }

    protected ValueNode genIfNode(LogicNode condition, FixedNode falseSuccessor, FixedNode trueSuccessor, double d)
    {
        return new IfNode(condition, falseSuccessor, trueSuccessor, d);
    }

    protected void genThrow()
    {
        genInfoPointNode(InfopointReason.BYTECODE_POSITION, null);

        ValueNode exception = frameState.pop(JavaKind.Object);
        FixedGuardNode nullCheck = append(new FixedGuardNode(graph.addOrUniqueWithInputs(IsNullNode.create(exception)), DeoptimizationReason.NullCheckException, DeoptimizationAction.InvalidateReprofile, true));
        ValueNode nonNullException = graph.maybeAddOrUnique(PiNode.create(exception, exception.stamp(NodeView.DEFAULT).join(StampFactory.objectNonNull()), nullCheck));
        lastInstr.setNext(handleException(nonNullException, bci(), false));
    }

    protected LogicNode createInstanceOf(TypeReference type, ValueNode object)
    {
        return InstanceOfNode.create(type, object);
    }

    protected AnchoringNode createAnchor(JavaTypeProfile profile)
    {
        if (profile == null || profile.getNotRecordedProbability() > 0.0)
        {
            return null;
        }
        else
        {
            return append(new ValueAnchorNode(null));
        }
    }

    protected LogicNode createInstanceOf(TypeReference type, ValueNode object, JavaTypeProfile profile)
    {
        return InstanceOfNode.create(type, object, profile, createAnchor(profile));
    }

    protected LogicNode createInstanceOfAllowNull(TypeReference type, ValueNode object, JavaTypeProfile profile)
    {
        return InstanceOfNode.createAllowNull(type, object, profile, createAnchor(profile));
    }

    protected ValueNode genConditional(ValueNode x)
    {
        return ConditionalNode.create((LogicNode) x, NodeView.DEFAULT);
    }

    protected NewInstanceNode createNewInstance(ResolvedJavaType type, boolean fillContents)
    {
        return new NewInstanceNode(type, fillContents);
    }

    protected NewArrayNode createNewArray(ResolvedJavaType elementType, ValueNode length, boolean fillContents)
    {
        return new NewArrayNode(elementType, length, fillContents);
    }

    protected NewMultiArrayNode createNewMultiArray(ResolvedJavaType type, ValueNode[] dimensions)
    {
        return new NewMultiArrayNode(type, dimensions);
    }

    protected ValueNode genLoadField(ValueNode receiver, ResolvedJavaField field)
    {
        StampPair stamp = graphBuilderConfig.getPlugins().getOverridingStamp(this, field.getType(), false);
        if (stamp == null)
        {
            return LoadFieldNode.create(getConstantFieldProvider(), getConstantReflection(), getMetaAccess(), getOptions(), getAssumptions(), receiver, field, false, false);
        }
        else
        {
            return LoadFieldNode.createOverrideStamp(getConstantFieldProvider(), getConstantReflection(), getMetaAccess(), getOptions(), stamp, receiver, field, false, false);
        }
    }

    protected StateSplitProxyNode genVolatileFieldReadProxy(ValueNode fieldRead)
    {
        return new StateSplitProxyNode(fieldRead);
    }

    protected ValueNode emitExplicitNullCheck(ValueNode receiver)
    {
        if (StampTool.isPointerNonNull(receiver.stamp(NodeView.DEFAULT)))
        {
            return receiver;
        }
        BytecodeExceptionNode exception = graph.add(new BytecodeExceptionNode(metaAccess, NullPointerException.class));
        AbstractBeginNode falseSucc = graph.add(new BeginNode());
        ValueNode nonNullReceiver = graph.addOrUniqueWithInputs(PiNode.create(receiver, StampFactory.objectNonNull(), falseSucc));
        append(new IfNode(graph.addOrUniqueWithInputs(IsNullNode.create(receiver)), exception, falseSucc, BranchProbabilityNode.SLOW_PATH_PROBABILITY));
        lastInstr = falseSucc;

        exception.setStateAfter(createFrameState(bci(), exception));
        exception.setNext(handleException(exception, bci(), false));
        return nonNullReceiver;
    }

    protected void emitExplicitBoundsCheck(ValueNode index, ValueNode length)
    {
        AbstractBeginNode trueSucc = graph.add(new BeginNode());
        BytecodeExceptionNode exception = graph.add(new BytecodeExceptionNode(metaAccess, ArrayIndexOutOfBoundsException.class, index));
        append(new IfNode(genUnique(IntegerBelowNode.create(constantReflection, metaAccess, options, null, index, length, NodeView.DEFAULT)), trueSucc, exception, BranchProbabilityNode.FAST_PATH_PROBABILITY));
        lastInstr = trueSucc;

        exception.setStateAfter(createFrameState(bci(), exception));
        exception.setNext(handleException(exception, bci(), false));
    }

    protected ValueNode genArrayLength(ValueNode x)
    {
        return ArrayLengthNode.create(x, constantReflection);
    }

    protected void genStoreField(ValueNode receiver, ResolvedJavaField field, ValueNode value)
    {
        StoreFieldNode storeFieldNode = new StoreFieldNode(receiver, field, value);
        append(storeFieldNode);
        storeFieldNode.setStateAfter(this.createFrameState(stream.nextBCI(), storeFieldNode));
    }

    /**
     * Ensure that concrete classes are at least linked before generating an invoke. Interfaces may
     * never be linked so simply return true for them.
     *
     * @return true if the declared holder is an interface or is linked
     */
    private static boolean callTargetIsResolved(JavaMethod target)
    {
        if (target instanceof ResolvedJavaMethod)
        {
            ResolvedJavaMethod resolvedTarget = (ResolvedJavaMethod) target;
            ResolvedJavaType resolvedType = resolvedTarget.getDeclaringClass();
            return resolvedType.isInterface() || resolvedType.isLinked();
        }
        return false;
    }

    protected void genInvokeStatic(int cpi, int opcode)
    {
        JavaMethod target = lookupMethod(cpi, opcode);
        genInvokeStatic(target);
    }

    void genInvokeStatic(JavaMethod target)
    {
        if (callTargetIsResolved(target))
        {
            ResolvedJavaMethod resolvedTarget = (ResolvedJavaMethod) target;
            ResolvedJavaType holder = resolvedTarget.getDeclaringClass();
            if (!holder.isInitialized() && GraalOptions.ResolveClassBeforeStaticInvoke.getValue(options))
            {
                handleUnresolvedInvoke(target, InvokeKind.Static);
            }
            else
            {
                ValueNode[] args = frameState.popArguments(resolvedTarget.getSignature().getParameterCount(false));
                Invoke invoke = appendInvoke(InvokeKind.Static, resolvedTarget, args);
                if (invoke != null)
                {
                    invoke.setClassInit(null);
                }
            }
        }
        else
        {
            handleUnresolvedInvoke(target, InvokeKind.Static);
        }
    }

    protected void genInvokeInterface(int cpi, int opcode)
    {
        JavaMethod target = lookupMethod(cpi, opcode);
        genInvokeInterface(target);
    }

    protected void genInvokeInterface(JavaMethod target)
    {
        if (callTargetIsResolved(target))
        {
            ValueNode[] args = frameState.popArguments(target.getSignature().getParameterCount(true));
            appendInvoke(InvokeKind.Interface, (ResolvedJavaMethod) target, args);
        }
        else
        {
            handleUnresolvedInvoke(target, InvokeKind.Interface);
        }
    }

    protected void genInvokeDynamic(int cpi, int opcode)
    {
        JavaMethod target = lookupMethod(cpi, opcode);
        genInvokeDynamic(target);
    }

    void genInvokeDynamic(JavaMethod target)
    {
        if (!(target instanceof ResolvedJavaMethod) || !genDynamicInvokeHelper((ResolvedJavaMethod) target, stream.readCPI4(), Bytecodes.INVOKEDYNAMIC))
        {
            handleUnresolvedInvoke(target, InvokeKind.Static);
        }
    }

    protected void genInvokeVirtual(int cpi, int opcode)
    {
        JavaMethod target = lookupMethod(cpi, opcode);
        genInvokeVirtual(target);
    }

    private boolean genDynamicInvokeHelper(ResolvedJavaMethod target, int cpi, int opcode)
    {
        InvokeDynamicPlugin invokeDynamicPlugin = graphBuilderConfig.getPlugins().getInvokeDynamicPlugin();

        if (opcode == Bytecodes.INVOKEVIRTUAL && invokeDynamicPlugin != null && !invokeDynamicPlugin.isResolvedDynamicInvoke(this, cpi, opcode))
        {
            // regular invokevirtual, let caller handle it
            return false;
        }

        JavaConstant appendix = constantPool.lookupAppendix(cpi, opcode);
        if (appendix != null)
        {
            ValueNode appendixNode;

            if (invokeDynamicPlugin != null)
            {
                invokeDynamicPlugin.recordDynamicMethod(this, cpi, opcode, target);

                // Will perform runtime type checks and static initialization
                FrameState stateBefore = frameState.create(bci(), getNonIntrinsicAncestor(), false, null, null);
                appendixNode = invokeDynamicPlugin.genAppendixNode(this, cpi, opcode, appendix, stateBefore);
            }
            else
            {
                appendixNode = ConstantNode.forConstant(appendix, metaAccess, graph);
            }

            frameState.push(JavaKind.Object, appendixNode);
        }

        boolean hasReceiver = (opcode == Bytecodes.INVOKEDYNAMIC) ? false : !target.isStatic();
        ValueNode[] args = frameState.popArguments(target.getSignature().getParameterCount(hasReceiver));
        if (hasReceiver)
        {
            appendInvoke(InvokeKind.Virtual, target, args);
        }
        else
        {
            appendInvoke(InvokeKind.Static, target, args);
        }

        return true;
    }

    void genInvokeVirtual(JavaMethod target)
    {
        if (!genInvokeVirtualHelper(target))
        {
            handleUnresolvedInvoke(target, InvokeKind.Virtual);
        }
    }

    private boolean genInvokeVirtualHelper(JavaMethod target)
    {
        if (!callTargetIsResolved(target))
        {
            return false;
        }

        ResolvedJavaMethod resolvedTarget = (ResolvedJavaMethod) target;
        int cpi = stream.readCPI();

        /*
         * Special handling for runtimes that rewrite an invocation of MethodHandle.invoke(...) or
         * MethodHandle.invokeExact(...) to a static adapter. HotSpot does this - see
         * https://wiki.openjdk.java.net/display/HotSpot/Method+handles+and+invokedynamic
         */

        if (genDynamicInvokeHelper(resolvedTarget, cpi, Bytecodes.INVOKEVIRTUAL))
        {
            return true;
        }

        ValueNode[] args = frameState.popArguments(target.getSignature().getParameterCount(true));
        appendInvoke(InvokeKind.Virtual, (ResolvedJavaMethod) target, args);

        return true;
    }

    protected void genInvokeSpecial(int cpi, int opcode)
    {
        JavaMethod target = lookupMethod(cpi, opcode);
        genInvokeSpecial(target);
    }

    void genInvokeSpecial(JavaMethod target)
    {
        if (callTargetIsResolved(target))
        {
            ValueNode[] args = frameState.popArguments(target.getSignature().getParameterCount(true));
            appendInvoke(InvokeKind.Special, (ResolvedJavaMethod) target, args);
        }
        else
        {
            handleUnresolvedInvoke(target, InvokeKind.Special);
        }
    }

    static class CurrentInvoke
    {
        final ValueNode[] args;
        final InvokeKind kind;
        final JavaType returnType;

        CurrentInvoke(ValueNode[] args, InvokeKind kind, JavaType returnType)
        {
            this.args = args;
            this.kind = kind;
            this.returnType = returnType;
        }
    }

    private CurrentInvoke currentInvoke;
    protected FrameStateBuilder frameState;
    protected BciBlock currentBlock;
    protected final BytecodeStream stream;
    protected final GraphBuilderConfiguration graphBuilderConfig;
    protected final ResolvedJavaMethod method;
    protected final Bytecode code;
    protected final BytecodeProvider bytecodeProvider;
    protected final ProfilingInfo profilingInfo;
    protected final OptimisticOptimizations optimisticOpts;
    protected final ConstantPool constantPool;
    protected final MetaAccessProvider metaAccess;
    private final ConstantReflectionProvider constantReflection;
    private final ConstantFieldProvider constantFieldProvider;
    private final StampProvider stampProvider;
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

    private boolean forceInliningEverything;

    @Override
    public void handleReplacedInvoke(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] args, boolean inlineEverything)
    {
        boolean previous = forceInliningEverything;
        forceInliningEverything = previous || inlineEverything;
        try
        {
            appendInvoke(invokeKind, targetMethod, args);
        }
        finally
        {
            forceInliningEverything = previous;
        }
    }

    @Override
    public void handleReplacedInvoke(CallTargetNode callTarget, JavaKind resultType)
    {
        BytecodeParser intrinsicCallSiteParser = getNonIntrinsicAncestor();
        ExceptionEdgeAction exceptionEdgeAction = intrinsicCallSiteParser == null ? getActionForInvokeExceptionEdge(null) : intrinsicCallSiteParser.getActionForInvokeExceptionEdge(null);
        createNonInlinedInvoke(exceptionEdgeAction, bci(), callTarget, resultType);
    }

    protected Invoke appendInvoke(InvokeKind initialInvokeKind, ResolvedJavaMethod initialTargetMethod, ValueNode[] args)
    {
        ResolvedJavaMethod targetMethod = initialTargetMethod;
        InvokeKind invokeKind = initialInvokeKind;
        if (initialInvokeKind.isIndirect())
        {
            ResolvedJavaType contextType = this.frameState.getMethod().getDeclaringClass();
            ResolvedJavaMethod specialCallTarget = MethodCallTargetNode.findSpecialCallTarget(initialInvokeKind, args[0], initialTargetMethod, contextType);
            if (specialCallTarget != null)
            {
                invokeKind = InvokeKind.Special;
                targetMethod = specialCallTarget;
            }
        }

        JavaKind resultType = targetMethod.getSignature().getReturnKind();
        if (!parsingIntrinsic() && GraalOptions.DeoptALot.getValue(options))
        {
            append(new DeoptimizeNode(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint));
            frameState.pushReturn(resultType, ConstantNode.defaultForKind(resultType, graph));
            return null;
        }

        JavaType returnType = targetMethod.getSignature().getReturnType(method.getDeclaringClass());
        if (graphBuilderConfig.eagerResolving() || parsingIntrinsic())
        {
            returnType = returnType.resolve(targetMethod.getDeclaringClass());
        }
        if (invokeKind.hasReceiver())
        {
            args[0] = emitExplicitExceptions(args[0]);
        }

        if (initialInvokeKind == InvokeKind.Special && !targetMethod.isConstructor())
        {
            emitCheckForInvokeSuperSpecial(args);
        }

        InlineInfo inlineInfo = null;
        try
        {
            currentInvoke = new CurrentInvoke(args, invokeKind, returnType);
            if (tryNodePluginForInvocation(args, targetMethod))
            {
                return null;
            }

            if (invokeKind.hasReceiver() && args[0].isNullConstant())
            {
                append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NullCheckException));
                return null;
            }

            if (!invokeKind.isIndirect() || BytecodeParserOptions.UseGuardedIntrinsics.getValue(options))
            {
                if (tryInvocationPlugin(invokeKind, args, targetMethod, resultType, returnType))
                {
                    return null;
                }
            }
            if (invokeKind.isDirect())
            {
                inlineInfo = tryInline(args, targetMethod);
                if (inlineInfo == SUCCESSFULLY_INLINED)
                {
                    return null;
                }
            }
        }
        finally
        {
            currentInvoke = null;
        }

        int invokeBci = bci();
        JavaTypeProfile profile = getProfileForInvoke(invokeKind);
        ExceptionEdgeAction edgeAction = getActionForInvokeExceptionEdge(inlineInfo);
        boolean partialIntrinsicExit = false;
        if (intrinsicContext != null && intrinsicContext.isCallToOriginal(targetMethod))
        {
            partialIntrinsicExit = true;
            ResolvedJavaMethod originalMethod = intrinsicContext.getOriginalMethod();
            BytecodeParser intrinsicCallSiteParser = getNonIntrinsicAncestor();
            if (intrinsicCallSiteParser != null)
            {
                // When exiting a partial intrinsic, the invoke to the original
                // must use the same context as the call to the intrinsic.
                invokeBci = intrinsicCallSiteParser.bci();
                profile = intrinsicCallSiteParser.getProfileForInvoke(invokeKind);
                edgeAction = intrinsicCallSiteParser.getActionForInvokeExceptionEdge(inlineInfo);
            }
            else
            {
                // We are parsing the intrinsic for the root compilation or for inlining,
                // This call is a partial intrinsic exit, and we do not have profile information
                // for this callsite. We also have to assume that the call needs an exception
                // edge. Finally, we know that this intrinsic is parsed for late inlining,
                // so the bci must be set to unknown, so that the inliner patches it later.
                invokeBci = BytecodeFrame.UNKNOWN_BCI;
                profile = null;
                edgeAction = graph.method().getAnnotation(Snippet.class) == null ? ExceptionEdgeAction.INCLUDE_AND_HANDLE : ExceptionEdgeAction.OMIT;
            }

            if (originalMethod.isStatic())
            {
                invokeKind = InvokeKind.Static;
            }
            else
            {
                // The original call to the intrinsic must have been devirtualized
                // otherwise we wouldn't be here.
                invokeKind = InvokeKind.Special;
            }
            Signature sig = originalMethod.getSignature();
            returnType = sig.getReturnType(method.getDeclaringClass());
            resultType = sig.getReturnKind();
            targetMethod = originalMethod;
        }
        Invoke invoke = createNonInlinedInvoke(edgeAction, invokeBci, args, targetMethod, invokeKind, resultType, returnType, profile);
        if (partialIntrinsicExit)
        {
            // This invoke must never be later inlined as it might select the intrinsic graph.
            // Until there is a mechanism to guarantee that any late inlining will not select
            // the intrinsic graph, prevent this invoke from being inlined.
            invoke.setUseForInlining(false);
        }
        return invoke;
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
    protected void emitCheckForInvokeSuperSpecial(ValueNode[] args)
    {
        ResolvedJavaType callingClass = method.getDeclaringClass();
        if (callingClass.getHostClass() != null)
        {
            callingClass = callingClass.getHostClass();
        }
        if (callingClass.isInterface())
        {
            ValueNode receiver = args[0];
            TypeReference checkedType = TypeReference.createTrusted(graph.getAssumptions(), callingClass);
            LogicNode condition = genUnique(createInstanceOf(checkedType, receiver, null));
            FixedGuardNode fixedGuard = append(new FixedGuardNode(condition, DeoptimizationReason.ClassCastException, DeoptimizationAction.None, false));
            args[0] = append(PiNode.create(receiver, StampFactory.object(checkedType, true), fixedGuard));
        }
    }

    protected JavaTypeProfile getProfileForInvoke(InvokeKind invokeKind)
    {
        if (invokeKind.isIndirect() && profilingInfo != null && this.optimisticOpts.useTypeCheckHints(getOptions()))
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
    private static boolean checkPartialIntrinsicExit(ValueNode[] originalArgs, ValueNode[] recursiveArgs)
    {
        if (originalArgs != null)
        {
            for (int i = 0; i < originalArgs.length; i++)
            {
                ValueNode arg = GraphUtil.unproxify(recursiveArgs[i]);
                ValueNode icArg = GraphUtil.unproxify(originalArgs[i]);
            }
        }
        else
        {
            for (int i = 0; i < recursiveArgs.length; i++)
            {
                ValueNode arg = GraphUtil.unproxify(recursiveArgs[i]);
            }
        }
        return true;
    }

    protected Invoke createNonInlinedInvoke(ExceptionEdgeAction exceptionEdge, int invokeBci, ValueNode[] invokeArgs, ResolvedJavaMethod targetMethod, InvokeKind invokeKind, JavaKind resultType, JavaType returnType, JavaTypeProfile profile)
    {
        StampPair returnStamp = graphBuilderConfig.getPlugins().getOverridingStamp(this, returnType, false);
        if (returnStamp == null)
        {
            returnStamp = StampFactory.forDeclaredType(graph.getAssumptions(), returnType, false);
        }

        MethodCallTargetNode callTarget = graph.add(createMethodCallTarget(invokeKind, targetMethod, invokeArgs, returnStamp, profile));
        Invoke invoke = createNonInlinedInvoke(exceptionEdge, invokeBci, callTarget, resultType);

        for (InlineInvokePlugin plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            plugin.notifyNotInlined(this, targetMethod, invoke);
        }

        return invoke;
    }

    protected Invoke createNonInlinedInvoke(ExceptionEdgeAction exceptionEdge, int invokeBci, CallTargetNode callTarget, JavaKind resultType)
    {
        if (exceptionEdge == ExceptionEdgeAction.OMIT)
        {
            return createInvoke(invokeBci, callTarget, resultType);
        }
        else
        {
            Invoke invoke = createInvokeWithException(invokeBci, callTarget, resultType, exceptionEdge);
            AbstractBeginNode beginNode = graph.add(KillingBeginNode.create(LocationIdentity.any()));
            invoke.setNext(beginNode);
            lastInstr = beginNode;
            return invoke;
        }
    }

    /**
     * Describes what should be done with the exception edge of an invocation. The edge can be
     * omitted or included. An included edge can handle the exception or transfer execution to the
     * interpreter for handling (deoptimize).
     */
    protected enum ExceptionEdgeAction
    {
        OMIT,
        INCLUDE_AND_HANDLE,
        INCLUDE_AND_DEOPTIMIZE
    }

    protected ExceptionEdgeAction getActionForInvokeExceptionEdge(InlineInfo lastInlineInfo)
    {
        if (lastInlineInfo == InlineInfo.DO_NOT_INLINE_WITH_EXCEPTION)
        {
            return ExceptionEdgeAction.INCLUDE_AND_HANDLE;
        }
        else if (lastInlineInfo == InlineInfo.DO_NOT_INLINE_NO_EXCEPTION)
        {
            return ExceptionEdgeAction.OMIT;
        }
        else if (lastInlineInfo == InlineInfo.DO_NOT_INLINE_DEOPTIMIZE_ON_EXCEPTION)
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
            if (!GraalOptions.StressInvokeWithExceptionNode.getValue(options))
            {
                if (optimisticOpts.useExceptionProbability(getOptions()))
                {
                    if (profilingInfo != null)
                    {
                        TriState exceptionSeen = profilingInfo.getExceptionSeen(bci());
                        if (exceptionSeen == TriState.FALSE)
                        {
                            return ExceptionEdgeAction.OMIT;
                        }
                    }
                }
            }
            return ExceptionEdgeAction.INCLUDE_AND_HANDLE;
        }
    }

    protected static class IntrinsicGuard
    {
        final FixedWithNextNode lastInstr;
        final Mark mark;
        final AbstractBeginNode nonIntrinsicBranch;
        final ValueNode receiver;
        final JavaTypeProfile profile;

        public IntrinsicGuard(FixedWithNextNode lastInstr, ValueNode receiver, Mark mark, AbstractBeginNode nonIntrinsicBranch, JavaTypeProfile profile)
        {
            this.lastInstr = lastInstr;
            this.receiver = receiver;
            this.mark = mark;
            this.nonIntrinsicBranch = nonIntrinsicBranch;
            this.profile = profile;
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
    protected IntrinsicGuard guardIntrinsic(ValueNode[] args, ResolvedJavaMethod targetMethod, InvocationPluginReceiver pluginReceiver)
    {
        ValueNode intrinsicReceiver = args[0];
        ResolvedJavaType receiverType = StampTool.typeOrNull(intrinsicReceiver);
        if (receiverType == null)
        {
            // The verifier guarantees it to be at least type declaring targetMethod
            receiverType = targetMethod.getDeclaringClass();
        }
        ResolvedJavaMethod resolvedMethod = receiverType.resolveMethod(targetMethod, method.getDeclaringClass());
        if (resolvedMethod == null || resolvedMethod.equals(targetMethod))
        {
            Mark mark = graph.getMark();
            FixedWithNextNode currentLastInstr = lastInstr;
            ValueNode nonNullReceiver = pluginReceiver.get();
            Stamp methodStamp = stampProvider.createMethodStamp();
            LoadHubNode hub = graph.unique(new LoadHubNode(stampProvider, nonNullReceiver));
            LoadMethodNode actual = append(new LoadMethodNode(methodStamp, targetMethod, receiverType, method.getDeclaringClass(), hub));
            ConstantNode expected = graph.unique(ConstantNode.forConstant(methodStamp, targetMethod.getEncoding(), getMetaAccess()));
            LogicNode compare = graph.addOrUniqueWithInputs(CompareNode.createCompareNode(constantReflection, metaAccess, options, null, CanonicalCondition.EQ, actual, expected, NodeView.DEFAULT));

            JavaTypeProfile profile = null;
            if (profilingInfo != null && this.optimisticOpts.useTypeCheckHints(getOptions()))
            {
                profile = profilingInfo.getTypeProfile(bci());
                if (profile != null)
                {
                    JavaTypeProfile newProfile = adjustProfileForInvocationPlugin(profile, targetMethod);
                    if (newProfile != profile)
                    {
                        if (newProfile.getTypes().length == 0)
                        {
                            // All profiled types select the intrinsic so
                            // emit a fixed guard instead of an if-then-else.
                            lastInstr = append(new FixedGuardNode(compare, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile, false));
                            return new IntrinsicGuard(currentLastInstr, intrinsicReceiver, mark, null, null);
                        }
                    }
                    else
                    {
                        // No profiled types select the intrinsic so emit a virtual call
                        return null;
                    }
                    profile = newProfile;
                }
            }

            AbstractBeginNode intrinsicBranch = graph.add(new BeginNode());
            AbstractBeginNode nonIntrinsicBranch = graph.add(new BeginNode());
            append(new IfNode(compare, intrinsicBranch, nonIntrinsicBranch, BranchProbabilityNode.FAST_PATH_PROBABILITY));
            lastInstr = intrinsicBranch;
            return new IntrinsicGuard(currentLastInstr, intrinsicReceiver, mark, nonIntrinsicBranch, profile);
        }
        else
        {
            // Receiver selects an overriding method so emit a virtual call
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
    protected JavaTypeProfile adjustProfileForInvocationPlugin(JavaTypeProfile profile, ResolvedJavaMethod targetMethod)
    {
        if (profile.getTypes().length > 0)
        {
            List<ProfiledType> retained = new ArrayList<>();
            double notRecordedProbability = profile.getNotRecordedProbability();
            for (ProfiledType ptype : profile.getTypes())
            {
                if (!ptype.getType().resolveMethod(targetMethod, method.getDeclaringClass()).equals(targetMethod))
                {
                    retained.add(ptype);
                }
                else
                {
                    notRecordedProbability += ptype.getProbability();
                }
            }
            if (!retained.isEmpty())
            {
                if (retained.size() != profile.getTypes().length)
                {
                    return new JavaTypeProfile(profile.getNullSeen(), notRecordedProbability, retained.toArray(new ProfiledType[retained.size()]));
                }
            }
            else
            {
                return new JavaTypeProfile(profile.getNullSeen(), notRecordedProbability, new ProfiledType[0]);
            }
        }
        return profile;
    }

    /**
     * Performs any action required after execution of an invocation plugin.
     * This includes checking invocation plugin invariants as well as weaving the {@code else}
     * branch of the code woven by {@link #guardIntrinsic} if {@code guard != null}.
     */
    protected void afterInvocationPluginExecution(boolean pluginHandledInvoke, IntrinsicGuard intrinsicGuard, InvokeKind invokeKind, ValueNode[] args, ResolvedJavaMethod targetMethod, JavaKind resultType, JavaType returnType)
    {
        if (intrinsicGuard != null)
        {
            if (pluginHandledInvoke)
            {
                if (intrinsicGuard.nonIntrinsicBranch != null)
                {
                    // Intrinsic emitted: emit a virtual call to the target method and
                    // merge it with the intrinsic branch
                    EndNode intrinsicEnd = append(new EndNode());

                    FrameStateBuilder intrinsicState = null;
                    FrameStateBuilder nonIntrinisicState = null;
                    if (resultType != JavaKind.Void)
                    {
                        intrinsicState = frameState.copy();
                        frameState.pop(resultType);
                        nonIntrinisicState = frameState;
                    }

                    lastInstr = intrinsicGuard.nonIntrinsicBranch;
                    createNonInlinedInvoke(getActionForInvokeExceptionEdge(null), bci(), args, targetMethod, invokeKind, resultType, returnType, intrinsicGuard.profile);

                    EndNode nonIntrinsicEnd = append(new EndNode());
                    AbstractMergeNode mergeNode = graph.add(new MergeNode());

                    mergeNode.addForwardEnd(intrinsicEnd);
                    if (intrinsicState != null)
                    {
                        intrinsicState.merge(mergeNode, nonIntrinisicState);
                        frameState = intrinsicState;
                    }
                    mergeNode.addForwardEnd(nonIntrinsicEnd);
                    mergeNode.setStateAfter(frameState.create(stream.nextBCI(), mergeNode));

                    lastInstr = mergeNode;
                }
            }
            else
            {
                // Intrinsic was not applied: remove intrinsic guard
                // and restore the original receiver node in the arguments array
                intrinsicGuard.lastInstr.setNext(null);
                GraphUtil.removeNewNodes(graph, intrinsicGuard.mark);
                lastInstr = intrinsicGuard.lastInstr;
                args[0] = intrinsicGuard.receiver;
            }
        }
    }

    protected boolean tryInvocationPlugin(InvokeKind invokeKind, ValueNode[] args, ResolvedJavaMethod targetMethod, JavaKind resultType, JavaType returnType)
    {
        InvocationPlugin plugin = graphBuilderConfig.getPlugins().getInvocationPlugins().lookupInvocation(targetMethod);
        if (plugin != null)
        {
            if (intrinsicContext != null && intrinsicContext.isCallToOriginal(targetMethod))
            {
                // Self recursive intrinsic means the original method should be called.
                return false;
            }

            InvocationPluginReceiver pluginReceiver = invocationPluginReceiver.init(targetMethod, args);

            IntrinsicGuard intrinsicGuard = null;
            if (invokeKind.isIndirect())
            {
                intrinsicGuard = guardIntrinsic(args, targetMethod, pluginReceiver);
                if (intrinsicGuard == null)
                {
                    return false;
                }
            }

            if (plugin.execute(this, targetMethod, pluginReceiver, args))
            {
                afterInvocationPluginExecution(true, intrinsicGuard, invokeKind, args, targetMethod, resultType, returnType);
                return !plugin.isDecorator();
            }
            else
            {
                afterInvocationPluginExecution(false, intrinsicGuard, invokeKind, args, targetMethod, resultType, returnType);
            }
        }
        return false;
    }

    private boolean tryNodePluginForInvocation(ValueNode[] args, ResolvedJavaMethod targetMethod)
    {
        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleInvoke(this, targetMethod, args))
            {
                return true;
            }
        }
        return false;
    }

    private static final InlineInfo SUCCESSFULLY_INLINED = InlineInfo.createStandardInlineInfo(null);

    /**
     * Try to inline a method. If the method was inlined, returns {@link #SUCCESSFULLY_INLINED}.
     * Otherwise, it returns the {@link InlineInfo} that lead to the decision to not inline it, or
     * {@code null} if there is no {@link InlineInfo} for this method.
     */
    private InlineInfo tryInline(ValueNode[] args, ResolvedJavaMethod targetMethod)
    {
        boolean canBeInlined = forceInliningEverything || parsingIntrinsic() || targetMethod.canBeInlined();
        if (!canBeInlined)
        {
            return null;
        }

        if (forceInliningEverything)
        {
            if (inline(targetMethod, targetMethod, null, args))
            {
                return SUCCESSFULLY_INLINED;
            }
            else
            {
                return null;
            }
        }

        for (InlineInvokePlugin plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            InlineInfo inlineInfo = plugin.shouldInlineInvoke(this, targetMethod, args);
            if (inlineInfo != null)
            {
                if (inlineInfo.getMethodToInline() != null)
                {
                    if (inline(targetMethod, inlineInfo.getMethodToInline(), inlineInfo.getIntrinsicBytecodeProvider(), args))
                    {
                        return SUCCESSFULLY_INLINED;
                    }
                    inlineInfo = null;
                }
                // Do not inline, and do not ask the remaining plugins.
                return inlineInfo;
            }
        }

        // There was no inline plugin with a definite answer to whether or not
        // to inline. If we're parsing an intrinsic, then we need to enforce the
        // invariant here that methods are always force inlined in intrinsics/snippets.
        if (parsingIntrinsic())
        {
            if (inline(targetMethod, targetMethod, this.bytecodeProvider, args))
            {
                return SUCCESSFULLY_INLINED;
            }
        }
        return null;
    }

    private static final int ACCESSOR_BYTECODE_LENGTH = 5;

    /**
     * Tries to inline {@code targetMethod} if it is an instance field accessor. This avoids the
     * overhead of creating and using a nested {@link BytecodeParser} object.
     */
    private boolean tryFastInlineAccessor(ValueNode[] args, ResolvedJavaMethod targetMethod)
    {
        byte[] bytecode = targetMethod.getCode();
        if (bytecode != null && bytecode.length == ACCESSOR_BYTECODE_LENGTH && Bytes.beU1(bytecode, 0) == Bytecodes.ALOAD_0 && Bytes.beU1(bytecode, 1) == Bytecodes.GETFIELD)
        {
            int b4 = Bytes.beU1(bytecode, 4);
            if (b4 >= Bytecodes.IRETURN && b4 <= Bytecodes.ARETURN)
            {
                int cpi = Bytes.beU2(bytecode, 2);
                JavaField field = targetMethod.getConstantPool().lookupField(cpi, targetMethod, Bytecodes.GETFIELD);
                if (field instanceof ResolvedJavaField)
                {
                    ValueNode receiver = invocationPluginReceiver.init(targetMethod, args).get();
                    ResolvedJavaField resolvedField = (ResolvedJavaField) field;
                    genGetField(resolvedField, receiver);
                    notifyBeforeInline(targetMethod);
                    notifyAfterInline(targetMethod);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean intrinsify(BytecodeProvider intrinsicBytecodeProvider, ResolvedJavaMethod targetMethod, ResolvedJavaMethod substitute, InvocationPlugin.Receiver receiver, ValueNode[] args)
    {
        if (receiver != null)
        {
            receiver.get();
        }
        boolean res = inline(targetMethod, substitute, intrinsicBytecodeProvider, args);
        return res;
    }

    private boolean inline(ResolvedJavaMethod targetMethod, ResolvedJavaMethod inlinedMethod, BytecodeProvider intrinsicBytecodeProvider, ValueNode[] args)
    {
        IntrinsicContext intrinsic = this.intrinsicContext;

        if (intrinsic == null && !graphBuilderConfig.insertFullInfopoints() && targetMethod.equals(inlinedMethod) && (targetMethod.getModifiers() & (Modifier.STATIC | Modifier.SYNCHRONIZED)) == 0 && tryFastInlineAccessor(args, targetMethod))
        {
            return true;
        }

        if (intrinsic != null && intrinsic.isCallToOriginal(targetMethod))
        {
            if (intrinsic.isCompilationRoot())
            {
                // A root compiled intrinsic needs to deoptimize
                // if the slow path is taken. During frame state
                // assignment, the deopt node will get its stateBefore
                // from the start node of the intrinsic
                append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.RuntimeConstraint));
                return true;
            }
            else
            {
                if (intrinsic.getOriginalMethod().isNative())
                {
                    return false;
                }
                if (canInlinePartialIntrinsicExit() && BytecodeParserOptions.InlinePartialIntrinsicExitDuringParsing.getValue(options))
                {
                    // Otherwise inline the original method. Any frame state created
                    // during the inlining will exclude frame(s) in the
                    // intrinsic method (see FrameStateBuilder.create(int bci)).
                    notifyBeforeInline(inlinedMethod);
                    parseAndInlineCallee(intrinsic.getOriginalMethod(), args, null);
                    notifyAfterInline(inlinedMethod);
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
            boolean isIntrinsic = intrinsicBytecodeProvider != null;
            if (intrinsic == null && isIntrinsic)
            {
                intrinsic = new IntrinsicContext(targetMethod, inlinedMethod, intrinsicBytecodeProvider, CompilationContext.INLINE_DURING_PARSING);
            }
            if (inlinedMethod.hasBytecodes())
            {
                notifyBeforeInline(inlinedMethod);
                parseAndInlineCallee(inlinedMethod, args, intrinsic);
                notifyAfterInline(inlinedMethod);
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    protected void notifyBeforeInline(ResolvedJavaMethod inlinedMethod)
    {
        for (InlineInvokePlugin plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            plugin.notifyBeforeInline(inlinedMethod);
        }
    }

    protected void notifyAfterInline(ResolvedJavaMethod inlinedMethod)
    {
        for (InlineInvokePlugin plugin : graphBuilderConfig.getPlugins().getInlineInvokePlugins())
        {
            plugin.notifyAfterInline(inlinedMethod);
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

    protected RuntimeException throwParserError(Throwable e)
    {
        if (e instanceof BytecodeParserError)
        {
            throw (BytecodeParserError) e;
        }
        BytecodeParser bp = this;
        BytecodeParserError res = new BytecodeParserError(e);
        while (bp != null)
        {
            res.addContext("parsing " + bp.code.asStackTraceElement(bp.bci()));
            bp = bp.parent;
        }
        throw res;
    }

    protected void parseAndInlineCallee(ResolvedJavaMethod targetMethod, ValueNode[] args, IntrinsicContext calleeIntrinsicContext)
    {
        FixedWithNextNode calleeBeforeUnwindNode = null;
        ValueNode calleeUnwindValue = null;

        try (IntrinsicScope s = calleeIntrinsicContext != null && !parsingIntrinsic() ? new IntrinsicScope(this, targetMethod.getSignature().toParameterKinds(!targetMethod.isStatic()), args) : null)
        {
            BytecodeParser parser = graphBuilderInstance.createBytecodeParser(graph, this, targetMethod, JVMCICompiler.INVOCATION_ENTRY_BCI, calleeIntrinsicContext);
            FrameStateBuilder startFrameState = new FrameStateBuilder(parser, parser.code, graph);
            if (!targetMethod.isStatic())
            {
                args[0] = nullCheckedValue(args[0]);
            }
            startFrameState.initializeFromArgumentsArray(args);
            parser.build(this.lastInstr, startFrameState);

            if (parser.returnDataList == null)
            {
                // Callee does not return.
                lastInstr = null;
            }
            else
            {
                ValueNode calleeReturnValue;
                MergeNode returnMergeNode = null;
                if (s != null)
                {
                    s.returnDataList = parser.returnDataList;
                }
                if (parser.returnDataList.size() == 1)
                {
                    // Callee has a single return, we can continue parsing at that point.
                    ReturnToCallerData singleReturnData = parser.returnDataList.get(0);
                    lastInstr = singleReturnData.beforeReturnNode;
                    calleeReturnValue = singleReturnData.returnValue;
                }
                else
                {
                    // Callee has multiple returns, we need to insert a control flow merge.
                    returnMergeNode = graph.add(new MergeNode());
                    calleeReturnValue = ValueMergeUtil.mergeValueProducers(returnMergeNode, parser.returnDataList, returnData -> returnData.beforeReturnNode, returnData -> returnData.returnValue);
                }

                if (calleeReturnValue != null)
                {
                    frameState.push(targetMethod.getSignature().getReturnKind().getStackKind(), calleeReturnValue);
                }
                if (returnMergeNode != null)
                {
                    returnMergeNode.setStateAfter(createFrameState(stream.nextBCI(), returnMergeNode));
                    lastInstr = finishInstruction(returnMergeNode, frameState);
                }
            }
            // Propagate any side effects into the caller when parsing intrinsics.
            if (parser.frameState.isAfterSideEffect() && parsingIntrinsic())
            {
                for (StateSplit sideEffect : parser.frameState.sideEffects())
                {
                    frameState.addSideEffect(sideEffect);
                }
            }

            calleeBeforeUnwindNode = parser.getBeforeUnwindNode();
            if (calleeBeforeUnwindNode != null)
            {
                calleeUnwindValue = parser.getUnwindValue();
            }
        }

        /*
         * Method handleException will call createTarget, which wires this exception edge to the
         * corresponding exception dispatch block in the caller. In the case where it wires to the
         * caller's unwind block, any FrameState created meanwhile, e.g., FrameState for
         * LoopExitNode, would be instantiated with AFTER_EXCEPTION_BCI. Such frame states should
         * not be fixed by IntrinsicScope.close, as they denote the states of the caller. Thus, the
         * following code should be placed outside the IntrinsicScope, so that correctly created
         * FrameStates are not replaced.
         */
        if (calleeBeforeUnwindNode != null)
        {
            calleeBeforeUnwindNode.setNext(handleException(calleeUnwindValue, bci(), false));
        }
    }

    public MethodCallTargetNode createMethodCallTarget(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] args, StampPair returnStamp, JavaTypeProfile profile)
    {
        return new MethodCallTargetNode(invokeKind, targetMethod, args, returnStamp, profile);
    }

    protected InvokeNode createInvoke(int invokeBci, CallTargetNode callTarget, JavaKind resultType)
    {
        InvokeNode invoke = append(new InvokeNode(callTarget, invokeBci));
        frameState.pushReturn(resultType, invoke);
        invoke.setStateAfter(createFrameState(stream.nextBCI(), invoke));
        return invoke;
    }

    protected InvokeWithExceptionNode createInvokeWithException(int invokeBci, CallTargetNode callTarget, JavaKind resultType, ExceptionEdgeAction exceptionEdgeAction)
    {
        if (currentBlock != null && stream.nextBCI() > currentBlock.endBci)
        {
            // Clear non-live locals early so that the exception handler entry gets the cleared state.
            frameState.clearNonLiveLocals(currentBlock, liveness, false);
        }

        AbstractBeginNode exceptionEdge = handleException(null, bci(), exceptionEdgeAction == ExceptionEdgeAction.INCLUDE_AND_DEOPTIMIZE);
        InvokeWithExceptionNode invoke = append(new InvokeWithExceptionNode(callTarget, exceptionEdge, invokeBci));
        frameState.pushReturn(resultType, invoke);
        invoke.setStateAfter(createFrameState(stream.nextBCI(), invoke));
        return invoke;
    }

    protected void genReturn(ValueNode returnVal, JavaKind returnKind)
    {
        if (parsingIntrinsic() && returnVal != null)
        {
            if (returnVal instanceof StateSplit)
            {
                StateSplit stateSplit = (StateSplit) returnVal;
                FrameState stateAfter = stateSplit.stateAfter();
                if (stateSplit.hasSideEffect())
                {
                    if (stateAfter.bci == BytecodeFrame.AFTER_BCI)
                    {
                        stateAfter.replaceAtUsages(graph.add(new FrameState(BytecodeFrame.AFTER_BCI, returnVal)));
                        GraphUtil.killWithUnusedFloatingInputs(stateAfter);
                    }
                    else
                    {
                        // This must be the return value from within a partial intrinsification.
                    }
                }
            }
        }

        ValueNode realReturnVal = processReturnValue(returnVal, returnKind);

        frameState.setRethrowException(false);
        frameState.clearStack();
        beforeReturn(realReturnVal, returnKind);
        if (parent == null)
        {
            append(new ReturnNode(realReturnVal));
        }
        else
        {
            if (returnDataList == null)
            {
                returnDataList = new ArrayList<>();
            }
            returnDataList.add(new ReturnToCallerData(realReturnVal, lastInstr));
            lastInstr = null;
        }
    }

    private ValueNode processReturnValue(ValueNode value, JavaKind kind)
    {
        JavaKind returnKind = method.getSignature().getReturnKind();
        if (kind != returnKind)
        {
            // sub-word integer
            IntegerStamp stamp = (IntegerStamp) value.stamp(NodeView.DEFAULT);

            // the bytecode verifier doesn't check that the value is in the correct range
            if (stamp.lowerBound() < returnKind.getMinValue() || returnKind.getMaxValue() < stamp.upperBound())
            {
                ValueNode narrow = append(genNarrow(value, returnKind.getBitCount()));
                if (returnKind.isUnsigned())
                {
                    return append(genZeroExtend(narrow, 32));
                }
                else
                {
                    return append(genSignExtend(narrow, 32));
                }
            }
        }

        return value;
    }

    private void beforeReturn(ValueNode x, JavaKind kind)
    {
        if (graph.method() != null && graph.method().isJavaLangObjectInit())
        {
            /*
             * Get the receiver from the initial state since bytecode rewriting could do arbitrary
             * things to the state of the locals.
             */
            ValueNode receiver = graph.start().stateAfter().localAt(0);
            if (RegisterFinalizerNode.mayHaveFinalizer(receiver, graph.getAssumptions()))
            {
                append(new RegisterFinalizerNode(receiver));
            }
        }
        genInfoPointNode(InfopointReason.METHOD_END, x);
        if (finalBarrierRequired)
        {
            /*
             * When compiling an OSR with a final field store, don't bother tracking the original
             * receiver since the receiver cannot be EA'ed.
             */
            append(new FinalFieldBarrierNode(entryBCI == JVMCICompiler.INVOCATION_ENTRY_BCI ? originalReceiver : null));
        }
        synchronizedEpilogue(BytecodeFrame.AFTER_BCI, x, kind);
    }

    protected MonitorEnterNode createMonitorEnterNode(ValueNode x, MonitorIdNode monitorId)
    {
        return new MonitorEnterNode(x, monitorId);
    }

    protected void genMonitorEnter(ValueNode x, int bci)
    {
        MonitorIdNode monitorId = graph.add(new MonitorIdNode(frameState.lockDepth(true)));
        MonitorEnterNode monitorEnter = append(createMonitorEnterNode(x, monitorId));
        frameState.pushLock(x, monitorId);
        monitorEnter.setStateAfter(createFrameState(bci, monitorEnter));
    }

    protected void genMonitorExit(ValueNode x, ValueNode escapedReturnValue, int bci)
    {
        if (frameState.lockDepth(false) == 0)
        {
            throw bailout("unbalanced monitors: too many exits");
        }
        MonitorIdNode monitorId = frameState.peekMonitorId();
        ValueNode lockedObject = frameState.popLock();
        if (GraphUtil.originalValue(lockedObject) != GraphUtil.originalValue(x))
        {
            throw bailout(String.format("unbalanced monitors: mismatch at monitorexit, %s != %s", GraphUtil.originalValue(x), GraphUtil.originalValue(lockedObject)));
        }
        MonitorExitNode monitorExit = append(new MonitorExitNode(lockedObject, monitorId, escapedReturnValue));
        monitorExit.setStateAfter(createFrameState(bci, monitorExit));
    }

    protected void genJsr(int dest)
    {
        BciBlock successor = currentBlock.getJsrSuccessor();
        JsrScope scope = currentBlock.getJsrScope();
        int nextBci = getStream().nextBCI();
        if (!successor.getJsrScope().pop().equals(scope))
        {
            throw new JsrNotSupportedBailout("unstructured control flow (internal limitation)");
        }
        if (successor.getJsrScope().nextReturnAddress() != nextBci)
        {
            throw new JsrNotSupportedBailout("unstructured control flow (internal limitation)");
        }
        ConstantNode nextBciNode = getJsrConstant(nextBci);
        frameState.push(JavaKind.Object, nextBciNode);
        appendGoto(successor);
    }

    protected void genRet(int localIndex)
    {
        BciBlock successor = currentBlock.getRetSuccessor();
        ValueNode local = frameState.loadLocal(localIndex, JavaKind.Object);
        JsrScope scope = currentBlock.getJsrScope();
        int retAddress = scope.nextReturnAddress();
        ConstantNode returnBciNode = getJsrConstant(retAddress);
        LogicNode guard = IntegerEqualsNode.create(constantReflection, metaAccess, options, null, local, returnBciNode, NodeView.DEFAULT);
        guard = graph.addOrUniqueWithInputs(guard);
        append(new FixedGuardNode(guard, DeoptimizationReason.JavaSubroutineMismatch, DeoptimizationAction.InvalidateReprofile));
        if (!successor.getJsrScope().equals(scope.pop()))
        {
            throw new JsrNotSupportedBailout("unstructured control flow (ret leaves more than one scope)");
        }
        appendGoto(successor);
    }

    private ConstantNode getJsrConstant(long bci)
    {
        JavaConstant nextBciConstant = new RawConstant(bci);
        Stamp nextBciStamp = StampFactory.forConstant(nextBciConstant);
        ConstantNode nextBciNode = new ConstantNode(nextBciConstant, nextBciStamp);
        return graph.unique(nextBciNode);
    }

    protected void genIntegerSwitch(ValueNode value, ArrayList<BciBlock> actualSuccessors, int[] keys, double[] keyProbabilities, int[] keySuccessors)
    {
        if (value.isConstant())
        {
            JavaConstant constant = (JavaConstant) value.asConstant();
            int constantValue = constant.asInt();
            for (int i = 0; i < keys.length; ++i)
            {
                if (keys[i] == constantValue)
                {
                    appendGoto(actualSuccessors.get(keySuccessors[i]));
                    return;
                }
            }
            appendGoto(actualSuccessors.get(keySuccessors[keys.length]));
        }
        else
        {
            this.controlFlowSplit = true;
            double[] successorProbabilities = successorProbabilites(actualSuccessors.size(), keySuccessors, keyProbabilities);
            IntegerSwitchNode switchNode = append(new IntegerSwitchNode(value, actualSuccessors.size(), keys, keyProbabilities, keySuccessors));
            for (int i = 0; i < actualSuccessors.size(); i++)
            {
                switchNode.setBlockSuccessor(i, createBlockTarget(successorProbabilities[i], actualSuccessors.get(i), frameState));
            }
        }
    }

    /**
     * Helper function that sums up the probabilities of all keys that lead to a specific successor.
     *
     * @return an array of size successorCount with the accumulated probability for each successor.
     */
    private static double[] successorProbabilites(int successorCount, int[] keySuccessors, double[] keyProbabilities)
    {
        double[] probability = new double[successorCount];
        for (int i = 0; i < keySuccessors.length; i++)
        {
            probability[keySuccessors[i]] += keyProbabilities[i];
        }
        return probability;
    }

    protected ConstantNode appendConstant(JavaConstant constant)
    {
        return ConstantNode.forConstant(constant, metaAccess, graph);
    }

    @Override
    public <T extends ValueNode> T append(T v)
    {
        if (v.graph() != null)
        {
            return v;
        }
        T added = graph.addOrUniqueWithInputs(v);
        if (added == v)
        {
            updateLastInstruction(v);
        }
        return added;
    }

    private <T extends ValueNode> void updateLastInstruction(T v)
    {
        if (v instanceof FixedNode)
        {
            FixedNode fixedNode = (FixedNode) v;
            lastInstr.setNext(fixedNode);
            if (fixedNode instanceof FixedWithNextNode)
            {
                FixedWithNextNode fixedWithNextNode = (FixedWithNextNode) fixedNode;
                lastInstr = fixedWithNextNode;
            }
            else
            {
                lastInstr = null;
            }
        }
    }

    private Target checkLoopExit(FixedNode target, BciBlock targetBlock, FrameStateBuilder state)
    {
        if (currentBlock != null)
        {
            long exits = currentBlock.loops & ~targetBlock.loops;
            if (exits != 0)
            {
                LoopExitNode firstLoopExit = null;
                LoopExitNode lastLoopExit = null;

                int pos = 0;
                ArrayList<BciBlock> exitLoops = new ArrayList<>(Long.bitCount(exits));
                do
                {
                    long lMask = 1L << pos;
                    if ((exits & lMask) != 0)
                    {
                        exitLoops.add(blockMap.getLoopHeader(pos));
                        exits &= ~lMask;
                    }
                    pos++;
                } while (exits != 0);

                Collections.sort(exitLoops, new Comparator<BciBlock>()
                {
                    @Override
                    public int compare(BciBlock o1, BciBlock o2)
                    {
                        return Long.bitCount(o2.loops) - Long.bitCount(o1.loops);
                    }
                });

                int bci = targetBlock.startBci;
                if (targetBlock instanceof ExceptionDispatchBlock)
                {
                    bci = ((ExceptionDispatchBlock) targetBlock).deoptBci;
                }
                FrameStateBuilder newState = state.copy();
                for (BciBlock loop : exitLoops)
                {
                    LoopBeginNode loopBegin = (LoopBeginNode) getFirstInstruction(loop);
                    LoopExitNode loopExit = graph.add(new LoopExitNode(loopBegin));
                    if (lastLoopExit != null)
                    {
                        lastLoopExit.setNext(loopExit);
                    }
                    if (firstLoopExit == null)
                    {
                        firstLoopExit = loopExit;
                    }
                    lastLoopExit = loopExit;
                    newState.clearNonLiveLocals(targetBlock, liveness, true);
                    newState.insertLoopProxies(loopExit, getEntryState(loop));
                    loopExit.setStateAfter(newState.create(bci, loopExit));
                }

                lastLoopExit.setNext(target);
                return new Target(firstLoopExit, newState);
            }
        }
        return new Target(target, state);
    }

    private FrameStateBuilder getEntryState(BciBlock block)
    {
        return entryStateArray[block.id];
    }

    private void setEntryState(BciBlock block, FrameStateBuilder entryState)
    {
        this.entryStateArray[block.id] = entryState;
    }

    private void setFirstInstruction(BciBlock block, FixedWithNextNode firstInstruction)
    {
        this.firstInstructionArray[block.id] = firstInstruction;
    }

    private FixedWithNextNode getFirstInstruction(BciBlock block)
    {
        return firstInstructionArray[block.id];
    }

    private FixedNode createTarget(double probability, BciBlock block, FrameStateBuilder stateAfter)
    {
        if (isNeverExecutedCode(probability))
        {
            return graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.UnreachedCode));
        }
        else
        {
            return createTarget(block, stateAfter);
        }
    }

    private FixedNode createTarget(BciBlock block, FrameStateBuilder state)
    {
        return createTarget(block, state, false, false);
    }

    private FixedNode createTarget(BciBlock block, FrameStateBuilder state, boolean canReuseInstruction, boolean canReuseState)
    {
        if (getFirstInstruction(block) == null)
        {
            /*
             * This is the first time we see this block as a branch target. Create and return a
             * placeholder that later can be replaced with a MergeNode when we see this block again.
             */
            FixedNode targetNode;
            if (canReuseInstruction && (block.getPredecessorCount() == 1 || !controlFlowSplit) && !block.isLoopHeader() && (currentBlock.loops & ~block.loops) == 0)
            {
                setFirstInstruction(block, lastInstr);
                lastInstr = null;
            }
            else
            {
                setFirstInstruction(block, graph.add(new BeginNode()));
            }
            targetNode = getFirstInstruction(block);
            Target target = checkLoopExit(targetNode, block, state);
            FixedNode result = target.fixed;
            FrameStateBuilder currentEntryState = target.state == state ? (canReuseState ? state : state.copy()) : target.state;
            setEntryState(block, currentEntryState);
            currentEntryState.clearNonLiveLocals(block, liveness, true);

            return result;
        }

        // We already saw this block before, so we have to merge states.
        if (!getEntryState(block).isCompatibleWith(state))
        {
            throw bailout(String.format("stacks do not match on merge from %d into %s; bytecodes would not verify:%nexpect: %s%nactual: %s", bci(), block, getEntryState(block), state));
        }

        if (getFirstInstruction(block) instanceof LoopBeginNode)
        {
            /*
             * Backward loop edge. We need to create a special LoopEndNode and merge with the
             * loop begin node created before.
             */
            LoopBeginNode loopBegin = (LoopBeginNode) getFirstInstruction(block);
            LoopEndNode loopEnd = graph.add(new LoopEndNode(loopBegin));
            Target target = checkLoopExit(loopEnd, block, state);
            FixedNode result = target.fixed;
            getEntryState(block).merge(loopBegin, target.state);

            return result;
        }

        if (getFirstInstruction(block) instanceof AbstractBeginNode && !(getFirstInstruction(block) instanceof AbstractMergeNode))
        {
            /*
             * This is the second time we see this block. Create the actual MergeNode and the
             * End Node for the already existing edge.
             */
            AbstractBeginNode beginNode = (AbstractBeginNode) getFirstInstruction(block);

            // The EndNode for the already existing edge.
            EndNode end = graph.add(new EndNode());
            // The MergeNode that replaces the placeholder.
            AbstractMergeNode mergeNode = graph.add(new MergeNode());
            FixedNode next = beginNode.next();

            if (beginNode.predecessor() instanceof ControlSplitNode)
            {
                beginNode.setNext(end);
            }
            else
            {
                beginNode.replaceAtPredecessor(end);
                beginNode.safeDelete();
            }

            mergeNode.addForwardEnd(end);
            mergeNode.setNext(next);

            setFirstInstruction(block, mergeNode);
        }

        AbstractMergeNode mergeNode = (AbstractMergeNode) getFirstInstruction(block);

        // The EndNode for the newly merged edge.
        EndNode newEnd = graph.add(new EndNode());
        Target target = checkLoopExit(newEnd, block, state);
        FixedNode result = target.fixed;
        getEntryState(block).merge(mergeNode, target.state);
        mergeNode.addForwardEnd(newEnd);

        return result;
    }

    /**
     * Returns a block begin node with the specified state. If the specified probability is 0, the
     * block deoptimizes immediately.
     */
    private AbstractBeginNode createBlockTarget(double probability, BciBlock block, FrameStateBuilder stateAfter)
    {
        FixedNode target = createTarget(probability, block, stateAfter);
        AbstractBeginNode begin = BeginNode.begin(target);

        return begin;
    }

    private ValueNode synchronizedObject(FrameStateBuilder state, ResolvedJavaMethod target)
    {
        if (target.isStatic())
        {
            return appendConstant(getConstantReflection().asJavaClass(target.getDeclaringClass()));
        }
        else
        {
            return state.loadLocal(0, JavaKind.Object);
        }
    }

    protected void processBlock(BciBlock block)
    {
        // Ignore blocks that have no predecessors by the time their bytecodes are parsed
        FixedWithNextNode firstInstruction = getFirstInstruction(block);
        if (firstInstruction == null)
        {
            return;
        }

        lastInstr = firstInstruction;
        frameState = getEntryState(block);
        setCurrentFrameState(frameState);
        currentBlock = block;

        if (block != blockMap.getUnwindBlock() && !(block instanceof ExceptionDispatchBlock))
        {
            frameState.setRethrowException(false);
        }

        if (firstInstruction instanceof AbstractMergeNode)
        {
            setMergeStateAfter(block, firstInstruction);
        }

        if (block == blockMap.getUnwindBlock())
        {
            handleUnwindBlock((ExceptionDispatchBlock) block);
        }
        else if (block instanceof ExceptionDispatchBlock)
        {
            createExceptionDispatch((ExceptionDispatchBlock) block);
        }
        else
        {
            iterateBytecodesForBlock(block);
        }
    }

    private void handleUnwindBlock(ExceptionDispatchBlock block)
    {
        if (parent == null)
        {
            finishPrepare(lastInstr, block.deoptBci);
            frameState.setRethrowException(false);
            createUnwind();
        }
        else
        {
            ValueNode exception = frameState.pop(JavaKind.Object);
            this.unwindValue = exception;
            this.beforeUnwindNode = this.lastInstr;
        }
    }

    private void setMergeStateAfter(BciBlock block, FixedWithNextNode firstInstruction)
    {
        AbstractMergeNode abstractMergeNode = (AbstractMergeNode) firstInstruction;
        if (abstractMergeNode.stateAfter() == null)
        {
            int bci = block.startBci;
            if (block instanceof ExceptionDispatchBlock)
            {
                bci = ((ExceptionDispatchBlock) block).deoptBci;
            }
            abstractMergeNode.setStateAfter(createFrameState(bci, abstractMergeNode));
        }
    }

    private void createUnwind()
    {
        synchronizedEpilogue(BytecodeFrame.AFTER_EXCEPTION_BCI, null, null);
        ValueNode exception = frameState.pop(JavaKind.Object);
        append(new UnwindNode(exception));
    }

    private void synchronizedEpilogue(int bci, ValueNode currentReturnValue, JavaKind currentReturnValueKind)
    {
        if (method.isSynchronized())
        {
            if (currentReturnValue != null)
            {
                frameState.push(currentReturnValueKind, currentReturnValue);
            }
            genMonitorExit(methodSynchronizedObject, currentReturnValue, bci);
            finishPrepare(lastInstr, bci);
        }
        if (frameState.lockDepth(false) != 0)
        {
            throw bailout("unbalanced monitors: too few exits exiting frame");
        }
    }

    private void createExceptionDispatch(ExceptionDispatchBlock block)
    {
        lastInstr = finishInstruction(lastInstr, frameState);

        if (block.handler.isCatchAll())
        {
            appendGoto(block.getSuccessor(0));
            return;
        }

        JavaType catchType = block.handler.getCatchType();
        if (graphBuilderConfig.eagerResolving())
        {
            catchType = lookupType(block.handler.catchTypeCPI(), Bytecodes.INSTANCEOF);
        }
        if (catchType instanceof ResolvedJavaType)
        {
            TypeReference checkedCatchType = TypeReference.createTrusted(graph.getAssumptions(), (ResolvedJavaType) catchType);

            if (graphBuilderConfig.getSkippedExceptionTypes() != null)
            {
                for (ResolvedJavaType skippedType : graphBuilderConfig.getSkippedExceptionTypes())
                {
                    if (skippedType.isAssignableFrom(checkedCatchType.getType()))
                    {
                        BciBlock nextBlock = block.getSuccessorCount() == 1 ? blockMap.getUnwindBlock() : block.getSuccessor(1);
                        ValueNode exception = frameState.stack[0];
                        FixedNode trueSuccessor = graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.UnreachedCode));
                        FixedNode nextDispatch = createTarget(nextBlock, frameState);
                        append(new IfNode(graph.addOrUniqueWithInputs(createInstanceOf(checkedCatchType, exception)), trueSuccessor, nextDispatch, 0));
                        return;
                    }
                }
            }

            BciBlock nextBlock = block.getSuccessorCount() == 1 ? blockMap.getUnwindBlock() : block.getSuccessor(1);
            ValueNode exception = frameState.stack[0];
            // Anchor for the piNode, which must be before any LoopExit inserted by createTarget.
            BeginNode piNodeAnchor = graph.add(new BeginNode());
            ObjectStamp checkedStamp = StampFactory.objectNonNull(checkedCatchType);
            PiNode piNode = graph.addWithoutUnique(new PiNode(exception, checkedStamp));
            frameState.pop(JavaKind.Object);
            frameState.push(JavaKind.Object, piNode);
            FixedNode catchSuccessor = createTarget(block.getSuccessor(0), frameState);
            frameState.pop(JavaKind.Object);
            frameState.push(JavaKind.Object, exception);
            FixedNode nextDispatch = createTarget(nextBlock, frameState);
            piNodeAnchor.setNext(catchSuccessor);
            IfNode ifNode = append(new IfNode(graph.unique(createInstanceOf(checkedCatchType, exception)), piNodeAnchor, nextDispatch, 0.5));
            piNode.setGuard(ifNode.trueSuccessor());
        }
        else
        {
            handleUnresolvedExceptionType(catchType);
        }
    }

    private void appendGoto(BciBlock successor)
    {
        FixedNode targetInstr = createTarget(successor, frameState, true, true);
        if (lastInstr != null && lastInstr != targetInstr)
        {
            lastInstr.setNext(targetInstr);
        }
    }

    protected void iterateBytecodesForBlock(BciBlock block)
    {
        if (block.isLoopHeader())
        {
            // Create the loop header block, which later will merge the backward branches of the loop.
            controlFlowSplit = true;
            LoopBeginNode loopBegin = appendLoopBegin(this.lastInstr, block.startBci);
            lastInstr = loopBegin;

            // Create phi functions for all local variables and operand stack slots.
            frameState.insertLoopPhis(liveness, block.loopId, loopBegin, forceLoopPhis(), stampFromValueForForcedPhis());
            loopBegin.setStateAfter(createFrameState(block.startBci, loopBegin));

            /*
             * We have seen all forward branches. All subsequent backward branches will merge to the
             * loop header. This ensures that the loop header has exactly one non-loop predecessor.
             */
            setFirstInstruction(block, loopBegin);
            /*
             * We need to preserve the frame state builder of the loop header so that we can merge
             * values for phi functions, so make a copy of it.
             */
            setEntryState(block, frameState.copy());
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

        int endBCI = stream.endBCI();

        stream.setBCI(block.startBci);
        int bci = block.startBci;

        // Reset line number for new block
        if (graphBuilderConfig.insertFullInfopoints())
        {
            previousLineNumber = -1;
        }

        while (bci < endBCI)
        {
            try
            {
                if (graphBuilderConfig.insertFullInfopoints() && !parsingIntrinsic())
                {
                    currentLineNumber = lnt != null ? lnt.getLineNumber(bci) : -1;
                    if (currentLineNumber != previousLineNumber)
                    {
                        genInfoPointNode(InfopointReason.BYTECODE_POSITION, null);
                        previousLineNumber = currentLineNumber;
                    }
                }

                // read the opcode
                int opcode = stream.currentBC();
                if (parent == null && bci == entryBCI)
                {
                    if (block.getJsrScope() != JsrScope.EMPTY_SCOPE)
                    {
                        throw new JsrNotSupportedBailout("OSR into a Bytecodes.JSR scope is not supported");
                    }
                    EntryMarkerNode x = append(new EntryMarkerNode());
                    frameState.insertProxies(value -> graph.unique(new EntryProxyNode(value, x)));
                    x.setStateAfter(createFrameState(bci, x));
                }

                processBytecode(bci, opcode);
            }
            catch (BailoutException e)
            {
                // Don't wrap bailouts as parser errors
                throw e;
            }
            catch (Throwable e)
            {
                throw throwParserError(e);
            }

            if (lastInstr == null || lastInstr.next() != null)
            {
                break;
            }

            stream.next();
            bci = stream.currentBCI();

            lastInstr = finishInstruction(lastInstr, frameState);
            if (bci < endBCI)
            {
                if (bci > block.endBci)
                {
                    // we fell through to the next block, add a goto and break
                    appendGoto(block.getSuccessor(0));
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

    private LoopBeginNode appendLoopBegin(FixedWithNextNode fixedWithNext, int startBci)
    {
        EndNode preLoopEnd = graph.add(new EndNode());
        LoopBeginNode loopBegin = graph.add(new LoopBeginNode());
        if (disableLoopSafepoint())
        {
            loopBegin.disableSafepoint();
        }
        fixedWithNext.setNext(preLoopEnd);
        // Add the single non-loop predecessor of the loop header.
        loopBegin.addForwardEnd(preLoopEnd);
        return loopBegin;
    }

    /**
     * Hook for subclasses to modify the last instruction or add other instructions.
     *
     * @param instr The last instruction (= fixed node) which was added.
     * @param state The current frame state.
     * @return Returns the (new) last instruction.
     */
    protected FixedWithNextNode finishInstruction(FixedWithNextNode instr, FrameStateBuilder state)
    {
        return instr;
    }

    private void genInfoPointNode(InfopointReason reason, ValueNode escapedReturnValue)
    {
        if (!parsingIntrinsic() && graphBuilderConfig.insertFullInfopoints())
        {
            append(new FullInfopointNode(reason, createFrameState(bci(), null), escapedReturnValue));
        }
    }

    protected void genIf(ValueNode x, Condition cond, ValueNode y)
    {
        BciBlock trueBlock = currentBlock.getSuccessor(0);
        BciBlock falseBlock = currentBlock.getSuccessor(1);

        if (trueBlock == falseBlock)
        {
            // The target block is the same independent of the condition.
            appendGoto(trueBlock);
            return;
        }

        ValueNode a = x;
        ValueNode b = y;
        BciBlock trueSuccessor = trueBlock;
        BciBlock falseSuccessor = falseBlock;

        CanonicalizedCondition canonicalizedCondition = cond.canonicalize();

        // Check whether the condition needs to mirror the operands.
        if (canonicalizedCondition.mustMirror())
        {
            a = y;
            b = x;
        }
        if (canonicalizedCondition.mustNegate())
        {
            trueSuccessor = falseBlock;
            falseSuccessor = trueBlock;
        }

        // Create the logic node for the condition.
        LogicNode condition = createLogicNode(canonicalizedCondition.getCanonicalCondition(), a, b);

        double probability = -1;
        if (condition instanceof IntegerEqualsNode)
        {
            probability = extractInjectedProbability((IntegerEqualsNode) condition);
            // the probability coming from here is about the actual condition
        }

        if (probability == -1)
        {
            probability = getProfileProbability(canonicalizedCondition.mustNegate());
        }

        probability = clampProbability(probability);
        genIf(condition, trueSuccessor, falseSuccessor, probability);
    }

    protected double getProfileProbability(boolean negate)
    {
        if (profilingInfo == null)
        {
            return 0.5;
        }

        double probability = profilingInfo.getBranchTakenProbability(bci());

        if (probability < 0)
        {
            return 0.5;
        }

        if (negate && shouldComplementProbability())
        {
            // the probability coming from profile is about the original condition
            probability = 1 - probability;
        }
        return probability;
    }

    private static double extractInjectedProbability(IntegerEqualsNode condition)
    {
        // Propagate injected branch probability if any.
        IntegerEqualsNode equalsNode = condition;
        BranchProbabilityNode probabilityNode = null;
        ValueNode other = null;
        if (equalsNode.getX() instanceof BranchProbabilityNode)
        {
            probabilityNode = (BranchProbabilityNode) equalsNode.getX();
            other = equalsNode.getY();
        }
        else if (equalsNode.getY() instanceof BranchProbabilityNode)
        {
            probabilityNode = (BranchProbabilityNode) equalsNode.getY();
            other = equalsNode.getX();
        }

        if (probabilityNode != null && probabilityNode.getProbability().isConstant() && other != null && other.isConstant())
        {
            double probabilityValue = probabilityNode.getProbability().asJavaConstant().asDouble();
            return other.asJavaConstant().asInt() == 0 ? 1.0 - probabilityValue : probabilityValue;
        }
        return -1;
    }

    protected void genIf(LogicNode conditionInput, BciBlock trueBlockInput, BciBlock falseBlockInput, double probabilityInput)
    {
        BciBlock trueBlock = trueBlockInput;
        BciBlock falseBlock = falseBlockInput;
        LogicNode condition = conditionInput;
        double probability = probabilityInput;

        // Remove a logic negation node.
        if (condition instanceof LogicNegationNode)
        {
            LogicNegationNode logicNegationNode = (LogicNegationNode) condition;
            BciBlock tmpBlock = trueBlock;
            trueBlock = falseBlock;
            falseBlock = tmpBlock;
            if (shouldComplementProbability())
            {
                // the probability coming from profile is about the original condition
                probability = 1 - probability;
            }
            condition = logicNegationNode.getValue();
        }

        if (condition instanceof LogicConstantNode)
        {
            genConstantTargetIf(trueBlock, falseBlock, condition);
        }
        else
        {
            if (condition.graph() == null)
            {
                condition = genUnique(condition);
            }

            if (isNeverExecutedCode(probability))
            {
                if (!graph.isOSR() || getParent() != null || graph.getEntryBCI() != trueBlock.startBci)
                {
                    append(new FixedGuardNode(condition, DeoptimizationReason.UnreachedCode, DeoptimizationAction.InvalidateReprofile, true));
                    appendGoto(falseBlock);
                    return;
                }
            }
            else if (isNeverExecutedCode(1 - probability))
            {
                if (!graph.isOSR() || getParent() != null || graph.getEntryBCI() != falseBlock.startBci)
                {
                    append(new FixedGuardNode(condition, DeoptimizationReason.UnreachedCode, DeoptimizationAction.InvalidateReprofile, false));
                    appendGoto(trueBlock);
                    return;
                }
            }

            int oldBci = stream.currentBCI();
            int trueBlockInt = checkPositiveIntConstantPushed(trueBlock);
            if (trueBlockInt != -1)
            {
                int falseBlockInt = checkPositiveIntConstantPushed(falseBlock);
                if (falseBlockInt != -1)
                {
                    if (tryGenConditionalForIf(trueBlock, falseBlock, condition, oldBci, trueBlockInt, falseBlockInt))
                    {
                        return;
                    }
                }
            }

            this.controlFlowSplit = true;
            FixedNode trueSuccessor = createTarget(trueBlock, frameState, false, false);
            FixedNode falseSuccessor = createTarget(falseBlock, frameState, false, true);
            ValueNode ifNode = genIfNode(condition, trueSuccessor, falseSuccessor, probability);
            postProcessIfNode(ifNode);
            append(ifNode);
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
    protected void postProcessIfNode(ValueNode node)
    {
    }

    private boolean tryGenConditionalForIf(BciBlock trueBlock, BciBlock falseBlock, LogicNode condition, int oldBci, int trueBlockInt, int falseBlockInt)
    {
        if (gotoOrFallThroughAfterConstant(trueBlock) && gotoOrFallThroughAfterConstant(falseBlock) && trueBlock.getSuccessor(0) == falseBlock.getSuccessor(0))
        {
            genConditionalForIf(trueBlock, condition, oldBci, trueBlockInt, falseBlockInt, false);
            return true;
        }
        else if (this.parent != null && returnAfterConstant(trueBlock) && returnAfterConstant(falseBlock))
        {
            genConditionalForIf(trueBlock, condition, oldBci, trueBlockInt, falseBlockInt, true);
            return true;
        }
        return false;
    }

    private void genConditionalForIf(BciBlock trueBlock, LogicNode condition, int oldBci, int trueBlockInt, int falseBlockInt, boolean genReturn)
    {
        ConstantNode trueValue = graph.unique(ConstantNode.forInt(trueBlockInt));
        ConstantNode falseValue = graph.unique(ConstantNode.forInt(falseBlockInt));
        ValueNode conditionalNode = ConditionalNode.create(condition, trueValue, falseValue, NodeView.DEFAULT);
        if (conditionalNode.graph() == null)
        {
            conditionalNode = graph.addOrUniqueWithInputs(conditionalNode);
        }
        if (genReturn)
        {
            JavaKind returnKind = method.getSignature().getReturnKind().getStackKind();
            this.genReturn(conditionalNode, returnKind);
        }
        else
        {
            frameState.push(JavaKind.Int, conditionalNode);
            appendGoto(trueBlock.getSuccessor(0));
            stream.setBCI(oldBci);
        }
    }

    private LogicNode createLogicNode(CanonicalCondition cond, ValueNode a, ValueNode b)
    {
        switch (cond)
        {
            case EQ:
                if (a.getStackKind() == JavaKind.Object)
                {
                    return genObjectEquals(a, b);
                }
                else
                {
                    return genIntegerEquals(a, b);
                }
            case LT:
                return genIntegerLessThan(a, b);
            default:
                throw GraalError.shouldNotReachHere("Unexpected condition: " + cond);
        }
    }

    private void genConstantTargetIf(BciBlock trueBlock, BciBlock falseBlock, LogicNode condition)
    {
        LogicConstantNode constantLogicNode = (LogicConstantNode) condition;
        boolean value = constantLogicNode.getValue();
        BciBlock nextBlock = falseBlock;
        if (value)
        {
            nextBlock = trueBlock;
        }
        int startBci = nextBlock.startBci;
        int targetAtStart = stream.readUByte(startBci);
        if (targetAtStart == Bytecodes.GOTO && nextBlock.getPredecessorCount() == 1)
        {
            // This is an empty block. Skip it.
            BciBlock successorBlock = nextBlock.successors.get(0);
            appendGoto(successorBlock);
        }
        else
        {
            appendGoto(nextBlock);
        }
    }

    private int checkPositiveIntConstantPushed(BciBlock block)
    {
        stream.setBCI(block.startBci);
        int currentBC = stream.currentBC();
        if (currentBC >= Bytecodes.ICONST_0 && currentBC <= Bytecodes.ICONST_5)
        {
            int constValue = currentBC - Bytecodes.ICONST_0;
            return constValue;
        }
        return -1;
    }

    private boolean gotoOrFallThroughAfterConstant(BciBlock block)
    {
        stream.setBCI(block.startBci);
        int currentBCI = stream.nextBCI();
        stream.setBCI(currentBCI);
        int currentBC = stream.currentBC();
        return stream.currentBCI() > block.endBci || currentBC == Bytecodes.GOTO || currentBC == Bytecodes.GOTO_W;
    }

    private boolean returnAfterConstant(BciBlock block)
    {
        stream.setBCI(block.startBci);
        int currentBCI = stream.nextBCI();
        stream.setBCI(currentBCI);
        int currentBC = stream.currentBC();
        return currentBC == Bytecodes.IRETURN;
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
    public void push(JavaKind slotKind, ValueNode value)
    {
        frameState.push(slotKind, value);
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
    public String toString()
    {
        Formatter fmt = new Formatter();
        BytecodeParser bp = this;
        String indent = "";
        while (bp != null)
        {
            if (bp != this)
            {
                fmt.format("%n%s", indent);
            }
            fmt.format("%s [bci: %d, intrinsic: %s]", bp.code.asStackTraceElement(bp.bci()), bp.bci(), bp.parsingIntrinsic());
            fmt.format("%n%s", new BytecodeDisassembler().disassemble(bp.code, bp.bci(), bp.bci() + 10));
            bp = bp.parent;
            indent += " ";
        }
        return fmt.toString();
    }

    @Override
    public BailoutException bailout(String string)
    {
        FrameState currentFrameState = createFrameState(bci(), null);
        StackTraceElement[] elements = GraphUtil.approxSourceStackTraceElement(currentFrameState);
        BailoutException bailout = new PermanentBailoutException(string);
        throw GraphUtil.createBailoutException(string, bailout, elements);
    }

    private FrameState createFrameState(int bci, StateSplit forStateSplit)
    {
        if (currentBlock != null && bci > currentBlock.endBci)
        {
            frameState.clearNonLiveLocals(currentBlock, liveness, false);
        }
        return frameState.create(bci, forStateSplit);
    }

    @Override
    public void setStateAfter(StateSplit sideEffect)
    {
        FrameState stateAfter = createFrameState(stream.nextBCI(), sideEffect);
        sideEffect.setStateAfter(stateAfter);
    }

    public void setCurrentFrameState(FrameStateBuilder frameState)
    {
        this.frameState = frameState;
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

    public void loadLocal(int index, JavaKind kind)
    {
        ValueNode value = frameState.loadLocal(index, kind);
        frameState.push(kind, value);
    }

    public void loadLocalObject(int index)
    {
        ValueNode value = frameState.loadLocal(index, JavaKind.Object);

        int nextBCI = stream.nextBCI();
        int nextBC = stream.readUByte(nextBCI);
        if (nextBCI <= currentBlock.endBci && nextBC == Bytecodes.GETFIELD)
        {
            stream.next();
            genGetField(stream.readCPI(), Bytecodes.GETFIELD, value);
        }
        else
        {
            frameState.push(JavaKind.Object, value);
        }
    }

    public void storeLocal(JavaKind kind, int index)
    {
        ValueNode value = frameState.pop(kind);
        frameState.storeLocal(index, kind, value);
    }

    private void genLoadConstant(int cpi, int opcode)
    {
        Object con = lookupConstant(cpi, opcode);

        if (con instanceof JavaType)
        {
            // this is a load of class constant which might be unresolved
            JavaType type = (JavaType) con;
            if (type instanceof ResolvedJavaType)
            {
                frameState.push(JavaKind.Object, appendConstant(getConstantReflection().asJavaClass((ResolvedJavaType) type)));
            }
            else
            {
                handleUnresolvedLoadConstant(type);
            }
        }
        else if (con instanceof JavaConstant)
        {
            JavaConstant constant = (JavaConstant) con;
            frameState.push(constant.getJavaKind(), appendConstant(constant));
        }
        else
        {
            throw new Error("lookupConstant returned an object of incorrect type");
        }
    }

    private void genLoadIndexed(JavaKind kind)
    {
        ValueNode index = frameState.pop(JavaKind.Int);
        ValueNode array = emitExplicitExceptions(frameState.pop(JavaKind.Object), index);

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleLoadIndexed(this, array, index, kind))
            {
                return;
            }
        }

        frameState.push(kind, append(genLoadIndexed(array, index, kind)));
    }

    private void genStoreIndexed(JavaKind kind)
    {
        ValueNode value = frameState.pop(kind);
        ValueNode index = frameState.pop(JavaKind.Int);
        ValueNode array = emitExplicitExceptions(frameState.pop(JavaKind.Object), index);

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleStoreIndexed(this, array, index, kind, value))
            {
                return;
            }
        }

        genStoreIndexed(array, index, kind, value);
    }

    private void genArithmeticOp(JavaKind kind, int opcode)
    {
        ValueNode y = frameState.pop(kind);
        ValueNode x = frameState.pop(kind);
        ValueNode v;
        switch (opcode)
        {
            case Bytecodes.IADD:
            case Bytecodes.LADD:
                v = genIntegerAdd(x, y);
                break;
            case Bytecodes.FADD:
            case Bytecodes.DADD:
                v = genFloatAdd(x, y);
                break;
            case Bytecodes.ISUB:
            case Bytecodes.LSUB:
                v = genIntegerSub(x, y);
                break;
            case Bytecodes.FSUB:
            case Bytecodes.DSUB:
                v = genFloatSub(x, y);
                break;
            case Bytecodes.IMUL:
            case Bytecodes.LMUL:
                v = genIntegerMul(x, y);
                break;
            case Bytecodes.FMUL:
            case Bytecodes.DMUL:
                v = genFloatMul(x, y);
                break;
            case Bytecodes.FDIV:
            case Bytecodes.DDIV:
                v = genFloatDiv(x, y);
                break;
            case Bytecodes.FREM:
            case Bytecodes.DREM:
                v = genFloatRem(x, y);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(kind, append(v));
    }

    private void genIntegerDivOp(JavaKind kind, int opcode)
    {
        ValueNode y = frameState.pop(kind);
        ValueNode x = frameState.pop(kind);
        ValueNode v;
        switch (opcode)
        {
            case Bytecodes.IDIV:
            case Bytecodes.LDIV:
                v = genIntegerDiv(x, y);
                break;
            case Bytecodes.IREM:
            case Bytecodes.LREM:
                v = genIntegerRem(x, y);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(kind, append(v));
    }

    private void genNegateOp(JavaKind kind)
    {
        ValueNode x = frameState.pop(kind);
        frameState.push(kind, append(genNegateOp(x)));
    }

    private void genShiftOp(JavaKind kind, int opcode)
    {
        ValueNode s = frameState.pop(JavaKind.Int);
        ValueNode x = frameState.pop(kind);
        ValueNode v;
        switch (opcode)
        {
            case Bytecodes.ISHL:
            case Bytecodes.LSHL:
                v = genLeftShift(x, s);
                break;
            case Bytecodes.ISHR:
            case Bytecodes.LSHR:
                v = genRightShift(x, s);
                break;
            case Bytecodes.IUSHR:
            case Bytecodes.LUSHR:
                v = genUnsignedRightShift(x, s);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(kind, append(v));
    }

    private void genLogicOp(JavaKind kind, int opcode)
    {
        ValueNode y = frameState.pop(kind);
        ValueNode x = frameState.pop(kind);
        ValueNode v;
        switch (opcode)
        {
            case Bytecodes.IAND:
            case Bytecodes.LAND:
                v = genAnd(x, y);
                break;
            case Bytecodes.IOR:
            case Bytecodes.LOR:
                v = genOr(x, y);
                break;
            case Bytecodes.IXOR:
            case Bytecodes.LXOR:
                v = genXor(x, y);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        frameState.push(kind, append(v));
    }

    private void genCompareOp(JavaKind kind, boolean isUnorderedLess)
    {
        ValueNode y = frameState.pop(kind);
        ValueNode x = frameState.pop(kind);
        frameState.push(JavaKind.Int, append(genNormalizeCompare(x, y, isUnorderedLess)));
    }

    private void genFloatConvert(FloatConvert op, JavaKind from, JavaKind to)
    {
        ValueNode input = frameState.pop(from);
        frameState.push(to, append(genFloatConvert(op, input)));
    }

    private void genSignExtend(JavaKind from, JavaKind to)
    {
        ValueNode input = frameState.pop(from);
        if (from != from.getStackKind())
        {
            input = append(genNarrow(input, from.getBitCount()));
        }
        frameState.push(to, append(genSignExtend(input, to.getBitCount())));
    }

    private void genZeroExtend(JavaKind from, JavaKind to)
    {
        ValueNode input = frameState.pop(from);
        if (from != from.getStackKind())
        {
            input = append(genNarrow(input, from.getBitCount()));
        }
        frameState.push(to, append(genZeroExtend(input, to.getBitCount())));
    }

    private void genNarrow(JavaKind from, JavaKind to)
    {
        ValueNode input = frameState.pop(from);
        frameState.push(to, append(genNarrow(input, to.getBitCount())));
    }

    private void genIncrement()
    {
        int index = getStream().readLocalIndex();
        int delta = getStream().readIncrement();
        ValueNode x = frameState.loadLocal(index, JavaKind.Int);
        ValueNode y = appendConstant(JavaConstant.forInt(delta));
        frameState.storeLocal(index, JavaKind.Int, append(genIntegerAdd(x, y)));
    }

    private void genIfZero(Condition cond)
    {
        ValueNode y = appendConstant(JavaConstant.INT_0);
        ValueNode x = frameState.pop(JavaKind.Int);
        genIf(x, cond, y);
    }

    private void genIfNull(Condition cond)
    {
        ValueNode y = appendConstant(JavaConstant.NULL_POINTER);
        ValueNode x = frameState.pop(JavaKind.Object);
        genIf(x, cond, y);
    }

    private void genIfSame(JavaKind kind, Condition cond)
    {
        ValueNode y = frameState.pop(kind);
        ValueNode x = frameState.pop(kind);
        genIf(x, cond, y);
    }

    private static void initialize(ResolvedJavaType resolvedType)
    {
        /*
         * Since we're potentially triggering class initialization here, we need synchronization to
         * mitigate the potential for class initialization related deadlock being caused by the
         * compiler (e.g., https://github.com/graalvm/graal-core/pull/232/files#r90788550).
         */
        synchronized (BytecodeParser.class)
        {
            resolvedType.initialize();
        }
    }

    protected JavaType lookupType(int cpi, int bytecode)
    {
        maybeEagerlyResolve(cpi, bytecode);
        JavaType result = constantPool.lookupType(cpi, bytecode);
        return result;
    }

    private JavaMethod lookupMethod(int cpi, int opcode)
    {
        maybeEagerlyResolve(cpi, opcode);
        JavaMethod result = constantPool.lookupMethod(cpi, opcode);
        return result;
    }

    protected JavaField lookupField(int cpi, int opcode)
    {
        maybeEagerlyResolve(cpi, opcode);
        JavaField result = constantPool.lookupField(cpi, method, opcode);
        if (parsingIntrinsic() || eagerInitializing)
        {
            if (result instanceof ResolvedJavaField)
            {
                ResolvedJavaType declaringClass = ((ResolvedJavaField) result).getDeclaringClass();
                if (!declaringClass.isInitialized())
                {
                    // Even with eager initialization, superinterfaces are not always initialized.
                    // See StaticInterfaceFieldTest
                    initialize(declaringClass);
                }
            }
        }
        return result;
    }

    private Object lookupConstant(int cpi, int opcode)
    {
        maybeEagerlyResolve(cpi, opcode);
        Object result = constantPool.lookupConstant(cpi);
        return result;
    }

    protected void maybeEagerlyResolve(int cpi, int bytecode)
    {
        if (intrinsicContext != null)
        {
            constantPool.loadReferencedType(cpi, bytecode);
        }
        else if (graphBuilderConfig.eagerResolving())
        {
            /*
             * Since we're potentially triggering class initialization here, we need synchronization
             * to mitigate the potential for class initialization related deadlock being caused by
             * the compiler (e.g., https://github.com/graalvm/graal-core/pull/232/files#r90788550).
             */
            synchronized (BytecodeParser.class)
            {
                constantPool.loadReferencedType(cpi, bytecode);
            }
        }
    }

    private JavaTypeProfile getProfileForTypeCheck(TypeReference type)
    {
        if (parsingIntrinsic() || profilingInfo == null || !optimisticOpts.useTypeCheckHints(getOptions()) || type.isExact())
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
        int cpi = getStream().readCPI();
        JavaType type = lookupType(cpi, Bytecodes.CHECKCAST);
        ValueNode object = frameState.pop(JavaKind.Object);

        if (!(type instanceof ResolvedJavaType))
        {
            handleUnresolvedCheckCast(type, object);
            return;
        }
        TypeReference checkedType = TypeReference.createTrusted(graph.getAssumptions(), (ResolvedJavaType) type);
        JavaTypeProfile profile = getProfileForTypeCheck(checkedType);

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleCheckCast(this, object, checkedType.getType(), profile))
            {
                return;
            }
        }

        ValueNode castNode = null;
        if (profile != null)
        {
            if (profile.getNullSeen().isFalse())
            {
                object = nullCheckedValue(object);
                ResolvedJavaType singleType = profile.asSingleType();
                if (singleType != null && checkedType.getType().isAssignableFrom(singleType))
                {
                    LogicNode typeCheck = append(createInstanceOf(TypeReference.createExactTrusted(singleType), object, profile));
                    if (typeCheck.isTautology())
                    {
                        castNode = object;
                    }
                    else
                    {
                        FixedGuardNode fixedGuard = append(new FixedGuardNode(typeCheck, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile, false));
                        castNode = append(PiNode.create(object, StampFactory.objectNonNull(TypeReference.createExactTrusted(singleType)), fixedGuard));
                    }
                }
            }
        }

        boolean nonNull = ((ObjectStamp) object.stamp(NodeView.DEFAULT)).nonNull();
        if (castNode == null)
        {
            LogicNode condition = genUnique(createInstanceOfAllowNull(checkedType, object, null));
            if (condition.isTautology())
            {
                castNode = object;
            }
            else
            {
                FixedGuardNode fixedGuard = append(new FixedGuardNode(condition, DeoptimizationReason.ClassCastException, DeoptimizationAction.InvalidateReprofile, false));
                castNode = append(PiNode.create(object, StampFactory.object(checkedType, nonNull), fixedGuard));
            }
        }
        frameState.push(JavaKind.Object, castNode);
    }

    private void genInstanceOf()
    {
        int cpi = getStream().readCPI();
        JavaType type = lookupType(cpi, Bytecodes.INSTANCEOF);
        ValueNode object = frameState.pop(JavaKind.Object);

        if (!(type instanceof ResolvedJavaType))
        {
            handleUnresolvedInstanceOf(type, object);
            return;
        }
        TypeReference resolvedType = TypeReference.createTrusted(graph.getAssumptions(), (ResolvedJavaType) type);
        JavaTypeProfile profile = getProfileForTypeCheck(resolvedType);

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleInstanceOf(this, object, resolvedType.getType(), profile))
            {
                return;
            }
        }

        LogicNode instanceOfNode = null;
        if (profile != null)
        {
            if (profile.getNullSeen().isFalse())
            {
                object = nullCheckedValue(object);
                ResolvedJavaType singleType = profile.asSingleType();
                if (singleType != null)
                {
                    LogicNode typeCheck = append(createInstanceOf(TypeReference.createExactTrusted(singleType), object, profile));
                    if (!typeCheck.isTautology())
                    {
                        append(new FixedGuardNode(typeCheck, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile));
                    }
                    instanceOfNode = LogicConstantNode.forBoolean(resolvedType.getType().isAssignableFrom(singleType));
                }
            }
        }
        if (instanceOfNode == null)
        {
            instanceOfNode = createInstanceOf(resolvedType, object, null);
        }
        LogicNode logicNode = genUnique(instanceOfNode);

        int next = getStream().nextBCI();
        int value = getStream().readUByte(next);
        if (next <= currentBlock.endBci && (value == Bytecodes.IFEQ || value == Bytecodes.IFNE))
        {
            getStream().next();
            BciBlock firstSucc = currentBlock.getSuccessor(0);
            BciBlock secondSucc = currentBlock.getSuccessor(1);
            if (firstSucc != secondSucc)
            {
                boolean negate = value != Bytecodes.IFNE;
                if (negate)
                {
                    BciBlock tmp = firstSucc;
                    firstSucc = secondSucc;
                    secondSucc = tmp;
                }
                genIf(instanceOfNode, firstSucc, secondSucc, getProfileProbability(negate));
            }
            else
            {
                appendGoto(firstSucc);
            }
        }
        else
        {
            // Most frequent for value is IRETURN, followed by ISTORE.
            frameState.push(JavaKind.Int, append(genConditional(logicNode)));
        }
    }

    protected void genNewInstance(int cpi)
    {
        JavaType type = lookupType(cpi, Bytecodes.NEW);
        genNewInstance(type);
    }

    void genNewInstance(JavaType type)
    {
        if (!(type instanceof ResolvedJavaType))
        {
            handleUnresolvedNewInstance(type);
            return;
        }
        ResolvedJavaType resolvedType = (ResolvedJavaType) type;
        if (!resolvedType.isInitialized())
        {
            handleUnresolvedNewInstance(type);
            return;
        }

        ResolvedJavaType[] skippedExceptionTypes = this.graphBuilderConfig.getSkippedExceptionTypes();
        if (skippedExceptionTypes != null)
        {
            for (ResolvedJavaType exceptionType : skippedExceptionTypes)
            {
                if (exceptionType.isAssignableFrom(resolvedType))
                {
                    append(new DeoptimizeNode(DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.RuntimeConstraint));
                    return;
                }
            }
        }

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleNewInstance(this, resolvedType))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewInstance(resolvedType, true)));
    }

    /**
     * Gets the kind of array elements for the array type code that appears in a
     * {@link Bytecodes#NEWARRAY} bytecode.
     *
     * @param code the array type code
     * @return the kind from the array type code
     */
    private static Class<?> arrayTypeCodeToClass(int code)
    {
        switch (code)
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
                throw new IllegalArgumentException("unknown array type code: " + code);
        }
    }

    private void genNewPrimitiveArray(int typeCode)
    {
        ResolvedJavaType elementType = metaAccess.lookupJavaType(arrayTypeCodeToClass(typeCode));
        ValueNode length = frameState.pop(JavaKind.Int);

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleNewArray(this, elementType, length))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewArray(elementType, length, true)));
    }

    private void genNewObjectArray(int cpi)
    {
        JavaType type = lookupType(cpi, Bytecodes.ANEWARRAY);

        if (!(type instanceof ResolvedJavaType))
        {
            ValueNode length = frameState.pop(JavaKind.Int);
            handleUnresolvedNewObjectArray(type, length);
            return;
        }

        ResolvedJavaType resolvedType = (ResolvedJavaType) type;

        ValueNode length = frameState.pop(JavaKind.Int);
        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleNewArray(this, resolvedType, length))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewArray(resolvedType, length, true)));
    }

    private void genNewMultiArray(int cpi)
    {
        JavaType type = lookupType(cpi, Bytecodes.MULTIANEWARRAY);
        int rank = getStream().readUByte(bci() + 3);
        ValueNode[] dims = new ValueNode[rank];

        if (!(type instanceof ResolvedJavaType))
        {
            for (int i = rank - 1; i >= 0; i--)
            {
                dims[i] = frameState.pop(JavaKind.Int);
            }
            handleUnresolvedNewMultiArray(type, dims);
            return;
        }
        ResolvedJavaType resolvedType = (ResolvedJavaType) type;

        for (int i = rank - 1; i >= 0; i--)
        {
            dims[i] = frameState.pop(JavaKind.Int);
        }

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleNewMultiArray(this, resolvedType, dims))
            {
                return;
            }
        }

        frameState.push(JavaKind.Object, append(createNewMultiArray(resolvedType, dims)));
    }

    protected void genGetField(int cpi, int opcode)
    {
        genGetField(cpi, opcode, frameState.pop(JavaKind.Object));
    }

    protected void genGetField(int cpi, int opcode, ValueNode receiverInput)
    {
        JavaField field = lookupField(cpi, opcode);
        genGetField(field, receiverInput);
    }

    private void genGetField(JavaField field, ValueNode receiverInput)
    {
        ValueNode receiver = emitExplicitExceptions(receiverInput);
        if (field instanceof ResolvedJavaField)
        {
            ResolvedJavaField resolvedField = (ResolvedJavaField) field;
            genGetField(resolvedField, receiver);
        }
        else
        {
            handleUnresolvedLoadField(field, receiver);
        }
    }

    private void genGetField(ResolvedJavaField resolvedField, ValueNode receiver)
    {
        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleLoadField(this, receiver, resolvedField))
            {
                return;
            }
        }

        ValueNode fieldRead = append(genLoadField(receiver, resolvedField));

        if (resolvedField.getDeclaringClass().getName().equals("Ljava/lang/ref/Reference;") && resolvedField.getName().equals("referent"))
        {
            LocationIdentity referentIdentity = new FieldLocationIdentity(resolvedField);
            append(new MembarNode(0, referentIdentity));
        }

        JavaKind fieldKind = resolvedField.getJavaKind();

        if (resolvedField.isVolatile() && fieldRead instanceof LoadFieldNode)
        {
            StateSplitProxyNode readProxy = append(genVolatileFieldReadProxy(fieldRead));
            frameState.push(fieldKind, readProxy);
            readProxy.setStateAfter(frameState.create(stream.nextBCI(), readProxy));
        }
        else
        {
            frameState.push(fieldKind, fieldRead);
        }
    }

    /**
     * @param receiver the receiver of an object based operation
     * @param index the index of an array based operation that is to be tested for out of bounds.
     *            This is null for a non-array operation.
     * @return the receiver value possibly modified to have a non-null stamp
     */
    protected ValueNode emitExplicitExceptions(ValueNode receiver, ValueNode index)
    {
        if (needsExplicitException())
        {
            ValueNode nonNullReceiver = emitExplicitNullCheck(receiver);
            ValueNode length = append(genArrayLength(nonNullReceiver));
            emitExplicitBoundsCheck(index, length);
            return nonNullReceiver;
        }
        return receiver;
    }

    protected ValueNode emitExplicitExceptions(ValueNode receiver)
    {
        if (StampTool.isPointerNonNull(receiver) || !needsExplicitException())
        {
            return receiver;
        }
        else
        {
            return emitExplicitNullCheck(receiver);
        }
    }

    protected boolean needsExplicitException()
    {
        BytecodeExceptionMode exceptionMode = graphBuilderConfig.getBytecodeExceptionMode();
        if (exceptionMode == BytecodeExceptionMode.CheckAll || GraalOptions.StressExplicitExceptionCode.getValue(options))
        {
            return true;
        }
        else if (exceptionMode == BytecodeExceptionMode.Profile && profilingInfo != null)
        {
            return profilingInfo.getExceptionSeen(bci()) == TriState.TRUE;
        }
        return false;
    }

    protected void genPutField(int cpi, int opcode)
    {
        JavaField field = lookupField(cpi, opcode);
        genPutField(field);
    }

    protected void genPutField(JavaField field)
    {
        genPutField(field, frameState.pop(field.getJavaKind()));
    }

    private void genPutField(JavaField field, ValueNode value)
    {
        ValueNode receiver = emitExplicitExceptions(frameState.pop(JavaKind.Object));
        if (field instanceof ResolvedJavaField)
        {
            ResolvedJavaField resolvedField = (ResolvedJavaField) field;

            for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
            {
                if (plugin.handleStoreField(this, receiver, resolvedField, value))
                {
                    return;
                }
            }

            if (resolvedField.isFinal() && method.isConstructor())
            {
                finalBarrierRequired = true;
            }
            genStoreField(receiver, resolvedField, value);
        }
        else
        {
            handleUnresolvedStoreField(field, value, receiver);
        }
    }

    protected void genGetStatic(int cpi, int opcode)
    {
        JavaField field = lookupField(cpi, opcode);
        genGetStatic(field);
    }

    private void genGetStatic(JavaField field)
    {
        ResolvedJavaField resolvedField = resolveStaticFieldAccess(field, null);
        if (resolvedField == null)
        {
            return;
        }

        /*
         * Javac does not allow use of "$assertionsDisabled" for a field name but Eclipse does, in
         * which case a suffix is added to the generated field.
         */
        if ((parsingIntrinsic() || graphBuilderConfig.omitAssertions()) && resolvedField.isSynthetic() && resolvedField.getName().startsWith("$assertionsDisabled"))
        {
            frameState.push(field.getJavaKind(), ConstantNode.forBoolean(true, graph));
            return;
        }

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleLoadStaticField(this, resolvedField))
            {
                return;
            }
        }

        frameState.push(field.getJavaKind(), append(genLoadField(null, resolvedField)));
    }

    private ResolvedJavaField resolveStaticFieldAccess(JavaField field, ValueNode value)
    {
        if (field instanceof ResolvedJavaField)
        {
            ResolvedJavaField resolvedField = (ResolvedJavaField) field;
            if (resolvedField.getDeclaringClass().isInitialized())
            {
                return resolvedField;
            }
            /*
             * Static fields have initialization semantics but may be safely accessed under certain
             * conditions while the class is being initialized. Executing in the clinit or init of
             * classes which are subtypes of the field holder are sure to be running in a context
             * where the access is safe.
             */
            if (resolvedField.getDeclaringClass().isAssignableFrom(method.getDeclaringClass()))
            {
                if (method.isClassInitializer() || method.isConstructor())
                {
                    return resolvedField;
                }
            }
        }
        if (value == null)
        {
            handleUnresolvedLoadField(field, null);
        }
        else
        {
            handleUnresolvedStoreField(field, value, null);
        }
        return null;
    }

    protected void genPutStatic(int cpi, int opcode)
    {
        JavaField field = lookupField(cpi, opcode);
        genPutStatic(field);
    }

    protected void genPutStatic(JavaField field)
    {
        ValueNode value = frameState.pop(field.getJavaKind());
        ResolvedJavaField resolvedField = resolveStaticFieldAccess(field, value);
        if (resolvedField == null)
        {
            return;
        }

        for (NodePlugin plugin : graphBuilderConfig.getPlugins().getNodePlugins())
        {
            if (plugin.handleStoreStaticField(this, resolvedField, value))
            {
                return;
            }
        }

        genStoreField(null, resolvedField, value);
    }

    private double[] switchProbability(int numberOfCases, int bci)
    {
        double[] prob = (profilingInfo == null ? null : profilingInfo.getSwitchProbabilities(bci));
        if (prob == null)
        {
            prob = new double[numberOfCases];
            for (int i = 0; i < numberOfCases; i++)
            {
                prob[i] = 1.0d / numberOfCases;
            }
        }
        return prob;
    }

    private static boolean allPositive(double[] a)
    {
        for (double d : a)
        {
            if (d < 0)
            {
                return false;
            }
        }
        return true;
    }

    static class SuccessorInfo
    {
        final int blockIndex;
        int actualIndex;

        SuccessorInfo(int blockSuccessorIndex)
        {
            this.blockIndex = blockSuccessorIndex;
            actualIndex = -1;
        }
    }

    private void genSwitch(BytecodeSwitch bs)
    {
        int bci = bci();
        ValueNode value = frameState.pop(JavaKind.Int);

        int nofCases = bs.numberOfCases();
        int nofCasesPlusDefault = nofCases + 1;
        double[] keyProbabilities = switchProbability(nofCasesPlusDefault, bci);

        EconomicMap<Integer, SuccessorInfo> bciToBlockSuccessorIndex = EconomicMap.create(Equivalence.DEFAULT);
        for (int i = 0; i < currentBlock.getSuccessorCount(); i++)
        {
            bciToBlockSuccessorIndex.put(currentBlock.getSuccessor(i).startBci, new SuccessorInfo(i));
        }

        ArrayList<BciBlock> actualSuccessors = new ArrayList<>();
        int[] keys = new int[nofCases];
        int[] keySuccessors = new int[nofCasesPlusDefault];
        int deoptSuccessorIndex = -1;
        int nextSuccessorIndex = 0;
        boolean constantValue = value.isConstant();
        for (int i = 0; i < nofCasesPlusDefault; i++)
        {
            if (i < nofCases)
            {
                keys[i] = bs.keyAt(i);
            }

            if (!constantValue && isNeverExecutedCode(keyProbabilities[i]))
            {
                if (deoptSuccessorIndex < 0)
                {
                    deoptSuccessorIndex = nextSuccessorIndex++;
                    actualSuccessors.add(null);
                }
                keySuccessors[i] = deoptSuccessorIndex;
            }
            else
            {
                int targetBci = i < nofCases ? bs.targetAt(i) : bs.defaultTarget();
                SuccessorInfo info = bciToBlockSuccessorIndex.get(targetBci);
                if (info.actualIndex < 0)
                {
                    info.actualIndex = nextSuccessorIndex++;
                    actualSuccessors.add(currentBlock.getSuccessor(info.blockIndex));
                }
                keySuccessors[i] = info.actualIndex;
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
        if (deoptSuccessorIndex >= 0)
        {
            int[] connectedCases = new int[nextSuccessorIndex];
            for (int i = 0; i < nofCasesPlusDefault; i++)
            {
                connectedCases[keySuccessors[i]]++;
            }

            for (int i = 0; i < nofCasesPlusDefault; i++)
            {
                if (keySuccessors[i] == deoptSuccessorIndex)
                {
                    int targetBci = i < nofCases ? bs.targetAt(i) : bs.defaultTarget();
                    SuccessorInfo info = bciToBlockSuccessorIndex.get(targetBci);
                    int rewiredIndex = info.actualIndex;
                    if (rewiredIndex >= 0 && connectedCases[rewiredIndex] > 1)
                    {
                        keySuccessors[i] = info.actualIndex;
                    }
                }
            }
        }

        genIntegerSwitch(value, actualSuccessors, keys, keyProbabilities, keySuccessors);
    }

    protected boolean isNeverExecutedCode(double probability)
    {
        return probability == 0 && optimisticOpts.removeNeverExecutedCode(getOptions());
    }

    private double clampProbability(double probability)
    {
        if (!optimisticOpts.removeNeverExecutedCode(getOptions()))
        {
            if (probability == 0)
            {
                return 0.0000001;
            }
            else if (probability == 1)
            {
                return 0.999999;
            }
        }
        return probability;
    }

    private boolean assertAtIfBytecode()
    {
        int bytecode = stream.currentBC();
        switch (bytecode)
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

    public final void processBytecode(int bci, int opcode)
    {
        int cpi;

        switch (opcode)
        {
            case Bytecodes.NOP:             /* nothing to do */ break;
            case Bytecodes.ACONST_NULL:     frameState.push(JavaKind.Object, appendConstant(JavaConstant.NULL_POINTER)); break;
            case Bytecodes.ICONST_M1:       // fall through
            case Bytecodes.ICONST_0:        // fall through
            case Bytecodes.ICONST_1:        // fall through
            case Bytecodes.ICONST_2:        // fall through
            case Bytecodes.ICONST_3:        // fall through
            case Bytecodes.ICONST_4:        // fall through
            case Bytecodes.ICONST_5:        frameState.push(JavaKind.Int, appendConstant(JavaConstant.forInt(opcode - Bytecodes.ICONST_0))); break;
            case Bytecodes.LCONST_0:        // fall through
            case Bytecodes.LCONST_1:        frameState.push(JavaKind.Long, appendConstant(JavaConstant.forLong(opcode - Bytecodes.LCONST_0))); break;
            case Bytecodes.FCONST_0:        // fall through
            case Bytecodes.FCONST_1:        // fall through
            case Bytecodes.FCONST_2:        frameState.push(JavaKind.Float, appendConstant(JavaConstant.forFloat(opcode - Bytecodes.FCONST_0))); break;
            case Bytecodes.DCONST_0:        // fall through
            case Bytecodes.DCONST_1:        frameState.push(JavaKind.Double, appendConstant(JavaConstant.forDouble(opcode - Bytecodes.DCONST_0))); break;
            case Bytecodes.BIPUSH:          frameState.push(JavaKind.Int, appendConstant(JavaConstant.forInt(stream.readByte()))); break;
            case Bytecodes.SIPUSH:          frameState.push(JavaKind.Int, appendConstant(JavaConstant.forInt(stream.readShort()))); break;
            case Bytecodes.LDC:             // fall through
            case Bytecodes.LDC_W:           // fall through
            case Bytecodes.LDC2_W:          genLoadConstant(stream.readCPI(), opcode); break;
            case Bytecodes.ILOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Int); break;
            case Bytecodes.LLOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Long); break;
            case Bytecodes.FLOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Float); break;
            case Bytecodes.DLOAD:           loadLocal(stream.readLocalIndex(), JavaKind.Double); break;
            case Bytecodes.ALOAD:           loadLocalObject(stream.readLocalIndex()); break;
            case Bytecodes.ILOAD_0:         // fall through
            case Bytecodes.ILOAD_1:         // fall through
            case Bytecodes.ILOAD_2:         // fall through
            case Bytecodes.ILOAD_3:         loadLocal(opcode - Bytecodes.ILOAD_0, JavaKind.Int); break;
            case Bytecodes.LLOAD_0:         // fall through
            case Bytecodes.LLOAD_1:         // fall through
            case Bytecodes.LLOAD_2:         // fall through
            case Bytecodes.LLOAD_3:         loadLocal(opcode - Bytecodes.LLOAD_0, JavaKind.Long); break;
            case Bytecodes.FLOAD_0:         // fall through
            case Bytecodes.FLOAD_1:         // fall through
            case Bytecodes.FLOAD_2:         // fall through
            case Bytecodes.FLOAD_3:         loadLocal(opcode - Bytecodes.FLOAD_0, JavaKind.Float); break;
            case Bytecodes.DLOAD_0:         // fall through
            case Bytecodes.DLOAD_1:         // fall through
            case Bytecodes.DLOAD_2:         // fall through
            case Bytecodes.DLOAD_3:         loadLocal(opcode - Bytecodes.DLOAD_0, JavaKind.Double); break;
            case Bytecodes.ALOAD_0:         // fall through
            case Bytecodes.ALOAD_1:         // fall through
            case Bytecodes.ALOAD_2:         // fall through
            case Bytecodes.ALOAD_3:         loadLocalObject(opcode - Bytecodes.ALOAD_0); break;
            case Bytecodes.IALOAD:          genLoadIndexed(JavaKind.Int   ); break;
            case Bytecodes.LALOAD:          genLoadIndexed(JavaKind.Long  ); break;
            case Bytecodes.FALOAD:          genLoadIndexed(JavaKind.Float ); break;
            case Bytecodes.DALOAD:          genLoadIndexed(JavaKind.Double); break;
            case Bytecodes.AALOAD:          genLoadIndexed(JavaKind.Object); break;
            case Bytecodes.BALOAD:          genLoadIndexed(JavaKind.Byte  ); break;
            case Bytecodes.CALOAD:          genLoadIndexed(JavaKind.Char  ); break;
            case Bytecodes.SALOAD:          genLoadIndexed(JavaKind.Short ); break;
            case Bytecodes.ISTORE:          storeLocal(JavaKind.Int, stream.readLocalIndex()); break;
            case Bytecodes.LSTORE:          storeLocal(JavaKind.Long, stream.readLocalIndex()); break;
            case Bytecodes.FSTORE:          storeLocal(JavaKind.Float, stream.readLocalIndex()); break;
            case Bytecodes.DSTORE:          storeLocal(JavaKind.Double, stream.readLocalIndex()); break;
            case Bytecodes.ASTORE:          storeLocal(JavaKind.Object, stream.readLocalIndex()); break;
            case Bytecodes.ISTORE_0:        // fall through
            case Bytecodes.ISTORE_1:        // fall through
            case Bytecodes.ISTORE_2:        // fall through
            case Bytecodes.ISTORE_3:        storeLocal(JavaKind.Int, opcode - Bytecodes.ISTORE_0); break;
            case Bytecodes.LSTORE_0:        // fall through
            case Bytecodes.LSTORE_1:        // fall through
            case Bytecodes.LSTORE_2:        // fall through
            case Bytecodes.LSTORE_3:        storeLocal(JavaKind.Long, opcode - Bytecodes.LSTORE_0); break;
            case Bytecodes.FSTORE_0:        // fall through
            case Bytecodes.FSTORE_1:        // fall through
            case Bytecodes.FSTORE_2:        // fall through
            case Bytecodes.FSTORE_3:        storeLocal(JavaKind.Float, opcode - Bytecodes.FSTORE_0); break;
            case Bytecodes.DSTORE_0:        // fall through
            case Bytecodes.DSTORE_1:        // fall through
            case Bytecodes.DSTORE_2:        // fall through
            case Bytecodes.DSTORE_3:        storeLocal(JavaKind.Double, opcode - Bytecodes.DSTORE_0); break;
            case Bytecodes.ASTORE_0:        // fall through
            case Bytecodes.ASTORE_1:        // fall through
            case Bytecodes.ASTORE_2:        // fall through
            case Bytecodes.ASTORE_3:        storeLocal(JavaKind.Object, opcode - Bytecodes.ASTORE_0); break;
            case Bytecodes.IASTORE:         genStoreIndexed(JavaKind.Int   ); break;
            case Bytecodes.LASTORE:         genStoreIndexed(JavaKind.Long  ); break;
            case Bytecodes.FASTORE:         genStoreIndexed(JavaKind.Float ); break;
            case Bytecodes.DASTORE:         genStoreIndexed(JavaKind.Double); break;
            case Bytecodes.AASTORE:         genStoreIndexed(JavaKind.Object); break;
            case Bytecodes.BASTORE:         genStoreIndexed(JavaKind.Byte  ); break;
            case Bytecodes.CASTORE:         genStoreIndexed(JavaKind.Char  ); break;
            case Bytecodes.SASTORE:         genStoreIndexed(JavaKind.Short ); break;
            case Bytecodes.POP:             // fall through
            case Bytecodes.POP2:            // fall through
            case Bytecodes.DUP:             // fall through
            case Bytecodes.DUP_X1:          // fall through
            case Bytecodes.DUP_X2:          // fall through
            case Bytecodes.DUP2:            // fall through
            case Bytecodes.DUP2_X1:         // fall through
            case Bytecodes.DUP2_X2:         // fall through
            case Bytecodes.SWAP:            frameState.stackOp(opcode); break;
            case Bytecodes.IADD:            // fall through
            case Bytecodes.ISUB:            // fall through
            case Bytecodes.IMUL:            genArithmeticOp(JavaKind.Int, opcode); break;
            case Bytecodes.IDIV:            // fall through
            case Bytecodes.IREM:            genIntegerDivOp(JavaKind.Int, opcode); break;
            case Bytecodes.LADD:            // fall through
            case Bytecodes.LSUB:            // fall through
            case Bytecodes.LMUL:            genArithmeticOp(JavaKind.Long, opcode); break;
            case Bytecodes.LDIV:            // fall through
            case Bytecodes.LREM:            genIntegerDivOp(JavaKind.Long, opcode); break;
            case Bytecodes.FADD:            // fall through
            case Bytecodes.FSUB:            // fall through
            case Bytecodes.FMUL:            // fall through
            case Bytecodes.FDIV:            // fall through
            case Bytecodes.FREM:            genArithmeticOp(JavaKind.Float, opcode); break;
            case Bytecodes.DADD:            // fall through
            case Bytecodes.DSUB:            // fall through
            case Bytecodes.DMUL:            // fall through
            case Bytecodes.DDIV:            // fall through
            case Bytecodes.DREM:            genArithmeticOp(JavaKind.Double, opcode); break;
            case Bytecodes.INEG:            genNegateOp(JavaKind.Int); break;
            case Bytecodes.LNEG:            genNegateOp(JavaKind.Long); break;
            case Bytecodes.FNEG:            genNegateOp(JavaKind.Float); break;
            case Bytecodes.DNEG:            genNegateOp(JavaKind.Double); break;
            case Bytecodes.ISHL:            // fall through
            case Bytecodes.ISHR:            // fall through
            case Bytecodes.IUSHR:           genShiftOp(JavaKind.Int, opcode); break;
            case Bytecodes.IAND:            // fall through
            case Bytecodes.IOR:             // fall through
            case Bytecodes.IXOR:            genLogicOp(JavaKind.Int, opcode); break;
            case Bytecodes.LSHL:            // fall through
            case Bytecodes.LSHR:            // fall through
            case Bytecodes.LUSHR:           genShiftOp(JavaKind.Long, opcode); break;
            case Bytecodes.LAND:            // fall through
            case Bytecodes.LOR:             // fall through
            case Bytecodes.LXOR:            genLogicOp(JavaKind.Long, opcode); break;
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
            case Bytecodes.GETSTATIC:       cpi = stream.readCPI(); genGetStatic(cpi, opcode); break;
            case Bytecodes.PUTSTATIC:       cpi = stream.readCPI(); genPutStatic(cpi, opcode); break;
            case Bytecodes.GETFIELD:        cpi = stream.readCPI(); genGetField(cpi, opcode); break;
            case Bytecodes.PUTFIELD:        cpi = stream.readCPI(); genPutField(cpi, opcode); break;
            case Bytecodes.INVOKEVIRTUAL:   cpi = stream.readCPI(); genInvokeVirtual(cpi, opcode); break;
            case Bytecodes.INVOKESPECIAL:   cpi = stream.readCPI(); genInvokeSpecial(cpi, opcode); break;
            case Bytecodes.INVOKESTATIC:    cpi = stream.readCPI(); genInvokeStatic(cpi, opcode); break;
            case Bytecodes.INVOKEINTERFACE: cpi = stream.readCPI(); genInvokeInterface(cpi, opcode); break;
            case Bytecodes.INVOKEDYNAMIC:   cpi = stream.readCPI4(); genInvokeDynamic(cpi, opcode); break;
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
            default:              throw new PermanentBailoutException("Unsupported opcode %d (%s) [bci=%d]", opcode, Bytecodes.nameOf(opcode), bci);
        }
    }

    private void genArrayLength()
    {
        ValueNode array = emitExplicitExceptions(frameState.pop(JavaKind.Object));
        frameState.push(JavaKind.Int, append(genArrayLength(array)));
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
        BytecodeParser ancestor = parent;
        while (ancestor != null && ancestor.parsingIntrinsic())
        {
            ancestor = ancestor.parent;
        }
        return ancestor;
    }

    static String nSpaces(int n)
    {
        return n == 0 ? "" : String.format("%" + n + "s", "");
    }
}
