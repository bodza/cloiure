package giraaff.nodes.java;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * The {@code AccessIndexedNode} class is the base class of instructions that read or write elements
 * of an array.
 */
// @class AccessIndexedNode
public abstract class AccessIndexedNode extends AccessArrayNode implements Lowerable
{
    // @def
    public static final NodeClass<AccessIndexedNode> TYPE = NodeClass.create(AccessIndexedNode.class);

    @Input
    // @field
    protected ValueNode index;
    // @field
    protected final JavaKind elementKind;

    public ValueNode index()
    {
        return index;
    }

    /**
     * Create an new AccessIndexedNode.
     *
     * @param stamp the result kind of the access
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param elementKind the kind of the elements of the array
     */
    // @cons
    protected AccessIndexedNode(NodeClass<? extends AccessIndexedNode> __c, Stamp __stamp, ValueNode __array, ValueNode __index, JavaKind __elementKind)
    {
        super(__c, __stamp, __array);
        this.index = __index;
        this.elementKind = __elementKind;
    }

    /**
     * Gets the element type of the array.
     *
     * @return the element type
     */
    public JavaKind elementKind()
    {
        return elementKind;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }
}
