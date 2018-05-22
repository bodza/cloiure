package graalvm.compiler.nodes.memory;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

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
