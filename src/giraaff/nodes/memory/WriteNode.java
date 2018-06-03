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

///
// Writes a given {@linkplain #value() value} a {@linkplain FixedAccessNode memory location}.
///
// @class WriteNode
public final class WriteNode extends AbstractWriteNode implements LIRLowerableAccess, Canonicalizable
{
    // @def
    public static final NodeClass<WriteNode> TYPE = NodeClass.create(WriteNode.class);

    // @cons
    public WriteNode(AddressNode __address, LocationIdentity __location, ValueNode __value, BarrierType __barrierType)
    {
        super(TYPE, __address, __location, __value, __barrierType);
    }

    // @cons
    protected WriteNode(NodeClass<? extends WriteNode> __c, AddressNode __address, LocationIdentity __location, ValueNode __value, BarrierType __barrierType)
    {
        super(__c, __address, __location, __value, __barrierType);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRKind __writeKind = __gen.getLIRGeneratorTool().getLIRKind(value().stamp(NodeView.DEFAULT));
        __gen.getLIRGeneratorTool().getArithmetic().emitStore(__writeKind, __gen.operand(this.___address), __gen.operand(value()), __gen.state(this));
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
    public Node canonical(CanonicalizerTool __tool)
    {
        if (__tool.canonicalizeReads() && hasExactlyOneUsage() && next() instanceof WriteNode)
        {
            WriteNode __write = (WriteNode) next();
            if (__write.___lastLocationAccess == this && __write.getAddress() == getAddress() && getAccessStamp().isCompatible(__write.getAccessStamp()))
            {
                __write.setLastLocationAccess(getLastLocationAccess());
                return __write;
            }
        }
        return this;
    }
}
