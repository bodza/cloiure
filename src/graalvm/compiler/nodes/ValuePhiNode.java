package graalvm.compiler.nodes;

import java.util.Map;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.ArrayLengthProvider;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.util.CollectionsUtil;

/**
 * Value {@link PhiNode}s merge data flow values at control flow merges.
 */
@NodeInfo(nameTemplate = "Phi({i#values}, {p#valueDescription})")
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
        assert stamp != StampFactory.forVoid();
        values = new NodeInputList<>(this);
    }

    public ValuePhiNode(Stamp stamp, AbstractMergeNode merge, ValueNode[] values)
    {
        super(TYPE, stamp, merge);
        assert stamp != StampFactory.forVoid();
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
        /*
         * Meet all the values feeding this Phi but don't use the stamp of this Phi since that's
         * what's being computed.
         */
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
    public boolean verify()
    {
        Stamp s = null;
        for (ValueNode input : values())
        {
            assert input != null;
            if (s == null)
            {
                s = input.stamp(NodeView.DEFAULT);
            }
            else
            {
                if (!s.isCompatible(input.stamp(NodeView.DEFAULT)))
                {
                    fail("Phi Input Stamps are not compatible. Phi:%s inputs:%s", this, CollectionsUtil.mapAndJoin(values(), x -> x.toString() + ":" + x.stamp(NodeView.DEFAULT), ", "));
                }
            }
        }
        return super.verify();
    }

    @Override
    protected String valueDescription()
    {
        return stamp(NodeView.DEFAULT).unrestricted().toString();
    }

    @Override
    public Map<Object, Object> getDebugProperties(Map<Object, Object> map)
    {
        Map<Object, Object> properties = super.getDebugProperties(map);
        properties.put("valueDescription", valueDescription());
        return properties;
    }
}
