package graalvm.compiler.hotspot.nodes.aot;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_IGNORED;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_IGNORED;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.hotspot.replacements.EncodedSymbolConstant;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.word.Word;

import jdk.vm.ci.meta.Constant;

@NodeInfo(cycles = CYCLES_IGNORED, size = SIZE_IGNORED)
public final class EncodedSymbolNode extends FloatingNode implements Canonicalizable {

    public static final NodeClass<EncodedSymbolNode> TYPE = NodeClass.create(EncodedSymbolNode.class);

    @OptionalInput protected ValueNode value;

    public EncodedSymbolNode(@InjectedNodeParameter Stamp stamp, ValueNode value) {
        super(TYPE, stamp);
        assert value != null;
        this.value = value;
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (value != null) {
            Constant constant = GraphUtil.foldIfConstantAndRemove(this, value);
            if (constant != null) {
                return new ConstantNode(new EncodedSymbolConstant(constant), StampFactory.pointer());
            }
        }
        return this;
    }

    @NodeIntrinsic
    public static native Word encode(Object constant);
}
