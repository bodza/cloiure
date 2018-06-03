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
 * Represents an atomic read-and-write operation like
 * {@link sun.misc.Unsafe#getAndSetInt(Object, long, int)}.
 */
// @class AtomicReadAndWriteNode
public final class AtomicReadAndWriteNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<AtomicReadAndWriteNode> TYPE = NodeClass.create(AtomicReadAndWriteNode.class);

    @Input
    // @field
    ValueNode object;
    @Input
    // @field
    ValueNode offset;
    @Input
    // @field
    ValueNode newValue;

    // @field
    protected final JavaKind valueKind;
    // @field
    protected final LocationIdentity locationIdentity;

    // @cons
    public AtomicReadAndWriteNode(ValueNode __object, ValueNode __offset, ValueNode __newValue, JavaKind __valueKind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forKind(__newValue.getStackKind()));
        this.object = __object;
        this.offset = __offset;
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
