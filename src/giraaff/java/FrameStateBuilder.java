package giraaff.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

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
import giraaff.core.common.PermanentBailoutException;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.java.BciBlockMapping.BciBlock;
import giraaff.nodeinfo.Verbosity;
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
    private static final ValueNode[] EMPTY_ARRAY = new ValueNode[0];
    private static final MonitorIdNode[] EMPTY_MONITOR_ARRAY = new MonitorIdNode[0];

    private final BytecodeParser parser;
    private final GraphBuilderTool tool;
    private final Bytecode code;
    private int stackSize;
    protected final ValueNode[] locals;
    protected final ValueNode[] stack;
    private ValueNode[] lockedObjects;

    /**
     * @see BytecodeFrame#rethrowException
     */
    private boolean rethrowException;

    private MonitorIdNode[] monitorIds;
    private final StructuredGraph graph;
    private final boolean clearNonLiveLocals;
    private FrameState outerFrameState;

    /**
     * The closest {@link StateSplit#hasSideEffect() side-effect} predecessors. There will be more
     * than one when the current block contains no side-effects but merging predecessor blocks do.
     */
    private List<StateSplit> sideEffects;

    /**
     * Creates a new frame state builder for the given method and the given target graph.
     *
     * @param method the method whose frame is simulated
     * @param graph the target graph of Graal nodes created by the builder
     */
    // @cons
    public FrameStateBuilder(GraphBuilderTool tool, ResolvedJavaMethod method, StructuredGraph graph)
    {
        this(tool, new ResolvedJavaMethodBytecode(method), graph);
    }

    /**
     * Creates a new frame state builder for the given code attribute, method and the given target graph.
     *
     * @param code the bytecode in which the frame exists
     * @param graph the target graph of Graal nodes created by the builder
     */
    // @cons
    public FrameStateBuilder(GraphBuilderTool tool, Bytecode code, StructuredGraph graph)
    {
        super();
        this.tool = tool;
        if (tool instanceof BytecodeParser)
        {
            this.parser = (BytecodeParser) tool;
        }
        else
        {
            this.parser = null;
        }
        this.code = code;
        this.locals = allocateArray(code.getMaxLocals());
        this.stack = allocateArray(Math.max(1, code.getMaxStackSize()));
        this.lockedObjects = allocateArray(0);

        this.monitorIds = EMPTY_MONITOR_ARRAY;
        this.graph = graph;
        this.clearNonLiveLocals = GraalOptions.OptClearNonLiveLocals.getValue(graph.getOptions());
    }

    public void initializeFromArgumentsArray(ValueNode[] arguments)
    {
        int javaIndex = 0;
        int index = 0;
        if (!getMethod().isStatic())
        {
            // set the receiver
            locals[javaIndex] = arguments[index];
            javaIndex = 1;
            index = 1;
        }
        Signature sig = getMethod().getSignature();
        int max = sig.getParameterCount(false);
        for (int i = 0; i < max; i++)
        {
            JavaKind kind = sig.getParameterKind(i);
            locals[javaIndex] = arguments[index];
            javaIndex++;
            if (kind.needsTwoSlots())
            {
                locals[javaIndex] = FrameState.TWO_SLOT_MARKER;
                javaIndex++;
            }
            index++;
        }
    }

    public void initializeForMethodStart(Assumptions assumptions, boolean eagerResolve, Plugins plugins)
    {
        int javaIndex = 0;
        int index = 0;
        ResolvedJavaMethod method = getMethod();
        ResolvedJavaType originalType = method.getDeclaringClass();
        if (!method.isStatic())
        {
            // add the receiver
            FloatingNode receiver = null;
            StampPair receiverStamp = null;
            if (plugins != null)
            {
                receiverStamp = plugins.getOverridingStamp(tool, originalType, true);
            }
            if (receiverStamp == null)
            {
                receiverStamp = StampFactory.forDeclaredType(assumptions, originalType, true);
            }

            if (plugins != null)
            {
                for (ParameterPlugin plugin : plugins.getParameterPlugins())
                {
                    receiver = plugin.interceptParameter(tool, index, receiverStamp);
                    if (receiver != null)
                    {
                        break;
                    }
                }
            }
            if (receiver == null)
            {
                receiver = new ParameterNode(javaIndex, receiverStamp);
            }

            locals[javaIndex] = graph.addOrUniqueWithInputs(receiver);
            javaIndex = 1;
            index = 1;
        }
        Signature sig = method.getSignature();
        int max = sig.getParameterCount(false);
        ResolvedJavaType accessingClass = originalType;
        for (int i = 0; i < max; i++)
        {
            JavaType type = sig.getParameterType(i, accessingClass);
            if (eagerResolve)
            {
                type = type.resolve(accessingClass);
            }
            JavaKind kind = type.getJavaKind();
            StampPair stamp = null;
            if (plugins != null)
            {
                stamp = plugins.getOverridingStamp(tool, type, false);
            }
            if (stamp == null)
            {
                stamp = StampFactory.forDeclaredType(assumptions, type, false);
            }

            FloatingNode param = null;
            if (plugins != null)
            {
                for (ParameterPlugin plugin : plugins.getParameterPlugins())
                {
                    param = plugin.interceptParameter(tool, index, stamp);
                    if (param != null)
                    {
                        break;
                    }
                }
            }
            if (param == null)
            {
                param = new ParameterNode(index, stamp);
            }

            locals[javaIndex] = graph.addOrUniqueWithInputs(param);
            javaIndex++;
            if (kind.needsTwoSlots())
            {
                locals[javaIndex] = FrameState.TWO_SLOT_MARKER;
                javaIndex++;
            }
            index++;
        }
    }

    // @cons
    private FrameStateBuilder(FrameStateBuilder other)
    {
        super();
        this.parser = other.parser;
        this.tool = other.tool;
        this.code = other.code;
        this.stackSize = other.stackSize;
        this.locals = other.locals.clone();
        this.stack = other.stack.clone();
        this.lockedObjects = other.lockedObjects.length == 0 ? other.lockedObjects : other.lockedObjects.clone();
        this.rethrowException = other.rethrowException;

        graph = other.graph;
        clearNonLiveLocals = other.clearNonLiveLocals;
        monitorIds = other.monitorIds.length == 0 ? other.monitorIds : other.monitorIds.clone();
    }

    private static ValueNode[] allocateArray(int length)
    {
        return length == 0 ? EMPTY_ARRAY : new ValueNode[length];
    }

    public ResolvedJavaMethod getMethod()
    {
        return code.getMethod();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[locals: [");
        for (int i = 0; i < locals.length; i++)
        {
            sb.append(i == 0 ? "" : ",").append(locals[i] == null ? "_" : locals[i] == FrameState.TWO_SLOT_MARKER ? "#" : locals[i].toString(Verbosity.Id));
        }
        sb.append("] stack: [");
        for (int i = 0; i < stackSize; i++)
        {
            sb.append(i == 0 ? "" : ",").append(stack[i] == null ? "_" : stack[i] == FrameState.TWO_SLOT_MARKER ? "#" : stack[i].toString(Verbosity.Id));
        }
        sb.append("] locks: [");
        for (int i = 0; i < lockedObjects.length; i++)
        {
            sb.append(i == 0 ? "" : ",").append(lockedObjects[i].toString(Verbosity.Id)).append(" / ").append(monitorIds[i].toString(Verbosity.Id));
        }
        sb.append("]");
        if (rethrowException)
        {
            sb.append(" rethrowException");
        }
        sb.append("]");
        return sb.toString();
    }

    public FrameState create(int bci, StateSplit forStateSplit)
    {
        if (parser != null && parser.parsingIntrinsic())
        {
            return parser.intrinsicContext.createFrameState(parser.getGraph(), this, forStateSplit);
        }

        // skip intrinsic frames
        return create(bci, parser != null ? parser.getNonIntrinsicAncestor() : null, false, null, null);
    }

    /**
     * @param pushedValues if non-null, values to {@link #push(JavaKind, ValueNode)} to the stack
     *            before creating the {@link FrameState}
     */
    public FrameState create(int bci, BytecodeParser parent, boolean duringCall, JavaKind[] pushedSlotKinds, ValueNode[] pushedValues)
    {
        if (outerFrameState == null && parent != null)
        {
            outerFrameState = parent.getFrameStateBuilder().create(parent.bci(), parent.getNonIntrinsicAncestor(), true, null, null);
        }
        if (bci == BytecodeFrame.AFTER_EXCEPTION_BCI && parent != null)
        {
            return outerFrameState.duplicateModified(outerFrameState.bci, true, false, JavaKind.Void, new JavaKind[] { JavaKind.Object }, new ValueNode[] { stack[0] });
        }
        if (bci == BytecodeFrame.INVALID_FRAMESTATE_BCI)
        {
            throw GraalError.shouldNotReachHere();
        }

        if (pushedValues != null)
        {
            int stackSizeToRestore = stackSize;
            for (int i = 0; i < pushedValues.length; i++)
            {
                push(pushedSlotKinds[i], pushedValues[i]);
            }
            FrameState res = graph.add(new FrameState(outerFrameState, code, bci, locals, stack, stackSize, lockedObjects, Arrays.asList(monitorIds), rethrowException, duringCall));
            stackSize = stackSizeToRestore;
            return res;
        }
        else
        {
            if (bci == BytecodeFrame.AFTER_EXCEPTION_BCI)
            {
                clearLocals();
            }
            return graph.add(new FrameState(outerFrameState, code, bci, locals, stack, stackSize, lockedObjects, Arrays.asList(monitorIds), rethrowException, duringCall));
        }
    }

    public FrameStateBuilder copy()
    {
        return new FrameStateBuilder(this);
    }

    public boolean isCompatibleWith(FrameStateBuilder other)
    {
        if (stackSize() != other.stackSize())
        {
            return false;
        }
        for (int i = 0; i < stackSize(); i++)
        {
            ValueNode x = stack[i];
            ValueNode y = other.stack[i];
            if (x != y && (x == FrameState.TWO_SLOT_MARKER || x.isDeleted() || y == FrameState.TWO_SLOT_MARKER || y.isDeleted() || x.getStackKind() != y.getStackKind()))
            {
                return false;
            }
        }
        if (lockedObjects.length != other.lockedObjects.length)
        {
            return false;
        }
        for (int i = 0; i < lockedObjects.length; i++)
        {
            if (GraphUtil.originalValue(lockedObjects[i]) != GraphUtil.originalValue(other.lockedObjects[i]) || monitorIds[i] != other.monitorIds[i])
            {
                throw new PermanentBailoutException("unbalanced monitors");
            }
        }
        return true;
    }

    public void merge(AbstractMergeNode block, FrameStateBuilder other)
    {
        for (int i = 0; i < localsSize(); i++)
        {
            locals[i] = merge(locals[i], other.locals[i], block);
        }
        for (int i = 0; i < stackSize(); i++)
        {
            stack[i] = merge(stack[i], other.stack[i], block);
        }
        for (int i = 0; i < lockedObjects.length; i++)
        {
            lockedObjects[i] = merge(lockedObjects[i], other.lockedObjects[i], block);
        }

        if (sideEffects == null)
        {
            sideEffects = other.sideEffects;
        }
        else
        {
            if (other.sideEffects != null)
            {
                sideEffects.addAll(other.sideEffects);
            }
        }
    }

    private ValueNode merge(ValueNode currentValue, ValueNode otherValue, AbstractMergeNode block)
    {
        if (currentValue == null || currentValue.isDeleted())
        {
            return null;
        }
        else if (block.isPhiAtMerge(currentValue))
        {
            if (otherValue == null || otherValue == FrameState.TWO_SLOT_MARKER || otherValue.isDeleted() || currentValue.getStackKind() != otherValue.getStackKind())
            {
                // This phi must be dead anyway, add input of correct stack kind to keep the graph invariants.
                ((PhiNode) currentValue).addInput(ConstantNode.defaultForKind(currentValue.getStackKind(), graph));
            }
            else
            {
                ((PhiNode) currentValue).addInput(otherValue);
            }
            return currentValue;
        }
        else if (currentValue != otherValue)
        {
            if (currentValue == FrameState.TWO_SLOT_MARKER || otherValue == FrameState.TWO_SLOT_MARKER)
            {
                return null;
            }
            else if (otherValue == null || otherValue.isDeleted() || currentValue.getStackKind() != otherValue.getStackKind())
            {
                return null;
            }
            return createValuePhi(currentValue, otherValue, block);
        }
        else
        {
            return currentValue;
        }
    }

    private ValuePhiNode createValuePhi(ValueNode currentValue, ValueNode otherValue, AbstractMergeNode block)
    {
        ValuePhiNode phi = graph.addWithoutUnique(new ValuePhiNode(currentValue.stamp(NodeView.DEFAULT).unrestricted(), block));
        for (int i = 0; i < block.phiPredecessorCount(); i++)
        {
            phi.addInput(currentValue);
        }
        phi.addInput(otherValue);
        return phi;
    }

    public void inferPhiStamps(AbstractMergeNode block)
    {
        for (int i = 0; i < localsSize(); i++)
        {
            inferPhiStamp(block, locals[i]);
        }
        for (int i = 0; i < stackSize(); i++)
        {
            inferPhiStamp(block, stack[i]);
        }
        for (int i = 0; i < lockedObjects.length; i++)
        {
            inferPhiStamp(block, lockedObjects[i]);
        }
    }

    private static void inferPhiStamp(AbstractMergeNode block, ValueNode node)
    {
        if (block.isPhiAtMerge(node))
        {
            node.inferStamp();
        }
    }

    public void insertLoopPhis(LocalLiveness liveness, int loopId, LoopBeginNode loopBegin, boolean forcePhis, boolean stampFromValueForForcedPhis)
    {
        for (int i = 0; i < localsSize(); i++)
        {
            boolean changedInLoop = liveness.localIsChangedInLoop(loopId, i);
            if (forcePhis || changedInLoop)
            {
                locals[i] = createLoopPhi(loopBegin, locals[i], stampFromValueForForcedPhis && !changedInLoop);
            }
        }
        for (int i = 0; i < stackSize(); i++)
        {
            stack[i] = createLoopPhi(loopBegin, stack[i], false);
        }
        for (int i = 0; i < lockedObjects.length; i++)
        {
            lockedObjects[i] = createLoopPhi(loopBegin, lockedObjects[i], false);
        }
    }

    public void insertLoopProxies(LoopExitNode loopExit, FrameStateBuilder loopEntryState)
    {
        for (int i = 0; i < localsSize(); i++)
        {
            ValueNode value = locals[i];
            if (value != null && value != FrameState.TWO_SLOT_MARKER && (!loopEntryState.contains(value) || loopExit.loopBegin().isPhiAtMerge(value)))
            {
                locals[i] = ProxyNode.forValue(value, loopExit, graph);
            }
        }
        for (int i = 0; i < stackSize(); i++)
        {
            ValueNode value = stack[i];
            if (value != null && value != FrameState.TWO_SLOT_MARKER && (!loopEntryState.contains(value) || loopExit.loopBegin().isPhiAtMerge(value)))
            {
                stack[i] = ProxyNode.forValue(value, loopExit, graph);
            }
        }
        for (int i = 0; i < lockedObjects.length; i++)
        {
            ValueNode value = lockedObjects[i];
            if (value != null && (!loopEntryState.contains(value) || loopExit.loopBegin().isPhiAtMerge(value)))
            {
                lockedObjects[i] = ProxyNode.forValue(value, loopExit, graph);
            }
        }
    }

    public void insertProxies(Function<ValueNode, ValueNode> proxyFunction)
    {
        for (int i = 0; i < localsSize(); i++)
        {
            ValueNode value = locals[i];
            if (value != null && value != FrameState.TWO_SLOT_MARKER)
            {
                locals[i] = proxyFunction.apply(value);
            }
        }
        for (int i = 0; i < stackSize(); i++)
        {
            ValueNode value = stack[i];
            if (value != null && value != FrameState.TWO_SLOT_MARKER)
            {
                stack[i] = proxyFunction.apply(value);
            }
        }
        for (int i = 0; i < lockedObjects.length; i++)
        {
            ValueNode value = lockedObjects[i];
            if (value != null)
            {
                lockedObjects[i] = proxyFunction.apply(value);
            }
        }
    }

    private ValueNode createLoopPhi(AbstractMergeNode block, ValueNode value, boolean stampFromValue)
    {
        if (value == null || value == FrameState.TWO_SLOT_MARKER)
        {
            return value;
        }

        ValuePhiNode phi = graph.addWithoutUnique(new ValuePhiNode(stampFromValue ? value.stamp(NodeView.DEFAULT) : value.stamp(NodeView.DEFAULT).unrestricted(), block));
        phi.addInput(value);
        return phi;
    }

    /**
     * Adds a locked monitor to this frame state.
     *
     * @param object the object whose monitor will be locked.
     */
    public void pushLock(ValueNode object, MonitorIdNode monitorId)
    {
        lockedObjects = Arrays.copyOf(lockedObjects, lockedObjects.length + 1);
        monitorIds = Arrays.copyOf(monitorIds, monitorIds.length + 1);
        lockedObjects[lockedObjects.length - 1] = object;
        monitorIds[monitorIds.length - 1] = monitorId;
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
    public int lockDepth(boolean includeParents)
    {
        int depth = lockedObjects.length;
        if (includeParents && parser.getParent() != null)
        {
            depth += parser.getParent().frameState.lockDepth(true);
        }
        return depth;
    }

    public boolean contains(ValueNode value)
    {
        for (int i = 0; i < localsSize(); i++)
        {
            if (locals[i] == value)
            {
                return true;
            }
        }
        for (int i = 0; i < stackSize(); i++)
        {
            if (stack[i] == value)
            {
                return true;
            }
        }
        for (int i = 0; i < lockedObjects.length; i++)
        {
            if (lockedObjects[i] == value || monitorIds[i] == value)
            {
                return true;
            }
        }
        return false;
    }

    public void clearNonLiveLocals(BciBlock block, LocalLiveness liveness, boolean liveIn)
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
        if (liveIn)
        {
            for (int i = 0; i < locals.length; i++)
            {
                if (!liveness.localIsLiveIn(block, i))
                {
                    locals[i] = null;
                }
            }
        }
        else
        {
            for (int i = 0; i < locals.length; i++)
            {
                if (!liveness.localIsLiveOut(block, i))
                {
                    locals[i] = null;
                }
            }
        }
    }

    /**
     * Clears all local variables.
     */
    public void clearLocals()
    {
        for (int i = 0; i < locals.length; i++)
        {
            locals[i] = null;
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
    public void setRethrowException(boolean b)
    {
        rethrowException = b;
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
    public ValueNode loadLocal(int i, JavaKind slotKind)
    {
        return locals[i];
    }

    /**
     * Stores a given local variable at the specified index. If the value occupies two slots, then
     * the next local variable index is also overwritten.
     *
     * @param i the index at which to store
     * @param slotKind the kind of the local variable from the point of view of the bytecodes
     * @param x the instruction which produces the value for the local
     */
    public void storeLocal(int i, JavaKind slotKind, ValueNode x)
    {
        if (locals[i] == FrameState.TWO_SLOT_MARKER)
        {
            // Writing the second slot of a two-slot value invalidates the first slot.
            locals[i - 1] = null;
        }
        locals[i] = x;
        if (slotKind.needsTwoSlots())
        {
            // Writing a two-slot value: mark the second slot.
            locals[i + 1] = FrameState.TWO_SLOT_MARKER;
        }
        else if (i < locals.length - 1 && locals[i + 1] == FrameState.TWO_SLOT_MARKER)
        {
            // Writing a one-slot value to an index previously occupied by a two-slot value: clear the old marker of the second slot.
            locals[i + 1] = null;
        }
    }

    /**
     * Pushes an instruction onto the stack with the expected type.
     *
     * @param slotKind the kind of the stack element from the point of view of the bytecodes
     * @param x the instruction to push onto the stack
     */
    public void push(JavaKind slotKind, ValueNode x)
    {
        xpush(x);
        if (slotKind.needsTwoSlots())
        {
            xpush(FrameState.TWO_SLOT_MARKER);
        }
    }

    public void pushReturn(JavaKind slotKind, ValueNode x)
    {
        if (slotKind != JavaKind.Void)
        {
            push(slotKind, x);
        }
    }

    /**
     * Pops an instruction off the stack with the expected type.
     *
     * @param slotKind the kind of the stack element from the point of view of the bytecodes
     * @return the instruction on the top of the stack
     */
    public ValueNode pop(JavaKind slotKind)
    {
        if (slotKind.needsTwoSlots())
        {
            ValueNode s = xpop();
        }
        return xpop();
    }

    private void xpush(ValueNode x)
    {
        stack[stackSize++] = x;
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
    public ValueNode[] popArguments(int argSize)
    {
        ValueNode[] result = allocateArray(argSize);
        for (int i = argSize - 1; i >= 0; i--)
        {
            ValueNode x = xpop();
            if (x == FrameState.TWO_SLOT_MARKER)
            {
                // Ignore second slot of two-slot value.
                x = xpop();
            }
            result[i] = x;
        }
        return result;
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
    public void stackOp(int opcode)
    {
        switch (opcode)
        {
            case Bytecodes.POP:
            {
                ValueNode w1 = xpop();
                break;
            }
            case Bytecodes.POP2:
            {
                xpop();
                ValueNode w2 = xpop();
                break;
            }
            case Bytecodes.DUP:
            {
                ValueNode w1 = xpeek();
                xpush(w1);
                break;
            }
            case Bytecodes.DUP_X1:
            {
                ValueNode w1 = xpop();
                ValueNode w2 = xpop();
                xpush(w1);
                xpush(w2);
                xpush(w1);
                break;
            }
            case Bytecodes.DUP_X2:
            {
                ValueNode w1 = xpop();
                ValueNode w2 = xpop();
                ValueNode w3 = xpop();
                xpush(w1);
                xpush(w3);
                xpush(w2);
                xpush(w1);
                break;
            }
            case Bytecodes.DUP2:
            {
                ValueNode w1 = xpop();
                ValueNode w2 = xpop();
                xpush(w2);
                xpush(w1);
                xpush(w2);
                xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X1:
            {
                ValueNode w1 = xpop();
                ValueNode w2 = xpop();
                ValueNode w3 = xpop();
                xpush(w2);
                xpush(w1);
                xpush(w3);
                xpush(w2);
                xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X2:
            {
                ValueNode w1 = xpop();
                ValueNode w2 = xpop();
                ValueNode w3 = xpop();
                ValueNode w4 = xpop();
                xpush(w2);
                xpush(w1);
                xpush(w4);
                xpush(w3);
                xpush(w2);
                xpush(w1);
                break;
            }
            case Bytecodes.SWAP:
            {
                ValueNode w1 = xpop();
                ValueNode w2 = xpop();
                xpush(w1);
                xpush(w2);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public int hashCode()
    {
        int result = hashCode(locals, locals.length);
        result *= 13;
        result += hashCode(stack, this.stackSize);
        return result;
    }

    private static int hashCode(Object[] a, int length)
    {
        int result = 1;
        for (int i = 0; i < length; ++i)
        {
            Object element = a[i];
            result = 31 * result + (element == null ? 0 : System.identityHashCode(element));
        }
        return result;
    }

    private static boolean equals(ValueNode[] a, ValueNode[] b, int length)
    {
        for (int i = 0; i < length; ++i)
        {
            if (a[i] != b[i])
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object otherObject)
    {
        if (otherObject instanceof FrameStateBuilder)
        {
            FrameStateBuilder other = (FrameStateBuilder) otherObject;
            if (!other.code.equals(code))
            {
                return false;
            }
            if (other.stackSize != stackSize)
            {
                return false;
            }
            if (other.parser != parser)
            {
                return false;
            }
            if (other.tool != tool)
            {
                return false;
            }
            if (other.rethrowException != rethrowException)
            {
                return false;
            }
            if (other.graph != graph)
            {
                return false;
            }
            if (other.locals.length != locals.length)
            {
                return false;
            }
            return equals(other.locals, locals, locals.length) && equals(other.stack, stack, stackSize) && equals(other.lockedObjects, lockedObjects, lockedObjects.length) && equals(other.monitorIds, monitorIds, monitorIds.length);
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
    public void addSideEffect(StateSplit sideEffect)
    {
        if (sideEffects == null)
        {
            sideEffects = new ArrayList<>(4);
        }
        sideEffects.add(sideEffect);
    }
}
