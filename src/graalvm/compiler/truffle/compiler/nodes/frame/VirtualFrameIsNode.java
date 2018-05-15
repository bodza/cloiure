package graalvm.compiler.truffle.compiler.nodes.frame;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.ConditionalNode;
import graalvm.compiler.nodes.calc.IntegerEqualsNode;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

import jdk.vm.ci.meta.JavaKind;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class VirtualFrameIsNode extends VirtualFrameAccessorNode implements Virtualizable {
    public static final NodeClass<VirtualFrameIsNode> TYPE = NodeClass.create(VirtualFrameIsNode.class);

    public VirtualFrameIsNode(Receiver frame, int frameSlotIndex, int accessTag) {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean), frame, frameSlotIndex, accessTag);
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode tagAlias = tool.getAlias(frame.virtualFrameTagArray);

        if (tagAlias instanceof VirtualObjectNode) {
            VirtualObjectNode tagVirtual = (VirtualObjectNode) tagAlias;

            if (frameSlotIndex < tagVirtual.entryCount()) {
                ValueNode actualTag = tool.getEntry(tagVirtual, frameSlotIndex);
                if (actualTag.isConstant()) {
                    tool.replaceWith(getConstant(actualTag.asJavaConstant().asInt() == accessTag ? 1 : 0));
                } else {
                    LogicNode comparison = new IntegerEqualsNode(actualTag, getConstant(accessTag));
                    tool.addNode(comparison);
                    ConditionalNode result = new ConditionalNode(comparison, getConstant(1), getConstant(0));
                    tool.addNode(result);
                    tool.replaceWith(result);
                }
                return;
            }
        }

        /*
         * We could "virtualize" to a UnsafeLoadNode here that remains a memory access. But it is
         * simpler, and consistent with the get and set intrinsification, to deoptimize.
         */
        insertDeoptimization(tool);
    }
}
