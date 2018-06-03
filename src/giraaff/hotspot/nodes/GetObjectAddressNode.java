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
// @class GetObjectAddressNode
public final class GetObjectAddressNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<GetObjectAddressNode> TYPE = NodeClass.create(GetObjectAddressNode.class);

    @Input
    // @field
    ValueNode object;

    // @cons
    public GetObjectAddressNode(ValueNode __obj)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Long));
        this.object = __obj;
    }

    @NodeIntrinsic
    public static native long get(Object array);

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        AllocatableValue __obj = __gen.getLIRGeneratorTool().newVariable(LIRKind.unknownReference(__gen.getLIRGeneratorTool().target().arch.getWordKind()));
        __gen.getLIRGeneratorTool().emitMove(__obj, __gen.operand(object));
        __gen.setResult(this, __obj);
    }
}
