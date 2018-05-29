package giraaff.nodes.memory.address;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node.IndirectCanonicalization;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.StructuralInput;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;

/**
 * Base class for nodes that deal with addressing calculation.
 */
// @NodeInfo.allowedUsageTypes "Association"
// @class AddressNode
public abstract class AddressNode extends FloatingNode implements IndirectCanonicalization
{
    public static final NodeClass<AddressNode> TYPE = NodeClass.create(AddressNode.class);

    // @cons
    protected AddressNode(NodeClass<? extends AddressNode> c)
    {
        super(c, StampFactory.pointer());
    }

    // @class AddressNode.Address
    public abstract static class Address extends StructuralInput.Association
    {
    }

    public abstract ValueNode getBase();

    public abstract ValueNode getIndex();

    /**
     * Constant that is the maximum displacement from the base and index for this address. This value
     * is used to determine whether using the access as an implicit null check on the base is valid.
     *
     * @return the maximum distance in bytes from the base that this address can be
     */
    public abstract long getMaxConstantDisplacement();
}
