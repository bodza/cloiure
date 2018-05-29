package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Removes the current frame and tail calls the uncommon trap routine.
 */
// @class DeoptimizeCallerNode
public final class DeoptimizeCallerNode extends ControlSinkNode implements LIRLowerable
{
    public static final NodeClass<DeoptimizeCallerNode> TYPE = NodeClass.create(DeoptimizeCallerNode.class);

    protected final DeoptimizationAction action;
    protected final DeoptimizationReason reason;

    // @cons
    public DeoptimizeCallerNode(DeoptimizationAction action, DeoptimizationReason reason)
    {
        super(TYPE, StampFactory.forVoid());
        this.action = action;
        this.reason = reason;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitDeoptimizeCaller(action, reason);
    }

    @NodeIntrinsic
    public static native void deopt(@ConstantNodeParameter DeoptimizationAction action, @ConstantNodeParameter DeoptimizationReason reason);
}
