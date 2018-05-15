package graalvm.compiler.nodes.debug;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class OpaqueNode extends FloatingNode implements LIRLowerable {

    public static final NodeClass<OpaqueNode> TYPE = NodeClass.create(OpaqueNode.class);
    @Input protected ValueNode value;

    public OpaqueNode(ValueNode value) {
        super(TYPE, value.stamp(NodeView.DEFAULT).unrestricted());
        this.value = value;
    }

    public ValueNode getValue() {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        gen.setResult(this, gen.operand(value));
    }
}
