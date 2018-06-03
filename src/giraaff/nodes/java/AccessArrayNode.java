package giraaff.nodes.java;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;

/**
 * This the base class of all array operations.
 */
// @class AccessArrayNode
public abstract class AccessArrayNode extends FixedWithNextNode
{
    // @def
    public static final NodeClass<AccessArrayNode> TYPE = NodeClass.create(AccessArrayNode.class);

    @Input
    // @field
    protected ValueNode array;

    public ValueNode array()
    {
        return array;
    }

    /**
     * Creates a new AccessArrayNode.
     *
     * @param array the instruction that produces the array object value
     */
    // @cons
    public AccessArrayNode(NodeClass<? extends AccessArrayNode> __c, Stamp __stamp, ValueNode __array)
    {
        super(__c, __stamp);
        this.array = __array;
    }

    public void setArray(ValueNode __array)
    {
        updateUsages(this.array, __array);
        this.array = __array;
    }
}
