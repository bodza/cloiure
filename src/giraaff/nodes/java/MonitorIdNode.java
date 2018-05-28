package giraaff.nodes.java;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * This node describes one locking scope; it ties the monitor enter, monitor exit and
 * the frame states together. It is thus referenced from the {@link MonitorEnterNode},
 * from the {@link MonitorExitNode} and from the {@link FrameState}.
 */
// @NodeInfo.allowedUsageTypes "Association"
public class MonitorIdNode extends ValueNode implements IterableNodeType, LIRLowerable
{
    public static final NodeClass<MonitorIdNode> TYPE = NodeClass.create(MonitorIdNode.class);

    protected int lockDepth;
    protected boolean eliminated;

    public MonitorIdNode(int lockDepth)
    {
        this(TYPE, lockDepth);
    }

    protected MonitorIdNode(NodeClass<? extends MonitorIdNode> c, int lockDepth)
    {
        super(c, StampFactory.forVoid());
        this.lockDepth = lockDepth;
    }

    public int getLockDepth()
    {
        return lockDepth;
    }

    public void setLockDepth(int lockDepth)
    {
        this.lockDepth = lockDepth;
    }

    public boolean isEliminated()
    {
        return eliminated;
    }

    public void setEliminated()
    {
        eliminated = true;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // nothing to do
    }
}
