package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_8;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Unwinds the current frame to an exception handler in the caller frame.
 */
@NodeInfo(cycles = CYCLES_8, size = SIZE_8, cyclesRationale = "stub call", sizeRationale = "stub call")
public final class UnwindNode extends ControlSinkNode implements Lowerable, LIRLowerable
{
    public static final NodeClass<UnwindNode> TYPE = NodeClass.create(UnwindNode.class);
    @Input ValueNode exception;

    public ValueNode exception()
    {
        return exception;
    }

    public UnwindNode(ValueNode exception)
    {
        super(TYPE, StampFactory.forVoid());
        this.exception = exception;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().emitUnwind(gen.operand(exception()));
    }
}
