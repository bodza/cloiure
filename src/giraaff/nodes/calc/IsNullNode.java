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

///
// An IsNullNode will be true if the supplied value is null, and false if it is non-null.
///
// @class IsNullNode
public final class IsNullNode extends UnaryOpLogicNode implements LIRLowerable, Virtualizable
{
    // @def
    public static final NodeClass<IsNullNode> TYPE = NodeClass.create(IsNullNode.class);

    // @cons
    public IsNullNode(ValueNode __object)
    {
        super(TYPE, __object);
    }

    public static LogicNode create(ValueNode __forValue)
    {
        return canonicalized(null, __forValue);
    }

    public static LogicNode tryCanonicalize(ValueNode __forValue)
    {
        if (StampTool.isPointerAlwaysNull(__forValue))
        {
            return LogicConstantNode.tautology();
        }
        else if (StampTool.isPointerNonNull(__forValue))
        {
            return LogicConstantNode.contradiction();
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // Nothing to do.
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        return canonicalized(this, __forValue);
    }

    private static LogicNode canonicalized(IsNullNode __isNullNode, ValueNode __forValue)
    {
        IsNullNode __self = __isNullNode;
        LogicNode __result = tryCanonicalize(__forValue);
        if (__result != null)
        {
            return __result;
        }

        if (__forValue instanceof PiNode)
        {
            return IsNullNode.create(GraphUtil.skipPi(__forValue));
        }

        if (__forValue instanceof ConvertNode)
        {
            ConvertNode __convertNode = (ConvertNode) __forValue;
            if (__convertNode.mayNullCheckSkipConversion())
            {
                return IsNullNode.create(__convertNode.getValue());
            }
        }

        if (__self == null)
        {
            __self = new IsNullNode(GraphUtil.skipPi(__forValue));
        }
        return __self;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(getValue());
        TriState __fold = tryFold(__alias.stamp(NodeView.DEFAULT));
        if (__fold != TriState.UNKNOWN)
        {
            __tool.replaceWithValue(LogicConstantNode.forBoolean(__fold.isTrue(), graph()));
        }
    }

    @Override
    public Stamp getSucceedingStampForValue(boolean __negated)
    {
        // ignore any more precise input stamp, since canonicalization will skip through PiNodes
        AbstractPointerStamp __pointerStamp = (AbstractPointerStamp) getValue().stamp(NodeView.DEFAULT).unrestricted();
        return __negated ? __pointerStamp.asNonNull() : __pointerStamp.asAlwaysNull();
    }

    @Override
    public TriState tryFold(Stamp __valueStamp)
    {
        if (__valueStamp instanceof ObjectStamp)
        {
            ObjectStamp __objectStamp = (ObjectStamp) __valueStamp;
            if (__objectStamp.alwaysNull())
            {
                return TriState.TRUE;
            }
            else if (__objectStamp.nonNull())
            {
                return TriState.FALSE;
            }
        }
        return TriState.UNKNOWN;
    }
}
