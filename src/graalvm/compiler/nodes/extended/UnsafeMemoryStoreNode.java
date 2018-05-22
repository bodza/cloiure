package graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.AbstractStateSplit;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

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
