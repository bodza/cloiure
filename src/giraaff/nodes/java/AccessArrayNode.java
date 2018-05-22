package giraaff.nodes.java;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;

/**
 * This the base class of all array operations.
 */
public abstract class AccessArrayNode extends FixedWithNextNode
{
    public static final NodeClass<AccessArrayNode> TYPE = NodeClass.create(AccessArrayNode.class);
    @Input protected ValueNode array;

    public ValueNode array()
    {
        return array;
    }

    /**
     * Creates a new AccessArrayNode.
     *
     * @param array the instruction that produces the array object value
     */
    public AccessArrayNode(NodeClass<? extends AccessArrayNode> c, Stamp stamp, ValueNode array)
    {
        super(c, stamp);
        this.array = array;
    }

    public void setArray(ValueNode array)
    {
        updateUsages(this.array, array);
        this.array = array;
    }
}
