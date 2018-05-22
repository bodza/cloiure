package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Writes a given {@linkplain #value() value} a {@linkplain FixedAccessNode memory location}.
 */
public class WriteNode extends AbstractWriteNode implements LIRLowerableAccess, Canonicalizable
{
    public static final NodeClass<WriteNode> TYPE = NodeClass.create(WriteNode.class);

    public WriteNode(AddressNode address, LocationIdentity location, ValueNode value, BarrierType barrierType)
    {
        super(TYPE, address, location, value, barrierType);
    }

    protected WriteNode(NodeClass<? extends WriteNode> c, AddressNode address, LocationIdentity location, ValueNode value, BarrierType barrierType)
    {
        super(c, address, location, value, barrierType);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRKind writeKind = gen.getLIRGeneratorTool().getLIRKind(value().stamp(NodeView.DEFAULT));
        gen.getLIRGeneratorTool().getArithmetic().emitStore(writeKind, gen.operand(address), gen.operand(value()), gen.state(this));
    }

    @Override
    public boolean canNullCheck()
    {
        return true;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return value().stamp(NodeView.DEFAULT);
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (tool.canonicalizeReads() && hasExactlyOneUsage() && next() instanceof WriteNode)
        {
            WriteNode write = (WriteNode) next();
            if (write.lastLocationAccess == this && write.getAddress() == getAddress() && getAccessStamp().isCompatible(write.getAccessStamp()))
            {
                write.setLastLocationAccess(getLastLocationAccess());
                return write;
            }
        }
        return this;
    }
}
