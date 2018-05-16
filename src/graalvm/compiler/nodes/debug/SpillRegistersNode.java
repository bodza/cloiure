package graalvm.compiler.nodes.debug;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.StandardOp;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

@NodeInfo(cycles = CYCLES_2, size = SIZE_0)
public final class SpillRegistersNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<SpillRegistersNode> TYPE = NodeClass.create(SpillRegistersNode.class);

    protected Object unique;

    public SpillRegistersNode()
    {
        super(TYPE, StampFactory.forVoid());
        // prevent control-flow optimization
        this.unique = new Object();
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().append(new StandardOp.SpillRegistersOp());
    }
}
