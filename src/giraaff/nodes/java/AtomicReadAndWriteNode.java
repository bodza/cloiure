package giraaff.nodes.java;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// Represents an atomic read-and-write operation like
// {@link sun.misc.Unsafe#getAndSetInt(Object, long, int)}.
///
// @class AtomicReadAndWriteNode
public final class AtomicReadAndWriteNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<AtomicReadAndWriteNode> TYPE = NodeClass.create(AtomicReadAndWriteNode.class);

    @Node.Input
    // @field
    ValueNode ___object;
    @Node.Input
    // @field
    ValueNode ___offset;
    @Node.Input
    // @field
    ValueNode ___newValue;

    // @field
    protected final JavaKind ___valueKind;
    // @field
    protected final LocationIdentity ___locationIdentity;

    // @cons AtomicReadAndWriteNode
    public AtomicReadAndWriteNode(ValueNode __object, ValueNode __offset, ValueNode __newValue, JavaKind __valueKind, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forKind(__newValue.getStackKind()));
        this.___object = __object;
        this.___offset = __offset;
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
