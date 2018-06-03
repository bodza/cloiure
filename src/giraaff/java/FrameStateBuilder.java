package giraaff.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

import giraaff.bytecode.Bytecode;
import giraaff.bytecode.Bytecodes;
import giraaff.bytecode.ResolvedJavaMethodBytecode;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.java.BciBlockMapping.BciBlock;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.graphbuilderconf.GraphBuilderTool;
import giraaff.nodes.graphbuilderconf.IntrinsicContext.SideEffectsState;
import giraaff.nodes.graphbuilderconf.ParameterPlugin;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

// @class FrameStateBuilder
public final class FrameStateBuilder implements SideEffectsState
{
    // @def
    private static final ValueNode[] EMPTY_ARRAY = new ValueNode[0];
    // @def
    private static final MonitorIdNode[] EMPTY_MONITOR_ARRAY = new MonitorIdNode[0];

    // @field
    private final BytecodeParser parser;
    // @field
    private final GraphBuilderTool tool;
    // @field
    private final Bytecode code;
    // @field
    private int stackSize;
    // @field
    protected final ValueNode[] locals;
    // @field
    protected final ValueNode[] stack;
    // @field
    private ValueNode[] lockedObjects;

    /**
     * @see BytecodeFrame#rethrowException
     */
    // @field
    private boolean rethrowException;

    // @field
    private MonitorIdNode[] monitorIds;
    // @field
    private final StructuredGraph graph;
    // @field
    private final boolean clearNonLiveLocals;
    // @field
    private FrameState outerFrameState;

    /**
     * The closest {@link StateSplit#hasSideEffect() side-effect} predecessors. There will be more
     * than one when the current block contains no side-effects but merging predecessor blocks do.
     */
    // @field
    private List<StateSplit> sideEffects;

    /**
     * Creates a new frame state builder for the given method and the given target graph.
     *
     * @param method the method whose frame is simulated
     * @param graph the target graph of Graal nodes created by the builder
     */
    // @cons
    public FrameStateBuilder(GraphBuilderTool __tool, ResolvedJavaMethod __method, StructuredGraph __graph)
    {
        this(__tool, new ResolvedJavaMethodBytecode(__method), __graph);
    }

    /**
     * Creates a new frame state builder for the given code attribute, method and the given target graph.
     *
     * @param code the bytecode in which the frame exists
     * @param graph the target graph of Graal nodes created by the builder
     */
    // @cons
    public FrameStateBuilder(GraphBuilderTool __tool, Bytecode __code, StructuredGraph __graph)
    {
        super();
        this.tool = __tool;
        if (__tool instanceof BytecodeParser)
        {
            this.parser = (BytecodeParser) __tool;
        }
        else
        {
            this.parser = null;
        }
        this.code = __code;
        this.locals = allocateArray(__code.getMaxLocals());
        this.stack = allocateArray(Math.max(1, __code.getMaxStackSize()));
        this.lockedObjects = allocateArray(0);

        this.monitorIds = EMPTY_MONITOR_ARRAY;
        this.graph = __graph;
        this.clearNonLiveLocals = GraalOptions.optClearNonLiveLocals;
    }

    public void initializeFromArgumentsArray(ValueNode[] __arguments)
    {
        int __javaIndex = 0;
        int __index = 0;
        if (!getMethod().isStatic())
        {
            // set the receiver
            locals[__javaIndex] = __arguments[__index];
            __javaIndex = 1;
            __index = 1;
        }
        Signature __sig = getMethod().getSignature();
        int __max = __sig.getParameterCount(false);
        for (int __i = 0; __i < __max; __i++)
        {
            JavaKind __kind = __sig.getParameterKind(__i);
            locals[__javaIndex] = __arguments[__index];
            __javaIndex++;
            if (__kind.needsTwoSlots())
            {
                locals[__javaIndex] = FrameState.TWO_SLOT_MARKER;
                __javaIndex++;
            }
            __index++;
        }
    }

    public void initializeForMethodStart(Assumptions __assumptions, boolean __eagerResolve, Plugins __plugins)
    {
        int __javaIndex = 0;
        int __index = 0;
        ResolvedJavaMethod __method = getMethod();
        ResolvedJavaType __originalType = __method.getDeclaringClass();
        if (!__method.isStatic())
        {
            // add the receiver
            FloatingNode __receiver = null;
            StampPair __receiverStamp = null;
            if (__plugins != null)
            {
                __receiverStamp = __plugins.getOverridingStamp(tool, __originalType, true);
            }
            if (__receiverStamp == null)
            {
                __receiverStamp = StampFactory.forDeclaredType(__assumptions, __originalType, true);
            }

            if (__plugins != null)
            {
                for (ParameterPlugin __plugin : __plugins.getParameterPlugins())
                {
                    __receiver = __plugin.interceptParameter(tool, __index, __receiverStamp);
                    if (__receiver != null)
                    {
                        break;
                    }
                }
            }
            if (__receiver == null)
            {
                __receiver = new ParameterNode(__javaIndex, __receiverStamp);
            }

            locals[__javaIndex] = graph.addOrUniqueWithInputs(__receiver);
            __javaIndex = 1;
            __index = 1;
        }
        Signature __sig = __method.getSignature();
        int __max = __sig.getParameterCount(false);
        ResolvedJavaType __accessingClass = __originalType;
        for (int __i = 0; __i < __max; __i++)
        {
            JavaType __type = __sig.getParameterType(__i, __accessingClass);
            if (__eagerResolve)
            {
                __type = __type.resolve(__accessingClass);
            }
            JavaKind __kind = __type.getJavaKind();
            StampPair __stamp = null;
            if (__plugins != null)
            {
                __stamp = __plugins.getOverridingStamp(tool, __type, false);
            }
            if (__stamp == null)
            {
                __stamp = StampFactory.forDeclaredType(__assumptions, __type, false);
            }

            FloatingNode __param = null;
            if (__plugins != null)
            {
                for (ParameterPlugin __plugin : __plugins.getParameterPlugins())
                {
                    __param = __plugin.interceptParameter(tool, __index, __stamp);
                    if (__param != null)
                    {
                        break;
                    }
                }
            }
            if (__param == null)
            {
                __param = new ParameterNode(__index, __stamp);
            }

            locals[__javaIndex] = graph.addOrUniqueWithInputs(__param);
            __javaIndex++;
            if (__kind.needsTwoSlots())
            {
                locals[__javaIndex] = FrameState.TWO_SLOT_MARKER;
                __javaIndex++;
            }
            __index++;
        }
    }

    // @cons
    private FrameStateBuilder(FrameStateBuilder __other)
    {
        super();
        this.parser = __other.parser;
        this.tool = __other.tool;
        this.code = __other.code;
        this.stackSize = __other.stackSize;
        this.locals = __other.locals.clone();
        this.stack = __other.stack.clone();
        this.lockedObjects = __other.lockedObjects.length == 0 ? __other.lockedObjects : __other.lockedObjects.clone();
        this.rethrowException = __other.rethrowException;

        graph = __other.graph;
        clearNonLiveLocals = __other.clearNonLiveLocals;
        monitorIds = __other.monitorIds.length == 0 ? __other.monitorIds : __other.monitorIds.clone();
    }

    private static ValueNode[] allocateArray(int __length)
    {
        return __length == 0 ? EMPTY_ARRAY : new ValueNode[__length];
    }

    public ResolvedJavaMethod getMethod()
    {
        return code.getMethod();
    }

    public FrameState create(int __bci, StateSplit __forStateSplit)
    {
        if (parser != null && parser.parsingIntrinsic())
        {
            return parser.intrinsicContext.createFrameState(parser.getGraph(), this, __forStateSplit);
        }

        // skip intrinsic frames
        return create(__bci, parser != null ? parser.getNonIntrinsicAncestor() : null, false, null, null);
    }

    /**
     * @param pushedValues if non-null, values to {@link #push(JavaKind, ValueNode)} to the stack
     *            before creating the {@link FrameState}
     */
    public FrameState create(int __bci, BytecodeParser __parent, boolean __duringCall, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        if (outerFrameState == null && __parent != null)
        {
            outerFrameState = __parent.getFrameStateBuilder().create(__parent.bci(), __parent.getNonIntrinsicAncestor(), true, null, null);
        }
        if (__bci == BytecodeFrame.AFTER_EXCEPTION_BCI && __parent != null)
        {
            return outerFrameState.duplicateModified(outerFrameState.bci, true, false, JavaKind.Void, new JavaKind[] { JavaKind.Object }, new ValueNode[] { stack[0] });
        }
        if (__bci == BytecodeFrame.INVALID_FRAMESTATE_BCI)
        {
            throw GraalError.shouldNotReachHere();
        }

        if (__pushedValues != null)
        {
            int __stackSizeToRestore = stackSize;
            for (int __i = 0; __i < __pushedValues.length; __i++)
            {
                push(__pushedSlotKinds[__i], __pushedValues[__i]);
            }
            FrameState __res = graph.add(new FrameState(outerFrameState, code, __bci, locals, stack, stackSize, lockedObjects, Arrays.asList(monitorIds), rethrowException, __duringCall));
            stackSize = __stackSizeToRestore;
            return __res;
        }
        else
        {
            if (__bci == BytecodeFrame.AFTER_EXCEPTION_BCI)
            {
                clearLocals();
            }
            return graph.add(new FrameState(outerFrameState, code, __bci, locals, stack, stackSize, lockedObjects, Arrays.asList(monitorIds), rethrowException, __duringCall));
        }
    }

    public FrameStateBuilder copy()
    {
        return new FrameStateBuilder(this);
    }

    public boolean isCompatibleWith(FrameStateBuilder __other)
    {
        if (stackSize() != __other.stackSize())
        {
            return false;
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            ValueNode __x = stack[__i];
            ValueNode __y = __other.stack[__i];
            if (__x != __y && (__x == FrameState.TWO_SLOT_MARKER || __x.isDeleted() || __y == FrameState.TWO_SLOT_MARKER || __y.isDeleted() || __x.getStackKind() != __y.getStackKind()))
            {
                return false;
            }
        }
        if (lockedObjects.length != __other.lockedObjects.length)
        {
            return false;
        }
        for (int __i = 0; __i < lockedObjects.length; __i++)
        {
            if (GraphUtil.originalValue(lockedObjects[__i]) != GraphUtil.originalValue(__other.lockedObjects[__i]) || monitorIds[__i] != __other.monitorIds[__i])
            {
                throw new BailoutException("unbalanced monitors");
            }
        }
        return true;
    }

    public void merge(AbstractMergeNode __block, FrameStateBuilder __other)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            locals[__i] = merge(locals[__i], __other.locals[__i], __block);
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            stack[__i] = merge(stack[__i], __other.stack[__i], __block);
        }
        for (int __i = 0; __i < lockedObjects.length; __i++)
        {
            lockedObjects[__i] = merge(lockedObjects[__i], __other.lockedObjects[__i], __block);
        }

        if (sideEffects == null)
        {
            sideEffects = __other.sideEffects;
        }
        else
        {
            if (__other.sideEffects != null)
            {
                sideEffects.addAll(__other.sideEffects);
            }
        }
    }

    private ValueNode merge(ValueNode __currentValue, ValueNode __otherValue, AbstractMergeNode __block)
    {
        if (__currentValue == null || __currentValue.isDeleted())
        {
            return null;
        }
        else if (__block.isPhiAtMerge(__currentValue))
        {
            if (__otherValue == null || __otherValue == FrameState.TWO_SLOT_MARKER || __otherValue.isDeleted() || __currentValue.getStackKind() != __otherValue.getStackKind())
            {
                // This phi must be dead anyway, add input of correct stack kind to keep the graph invariants.
                ((PhiNode) __currentValue).addInput(ConstantNode.defaultForKind(__currentValue.getStackKind(), graph));
            }
            else
            {
                ((PhiNode) __currentValue).addInput(__otherValue);
            }
            return __currentValue;
        }
        else if (__currentValue != __otherValue)
        {
            if (__currentValue == FrameState.TWO_SLOT_MARKER || __otherValue == FrameState.TWO_SLOT_MARKER)
            {
                return null;
            }
            else if (__otherValue == null || __otherValue.isDeleted() || __currentValue.getStackKind() != __otherValue.getStackKind())
            {
                return null;
            }
            return createValuePhi(__currentValue, __otherValue, __block);
        }
        else
        {
            return __currentValue;
        }
    }

    private ValuePhiNode createValuePhi(ValueNode __currentValue, ValueNode __otherValue, AbstractMergeNode __block)
    {
        ValuePhiNode __phi = graph.addWithoutUnique(new ValuePhiNode(__currentValue.stamp(NodeView.DEFAULT).unrestricted(), __block));
        for (int __i = 0; __i < __block.phiPredecessorCount(); __i++)
        {
            __phi.addInput(__currentValue);
        }
        __phi.addInput(__otherValue);
        return __phi;
    }

    public void inferPhiStamps(AbstractMergeNode __block)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            inferPhiStamp(__block, locals[__i]);
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            inferPhiStamp(__block, stack[__i]);
        }
        for (int __i = 0; __i < lockedObjects.length; __i++)
        {
            inferPhiStamp(__block, lockedObjects[__i]);
        }
    }

    private static void inferPhiStamp(AbstractMergeNode __block, ValueNode __node)
    {
        if (__block.isPhiAtMerge(__node))
        {
            __node.inferStamp();
        }
    }

    public void insertLoopPhis(LocalLiveness __liveness, int __loopId, LoopBeginNode __loopBegin, boolean __forcePhis, boolean __stampFromValueForForcedPhis)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            boolean __changedInLoop = __liveness.localIsChangedInLoop(__loopId, __i);
            if (__forcePhis || __changedInLoop)
            {
                locals[__i] = createLoopPhi(__loopBegin, locals[__i], __stampFromValueForForcedPhis && !__changedInLoop);
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            stack[__i] = createLoopPhi(__loopBegin, stack[__i], false);
        }
        for (int __i = 0; __i < lockedObjects.length; __i++)
        {
            lockedObjects[__i] = createLoopPhi(__loopBegin, lockedObjects[__i], false);
        }
    }

    public void insertLoopProxies(LoopExitNode __loopExit, FrameStateBuilder __loopEntryState)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            ValueNode __value = locals[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER && (!__loopEntryState.contains(__value) || __loopExit.loopBegin().isPhiAtMerge(__value)))
            {
                locals[__i] = ProxyNode.forValue(__value, __loopExit, graph);
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            ValueNode __value = stack[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER && (!__loopEntryState.contains(__value) || __loopExit.loopBegin().isPhiAtMerge(__value)))
            {
                stack[__i] = ProxyNode.forValue(__value, __loopExit, graph);
            }
        }
        for (int __i = 0; __i < lockedObjects.length; __i++)
        {
            ValueNode __value = lockedObjects[__i];
            if (__value != null && (!__loopEntryState.contains(__value) || __loopExit.loopBegin().isPhiAtMerge(__value)))
            {
                lockedObjects[__i] = ProxyNode.forValue(__value, __loopExit, graph);
            }
        }
    }

    public void insertProxies(Function<ValueNode, ValueNode> __proxyFunction)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            ValueNode __value = locals[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER)
            {
                locals[__i] = __proxyFunction.apply(__value);
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            ValueNode __value = stack[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER)
            {
                stack[__i] = __proxyFunction.apply(__value);
            }
        }
        for (int __i = 0; __i < lockedObjects.length; __i++)
        {
            ValueNode __value = lockedObjects[__i];
            if (__value != null)
            {
                lockedObjects[__i] = __proxyFunction.apply(__value);
            }
        }
    }

    private ValueNode createLoopPhi(AbstractMergeNode __block, ValueNode __value, boolean __stampFromValue)
    {
        if (__value == null || __value == FrameState.TWO_SLOT_MARKER)
        {
            return __value;
        }

        ValuePhiNode __phi = graph.addWithoutUnique(new ValuePhiNode(__stampFromValue ? __value.stamp(NodeView.DEFAULT) : __value.stamp(NodeView.DEFAULT).unrestricted(), __block));
        __phi.addInput(__value);
        return __phi;
    }

    /**
     * Adds a locked monitor to this frame state.
     *
     * @param object the object whose monitor will be locked.
     */
    public void pushLock(ValueNode __object, MonitorIdNode __monitorId)
    {
        lockedObjects = Arrays.copyOf(lockedObjects, lockedObjects.length + 1);
        monitorIds = Arrays.copyOf(monitorIds, monitorIds.length + 1);
        lockedObjects[lockedObjects.length - 1] = __object;
        monitorIds[monitorIds.length - 1] = __monitorId;
    }

    /**
     * Removes a locked monitor from this frame state.
     *
     * @return the object whose monitor was removed from the locks list.
     */
    public ValueNode popLock()
    {
        try
        {
            return lockedObjects[lockedObjects.length - 1];
        }
        finally
        {
            lockedObjects = lockedObjects.length == 1 ? EMPTY_ARRAY : Arrays.copyOf(lockedObjects, lockedObjects.length - 1);
            monitorIds = monitorIds.length == 1 ? EMPTY_MONITOR_ARRAY : Arrays.copyOf(monitorIds, monitorIds.length - 1);
        }
    }

    public MonitorIdNode peekMonitorId()
    {
        return monitorIds[monitorIds.length - 1];
    }

    /**
     * @return the current lock depth
     */
    public int lockDepth(boolean __includeParents)
    {
        int __depth = lockedObjects.length;
        if (__includeParents && parser.getParent() != null)
        {
            __depth += parser.getParent().frameState.lockDepth(true);
        }
        return __depth;
    }

    public boolean contains(ValueNode __value)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            if (locals[__i] == __value)
            {
                return true;
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            if (stack[__i] == __value)
            {
                return true;
            }
        }
        for (int __i = 0; __i < lockedObjects.length; __i++)
        {
            if (lockedObjects[__i] == __value || monitorIds[__i] == __value)
            {
                return true;
            }
        }
        return false;
    }

    public void clearNonLiveLocals(BciBlock __block, LocalLiveness __liveness, boolean __liveIn)
    {
        /*
         * (lstadler) if somebody is tempted to remove/disable this clearing code: it's possible to
         * remove it for normal compilations, but not for OSR compilations - otherwise dead object
         * slots at the OSR entry aren't cleared. it is also not enough to rely on PiNodes with
         * Kind.Illegal, because the conflicting branch might not have been parsed.
         */
        if (!clearNonLiveLocals)
        {
            return;
        }
        if (__liveIn)
        {
            for (int __i = 0; __i < locals.length; __i++)
            {
                if (!__liveness.localIsLiveIn(__block, __i))
                {
                    locals[__i] = null;
                }
            }
        }
        else
        {
            for (int __i = 0; __i < locals.length; __i++)
            {
                if (!__liveness.localIsLiveOut(__block, __i))
                {
                    locals[__i] = null;
                }
            }
        }
    }

    /**
     * Clears all local variables.
     */
    public void clearLocals()
    {
        for (int __i = 0; __i < locals.length; __i++)
        {
            locals[__i] = null;
        }
    }

    /**
     * @see BytecodeFrame#rethrowException
     */
    public boolean rethrowException()
    {
        return rethrowException;
    }

    /**
     * @see BytecodeFrame#rethrowException
     */
    public void setRethrowException(boolean __b)
    {
        rethrowException = __b;
    }

    /**
     * Returns the size of the local variables.
     *
     * @return the size of the local variables
     */
    public int localsSize()
    {
        return locals.length;
    }

    /**
     * Gets the current size (height) of the stack.
     */
    public int stackSize()
    {
        return stackSize;
    }

    /**
     * Loads the local variable at the specified index, checking that the returned value is non-null
     * and that two-stack values are properly handled.
     *
     * @param i the index of the local variable to load
     * @param slotKind the kind of the local variable from the point of view of the bytecodes
     * @return the instruction that produced the specified local
     */
    public ValueNode loadLocal(int __i, JavaKind __slotKind)
    {
        return locals[__i];
    }

    /**
     * Stores a given local variable at the specified index. If the value occupies two slots, then
     * the next local variable index is also overwritten.
     *
     * @param i the index at which to store
     * @param slotKind the kind of the local variable from the point of view of the bytecodes
     * @param x the instruction which produces the value for the local
     */
    public void storeLocal(int __i, JavaKind __slotKind, ValueNode __x)
    {
        if (locals[__i] == FrameState.TWO_SLOT_MARKER)
        {
            // Writing the second slot of a two-slot value invalidates the first slot.
            locals[__i - 1] = null;
        }
        locals[__i] = __x;
        if (__slotKind.needsTwoSlots())
        {
            // Writing a two-slot value: mark the second slot.
            locals[__i + 1] = FrameState.TWO_SLOT_MARKER;
        }
        else if (__i < locals.length - 1 && locals[__i + 1] == FrameState.TWO_SLOT_MARKER)
        {
            // Writing a one-slot value to an index previously occupied by a two-slot value: clear the old marker of the second slot.
            locals[__i + 1] = null;
        }
    }

    /**
     * Pushes an instruction onto the stack with the expected type.
     *
     * @param slotKind the kind of the stack element from the point of view of the bytecodes
     * @param x the instruction to push onto the stack
     */
    public void push(JavaKind __slotKind, ValueNode __x)
    {
        xpush(__x);
        if (__slotKind.needsTwoSlots())
        {
            xpush(FrameState.TWO_SLOT_MARKER);
        }
    }

    public void pushReturn(JavaKind __slotKind, ValueNode __x)
    {
        if (__slotKind != JavaKind.Void)
        {
            push(__slotKind, __x);
        }
    }

    /**
     * Pops an instruction off the stack with the expected type.
     *
     * @param slotKind the kind of the stack element from the point of view of the bytecodes
     * @return the instruction on the top of the stack
     */
    public ValueNode pop(JavaKind __slotKind)
    {
        if (__slotKind.needsTwoSlots())
        {
            ValueNode __s = xpop();
        }
        return xpop();
    }

    private void xpush(ValueNode __x)
    {
        stack[stackSize++] = __x;
    }

    private ValueNode xpop()
    {
        return stack[--stackSize];
    }

    private ValueNode xpeek()
    {
        return stack[stackSize - 1];
    }

    /**
     * Pop the specified number of slots off of this stack and return them as an array of instructions.
     *
     * @return an array containing the arguments off of the stack
     */
    public ValueNode[] popArguments(int __argSize)
    {
        ValueNode[] __result = allocateArray(__argSize);
        for (int __i = __argSize - 1; __i >= 0; __i--)
        {
            ValueNode __x = xpop();
            if (__x == FrameState.TWO_SLOT_MARKER)
            {
                // Ignore second slot of two-slot value.
                __x = xpop();
            }
            __result[__i] = __x;
        }
        return __result;
    }

    /**
     * Clears all values on this stack.
     */
    public void clearStack()
    {
        stackSize = 0;
    }

    /**
     * Performs a raw stack operation as defined in the Java bytecode specification.
     *
     * @param opcode The Java bytecode.
     */
    public void stackOp(int __opcode)
    {
        switch (__opcode)
        {
            case Bytecodes.POP:
            {
                ValueNode __w1 = xpop();
                break;
            }
            case Bytecodes.POP2:
            {
                xpop();
                ValueNode __w2 = xpop();
                break;
            }
            case Bytecodes.DUP:
            {
                ValueNode __w1 = xpeek();
                xpush(__w1);
                break;
            }
            case Bytecodes.DUP_X1:
            {
                ValueNode __w1 = xpop();
                ValueNode __w2 = xpop();
                xpush(__w1);
                xpush(__w2);
                xpush(__w1);
                break;
            }
            case Bytecodes.DUP_X2:
            {
                ValueNode __w1 = xpop();
                ValueNode __w2 = xpop();
                ValueNode __w3 = xpop();
                xpush(__w1);
                xpush(__w3);
                xpush(__w2);
                xpush(__w1);
                break;
            }
            case Bytecodes.DUP2:
            {
                ValueNode __w1 = xpop();
                ValueNode __w2 = xpop();
                xpush(__w2);
                xpush(__w1);
                xpush(__w2);
                xpush(__w1);
                break;
            }
            case Bytecodes.DUP2_X1:
            {
                ValueNode __w1 = xpop();
                ValueNode __w2 = xpop();
                ValueNode __w3 = xpop();
                xpush(__w2);
                xpush(__w1);
                xpush(__w3);
                xpush(__w2);
                xpush(__w1);
                break;
            }
            case Bytecodes.DUP2_X2:
            {
                ValueNode __w1 = xpop();
                ValueNode __w2 = xpop();
                ValueNode __w3 = xpop();
                ValueNode __w4 = xpop();
                xpush(__w2);
                xpush(__w1);
                xpush(__w4);
                xpush(__w3);
                xpush(__w2);
                xpush(__w1);
                break;
            }
            case Bytecodes.SWAP:
            {
                ValueNode __w1 = xpop();
                ValueNode __w2 = xpop();
                xpush(__w1);
                xpush(__w2);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public int hashCode()
    {
        int __result = hashCode(locals, locals.length);
        __result *= 13;
        __result += hashCode(stack, this.stackSize);
        return __result;
    }

    private static int hashCode(Object[] __a, int __length)
    {
        int __result = 1;
        for (int __i = 0; __i < __length; ++__i)
        {
            Object __element = __a[__i];
            __result = 31 * __result + (__element == null ? 0 : System.identityHashCode(__element));
        }
        return __result;
    }

    private static boolean equals(ValueNode[] __a, ValueNode[] __b, int __length)
    {
        for (int __i = 0; __i < __length; ++__i)
        {
            if (__a[__i] != __b[__i])
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object __otherObject)
    {
        if (__otherObject instanceof FrameStateBuilder)
        {
            FrameStateBuilder __other = (FrameStateBuilder) __otherObject;
            if (!__other.code.equals(code))
            {
                return false;
            }
            if (__other.stackSize != stackSize)
            {
                return false;
            }
            if (__other.parser != parser)
            {
                return false;
            }
            if (__other.tool != tool)
            {
                return false;
            }
            if (__other.rethrowException != rethrowException)
            {
                return false;
            }
            if (__other.graph != graph)
            {
                return false;
            }
            if (__other.locals.length != locals.length)
            {
                return false;
            }
            return equals(__other.locals, locals, locals.length) && equals(__other.stack, stack, stackSize) && equals(__other.lockedObjects, lockedObjects, lockedObjects.length) && equals(__other.monitorIds, monitorIds, monitorIds.length);
        }
        return false;
    }

    @Override
    public boolean isAfterSideEffect()
    {
        return sideEffects != null;
    }

    @Override
    public Iterable<StateSplit> sideEffects()
    {
        return sideEffects;
    }

    @Override
    public void addSideEffect(StateSplit __sideEffect)
    {
        if (sideEffects == null)
        {
            sideEffects = new ArrayList<>(4);
        }
        sideEffects.add(__sideEffect);
    }
}
