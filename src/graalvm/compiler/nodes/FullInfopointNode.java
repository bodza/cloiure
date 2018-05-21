package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.State;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.NodeWithState;

import jdk.vm.ci.code.site.InfopointReason;

/**
 * Nodes of this type are inserted into the graph to denote points of interest to debugging.
 */
public final class FullInfopointNode extends FixedWithNextNode implements LIRLowerable, NodeWithState, Simplifiable
{
    public static final NodeClass<FullInfopointNode> TYPE = NodeClass.create(FullInfopointNode.class);
    protected final InfopointReason reason;
    @Input(State) FrameState state;
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
