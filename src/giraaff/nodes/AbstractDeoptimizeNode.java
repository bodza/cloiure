package giraaff.nodes;

import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

/**
 * This node represents an unconditional explicit request for immediate deoptimization.
 *
 * After this node, execution will continue using a fallback execution engine (such as an
 * interpreter) at the position described by the {@link #stateBefore() deoptimization state}.
 */
// @class AbstractDeoptimizeNode
public abstract class AbstractDeoptimizeNode extends ControlSinkNode implements IterableNodeType, DeoptimizingNode.DeoptBefore
{
    // @def
    public static final NodeClass<AbstractDeoptimizeNode> TYPE = NodeClass.create(AbstractDeoptimizeNode.class);

    @OptionalInput(InputType.State)
    // @field
    FrameState stateBefore;

    // @cons
    protected AbstractDeoptimizeNode(NodeClass<? extends AbstractDeoptimizeNode> __c, FrameState __stateBefore)
    {
        super(__c, StampFactory.forVoid());
        this.stateBefore = __stateBefore;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public FrameState stateBefore()
    {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __f)
    {
        updateUsages(stateBefore, __f);
        stateBefore = __f;
    }

    public abstract ValueNode getActionAndReason(MetaAccessProvider metaAccess);

    public abstract ValueNode getSpeculation(MetaAccessProvider metaAccess);
}
