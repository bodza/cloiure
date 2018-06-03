package giraaff.lir.constopt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.BlockMap;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.LIR;
import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.StandardOp.LoadConstantOp;
import giraaff.lir.Variable;
import giraaff.lir.constopt.ConstantTree.Flags;
import giraaff.lir.constopt.ConstantTree.NodeCost;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.PreAllocationOptimizationPhase;

///
// This optimization tries to improve the handling of constants by replacing a single definition of
// a constant, which is potentially scheduled into a block with high probability, with one or more
// definitions in blocks with a lower probability.
///
// @class ConstantLoadOptimization
public final class ConstantLoadOptimization extends PreAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PreAllocationOptimizationContext __context)
    {
        LIRGeneratorTool __lirGen = __context.___lirGen;
        new Optimization(__lirGenRes.getLIR(), __lirGen).apply();
    }

    // @class ConstantLoadOptimization.Optimization
    private static final class Optimization
    {
        // @field
        private final LIR ___lir;
        // @field
        private final LIRGeneratorTool ___lirGen;
        // @field
        private final VariableMap<DefUseTree> ___map;
        // @field
        private final BitSet ___phiConstants;
        // @field
        private final BitSet ___defined;
        // @field
        private final BlockMap<List<UseEntry>> ___blockMap;
        // @field
        private final BlockMap<LIRInsertionBuffer> ___insertionBuffers;

        // @cons
        private Optimization(LIR __lir, LIRGeneratorTool __lirGen)
        {
            super();
            this.___lir = __lir;
            this.___lirGen = __lirGen;
            this.___map = new VariableMap<>();
            this.___phiConstants = new BitSet();
            this.___defined = new BitSet();
            this.___insertionBuffers = new BlockMap<>(__lir.getControlFlowGraph());
            this.___blockMap = new BlockMap<>(__lir.getControlFlowGraph());
        }

        private void apply()
        {
            // build DefUseTree
            for (AbstractBlockBase<?> __b : this.___lir.getControlFlowGraph().getBlocks())
            {
                this.analyzeBlock(__b);
            }
            // remove all with only one use
            this.___map.filter(__t ->
            {
                if (__t.usageCount() > 1)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            });
            // collect block map
            this.___map.forEach(__tree -> __tree.forEach(this::addUsageToBlockMap));

            // create ConstantTree
            this.___map.forEach(this::createConstantTree);

            // insert moves, delete null instructions and reset instruction ids
            for (AbstractBlockBase<?> __b : this.___lir.getControlFlowGraph().getBlocks())
            {
                this.rewriteBlock(__b);
            }
        }

        private static boolean isConstantLoad(LIRInstruction __inst)
        {
            if (!LoadConstantOp.isLoadConstantOp(__inst))
            {
                return false;
            }
            return LIRValueUtil.isVariable(LoadConstantOp.asLoadConstantOp(__inst).getResult());
        }

        private void addUsageToBlockMap(UseEntry __entry)
        {
            AbstractBlockBase<?> __block = __entry.getBlock();
            List<UseEntry> __list = this.___blockMap.get(__block);
            if (__list == null)
            {
                __list = new ArrayList<>();
                this.___blockMap.put(__block, __list);
            }
            __list.add(__entry);
        }

        ///
        // Collects def-use information for a {@code block}.
        ///
        private void analyzeBlock(AbstractBlockBase<?> __block)
        {
            InstructionValueConsumer __loadConsumer = (__instruction, __value, __mode, __flags) ->
            {
                if (LIRValueUtil.isVariable(__value))
                {
                    Variable __var = (Variable) __value;

                    if (!this.___phiConstants.get(__var.___index))
                    {
                        if (!this.___defined.get(__var.___index))
                        {
                            this.___defined.set(__var.___index);
                            if (isConstantLoad(__instruction))
                            {
                                this.___map.put(__var, new DefUseTree(__instruction, __block));
                            }
                        }
                        else
                        {
                            // Variable is redefined, this only happens for constant loads
                            // introduced by phi resolution -> ignore.
                            this.___map.remove(__var);
                            this.___phiConstants.set(__var.___index);
                        }
                    }
                }
            };

            InstructionValueConsumer __useConsumer = (__instruction, __value, __mode, __flags) ->
            {
                if (LIRValueUtil.isVariable(__value))
                {
                    Variable __var = (Variable) __value;
                    if (!this.___phiConstants.get(__var.___index))
                    {
                        DefUseTree __tree = this.___map.get(__var);
                        if (__tree != null)
                        {
                            __tree.addUsage(__block, __instruction, __value);
                        }
                    }
                }
            };

            int __opId = 0;
            for (LIRInstruction __inst : this.___lir.getLIRforBlock(__block))
            {
                // set instruction id to the index in the lir instruction list
                __inst.setId(__opId++);
                __inst.visitEachOutput(__loadConsumer);
                __inst.visitEachInput(__useConsumer);
                __inst.visitEachAlive(__useConsumer);
            }
        }

        ///
        // Creates the dominator tree and searches for an solution.
        ///
        private void createConstantTree(DefUseTree __tree)
        {
            ConstantTree __constTree = new ConstantTree(this.___lir.getControlFlowGraph(), __tree);
            __constTree.set(Flags.SUBTREE, __tree.getBlock());
            __tree.forEach(__u -> __constTree.set(Flags.USAGE, __u.getBlock()));

            if (__constTree.get(Flags.USAGE, __tree.getBlock()))
            {
                // usage in the definition block -> no optimization
                return;
            }

            __constTree.markBlocks();

            NodeCost __cost = ConstantTreeAnalyzer.analyze(__constTree, __tree.getBlock());
            int __usageCount = __cost.getUsages().size();

            if (__cost.getNumMaterializations() > 1 || __cost.getBestCost() < __tree.getBlock().probability())
            {
                // mark original load for removal
                deleteInstruction(__tree);

                // collect result
                createLoads(__tree, __constTree, __tree.getBlock());
            }
            else
            {
                // no better solution found
            }
        }

        private void createLoads(DefUseTree __tree, ConstantTree __constTree, AbstractBlockBase<?> __startBlock)
        {
            Deque<AbstractBlockBase<?>> __worklist = new ArrayDeque<>();
            __worklist.add(__startBlock);
            while (!__worklist.isEmpty())
            {
                AbstractBlockBase<?> __block = __worklist.pollLast();
                if (__constTree.get(Flags.CANDIDATE, __block))
                {
                    __constTree.set(Flags.MATERIALIZE, __block);
                    // create and insert load
                    insertLoad(__tree.getConstant(), __tree.getVariable().getValueKind(), __block, __constTree.getCost(__block).getUsages());
                }
                else
                {
                    AbstractBlockBase<?> __dominated = __block.getFirstDominated();
                    while (__dominated != null)
                    {
                        if (__constTree.isMarked(__dominated))
                        {
                            __worklist.addLast(__dominated);
                        }
                        __dominated = __dominated.getDominatedSibling();
                    }
                }
            }
        }

        private void insertLoad(Constant __constant, ValueKind<?> __kind, AbstractBlockBase<?> __block, List<UseEntry> __usages)
        {
            // create variable
            Variable __variable = this.___lirGen.newVariable(__kind);
            // create move
            LIRInstruction __move = this.___lirGen.getSpillMoveFactory().createLoad(__variable, __constant);
            // insert instruction
            getInsertionBuffer(__block).append(1, __move);
            // update usages
            for (UseEntry __u : __usages)
            {
                __u.setValue(__variable);
            }
        }

        ///
        // Inserts the constant loads created in {@link #createConstantTree} and deletes the
        // original definition.
        ///
        private void rewriteBlock(AbstractBlockBase<?> __block)
        {
            // insert moves
            LIRInsertionBuffer __buffer = this.___insertionBuffers.get(__block);
            if (__buffer != null)
            {
                __buffer.finish();
            }

            // delete instructions
            ArrayList<LIRInstruction> __instructions = this.___lir.getLIRforBlock(__block);
            boolean __hasDead = false;
            for (LIRInstruction __inst : __instructions)
            {
                if (__inst == null)
                {
                    __hasDead = true;
                }
                else
                {
                    __inst.setId(-1);
                }
            }
            if (__hasDead)
            {
                // Remove null values from the list.
                __instructions.removeAll(Collections.singleton(null));
            }
        }

        private void deleteInstruction(DefUseTree __tree)
        {
            AbstractBlockBase<?> __block = __tree.getBlock();
            LIRInstruction __instruction = __tree.getInstruction();
            this.___lir.getLIRforBlock(__block).set(__instruction.id(), null);
        }

        private LIRInsertionBuffer getInsertionBuffer(AbstractBlockBase<?> __block)
        {
            LIRInsertionBuffer __insertionBuffer = this.___insertionBuffers.get(__block);
            if (__insertionBuffer == null)
            {
                __insertionBuffer = new LIRInsertionBuffer();
                this.___insertionBuffers.put(__block, __insertionBuffer);
                ArrayList<LIRInstruction> __instructions = this.___lir.getLIRforBlock(__block);
                __insertionBuffer.init(__instructions);
            }
            return __insertionBuffer;
        }
    }
}
