package giraaff.lir.amd64.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.RedundantMoveElimination;
import giraaff.lir.amd64.AMD64Move.AMD64MultiStackMove;
import giraaff.lir.amd64.AMD64Move.AMD64StackMove;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.PostAllocationOptimizationPhase;
import giraaff.options.NestedBooleanOptionKey;

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
    // @class StackMoveOptimizationPhase.Options
    public static final class Options
    {
        public static final NestedBooleanOptionKey LIROptStackMoveOptimizer = new NestedBooleanOptionKey(LIRPhase.Options.LIROptimization, true);
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PostAllocationOptimizationContext context)
    {
        LIR lir = lirGenRes.getLIR();
        for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks())
        {
            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
            new Closure().process(instructions);
        }
    }

    // @class StackMoveOptimizationPhase.Closure
    private static class Closure
    {
        private static final int NONE = -1;

        private int begin = NONE;
        private Register reg = null;
        private List<AllocatableValue> dst;
        private List<Value> src;
        private AllocatableValue slot;
        private boolean removed = false;

        public void process(List<LIRInstruction> instructions)
        {
            for (int i = 0; i < instructions.size(); i++)
            {
                LIRInstruction inst = instructions.get(i);

                if (isStackMove(inst))
                {
                    AMD64StackMove move = asStackMove(inst);

                    if (reg != null && !reg.equals(move.getScratchRegister()))
                    {
                        // end of trace & start of new
                        replaceStackMoves(instructions);
                    }

                    // lazy initialize
                    if (dst == null)
                    {
                        dst = new ArrayList<>();
                        src = new ArrayList<>();
                    }

                    dst.add(move.getResult());
                    src.add(move.getInput());

                    if (begin == NONE)
                    {
                        // trace begin
                        begin = i;
                        reg = move.getScratchRegister();
                        slot = move.getBackupSlot();
                    }
                }
                else if (begin != NONE)
                {
                    // end of trace
                    replaceStackMoves(instructions);
                }
            }
            // remove instructions
            if (removed)
            {
                instructions.removeAll(Collections.singleton(null));
            }
        }

        private void replaceStackMoves(List<LIRInstruction> instructions)
        {
            int size = dst.size();
            if (size > 1)
            {
                AMD64MultiStackMove multiMove = new AMD64MultiStackMove(dst.toArray(new AllocatableValue[size]), src.toArray(new AllocatableValue[size]), reg, slot);
                // replace first instruction
                instructions.set(begin, multiMove);
                // and null out others
                Collections.fill(instructions.subList(begin + 1, begin + size), null);
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

    private static AMD64StackMove asStackMove(LIRInstruction inst)
    {
        return (AMD64StackMove) inst;
    }

    private static boolean isStackMove(LIRInstruction inst)
    {
        return inst instanceof AMD64StackMove;
    }
}
