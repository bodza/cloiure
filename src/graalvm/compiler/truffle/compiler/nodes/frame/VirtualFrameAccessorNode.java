package graalvm.compiler.truffle.compiler.nodes.frame;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.debug.ControlFlowAnchored;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import graalvm.compiler.nodes.spi.VirtualizerTool;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public abstract class VirtualFrameAccessorNode extends FixedWithNextNode implements ControlFlowAnchored {
    public static final NodeClass<VirtualFrameAccessorNode> TYPE = NodeClass.create(VirtualFrameAccessorNode.class);

    @Input protected NewFrameNode frame;

    protected final int frameSlotIndex;
    protected final int accessTag;

    protected VirtualFrameAccessorNode(NodeClass<? extends VirtualFrameAccessorNode> c, Stamp stamp, Receiver frame, int frameSlotIndex, int accessTag) {
        super(c, stamp);
        this.frame = (NewFrameNode) frame.get();
        this.frameSlotIndex = frameSlotIndex;
        this.accessTag = accessTag;
    }

    protected ValueNode getConstant(int n) {
        return frame.smallIntConstants.get(n);
    }

    protected void insertDeoptimization(VirtualizerTool tool) {
        /*
         * Escape analysis does not allow insertion of a DeoptimizeNode. We work around this
         * restriction by inserting an always-failing guard, which will be canonicalized to a
         * DeoptimizeNode later on.
         */
        LogicNode condition = LogicConstantNode.contradiction();
        tool.addNode(condition);
        JavaConstant speculation = graph().getSpeculationLog().speculate(frame.getIntrinsifyAccessorsSpeculation());
        tool.addNode(new FixedGuardNode(condition, DeoptimizationReason.RuntimeConstraint, DeoptimizationAction.InvalidateRecompile, speculation, false));

        if (getStackKind() == JavaKind.Void) {
            tool.delete();
        } else {
            /*
             * Even though all usages will be eventually dead, we need to provide a valid
             * replacement value for now.
             */
            ConstantNode unusedValue = ConstantNode.forConstant(JavaConstant.defaultForKind(getStackKind()), tool.getMetaAccessProvider());
            tool.addNode(unusedValue);
            tool.replaceWith(unusedValue);
        }
    }
}
