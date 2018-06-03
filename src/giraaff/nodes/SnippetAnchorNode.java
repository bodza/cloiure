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
    // @def
    public static final NodeClass<SnippetAnchorNode> TYPE = NodeClass.create(SnippetAnchorNode.class);

    // @cons
    public SnippetAnchorNode()
    {
        super(TYPE, StampFactory.object());
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        AbstractBeginNode __prevBegin = AbstractBeginNode.prevBegin(this);
        replaceAtUsages(InputType.Anchor, __prevBegin);
        replaceAtUsages(InputType.Guard, __prevBegin);
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            graph().removeFixed(this);
        }
    }

    @NodeIntrinsic
    public static native GuardingNode anchor();
}
