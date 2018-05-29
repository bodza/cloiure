package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.GuardingNode;

// @NodeInfo.allowedUsageTypes "Value, Anchor, Guard"
// @class SnippetAnchorNode
public final class SnippetAnchorNode extends FixedWithNextNode implements Simplifiable, GuardingNode
{
    public static final NodeClass<SnippetAnchorNode> TYPE = NodeClass.create(SnippetAnchorNode.class);

    // @cons
    public SnippetAnchorNode()
    {
        super(TYPE, StampFactory.object());
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        AbstractBeginNode prevBegin = AbstractBeginNode.prevBegin(this);
        replaceAtUsages(InputType.Anchor, prevBegin);
        replaceAtUsages(InputType.Guard, prevBegin);
        if (tool.allUsagesAvailable() && hasNoUsages())
        {
            graph().removeFixed(this);
        }
    }

    @NodeIntrinsic
    public static native GuardingNode anchor();
}
