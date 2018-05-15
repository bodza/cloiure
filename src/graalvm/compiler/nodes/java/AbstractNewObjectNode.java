package graalvm.compiler.nodes.java;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_8;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

/**
 * The {@code AbstractNewObjectNode} is the base class for the new instance and new array nodes.
 */
@NodeInfo(cycles = CYCLES_8, cyclesRationale = "tlab alloc + header init", size = SIZE_8)
public abstract class AbstractNewObjectNode extends DeoptimizingFixedWithNextNode implements Lowerable {

    public static final NodeClass<AbstractNewObjectNode> TYPE = NodeClass.create(AbstractNewObjectNode.class);
    protected final boolean fillContents;

    protected AbstractNewObjectNode(NodeClass<? extends AbstractNewObjectNode> c, Stamp stamp, boolean fillContents, FrameState stateBefore) {
        super(c, stamp, stateBefore);
        this.fillContents = fillContents;
    }

    /**
     * @return <code>true</code> if the object's contents should be initialized to zero/null.
     */
    public boolean fillContents() {
        return fillContents;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public boolean canDeoptimize() {
        return true;
    }
}
