package graalvm.compiler.nodes.java;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.AbstractStateSplit;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

public final class LoadExceptionObjectNode extends AbstractStateSplit implements Lowerable
{
    public static final NodeClass<LoadExceptionObjectNode> TYPE = NodeClass.create(LoadExceptionObjectNode.class);

    public LoadExceptionObjectNode(Stamp stamp)
    {
        super(TYPE, stamp);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }
}
