package graalvm.compiler.nodes;

import java.util.Collections;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public abstract class AbstractEndNode extends FixedNode implements LIRLowerable
{
    public static final NodeClass<AbstractEndNode> TYPE = NodeClass.create(AbstractEndNode.class);

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
