package graalvm.compiler.truffle.compiler.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.extended.BoxNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.JavaKind;

@NodeInfo(cycles = CYCLES_0, size = SIZE_1)
public final class IsCompilationConstantNode extends FloatingNode implements Lowerable, Canonicalizable {

    public static final NodeClass<IsCompilationConstantNode> TYPE = NodeClass.create(IsCompilationConstantNode.class);

    @Input ValueNode value;

    public IsCompilationConstantNode(ValueNode value) {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean));
        this.value = value;
    }

    @Override
    public void lower(LoweringTool tool) {
        replaceAtUsagesAndDelete(ConstantNode.forBoolean(false, graph()));
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        ValueNode arg0 = value;
        if (arg0 instanceof BoxNode) {
            arg0 = ((BoxNode) arg0).getValue();
        }
        if (arg0.isConstant()) {
            return ConstantNode.forBoolean(true);
        }
        return this;
    }

    @NodeIntrinsic
    public static native boolean check(Object value);
}
