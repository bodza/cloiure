package graalvm.compiler.nodes.memory.address;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node.IndirectCanonicalization;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.StructuralInput;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;

/**
 * Base class for nodes that deal with addressing calculation.
 */
public abstract class AddressNode extends FloatingNode implements IndirectCanonicalization
{
    public static final NodeClass<AddressNode> TYPE = NodeClass.create(AddressNode.class);

    protected AddressNode(NodeClass<? extends AddressNode> c)
    {
        super(c, StampFactory.pointer());
    }

    public abstract static class Address extends StructuralInput.Association
    {
    }

    public abstract ValueNode getBase();

    public abstract ValueNode getIndex();

    /**
     * Constant that is the maximum displacement from the base and index for this address. This
     * value is used to determine whether using the access as an implicit null check on the base is
     * valid.
     *
     * @return the maximum distance in bytes from the base that this address can be
     */
    public abstract long getMaxConstantDisplacement();
}
