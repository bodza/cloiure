package giraaff.nodes.java;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// Represents an atomic compare-and-swap operation The result is a boolean that contains whether the
// value matched the expected value.
///
// @NodeInfo.allowedUsageTypes "Value, Memory"
// @class UnsafeCompareAndSwapNode
public final class UnsafeCompareAndSwapNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<UnsafeCompareAndSwapNode> TYPE = NodeClass.create(UnsafeCompareAndSwapNode.class);

    @Input
    // @field
    ValueNode ___object;
    @Input
    // @field
    ValueNode ___offset;
    @Input
    // @field
    ValueNode ___expected;
    @Input
    // @field
    ValueNode ___newValue;

    // @field
    private final JavaKind ___valueKind;
    // @field
    private final LocationIdentity ___locationIdentity;

    // @cons
    public UnsafeCompareAndSwapNode(ValueNode __object, ValueNode __offset, ValueNode __expected, ValueNode __newValue, JavaKind __valueKind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean.getStackKind()));
        this.___object = __object;
        this.___offset = __offset;
        this.___expected = __expected;
        this.___newValue = __newValue;
        this.___valueKind = __valueKind;
        this.___locationIdentity = __locationIdentity;
    }

    public ValueNode object()
    {
        return this.___object;
    }

    public ValueNode offset()
    {
        return this.___offset;
    }

    public ValueNode expected()
    {
        return this.___expected;
    }

    public ValueNode newValue()
    {
        return this.___newValue;
    }

    public JavaKind getValueKind()
    {
        return this.___valueKind;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }
}
