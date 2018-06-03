package giraaff.nodes.java;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// This node describes one locking scope; it ties the monitor enter, monitor exit and
// the frame states together. It is thus referenced from the {@link MonitorEnterNode},
// from the {@link MonitorExitNode} and from the {@link FrameState}.
///
// @NodeInfo.allowedUsageTypes "Association"
// @class MonitorIdNode
public final class MonitorIdNode extends ValueNode implements IterableNodeType, LIRLowerable
{
    // @def
    public static final NodeClass<MonitorIdNode> TYPE = NodeClass.create(MonitorIdNode.class);

    // @field
    protected int ___lockDepth;
    // @field
    protected boolean ___eliminated;

    // @cons
    public MonitorIdNode(int __lockDepth)
    {
        this(TYPE, __lockDepth);
    }

    // @cons
    protected MonitorIdNode(NodeClass<? extends MonitorIdNode> __c, int __lockDepth)
    {
        super(__c, StampFactory.forVoid());
        this.___lockDepth = __lockDepth;
    }

    public int getLockDepth()
    {
        return this.___lockDepth;
    }

    public void setLockDepth(int __lockDepth)
    {
        this.___lockDepth = __lockDepth;
    }

    public boolean isEliminated()
    {
        return this.___eliminated;
    }

    public void setEliminated()
    {
        this.___eliminated = true;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // nothing to do
    }
}
