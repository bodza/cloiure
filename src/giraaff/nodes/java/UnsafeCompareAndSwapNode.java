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
public final class UnsafeCompareAndSwapNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<UnsafeCompareAndSwapNode> TYPE = NodeClass.create(UnsafeCompareAndSwapNode.class);

    @Input ValueNode object;
    @Input ValueNode offset;
    @Input ValueNode expected;
    @Input ValueNode newValue;

    private final JavaKind valueKind;
    private final LocationIdentity locationIdentity;

    public UnsafeCompareAndSwapNode(ValueNode object, ValueNode offset, ValueNode expected, ValueNode newValue, JavaKind valueKind, LocationIdentity locationIdentity)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean.getStackKind()));
        this.object = object;
        this.offset = offset;
        this.expected = expected;
        this.newValue = newValue;
        this.valueKind = valueKind;
        this.locationIdentity = locationIdentity;
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
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }
}
