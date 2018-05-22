package graalvm.compiler.hotspot.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.nodes.ControlSinkNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Removes the current frame and tail calls the uncommon trap routine.
 */
public final class DeoptimizeCallerNode extends ControlSinkNode implements LIRLowerable
{
    public static final NodeClass<DeoptimizeCallerNode> TYPE = NodeClass.create(DeoptimizeCallerNode.class);
    protected final DeoptimizationAction action;
    protected final DeoptimizationReason reason;

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
