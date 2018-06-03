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

/**
 * Represents an atomic compare-and-swap operation The result is a boolean that contains whether the
 * value matched the expected value.
 */
// @NodeInfo.allowedUsageTypes "Value, Memory"
// @class UnsafeCompareAndSwapNode
public final class UnsafeCompareAndSwapNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<UnsafeCompareAndSwapNode> TYPE = NodeClass.create(UnsafeCompareAndSwapNode.class);

    @Input
    // @field
    ValueNode object;
    @Input
    // @field
    ValueNode offset;
    @Input
    // @field
    ValueNode expected;
    @Input
    // @field
    ValueNode newValue;

    // @field
    private final JavaKind valueKind;
    // @field
    private final LocationIdentity locationIdentity;

    // @cons
    public UnsafeCompareAndSwapNode(ValueNode __object, ValueNode __offset, ValueNode __expected, ValueNode __newValue, JavaKind __valueKind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean.getStackKind()));
        this.object = __object;
        this.offset = __offset;
        this.expected = __expected;
        this.newValue = __newValue;
        this.valueKind = __valueKind;
        this.locationIdentity = __locationIdentity;
    }

    public ValueNode object()
    {
        return object;
    }

    public ValueNode offset()
    {
        return offset;
    }

    public ValueNode expected()
    {
        return expected;
    }

    public ValueNode newValue()
    {
        return newValue;
    }

    public JavaKind getValueKind()
    {
        return valueKind;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }
}
