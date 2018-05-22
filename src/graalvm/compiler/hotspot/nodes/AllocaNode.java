package graalvm.compiler.hotspot.nodes;

import java.util.BitSet;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;

/**
 * Reserves a block of memory in the stack frame of a method. The block is reserved in the frame for
 * the entire execution of the associated method.
 */
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

    public AllocaNode(@InjectedNodeParameter WordTypes wordTypes, int slots)
    {
        this(slots, wordTypes.getWordKind(), new BitSet());
    }

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
