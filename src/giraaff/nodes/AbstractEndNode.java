package giraaff.nodes;

import java.util.Collections;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class AbstractEndNode
public abstract class AbstractEndNode extends FixedNode implements LIRLowerable
{
    public static final NodeClass<AbstractEndNode> TYPE = NodeClass.create(AbstractEndNode.class);

    // @cons
    protected AbstractEndNode(NodeClass<? extends AbstractEndNode> c)
    {
        super(c, StampFactory.forVoid());
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.visitEndNode(this);
    }

    public AbstractMergeNode merge()
    {
        return (AbstractMergeNode) usages().first();
    }

    @Override
    public Iterable<? extends Node> cfgSuccessors()
    {
        AbstractMergeNode merge = merge();
        if (merge != null)
        {
            return Collections.singletonList(merge);
        }
        return Collections.emptyList();
    }
}
