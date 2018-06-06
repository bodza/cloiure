package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.GuardingNode;

// @NodeInfo.allowedUsageTypes "InputType.Value, InputType.Anchor, InputType.Guard"
// @class SnippetAnchorNode
public final class SnippetAnchorNode extends FixedWithNextNode implements Simplifiable, GuardingNode
{
    // @def
    public static final NodeClass<SnippetAnchorNode> TYPE = NodeClass.create(SnippetAnchorNode.class);

    // @cons SnippetAnchorNode
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

    @Node.NodeIntrinsic
    public static native GuardingNode anchor();
}
