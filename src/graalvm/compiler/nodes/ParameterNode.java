package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.UncheckedInterfaceProvider;

/**
 * The {@code Parameter} instruction is a placeholder for an incoming argument to a function call.
 */
@NodeInfo(nameTemplate = "P({p#index})")
public final class ParameterNode extends AbstractLocalNode implements IterableNodeType, UncheckedInterfaceProvider
{
    public static final NodeClass<ParameterNode> TYPE = NodeClass.create(ParameterNode.class);

    private Stamp uncheckedStamp;

    public ParameterNode(int index, StampPair stamp)
    {
        super(TYPE, index, stamp.getTrustedStamp());
        this.uncheckedStamp = stamp.getUncheckedStamp();
    }

    @Override
    public Stamp uncheckedStamp()
    {
        return uncheckedStamp;
    }
}
