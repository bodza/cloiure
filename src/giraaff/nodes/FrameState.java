package giraaff.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.Bytecode;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.java.ExceptionObjectNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.util.GraalError;

/**
 * The {@code FrameState} class encapsulates the frame state (i.e. local variables and operand
 * stack) at a particular point in the abstract interpretation.
 *
 * This can be used as debug or deoptimization information.
 */
// @class FrameState
public final class FrameState extends VirtualState implements IterableNodeType
{
    // @def
    public static final NodeClass<FrameState> TYPE = NodeClass.create(FrameState.class);

    /**
     * Marker value for the second slot of values that occupy two local variable or expression stack
     * slots. The marker value is used by the bytecode parser, but replaced with {@code null} in the
     * {@link #values} of the {@link FrameState}.
     */
    // @def
    public static final ValueNode TWO_SLOT_MARKER = new TwoSlotMarker();

    // @class FrameState.TwoSlotMarker
    private static final class TwoSlotMarker extends ValueNode
    {
        // @def
        public static final NodeClass<TwoSlotMarker> TYPE = NodeClass.create(TwoSlotMarker.class);

        // @cons
        protected TwoSlotMarker()
        {
            super(TYPE, StampFactory.forKind(JavaKind.Illegal));
        }
    }

    // @field
    protected final int localsSize;

    // @field
    protected final int stackSize;

    /**
     * @see BytecodeFrame#rethrowException
     */
    // @field
    protected final boolean rethrowException;

    // @field
    protected final boolean duringCall;

    @OptionalInput(value = InputType.State)
    // @field
    FrameState outerFrameState;

    /**
     * Contains the locals, the expressions and the locked objects, in this order.
     */
    @OptionalInput
    // @field
    NodeInputList<ValueNode> values;

    @Input(InputType.Association)
    // @field
    NodeInputList<MonitorIdNode> monitorIds;

    @OptionalInput(InputType.State)
    // @field
    NodeInputList<EscapeObjectState> virtualObjectMappings;

    /**
     * The bytecode index to which this frame state applies.
     */
    // @field
    public final int bci;

    /**
     * The bytecode to which this frame state applies.
     */
    // @field
    protected final Bytecode code;

    // @cons
    public FrameState(FrameState __outerFrameState, Bytecode __code, int __bci, int __localsSize, int __stackSize, int __lockSize, boolean __rethrowException, boolean __duringCall, List<MonitorIdNode> __monitorIds, List<EscapeObjectState> __virtualObjectMappings)
    {
        super(TYPE);
        if (__code != null)
        {
            /*
             * Make sure the bci is within range of the bytecodes. If the code size is 0 then allow
             * any value, otherwise the bci must be less than the code size. Any negative value is
             * also allowed to represent special bytecode states.
             */
            int __codeSize = __code.getCodeSize();
            if (__codeSize != 0 && __bci >= __codeSize)
            {
                throw new GraalError("bci %d is out of range for %s %d bytes", __bci, __code.getMethod().format("%H.%n(%p)"), __codeSize);
            }
        }
        this.outerFrameState = __outerFrameState;
        this.code = __code;
        this.bci = __bci;
        this.localsSize = __localsSize;
        this.stackSize = __stackSize;
        this.values = new NodeInputList<>(this, __localsSize + __stackSize + __lockSize);

        if (__monitorIds != null && __monitorIds.size() > 0)
        {
            this.monitorIds = new NodeInputList<>(this, __monitorIds);
        }

        if (__virtualObjectMappings != null && __virtualObjectMappings.size() > 0)
        {
            this.virtualObjectMappings = new NodeInputList<>(this, __virtualObjectMappings);
        }

        this.rethrowException = __rethrowException;
        this.duringCall = __duringCall;
    }

    // @cons
    public FrameState(FrameState __outerFrameState, Bytecode __code, int __bci, List<ValueNode> __values, int __localsSize, int __stackSize, boolean __rethrowException, boolean __duringCall, List<MonitorIdNode> __monitorIds, List<EscapeObjectState> __virtualObjectMappings)
    {
        this(__outerFrameState, __code, __bci, __localsSize, __stackSize, __values.size() - __localsSize - __stackSize, __rethrowException, __duringCall, __monitorIds, __virtualObjectMappings);
        for (int __i = 0; __i < __values.size(); ++__i)
        {
            this.values.initialize(__i, __values.get(__i));
        }
    }

    // @cons
    public FrameState(int __bci)
    {
        this(null, null, __bci, 0, 0, 0, false, false, null, Collections.<EscapeObjectState> emptyList());
    }

    /**
     * Creates a placeholder frame state with a single element on the stack representing a return
     * value or thrown exception. This allows the parsing of an intrinsic to communicate the
     * returned or thrown value in a {@link StateSplit#stateAfter() stateAfter} to the inlining call site.
     *
     * @param bci this must be {@link BytecodeFrame#AFTER_BCI}
     */
    // @cons
    public FrameState(int __bci, ValueNode __returnValueOrExceptionObject)
    {
        this(null, null, __bci, 0, __returnValueOrExceptionObject.getStackKind().getSlotCount(), 0, __returnValueOrExceptionObject instanceof ExceptionObjectNode, false, null, Collections.<EscapeObjectState> emptyList());
        this.values.initialize(0, __returnValueOrExceptionObject);
    }

    // @cons
    public FrameState(FrameState __outerFrameState, Bytecode __code, int __bci, ValueNode[] __locals, ValueNode[] __stack, int __stackSize, ValueNode[] __locks, List<MonitorIdNode> __monitorIds, boolean __rethrowException, boolean __duringCall)
    {
        this(__outerFrameState, __code, __bci, __locals.length, __stackSize, __locks.length, __rethrowException, __duringCall, __monitorIds, Collections.<EscapeObjectState> emptyList());
        createValues(__locals, __stack, __locks);
    }

    private void createValues(ValueNode[] __locals, ValueNode[] __stack, ValueNode[] __locks)
    {
        int __index = 0;
        for (int __i = 0; __i < __locals.length; ++__i)
        {
            ValueNode __value = __locals[__i];
            if (__value == TWO_SLOT_MARKER)
            {
                __value = null;
            }
            this.values.initialize(__index++, __value);
        }
        for (int __i = 0; __i < stackSize; ++__i)
        {
            ValueNode __value = __stack[__i];
            if (__value == TWO_SLOT_MARKER)
            {
                __value = null;
            }
            this.values.initialize(__index++, __value);
        }
        for (int __i = 0; __i < __locks.length; ++__i)
        {
            ValueNode __value = __locks[__i];
            this.values.initialize(__index++, __value);
        }
    }

    public NodeInputList<ValueNode> values()
    {
        return values;
    }

    public NodeInputList<MonitorIdNode> monitorIds()
    {
        return monitorIds;
    }

    public FrameState outerFrameState()
    {
        return outerFrameState;
    }

    public void setOuterFrameState(FrameState __x)
    {
        updateUsages(this.outerFrameState, __x);
        this.outerFrameState = __x;
    }

    /**
     * @see BytecodeFrame#rethrowException
     */
    public boolean rethrowException()
    {
        return rethrowException;
    }

    public boolean duringCall()
    {
        return duringCall;
    }

    public Bytecode getCode()
    {
        return code;
    }

    public ResolvedJavaMethod getMethod()
    {
        return code == null ? null : code.getMethod();
    }

    /**
     * Determines if this frame state can be converted to a {@link BytecodeFrame}.
     *
     * Since a {@link BytecodeFrame} encodes {@link #getMethod()} and {@link #bci}, it does not
     * preserve {@link #getCode()}. {@link #bci} is only guaranteed to be valid in terms of
     * {@code getCode().getCode()} which may be different from {@code getMethod().getCode()} if the
     * latter has been subject to instrumentation.
     */
    public boolean canProduceBytecodeFrame()
    {
        return code != null && code.getCode() == code.getMethod().getCode();
    }

    public void addVirtualObjectMapping(EscapeObjectState __virtualObject)
    {
        if (virtualObjectMappings == null)
        {
            virtualObjectMappings = new NodeInputList<>(this);
        }
        virtualObjectMappings.add(__virtualObject);
    }

    public int virtualObjectMappingCount()
    {
        if (virtualObjectMappings == null)
        {
            return 0;
        }
        return virtualObjectMappings.size();
    }

    public EscapeObjectState virtualObjectMappingAt(int __i)
    {
        return virtualObjectMappings.get(__i);
    }

    public NodeInputList<EscapeObjectState> virtualObjectMappings()
    {
        return virtualObjectMappings;
    }

    /**
     * Gets a copy of this frame state.
     */
    public FrameState duplicate(int __newBci)
    {
        return graph().add(new FrameState(outerFrameState(), code, __newBci, values, localsSize, stackSize, rethrowException, duringCall, monitorIds, virtualObjectMappings));
    }

    /**
     * Gets a copy of this frame state.
     */
    public FrameState duplicate()
    {
        return duplicate(bci);
    }

    /**
     * Duplicates a FrameState, along with a deep copy of all connected VirtualState (outer
     * FrameStates, VirtualObjectStates, ...).
     */
    @Override
    public FrameState duplicateWithVirtualState()
    {
        FrameState __newOuterFrameState = outerFrameState();
        if (__newOuterFrameState != null)
        {
            __newOuterFrameState = __newOuterFrameState.duplicateWithVirtualState();
        }
        ArrayList<EscapeObjectState> __newVirtualMappings = null;
        if (virtualObjectMappings != null)
        {
            __newVirtualMappings = new ArrayList<>(virtualObjectMappings.size());
            for (EscapeObjectState __state : virtualObjectMappings)
            {
                __newVirtualMappings.add(__state.duplicateWithVirtualState());
            }
        }
        return graph().add(new FrameState(__newOuterFrameState, code, bci, values, localsSize, stackSize, rethrowException, duringCall, monitorIds, __newVirtualMappings));
    }

    /**
     * Creates a copy of this frame state with one stack element of type {@code popKind} popped from
     * the stack.
     */
    public FrameState duplicateModifiedDuringCall(int __newBci, JavaKind __popKind)
    {
        return duplicateModified(graph(), __newBci, rethrowException, true, __popKind, null, null);
    }

    public FrameState duplicateModifiedBeforeCall(int __newBci, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        return duplicateModified(graph(), __newBci, rethrowException, false, __popKind, __pushedSlotKinds, __pushedValues);
    }

    /**
     * Creates a copy of this frame state with one stack element of type {@code popKind} popped from
     * the stack and the values in {@code pushedValues} pushed on the stack. The
     * {@code pushedValues} will be formatted correctly in slot encoding: a long or double will be
     * followed by a null slot.
     */
    public FrameState duplicateModified(int __newBci, boolean __newRethrowException, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        return duplicateModified(graph(), __newBci, __newRethrowException, duringCall, __popKind, __pushedSlotKinds, __pushedValues);
    }

    public FrameState duplicateModified(int __newBci, boolean __newRethrowException, boolean __newDuringCall, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        return duplicateModified(graph(), __newBci, __newRethrowException, __newDuringCall, __popKind, __pushedSlotKinds, __pushedValues);
    }

    /**
     * Creates a copy of this frame state with the top of stack replaced with with
     * {@code pushedValue} which must be of type {@code popKind}.
     */
    public FrameState duplicateModified(JavaKind __popKind, JavaKind __pushedSlotKind, ValueNode __pushedValue)
    {
        return duplicateModified(graph(), bci, rethrowException, duringCall, __popKind, new JavaKind[] { __pushedSlotKind }, new ValueNode[] { __pushedValue });
    }

    /**
     * Creates a copy of this frame state with one stack element of type popKind popped from the
     * stack and the values in pushedValues pushed on the stack. The pushedValues will be formatted
     * correctly in slot encoding: a long or double will be followed by a null slot. The bci will be
     * changed to newBci.
     */
    public FrameState duplicateModified(StructuredGraph __graph, int __newBci, boolean __newRethrowException, boolean __newDuringCall, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        ArrayList<ValueNode> __copy;
        if (__newRethrowException && !rethrowException && __popKind == JavaKind.Void)
        {
            __copy = new ArrayList<>(values.subList(0, localsSize));
        }
        else
        {
            __copy = new ArrayList<>(values.subList(0, localsSize + stackSize));
            if (__popKind != JavaKind.Void)
            {
                if (stackAt(stackSize() - 1) == null)
                {
                    __copy.remove(__copy.size() - 1);
                }
                ValueNode __lastSlot = __copy.get(__copy.size() - 1);
                __copy.remove(__copy.size() - 1);
            }
        }
        if (__pushedValues != null)
        {
            for (int __i = 0; __i < __pushedValues.length; __i++)
            {
                __copy.add(__pushedValues[__i]);
                if (__pushedSlotKinds[__i].needsTwoSlots())
                {
                    __copy.add(null);
                }
            }
        }
        int __newStackSize = __copy.size() - localsSize;
        __copy.addAll(values.subList(localsSize + stackSize, values.size()));

        return __graph.add(new FrameState(outerFrameState(), code, __newBci, __copy, localsSize, __newStackSize, __newRethrowException, __newDuringCall, monitorIds, virtualObjectMappings));
    }

    /**
     * Gets the size of the local variables.
     */
    public int localsSize()
    {
        return localsSize;
    }

    /**
     * Gets the current size (height) of the stack.
     */
    public int stackSize()
    {
        return stackSize;
    }

    /**
     * Gets the number of locked monitors in this frame state.
     */
    public int locksSize()
    {
        return values.size() - localsSize - stackSize;
    }

    /**
     * Gets the number of locked monitors in this frame state and all {@linkplain #outerFrameState() outer} frame states.
     */
    public int nestedLockDepth()
    {
        int __depth = locksSize();
        for (FrameState __outer = outerFrameState(); __outer != null; __outer = __outer.outerFrameState())
        {
            __depth += __outer.locksSize();
        }
        return __depth;
    }

    /**
     * Gets the value in the local variables at the specified index.
     *
     * @param i the index into the locals
     * @return the instruction that produced the value for the specified local
     */
    public ValueNode localAt(int __i)
    {
        return values.get(__i);
    }

    /**
     * Get the value on the stack at the specified stack index.
     *
     * @param i the index into the stack, with {@code 0} being the bottom of the stack
     * @return the instruction at the specified position in the stack
     */
    public ValueNode stackAt(int __i)
    {
        return values.get(localsSize + __i);
    }

    /**
     * Get the monitor owner at the specified index.
     *
     * @param i the index into the list of locked monitors.
     * @return the lock owner at the given index.
     */
    public ValueNode lockAt(int __i)
    {
        return values.get(localsSize + stackSize + __i);
    }

    /**
     * Get the MonitorIdNode that corresponds to the locked object at the specified index.
     */
    public MonitorIdNode monitorIdAt(int __i)
    {
        return monitorIds.get(__i);
    }

    public int monitorIdCount()
    {
        if (monitorIds == null)
        {
            return 0;
        }
        else
        {
            return monitorIds.size();
        }
    }

    public NodeIterable<FrameState> innerFrameStates()
    {
        return usages().filter(FrameState.class);
    }

    private int outerLockDepth()
    {
        int __depth = 0;
        FrameState __outer = outerFrameState;
        while (__outer != null)
        {
            __depth += __outer.monitorIdCount();
            __outer = __outer.outerFrameState;
        }
        return __depth;
    }

    @Override
    public void applyToNonVirtual(NodeClosure<? super ValueNode> __closure)
    {
        for (ValueNode __value : values)
        {
            if (__value != null)
            {
                __closure.apply(this, __value);
            }
        }

        if (monitorIds != null)
        {
            for (MonitorIdNode __monitorId : monitorIds)
            {
                if (__monitorId != null)
                {
                    __closure.apply(this, __monitorId);
                }
            }
        }

        if (virtualObjectMappings != null)
        {
            for (EscapeObjectState __state : virtualObjectMappings)
            {
                __state.applyToNonVirtual(__closure);
            }
        }

        if (outerFrameState() != null)
        {
            outerFrameState().applyToNonVirtual(__closure);
        }
    }

    @Override
    public void applyToVirtual(VirtualClosure __closure)
    {
        __closure.apply(this);
        if (virtualObjectMappings != null)
        {
            for (EscapeObjectState __state : virtualObjectMappings)
            {
                __state.applyToVirtual(__closure);
            }
        }
        if (outerFrameState() != null)
        {
            outerFrameState().applyToVirtual(__closure);
        }
    }

    @Override
    public boolean isPartOfThisState(VirtualState __state)
    {
        if (__state == this)
        {
            return true;
        }
        if (outerFrameState() != null && outerFrameState().isPartOfThisState(__state))
        {
            return true;
        }
        if (virtualObjectMappings != null)
        {
            for (EscapeObjectState __objectState : virtualObjectMappings)
            {
                if (__objectState.isPartOfThisState(__state))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
