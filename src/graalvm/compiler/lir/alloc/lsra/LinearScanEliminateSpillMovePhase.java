package graalvm.compiler.lir.alloc.lsra;

import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayList;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.Assertions;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp.LoadConstantOp;
import graalvm.compiler.lir.StandardOp.MoveOp;
import graalvm.compiler.lir.StandardOp.ValueMoveOp;
import graalvm.compiler.lir.alloc.lsra.Interval.SpillState;
import graalvm.compiler.lir.alloc.lsra.LinearScan.IntervalPredicate;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.options.NestedBooleanOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

public class LinearScanEliminateSpillMovePhase extends LinearScanAllocationPhase {

    public static class Options {
        // @formatter:off
        @Option(help = "Enable spill move elimination.", type = OptionType.Debug)
        public static final OptionKey<Boolean> LIROptLSRAEliminateSpillMoves = new NestedBooleanOptionKey(LIROptimization, true);
        // @formatter:on
    }

    private static final IntervalPredicate mustStoreAtDefinition = new LinearScan.IntervalPredicate() {

        @Override
        public boolean apply(Interval i) {
            return i.isSplitParent() && i.spillState() == SpillState.StoreAtDefinition;
        }
    };

    protected final LinearScan allocator;

    protected LinearScanEliminateSpillMovePhase(LinearScan allocator) {
        this.allocator = allocator;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context) {
        eliminateSpillMoves(lirGenRes);
    }

    /**
     * @return the index of the first instruction that is of interest for
     *         {@link #eliminateSpillMoves}
     */
    protected int firstInstructionOfInterest() {
        // skip the first because it is always a label
        return 1;
    }

    // called once before assignment of register numbers
    @SuppressWarnings("try")
    void eliminateSpillMoves(LIRGenerationResult res) {
        DebugContext debug = allocator.getDebug();
        try (Indent indent = debug.logAndIndent("Eliminating unnecessary spill moves")) {

            /*
             * collect all intervals that must be stored after their definition. The list is sorted
             * by Interval.spillDefinitionPos.
             */
            Interval interval;
            interval = allocator.createUnhandledLists(mustStoreAtDefinition, null).getLeft();
            if (Assertions.detailedAssertionsEnabled(allocator.getOptions())) {
                checkIntervals(debug, interval);
            }

            LIRInsertionBuffer insertionBuffer = new LIRInsertionBuffer();
            for (AbstractBlockBase<?> block : allocator.sortedBlocks()) {
                try (Indent indent1 = debug.logAndIndent("Handle %s", block)) {
                    ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);
                    int numInst = instructions.size();

                    // iterate all instructions of the block.
                    for (int j = firstInstructionOfInterest(); j < numInst; j++) {
                        LIRInstruction op = instructions.get(j);
                        int opId = op.id();

                        if (opId == -1) {
                            MoveOp move = MoveOp.asMoveOp(op);
                            /*
                             * Remove move from register to stack if the stack slot is guaranteed to
                             * be correct. Only moves that have been inserted by LinearScan can be
                             * removed.
                             */
                            if (Options.LIROptLSRAEliminateSpillMoves.getValue(allocator.getOptions()) && canEliminateSpillMove(block, move)) {
                                /*
                                 * Move target is a stack slot that is always correct, so eliminate
                                 * instruction.
                                 */
                                if (debug.isLogEnabled()) {
                                    if (ValueMoveOp.isValueMoveOp(op)) {
                                        ValueMoveOp vmove = ValueMoveOp.asValueMoveOp(op);
                                        debug.log("eliminating move from interval %d (%s) to %d (%s) in block %s", allocator.operandNumber(vmove.getInput()), vmove.getInput(),
                                                        allocator.operandNumber(vmove.getResult()), vmove.getResult(), block);
                                    } else {
                                        LoadConstantOp load = LoadConstantOp.asLoadConstantOp(op);
                                        debug.log("eliminating constant load from %s to %d (%s) in block %s", load.getConstant(), allocator.operandNumber(load.getResult()), load.getResult(), block);
                                    }
                                }

                                // null-instructions are deleted by assignRegNum
                                instructions.set(j, null);
                            }

                        } else {
                            /*
                             * Insert move from register to stack just after the beginning of the
                             * interval.
                             */
                            assert interval.isEndMarker() || interval.spillDefinitionPos() >= opId : "invalid order";
                            assert interval.isEndMarker() || (interval.isSplitParent() && interval.spillState() == SpillState.StoreAtDefinition) : "invalid interval";

                            while (!interval.isEndMarker() && interval.spillDefinitionPos() == opId) {
                                if (!interval.canMaterialize()) {
                                    if (!insertionBuffer.initialized()) {
                                        /*
                                         * prepare insertion buffer (appended when all instructions
                                         * in the block are processed)
                                         */
                                        insertionBuffer.init(instructions);
                                    }

                                    AllocatableValue fromLocation = interval.location();
                                    AllocatableValue toLocation = LinearScan.canonicalSpillOpr(interval);
                                    if (!fromLocation.equals(toLocation)) {

                                        assert isRegister(fromLocation) : "from operand must be a register but is: " + fromLocation + " toLocation=" + toLocation + " spillState=" +
                                                        interval.spillState();
                                        assert isStackSlotValue(toLocation) : "to operand must be a stack slot";

                                        LIRInstruction move = allocator.getSpillMoveFactory().createMove(toLocation, fromLocation);
                                        insertionBuffer.append(j + 1, move);
                                        move.setComment(res, "LSRAEliminateSpillMove: store at definition");

                                        if (debug.isLogEnabled()) {
                                            debug.log("inserting move after definition of interval %d to stack slot %s at opId %d", interval.operandNumber, interval.spillSlot(), opId);
                                        }
                                    }
                                }
                                interval = interval.next;
                            }
                        }
                    } // end of instruction iteration

                    if (insertionBuffer.initialized()) {
                        insertionBuffer.finish();
                    }
                }
            } // end of block iteration

            assert interval.isEndMarker() : "missed an interval";
        }
    }

    /**
     * @param block The block {@code move} is located in.
     * @param move Spill move.
     */
    protected boolean canEliminateSpillMove(AbstractBlockBase<?> block, MoveOp move) {
        assert isVariable(move.getResult()) : "LinearScan inserts only moves to variables: " + move;

        Interval curInterval = allocator.intervalFor(move.getResult());

        if (!isRegister(curInterval.location()) && curInterval.alwaysInMemory()) {
            assert isStackSlotValue(curInterval.location()) : "Not a stack slot: " + curInterval.location();
            return true;
        }
        return false;
    }

    private static void checkIntervals(DebugContext debug, Interval interval) {
        Interval prev = null;
        Interval temp = interval;
        while (!temp.isEndMarker()) {
            assert temp.spillDefinitionPos() > 0 : "invalid spill definition pos";
            if (prev != null) {
                assert temp.from() >= prev.from() : "intervals not sorted";
                assert temp.spillDefinitionPos() >= prev.spillDefinitionPos() : "when intervals are sorted by from :  then they must also be sorted by spillDefinitionPos";
            }

            assert temp.spillSlot() != null || temp.canMaterialize() : "interval has no spill slot assigned";
            assert temp.spillDefinitionPos() >= temp.from() : "invalid order";
            assert temp.spillDefinitionPos() <= temp.from() + 2 : "only intervals defined once at their start-pos can be optimized";

            if (debug.isLogEnabled()) {
                debug.log("interval %d (from %d to %d) must be stored at %d", temp.operandNumber, temp.from(), temp.to(), temp.spillDefinitionPos());
            }

            prev = temp;
            temp = temp.next;
        }
    }

}