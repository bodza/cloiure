package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class StoreHubNode extends FixedWithNextNode implements Lowerable {

    public static final NodeClass<StoreHubNode> TYPE = NodeClass.create(StoreHubNode.class);
    @Input ValueNode value;
    @Input ValueNode object;

    public ValueNode getValue() {
        return value;
    }

    public ValueNode getObject() {
        return object;
    }

    public StoreHubNode(ValueNode object, ValueNode value) {
        super(TYPE, StampFactory.forVoid());
        this.value = value;
        this.object = object;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @NodeIntrinsic
    public static native void write(Object object, Object value);
}
