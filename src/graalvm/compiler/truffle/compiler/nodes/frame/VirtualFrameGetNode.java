package graalvm.compiler.truffle.compiler.nodes.frame;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.IntegerEqualsNode;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class VirtualFrameGetNode extends VirtualFrameAccessorNode implements Virtualizable {
    public static final NodeClass<VirtualFrameGetNode> TYPE = NodeClass.create(VirtualFrameGetNode.class);

    public VirtualFrameGetNode(Receiver frame, int frameSlotIndex, JavaKind accessKind, int accessTag) {
        super(TYPE, StampFactory.forKind(accessKind), frame, frameSlotIndex, accessTag);
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode tagAlias = tool.getAlias(frame.virtualFrameTagArray);
        ValueNode dataAlias = tool.getAlias(
                        TruffleCompilerRuntime.getRuntime().getJavaKindForFrameSlotKind(accessTag) == JavaKind.Object ? frame.virtualFrameObjectArray : frame.virtualFramePrimitiveArray);

        if (tagAlias instanceof VirtualObjectNode && dataAlias instanceof VirtualObjectNode) {
            VirtualObjectNode tagVirtual = (VirtualObjectNode) tagAlias;
            VirtualObjectNode dataVirtual = (VirtualObjectNode) dataAlias;

            if (frameSlotIndex < tagVirtual.entryCount() && frameSlotIndex < dataVirtual.entryCount()) {
                ValueNode actualTag = tool.getEntry(tagVirtual, frameSlotIndex);
                if (!actualTag.isConstant() || actualTag.asJavaConstant().asInt() != accessTag) {
                    /*
                     * We cannot constant fold the tag-check immediately, so we need to create a
                     * guard comparing the actualTag with the accessTag.
                     */
                    LogicNode comparison = new IntegerEqualsNode(actualTag, getConstant(accessTag));
                    tool.addNode(comparison);
                    tool.addNode(new FixedGuardNode(comparison, DeoptimizationReason.RuntimeConstraint, DeoptimizationAction.InvalidateRecompile));
                }

                ValueNode dataEntry = tool.getEntry(dataVirtual, frameSlotIndex);
                if (dataEntry.getStackKind() == getStackKind()) {
                    tool.replaceWith(dataEntry);
                    return;
                }
            }
        }

        /*
         * We could "virtualize" to a UnsafeLoadNode here that remains a memory access. However,
         * that could prevent further escape analysis for parts of the method that actually matter.
         * So we just deoptimize.
         */
        insertDeoptimization(tool);
    }
}
