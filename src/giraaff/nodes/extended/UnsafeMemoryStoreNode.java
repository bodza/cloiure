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

///
// Store of a value at a location specified as an absolute address.
///
// @class UnsafeMemoryStoreNode
public final class UnsafeMemoryStoreNode extends AbstractStateSplit implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<UnsafeMemoryStoreNode> TYPE = NodeClass.create(UnsafeMemoryStoreNode.class);

    @Input
    // @field
    protected ValueNode ___value;
    @Input
    // @field
    protected ValueNode ___address;
    // @field
    protected final JavaKind ___kind;
    // @field
    protected final LocationIdentity ___locationIdentity;

    // @cons
    public UnsafeMemoryStoreNode(ValueNode __address, ValueNode __value, JavaKind __kind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forVoid());
        this.___address = __address;
        this.___value = __value;
        this.___kind = __kind;
        this.___locationIdentity = __locationIdentity;
    }

    public ValueNode getValue()
    {
        return this.___value;
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

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }
}
