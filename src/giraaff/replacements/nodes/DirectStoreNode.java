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

/**
 * A special purpose store node that differs from {@link RawStoreNode} in that it is not a
 * {@link StateSplit} and takes a computed address instead of an object.
 */
// @class DirectStoreNode
public final class DirectStoreNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<DirectStoreNode> TYPE = NodeClass.create(DirectStoreNode.class);

    @Input protected ValueNode address;
    @Input protected ValueNode value;
    protected final JavaKind kind;

    // @cons
    public DirectStoreNode(ValueNode address, ValueNode value, JavaKind kind)
    {
        super(TYPE, StampFactory.forVoid());
        this.address = address;
        this.value = value;
        this.kind = kind;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value v = gen.operand(value);
        LIRKind lirKind = LIRKind.fromJavaKind(gen.getLIRGeneratorTool().target().arch, kind);
        gen.getLIRGeneratorTool().getArithmetic().emitStore(lirKind, gen.operand(address), v, null);
    }

    public ValueNode getAddress()
    {
        return address;
    }

    public ValueNode getValue()
    {
        return value;
    }

    @NodeIntrinsic
    public static native void storeBoolean(long address, boolean value, @ConstantNodeParameter JavaKind kind);
}
