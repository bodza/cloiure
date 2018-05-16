package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.type.AbstractPointerStamp;
import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeCycles;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.UnaryOpLogicNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.nodes.util.GraphUtil;

import jdk.vm.ci.meta.TriState;

/**
 * An IsNullNode will be true if the supplied value is null, and false if it is non-null.
 */
@NodeInfo(cycles = NodeCycles.CYCLES_2)
public final class IsNullNode extends UnaryOpLogicNode implements LIRLowerable, Virtualizable
{
    public static final NodeClass<IsNullNode> TYPE = NodeClass.create(IsNullNode.class);

    public IsNullNode(ValueNode object)
    {
        super(TYPE, object);
        assert object != null;
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
    public boolean verify()
    {
        assertTrue(getValue() != null, "is null input must not be null");
        assertTrue(getValue().stamp(NodeView.DEFAULT) instanceof AbstractPointerStamp, "input must be a pointer not %s", getValue().stamp(NodeView.DEFAULT));
        return super.verify();
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
        // Ignore any more precise input stamp since canonicalization will skip through PiNodes
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
