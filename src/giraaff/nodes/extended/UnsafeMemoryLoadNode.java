package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Load of a value at a location specified as an absolute address.
 */
// @class UnsafeMemoryLoadNode
public final class UnsafeMemoryLoadNode extends FixedWithNextNode implements Lowerable
{
    // @def
    public static final NodeClass<UnsafeMemoryLoadNode> TYPE = NodeClass.create(UnsafeMemoryLoadNode.class);

    @Input
    // @field
    protected ValueNode address;
    // @field
    protected final JavaKind kind;
    // @field
    protected final LocationIdentity locationIdentity;

    // @cons
    public UnsafeMemoryLoadNode(ValueNode __address, JavaKind __kind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forKind(__kind.getStackKind()));
        this.address = __address;
        this.kind = __kind;
        this.locationIdentity = __locationIdentity;
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

    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }
}
