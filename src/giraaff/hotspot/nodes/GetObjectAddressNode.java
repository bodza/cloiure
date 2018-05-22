package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Intrinsification for getting the address of an object. The code path(s) between a call to
 * {@link #get(Object)} and all uses of the returned value must not contain safepoints. This can
 * only be guaranteed if used in a snippet that is instantiated after frame state assignment.
 * {@link ComputeObjectAddressNode} should generally be used in preference to this node.
 */
public final class GetObjectAddressNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<GetObjectAddressNode> TYPE = NodeClass.create(GetObjectAddressNode.class);

    @Input ValueNode object;

    public GetObjectAddressNode(ValueNode obj)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Long));
        this.object = obj;
    }

    @NodeIntrinsic
    public static native long get(Object array);

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        AllocatableValue obj = gen.getLIRGeneratorTool().newVariable(LIRKind.unknownReference(gen.getLIRGeneratorTool().target().arch.getWordKind()));
        gen.getLIRGeneratorTool().emitMove(obj, gen.operand(object));
        gen.setResult(this, obj);
    }
}
