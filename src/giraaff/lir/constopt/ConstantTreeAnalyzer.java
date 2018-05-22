package giraaff.lir.constopt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.constopt.ConstantTree.Flags;
import giraaff.lir.constopt.ConstantTree.NodeCost;

/**
 * Analyzes a {@link ConstantTree} and marks potential materialization positions.
 */
public final class ConstantTreeAnalyzer
{
    private final ConstantTree tree;
    private final BitSet visited;

    public static NodeCost analyze(ConstantTree tree, AbstractBlockBase<?> startBlock)
    {
        ConstantTreeAnalyzer analyzer = new ConstantTreeAnalyzer(tree);
        analyzer.analyzeBlocks(startBlock);
        return tree.getCost(startBlock);
    }

    private ConstantTreeAnalyzer(ConstantTree tree)
    {
        this.tree = tree;
        this.visited = new BitSet(tree.size());
    }

    /**
     * Queues all relevant blocks for {@linkplain #process processing}.
     *
     * This is a worklist-style algorithm because a (more elegant) recursive implementation may
     * cause {@linkplain StackOverflowError stack overflows} on larger graphs.
     *
     * @param startBlock The start block of the dominator subtree.
     */
    private void analyzeBlocks(AbstractBlockBase<?> startBlock)
    {
        Deque<AbstractBlockBase<?>> worklist = new ArrayDeque<>();
        worklist.offerLast(startBlock);
        while (!worklist.isEmpty())
        {
            AbstractBlockBase<?> block = worklist.pollLast();

            if (isLeafBlock(block))
            {
                leafCost(block);
                continue;
            }

            if (!visited.get(block.getId()))
            {
                // if not yet visited (and not a leaf block) process all children first!
                worklist.offerLast(block);
                AbstractBlockBase<?> dominated = block.getFirstDominated();
                while (dominated != null)
                {
                    filteredPush(worklist, dominated);
                    dominated = dominated.getDominatedSibling();
                }
                visited.set(block.getId());
            }
            else
            {
                // otherwise, process block
                process(block);
            }
        }
    }

    /**
     * Calculates the cost of a {@code block}. It is assumed that all {@code children} have already
     * been {@linkplain #process processed}
     *
     * @param block The block to be processed.
     */
    private void process(AbstractBlockBase<?> block)
    {
        List<UseEntry> usages = new ArrayList<>();
        double bestCost = 0;
        int numMat = 0;

        // collect children costs
        AbstractBlockBase<?> child = block.getFirstDominated();
        while (child != null)
        {
            if (isMarked(child))
            {
                NodeCost childCost = tree.getCost(child);
                usages.addAll(childCost.getUsages());
                numMat += childCost.getNumMaterializations();
                bestCost += childCost.getBestCost();
            }
            child = child.getDominatedSibling();
        }

        // choose block
        List<UseEntry> usagesBlock = tree.getUsages(block);
        double probabilityBlock = block.probability();

        if (!usagesBlock.isEmpty() || shouldMaterializerInCurrentBlock(probabilityBlock, bestCost, numMat))
        {
            // mark current block as potential materialization position
            usages.addAll(usagesBlock);
            bestCost = probabilityBlock;
            numMat = 1;
            tree.set(Flags.CANDIDATE, block);
        }
        else
        {
            // stick with the current solution
        }

        NodeCost nodeCost = new NodeCost(bestCost, usages, numMat);
        tree.setCost(block, nodeCost);
    }

    /**
     * This is the cost function that decides whether a materialization should be inserted in the
     * current block.
     *
     * Note that this function does not take into account if a materialization is required despite
     * the probabilities (e.g. there are usages in the current block).
     *
     * @param probabilityBlock Probability of the current block.
     * @param probabilityChildren Accumulated probability of the children.
     * @param numMat Number of materializations along the subtrees. We use {@code numMat - 1} to
     *            insert materializations as late as possible if the probabilities are the same.
     */
    private static boolean shouldMaterializerInCurrentBlock(double probabilityBlock, double probabilityChildren, int numMat)
    {
        return probabilityBlock * Math.pow(0.9, numMat - 1) < probabilityChildren;
    }

    private void filteredPush(Deque<AbstractBlockBase<?>> worklist, AbstractBlockBase<?> block)
    {
        if (isMarked(block))
        {
            worklist.offerLast(block);
        }
    }

    private void leafCost(AbstractBlockBase<?> block)
    {
        tree.set(Flags.CANDIDATE, block);
        tree.getOrInitCost(block);
    }

    private boolean isMarked(AbstractBlockBase<?> block)
    {
        return tree.isMarked(block);
    }

    private boolean isLeafBlock(AbstractBlockBase<?> block)
    {
        return tree.isLeafBlock(block);
    }
}
