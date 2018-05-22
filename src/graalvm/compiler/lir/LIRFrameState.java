package graalvm.compiler.lir;

import java.util.EnumSet;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.DebugInfo;
import jdk.vm.ci.code.StackLockValue;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.code.VirtualObject;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.LIRValueUtil;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.util.IndexedValueMap;

/**
 * This class represents garbage collection and deoptimization information attached to a LIR
 * instruction.
 */
public class LIRFrameState
{
    public static final LIRFrameState NO_STATE = new LIRFrameState(null, null, null);

    public final BytecodeFrame topFrame;
    private final VirtualObject[] virtualObjects;
    public final LabelRef exceptionEdge;
    protected DebugInfo debugInfo;

    private IndexedValueMap liveBasePointers;

    public LIRFrameState(BytecodeFrame topFrame, VirtualObject[] virtualObjects, LabelRef exceptionEdge)
    {
        this.topFrame = topFrame;
        this.virtualObjects = virtualObjects;
        this.exceptionEdge = exceptionEdge;
    }

    public boolean hasDebugInfo()
    {
        return debugInfo != null;
    }

    public DebugInfo debugInfo()
    {
        return debugInfo;
    }

    /**
     * Iterates the frame state and calls the {@link InstructionValueProcedure} for every variable.
     *
     * @param proc The procedure called for variables.
     */
    public void forEachState(LIRInstruction inst, InstructionValueProcedure proc)
    {
        for (BytecodeFrame cur = topFrame; cur != null; cur = cur.caller())
        {
            processValues(inst, cur.values, proc);
        }
        if (virtualObjects != null)
        {
            for (VirtualObject obj : virtualObjects)
            {
                processValues(inst, obj.getValues(), proc);
            }
        }
        if (liveBasePointers != null)
        {
            liveBasePointers.forEach(inst, OperandMode.ALIVE, STATE_FLAGS, proc);
        }
    }

    /**
     * Iterates the frame state and calls the {@link InstructionValueConsumer} for every variable.
     *
     * @param proc The procedure called for variables.
     */
    public void visitEachState(LIRInstruction inst, InstructionValueConsumer proc)
    {
        for (BytecodeFrame cur = topFrame; cur != null; cur = cur.caller())
        {
            visitValues(inst, cur.values, proc);
        }
        if (virtualObjects != null)
        {
            for (VirtualObject obj : virtualObjects)
            {
                visitValues(inst, obj.getValues(), proc);
            }
        }
        if (liveBasePointers != null)
        {
            liveBasePointers.visitEach(inst, OperandMode.ALIVE, STATE_FLAGS, proc);
        }
    }

    /**
     * We filter out constant and illegal values ourself before calling the procedure, so
     * {@link OperandFlag#CONST} and {@link OperandFlag#ILLEGAL} need not be set.
     */
    protected static final EnumSet<OperandFlag> STATE_FLAGS = EnumSet.of(OperandFlag.REG, OperandFlag.STACK);

    protected void processValues(LIRInstruction inst, JavaValue[] values, InstructionValueProcedure proc)
    {
        for (int i = 0; i < values.length; i++)
        {
            JavaValue value = values[i];
            if (ValueUtil.isIllegalJavaValue(value))
            {
                continue;
            }
            if (value instanceof AllocatableValue)
            {
                AllocatableValue allocatable = (AllocatableValue) value;
                Value result = proc.doValue(inst, allocatable, OperandMode.ALIVE, STATE_FLAGS);
                if (!allocatable.identityEquals(result))
                {
                    values[i] = (JavaValue) result;
                }
            }
            else if (value instanceof StackLockValue)
            {
                StackLockValue monitor = (StackLockValue) value;
                JavaValue owner = monitor.getOwner();
                if (owner instanceof AllocatableValue)
                {
                    monitor.setOwner((JavaValue) proc.doValue(inst, (AllocatableValue) owner, OperandMode.ALIVE, STATE_FLAGS));
                }
                Value slot = monitor.getSlot();
                if (LIRValueUtil.isVirtualStackSlot(slot))
                {
                    monitor.setSlot(ValueUtil.asAllocatableValue(proc.doValue(inst, slot, OperandMode.ALIVE, STATE_FLAGS)));
                }
            }
        }
    }

    protected void visitValues(LIRInstruction inst, JavaValue[] values, InstructionValueConsumer proc)
    {
        for (int i = 0; i < values.length; i++)
        {
            JavaValue value = values[i];
            if (ValueUtil.isIllegalJavaValue(value))
            {
                continue;
            }
            else if (value instanceof AllocatableValue)
            {
                proc.visitValue(inst, (AllocatableValue) value, OperandMode.ALIVE, STATE_FLAGS);
            }
            else if (value instanceof StackLockValue)
            {
                StackLockValue monitor = (StackLockValue) value;
                JavaValue owner = monitor.getOwner();
                if (owner instanceof AllocatableValue)
                {
                    proc.visitValue(inst, (AllocatableValue) owner, OperandMode.ALIVE, STATE_FLAGS);
                }
                Value slot = monitor.getSlot();
                if (LIRValueUtil.isVirtualStackSlot(slot))
                {
                    proc.visitValue(inst, slot, OperandMode.ALIVE, STATE_FLAGS);
                }
            }
        }
    }

    private boolean unprocessed(JavaValue value)
    {
        if (ValueUtil.isIllegalJavaValue(value))
        {
            // Ignore dead local variables.
            return true;
        }
        else if (ValueUtil.isConstantJavaValue(value))
        {
            // Ignore constants, the register allocator does not need to see them.
            return true;
        }
        else if (ValueUtil.isVirtualObject(value))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Called by the register allocator to initialize the frame state.
     *
     * @param frameMap The frame map.
     * @param canHaveRegisters True if there can be any register map entries.
     */
    public void initDebugInfo(FrameMap frameMap, boolean canHaveRegisters)
    {
        debugInfo = new DebugInfo(topFrame, virtualObjects);
    }

    public IndexedValueMap getLiveBasePointers()
    {
        return liveBasePointers;
    }

    public void setLiveBasePointers(IndexedValueMap liveBasePointers)
    {
        this.liveBasePointers = liveBasePointers;
    }

    @Override
    public String toString()
    {
        return debugInfo != null ? debugInfo.toString() : topFrame != null ? topFrame.toString() : "<empty>";
    }
}
