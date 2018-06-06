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

///
// Represents a dominator (sub-)tree for a constant definition.
///
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

    ///
    // Costs associated with a block.
    ///
    // @class ConstantTree.NodeCost
    public static final class NodeCost implements PropertyConsumable
    {
        // @field
        private List<UseEntry> ___usages;
        // @field
        private double ___bestCost;
        // @field
        private int ___numMat;

        // @cons ConstantTree.NodeCost
        public NodeCost(double __bestCost, List<UseEntry> __usages, int __numMat)
        {
            super();
            this.___bestCost = __bestCost;
            this.___usages = __usages;
            this.___numMat = __numMat;
        }

        @Override
        public void forEachProperty(BiConsumer<String, String> __action)
        {
            __action.accept("bestCost", Double.toString(getBestCost()));
            __action.accept("numMat", Integer.toString(getNumMaterializations()));
            __action.accept("numUsages", Integer.toString(this.___usages.size()));
        }

        public void addUsage(UseEntry __usage)
        {
            if (this.___usages == null)
            {
                this.___usages = new ArrayList<>();
            }
            this.___usages.add(__usage);
        }

        public List<UseEntry> getUsages()
        {
            if (this.___usages == null)
            {
                return Collections.emptyList();
            }
            return this.___usages;
        }

        public double getBestCost()
        {
            return this.___bestCost;
        }

        public int getNumMaterializations()
        {
            return this.___numMat;
        }

        public void setBestCost(double __cost)
        {
            this.___bestCost = __cost;
        }
    }

    // @field
    private final BlockMap<List<UseEntry>> ___blockMap;

    // @cons ConstantTree
    public ConstantTree(AbstractControlFlowGraph<?> __cfg, DefUseTree __tree)
    {
        super(ConstantTree.Flags.class, __cfg);
        this.___blockMap = new BlockMap<>(__cfg);
        __tree.forEach(__u -> getOrInitList(__u.getBlock()).add(__u));
    }

    private List<UseEntry> getOrInitList(AbstractBlockBase<?> __block)
    {
        List<UseEntry> __list = this.___blockMap.get(__block);
        if (__list == null)
        {
            __list = new ArrayList<>();
            this.___blockMap.put(__block, __list);
        }
        return __list;
    }

    public List<UseEntry> getUsages(AbstractBlockBase<?> __block)
    {
        List<UseEntry> __list = this.___blockMap.get(__block);
        if (__list == null)
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(__list);
    }

    ///
    // Returns the cost object associated with {@code block}. If there is none, a new cost object is created.
    ///
    ConstantTree.NodeCost getOrInitCost(AbstractBlockBase<?> __block)
    {
        ConstantTree.NodeCost __cost = getCost(__block);
        if (__cost == null)
        {
            __cost = new ConstantTree.NodeCost(__block.probability(), this.___blockMap.get(__block), 1);
            setCost(__block, __cost);
        }
        return __cost;
    }

    @Override
    public String getName(ConstantTree.Flags __type)
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
        if (get(ConstantTree.Flags.SUBTREE, __block) && (__block.getDominator() == null || !get(ConstantTree.Flags.SUBTREE, __block.getDominator())))
        {
            __action.accept("hasDefinition", "true");
        }
        super.forEachPropertyPair(__block, __action);
    }

    public long subTreeSize()
    {
        return stream(ConstantTree.Flags.SUBTREE).count();
    }

    public AbstractBlockBase<?> getStartBlock()
    {
        return stream(ConstantTree.Flags.SUBTREE).findFirst().get();
    }

    public void markBlocks()
    {
        for (AbstractBlockBase<?> __block : getBlocks())
        {
            if (get(ConstantTree.Flags.USAGE, __block))
            {
                setDominatorPath(ConstantTree.Flags.SUBTREE, __block);
            }
        }
    }

    public boolean isMarked(AbstractBlockBase<?> __block)
    {
        return get(ConstantTree.Flags.SUBTREE, __block);
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
        set(ConstantTree.Flags.MATERIALIZE, __block);
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
