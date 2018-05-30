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
    public static final NodeClass<FrameState> TYPE = NodeClass.create(FrameState.class);

    /**
     * Marker value for the second slot of values that occupy two local variable or expression stack
     * slots. The marker value is used by the bytecode parser, but replaced with {@code null} in the
     * {@link #values} of the {@link FrameState}.
     */
    public static final ValueNode TWO_SLOT_MARKER = new TwoSlotMarker();

    // @class FrameState.TwoSlotMarker
    private static final class TwoSlotMarker extends ValueNode
    {
        public static final NodeClass<TwoSlotMarker> TYPE = NodeClass.create(TwoSlotMarker.class);

        // @cons
        protected TwoSlotMarker()
        {
            super(TYPE, StampFactory.forKind(JavaKind.Illegal));
        }
    }

    protected final int localsSize;

    protected final int stackSize;

    /**
     * @see BytecodeFrame#rethrowException
     */
    protected final boolean rethrowException;

    protected final boolean duringCall;

    @OptionalInput(value = InputType.State) FrameState outerFrameState;

    /**
     * Contains the locals, the expressions and the locked objects, in this order.
     */
    @OptionalInput NodeInputList<ValueNode> values;

    @Input(InputType.Association) NodeInputList<MonitorIdNode> monitorIds;

    @OptionalInput(InputType.State) NodeInputList<EscapeObjectState> virtualObjectMappings;

    /**
     * The bytecode index to which this frame state applies.
     */
    public final int bci;

    /**
     * The bytecode to which this frame state applies.
     */
    protected final Bytecode code;

    // @cons
    public FrameState(FrameState outerFrameState, Bytecode code, int bci, int localsSize, int stackSize, int lockSize, boolean rethrowException, boolean duringCall, List<MonitorIdNode> monitorIds, List<EscapeObjectState> virtualObjectMappings)
    {
        super(TYPE);
        if (code != null)
        {
            /*
             * Make sure the bci is within range of the bytecodes. If the code size is 0 then allow
             * any value, otherwise the bci must be less than the code size. Any negative value is
             * also allowed to represent special bytecode states.
             */
            int codeSize = code.getCodeSize();
            if (codeSize != 0 && bci >= codeSize)
            {
                throw new GraalError("bci %d is out of range for %s %d bytes", bci, code.getMethod().format("%H.%n(%p)"), codeSize);
            }
        }
        this.outerFrameState = outerFrameState;
        this.code = code;
        this.bci = bci;
        this.localsSize = localsSize;
        this.stackSize = stackSize;
        this.values = new NodeInputList<>(this, localsSize + stackSize + lockSize);

        if (monitorIds != null && monitorIds.size() > 0)
        {
            this.monitorIds = new NodeInputList<>(this, monitorIds);
        }

        if (virtualObjectMappings != null && virtualObjectMappings.size() > 0)
        {
            this.virtualObjectMappings = new NodeInputList<>(this, virtualObjectMappings);
        }

        this.rethrowException = rethrowException;
        this.duringCall = duringCall;
    }

    // @cons
    public FrameState(FrameState outerFrameState, Bytecode code, int bci, List<ValueNode> values, int localsSize, int stackSize, boolean rethrowException, boolean duringCall, List<MonitorIdNode> monitorIds, List<EscapeObjectState> virtualObjectMappings)
    {
        this(outerFrameState, code, bci, localsSize, stackSize, values.size() - localsSize - stackSize, rethrowException, duringCall, monitorIds, virtualObjectMappings);
        for (int i = 0; i < values.size(); ++i)
        {
            this.values.initialize(i, values.get(i));
        }
    }

    // @cons
    public FrameState(int bci)
    {
        this(null, null, bci, 0, 0, 0, false, false, null, Collections.<EscapeObjectState> emptyList());
    }

    /**
     * Creates a placeholder frame state with a single element on the stack representing a return
     * value or thrown exception. This allows the parsing of an intrinsic to communicate the
     * returned or thrown value in a {@link StateSplit#stateAfter() stateAfter} to the inlining call site.
     *
     * @param bci this must be {@link BytecodeFrame#AFTER_BCI}
     */
    // @cons
    public FrameState(int bci, ValueNode returnValueOrExceptionObject)
    {
        this(null, null, bci, 0, returnValueOrExceptionObject.getStackKind().getSlotCount(), 0, returnValueOrExceptionObject instanceof ExceptionObjectNode, false, null, Collections.<EscapeObjectState> emptyList());
        this.values.initialize(0, returnValueOrExceptionObject);
    }

    // @cons
    public FrameState(FrameState outerFrameState, Bytecode code, int bci, ValueNode[] locals, ValueNode[] stack, int stackSize, ValueNode[] locks, List<MonitorIdNode> monitorIds, boolean rethrowException, boolean duringCall)
    {
        this(outerFrameState, code, bci, locals.length, stackSize, locks.length, rethrowException, duringCall, monitorIds, Collections.<EscapeObjectState> emptyList());
        createValues(locals, stack, locks);
    }

    private void createValues(ValueNode[] locals, ValueNode[] stack, ValueNode[] locks)
    {
        int index = 0;
        for (int i = 0; i < locals.length; ++i)
        {
            ValueNode value = locals[i];
            if (value == TWO_SLOT_MARKER)
            {
                value = null;
            }
            this.values.initialize(index++, value);
        }
        for (int i = 0; i < stackSize; ++i)
        {
            ValueNode value = stack[i];
            if (value == TWO_SLOT_MARKER)
            {
                value = null;
            }
            this.values.initialize(index++, value);
        }
        for (int i = 0; i < locks.length; ++i)
        {
            ValueNode value = locks[i];
            this.values.initialize(index++, value);
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

    public void setOuterFrameState(FrameState x)
    {
        updateUsages(this.outerFrameState, x);
        this.outerFrameState = x;
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

    public void addVirtualObjectMapping(EscapeObjectState virtualObject)
    {
        if (virtualObjectMappings == null)
        {
            virtualObjectMappings = new NodeInputList<>(this);
        }
        virtualObjectMappings.add(virtualObject);
    }

    public int virtualObjectMappingCount()
    {
        if (virtualObjectMappings == null)
        {
            return 0;
        }
        return virtualObjectMappings.size();
    }

    public EscapeObjectState virtualObjectMappingAt(int i)
    {
        return virtualObjectMappings.get(i);
    }

    public NodeInputList<EscapeObjectState> virtualObjectMappings()
    {
        return virtualObjectMappings;
    }

    /**
     * Gets a copy of this frame state.
     */
    public FrameState duplicate(int newBci)
    {
        return graph().add(new FrameState(outerFrameState(), code, newBci, values, localsSize, stackSize, rethrowException, duringCall, monitorIds, virtualObjectMappings));
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
        FrameState newOuterFrameState = outerFrameState();
        if (newOuterFrameState != null)
        {
            newOuterFrameState = newOuterFrameState.duplicateWithVirtualState();
        }
        ArrayList<EscapeObjectState> newVirtualMappings = null;
        if (virtualObjectMappings != null)
        {
            newVirtualMappings = new ArrayList<>(virtualObjectMappings.size());
            for (EscapeObjectState state : virtualObjectMappings)
            {
                newVirtualMappings.add(state.duplicateWithVirtualState());
            }
        }
        return graph().add(new FrameState(newOuterFrameState, code, bci, values, localsSize, stackSize, rethrowException, duringCall, monitorIds, newVirtualMappings));
    }

    /**
     * Creates a copy of this frame state with one stack element of type {@code popKind} popped from
     * the stack.
     */
    public FrameState duplicateModifiedDuringCall(int newBci, JavaKind popKind)
    {
        return duplicateModified(graph(), newBci, rethrowException, true, popKind, null, null);
    }

    public FrameState duplicateModifiedBeforeCall(int newBci, JavaKind popKind, JavaKind[] pushedSlotKinds, ValueNode[] pushedValues)
    {
        return duplicateModified(graph(), newBci, rethrowException, false, popKind, pushedSlotKinds, pushedValues);
    }

    /**
     * Creates a copy of this frame state with one stack element of type {@code popKind} popped from
     * the stack and the values in {@code pushedValues} pushed on the stack. The
     * {@code pushedValues} will be formatted correctly in slot encoding: a long or double will be
     * followed by a null slot.
     */
    public FrameState duplicateModified(int newBci, boolean newRethrowException, JavaKind popKind, JavaKind[] pushedSlotKinds, ValueNode[] pushedValues)
    {
        return duplicateModified(graph(), newBci, newRethrowException, duringCall, popKind, pushedSlotKinds, pushedValues);
    }

    public FrameState duplicateModified(int newBci, boolean newRethrowException, boolean newDuringCall, JavaKind popKind, JavaKind[] pushedSlotKinds, ValueNode[] pushedValues)
    {
        return duplicateModified(graph(), newBci, newRethrowException, newDuringCall, popKind, pushedSlotKinds, pushedValues);
    }

    /**
     * Creates a copy of this frame state with the top of stack replaced with with
     * {@code pushedValue} which must be of type {@code popKind}.
     */
    public FrameState duplicateModified(JavaKind popKind, JavaKind pushedSlotKind, ValueNode pushedValue)
    {
        return duplicateModified(graph(), bci, rethrowException, duringCall, popKind, new JavaKind[] { pushedSlotKind }, new ValueNode[] { pushedValue });
    }

    /**
     * Creates a copy of this frame state with one stack element of type popKind popped from the
     * stack and the values in pushedValues pushed on the stack. The pushedValues will be formatted
     * correctly in slot encoding: a long or double will be followed by a null slot. The bci will be
     * changed to newBci.
     */
    public FrameState duplicateModified(StructuredGraph graph, int newBci, boolean newRethrowException, boolean newDuringCall, JavaKind popKind, JavaKind[] pushedSlotKinds, ValueNode[] pushedValues)
    {
        ArrayList<ValueNode> copy;
        if (newRethrowException && !rethrowException && popKind == JavaKind.Void)
        {
            copy = new ArrayList<>(values.subList(0, localsSize));
        }
        else
        {
            copy = new ArrayList<>(values.subList(0, localsSize + stackSize));
            if (popKind != JavaKind.Void)
            {
                if (stackAt(stackSize() - 1) == null)
                {
                    copy.remove(copy.size() - 1);
                }
                ValueNode lastSlot = copy.get(copy.size() - 1);
                copy.remove(copy.size() - 1);
            }
        }
        if (pushedValues != null)
        {
            for (int i = 0; i < pushedValues.length; i++)
            {
                copy.add(pushedValues[i]);
                if (pushedSlotKinds[i].needsTwoSlots())
                {
                    copy.add(null);
                }
            }
        }
        int newStackSize = copy.size() - localsSize;
        copy.addAll(values.subList(localsSize + stackSize, values.size()));

        return graph.add(new FrameState(outerFrameState(), code, newBci, copy, localsSize, newStackSize, newRethrowException, newDuringCall, monitorIds, virtualObjectMappings));
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
        int depth = locksSize();
        for (FrameState outer = outerFrameState(); outer != null; outer = outer.outerFrameState())
        {
            depth += outer.locksSize();
        }
        return depth;
    }

    /**
     * Gets the value in the local variables at the specified index.
     *
     * @param i the index into the locals
     * @return the instruction that produced the value for the specified local
     */
    public ValueNode localAt(int i)
    {
        return values.get(i);
    }

    /**
     * Get the value on the stack at the specified stack index.
     *
     * @param i the index into the stack, with {@code 0} being the bottom of the stack
     * @return the instruction at the specified position in the stack
     */
    public ValueNode stackAt(int i)
    {
        return values.get(localsSize + i);
    }

    /**
     * Get the monitor owner at the specified index.
     *
     * @param i the index into the list of locked monitors.
     * @return the lock owner at the given index.
     */
    public ValueNode lockAt(int i)
    {
        return values.get(localsSize + stackSize + i);
    }

    /**
     * Get the MonitorIdNode that corresponds to the locked object at the specified index.
     */
    public MonitorIdNode monitorIdAt(int i)
    {
        return monitorIds.get(i);
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
        int depth = 0;
        FrameState outer = outerFrameState;
        while (outer != null)
        {
            depth += outer.monitorIdCount();
            outer = outer.outerFrameState;
        }
        return depth;
    }

    @Override
    public void applyToNonVirtual(NodeClosure<? super ValueNode> closure)
    {
        for (ValueNode value : values)
        {
            if (value != null)
            {
                closure.apply(this, value);
            }
        }

        if (monitorIds != null)
        {
            for (MonitorIdNode monitorId : monitorIds)
            {
                if (monitorId != null)
                {
                    closure.apply(this, monitorId);
                }
            }
        }

        if (virtualObjectMappings != null)
        {
            for (EscapeObjectState state : virtualObjectMappings)
            {
                state.applyToNonVirtual(closure);
            }
        }

        if (outerFrameState() != null)
        {
            outerFrameState().applyToNonVirtual(closure);
        }
    }

    @Override
    public void applyToVirtual(VirtualClosure closure)
    {
        closure.apply(this);
        if (virtualObjectMappings != null)
        {
            for (EscapeObjectState state : virtualObjectMappings)
            {
                state.applyToVirtual(closure);
            }
        }
        if (outerFrameState() != null)
        {
            outerFrameState().applyToVirtual(closure);
        }
    }

    @Override
    public boolean isPartOfThisState(VirtualState state)
    {
        if (state == this)
        {
            return true;
        }
        if (outerFrameState() != null && outerFrameState().isPartOfThisState(state))
        {
            return true;
        }
        if (virtualObjectMappings != null)
        {
            for (EscapeObjectState objectState : virtualObjectMappings)
            {
                if (objectState.isPartOfThisState(state))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
