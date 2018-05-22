package graalvm.compiler.nodes.java;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * This node describes one locking scope; it ties the monitor enter, monitor exit and the frame
 * states together. It is thus referenced from the {@link MonitorEnterNode}, from the
 * {@link MonitorExitNode} and from the {@link FrameState}.
 */
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
    public void generate(NodeLIRBuilderTool generator)
    {
        // nothing to do
    }
}
