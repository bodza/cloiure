package giraaff.nodes;

import jdk.vm.ci.code.site.InfopointReason;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.NodeWithState;

/**
 * Nodes of this type are inserted into the graph to denote points of interest to debugging.
 */
public final class FullInfopointNode extends FixedWithNextNode implements LIRLowerable, NodeWithState, Simplifiable
{
    public static final NodeClass<FullInfopointNode> TYPE = NodeClass.create(FullInfopointNode.class);
    protected final InfopointReason reason;
    @Input(InputType.State) FrameState state;
    @OptionalInput ValueNode escapedReturnValue;

    public FullInfopointNode(InfopointReason reason, FrameState state, ValueNode escapedReturnValue)
    {
        super(TYPE, StampFactory.forVoid());
        this.reason = reason;
        this.state = state;
        this.escapedReturnValue = escapedReturnValue;
    }

    public InfopointReason getReason()
    {
        return reason;
    }

    private void setEscapedReturnValue(ValueNode x)
    {
        updateUsages(escapedReturnValue, x);
        escapedReturnValue = x;
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        if (escapedReturnValue != null && state != null && state.outerFrameState() != null)
        {
            ValueNode returnValue = escapedReturnValue;
            setEscapedReturnValue(null);
            tool.removeIfUnused(returnValue);
        }
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        generator.visitFullInfopointNode(this);
    }

    public FrameState getState()
    {
        return state;
    }
}