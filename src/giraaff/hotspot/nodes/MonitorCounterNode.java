package giraaff.hotspot.nodes;

import java.util.BitSet;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.lir.VirtualStackSlot;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

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
