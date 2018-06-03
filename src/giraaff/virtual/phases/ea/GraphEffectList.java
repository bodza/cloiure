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

// @class GraphEffectList
public final class GraphEffectList extends EffectList
{
    // @cons
    public GraphEffectList()
    {
        super();
    }

    /**
     * Determines how many objects are virtualized (positive) or materialized (negative) by this effect.
     */
    // @field
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
    public void addFixedNodeBefore(FixedWithNextNode __node, FixedNode __position)
    {
        add("add fixed node", __graph ->
        {
            __graph.addBeforeFixed(__position, __graph.add(__node));
        });
    }

    public void ensureAdded(ValueNode __node, FixedNode __position)
    {
        add("ensure added", __graph ->
        {
            if (!__node.isAlive())
            {
                __graph.addOrUniqueWithInputs(__node);
                if (__node instanceof FixedWithNextNode)
                {
                    __graph.addBeforeFixed(__position, (FixedWithNextNode) __node);
                }
            }
        });
    }

    public void addVirtualizationDelta(int __delta)
    {
        virtualizationDelta += __delta;
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
    public void addFloatingNode(ValueNode __node, @SuppressWarnings("unused") String __cause)
    {
        add("add floating node", __graph ->
        {
            __graph.addWithoutUniqueWithInputs(__node);
        });
    }

    /**
     * Sets the phi node's input at the given index to the given value, adding new phi inputs as needed.
     *
     * @param node The phi node whose input should be changed.
     * @param index The index of the phi input to be changed.
     * @param value The new value for the phi input.
     */
    public void initializePhiInput(PhiNode __node, int __index, ValueNode __value)
    {
        add("set phi input", (__graph, __obsoleteNodes) ->
        {
            __node.initializeValueAt(__index, __graph.addOrUniqueWithInputs(__value));
        });
    }

    /**
     * Adds a virtual object's state to the given frame state. If the given reusedVirtualObjects set
     * contains the virtual object then old states for this object will be removed.
     *
     * @param node The frame state to which the state should be added.
     * @param state The virtual object state to add.
     */
    public void addVirtualMapping(FrameState __node, EscapeObjectState __state)
    {
        // @closure
        add("add virtual mapping", new Effect()
        {
            @Override
            public void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes)
            {
                if (__node.isAlive())
                {
                    FrameState __stateAfter = __node;
                    for (int __i = 0; __i < __stateAfter.virtualObjectMappingCount(); __i++)
                    {
                        if (__stateAfter.virtualObjectMappingAt(__i).object() == __state.object())
                        {
                            __stateAfter.virtualObjectMappings().remove(__i);
                        }
                    }
                    __stateAfter.addVirtualObjectMapping(__graph.addOrUniqueWithInputs(__state));
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
    public void deleteNode(Node __node)
    {
        add("delete fixed node", (__graph, __obsoleteNodes) ->
        {
            if (__node instanceof FixedWithNextNode)
            {
                GraphUtil.unlinkFixedNode((FixedWithNextNode) __node);
            }
            __obsoleteNodes.add(__node);
        });
    }

    public void killIfBranch(IfNode __ifNode, boolean __constantCondition)
    {
        // @closure
        add("kill if branch", new Effect()
        {
            @Override
            public void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes)
            {
                __graph.removeSplitPropagate(__ifNode, __ifNode.getSuccessor(__constantCondition));
            }

            @Override
            public boolean isCfgKill()
            {
                return true;
            }
        });
    }

    public void replaceWithSink(FixedWithNextNode __node, ControlSinkNode __sink)
    {
        // @closure
        add("kill if branch", new Effect()
        {
            @Override
            public void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes)
            {
                __graph.addWithoutUnique(__sink);
                __node.replaceAtPredecessor(__sink);
                GraphUtil.killCFG(__node);
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
     */
    public void replaceAtUsages(ValueNode __node, ValueNode __replacement, FixedNode __insertBefore)
    {
        add("replace at usages", (__graph, __obsoleteNodes) ->
        {
            ValueNode __replacementNode = __graph.addOrUniqueWithInputs(__replacement);
            if (__replacementNode instanceof FixedWithNextNode && ((FixedWithNextNode) __replacementNode).next() == null)
            {
                __graph.addBeforeFixed(__insertBefore, (FixedWithNextNode) __replacementNode);
            }
            /*
             * Keep the (better) stamp information when replacing a node with another one if the
             * replacement has a less precise stamp than the original node. This can happen for
             * example in the context of read nodes and unguarded pi nodes where the pi will be used
             * to improve the stamp information of the read. Such a read might later be replaced
             * with a read with a less precise stamp.
             */
            if (!__node.stamp(NodeView.DEFAULT).equals(__replacementNode.stamp(NodeView.DEFAULT)))
            {
                __replacementNode = __graph.unique(new PiNode(__replacementNode, __node.stamp(NodeView.DEFAULT)));
            }
            __node.replaceAtUsages(__replacementNode);
            if (__node instanceof FixedWithNextNode)
            {
                GraphUtil.unlinkFixedNode((FixedWithNextNode) __node);
            }
            __obsoleteNodes.add(__node);
        });
    }

    /**
     * Replaces the first occurrence of oldInput in node with newInput.
     *
     * @param node The node whose input should be changed.
     * @param oldInput The value to look for.
     * @param newInput The value to replace with.
     */
    public void replaceFirstInput(Node __node, Node __oldInput, Node __newInput)
    {
        // @closure
        add("replace first input", new Effect()
        {
            @Override
            public void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes)
            {
                if (__node.isAlive())
                {
                    __node.replaceFirstInput(__oldInput, __newInput);
                }
            }

            @Override
            public boolean isVisible()
            {
                return !(__node instanceof FrameState);
            }
        });
    }
}
