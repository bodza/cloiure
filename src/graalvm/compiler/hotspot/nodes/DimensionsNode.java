package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.core.common.NumUtil.roundUp;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import java.util.BitSet;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;

import jdk.vm.ci.meta.Value;

/**
 * Intrinsic for allocating an on-stack array of integers to hold the dimensions of a multianewarray
 * instruction.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class DimensionsNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<DimensionsNode> TYPE = NodeClass.create(DimensionsNode.class);
    protected final int rank;

    public DimensionsNode(@InjectedNodeParameter WordTypes wordTypes, int rank)
    {
        super(TYPE, StampFactory.forKind(wordTypes.getWordKind()));
        this.rank = rank;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRGeneratorTool lirGen = gen.getLIRGeneratorTool();
        int size = rank * 4;
        int wordSize = lirGen.target().wordSize;
        int slots = roundUp(size, wordSize) / wordSize;
        VirtualStackSlot array = lirGen.getResult().getFrameMapBuilder().allocateStackSlots(slots, new BitSet(0), null);
        Value result = lirGen.emitAddress(array);
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native Word allocaDimsArray(@ConstantNodeParameter int rank);
}
