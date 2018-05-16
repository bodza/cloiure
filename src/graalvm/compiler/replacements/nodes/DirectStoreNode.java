package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.RawStoreNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

/**
 * A special purpose store node that differs from {@link RawStoreNode} in that it is not a
 * {@link StateSplit} and takes a computed address instead of an object.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class DirectStoreNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<DirectStoreNode> TYPE = NodeClass.create(DirectStoreNode.class);
    @Input protected ValueNode address;
    @Input protected ValueNode value;
    protected final JavaKind kind;

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
