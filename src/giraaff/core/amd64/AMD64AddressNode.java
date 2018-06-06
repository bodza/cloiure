package giraaff.core.amd64;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.core.common.LIRKind;
import giraaff.core.common.type.IntegerStamp;
import giraaff.graph.Node;
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

///
// Represents an address of the form [base + index*scale + displacement]. Both base and index are optional.
///
// @class AMD64AddressNode
public final class AMD64AddressNode extends AddressNode implements Simplifiable, LIRLowerable
{
    // @def
    public static final NodeClass<AMD64AddressNode> TYPE = NodeClass.create(AMD64AddressNode.class);

    @Node.OptionalInput
    // @field
    private ValueNode ___base;

    @Node.OptionalInput
    // @field
    private ValueNode ___index;
    // @field
    private AMD64Address.Scale ___scale;

    // @field
    private int ___displacement;

    // @cons AMD64AddressNode
    public AMD64AddressNode(ValueNode __base)
    {
        this(__base, null);
    }

    // @cons AMD64AddressNode
    public AMD64AddressNode(ValueNode __base, ValueNode __index)
    {
        super(TYPE);
        this.___base = __base;
        this.___index = __index;
        this.___scale = AMD64Address.Scale.Times1;
    }

    public void canonicalizeIndex(SimplifierTool __tool)
    {
        if (this.___index instanceof AddNode && ((IntegerStamp) this.___index.stamp(NodeView.DEFAULT)).getBits() == 64)
        {
            AddNode __add = (AddNode) this.___index;
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
                            this.___displacement = this.___displacement + this.___scale.___value * __addBy;
                            replaceFirstInput(this.___index, __phi);
                            __tool.addToWorkList(this.___index);
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

        AllocatableValue __baseValue = this.___base == null ? Value.ILLEGAL : __tool.asAllocatable(__gen.operand(this.___base));
        AllocatableValue __indexValue = this.___index == null ? Value.ILLEGAL : __tool.asAllocatable(__gen.operand(this.___index));

        AllocatableValue __baseReference = LIRKind.derivedBaseFromValue(__baseValue);
        AllocatableValue __indexReference;
        if (this.___index == null)
        {
            __indexReference = null;
        }
        else if (this.___scale.equals(AMD64Address.Scale.Times1))
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
        __gen.setResult(this, new AMD64AddressValue(__kind, __baseValue, __indexValue, this.___scale, this.___displacement));
    }

    @Override
    public ValueNode getBase()
    {
        return this.___base;
    }

    public void setBase(ValueNode __base)
    {
        // allow modification before inserting into the graph
        if (isAlive())
        {
            updateUsages(this.___base, __base);
        }
        this.___base = __base;
    }

    @Override
    public ValueNode getIndex()
    {
        return this.___index;
    }

    public void setIndex(ValueNode __index)
    {
        // allow modification before inserting into the graph
        if (isAlive())
        {
            updateUsages(this.___index, __index);
        }
        this.___index = __index;
    }

    public AMD64Address.Scale getScale()
    {
        return this.___scale;
    }

    public void setScale(AMD64Address.Scale __scale)
    {
        this.___scale = __scale;
    }

    public int getDisplacement()
    {
        return this.___displacement;
    }

    public void setDisplacement(int __displacement)
    {
        this.___displacement = __displacement;
    }

    @Override
    public long getMaxConstantDisplacement()
    {
        return this.___displacement;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        canonicalizeIndex(__tool);
    }
}
