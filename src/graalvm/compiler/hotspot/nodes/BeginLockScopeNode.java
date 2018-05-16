package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.extended.MonitorEnter;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

/**
 * Intrinsic for opening a scope binding a stack-based lock with an object. A lock scope must be
 * closed with an {@link EndLockScopeNode}. The frame state after this node denotes that the object
 * is locked (ensuring the GC sees and updates the object) so it must come after any null pointer
 * check on the object.
 */
@NodeInfo(allowedUsageTypes = Memory, cycles = CYCLES_2, size = SIZE_1)
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
        assert lockDepth != -1;
        HotSpotLIRGenerator hsGen = (HotSpotLIRGenerator) gen.getLIRGeneratorTool();
        VirtualStackSlot slot = hsGen.getLockSlot(lockDepth);
        Value result = gen.getLIRGeneratorTool().emitAddress(slot);
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native Word beginLockScope(@ConstantNodeParameter int lockDepth);
}
