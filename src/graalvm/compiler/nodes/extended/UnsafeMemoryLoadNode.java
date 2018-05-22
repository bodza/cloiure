package graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

/**
 * Load of a value at a location specified as an absolute address.
 */
public class UnsafeMemoryLoadNode extends FixedWithNextNode implements Lowerable
{
    public static final NodeClass<UnsafeMemoryLoadNode> TYPE = NodeClass.create(UnsafeMemoryLoadNode.class);

    @Input protected ValueNode address;
    protected final JavaKind kind;
    protected final LocationIdentity locationIdentity;

    public UnsafeMemoryLoadNode(ValueNode address, JavaKind kind, LocationIdentity locationIdentity)
    {
        super(TYPE, StampFactory.forKind(kind.getStackKind()));
        this.address = address;
        this.kind = kind;
        this.locationIdentity = locationIdentity;
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

    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }
}
