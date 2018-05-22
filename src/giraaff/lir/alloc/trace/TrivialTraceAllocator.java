package giraaff.lir.alloc.trace;

import java.util.Arrays;
import java.util.EnumSet;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.alloc.Trace;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.StandardOp.JumpOp;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.ValueProcedure;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.ssa.SSAUtil;

/**
 * Allocates a trivial trace i.e. a trace consisting of a single block with no instructions other
 * than the {@link LabelOp} and the {@link JumpOp}.
 */
public final class TrivialTraceAllocator extends TraceAllocationPhase<TraceAllocationPhase.TraceAllocationContext>
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, TraceAllocationContext context)
    {
        LIR lir = lirGenRes.getLIR();
        AbstractBlockBase<?> block = trace.getBlocks()[0];
        AbstractBlockBase<?> pred = block.getPredecessors()[0];

        GlobalLivenessInfo livenessInfo = context.livenessInfo;
        allocate(block, pred, livenessInfo, SSAUtil.phiOutOrNull(lir, block));
    }

    public static void allocate(AbstractBlockBase<?> block, AbstractBlockBase<?> pred, GlobalLivenessInfo livenessInfo, LIRInstruction jump)
    {
        // exploit that the live sets are sorted

        // setup incoming variables/locations
        final int[] blockIn = livenessInfo.getBlockIn(block);
        final Value[] predLocOut = livenessInfo.getOutLocation(pred);
        int inLenght = blockIn.length;

        // setup outgoing variables/locations
        final int[] blockOut = livenessInfo.getBlockOut(block);
        int outLength = blockOut.length;
        final Value[] locationOut = new Value[outLength];

        for (int outIdx = 0, inIdx = 0; outIdx < outLength; inIdx++)
        {
            if (blockOut[outIdx] == blockIn[inIdx])
            {
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
        if (jump != null)
        {
            handlePhiOut(jump, blockIn, predLocOut);
        }
    }

    private static void handlePhiOut(LIRInstruction jump, int[] varIn, Value[] locIn)
    {
        // handle outgoing phi values
        ValueProcedure outputConsumer = new ValueProcedure()
        {
            @Override
            public Value doValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (LIRValueUtil.isVariable(value))
                {
                    // since incoming variables are sorted, we can do a binary search
                    return locIn[Arrays.binarySearch(varIn, LIRValueUtil.asVariable(value).index)];
                }
                return value;
            }
        };

        // Jumps have only alive values (outgoing phi values)
        jump.forEachAlive(outputConsumer);
    }
}
