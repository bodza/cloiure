package giraaff.lir.constopt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.constopt.ConstantTree.Flags;
import giraaff.lir.constopt.ConstantTree.NodeCost;

///
// Analyzes a {@link ConstantTree} and marks potential materialization positions.
///
// @class ConstantTreeAnalyzer
public final class ConstantTreeAnalyzer
{
    // @field
    private final ConstantTree ___tree;
    // @field
    private final BitSet ___visited;

    public static NodeCost analyze(ConstantTree __tree, AbstractBlockBase<?> __startBlock)
    {
        ConstantTreeAnalyzer __analyzer = new ConstantTreeAnalyzer(__tree);
        __analyzer.analyzeBlocks(__startBlock);
        return __tree.getCost(__startBlock);
    }

    // @cons
    private ConstantTreeAnalyzer(ConstantTree __tree)
    {
        super();
        this.___tree = __tree;
        this.___visited = new BitSet(__tree.size());
    }

    ///
    // Queues all relevant blocks for {@linkplain #process processing}.
    //
    // This is a worklist-style algorithm because a (more elegant) recursive implementation may
    // cause {@linkplain StackOverflowError stack overflows} on larger graphs.
    //
    // @param startBlock The start block of the dominator subtree.
    ///
    private void analyzeBlocks(AbstractBlockBase<?> __startBlock)
    {
        Deque<AbstractBlockBase<?>> __worklist = new ArrayDeque<>();
        __worklist.offerLast(__startBlock);
        while (!__worklist.isEmpty())
        {
            AbstractBlockBase<?> __block = __worklist.pollLast();

            if (isLeafBlock(__block))
            {
                leafCost(__block);
                continue;
            }

            if (!this.___visited.get(__block.getId()))
            {
                // if not yet visited (and not a leaf block) process all children first!
                __worklist.offerLast(__block);
                AbstractBlockBase<?> __dominated = __block.getFirstDominated();
                while (__dominated != null)
                {
                    filteredPush(__worklist, __dominated);
                    __dominated = __dominated.getDominatedSibling();
                }
                this.___visited.set(__block.getId());
            }
            else
            {
                // otherwise, process block
                process(__block);
            }
        }
    }

    ///
    // Calculates the cost of a {@code block}. It is assumed that all {@code children} have already
    // been {@linkplain #process processed}
    //
    // @param block The block to be processed.
    ///
    private void process(AbstractBlockBase<?> __block)
    {
        List<UseEntry> __usages = new ArrayList<>();
        double __bestCost = 0;
        int __numMat = 0;

        // collect children costs
        AbstractBlockBase<?> __child = __block.getFirstDominated();
        while (__child != null)
        {
            if (isMarked(__child))
            {
                NodeCost __childCost = this.___tree.getCost(__child);
                __usages.addAll(__childCost.getUsages());
                __numMat += __childCost.getNumMaterializations();
                __bestCost += __childCost.getBestCost();
            }
            __child = __child.getDominatedSibling();
        }

        // choose block
        List<UseEntry> __usagesBlock = this.___tree.getUsages(__block);
        double __probabilityBlock = __block.probability();

        if (!__usagesBlock.isEmpty() || shouldMaterializerInCurrentBlock(__probabilityBlock, __bestCost, __numMat))
        {
            // mark current block as potential materialization position
            __usages.addAll(__usagesBlock);
            __bestCost = __probabilityBlock;
            __numMat = 1;
            this.___tree.set(Flags.CANDIDATE, __block);
        }
        else
        {
            // stick with the current solution
        }

        NodeCost __nodeCost = new NodeCost(__bestCost, __usages, __numMat);
        this.___tree.setCost(__block, __nodeCost);
    }

    ///
    // This is the cost function that decides whether a materialization should be inserted in the
    // current block.
    //
    // Note that this function does not take into account if a materialization is required despite
    // the probabilities (e.g. there are usages in the current block).
    //
    // @param probabilityBlock Probability of the current block.
    // @param probabilityChildren Accumulated probability of the children.
    // @param numMat Number of materializations along the subtrees. We use {@code numMat - 1} to
    //            insert materializations as late as possible if the probabilities are the same.
    ///
    private static boolean shouldMaterializerInCurrentBlock(double __probabilityBlock, double __probabilityChildren, int __numMat)
    {
        return __probabilityBlock * Math.pow(0.9, __numMat - 1) < __probabilityChildren;
    }

    private void filteredPush(Deque<AbstractBlockBase<?>> __worklist, AbstractBlockBase<?> __block)
    {
        if (isMarked(__block))
        {
            __worklist.offerLast(__block);
        }
    }

    private void leafCost(AbstractBlockBase<?> __block)
    {
        this.___tree.set(Flags.CANDIDATE, __block);
        this.___tree.getOrInitCost(__block);
    }

    private boolean isMarked(AbstractBlockBase<?> __block)
    {
        return this.___tree.isMarked(__block);
    }

    private boolean isLeafBlock(AbstractBlockBase<?> __block)
    {
        return this.___tree.isLeafBlock(__block);
    }
}
