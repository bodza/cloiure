package giraaff.hotspot.nodes;

import java.util.BitSet;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.lir.VirtualStackSlot;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

/**
 * Reserves a block of memory in the stack frame of a method. The block is reserved in the frame for
 * the entire execution of the associated method.
 */
// @class AllocaNode
public final class AllocaNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<AllocaNode> TYPE = NodeClass.create(AllocaNode.class);

    /**
     * The number of slots in block.
     */
    protected final int slots;

    /**
     * The indexes of the object pointer slots in the block. Each such object pointer slot must be
     * initialized before any safepoint in the method otherwise the garbage collector will see
     * garbage values when processing these slots.
     */
    protected final BitSet objects;

    // @cons
    public AllocaNode(@InjectedNodeParameter WordTypes wordTypes, int slots)
    {
        this(slots, wordTypes.getWordKind(), new BitSet());
    }

    // @cons
    public AllocaNode(int slots, JavaKind wordKind, BitSet objects)
    {
        super(TYPE, StampFactory.forKind(wordKind));
        this.slots = slots;
        this.objects = objects;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        VirtualStackSlot array = gen.getLIRGeneratorTool().getResult().getFrameMapBuilder().allocateStackSlots(slots, objects, null);
        Value result = gen.getLIRGeneratorTool().emitAddress(array);
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native Word alloca(@ConstantNodeParameter int slots);
}
