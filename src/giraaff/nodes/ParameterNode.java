package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampPair;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.spi.UncheckedInterfaceProvider;

/**
 * The {@code Parameter} instruction is a placeholder for an incoming argument to a function call.
 */
// @class ParameterNode
public final class ParameterNode extends AbstractLocalNode implements IterableNodeType, UncheckedInterfaceProvider
{
    public static final NodeClass<ParameterNode> TYPE = NodeClass.create(ParameterNode.class);

    private Stamp uncheckedStamp;

    // @cons
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
