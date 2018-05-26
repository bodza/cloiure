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
public final class PointerCastNode extends FloatingNode implements LIRLowerable, Node.ValueNumberable
{
    public static final NodeClass<PointerCastNode> TYPE = NodeClass.create(PointerCastNode.class);
    @Input ValueNode input;

    public PointerCastNode(Stamp stamp, ValueNode input)
    {
        super(TYPE, stamp);
        this.input = input;
    }

    public ValueNode getInput()
    {
        return input;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.setResult(this, gen.operand(input));
    }
}
