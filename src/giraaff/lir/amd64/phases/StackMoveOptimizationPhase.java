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
import giraaff.lir.amd64.AMD64Move;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

///
// Replaces sequential {@link AMD64Move.AMD64StackMove}s of the same type with a single
// {@link AMD64Move.AMD64MultiStackMove} to avoid storing/restoring the scratch register multiple times.
//
// Note: this phase must be inserted <b>after</b> {@link RedundantMoveElimination} phase because
// {@link AMD64Move.AMD64MultiStackMove} are not probably detected.
///
// @class StackMoveOptimizationPhase
public final class StackMoveOptimizationPhase extends PostAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PostAllocationOptimizationPhase.PostAllocationOptimizationContext __context)
    {
        LIR __lir = __lirGenRes.getLIR();
        for (AbstractBlockBase<?> __block : __lir.getControlFlowGraph().getBlocks())
        {
            ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);
            new StackMoveOptimizationPhase.Closure0().process(__instructions);
        }
    }

    // @class StackMoveOptimizationPhase.Closure0
    private static class Closure0
    {
        // @def
        private static final int NONE = -1;

        // @field
        private int ___begin = NONE;
        // @field
        private Register ___reg = null;
        // @field
        private List<AllocatableValue> ___dst;
        // @field
        private List<Value> ___src;
        // @field
        private AllocatableValue ___slot;
        // @field
        private boolean ___removed = false;

        public void process(List<LIRInstruction> __instructions)
        {
            for (int __i = 0; __i < __instructions.size(); __i++)
            {
                LIRInstruction __inst = __instructions.get(__i);

                if (isStackMove(__inst))
                {
                    AMD64Move.AMD64StackMove __move = asStackMove(__inst);

                    if (this.___reg != null && !this.___reg.equals(__move.getScratchRegister()))
                    {
                        // end of trace & start of new
                        replaceStackMoves(__instructions);
                    }

                    // lazy initialize
                    if (this.___dst == null)
                    {
                        this.___dst = new ArrayList<>();
                        this.___src = new ArrayList<>();
                    }

                    this.___dst.add(__move.getResult());
                    this.___src.add(__move.getInput());

                    if (this.___begin == NONE)
                    {
                        // trace begin
                        this.___begin = __i;
                        this.___reg = __move.getScratchRegister();
                        this.___slot = __move.getBackupSlot();
                    }
                }
                else if (this.___begin != NONE)
                {
                    // end of trace
                    replaceStackMoves(__instructions);
                }
            }
            // remove instructions
            if (this.___removed)
            {
                __instructions.removeAll(Collections.singleton(null));
            }
        }

        private void replaceStackMoves(List<LIRInstruction> __instructions)
        {
            int __size = this.___dst.size();
            if (__size > 1)
            {
                AMD64Move.AMD64MultiStackMove __multiMove = new AMD64Move.AMD64MultiStackMove(this.___dst.toArray(new AllocatableValue[__size]), this.___src.toArray(new AllocatableValue[__size]), this.___reg, this.___slot);
                // replace first instruction
                __instructions.set(this.___begin, __multiMove);
                // and null out others
                Collections.fill(__instructions.subList(this.___begin + 1, this.___begin + __size), null);
                // removed
                this.___removed = true;
            }
            // reset
            this.___dst.clear();
            this.___src.clear();
            this.___begin = NONE;
            this.___reg = null;
            this.___slot = null;
        }
    }

    private static AMD64Move.AMD64StackMove asStackMove(LIRInstruction __inst)
    {
        return (AMD64Move.AMD64StackMove) __inst;
    }

    private static boolean isStackMove(LIRInstruction __inst)
    {
        return __inst instanceof AMD64Move.AMD64StackMove;
    }
}
