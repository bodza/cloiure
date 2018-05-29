package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.lir.StandardOp;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class SpillRegistersNode
public final class SpillRegistersNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<SpillRegistersNode> TYPE = NodeClass.create(SpillRegistersNode.class);

    protected Object unique;

    // @cons
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
