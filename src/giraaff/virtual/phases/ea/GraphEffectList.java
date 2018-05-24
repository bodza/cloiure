package giraaff.virtual.phases.ea;

import java.util.ArrayList;

import giraaff.graph.Node;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.IfNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.phases.common.DeadCodeEliminationPhase;

public final class GraphEffectList extends EffectList
{
    public GraphEffectList()
    {
        super();
    }

    /**
     * Determines how many objects are virtualized (positive) or materialized (negative) by this effect.
     */
    private int virtualizationDelta;

    @Override
    public void clear()
    {
        super.clear();
        virtualizationDelta = 0;
    }

    /**
     * Adds the given fixed node to the graph's control flow, before position (so that the original
     * predecessor of position will then be node's predecessor).
     *
     * @param node The fixed node to be added to the graph.
     * @param position The fixed node before which the node should be added.
     */
    public void addFixedNodeBefore(FixedWithNextNode node, FixedNode position)
    {
        add("add fixed node", graph ->
        {
            graph.addBeforeFixed(position, graph.add(node));
        });
    }

    public void ensureAdded(ValueNode node, FixedNode position)
    {
        add("ensure added", graph ->
        {
            if (!node.isAlive())
            {
                graph.addOrUniqueWithInputs(node);
                if (node instanceof FixedWithNextNode)
                {
                    graph.addBeforeFixed(position, (FixedWithNextNode) node);
                }
            }
        });
    }

    public void addVirtualizationDelta(int delta)
    {
        virtualizationDelta += delta;
    }

    public int getVirtualizationDelta()
    {
        return virtualizationDelta;
    }

    /**
     * Add the given floating node to the graph.
     *
     * @param node The floating node to be added.
     */
    public void addFloatingNode(ValueNode node, @SuppressWarnings("unused") String cause)
    {
        add("add floating node", graph ->
        {
            graph.addWithoutUniqueWithInputs(node);
        });
    }

    /**
     * Sets the phi node's input at the given index to the given value, adding new phi inputs as needed.
     *
     * @param node The phi node whose input should be changed.
     * @param index The index of the phi input to be changed.
     * @param value The new value for the phi input.
     */
    public void initializePhiInput(PhiNode node, int index, ValueNode value)
    {
        add("set phi input", (graph, obsoleteNodes) ->
        {
            node.initializeValueAt(index, graph.addOrUniqueWithInputs(value));
        });
    }

    /**
     * Adds a virtual object's state to the given frame state. If the given reusedVirtualObjects set
     * contains the virtual object then old states for this object will be removed.
     *
     * @param node The frame state to which the state should be added.
     * @param state The virtual object state to add.
     */
    public void addVirtualMapping(FrameState node, EscapeObjectState state)
    {
        add("add virtual mapping", new Effect()
        {
            @Override
            public void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes)
            {
                if (node.isAlive())
                {
                    FrameState stateAfter = node;
                    for (int i = 0; i < stateAfter.virtualObjectMappingCount(); i++)
                    {
                        if (stateAfter.virtualObjectMappingAt(i).object() == state.object())
                        {
                            stateAfter.virtualObjectMappings().remove(i);
                        }
                    }
                    stateAfter.addVirtualObjectMapping(graph.addOrUniqueWithInputs(state));
                }
            }

            @Override
            public boolean isVisible()
            {
                return false;
            }
        });
    }

    /**
     * Removes the given fixed node from the control flow and deletes it.
     *
     * @param node The fixed node that should be deleted.
     */
    public void deleteNode(Node node)
    {
        add("delete fixed node", (graph, obsoleteNodes) ->
        {
            if (node instanceof FixedWithNextNode)
            {
                GraphUtil.unlinkFixedNode((FixedWithNextNode) node);
            }
            obsoleteNodes.add(node);
        });
    }

    public void killIfBranch(IfNode ifNode, boolean constantCondition)
    {
        add("kill if branch", new Effect()
        {
            @Override
            public void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes)
            {
                graph.removeSplitPropagate(ifNode, ifNode.getSuccessor(constantCondition));
            }

            @Override
            public boolean isCfgKill()
            {
                return true;
            }
        });
    }

    public void replaceWithSink(FixedWithNextNode node, ControlSinkNode sink)
    {
        add("kill if branch", new Effect()
        {
            @Override
            public void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes)
            {
                graph.addWithoutUnique(sink);
                node.replaceAtPredecessor(sink);
                GraphUtil.killCFG(node);
            }

            @Override
            public boolean isCfgKill()
            {
                return true;
            }
        });
    }

    /**
     * Replaces the given node at its usages without deleting it. If the current node is a fixed
     * node it will be disconnected from the control flow, so that it will be deleted by a
     * subsequent {@link DeadCodeEliminationPhase}
     *
     * @param node The node to be replaced.
     * @param replacement The node that should replace the original value. If the replacement is a
     *            non-connected {@link FixedWithNextNode} it will be added to the control flow.
     *
     */
    public void replaceAtUsages(ValueNode node, ValueNode replacement, FixedNode insertBefore)
    {
        add("replace at usages", (graph, obsoleteNodes) ->
        {
            ValueNode replacementNode = graph.addOrUniqueWithInputs(replacement);
            if (replacementNode instanceof FixedWithNextNode && ((FixedWithNextNode) replacementNode).next() == null)
            {
                graph.addBeforeFixed(insertBefore, (FixedWithNextNode) replacementNode);
            }
            /*
             * Keep the (better) stamp information when replacing a node with another one if the
             * replacement has a less precise stamp than the original node. This can happen for
             * example in the context of read nodes and unguarded pi nodes where the pi will be used
             * to improve the stamp information of the read. Such a read might later be replaced
             * with a read with a less precise stamp.
             */
            if (!node.stamp(NodeView.DEFAULT).equals(replacementNode.stamp(NodeView.DEFAULT)))
            {
                replacementNode = graph.unique(new PiNode(replacementNode, node.stamp(NodeView.DEFAULT)));
            }
            node.replaceAtUsages(replacementNode);
            if (node instanceof FixedWithNextNode)
            {
                GraphUtil.unlinkFixedNode((FixedWithNextNode) node);
            }
            obsoleteNodes.add(node);
        });
    }

    /**
     * Replaces the first occurrence of oldInput in node with newInput.
     *
     * @param node The node whose input should be changed.
     * @param oldInput The value to look for.
     * @param newInput The value to replace with.
     */
    public void replaceFirstInput(Node node, Node oldInput, Node newInput)
    {
        add("replace first input", new Effect()
        {
            @Override
            public void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes)
            {
                if (node.isAlive())
                {
                    node.replaceFirstInput(oldInput, newInput);
                }
            }

            @Override
            public boolean isVisible()
            {
                return !(node instanceof FrameState);
            }
        });
    }
}
