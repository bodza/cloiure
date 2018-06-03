package giraaff.hotspot.word;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.hotspot.word.HotSpotOperation.HotspotOpcode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Cast between Word and metaspace pointers exposed by the {@link HotspotOpcode#FROM_POINTER} and
 * {@link HotspotOpcode#TO_KLASS_POINTER} operations.
 */
// @class PointerCastNode
public final class PointerCastNode extends FloatingNode implements LIRLowerable, Node.ValueNumberable
{
    // @def
    public static final NodeClass<PointerCastNode> TYPE = NodeClass.create(PointerCastNode.class);

    @Input
    // @field
    ValueNode input;

    // @cons
    public PointerCastNode(Stamp __stamp, ValueNode __input)
    {
        super(TYPE, __stamp);
        this.input = __input;
    }

    public ValueNode getInput()
    {
        return input;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, __gen.operand(input));
    }
}
