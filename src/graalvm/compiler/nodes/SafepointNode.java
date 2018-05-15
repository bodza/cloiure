package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Marks a position in the graph where a safepoint should be emitted.
 */
// @formatter:off
@NodeInfo(cycles = CYCLES_2,
          cyclesRationale = "read",
          size = SIZE_1)
// @formatter:on
public final class SafepointNode extends DeoptimizingFixedWithNextNode implements Lowerable, LIRLowerable {

    public static final NodeClass<SafepointNode> TYPE = NodeClass.create(SafepointNode.class);

    public SafepointNode() {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        gen.visitSafepointNode(this);
    }

    @Override
    public boolean canDeoptimize() {
        return true;
    }
}
