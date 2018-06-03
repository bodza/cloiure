package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;

/**
 * Value {@link PhiNode}s merge data flow values at control flow merges.
 */
// @class ValuePhiNode
public final class ValuePhiNode extends PhiNode implements ArrayLengthProvider
{
    // @def
    public static final NodeClass<ValuePhiNode> TYPE = NodeClass.create(ValuePhiNode.class);

    @Input
    // @field
    protected NodeInputList<ValueNode> values;

    // @cons
    public ValuePhiNode(Stamp __stamp, AbstractMergeNode __merge)
    {
        this(TYPE, __stamp, __merge);
    }

    // @cons
    protected ValuePhiNode(NodeClass<? extends ValuePhiNode> __c, Stamp __stamp, AbstractMergeNode __merge)
    {
        super(__c, __stamp, __merge);
        values = new NodeInputList<>(this);
    }

    // @cons
    public ValuePhiNode(Stamp __stamp, AbstractMergeNode __merge, ValueNode[] __values)
    {
        super(TYPE, __stamp, __merge);
        this.values = new NodeInputList<>(this, __values);
    }

    @Override
    public NodeInputList<ValueNode> values()
    {
        return values;
    }

    @Override
    public boolean inferStamp()
    {
        // Meet all the values feeding this Phi but don't use the stamp of this Phi since that's what's being computed.
        Stamp __valuesStamp = StampTool.meetOrNull(values(), this);
        if (__valuesStamp == null)
        {
            __valuesStamp = stamp;
        }
        else if (stamp.isCompatible(__valuesStamp))
        {
            __valuesStamp = stamp.join(__valuesStamp);
        }
        return updateStamp(__valuesStamp);
    }

    @Override
    public ValueNode length()
    {
        if (merge() instanceof LoopBeginNode)
        {
            return null;
        }
        ValueNode __length = null;
        for (ValueNode __input : values())
        {
            ValueNode __l = GraphUtil.arrayLength(__input);
            if (__l == null)
            {
                return null;
            }
            if (__length == null)
            {
                __length = __l;
            }
            else if (__length != __l)
            {
                return null;
            }
        }
        return __length;
    }
}
