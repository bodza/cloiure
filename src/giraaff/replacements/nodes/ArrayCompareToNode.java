package giraaff.replacements.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueNodeUtil;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.util.GraphUtil;

///
// Compares two arrays lexicographically.
///
// @class ArrayCompareToNode
public final class ArrayCompareToNode extends FixedWithNextNode implements LIRLowerable, Canonicalizable, Virtualizable, MemoryAccess
{
    // @def
    public static final NodeClass<ArrayCompareToNode> TYPE = NodeClass.create(ArrayCompareToNode.class);

    /// {@link JavaKind} of one array to compare.
    // @field
    protected final JavaKind ___kind1;

    /// {@link JavaKind} of the other array to compare.
    // @field
    protected final JavaKind ___kind2;

    /// One array to be tested for equality.
    @Node.Input
    // @field
    ValueNode ___array1;

    /// The other array to be tested for equality.
    @Node.Input
    // @field
    ValueNode ___array2;

    /// Length of one array.
    @Node.Input
    // @field
    ValueNode ___length1;

    /// Length of the other array.
    @Node.Input
    // @field
    ValueNode ___length2;

    @Node.OptionalInput(InputType.Memory)
    // @field
    MemoryNode ___lastLocationAccess;

    // @cons ArrayCompareToNode
    public ArrayCompareToNode(ValueNode __array1, ValueNode __array2, ValueNode __length1, ValueNode __length2, @Node.ConstantNodeParameter JavaKind __kind1, @Node.ConstantNodeParameter JavaKind __kind2)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.___kind1 = __kind1;
        this.___kind2 = __kind2;
        this.___array1 = __array1;
        this.___array2 = __array2;
        this.___length1 = __length1;
        this.___length2 = __length2;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        ValueNode __a1 = GraphUtil.unproxify(this.___array1);
        ValueNode __a2 = GraphUtil.unproxify(this.___array2);
        if (__a1 == __a2)
        {
            return ConstantNode.forInt(0);
        }
        return this;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias1 = __tool.getAlias(this.___array1);
        ValueNode __alias2 = __tool.getAlias(this.___array2);
        if (__alias1 == __alias2)
        {
            // the same virtual objects will always have the same contents
            __tool.replaceWithValue(ConstantNode.forInt(0, graph()));
        }
    }

    @Node.NodeIntrinsic
    public static native int compareTo(Object __array1, Object __array2, int __length1, int __length2, @Node.ConstantNodeParameter JavaKind __kind1, @Node.ConstantNodeParameter JavaKind __kind2);

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __result = __gen.getLIRGeneratorTool().emitArrayCompareTo(this.___kind1, this.___kind2, __gen.operand(this.___array1), __gen.operand(this.___array2), __gen.operand(this.___length1), __gen.operand(this.___length2));
        __gen.setResult(this, __result);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return NamedLocationIdentity.getArrayLocation(this.___kind1);
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return this.___lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __lla)
    {
        updateUsages(ValueNodeUtil.asNode(this.___lastLocationAccess), ValueNodeUtil.asNode(__lla));
        this.___lastLocationAccess = __lla;
    }
}
