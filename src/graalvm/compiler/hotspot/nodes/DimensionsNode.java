package graalvm.compiler.hotspot.nodes;

import java.util.BitSet;

import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.NumUtil;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;

/**
 * Intrinsic for allocating an on-stack array of integers to hold the dimensions of a multianewarray
 * instruction.
 */
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
        int slots = NumUtil.roundUp(size, wordSize) / wordSize;
        VirtualStackSlot array = lirGen.getResult().getFrameMapBuilder().allocateStackSlots(slots, new BitSet(0), null);
        Value result = lirGen.emitAddress(array);
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native Word allocaDimsArray(@ConstantNodeParameter int rank);
}
