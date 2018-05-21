package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Anchor;
import static graalvm.compiler.nodeinfo.InputType.Guard;
import static graalvm.compiler.nodeinfo.InputType.Value;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodes.extended.GuardingNode;

public final class SnippetAnchorNode extends FixedWithNextNode implements Simplifiable, GuardingNode
{
    public static final NodeClass<SnippetAnchorNode> TYPE = NodeClass.create(SnippetAnchorNode.class);

    public SnippetAnchorNode()
    {
        super(TYPE, StampFactory.object());
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        AbstractBeginNode prevBegin = AbstractBeginNode.prevBegin(this);
        replaceAtUsages(Anchor, prevBegin);
        replaceAtUsages(Guard, prevBegin);
        if (tool.allUsagesAvailable() && hasNoUsages())
        {
            graph().removeFixed(this);
        }
    }

    @NodeIntrinsic
    public static native GuardingNode anchor();
}
