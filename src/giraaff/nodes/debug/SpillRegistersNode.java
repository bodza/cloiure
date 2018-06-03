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
    // @def
    public static final NodeClass<SpillRegistersNode> TYPE = NodeClass.create(SpillRegistersNode.class);

    // @field
    protected Object ___unique;

    // @cons
    public SpillRegistersNode()
    {
        super(TYPE, StampFactory.forVoid());
        // prevent control-flow optimization
        this.___unique = new Object();
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().append(new StandardOp.SpillRegistersOp());
    }
}
