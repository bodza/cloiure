package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Removes the current frame and tail calls the uncommon trap routine.
///
// @class DeoptimizeCallerNode
public final class DeoptimizeCallerNode extends ControlSinkNode implements LIRLowerable
{
    // @def
    public static final NodeClass<DeoptimizeCallerNode> TYPE = NodeClass.create(DeoptimizeCallerNode.class);

    // @field
    protected final DeoptimizationAction ___action;
    // @field
    protected final DeoptimizationReason ___reason;

    // @cons DeoptimizeCallerNode
    public DeoptimizeCallerNode(DeoptimizationAction __action, DeoptimizationReason __reason)
    {
        super(TYPE, StampFactory.forVoid());
        this.___action = __action;
        this.___reason = __reason;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitDeoptimizeCaller(this.___action, this.___reason);
    }

    @Node.NodeIntrinsic
    public static native void deopt(@Node.ConstantNodeParameter DeoptimizationAction __action, @Node.ConstantNodeParameter DeoptimizationReason __reason);
}
