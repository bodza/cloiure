package graalvm.compiler.lir.alloc.trace;

import java.util.ArrayList;

import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp.JumpOp;
import graalvm.compiler.lir.StandardOp.LabelOp;

import jdk.vm.ci.meta.Value;

public class TraceUtil {

    public static boolean isShadowedRegisterValue(Value value) {
        assert value != null;
        return value instanceof ShadowedRegisterValue;
    }

    public static ShadowedRegisterValue asShadowedRegisterValue(Value value) {
        assert isShadowedRegisterValue(value);
        return (ShadowedRegisterValue) value;
    }

    public static boolean isTrivialTrace(LIR lir, Trace trace) {
        if (trace.size() != 1) {
            return false;
        }
        ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(trace.getBlocks()[0]);
        if (instructions.size() != 2) {
            return false;
        }
        assert instructions.get(0) instanceof LabelOp : "First instruction not a LabelOp: " + instructions.get(0);
        if (((LabelOp) instructions.get(0)).isPhiIn()) {
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

    public static boolean hasInterTracePredecessor(TraceBuilderResult result, Trace trace, AbstractBlockBase<?> block) {
        assert result.getTraceForBlock(block).equals(trace);
        if (block.getPredecessorCount() == 0) {
            // start block
            return false;
        }
        if (block.getPredecessorCount() == 1) {
            return !result.getTraceForBlock(block.getPredecessors()[0]).equals(trace);
        }
        return true;
    }

    public static boolean hasInterTraceSuccessor(TraceBuilderResult result, Trace trace, AbstractBlockBase<?> block) {
        assert result.getTraceForBlock(block).equals(trace);
        if (block.getSuccessorCount() == 0) {
            // method end block
            return false;
        }
        if (block.getSuccessorCount() == 1) {
            return !result.getTraceForBlock(block.getSuccessors()[0]).equals(trace);
        }
        return true;
    }
}
