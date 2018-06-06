package giraaff.nodes;

import giraaff.graph.NodeClass;

///
// Denotes the merging of multiple control-flow paths.
///
// @class MergeNode
public final class MergeNode extends AbstractMergeNode
{
    // @def
    public static final NodeClass<MergeNode> TYPE = NodeClass.create(MergeNode.class);

    // @cons MergeNode
    public MergeNode()
    {
        super(TYPE);
    }

    public static void removeMergeIfDegenerated(MergeNode __node)
    {
        if (__node.forwardEndCount() == 1 && __node.hasNoUsages())
        {
            FixedNode __currentNext = __node.next();
            __node.setNext(null);
            EndNode __forwardEnd = __node.forwardEndAt(0);
            __forwardEnd.replaceAtPredecessor(__currentNext);
            __node.markDeleted();
            __forwardEnd.markDeleted();
        }
    }
}
