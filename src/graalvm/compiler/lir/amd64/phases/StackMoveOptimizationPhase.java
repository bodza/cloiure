package graalvm.compiler.lir.amd64.phases;

import static graalvm.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.RedundantMoveElimination;
import graalvm.compiler.lir.amd64.AMD64Move.AMD64MultiStackMove;
import graalvm.compiler.lir.amd64.AMD64Move.AMD64StackMove;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase;
import graalvm.compiler.options.NestedBooleanOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionType;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

/**
 * Replaces sequential {@link AMD64StackMove}s of the same type with a single
 * {@link AMD64MultiStackMove} to avoid storing/restoring the scratch register multiple times.
 *
 * Note: this phase must be inserted <b>after</b> {@link RedundantMoveElimination} phase because
 * {@link AMD64MultiStackMove} are not probably detected.
 */
public class StackMoveOptimizationPhase extends PostAllocationOptimizationPhase
{
    public static class Options
    {
        @Option(help = "", type = OptionType.Debug)
        public static final NestedBooleanOptionKey LIROptStackMoveOptimizer = new NestedBooleanOptionKey(LIROptimization, true);
    }

    private static final CounterKey eliminatedBackup = DebugContext.counter("StackMoveOptimizer[EliminatedScratchBackupRestore]");

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PostAllocationOptimizationContext context)
    {
        LIR lir = lirGenRes.getLIR();
        DebugContext debug = lir.getDebug();
        for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks())
        {
            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
            new Closure().process(debug, instructions);
        }
    }

    private static class Closure
    {
        private static final int NONE = -1;

        private int begin = NONE;
        private Register reg = null;
        private List<AllocatableValue> dst;
        private List<Value> src;
        private AllocatableValue slot;
        private boolean removed = false;

        public void process(DebugContext debug, List<LIRInstruction> instructions)
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
                        replaceStackMoves(debug, instructions);
                    }

                    // lazy initialize
                    if (dst == null)
                    {
                        assert src == null;
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
                    replaceStackMoves(debug, instructions);
                }
            }
            // remove instructions
            if (removed)
            {
                instructions.removeAll(Collections.singleton(null));
            }
        }

        private void replaceStackMoves(DebugContext debug, List<LIRInstruction> instructions)
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
                eliminatedBackup.add(debug, size - 1);
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
        assert isStackMove(inst);
        return (AMD64StackMove) inst;
    }

    private static boolean isStackMove(LIRInstruction inst)
    {
        return inst instanceof AMD64StackMove;
    }
}
