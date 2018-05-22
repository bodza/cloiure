package graalvm.compiler.nodes;

import jdk.vm.ci.meta.MetaAccessProvider;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;

/**
 * This node represents an unconditional explicit request for immediate deoptimization.
 *
 * After this node, execution will continue using a fallback execution engine (such as an
 * interpreter) at the position described by the {@link #stateBefore() deoptimization state}.
 */
public abstract class AbstractDeoptimizeNode extends ControlSinkNode implements IterableNodeType, DeoptimizingNode.DeoptBefore
{
    public static final NodeClass<AbstractDeoptimizeNode> TYPE = NodeClass.create(AbstractDeoptimizeNode.class);
    @OptionalInput(InputType.State) FrameState stateBefore;

    protected AbstractDeoptimizeNode(NodeClass<? extends AbstractDeoptimizeNode> c, FrameState stateBefore)
    {
        super(c, StampFactory.forVoid());
        this.stateBefore = stateBefore;
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
    public void setStateBefore(FrameState f)
    {
        updateUsages(stateBefore, f);
        stateBefore = f;
    }

    public abstract ValueNode getActionAndReason(MetaAccessProvider metaAccess);

    public abstract ValueNode getSpeculation(MetaAccessProvider metaAccess);
}
