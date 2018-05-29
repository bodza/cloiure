package giraaff.nodes.calc;

import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.UnaryOpLogicNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;

/**
 * An IsNullNode will be true if the supplied value is null, and false if it is non-null.
 */
// @class IsNullNode
public final class IsNullNode extends UnaryOpLogicNode implements LIRLowerable, Virtualizable
{
    public static final NodeClass<IsNullNode> TYPE = NodeClass.create(IsNullNode.class);

    // @cons
    public IsNullNode(ValueNode object)
    {
        super(TYPE, object);
    }

    public static LogicNode create(ValueNode forValue)
    {
        return canonicalized(null, forValue);
    }

    public static LogicNode tryCanonicalize(ValueNode forValue)
    {
        if (StampTool.isPointerAlwaysNull(forValue))
        {
            return LogicConstantNode.tautology();
        }
        else if (StampTool.isPointerNonNull(forValue))
        {
            return LogicConstantNode.contradiction();
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // Nothing to do.
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        return canonicalized(this, forValue);
    }

    private static LogicNode canonicalized(IsNullNode isNullNode, ValueNode forValue)
    {
        IsNullNode self = isNullNode;
        LogicNode result = tryCanonicalize(forValue);
        if (result != null)
        {
            return result;
        }

        if (forValue instanceof PiNode)
        {
            return IsNullNode.create(GraphUtil.skipPi(forValue));
        }

        if (forValue instanceof ConvertNode)
        {
            ConvertNode convertNode = (ConvertNode) forValue;
            if (convertNode.mayNullCheckSkipConversion())
            {
                return IsNullNode.create(convertNode.getValue());
            }
        }

        if (self == null)
        {
            self = new IsNullNode(GraphUtil.skipPi(forValue));
        }
        return self;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(getValue());
        TriState fold = tryFold(alias.stamp(NodeView.DEFAULT));
        if (fold != TriState.UNKNOWN)
        {
            tool.replaceWithValue(LogicConstantNode.forBoolean(fold.isTrue(), graph()));
        }
    }

    @Override
    public Stamp getSucceedingStampForValue(boolean negated)
    {
        // ignore any more precise input stamp, since canonicalization will skip through PiNodes
        AbstractPointerStamp pointerStamp = (AbstractPointerStamp) getValue().stamp(NodeView.DEFAULT).unrestricted();
        return negated ? pointerStamp.asNonNull() : pointerStamp.asAlwaysNull();
    }

    @Override
    public TriState tryFold(Stamp valueStamp)
    {
        if (valueStamp instanceof ObjectStamp)
        {
            ObjectStamp objectStamp = (ObjectStamp) valueStamp;
            if (objectStamp.alwaysNull())
            {
                return TriState.TRUE;
            }
            else if (objectStamp.nonNull())
            {
                return TriState.FALSE;
            }
        }
        return TriState.UNKNOWN;
    }
}
