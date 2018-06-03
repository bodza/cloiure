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
    // @def
    public static final NodeClass<UnwindNode> TYPE = NodeClass.create(UnwindNode.class);

    @Input
    // @field
    ValueNode exception;

    public ValueNode exception()
    {
        return exception;
    }

    // @cons
    public UnwindNode(ValueNode __exception)
    {
        super(TYPE, StampFactory.forVoid());
        this.exception = __exception;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitUnwind(__gen.operand(exception()));
    }
}
