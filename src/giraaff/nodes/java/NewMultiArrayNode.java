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
public class NewMultiArrayNode extends DeoptimizingFixedWithNextNode implements Lowerable, ArrayLengthProvider
{
    public static final NodeClass<NewMultiArrayNode> TYPE = NodeClass.create(NewMultiArrayNode.class);

    @Input protected NodeInputList<ValueNode> dimensions;
    protected final ResolvedJavaType type;

    public ValueNode dimension(int index)
    {
        return dimensions.get(index);
    }

    public int dimensionCount()
    {
        return dimensions.size();
    }

    public NodeList<ValueNode> dimensions()
    {
        return dimensions;
    }

    public NewMultiArrayNode(ResolvedJavaType type, ValueNode[] dimensions)
    {
        this(TYPE, type, dimensions);
    }

    protected NewMultiArrayNode(NodeClass<? extends NewMultiArrayNode> c, ResolvedJavaType type, ValueNode[] dimensions)
    {
        super(c, StampFactory.objectNonNull(TypeReference.createExactTrusted(type)));
        this.type = type;
        this.dimensions = new NodeInputList<>(this, dimensions);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
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
