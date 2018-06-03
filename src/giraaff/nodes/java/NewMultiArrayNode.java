package giraaff.nodes.java;

import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.graph.NodeList;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * The {@code NewMultiArrayNode} represents an allocation of a multi-dimensional object array.
 */
// @class NewMultiArrayNode
public final class NewMultiArrayNode extends DeoptimizingFixedWithNextNode implements Lowerable, ArrayLengthProvider
{
    // @def
    public static final NodeClass<NewMultiArrayNode> TYPE = NodeClass.create(NewMultiArrayNode.class);

    @Input
    // @field
    protected NodeInputList<ValueNode> dimensions;
    // @field
    protected final ResolvedJavaType type;

    public ValueNode dimension(int __index)
    {
        return dimensions.get(__index);
    }

    public int dimensionCount()
    {
        return dimensions.size();
    }

    public NodeList<ValueNode> dimensions()
    {
        return dimensions;
    }

    // @cons
    public NewMultiArrayNode(ResolvedJavaType __type, ValueNode[] __dimensions)
    {
        this(TYPE, __type, __dimensions);
    }

    // @cons
    protected NewMultiArrayNode(NodeClass<? extends NewMultiArrayNode> __c, ResolvedJavaType __type, ValueNode[] __dimensions)
    {
        super(__c, StampFactory.objectNonNull(TypeReference.createExactTrusted(__type)));
        this.type = __type;
        this.dimensions = new NodeInputList<>(this, __dimensions);
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public ResolvedJavaType type()
    {
        return type;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public ValueNode length()
    {
        return dimension(0);
    }
}
