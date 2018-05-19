package graalvm.compiler.nodes.memory.address;

import graalvm.compiler.core.common.type.AbstractPointerStamp;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.PrimitiveStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.AddNode;
import graalvm.compiler.nodes.calc.BinaryArithmeticNode;

import jdk.vm.ci.meta.JavaKind;

/**
 * Represents an address that is composed of a base and an offset. The base can be either a
 * {@link JavaKind#Object}, a word-sized integer or another pointer. The offset must be a word-sized
 * integer.
 */
@NodeInfo(allowedUsageTypes = InputType.Association)
public class OffsetAddressNode extends AddressNode implements Canonicalizable
{
    public static final NodeClass<OffsetAddressNode> TYPE = NodeClass.create(OffsetAddressNode.class);

    @Input ValueNode base;
    @Input ValueNode offset;

    public OffsetAddressNode(ValueNode base, ValueNode offset)
    {
        super(TYPE);
        this.base = base;
        this.offset = offset;
    }

    public static OffsetAddressNode create(ValueNode base)
    {
        return new OffsetAddressNode(base, ConstantNode.forIntegerBits(PrimitiveStamp.getBits(base.stamp(NodeView.DEFAULT)), 0));
    }

    @Override
    public ValueNode getBase()
    {
        return base;
    }

    public void setBase(ValueNode base)
    {
        updateUsages(this.base, base);
        this.base = base;
    }

    public ValueNode getOffset()
    {
        return offset;
    }

    public void setOffset(ValueNode offset)
    {
        updateUsages(this.offset, offset);
        this.offset = offset;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (base instanceof OffsetAddressNode)
        {
            NodeView view = NodeView.from(tool);
            // Rewrite (&base[offset1])[offset2] to base[offset1 + offset2].
            OffsetAddressNode b = (OffsetAddressNode) base;
            return new OffsetAddressNode(b.getBase(), BinaryArithmeticNode.add(b.getOffset(), this.getOffset(), view));
        }
        else if (base instanceof AddNode)
        {
            AddNode add = (AddNode) base;
            if (add.getY().isConstant())
            {
                return new OffsetAddressNode(add.getX(), new AddNode(add.getY(), getOffset()));
            }
        }
        return this;
    }

    @NodeIntrinsic
    public static native Address address(Object base, long offset);

    @Override
    public long getMaxConstantDisplacement()
    {
        Stamp curStamp = offset.stamp(NodeView.DEFAULT);
        if (curStamp instanceof IntegerStamp)
        {
            IntegerStamp integerStamp = (IntegerStamp) curStamp;
            if (integerStamp.lowerBound() >= 0)
            {
                return integerStamp.upperBound();
            }
        }
        return Long.MAX_VALUE;
    }

    @Override
    public ValueNode getIndex()
    {
        return null;
    }
}
