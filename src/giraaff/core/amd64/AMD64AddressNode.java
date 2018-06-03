package giraaff.core.amd64;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.core.common.LIRKind;
import giraaff.core.common.type.IntegerStamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.lir.amd64.AMD64AddressValue;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Represents an address of the form [base + index*scale + displacement]. Both base and index are optional.
 */
// @class AMD64AddressNode
public final class AMD64AddressNode extends AddressNode implements Simplifiable, LIRLowerable
{
    // @def
    public static final NodeClass<AMD64AddressNode> TYPE = NodeClass.create(AMD64AddressNode.class);

    @OptionalInput
    // @field
    private ValueNode base;

    @OptionalInput
    // @field
    private ValueNode index;
    // @field
    private Scale scale;

    // @field
    private int displacement;

    // @cons
    public AMD64AddressNode(ValueNode __base)
    {
        this(__base, null);
    }

    // @cons
    public AMD64AddressNode(ValueNode __base, ValueNode __index)
    {
        super(TYPE);
        this.base = __base;
        this.index = __index;
        this.scale = Scale.Times1;
    }

    public void canonicalizeIndex(SimplifierTool __tool)
    {
        if (index instanceof AddNode && ((IntegerStamp) index.stamp(NodeView.DEFAULT)).getBits() == 64)
        {
            AddNode __add = (AddNode) index;
            ValueNode __valX = __add.getX();
            if (__valX instanceof PhiNode)
            {
                PhiNode __phi = (PhiNode) __valX;
                if (__phi.merge() instanceof LoopBeginNode)
                {
                    LoopBeginNode __loopNode = (LoopBeginNode) __phi.merge();
                    if (!__loopNode.isSimpleLoop())
                    {
                        ValueNode __valY = __add.getY();
                        if (__valY instanceof ConstantNode)
                        {
                            int __addBy = __valY.asJavaConstant().asInt();
                            displacement = displacement + scale.value * __addBy;
                            replaceFirstInput(index, __phi);
                            __tool.addToWorkList(index);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRGeneratorTool __tool = __gen.getLIRGeneratorTool();

        AllocatableValue __baseValue = base == null ? Value.ILLEGAL : __tool.asAllocatable(__gen.operand(base));
        AllocatableValue __indexValue = index == null ? Value.ILLEGAL : __tool.asAllocatable(__gen.operand(index));

        AllocatableValue __baseReference = LIRKind.derivedBaseFromValue(__baseValue);
        AllocatableValue __indexReference;
        if (index == null)
        {
            __indexReference = null;
        }
        else if (scale.equals(Scale.Times1))
        {
            __indexReference = LIRKind.derivedBaseFromValue(__indexValue);
        }
        else
        {
            if (LIRKind.isValue(__indexValue))
            {
                __indexReference = null;
            }
            else
            {
                __indexReference = Value.ILLEGAL;
            }
        }

        LIRKind __kind = LIRKind.combineDerived(__tool.getLIRKind(stamp(NodeView.DEFAULT)), __baseReference, __indexReference);
        __gen.setResult(this, new AMD64AddressValue(__kind, __baseValue, __indexValue, scale, displacement));
    }

    @Override
    public ValueNode getBase()
    {
        return base;
    }

    public void setBase(ValueNode __base)
    {
        // allow modification before inserting into the graph
        if (isAlive())
        {
            updateUsages(this.base, __base);
        }
        this.base = __base;
    }

    @Override
    public ValueNode getIndex()
    {
        return index;
    }

    public void setIndex(ValueNode __index)
    {
        // allow modification before inserting into the graph
        if (isAlive())
        {
            updateUsages(this.index, __index);
        }
        this.index = __index;
    }

    public Scale getScale()
    {
        return scale;
    }

    public void setScale(Scale __scale)
    {
        this.scale = __scale;
    }

    public int getDisplacement()
    {
        return displacement;
    }

    public void setDisplacement(int __displacement)
    {
        this.displacement = __displacement;
    }

    @Override
    public long getMaxConstantDisplacement()
    {
        return displacement;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        canonicalizeIndex(__tool);
    }
}
