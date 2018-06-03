package giraaff.hotspot.nodes;

import java.util.BitSet;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.NumUtil;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

/**
 * Intrinsic for allocating an on-stack array of integers to hold the dimensions of a multianewarray instruction.
 */
// @class DimensionsNode
public final class DimensionsNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<DimensionsNode> TYPE = NodeClass.create(DimensionsNode.class);

    // @field
    protected final int rank;

    // @cons
    public DimensionsNode(@InjectedNodeParameter WordTypes __wordTypes, int __rank)
    {
        super(TYPE, StampFactory.forKind(__wordTypes.getWordKind()));
        this.rank = __rank;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRGeneratorTool __lirGen = __gen.getLIRGeneratorTool();
        int __size = rank * 4;
        int __wordSize = __lirGen.target().wordSize;
        int __slots = NumUtil.roundUp(__size, __wordSize) / __wordSize;
        VirtualStackSlot __array = __lirGen.getResult().getFrameMapBuilder().allocateStackSlots(__slots, new BitSet(0), null);
        Value __result = __lirGen.emitAddress(__array);
        __gen.setResult(this, __result);
    }

    @NodeIntrinsic
    public static native Word allocaDimsArray(@ConstantNodeParameter int rank);
}
