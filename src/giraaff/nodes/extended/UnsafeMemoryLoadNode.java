package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// Load of a value at a location specified as an absolute address.
///
// @class UnsafeMemoryLoadNode
public final class UnsafeMemoryLoadNode extends FixedWithNextNode implements Lowerable
{
    // @def
    public static final NodeClass<UnsafeMemoryLoadNode> TYPE = NodeClass.create(UnsafeMemoryLoadNode.class);

    @Node.Input
    // @field
    protected ValueNode ___address;
    // @field
    protected final JavaKind ___kind;
    // @field
    protected final LocationIdentity ___locationIdentity;

    // @cons UnsafeMemoryLoadNode
    public UnsafeMemoryLoadNode(ValueNode __address, JavaKind __kind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forKind(__kind.getStackKind()));
        this.___address = __address;
        this.___kind = __kind;
        this.___locationIdentity = __locationIdentity;
    }

    public ValueNode getAddress()
    {
        return this.___address;
    }

    public JavaKind getKind()
    {
        return this.___kind;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }
}
