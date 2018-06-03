package giraaff.nodes.extended;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Creates a memory barrier.
///
// @NodeInfo.allowedUsageTypes "Memory"
// @class MembarNode
public final class MembarNode extends FixedWithNextNode implements LIRLowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<MembarNode> TYPE = NodeClass.create(MembarNode.class);

    // @field
    protected final int ___barriers;
    // @field
    protected final LocationIdentity ___location;

    // @cons
    public MembarNode(int __barriers)
    {
        this(__barriers, LocationIdentity.any());
    }

    // @cons
    public MembarNode(int __barriers, LocationIdentity __location)
    {
        super(TYPE, StampFactory.forVoid());
        this.___barriers = __barriers;
        this.___location = __location;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___location;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitMembar(this.___barriers);
    }

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int __barriers);

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int __barriers, @ConstantNodeParameter LocationIdentity __location);
}
