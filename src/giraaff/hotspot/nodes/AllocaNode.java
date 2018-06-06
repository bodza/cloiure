package giraaff.hotspot.nodes;

import java.util.BitSet;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.lir.VirtualStackSlot;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

///
// Reserves a block of memory in the stack frame of a method. The block is reserved in the frame for
// the entire execution of the associated method.
///
// @class AllocaNode
public final class AllocaNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<AllocaNode> TYPE = NodeClass.create(AllocaNode.class);

    ///
    // The number of slots in block.
    ///
    // @field
    protected final int ___slots;

    ///
    // The indexes of the object pointer slots in the block. Each such object pointer slot must be
    // initialized before any safepoint in the method otherwise the garbage collector will see
    // garbage values when processing these slots.
    ///
    // @field
    protected final BitSet ___objects;

    // @cons AllocaNode
    public AllocaNode(@Node.InjectedNodeParameter WordTypes __wordTypes, int __slots)
    {
        this(__slots, __wordTypes.getWordKind(), new BitSet());
    }

    // @cons AllocaNode
    public AllocaNode(int __slots, JavaKind __wordKind, BitSet __objects)
    {
        super(TYPE, StampFactory.forKind(__wordKind));
        this.___slots = __slots;
        this.___objects = __objects;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        VirtualStackSlot __array = __gen.getLIRGeneratorTool().getResult().getFrameMapBuilder().allocateStackSlots(this.___slots, this.___objects, null);
        Value __result = __gen.getLIRGeneratorTool().emitAddress(__array);
        __gen.setResult(this, __result);
    }

    @Node.NodeIntrinsic
    public static native Word alloca(@Node.ConstantNodeParameter int __slots);
}
