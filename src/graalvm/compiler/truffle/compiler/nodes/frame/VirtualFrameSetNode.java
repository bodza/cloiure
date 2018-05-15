package graalvm.compiler.truffle.compiler.nodes.frame;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;

import jdk.vm.ci.meta.JavaKind;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class VirtualFrameSetNode extends VirtualFrameAccessorNode implements Virtualizable {
    public static final NodeClass<VirtualFrameSetNode> TYPE = NodeClass.create(VirtualFrameSetNode.class);

    @Input private ValueNode value;

    public VirtualFrameSetNode(Receiver frame, int frameSlotIndex, int accessTag, ValueNode value) {
        super(TYPE, StampFactory.forVoid(), frame, frameSlotIndex, accessTag);
        this.value = value;
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
                tool.setVirtualEntry(tagVirtual, frameSlotIndex, getConstant(accessTag));

                ValueNode dataEntry = tool.getEntry(dataVirtual, frameSlotIndex);
                if (dataEntry.getStackKind() == value.getStackKind()) {
                    if (tool.setVirtualEntry(dataVirtual, frameSlotIndex, value, value.getStackKind(), -1)) {
                        tool.delete();
                        return;
                    }
                }
            }
        }

        /*
         * Deoptimization is our only option here. We cannot go back to a UnsafeStoreNode because we
         * do not have a FrameState to use for the memory store.
         */
        insertDeoptimization(tool);
    }
}
