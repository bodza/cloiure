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
    // @def
    public static final NodeClass<AbstractEndNode> TYPE = NodeClass.create(AbstractEndNode.class);

    // @cons AbstractEndNode
    protected AbstractEndNode(NodeClass<? extends AbstractEndNode> __c)
    {
        super(__c, StampFactory.forVoid());
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.visitEndNode(this);
    }

    public AbstractMergeNode merge()
    {
        return (AbstractMergeNode) usages().first();
    }

    @Override
    public Iterable<? extends Node> cfgSuccessors()
    {
        AbstractMergeNode __merge = merge();
        if (__merge != null)
        {
            return Collections.singletonList(__merge);
        }
        return Collections.emptyList();
    }
}
