package graalvm.compiler.lir.alloc.trace;

import static graalvm.compiler.lir.LIRValueUtil.asVariable;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.alloc.trace.TraceUtil.isTrivialTrace;

import java.util.Arrays;
import java.util.EnumSet;

import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.StandardOp.JumpOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.ValueProcedure;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.ssa.SSAUtil;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;

/**
 * Allocates a trivial trace i.e. a trace consisting of a single block with no instructions other
 * than the {@link LabelOp} and the {@link JumpOp}.
 */
public final class TrivialTraceAllocator extends TraceAllocationPhase<TraceAllocationPhase.TraceAllocationContext> {

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, TraceAllocationContext context) {
        LIR lir = lirGenRes.getLIR();
        assert isTrivialTrace(lir, trace) : "Not a trivial trace! " + trace;
        AbstractBlockBase<?> block = trace.getBlocks()[0];
        assert TraceAssertions.singleHeadPredecessor(trace) : "Trace head with more than one predecessor?!" + trace;
        AbstractBlockBase<?> pred = block.getPredecessors()[0];

        GlobalLivenessInfo livenessInfo = context.livenessInfo;
        allocate(block, pred, livenessInfo, SSAUtil.phiOutOrNull(lir, block));
    }

    public static void allocate(AbstractBlockBase<?> block, AbstractBlockBase<?> pred, GlobalLivenessInfo livenessInfo, LIRInstruction jump) {
        // exploit that the live sets are sorted
        assert TraceAssertions.liveSetsAreSorted(livenessInfo, block);
        assert TraceAssertions.liveSetsAreSorted(livenessInfo, pred);

        // setup incoming variables/locations
        final int[] blockIn = livenessInfo.getBlockIn(block);
        final Value[] predLocOut = livenessInfo.getOutLocation(pred);
        int inLenght = blockIn.length;

        // setup outgoing variables/locations
        final int[] blockOut = livenessInfo.getBlockOut(block);
        int outLength = blockOut.length;
        final Value[] locationOut = new Value[outLength];

        assert outLength <= inLenght : "Trivial Trace! There cannot be more outgoing values than incoming.";
        for (int outIdx = 0, inIdx = 0; outIdx < outLength; inIdx++) {
            if (blockOut[outIdx] == blockIn[inIdx]) {
                // set the outgoing location to the incoming value
                locationOut[outIdx++] = predLocOut[inIdx];
            }
        }

        /*
         * Since we do not change any of the location we can just use the outgoing of the
         * predecessor.
         */
        livenessInfo.setInLocations(block, predLocOut);
        livenessInfo.setOutLocations(block, locationOut);
        if (jump != null) {
            handlePhiOut(jump, blockIn, predLocOut);
        }
    }

    private static void handlePhiOut(LIRInstruction jump, int[] varIn, Value[] locIn) {
        // handle outgoing phi values
        ValueProcedure outputConsumer = new ValueProcedure() {
            @Override
            public Value doValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags) {
                if (isVariable(value)) {
                    // since incoming variables are sorted, we can do a binary search
                    return locIn[Arrays.binarySearch(varIn, asVariable(value).index)];
                }
                return value;
            }
        };

        // Jumps have only alive values (outgoing phi values)
        jump.forEachAlive(outputConsumer);
    }

}
