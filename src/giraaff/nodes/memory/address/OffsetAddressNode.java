package giraaff.nodes.memory.address;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.BinaryArithmeticNode;

/**
 * Represents an address that is composed of a base and an offset. The base can be either a
 * {@link JavaKind#Object}, a word-sized integer or another pointer. The offset must be a word-sized integer.
 */
// @NodeInfo.allowedUsageTypes "Association"
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
