package giraaff.nodes.memory.address;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
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
// @class OffsetAddressNode
public final class OffsetAddressNode extends AddressNode implements Canonicalizable
{
    // @def
    public static final NodeClass<OffsetAddressNode> TYPE = NodeClass.create(OffsetAddressNode.class);

    @Input
    // @field
    ValueNode base;
    @Input
    // @field
    ValueNode offset;

    // @cons
    public OffsetAddressNode(ValueNode __base, ValueNode __offset)
    {
        super(TYPE);
        this.base = __base;
        this.offset = __offset;
    }

    public static OffsetAddressNode create(ValueNode __base)
    {
        return new OffsetAddressNode(__base, ConstantNode.forIntegerBits(PrimitiveStamp.getBits(__base.stamp(NodeView.DEFAULT)), 0));
    }

    @Override
    public ValueNode getBase()
    {
        return base;
    }

    public void setBase(ValueNode __base)
    {
        updateUsages(this.base, __base);
        this.base = __base;
    }

    public ValueNode getOffset()
    {
        return offset;
    }

    public void setOffset(ValueNode __offset)
    {
        updateUsages(this.offset, __offset);
        this.offset = __offset;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (base instanceof OffsetAddressNode)
        {
            NodeView __view = NodeView.from(__tool);
            // Rewrite (&base[offset1])[offset2] to base[offset1 + offset2].
            OffsetAddressNode __b = (OffsetAddressNode) base;
            return new OffsetAddressNode(__b.getBase(), BinaryArithmeticNode.add(__b.getOffset(), this.getOffset(), __view));
        }
        else if (base instanceof AddNode)
        {
            AddNode __add = (AddNode) base;
            if (__add.getY().isConstant())
            {
                return new OffsetAddressNode(__add.getX(), new AddNode(__add.getY(), getOffset()));
            }
        }
        return this;
    }

    @NodeIntrinsic
    public static native Address address(Object base, long offset);

    @Override
    public long getMaxConstantDisplacement()
    {
        Stamp __curStamp = offset.stamp(NodeView.DEFAULT);
        if (__curStamp instanceof IntegerStamp)
        {
            IntegerStamp __integerStamp = (IntegerStamp) __curStamp;
            if (__integerStamp.lowerBound() >= 0)
            {
                return __integerStamp.upperBound();
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
