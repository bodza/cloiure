package graalvm.compiler.core.amd64;

import graalvm.compiler.asm.amd64.AMD64Address.Scale;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.lir.amd64.AMD64AddressValue;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.AddNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

/**
 * Represents an address of the form [base + index*scale + displacement]. Both base and index are
 * optional.
 */
@NodeInfo
public class AMD64AddressNode extends AddressNode implements Simplifiable, LIRLowerable
{
    public static final NodeClass<AMD64AddressNode> TYPE = NodeClass.create(AMD64AddressNode.class);

    @OptionalInput private ValueNode base;

    @OptionalInput private ValueNode index;
    private Scale scale;

    private int displacement;

    public AMD64AddressNode(ValueNode base)
    {
        this(base, null);
    }

    public AMD64AddressNode(ValueNode base, ValueNode index)
    {
        super(TYPE);
        this.base = base;
        this.index = index;
        this.scale = Scale.Times1;
    }

    public void canonicalizeIndex(SimplifierTool tool)
    {
        if (index instanceof AddNode && ((IntegerStamp) index.stamp(NodeView.DEFAULT)).getBits() == 64)
        {
            AddNode add = (AddNode) index;
            ValueNode valX = add.getX();
            if (valX instanceof PhiNode)
            {
                PhiNode phi = (PhiNode) valX;
                if (phi.merge() instanceof LoopBeginNode)
                {
                    LoopBeginNode loopNode = (LoopBeginNode) phi.merge();
                    if (!loopNode.isSimpleLoop())
                    {
                        ValueNode valY = add.getY();
                        if (valY instanceof ConstantNode)
                        {
                            int addBy = valY.asJavaConstant().asInt();
                            displacement = displacement + scale.value * addBy;
                            replaceFirstInput(index, phi);
                            tool.addToWorkList(index);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRGeneratorTool tool = gen.getLIRGeneratorTool();

        AllocatableValue baseValue = base == null ? Value.ILLEGAL : tool.asAllocatable(gen.operand(base));
        AllocatableValue indexValue = index == null ? Value.ILLEGAL : tool.asAllocatable(gen.operand(index));

        AllocatableValue baseReference = LIRKind.derivedBaseFromValue(baseValue);
        AllocatableValue indexReference;
        if (index == null)
        {
            indexReference = null;
        }
        else if (scale.equals(Scale.Times1))
        {
            indexReference = LIRKind.derivedBaseFromValue(indexValue);
        }
        else
        {
            if (LIRKind.isValue(indexValue))
            {
                indexReference = null;
            }
            else
            {
                indexReference = Value.ILLEGAL;
            }
        }

        LIRKind kind = LIRKind.combineDerived(tool.getLIRKind(stamp(NodeView.DEFAULT)), baseReference, indexReference);
        gen.setResult(this, new AMD64AddressValue(kind, baseValue, indexValue, scale, displacement));
    }

    @Override
    public ValueNode getBase()
    {
        return base;
    }

    public void setBase(ValueNode base)
    {
        // allow modification before inserting into the graph
        if (isAlive())
        {
            updateUsages(this.base, base);
        }
        this.base = base;
    }

    @Override
    public ValueNode getIndex()
    {
        return index;
    }

    public void setIndex(ValueNode index)
    {
        // allow modification before inserting into the graph
        if (isAlive())
        {
            updateUsages(this.index, index);
        }
        this.index = index;
    }

    public Scale getScale()
    {
        return scale;
    }

    public void setScale(Scale scale)
    {
        this.scale = scale;
    }

    public int getDisplacement()
    {
        return displacement;
    }

    public void setDisplacement(int displacement)
    {
        this.displacement = displacement;
    }

    @Override
    public long getMaxConstantDisplacement()
    {
        return displacement;
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        canonicalizeIndex(tool);
    }
}
