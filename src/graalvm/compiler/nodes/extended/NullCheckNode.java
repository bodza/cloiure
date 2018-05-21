package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.InputType.Guard;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class NullCheckNode extends DeoptimizingFixedWithNextNode implements LIRLowerable, GuardingNode
{
    public static final NodeClass<NullCheckNode> TYPE = NodeClass.create(NullCheckNode.class);
    @Input ValueNode object;

    public NullCheckNode(ValueNode object)
    {
        super(TYPE, StampFactory.forVoid());
        this.object = object;
    }

    public ValueNode getObject()
    {
        return object;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        generator.getLIRGeneratorTool().emitNullCheck(generator.operand(object), generator.state(this));
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @NodeIntrinsic
    public static native void nullCheck(Object object);
}
