package giraaff.replacements.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.RawStoreNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// A special purpose store node that differs from {@link RawStoreNode} in that it is not a
// {@link StateSplit} and takes a computed address instead of an object.
///
// @class DirectStoreNode
public final class DirectStoreNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<DirectStoreNode> TYPE = NodeClass.create(DirectStoreNode.class);

    @Input
    // @field
    protected ValueNode ___address;
    @Input
    // @field
    protected ValueNode ___value;
    // @field
    protected final JavaKind ___kind;

    // @cons
    public DirectStoreNode(ValueNode __address, ValueNode __value, JavaKind __kind)
    {
        super(TYPE, StampFactory.forVoid());
        this.___address = __address;
        this.___value = __value;
        this.___kind = __kind;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __v = __gen.operand(this.___value);
        LIRKind __lirKind = LIRKind.fromJavaKind(__gen.getLIRGeneratorTool().target().arch, this.___kind);
        __gen.getLIRGeneratorTool().getArithmetic().emitStore(__lirKind, __gen.operand(this.___address), __v, null);
    }

    public ValueNode getAddress()
    {
        return this.___address;
    }

    public ValueNode getValue()
    {
        return this.___value;
    }

    @NodeIntrinsic
    public static native void storeBoolean(long __address, boolean __value, @ConstantNodeParameter JavaKind __kind);
}
