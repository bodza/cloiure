package giraaff.lir.amd64.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.RedundantMoveElimination;
import giraaff.lir.amd64.AMD64Move.AMD64MultiStackMove;
import giraaff.lir.amd64.AMD64Move.AMD64StackMove;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

/**
 * Replaces sequential {@link AMD64StackMove}s of the same type with a single
 * {@link AMD64MultiStackMove} to avoid storing/restoring the scratch register multiple times.
 *
 * Note: this phase must be inserted <b>after</b> {@link RedundantMoveElimination} phase because
 * {@link AMD64MultiStackMove} are not probably detected.
 */
// @class StackMoveOptimizationPhase
public final class StackMoveOptimizationPhase extends PostAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PostAllocationOptimizationContext __context)
    {
        LIR __lir = __lirGenRes.getLIR();
        for (AbstractBlockBase<?> __block : __lir.getControlFlowGraph().getBlocks())
        {
            ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);
            new Closure().process(__instructions);
        }
    }

    // @class StackMoveOptimizationPhase.Closure
    private static class Closure
    {
        // @def
        private static final int NONE = -1;

        // @field
        private int begin = NONE;
        // @field
        private Register reg = null;
        // @field
        private List<AllocatableValue> dst;
        // @field
        private List<Value> src;
        // @field
        private AllocatableValue slot;
        // @field
        private boolean removed = false;

        public void process(List<LIRInstruction> __instructions)
        {
            for (int __i = 0; __i < __instructions.size(); __i++)
            {
                LIRInstruction __inst = __instructions.get(__i);

                if (isStackMove(__inst))
                {
                    AMD64StackMove __move = asStackMove(__inst);

                    if (reg != null && !reg.equals(__move.getScratchRegister()))
                    {
                        // end of trace & start of new
                        replaceStackMoves(__instructions);
                    }

                    // lazy initialize
                    if (dst == null)
                    {
                        dst = new ArrayList<>();
                        src = new ArrayList<>();
                    }

                    dst.add(__move.getResult());
                    src.add(__move.getInput());

                    if (begin == NONE)
                    {
                        // trace begin
                        begin = __i;
                        reg = __move.getScratchRegister();
                        slot = __move.getBackupSlot();
                    }
                }
                else if (begin != NONE)
                {
                    // end of trace
                    replaceStackMoves(__instructions);
                }
            }
            // remove instructions
            if (removed)
            {
                __instructions.removeAll(Collections.singleton(null));
            }
        }

        private void replaceStackMoves(List<LIRInstruction> __instructions)
        {
            int __size = dst.size();
            if (__size > 1)
            {
                AMD64MultiStackMove __multiMove = new AMD64MultiStackMove(dst.toArray(new AllocatableValue[__size]), src.toArray(new AllocatableValue[__size]), reg, slot);
                // replace first instruction
                __instructions.set(begin, __multiMove);
                // and null out others
                Collections.fill(__instructions.subList(begin + 1, begin + __size), null);
                // removed
                removed = true;
            }
            // reset
            dst.clear();
            src.clear();
            begin = NONE;
            reg = null;
            slot = null;
        }
    }

    private static AMD64StackMove asStackMove(LIRInstruction __inst)
    {
        return (AMD64StackMove) __inst;
    }

    private static boolean isStackMove(LIRInstruction __inst)
    {
        return __inst instanceof AMD64StackMove;
    }
}
