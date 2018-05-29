package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Unwinds the current frame to an exception handler in the caller frame.
 */
// @class UnwindNode
public final class UnwindNode extends ControlSinkNode implements Lowerable, LIRLowerable
{
    public static final NodeClass<UnwindNode> TYPE = NodeClass.create(UnwindNode.class);

    @Input ValueNode exception;

    public ValueNode exception()
    {
        return exception;
    }

    // @cons
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
