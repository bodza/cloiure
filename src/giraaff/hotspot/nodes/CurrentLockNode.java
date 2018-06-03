package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.lir.VirtualStackSlot;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

/**
 * Intrinsic for getting the lock in the current {@linkplain BeginLockScopeNode lock scope}.
 */
// @class CurrentLockNode
public final class CurrentLockNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<CurrentLockNode> TYPE = NodeClass.create(CurrentLockNode.class);

    // @field
    protected int lockDepth;

    // @cons
    public CurrentLockNode(@InjectedNodeParameter WordTypes __wordTypes, int __lockDepth)
    {
        super(TYPE, StampFactory.forKind(__wordTypes.getWordKind()));
        this.lockDepth = __lockDepth;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        HotSpotLIRGenerator __hsGen = (HotSpotLIRGenerator) __gen.getLIRGeneratorTool();
        VirtualStackSlot __slot = __hsGen.getLockSlot(lockDepth);
        // the register allocator cannot handle stack -> register moves, so we use an LEA here
        Value __result = __gen.getLIRGeneratorTool().emitAddress(__slot);
        __gen.setResult(this, __result);
    }

    @NodeIntrinsic
    public static native Word currentLock(@ConstantNodeParameter int lockDepth);
}
