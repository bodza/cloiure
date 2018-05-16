package graalvm.compiler.loop;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.core.common.cfg.Loop;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Graph.DuplicationReplacement;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeBitMap;
import graalvm.compiler.nodes.EndNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.cfg.Block;

public class LoopFragmentWhole extends LoopFragment
{
    public LoopFragmentWhole(LoopEx loop)
    {
        super(loop);
    }

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
        assert this.isDuplicate();

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
        // TODO (gd) ?
    }

    void cleanupLoopExits()
    {
        LoopBeginNode loopBegin = original().loop().loopBegin();
        assert nodes == null || nodes.contains(loopBegin);
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
                    graph.getDebug().dump(DebugContext.VERBOSE_LEVEL, graph, "Adjusting loop exit node for %s", loopBegin);
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
        // TODO Auto-generated method stub
    }
}
