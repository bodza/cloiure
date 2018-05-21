package graalvm.compiler.nodes.java;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.graph.NodeList;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArrayLengthProvider;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.ResolvedJavaType;

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
