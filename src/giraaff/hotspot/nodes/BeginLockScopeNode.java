package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.lir.VirtualStackSlot;
import giraaff.nodes.extended.MonitorEnter;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;
import giraaff.word.WordTypes;

///
// Intrinsic for opening a scope binding a stack-based lock with an object. A lock scope must be
// closed with an {@link EndLockScopeNode}. The frame state after this node denotes that the object
// is locked (ensuring the GC sees and updates the object) so it must come after any null pointer
// check on the object.
///
// @NodeInfo.allowedUsageTypes "Memory"
// @class BeginLockScopeNode
public final class BeginLockScopeNode extends AbstractMemoryCheckpoint implements LIRLowerable, MonitorEnter, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<BeginLockScopeNode> TYPE = NodeClass.create(BeginLockScopeNode.class);

    // @field
    protected int ___lockDepth;

    // @cons
    public BeginLockScopeNode(@InjectedNodeParameter WordTypes __wordTypes, int __lockDepth)
    {
        super(TYPE, StampFactory.forKind(__wordTypes.getWordKind()));
        this.___lockDepth = __lockDepth;
    }

    // @cons
    public BeginLockScopeNode(JavaKind __kind, int __lockDepth)
    {
        super(TYPE, StampFactory.forKind(__kind));
        this.___lockDepth = __lockDepth;
    }

    @Override
    public boolean hasSideEffect()
    {
        return false;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        HotSpotLIRGenerator __hsGen = (HotSpotLIRGenerator) __gen.getLIRGeneratorTool();
        VirtualStackSlot __slot = __hsGen.getLockSlot(this.___lockDepth);
        Value __result = __gen.getLIRGeneratorTool().emitAddress(__slot);
        __gen.setResult(this, __result);
    }

    @NodeIntrinsic
    public static native Word beginLockScope(@ConstantNodeParameter int __lockDepth);
}
