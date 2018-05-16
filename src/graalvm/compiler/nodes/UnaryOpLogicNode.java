package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.TriState;

@NodeInfo
public abstract class UnaryOpLogicNode extends LogicNode implements LIRLowerable, Canonicalizable.Unary<ValueNode>
{
    public static final NodeClass<UnaryOpLogicNode> TYPE = NodeClass.create(UnaryOpLogicNode.class);
    @Input protected ValueNode value;

    @Override
    public ValueNode getValue()
    {
        return value;
    }

    public UnaryOpLogicNode(NodeClass<? extends UnaryOpLogicNode> c, ValueNode value)
    {
        super(c);
        assert value != null;
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
    }

    public Stamp getSucceedingStampForValue(boolean negated, Stamp valueStamp)
    {
        Stamp succStamp = getSucceedingStampForValue(negated);
        if (succStamp != null)
        {
            succStamp = succStamp.join(valueStamp);
        }
        return succStamp;
    }

    public abstract Stamp getSucceedingStampForValue(boolean negated);

    public abstract TriState tryFold(Stamp valueStamp);
}
