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

/**
 * Intrinsic for opening a scope binding a stack-based lock with an object. A lock scope must be
 * closed with an {@link EndLockScopeNode}. The frame state after this node denotes that the object
 * is locked (ensuring the GC sees and updates the object) so it must come after any null pointer
 * check on the object.
 */
// @NodeInfo.allowedUsageTypes "Memory"
public final class BeginLockScopeNode extends AbstractMemoryCheckpoint implements LIRLowerable, MonitorEnter, MemoryCheckpoint.Single
{
    public static final NodeClass<BeginLockScopeNode> TYPE = NodeClass.create(BeginLockScopeNode.class);
    protected int lockDepth;

    public BeginLockScopeNode(@InjectedNodeParameter WordTypes wordTypes, int lockDepth)
    {
        super(TYPE, StampFactory.forKind(wordTypes.getWordKind()));
        this.lockDepth = lockDepth;
    }

    public BeginLockScopeNode(JavaKind kind, int lockDepth)
    {
        super(TYPE, StampFactory.forKind(kind));
        this.lockDepth = lockDepth;
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
    public void generate(NodeLIRBuilderTool gen)
    {
        HotSpotLIRGenerator hsGen = (HotSpotLIRGenerator) gen.getLIRGeneratorTool();
        VirtualStackSlot slot = hsGen.getLockSlot(lockDepth);
        Value result = gen.getLIRGeneratorTool().emitAddress(slot);
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native Word beginLockScope(@ConstantNodeParameter int lockDepth);
}
