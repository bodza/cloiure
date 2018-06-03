package giraaff.loop;

import org.graalvm.collections.EconomicSet;

import giraaff.core.common.cfg.Loop;
import giraaff.graph.Graph;
import giraaff.graph.Graph.DuplicationReplacement;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.cfg.Block;

// @class LoopFragmentWhole
public final class LoopFragmentWhole extends LoopFragment
{
    // @cons
    public LoopFragmentWhole(LoopEx __loop)
    {
        super(__loop);
    }

    // @cons
    public LoopFragmentWhole(LoopFragmentWhole __original)
    {
        super(null, __original);
    }

    @Override
    public LoopFragmentWhole duplicate()
    {
        LoopFragmentWhole __loopFragmentWhole = new LoopFragmentWhole(this);
        __loopFragmentWhole.reify();
        return __loopFragmentWhole;
    }

    private void reify()
    {
        patchNodes(null);

        mergeEarlyExits();
    }

    @Override
    public NodeBitMap nodes()
    {
        if (nodes == null)
        {
            Loop<Block> __loop = loop().loop();
            nodes = LoopFragment.computeNodes(graph(), LoopFragment.toHirBlocks(__loop.getBlocks()), LoopFragment.toHirExits(__loop.getExits()));
        }
        return nodes;
    }

    @Override
    protected ValueNode prim(ValueNode __b)
    {
        return getDuplicatedNode(__b);
    }

    @Override
    protected DuplicationReplacement getDuplicationReplacement()
    {
        final FixedNode __entry = loop().entryPoint();
        final Graph __graph = this.graph();
        // @closure
        return new DuplicationReplacement()
        {
            // @field
            private EndNode endNode;

            @Override
            public Node replacement(Node __o)
            {
                if (__o == __entry)
                {
                    if (endNode == null)
                    {
                        endNode = __graph.add(new EndNode());
                    }
                    return endNode;
                }
                return __o;
            }
        };
    }

    public FixedNode entryPoint()
    {
        if (isDuplicate())
        {
            LoopBeginNode __newLoopBegin = getDuplicatedNode(original().loop().loopBegin());
            return __newLoopBegin.forwardEnd();
        }
        return loop().entryPoint();
    }

    @Override
    protected void finishDuplication()
    {
        // TODO
    }

    void cleanupLoopExits()
    {
        LoopBeginNode __loopBegin = original().loop().loopBegin();
        StructuredGraph __graph = __loopBegin.graph();
        if (__graph.getGuardsStage() == StructuredGraph.GuardsStage.AFTER_FSA)
        {
            // After FrameStateAssignment ControlFlowGraph treats loop exits differently which means
            // that the LoopExitNodes can be in a block which post dominates the true loop exit. For
            // cloning to work right they must agree.
            EconomicSet<LoopExitNode> __exits = EconomicSet.create();
            for (Block __exitBlock : original().loop().loop().getExits())
            {
                LoopExitNode __exitNode = __exitBlock.getLoopExit();
                if (__exitNode == null)
                {
                    __exitNode = __graph.add(new LoopExitNode(__loopBegin));
                    __graph.addAfterFixed(__exitBlock.getBeginNode(), __exitNode);
                    if (nodes != null)
                    {
                        nodes.mark(__exitNode);
                    }
                }
                __exits.add(__exitNode);
            }
            for (LoopExitNode __exitNode : __loopBegin.loopExits())
            {
                if (!__exits.contains(__exitNode))
                {
                    if (nodes != null)
                    {
                        nodes.clear(__exitNode);
                    }
                    __graph.removeFixed(__exitNode);
                }
            }
        }
    }

    @Override
    protected void beforeDuplication()
    {
        cleanupLoopExits();
    }

    @Override
    public void insertBefore(LoopEx __loop)
    {
        // TODO auto-generated method stub
    }
}
