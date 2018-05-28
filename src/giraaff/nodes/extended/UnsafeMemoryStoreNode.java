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
public class UnsafeMemoryStoreNode extends AbstractStateSplit implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<UnsafeMemoryStoreNode> TYPE = NodeClass.create(UnsafeMemoryStoreNode.class);

    @Input protected ValueNode value;
    @Input protected ValueNode address;
    protected final JavaKind kind;
    protected final LocationIdentity locationIdentity;

    public UnsafeMemoryStoreNode(ValueNode address, ValueNode value, JavaKind kind, LocationIdentity locationIdentity)
    {
        super(TYPE, StampFactory.forVoid());
        this.address = address;
        this.value = value;
        this.kind = kind;
        this.locationIdentity = locationIdentity;
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
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }
}
