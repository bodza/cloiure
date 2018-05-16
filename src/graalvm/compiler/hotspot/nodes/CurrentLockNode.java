package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;

import jdk.vm.ci.meta.Value;

/**
 * Intrinsic for getting the lock in the current {@linkplain BeginLockScopeNode lock scope}.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class CurrentLockNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<CurrentLockNode> TYPE = NodeClass.create(CurrentLockNode.class);

    protected int lockDepth;

    public CurrentLockNode(@InjectedNodeParameter WordTypes wordTypes, int lockDepth)
    {
        super(TYPE, StampFactory.forKind(wordTypes.getWordKind()));
        this.lockDepth = lockDepth;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        assert lockDepth != -1;
        HotSpotLIRGenerator hsGen = (HotSpotLIRGenerator) gen.getLIRGeneratorTool();
        VirtualStackSlot slot = hsGen.getLockSlot(lockDepth);
        // The register allocator cannot handle stack -> register moves so we use an LEA here
        Value result = gen.getLIRGeneratorTool().emitAddress(slot);
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native Word currentLock(@ConstantNodeParameter int lockDepth);
}
