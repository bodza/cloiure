package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractStateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Store of a value at a location specified as an absolute address.
 */
// @class UnsafeMemoryStoreNode
public final class UnsafeMemoryStoreNode extends AbstractStateSplit implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<UnsafeMemoryStoreNode> TYPE = NodeClass.create(UnsafeMemoryStoreNode.class);

    @Input
    // @field
    protected ValueNode value;
    @Input
    // @field
    protected ValueNode address;
    // @field
    protected final JavaKind kind;
    // @field
    protected final LocationIdentity locationIdentity;

    // @cons
    public UnsafeMemoryStoreNode(ValueNode __address, ValueNode __value, JavaKind __kind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forVoid());
        this.address = __address;
        this.value = __value;
        this.kind = __kind;
        this.locationIdentity = __locationIdentity;
    }

    public ValueNode getValue()
    {
        return value;
    }

    public ValueNode getAddress()
    {
        return address;
    }

    public JavaKind getKind()
    {
        return kind;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }
}
