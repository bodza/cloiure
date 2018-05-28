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
public class ValuePhiNode extends PhiNode implements ArrayLengthProvider
{
    public static final NodeClass<ValuePhiNode> TYPE = NodeClass.create(ValuePhiNode.class);

    @Input protected NodeInputList<ValueNode> values;

    public ValuePhiNode(Stamp stamp, AbstractMergeNode merge)
    {
        this(TYPE, stamp, merge);
    }

    protected ValuePhiNode(NodeClass<? extends ValuePhiNode> c, Stamp stamp, AbstractMergeNode merge)
    {
        super(c, stamp, merge);
        values = new NodeInputList<>(this);
    }

    public ValuePhiNode(Stamp stamp, AbstractMergeNode merge, ValueNode[] values)
    {
        super(TYPE, stamp, merge);
        this.values = new NodeInputList<>(this, values);
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
        Stamp valuesStamp = StampTool.meetOrNull(values(), this);
        if (valuesStamp == null)
        {
            valuesStamp = stamp;
        }
        else if (stamp.isCompatible(valuesStamp))
        {
            valuesStamp = stamp.join(valuesStamp);
        }
        return updateStamp(valuesStamp);
    }

    @Override
    public ValueNode length()
    {
        if (merge() instanceof LoopBeginNode)
        {
            return null;
        }
        ValueNode length = null;
        for (ValueNode input : values())
        {
            ValueNode l = GraphUtil.arrayLength(input);
            if (l == null)
            {
                return null;
            }
            if (length == null)
            {
                length = l;
            }
            else if (length != l)
            {
                return null;
            }
        }
        return length;
    }

    @Override
    protected String valueDescription()
    {
        return stamp(NodeView.DEFAULT).unrestricted().toString();
    }
}
