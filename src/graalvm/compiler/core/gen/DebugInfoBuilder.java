package graalvm.compiler.core.gen;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.ConstantValue;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LabelRef;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeValueMap;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.nodes.virtual.EscapeObjectState;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.virtual.nodes.MaterializedObjectState;
import graalvm.compiler.virtual.nodes.VirtualObjectState;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.VirtualObject;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.Value;

/**
 * Builds {@link LIRFrameState}s from {@link FrameState}s.
 */
public class DebugInfoBuilder
{
    protected final NodeValueMap nodeValueMap;

    public DebugInfoBuilder(NodeValueMap nodeValueMap)
    {
        this.nodeValueMap = nodeValueMap;
    }

    private static final JavaValue[] NO_JAVA_VALUES = {};
    private static final JavaKind[] NO_JAVA_KINDS = {};

    protected final EconomicMap<VirtualObjectNode, VirtualObject> virtualObjects = EconomicMap.create(Equivalence.IDENTITY);
    protected final EconomicMap<VirtualObjectNode, EscapeObjectState> objectStates = EconomicMap.create(Equivalence.IDENTITY);

    protected final Queue<VirtualObjectNode> pendingVirtualObjects = new ArrayDeque<>();

    public LIRFrameState build(FrameState topState, LabelRef exceptionEdge)
    {
        // collect all VirtualObjectField instances:
        FrameState current = topState;
        do
        {
            if (current.virtualObjectMappingCount() > 0)
            {
                for (EscapeObjectState state : current.virtualObjectMappings())
                {
                    if (!objectStates.containsKey(state.object()))
                    {
                        if (!(state instanceof MaterializedObjectState) || ((MaterializedObjectState) state).materializedValue() != state.object())
                        {
                            objectStates.put(state.object(), state);
                        }
                    }
                }
            }
            current = current.outerFrameState();
        } while (current != null);

        BytecodeFrame frame = computeFrameForState(topState);

        VirtualObject[] virtualObjectsArray = null;
        if (virtualObjects.size() != 0)
        {
            // fill in the VirtualObject values
            VirtualObjectNode vobjNode;
            while ((vobjNode = pendingVirtualObjects.poll()) != null)
            {
                VirtualObject vobjValue = virtualObjects.get(vobjNode);

                JavaValue[] values;
                JavaKind[] slotKinds;
                int entryCount = vobjNode.entryCount();
                if (entryCount == 0)
                {
                    values = NO_JAVA_VALUES;
                    slotKinds = NO_JAVA_KINDS;
                }
                else
                {
                    values = new JavaValue[entryCount];
                    slotKinds = new JavaKind[entryCount];
                }
                if (values.length > 0)
                {
                    VirtualObjectState currentField = (VirtualObjectState) objectStates.get(vobjNode);
                    int pos = 0;
                    for (int i = 0; i < entryCount; i++)
                    {
                        ValueNode value = currentField.values().get(i);
                        if (value == null)
                        {
                            JavaKind entryKind = vobjNode.entryKind(i);
                            values[pos] = JavaConstant.defaultForKind(entryKind.getStackKind());
                            slotKinds[pos] = entryKind.getStackKind();
                            pos++;
                        }
                        else if (!value.isConstant() || value.asJavaConstant().getJavaKind() != JavaKind.Illegal)
                        {
                            values[pos] = toJavaValue(value);
                            slotKinds[pos] = toSlotKind(value);
                            pos++;
                        }
                        else
                        {
                            ValueNode previousValue = currentField.values().get(i - 1);
                            if (previousValue == null || !previousValue.getStackKind().needsTwoSlots())
                            {
                                // Don't allow the IllegalConstant to leak into the debug info
                                JavaKind entryKind = vobjNode.entryKind(i);
                                values[pos] = JavaConstant.defaultForKind(entryKind.getStackKind());
                                slotKinds[pos] = entryKind.getStackKind();
                                pos++;
                            }
                        }
                    }
                    if (pos != entryCount)
                    {
                        values = Arrays.copyOf(values, pos);
                        slotKinds = Arrays.copyOf(slotKinds, pos);
                    }
                }
                vobjValue.setValues(values, slotKinds);
            }

            virtualObjectsArray = new VirtualObject[virtualObjects.size()];
            int index = 0;
            for (VirtualObject value : virtualObjects.getValues())
            {
                virtualObjectsArray[index++] = value;
            }
            virtualObjects.clear();
        }
        objectStates.clear();

        return newLIRFrameState(exceptionEdge, frame, virtualObjectsArray);
    }

    /*
     * Customization point for subclasses. For example, Word types have a kind Object, but are
     * internally stored as a primitive value. We do not know about Word types here, but subclasses
     * do know.
     */
    protected JavaKind storageKind(JavaType type)
    {
        return type.getJavaKind();
    }

    protected LIRFrameState newLIRFrameState(LabelRef exceptionEdge, BytecodeFrame frame, VirtualObject[] virtualObjectsArray)
    {
        return new LIRFrameState(frame, virtualObjectsArray, exceptionEdge);
    }

    protected BytecodeFrame computeFrameForState(FrameState state)
    {
        try
        {
            int numLocals = state.localsSize();
            int numStack = state.stackSize();
            int numLocks = state.locksSize();

            int numValues = numLocals + numStack + numLocks;
            int numKinds = numLocals + numStack;

            JavaValue[] values = numValues == 0 ? NO_JAVA_VALUES : new JavaValue[numValues];
            JavaKind[] slotKinds = numKinds == 0 ? NO_JAVA_KINDS : new JavaKind[numKinds];
            computeLocals(state, numLocals, values, slotKinds);
            computeStack(state, numLocals, numStack, values, slotKinds);
            computeLocks(state, values);

            BytecodeFrame caller = null;
            if (state.outerFrameState() != null)
            {
                caller = computeFrameForState(state.outerFrameState());
            }

            if (!state.canProduceBytecodeFrame())
            {
                // This typically means a snippet or intrinsic frame state made it to the backend
                StackTraceElement ste = state.getCode().asStackTraceElement(state.bci);
                throw new GraalError("Frame state for %s cannot be converted to a BytecodeFrame since the frame state's code is " + "not the same as the frame state method's code", ste);
            }

            return new BytecodeFrame(caller, state.getMethod(), state.bci, state.rethrowException(), state.duringCall(), values, slotKinds, numLocals, numStack, numLocks);
        }
        catch (GraalError e)
        {
            throw e.addContext("FrameState: ", state);
        }
    }

    protected void computeLocals(FrameState state, int numLocals, JavaValue[] values, JavaKind[] slotKinds)
    {
        for (int i = 0; i < numLocals; i++)
        {
            ValueNode local = state.localAt(i);
            values[i] = toJavaValue(local);
            slotKinds[i] = toSlotKind(local);
        }
    }

    protected void computeStack(FrameState state, int numLocals, int numStack, JavaValue[] values, JavaKind[] slotKinds)
    {
        for (int i = 0; i < numStack; i++)
        {
            ValueNode stack = state.stackAt(i);
            values[numLocals + i] = toJavaValue(stack);
            slotKinds[numLocals + i] = toSlotKind(stack);
        }
    }

    protected void computeLocks(FrameState state, JavaValue[] values)
    {
        for (int i = 0; i < state.locksSize(); i++)
        {
            values[state.localsSize() + state.stackSize() + i] = computeLockValue(state, i);
        }
    }

    protected JavaValue computeLockValue(FrameState state, int i)
    {
        return toJavaValue(state.lockAt(i));
    }

    private static JavaKind toSlotKind(ValueNode value)
    {
        if (value == null)
        {
            return JavaKind.Illegal;
        }
        else
        {
            return value.getStackKind();
        }
    }

    protected JavaValue toJavaValue(ValueNode value)
    {
        try
        {
            if (value instanceof VirtualObjectNode)
            {
                VirtualObjectNode obj = (VirtualObjectNode) value;
                EscapeObjectState state = objectStates.get(obj);
                if (state == null && obj.entryCount() > 0)
                {
                    // null states occur for objects with 0 fields
                    throw new GraalError("no mapping found for virtual object %s", obj);
                }
                if (state instanceof MaterializedObjectState)
                {
                    return toJavaValue(((MaterializedObjectState) state).materializedValue());
                }
                else
                {
                    VirtualObject vobject = virtualObjects.get(obj);
                    if (vobject == null)
                    {
                        vobject = VirtualObject.get(obj.type(), virtualObjects.size());
                        virtualObjects.put(obj, vobject);
                        pendingVirtualObjects.add(obj);
                    }
                    return vobject;
                }
            }
            else
            {
                // Remove proxies from constants so the constant can be directly embedded.
                ValueNode unproxied = GraphUtil.unproxify(value);
                if (unproxied instanceof ConstantNode)
                {
                    return unproxied.asJavaConstant();
                }
                else if (value != null)
                {
                    Value operand = nodeValueMap.operand(value);
                    if (operand instanceof ConstantValue && ((ConstantValue) operand).isJavaConstant())
                    {
                        return ((ConstantValue) operand).getJavaConstant();
                    }
                    else
                    {
                        return (JavaValue) operand;
                    }
                }
                else
                {
                    // return a dummy value because real value not needed
                    return Value.ILLEGAL;
                }
            }
        }
        catch (GraalError e)
        {
            throw e.addContext("toValue: ", value);
        }
    }
}
