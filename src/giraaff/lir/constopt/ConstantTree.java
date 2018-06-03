package giraaff.lir.constopt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.core.common.cfg.BlockMap;
import giraaff.core.common.cfg.PrintableDominatorOptimizationProblem;
import giraaff.core.common.cfg.PropertyConsumable;

/**
 * Represents a dominator (sub-)tree for a constant definition.
 */
// @class ConstantTree
public final class ConstantTree extends PrintableDominatorOptimizationProblem<ConstantTree.Flags, ConstantTree.NodeCost>
{
    // @enum ConstantTree.Flags
    public enum Flags
    {
        SUBTREE,
        USAGE,
        MATERIALIZE,
        CANDIDATE,
    }

    /**
     * Costs associated with a block.
     */
    // @class ConstantTree.NodeCost
    public static final class NodeCost implements PropertyConsumable
    {
        // @field
        private List<UseEntry> usages;
        // @field
        private double bestCost;
        // @field
        private int numMat;

        // @cons
        public NodeCost(double __bestCost, List<UseEntry> __usages, int __numMat)
        {
            super();
            this.bestCost = __bestCost;
            this.usages = __usages;
            this.numMat = __numMat;
        }

        @Override
        public void forEachProperty(BiConsumer<String, String> __action)
        {
            __action.accept("bestCost", Double.toString(getBestCost()));
            __action.accept("numMat", Integer.toString(getNumMaterializations()));
            __action.accept("numUsages", Integer.toString(usages.size()));
        }

        public void addUsage(UseEntry __usage)
        {
            if (usages == null)
            {
                usages = new ArrayList<>();
            }
            usages.add(__usage);
        }

        public List<UseEntry> getUsages()
        {
            if (usages == null)
            {
                return Collections.emptyList();
            }
            return usages;
        }

        public double getBestCost()
        {
            return bestCost;
        }

        public int getNumMaterializations()
        {
            return numMat;
        }

        public void setBestCost(double __cost)
        {
            bestCost = __cost;
        }
    }

    // @field
    private final BlockMap<List<UseEntry>> blockMap;

    // @cons
    public ConstantTree(AbstractControlFlowGraph<?> __cfg, DefUseTree __tree)
    {
        super(Flags.class, __cfg);
        this.blockMap = new BlockMap<>(__cfg);
        __tree.forEach(__u -> getOrInitList(__u.getBlock()).add(__u));
    }

    private List<UseEntry> getOrInitList(AbstractBlockBase<?> __block)
    {
        List<UseEntry> __list = blockMap.get(__block);
        if (__list == null)
        {
            __list = new ArrayList<>();
            blockMap.put(__block, __list);
        }
        return __list;
    }

    public List<UseEntry> getUsages(AbstractBlockBase<?> __block)
    {
        List<UseEntry> __list = blockMap.get(__block);
        if (__list == null)
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(__list);
    }

    /**
     * Returns the cost object associated with {@code block}. If there is none, a new cost object is created.
     */
    NodeCost getOrInitCost(AbstractBlockBase<?> __block)
    {
        NodeCost __cost = getCost(__block);
        if (__cost == null)
        {
            __cost = new NodeCost(__block.probability(), blockMap.get(__block), 1);
            setCost(__block, __cost);
        }
        return __cost;
    }

    @Override
    public String getName(Flags __type)
    {
        switch (__type)
        {
            case USAGE:
                return "hasUsage";
            case SUBTREE:
                return "inSubtree";
            case MATERIALIZE:
                return "materialize";
            case CANDIDATE:
                return "candidate";
        }
        return super.getName(__type);
    }

    @Override
    public void forEachPropertyPair(AbstractBlockBase<?> __block, BiConsumer<String, String> __action)
    {
        if (get(Flags.SUBTREE, __block) && (__block.getDominator() == null || !get(Flags.SUBTREE, __block.getDominator())))
        {
            __action.accept("hasDefinition", "true");
        }
        super.forEachPropertyPair(__block, __action);
    }

    public long subTreeSize()
    {
        return stream(Flags.SUBTREE).count();
    }

    public AbstractBlockBase<?> getStartBlock()
    {
        return stream(Flags.SUBTREE).findFirst().get();
    }

    public void markBlocks()
    {
        for (AbstractBlockBase<?> __block : getBlocks())
        {
            if (get(Flags.USAGE, __block))
            {
                setDominatorPath(Flags.SUBTREE, __block);
            }
        }
    }

    public boolean isMarked(AbstractBlockBase<?> __block)
    {
        return get(Flags.SUBTREE, __block);
    }

    public boolean isLeafBlock(AbstractBlockBase<?> __block)
    {
        AbstractBlockBase<?> __dom = __block.getFirstDominated();
        while (__dom != null)
        {
            if (isMarked(__dom))
            {
                return false;
            }
            __dom = __dom.getDominatedSibling();
        }
        return true;
    }

    public void setSolution(AbstractBlockBase<?> __block)
    {
        set(Flags.MATERIALIZE, __block);
    }

    public int size()
    {
        return getBlocks().length;
    }

    public void traverseTreeWhileTrue(AbstractBlockBase<?> __block, Predicate<AbstractBlockBase<?>> __action)
    {
        if (__action.test(__block))
        {
            AbstractBlockBase<?> __dom = __block.getFirstDominated();
            while (__dom != null)
            {
                if (this.isMarked(__dom))
                {
                    traverseTreeWhileTrue(__dom, __action);
                }
                __dom = __dom.getDominatedSibling();
            }
        }
    }
}
