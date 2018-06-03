package giraaff.loop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.cfg.Loop;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;

// @class LoopsData
public final class LoopsData
{
    // @field
    private final EconomicMap<LoopBeginNode, LoopEx> loopBeginToEx = EconomicMap.create(Equivalence.IDENTITY);
    // @field
    private final ControlFlowGraph cfg;
    // @field
    private final List<LoopEx> loops;

    // @cons
    public LoopsData(final StructuredGraph __graph)
    {
        super();
        cfg = ControlFlowGraph.compute(__graph, true, true, true, true);
        loops = new ArrayList<>(cfg.getLoops().size());
        for (Loop<Block> __loop : cfg.getLoops())
        {
            LoopEx __ex = new LoopEx(__loop, this);
            loops.add(__ex);
            loopBeginToEx.put(__ex.loopBegin(), __ex);
        }
    }

    /**
     * Checks that loops are ordered such that outer loops appear first.
     */
    private static boolean checkLoopOrder(Iterable<Loop<Block>> __loops)
    {
        EconomicSet<Loop<Block>> __seen = EconomicSet.create(Equivalence.IDENTITY);
        for (Loop<Block> __loop : __loops)
        {
            if (__loop.getParent() != null && !__seen.contains(__loop.getParent()))
            {
                return false;
            }
            __seen.add(__loop);
        }
        return true;
    }

    public LoopEx loop(Loop<Block> __loop)
    {
        return loopBeginToEx.get((LoopBeginNode) __loop.getHeader().getBeginNode());
    }

    public LoopEx loop(LoopBeginNode __loopBegin)
    {
        return loopBeginToEx.get(__loopBegin);
    }

    public List<LoopEx> loops()
    {
        return loops;
    }

    public List<LoopEx> outerFirst()
    {
        return loops;
    }

    public Collection<LoopEx> countedLoops()
    {
        List<LoopEx> __counted = new LinkedList<>();
        for (LoopEx __loop : loops())
        {
            if (__loop.isCounted())
            {
                __counted.add(__loop);
            }
        }
        return __counted;
    }

    public void detectedCountedLoops()
    {
        for (LoopEx __loop : loops())
        {
            __loop.detectCounted();
        }
    }

    public ControlFlowGraph getCFG()
    {
        return cfg;
    }

    public InductionVariable getInductionVariable(ValueNode __value)
    {
        InductionVariable __match = null;
        for (LoopEx __loop : loops())
        {
            InductionVariable __iv = __loop.getInductionVariables().get(__value);
            if (__iv != null)
            {
                if (__match != null)
                {
                    return null;
                }
                __match = __iv;
            }
        }
        return __match;
    }

    /**
     * Deletes any nodes created within the scope of this object that have no usages.
     */
    public void deleteUnusedNodes()
    {
        for (LoopEx __loop : loops())
        {
            __loop.deleteUnusedNodes();
        }
    }
}
