package graalvm.compiler.lir.constopt;

import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.lir.InstructionValueConsumer;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp.LoadConstantOp;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.constopt.ConstantTree.Flags;
import graalvm.compiler.lir.constopt.ConstantTree.NodeCost;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.lir.phases.PreAllocationOptimizationPhase;
import graalvm.compiler.options.NestedBooleanOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionType;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ValueKind;

/**
 * This optimization tries to improve the handling of constants by replacing a single definition of
 * a constant, which is potentially scheduled into a block with high probability, with one or more
 * definitions in blocks with a lower probability.
 */
public final class ConstantLoadOptimization extends PreAllocationOptimizationPhase
{
    public static class Options
    {
        @Option(help = "Enable constant load optimization.", type = OptionType.Debug)
        public static final NestedBooleanOptionKey LIROptConstantLoadOptimization = new NestedBooleanOptionKey(LIROptimization, true);
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PreAllocationOptimizationContext context)
    {
        LIRGeneratorTool lirGen = context.lirGen;
        new Optimization(lirGenRes.getLIR(), lirGen).apply();
    }

    private static final class Optimization
    {
        private final LIR lir;
        private final LIRGeneratorTool lirGen;
        private final VariableMap<DefUseTree> map;
        private final BitSet phiConstants;
        private final BitSet defined;
        private final BlockMap<List<UseEntry>> blockMap;
        private final BlockMap<LIRInsertionBuffer> insertionBuffers;

        private Optimization(LIR lir, LIRGeneratorTool lirGen)
        {
            this.lir = lir;
            this.lirGen = lirGen;
            this.map = new VariableMap<>();
            this.phiConstants = new BitSet();
            this.defined = new BitSet();
            this.insertionBuffers = new BlockMap<>(lir.getControlFlowGraph());
            this.blockMap = new BlockMap<>(lir.getControlFlowGraph());
        }

        private void apply()
        {
            // build DefUseTree
            for (AbstractBlockBase<?> b : lir.getControlFlowGraph().getBlocks())
            {
                this.analyzeBlock(b);
            }
            // remove all with only one use
            map.filter(t ->
            {
                if (t.usageCount() > 1)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            });
            // collect block map
            map.forEach(tree -> tree.forEach(this::addUsageToBlockMap));

            // create ConstantTree
            map.forEach(this::createConstantTree);

            // insert moves, delete null instructions and reset instruction ids
            for (AbstractBlockBase<?> b : lir.getControlFlowGraph().getBlocks())
            {
                this.rewriteBlock(b);
            }
        }

        private static boolean isConstantLoad(LIRInstruction inst)
        {
            if (!LoadConstantOp.isLoadConstantOp(inst))
            {
                return false;
            }
            return isVariable(LoadConstantOp.asLoadConstantOp(inst).getResult());
        }

        private void addUsageToBlockMap(UseEntry entry)
        {
            AbstractBlockBase<?> block = entry.getBlock();
            List<UseEntry> list = blockMap.get(block);
            if (list == null)
            {
                list = new ArrayList<>();
                blockMap.put(block, list);
            }
            list.add(entry);
        }

        /**
         * Collects def-use information for a {@code block}.
         */
        private void analyzeBlock(AbstractBlockBase<?> block)
        {
            InstructionValueConsumer loadConsumer = (instruction, value, mode, flags) ->
            {
                if (isVariable(value))
                {
                    Variable var = (Variable) value;

                    if (!phiConstants.get(var.index))
                    {
                        if (!defined.get(var.index))
                        {
                            defined.set(var.index);
                            if (isConstantLoad(instruction))
                            {
                                map.put(var, new DefUseTree(instruction, block));
                            }
                        }
                        else
                        {
                            // Variable is redefined, this only happens for constant loads
                            // introduced by phi resolution -> ignore.
                            map.remove(var);
                            phiConstants.set(var.index);
                        }
                    }
                }
            };

            InstructionValueConsumer useConsumer = (instruction, value, mode, flags) ->
            {
                if (isVariable(value))
                {
                    Variable var = (Variable) value;
                    if (!phiConstants.get(var.index))
                    {
                        DefUseTree tree = map.get(var);
                        if (tree != null)
                        {
                            tree.addUsage(block, instruction, value);
                        }
                    }
                }
            };

            int opId = 0;
            for (LIRInstruction inst : lir.getLIRforBlock(block))
            {
                // set instruction id to the index in the lir instruction list
                inst.setId(opId++);
                inst.visitEachOutput(loadConsumer);
                inst.visitEachInput(useConsumer);
                inst.visitEachAlive(useConsumer);
            }
        }

        /**
         * Creates the dominator tree and searches for an solution.
         */
        private void createConstantTree(DefUseTree tree)
        {
            ConstantTree constTree = new ConstantTree(lir.getControlFlowGraph(), tree);
            constTree.set(Flags.SUBTREE, tree.getBlock());
            tree.forEach(u -> constTree.set(Flags.USAGE, u.getBlock()));

            if (constTree.get(Flags.USAGE, tree.getBlock()))
            {
                // usage in the definition block -> no optimization
                return;
            }

            constTree.markBlocks();

            NodeCost cost = ConstantTreeAnalyzer.analyze(constTree, tree.getBlock());
            int usageCount = cost.getUsages().size();

            if (cost.getNumMaterializations() > 1 || cost.getBestCost() < tree.getBlock().probability())
            {
                // mark original load for removal
                deleteInstruction(tree);

                // collect result
                createLoads(tree, constTree, tree.getBlock());
            }
            else
            {
                // no better solution found
            }
        }

        private void createLoads(DefUseTree tree, ConstantTree constTree, AbstractBlockBase<?> startBlock)
        {
            Deque<AbstractBlockBase<?>> worklist = new ArrayDeque<>();
            worklist.add(startBlock);
            while (!worklist.isEmpty())
            {
                AbstractBlockBase<?> block = worklist.pollLast();
                if (constTree.get(Flags.CANDIDATE, block))
                {
                    constTree.set(Flags.MATERIALIZE, block);
                    // create and insert load
                    insertLoad(tree.getConstant(), tree.getVariable().getValueKind(), block, constTree.getCost(block).getUsages());
                }
                else
                {
                    AbstractBlockBase<?> dominated = block.getFirstDominated();
                    while (dominated != null)
                    {
                        if (constTree.isMarked(dominated))
                        {
                            worklist.addLast(dominated);
                        }
                        dominated = dominated.getDominatedSibling();
                    }
                }
            }
        }

        private void insertLoad(Constant constant, ValueKind<?> kind, AbstractBlockBase<?> block, List<UseEntry> usages)
        {
            // create variable
            Variable variable = lirGen.newVariable(kind);
            // create move
            LIRInstruction move = lirGen.getSpillMoveFactory().createLoad(variable, constant);
            // insert instruction
            getInsertionBuffer(block).append(1, move);
            // update usages
            for (UseEntry u : usages)
            {
                u.setValue(variable);
            }
        }

        /**
         * Inserts the constant loads created in {@link #createConstantTree} and deletes the
         * original definition.
         */
        private void rewriteBlock(AbstractBlockBase<?> block)
        {
            // insert moves
            LIRInsertionBuffer buffer = insertionBuffers.get(block);
            if (buffer != null)
            {
                buffer.finish();
            }

            // delete instructions
            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
            boolean hasDead = false;
            for (LIRInstruction inst : instructions)
            {
                if (inst == null)
                {
                    hasDead = true;
                }
                else
                {
                    inst.setId(-1);
                }
            }
            if (hasDead)
            {
                // Remove null values from the list.
                instructions.removeAll(Collections.singleton(null));
            }
        }

        private void deleteInstruction(DefUseTree tree)
        {
            AbstractBlockBase<?> block = tree.getBlock();
            LIRInstruction instruction = tree.getInstruction();
            lir.getLIRforBlock(block).set(instruction.id(), null);
        }

        private LIRInsertionBuffer getInsertionBuffer(AbstractBlockBase<?> block)
        {
            LIRInsertionBuffer insertionBuffer = insertionBuffers.get(block);
            if (insertionBuffer == null)
            {
                insertionBuffer = new LIRInsertionBuffer();
                insertionBuffers.put(block, insertionBuffer);
                ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
                insertionBuffer.init(instructions);
            }
            return insertionBuffer;
        }
    }
}
