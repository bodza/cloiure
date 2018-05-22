package giraaff.lir.alloc.trace;

import java.util.ArrayList;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.alloc.Trace;
import giraaff.core.common.alloc.TraceBuilderResult;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp.JumpOp;
import giraaff.lir.StandardOp.LabelOp;

public class TraceUtil
{
    public static boolean isShadowedRegisterValue(Value value)
    {
        return value instanceof ShadowedRegisterValue;
    }

    public static ShadowedRegisterValue asShadowedRegisterValue(Value value)
    {
        return (ShadowedRegisterValue) value;
    }

    public static boolean isTrivialTrace(LIR lir, Trace trace)
    {
        if (trace.size() != 1)
        {
            return false;
        }
        ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(trace.getBlocks()[0]);
        if (instructions.size() != 2)
        {
            return false;
        }
        if (((LabelOp) instructions.get(0)).isPhiIn())
        {
            /*
             * Merge blocks are in general not trivial block because variables defined by a PHI
             * always need a location. If the outgoing value of the predecessor is a constant we
             * need to find an appropriate location (register or stack).
             *
             * Note that this case should not happen in practice since the trace containing the
             * merge block should also contain one of the predecessors. For non-standard trace
             * builders (e.g. the single block trace builder) this is not the true, though.
             */
            return false;
        }
        /*
         * Now we need to check if the BlockEndOp has no special operand requirements (i.e.
         * stack-slot, register). For now we just check for JumpOp because we know that it doesn't.
         */
        return instructions.get(1) instanceof JumpOp;
    }

    public static boolean hasInterTracePredecessor(TraceBuilderResult result, Trace trace, AbstractBlockBase<?> block)
    {
        if (block.getPredecessorCount() == 0)
        {
            // start block
            return false;
        }
        if (block.getPredecessorCount() == 1)
        {
            return !result.getTraceForBlock(block.getPredecessors()[0]).equals(trace);
        }
        return true;
    }

    public static boolean hasInterTraceSuccessor(TraceBuilderResult result, Trace trace, AbstractBlockBase<?> block)
    {
        if (block.getSuccessorCount() == 0)
        {
            // method end block
            return false;
        }
        if (block.getSuccessorCount() == 1)
        {
            return !result.getTraceForBlock(block.getSuccessors()[0]).equals(trace);
        }
        return true;
    }
}
