package graalvm.compiler.hotspot.nodes;

import java.util.BitSet;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;

import jdk.vm.ci.meta.Value;

/**
 * Node that is used to maintain a stack based counter of how many locks are currently held.
 */
public final class MonitorCounterNode extends FloatingNode implements LIRLowerable, Node.ValueNumberable
{
    public static final NodeClass<MonitorCounterNode> TYPE = NodeClass.create(MonitorCounterNode.class);

    public MonitorCounterNode(@InjectedNodeParameter WordTypes wordTypes)
    {
        super(TYPE, StampFactory.forKind(wordTypes.getWordKind()));
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        VirtualStackSlot counter = gen.getLIRGeneratorTool().getResult().getFrameMapBuilder().allocateStackSlots(1, new BitSet(0), null);
        Value result = gen.getLIRGeneratorTool().emitAddress(counter);
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native Word counter();
}
