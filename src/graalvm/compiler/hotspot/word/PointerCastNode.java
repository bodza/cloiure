package graalvm.compiler.hotspot.word;

import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.word.HotSpotOperation.HotspotOpcode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

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
    public void generate(NodeLIRBuilderTool generator)
    {
        Value value = generator.operand(input);

        generator.setResult(this, value);
    }
}
