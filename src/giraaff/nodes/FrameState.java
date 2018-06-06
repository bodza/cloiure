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
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.java.ExceptionObjectNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.util.GraalError;

///
// The {@code FrameState} class encapsulates the frame state (i.e. local variables and operand
// stack) at a particular point in the abstract interpretation.
//
// This can be used as debug or deoptimization information.
///
// @class FrameState
public final class FrameState extends VirtualState implements IterableNodeType
{
    // @def
    public static final NodeClass<FrameState> TYPE = NodeClass.create(FrameState.class);

    ///
    // Marker value for the second slot of values that occupy two local variable or expression stack
    // slots. The marker value is used by the bytecode parser, but replaced with {@code null} in the
    // {@link #values} of the {@link FrameState}.
    ///
    // @def
    public static final ValueNode TWO_SLOT_MARKER = new FrameState.TwoSlotMarker();

    // @class FrameState.TwoSlotMarker
    private static final class TwoSlotMarker extends ValueNode
    {
        // @def
        public static final NodeClass<FrameState.TwoSlotMarker> TYPE = NodeClass.create(FrameState.TwoSlotMarker.class);

        // @cons FrameState.TwoSlotMarker
        protected TwoSlotMarker()
        {
            super(TYPE, StampFactory.forKind(JavaKind.Illegal));
        }
    }

    // @field
    protected final int ___localsSize;

    // @field
    protected final int ___stackSize;

    ///
    // @see BytecodeFrame#rethrowException
    ///
    // @field
    protected final boolean ___rethrowException;

    // @field
    protected final boolean ___duringCall;

    @Node.OptionalInput(value = InputType.StateI)
    // @field
    FrameState ___outerFrameState;

    ///
    // Contains the locals, the expressions and the locked objects, in this order.
    ///
    @Node.OptionalInput
    // @field
    NodeInputList<ValueNode> ___values;

    @Node.Input(InputType.Association)
    // @field
    NodeInputList<MonitorIdNode> ___monitorIds;

    @Node.OptionalInput(InputType.StateI)
    // @field
    NodeInputList<EscapeObjectState> ___virtualObjectMappings;

    ///
    // The bytecode index to which this frame state applies.
    ///
    // @field
    public final int ___bci;

    ///
    // The bytecode to which this frame state applies.
    ///
    // @field
    protected final Bytecode ___code;

    // @cons FrameState
    public FrameState(FrameState __outerFrameState, Bytecode __code, int __bci, int __localsSize, int __stackSize, int __lockSize, boolean __rethrowException, boolean __duringCall, List<MonitorIdNode> __monitorIds, List<EscapeObjectState> __virtualObjectMappings)
    {
        super(TYPE);
        if (__code != null)
        {
            // Make sure the bci is within range of the bytecodes. If the code size is 0 then allow
            // any value, otherwise the bci must be less than the code size. Any negative value is
            // also allowed to represent special bytecode states.
            int __codeSize = __code.getCodeSize();
            if (__codeSize != 0 && __bci >= __codeSize)
            {
                throw new GraalError("bci %d is out of range for %s %d bytes", __bci, __code.getMethod().format("%H.%n(%p)"), __codeSize);
            }
        }
        this.___outerFrameState = __outerFrameState;
        this.___code = __code;
        this.___bci = __bci;
        this.___localsSize = __localsSize;
        this.___stackSize = __stackSize;
        this.___values = new NodeInputList<>(this, __localsSize + __stackSize + __lockSize);

        if (__monitorIds != null && __monitorIds.size() > 0)
        {
            this.___monitorIds = new NodeInputList<>(this, __monitorIds);
        }

        if (__virtualObjectMappings != null && __virtualObjectMappings.size() > 0)
        {
            this.___virtualObjectMappings = new NodeInputList<>(this, __virtualObjectMappings);
        }

        this.___rethrowException = __rethrowException;
        this.___duringCall = __duringCall;
    }

    // @cons FrameState
    public FrameState(FrameState __outerFrameState, Bytecode __code, int __bci, List<ValueNode> __values, int __localsSize, int __stackSize, boolean __rethrowException, boolean __duringCall, List<MonitorIdNode> __monitorIds, List<EscapeObjectState> __virtualObjectMappings)
    {
        this(__outerFrameState, __code, __bci, __localsSize, __stackSize, __values.size() - __localsSize - __stackSize, __rethrowException, __duringCall, __monitorIds, __virtualObjectMappings);
        for (int __i = 0; __i < __values.size(); ++__i)
        {
            this.___values.initialize(__i, __values.get(__i));
        }
    }

    // @cons FrameState
    public FrameState(int __bci)
    {
        this(null, null, __bci, 0, 0, 0, false, false, null, Collections.<EscapeObjectState> emptyList());
    }

    ///
    // Creates a placeholder frame state with a single element on the stack representing a return
    // value or thrown exception. This allows the parsing of an intrinsic to communicate the
    // returned or thrown value in a {@link StateSplit#stateAfter() stateAfter} to the inlining call site.
    //
    // @param bci this must be {@link BytecodeFrame#AFTER_BCI}
    ///
    // @cons FrameState
    public FrameState(int __bci, ValueNode __returnValueOrExceptionObject)
    {
        this(null, null, __bci, 0, __returnValueOrExceptionObject.getStackKind().getSlotCount(), 0, __returnValueOrExceptionObject instanceof ExceptionObjectNode, false, null, Collections.<EscapeObjectState> emptyList());
        this.___values.initialize(0, __returnValueOrExceptionObject);
    }

    // @cons FrameState
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
            this.___values.initialize(__index++, __value);
        }
        for (int __i = 0; __i < this.___stackSize; ++__i)
        {
            ValueNode __value = __stack[__i];
            if (__value == TWO_SLOT_MARKER)
            {
                __value = null;
            }
            this.___values.initialize(__index++, __value);
        }
        for (int __i = 0; __i < __locks.length; ++__i)
        {
            ValueNode __value = __locks[__i];
            this.___values.initialize(__index++, __value);
        }
    }

    public NodeInputList<ValueNode> values()
    {
        return this.___values;
    }

    public NodeInputList<MonitorIdNode> monitorIds()
    {
        return this.___monitorIds;
    }

    public FrameState outerFrameState()
    {
        return this.___outerFrameState;
    }

    public void setOuterFrameState(FrameState __x)
    {
        updateUsages(this.___outerFrameState, __x);
        this.___outerFrameState = __x;
    }

    ///
    // @see BytecodeFrame#rethrowException
    ///
    public boolean rethrowException()
    {
        return this.___rethrowException;
    }

    public boolean duringCall()
    {
        return this.___duringCall;
    }

    public Bytecode getCode()
    {
        return this.___code;
    }

    public ResolvedJavaMethod getMethod()
    {
        return this.___code == null ? null : this.___code.getMethod();
    }

    ///
    // Determines if this frame state can be converted to a {@link BytecodeFrame}.
    //
    // Since a {@link BytecodeFrame} encodes {@link #getMethod()} and {@link #bci}, it does not
    // preserve {@link #getCode()}. {@link #bci} is only guaranteed to be valid in terms of
    // {@code getCode().getCode()} which may be different from {@code getMethod().getCode()} if the
    // latter has been subject to instrumentation.
    ///
    public boolean canProduceBytecodeFrame()
    {
        return this.___code != null && this.___code.getCode() == this.___code.getMethod().getCode();
    }

    public void addVirtualObjectMapping(EscapeObjectState __virtualObject)
    {
        if (this.___virtualObjectMappings == null)
        {
            this.___virtualObjectMappings = new NodeInputList<>(this);
        }
        this.___virtualObjectMappings.add(__virtualObject);
    }

    public int virtualObjectMappingCount()
    {
        if (this.___virtualObjectMappings == null)
        {
            return 0;
        }
        return this.___virtualObjectMappings.size();
    }

    public EscapeObjectState virtualObjectMappingAt(int __i)
    {
        return this.___virtualObjectMappings.get(__i);
    }

    public NodeInputList<EscapeObjectState> virtualObjectMappings()
    {
        return this.___virtualObjectMappings;
    }

    ///
    // Gets a copy of this frame state.
    ///
    public FrameState duplicate(int __newBci)
    {
        return graph().add(new FrameState(outerFrameState(), this.___code, __newBci, this.___values, this.___localsSize, this.___stackSize, this.___rethrowException, this.___duringCall, this.___monitorIds, this.___virtualObjectMappings));
    }

    ///
    // Gets a copy of this frame state.
    ///
    public FrameState duplicate()
    {
        return duplicate(this.___bci);
    }

    ///
    // Duplicates a FrameState, along with a deep copy of all connected VirtualState (outer
    // FrameStates, VirtualObjectStates, ...).
    ///
    @Override
    public FrameState duplicateWithVirtualState()
    {
        FrameState __newOuterFrameState = outerFrameState();
        if (__newOuterFrameState != null)
        {
            __newOuterFrameState = __newOuterFrameState.duplicateWithVirtualState();
        }
        ArrayList<EscapeObjectState> __newVirtualMappings = null;
        if (this.___virtualObjectMappings != null)
        {
            __newVirtualMappings = new ArrayList<>(this.___virtualObjectMappings.size());
            for (EscapeObjectState __state : this.___virtualObjectMappings)
            {
                __newVirtualMappings.add(__state.duplicateWithVirtualState());
            }
        }
        return graph().add(new FrameState(__newOuterFrameState, this.___code, this.___bci, this.___values, this.___localsSize, this.___stackSize, this.___rethrowException, this.___duringCall, this.___monitorIds, __newVirtualMappings));
    }

    ///
    // Creates a copy of this frame state with one stack element of type {@code popKind} popped from
    // the stack.
    ///
    public FrameState duplicateModifiedDuringCall(int __newBci, JavaKind __popKind)
    {
        return duplicateModified(graph(), __newBci, this.___rethrowException, true, __popKind, null, null);
    }

    public FrameState duplicateModifiedBeforeCall(int __newBci, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        return duplicateModified(graph(), __newBci, this.___rethrowException, false, __popKind, __pushedSlotKinds, __pushedValues);
    }

    ///
    // Creates a copy of this frame state with one stack element of type {@code popKind} popped from
    // the stack and the values in {@code pushedValues} pushed on the stack. The
    // {@code pushedValues} will be formatted correctly in slot encoding: a long or double will be
    // followed by a null slot.
    ///
    public FrameState duplicateModified(int __newBci, boolean __newRethrowException, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        return duplicateModified(graph(), __newBci, __newRethrowException, this.___duringCall, __popKind, __pushedSlotKinds, __pushedValues);
    }

    public FrameState duplicateModified(int __newBci, boolean __newRethrowException, boolean __newDuringCall, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        return duplicateModified(graph(), __newBci, __newRethrowException, __newDuringCall, __popKind, __pushedSlotKinds, __pushedValues);
    }

    ///
    // Creates a copy of this frame state with the top of stack replaced with with
    // {@code pushedValue} which must be of type {@code popKind}.
    ///
    public FrameState duplicateModified(JavaKind __popKind, JavaKind __pushedSlotKind, ValueNode __pushedValue)
    {
        return duplicateModified(graph(), this.___bci, this.___rethrowException, this.___duringCall, __popKind, new JavaKind[] { __pushedSlotKind }, new ValueNode[] { __pushedValue });
    }

    ///
    // Creates a copy of this frame state with one stack element of type popKind popped from the
    // stack and the values in pushedValues pushed on the stack. The pushedValues will be formatted
    // correctly in slot encoding: a long or double will be followed by a null slot. The bci will be
    // changed to newBci.
    ///
    public FrameState duplicateModified(StructuredGraph __graph, int __newBci, boolean __newRethrowException, boolean __newDuringCall, JavaKind __popKind, JavaKind[] __pushedSlotKinds, ValueNode[] __pushedValues)
    {
        ArrayList<ValueNode> __copy;
        if (__newRethrowException && !this.___rethrowException && __popKind == JavaKind.Void)
        {
            __copy = new ArrayList<>(this.___values.subList(0, this.___localsSize));
        }
        else
        {
            __copy = new ArrayList<>(this.___values.subList(0, this.___localsSize + this.___stackSize));
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
        int __newStackSize = __copy.size() - this.___localsSize;
        __copy.addAll(this.___values.subList(this.___localsSize + this.___stackSize, this.___values.size()));

        return __graph.add(new FrameState(outerFrameState(), this.___code, __newBci, __copy, this.___localsSize, __newStackSize, __newRethrowException, __newDuringCall, this.___monitorIds, this.___virtualObjectMappings));
    }

    ///
    // Gets the size of the local variables.
    ///
    public int localsSize()
    {
        return this.___localsSize;
    }

    ///
    // Gets the current size (height) of the stack.
    ///
    public int stackSize()
    {
        return this.___stackSize;
    }

    ///
    // Gets the number of locked monitors in this frame state.
    ///
    public int locksSize()
    {
        return this.___values.size() - this.___localsSize - this.___stackSize;
    }

    ///
    // Gets the number of locked monitors in this frame state and all {@linkplain #outerFrameState() outer} frame states.
    ///
    public int nestedLockDepth()
    {
        int __depth = locksSize();
        for (FrameState __outer = outerFrameState(); __outer != null; __outer = __outer.outerFrameState())
        {
            __depth += __outer.locksSize();
        }
        return __depth;
    }

    ///
    // Gets the value in the local variables at the specified index.
    //
    // @param i the index into the locals
    // @return the instruction that produced the value for the specified local
    ///
    public ValueNode localAt(int __i)
    {
        return this.___values.get(__i);
    }

    ///
    // Get the value on the stack at the specified stack index.
    //
    // @param i the index into the stack, with {@code 0} being the bottom of the stack
    // @return the instruction at the specified position in the stack
    ///
    public ValueNode stackAt(int __i)
    {
        return this.___values.get(this.___localsSize + __i);
    }

    ///
    // Get the monitor owner at the specified index.
    //
    // @param i the index into the list of locked monitors.
    // @return the lock owner at the given index.
    ///
    public ValueNode lockAt(int __i)
    {
        return this.___values.get(this.___localsSize + this.___stackSize + __i);
    }

    ///
    // Get the MonitorIdNode that corresponds to the locked object at the specified index.
    ///
    public MonitorIdNode monitorIdAt(int __i)
    {
        return this.___monitorIds.get(__i);
    }

    public int monitorIdCount()
    {
        if (this.___monitorIds == null)
        {
            return 0;
        }
        else
        {
            return this.___monitorIds.size();
        }
    }

    public NodeIterable<FrameState> innerFrameStates()
    {
        return usages().filter(FrameState.class);
    }

    private int outerLockDepth()
    {
        int __depth = 0;
        FrameState __outer = this.___outerFrameState;
        while (__outer != null)
        {
            __depth += __outer.monitorIdCount();
            __outer = __outer.___outerFrameState;
        }
        return __depth;
    }

    @Override
    public void applyToNonVirtual(VirtualState.NodeClosure<? super ValueNode> __closure)
    {
        for (ValueNode __value : this.___values)
        {
            if (__value != null)
            {
                __closure.apply(this, __value);
            }
        }

        if (this.___monitorIds != null)
        {
            for (MonitorIdNode __monitorId : this.___monitorIds)
            {
                if (__monitorId != null)
                {
                    __closure.apply(this, __monitorId);
                }
            }
        }

        if (this.___virtualObjectMappings != null)
        {
            for (EscapeObjectState __state : this.___virtualObjectMappings)
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
    public void applyToVirtual(VirtualState.VirtualClosure __closure)
    {
        __closure.apply(this);
        if (this.___virtualObjectMappings != null)
        {
            for (EscapeObjectState __state : this.___virtualObjectMappings)
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
        if (this.___virtualObjectMappings != null)
        {
            for (EscapeObjectState __objectState : this.___virtualObjectMappings)
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
