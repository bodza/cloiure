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
import giraaff.java.BciBlockMapping;
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
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderTool;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.ParameterPlugin;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

// @class FrameStateBuilder
public final class FrameStateBuilder implements IntrinsicContext.SideEffectsState
{
    // @def
    private static final ValueNode[] EMPTY_ARRAY = new ValueNode[0];
    // @def
    private static final MonitorIdNode[] EMPTY_MONITOR_ARRAY = new MonitorIdNode[0];

    // @field
    private final BytecodeParser ___parser;
    // @field
    private final GraphBuilderTool ___tool;
    // @field
    private final Bytecode ___code;
    // @field
    private int ___stackSize;
    // @field
    protected final ValueNode[] ___locals;
    // @field
    protected final ValueNode[] ___stack;
    // @field
    private ValueNode[] ___lockedObjects;

    ///
    // @see BytecodeFrame#rethrowException
    ///
    // @field
    private boolean ___rethrowException;

    // @field
    private MonitorIdNode[] ___monitorIds;
    // @field
    private final StructuredGraph ___graph;
    // @field
    private final boolean ___clearNonLiveLocals;
    // @field
    private FrameState ___outerFrameState;

    ///
    // The closest {@link StateSplit#hasSideEffect() side-effect} predecessors. There will be more
    // than one when the current block contains no side-effects but merging predecessor blocks do.
    ///
    // @field
    private List<StateSplit> ___sideEffects;

    ///
    // Creates a new frame state builder for the given method and the given target graph.
    //
    // @param method the method whose frame is simulated
    // @param graph the target graph of Graal nodes created by the builder
    ///
    // @cons FrameStateBuilder
    public FrameStateBuilder(GraphBuilderTool __tool, ResolvedJavaMethod __method, StructuredGraph __graph)
    {
        this(__tool, new ResolvedJavaMethodBytecode(__method), __graph);
    }

    ///
    // Creates a new frame state builder for the given code attribute, method and the given target graph.
    //
    // @param code the bytecode in which the frame exists
    // @param graph the target graph of Graal nodes created by the builder
    ///
    // @cons FrameStateBuilder
    public FrameStateBuilder(GraphBuilderTool __tool, Bytecode __code, StructuredGraph __graph)
    {
        super();
        this.___tool = __tool;
        if (__tool instanceof BytecodeParser)
        {
            this.___parser = (BytecodeParser) __tool;
        }
        else
        {
            this.___parser = null;
        }
        this.___code = __code;
        this.___locals = allocateArray(__code.getMaxLocals());
        this.___stack = allocateArray(Math.max(1, __code.getMaxStackSize()));
        this.___lockedObjects = allocateArray(0);

        this.___monitorIds = EMPTY_MONITOR_ARRAY;
        this.___graph = __graph;
        this.___clearNonLiveLocals = GraalOptions.optClearNonLiveLocals;
    }

    public void initializeFromArgumentsArray(ValueNode[] __arguments)
    {
        int __javaIndex = 0;
        int __index = 0;
        if (!getMethod().isStatic())
        {
            // set the receiver
            this.___locals[__javaIndex] = __arguments[__index];
            __javaIndex = 1;
            __index = 1;
        }
        Signature __sig = getMethod().getSignature();
        int __max = __sig.getParameterCount(false);
        for (int __i = 0; __i < __max; __i++)
        {
            JavaKind __kind = __sig.getParameterKind(__i);
            this.___locals[__javaIndex] = __arguments[__index];
            __javaIndex++;
            if (__kind.needsTwoSlots())
            {
                this.___locals[__javaIndex] = FrameState.TWO_SLOT_MARKER;
                __javaIndex++;
            }
            __index++;
        }
    }

    public void initializeForMethodStart(Assumptions __assumptions, boolean __eagerResolve, GraphBuilderConfiguration.Plugins __plugins)
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
                __receiverStamp = __plugins.getOverridingStamp(this.___tool, __originalType, true);
            }
            if (__receiverStamp == null)
            {
                __receiverStamp = StampFactory.forDeclaredType(__assumptions, __originalType, true);
            }

            if (__plugins != null)
            {
                for (ParameterPlugin __plugin : __plugins.getParameterPlugins())
                {
                    __receiver = __plugin.interceptParameter(this.___tool, __index, __receiverStamp);
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

            this.___locals[__javaIndex] = this.___graph.addOrUniqueWithInputs(__receiver);
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
                __stamp = __plugins.getOverridingStamp(this.___tool, __type, false);
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
                    __param = __plugin.interceptParameter(this.___tool, __index, __stamp);
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

            this.___locals[__javaIndex] = this.___graph.addOrUniqueWithInputs(__param);
            __javaIndex++;
            if (__kind.needsTwoSlots())
            {
                this.___locals[__javaIndex] = FrameState.TWO_SLOT_MARKER;
                __javaIndex++;
            }
            __index++;
        }
    }

    // @cons FrameStateBuilder
    private FrameStateBuilder(FrameStateBuilder __other)
    {
        super();
        this.___parser = __other.___parser;
        this.___tool = __other.___tool;
        this.___code = __other.___code;
        this.___stackSize = __other.___stackSize;
        this.___locals = __other.___locals.clone();
        this.___stack = __other.___stack.clone();
        this.___lockedObjects = __other.___lockedObjects.length == 0 ? __other.___lockedObjects : __other.___lockedObjects.clone();
        this.___rethrowException = __other.___rethrowException;

        this.___graph = __other.___graph;
        this.___clearNonLiveLocals = __other.___clearNonLiveLocals;
        this.___monitorIds = __other.___monitorIds.length == 0 ? __other.___monitorIds : __other.___monitorIds.clone();
    }

    private static ValueNode[] allocateArray(int __length)
    {
        return __length == 0 ? EMPTY_ARRAY : new ValueNode[__length];
    }

    public ResolvedJavaMethod getMethod()
    {
        return this.___code.getMethod();
    }

    public FrameState create(int __bci, StateSplit __forStateSplit)
    {
        if (this.___parser != null && this.___parser.parsingIntrinsic())
        {
            return this.___parser.___intrinsicContext.createFrameState(this.___parser.getGraph(), this, __forStateSplit);
        }

        // skip intrinsic frames
        return create(__bci, this.___parser != null ? this.___parser.getNonIntrinsicAncestor() : null, false, null, null);
    }

    ///
    // @param pushedValues if non-null, values to {@link #push(JavaKind, ValueNode)} to the stack
    //            before creating the {@link FrameState}
    ///
    public FrameState create(int __bci, BytecodeParser __parent, boolean __duringCall, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        if (this.___outerFrameState == null && __parent != null)
        {
            this.___outerFrameState = __parent.getFrameStateBuilder().create(__parent.bci(), __parent.getNonIntrinsicAncestor(), true, null, null);
        }
        if (__bci == BytecodeFrame.AFTER_EXCEPTION_BCI && __parent != null)
        {
            return this.___outerFrameState.duplicateModified(this.___outerFrameState.___bci, true, false, JavaKind.Void, new JavaKind[] { JavaKind.Object }, new ValueNode[] { this.___stack[0] });
        }
        if (__bci == BytecodeFrame.INVALID_FRAMESTATE_BCI)
        {
            throw GraalError.shouldNotReachHere();
        }

        if (__pushedValues != null)
        {
            int __stackSizeToRestore = this.___stackSize;
            for (int __i = 0; __i < __pushedValues.length; __i++)
            {
                push(__pushedSlotKinds[__i], __pushedValues[__i]);
            }
            FrameState __res = this.___graph.add(new FrameState(this.___outerFrameState, this.___code, __bci, this.___locals, this.___stack, this.___stackSize, this.___lockedObjects, Arrays.asList(this.___monitorIds), this.___rethrowException, __duringCall));
            this.___stackSize = __stackSizeToRestore;
            return __res;
        }
        else
        {
            if (__bci == BytecodeFrame.AFTER_EXCEPTION_BCI)
            {
                clearLocals();
            }
            return this.___graph.add(new FrameState(this.___outerFrameState, this.___code, __bci, this.___locals, this.___stack, this.___stackSize, this.___lockedObjects, Arrays.asList(this.___monitorIds), this.___rethrowException, __duringCall));
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
            ValueNode __x = this.___stack[__i];
            ValueNode __y = __other.___stack[__i];
            if (__x != __y && (__x == FrameState.TWO_SLOT_MARKER || __x.isDeleted() || __y == FrameState.TWO_SLOT_MARKER || __y.isDeleted() || __x.getStackKind() != __y.getStackKind()))
            {
                return false;
            }
        }
        if (this.___lockedObjects.length != __other.___lockedObjects.length)
        {
            return false;
        }
        for (int __i = 0; __i < this.___lockedObjects.length; __i++)
        {
            if (GraphUtil.originalValue(this.___lockedObjects[__i]) != GraphUtil.originalValue(__other.___lockedObjects[__i]) || this.___monitorIds[__i] != __other.___monitorIds[__i])
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
            this.___locals[__i] = merge(this.___locals[__i], __other.___locals[__i], __block);
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            this.___stack[__i] = merge(this.___stack[__i], __other.___stack[__i], __block);
        }
        for (int __i = 0; __i < this.___lockedObjects.length; __i++)
        {
            this.___lockedObjects[__i] = merge(this.___lockedObjects[__i], __other.___lockedObjects[__i], __block);
        }

        if (this.___sideEffects == null)
        {
            this.___sideEffects = __other.___sideEffects;
        }
        else
        {
            if (__other.___sideEffects != null)
            {
                this.___sideEffects.addAll(__other.___sideEffects);
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
                ((PhiNode) __currentValue).addInput(ConstantNode.defaultForKind(__currentValue.getStackKind(), this.___graph));
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
        ValuePhiNode __phi = this.___graph.addWithoutUnique(new ValuePhiNode(__currentValue.stamp(NodeView.DEFAULT).unrestricted(), __block));
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
            inferPhiStamp(__block, this.___locals[__i]);
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            inferPhiStamp(__block, this.___stack[__i]);
        }
        for (int __i = 0; __i < this.___lockedObjects.length; __i++)
        {
            inferPhiStamp(__block, this.___lockedObjects[__i]);
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
                this.___locals[__i] = createLoopPhi(__loopBegin, this.___locals[__i], __stampFromValueForForcedPhis && !__changedInLoop);
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            this.___stack[__i] = createLoopPhi(__loopBegin, this.___stack[__i], false);
        }
        for (int __i = 0; __i < this.___lockedObjects.length; __i++)
        {
            this.___lockedObjects[__i] = createLoopPhi(__loopBegin, this.___lockedObjects[__i], false);
        }
    }

    public void insertLoopProxies(LoopExitNode __loopExit, FrameStateBuilder __loopEntryState)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            ValueNode __value = this.___locals[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER && (!__loopEntryState.contains(__value) || __loopExit.loopBegin().isPhiAtMerge(__value)))
            {
                this.___locals[__i] = ProxyNode.forValue(__value, __loopExit, this.___graph);
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            ValueNode __value = this.___stack[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER && (!__loopEntryState.contains(__value) || __loopExit.loopBegin().isPhiAtMerge(__value)))
            {
                this.___stack[__i] = ProxyNode.forValue(__value, __loopExit, this.___graph);
            }
        }
        for (int __i = 0; __i < this.___lockedObjects.length; __i++)
        {
            ValueNode __value = this.___lockedObjects[__i];
            if (__value != null && (!__loopEntryState.contains(__value) || __loopExit.loopBegin().isPhiAtMerge(__value)))
            {
                this.___lockedObjects[__i] = ProxyNode.forValue(__value, __loopExit, this.___graph);
            }
        }
    }

    public void insertProxies(Function<ValueNode, ValueNode> __proxyFunction)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            ValueNode __value = this.___locals[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER)
            {
                this.___locals[__i] = __proxyFunction.apply(__value);
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            ValueNode __value = this.___stack[__i];
            if (__value != null && __value != FrameState.TWO_SLOT_MARKER)
            {
                this.___stack[__i] = __proxyFunction.apply(__value);
            }
        }
        for (int __i = 0; __i < this.___lockedObjects.length; __i++)
        {
            ValueNode __value = this.___lockedObjects[__i];
            if (__value != null)
            {
                this.___lockedObjects[__i] = __proxyFunction.apply(__value);
            }
        }
    }

    private ValueNode createLoopPhi(AbstractMergeNode __block, ValueNode __value, boolean __stampFromValue)
    {
        if (__value == null || __value == FrameState.TWO_SLOT_MARKER)
        {
            return __value;
        }

        ValuePhiNode __phi = this.___graph.addWithoutUnique(new ValuePhiNode(__stampFromValue ? __value.stamp(NodeView.DEFAULT) : __value.stamp(NodeView.DEFAULT).unrestricted(), __block));
        __phi.addInput(__value);
        return __phi;
    }

    ///
    // Adds a locked monitor to this frame state.
    //
    // @param object the object whose monitor will be locked.
    ///
    public void pushLock(ValueNode __object, MonitorIdNode __monitorId)
    {
        this.___lockedObjects = Arrays.copyOf(this.___lockedObjects, this.___lockedObjects.length + 1);
        this.___monitorIds = Arrays.copyOf(this.___monitorIds, this.___monitorIds.length + 1);
        this.___lockedObjects[this.___lockedObjects.length - 1] = __object;
        this.___monitorIds[this.___monitorIds.length - 1] = __monitorId;
    }

    ///
    // Removes a locked monitor from this frame state.
    //
    // @return the object whose monitor was removed from the locks list.
    ///
    public ValueNode popLock()
    {
        try
        {
            return this.___lockedObjects[this.___lockedObjects.length - 1];
        }
        finally
        {
            this.___lockedObjects = this.___lockedObjects.length == 1 ? EMPTY_ARRAY : Arrays.copyOf(this.___lockedObjects, this.___lockedObjects.length - 1);
            this.___monitorIds = this.___monitorIds.length == 1 ? EMPTY_MONITOR_ARRAY : Arrays.copyOf(this.___monitorIds, this.___monitorIds.length - 1);
        }
    }

    public MonitorIdNode peekMonitorId()
    {
        return this.___monitorIds[this.___monitorIds.length - 1];
    }

    ///
    // @return the current lock depth
    ///
    public int lockDepth(boolean __includeParents)
    {
        int __depth = this.___lockedObjects.length;
        if (__includeParents && this.___parser.getParent() != null)
        {
            __depth += this.___parser.getParent().___frameState.lockDepth(true);
        }
        return __depth;
    }

    public boolean contains(ValueNode __value)
    {
        for (int __i = 0; __i < localsSize(); __i++)
        {
            if (this.___locals[__i] == __value)
            {
                return true;
            }
        }
        for (int __i = 0; __i < stackSize(); __i++)
        {
            if (this.___stack[__i] == __value)
            {
                return true;
            }
        }
        for (int __i = 0; __i < this.___lockedObjects.length; __i++)
        {
            if (this.___lockedObjects[__i] == __value || this.___monitorIds[__i] == __value)
            {
                return true;
            }
        }
        return false;
    }

    public void clearNonLiveLocals(BciBlockMapping.BciBlock __block, LocalLiveness __liveness, boolean __liveIn)
    {
        // (lstadler) if somebody is tempted to remove/disable this clearing code: it's possible to
        // remove it for normal compilations, but not for OSR compilations - otherwise dead object
        // slots at the OSR entry aren't cleared. it is also not enough to rely on PiNodes with
        // Kind.Illegal, because the conflicting branch might not have been parsed.
        if (!this.___clearNonLiveLocals)
        {
            return;
        }
        if (__liveIn)
        {
            for (int __i = 0; __i < this.___locals.length; __i++)
            {
                if (!__liveness.localIsLiveIn(__block, __i))
                {
                    this.___locals[__i] = null;
                }
            }
        }
        else
        {
            for (int __i = 0; __i < this.___locals.length; __i++)
            {
                if (!__liveness.localIsLiveOut(__block, __i))
                {
                    this.___locals[__i] = null;
                }
            }
        }
    }

    ///
    // Clears all local variables.
    ///
    public void clearLocals()
    {
        for (int __i = 0; __i < this.___locals.length; __i++)
        {
            this.___locals[__i] = null;
        }
    }

    ///
    // @see BytecodeFrame#rethrowException
    ///
    public boolean rethrowException()
    {
        return this.___rethrowException;
    }

    ///
    // @see BytecodeFrame#rethrowException
    ///
    public void setRethrowException(boolean __b)
    {
        this.___rethrowException = __b;
    }

    ///
    // Returns the size of the local variables.
    //
    // @return the size of the local variables
    ///
    public int localsSize()
    {
        return this.___locals.length;
    }

    ///
    // Gets the current size (height) of the stack.
    ///
    public int stackSize()
    {
        return this.___stackSize;
    }

    ///
    // Loads the local variable at the specified index, checking that the returned value is non-null
    // and that two-stack values are properly handled.
    //
    // @param i the index of the local variable to load
    // @param slotKind the kind of the local variable from the point of view of the bytecodes
    // @return the instruction that produced the specified local
    ///
    public ValueNode loadLocal(int __i, JavaKind __slotKind)
    {
        return this.___locals[__i];
    }

    ///
    // Stores a given local variable at the specified index. If the value occupies two slots, then
    // the next local variable index is also overwritten.
    //
    // @param i the index at which to store
    // @param slotKind the kind of the local variable from the point of view of the bytecodes
    // @param x the instruction which produces the value for the local
    ///
    public void storeLocal(int __i, JavaKind __slotKind, ValueNode __x)
    {
        if (this.___locals[__i] == FrameState.TWO_SLOT_MARKER)
        {
            // Writing the second slot of a two-slot value invalidates the first slot.
            this.___locals[__i - 1] = null;
        }
        this.___locals[__i] = __x;
        if (__slotKind.needsTwoSlots())
        {
            // Writing a two-slot value: mark the second slot.
            this.___locals[__i + 1] = FrameState.TWO_SLOT_MARKER;
        }
        else if (__i < this.___locals.length - 1 && this.___locals[__i + 1] == FrameState.TWO_SLOT_MARKER)
        {
            // Writing a one-slot value to an index previously occupied by a two-slot value: clear the old marker of the second slot.
            this.___locals[__i + 1] = null;
        }
    }

    ///
    // Pushes an instruction onto the stack with the expected type.
    //
    // @param slotKind the kind of the stack element from the point of view of the bytecodes
    // @param x the instruction to push onto the stack
    ///
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

    ///
    // Pops an instruction off the stack with the expected type.
    //
    // @param slotKind the kind of the stack element from the point of view of the bytecodes
    // @return the instruction on the top of the stack
    ///
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
        this.___stack[this.___stackSize++] = __x;
    }

    private ValueNode xpop()
    {
        return this.___stack[--this.___stackSize];
    }

    private ValueNode xpeek()
    {
        return this.___stack[this.___stackSize - 1];
    }

    ///
    // Pop the specified number of slots off of this stack and return them as an array of instructions.
    //
    // @return an array containing the arguments off of the stack
    ///
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

    ///
    // Clears all values on this stack.
    ///
    public void clearStack()
    {
        this.___stackSize = 0;
    }

    ///
    // Performs a raw stack operation as defined in the Java bytecode specification.
    //
    // @param opcode The Java bytecode.
    ///
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
        int __result = hashCode(this.___locals, this.___locals.length);
        __result *= 13;
        __result += hashCode(this.___stack, this.___stackSize);
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
            if (!__other.___code.equals(this.___code))
            {
                return false;
            }
            if (__other.___stackSize != this.___stackSize)
            {
                return false;
            }
            if (__other.___parser != this.___parser)
            {
                return false;
            }
            if (__other.___tool != this.___tool)
            {
                return false;
            }
            if (__other.___rethrowException != this.___rethrowException)
            {
                return false;
            }
            if (__other.___graph != this.___graph)
            {
                return false;
            }
            if (__other.___locals.length != this.___locals.length)
            {
                return false;
            }
            return equals(__other.___locals, this.___locals, this.___locals.length) && equals(__other.___stack, this.___stack, this.___stackSize) && equals(__other.___lockedObjects, this.___lockedObjects, this.___lockedObjects.length) && equals(__other.___monitorIds, this.___monitorIds, this.___monitorIds.length);
        }
        return false;
    }

    @Override
    public boolean isAfterSideEffect()
    {
        return this.___sideEffects != null;
    }

    @Override
    public Iterable<StateSplit> sideEffects()
    {
        return this.___sideEffects;
    }

    @Override
    public void addSideEffect(StateSplit __sideEffect)
    {
        if (this.___sideEffects == null)
        {
            this.___sideEffects = new ArrayList<>(4);
        }
        this.___sideEffects.add(__sideEffect);
    }
}
