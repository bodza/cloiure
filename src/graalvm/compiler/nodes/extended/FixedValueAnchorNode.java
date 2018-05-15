package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.ValueProxy;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public class FixedValueAnchorNode extends FixedWithNextNode implements LIRLowerable, ValueProxy, GuardingNode {
    public static final NodeClass<FixedValueAnchorNode> TYPE = NodeClass.create(FixedValueAnchorNode.class);

    @Input ValueNode object;
    private Stamp predefinedStamp;

    public ValueNode object() {
        return object;
    }

    protected FixedValueAnchorNode(NodeClass<? extends FixedValueAnchorNode> c, ValueNode object) {
        super(c, object.stamp(NodeView.DEFAULT));
        this.object = object;
    }

    public FixedValueAnchorNode(ValueNode object) {
        this(TYPE, object);
    }

    public FixedValueAnchorNode(ValueNode object, Stamp predefinedStamp) {
        super(TYPE, predefinedStamp);
        this.object = object;
        this.predefinedStamp = predefinedStamp;
    }

    @Override
    public boolean inferStamp() {
        if (predefinedStamp == null) {
            return updateStamp(object.stamp(NodeView.DEFAULT));
        } else {
            return false;
        }
    }

    @NodeIntrinsic
    public static native Object getObject(Object object);

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        generator.setResult(this, generator.operand(object));
    }

    @Override
    public ValueNode getOriginalNode() {
        return object;
    }

    @Override
    public GuardingNode getGuard() {
        return this;
    }

}
