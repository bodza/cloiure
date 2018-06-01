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
    public LoopFragmentWhole(LoopEx loop)
    {
        super(loop);
    }

    // @cons
    public LoopFragmentWhole(LoopFragmentWhole original)
    {
        super(null, original);
    }

    @Override
    public LoopFragmentWhole duplicate()
    {
        LoopFragmentWhole loopFragmentWhole = new LoopFragmentWhole(this);
        loopFragmentWhole.reify();
        return loopFragmentWhole;
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
            Loop<Block> loop = loop().loop();
            nodes = LoopFragment.computeNodes(graph(), LoopFragment.toHirBlocks(loop.getBlocks()), LoopFragment.toHirExits(loop.getExits()));
        }
        return nodes;
    }

    @Override
    protected ValueNode prim(ValueNode b)
    {
        return getDuplicatedNode(b);
    }

    @Override
    protected DuplicationReplacement getDuplicationReplacement()
    {
        final FixedNode entry = loop().entryPoint();
        final Graph graph = this.graph();
        // @closure
        return new DuplicationReplacement()
        {
            private EndNode endNode;

            @Override
            public Node replacement(Node o)
            {
                if (o == entry)
                {
                    if (endNode == null)
                    {
                        endNode = graph.add(new EndNode());
                    }
                    return endNode;
                }
                return o;
            }
        };
    }

    public FixedNode entryPoint()
    {
        if (isDuplicate())
        {
            LoopBeginNode newLoopBegin = getDuplicatedNode(original().loop().loopBegin());
            return newLoopBegin.forwardEnd();
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
        LoopBeginNode loopBegin = original().loop().loopBegin();
        StructuredGraph graph = loopBegin.graph();
        if (graph.getGuardsStage() == StructuredGraph.GuardsStage.AFTER_FSA)
        {
            // After FrameStateAssignment ControlFlowGraph treats loop exits differently which means
            // that the LoopExitNodes can be in a block which post dominates the true loop exit. For
            // cloning to work right they must agree.
            EconomicSet<LoopExitNode> exits = EconomicSet.create();
            for (Block exitBlock : original().loop().loop().getExits())
            {
                LoopExitNode exitNode = exitBlock.getLoopExit();
                if (exitNode == null)
                {
                    exitNode = graph.add(new LoopExitNode(loopBegin));
                    graph.addAfterFixed(exitBlock.getBeginNode(), exitNode);
                    if (nodes != null)
                    {
                        nodes.mark(exitNode);
                    }
                }
                exits.add(exitNode);
            }
            for (LoopExitNode exitNode : loopBegin.loopExits())
            {
                if (!exits.contains(exitNode))
                {
                    if (nodes != null)
                    {
                        nodes.clear(exitNode);
                    }
                    graph.removeFixed(exitNode);
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
    public void insertBefore(LoopEx loop)
    {
        // TODO auto-generated method stub
    }
}
