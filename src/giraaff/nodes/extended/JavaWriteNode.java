package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractWriteNode;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Write a raw memory location according to Java field or array write semantics. It will perform
 * write barriers, implicit conversions and optionally oop compression.
 */
public final class JavaWriteNode extends AbstractWriteNode implements Lowerable, StateSplit, MemoryAccess, MemoryCheckpoint.Single
{
    public static final NodeClass<JavaWriteNode> TYPE = NodeClass.create(JavaWriteNode.class);

    protected final JavaKind writeKind;
    protected final boolean compressible;

    public JavaWriteNode(JavaKind writeKind, AddressNode address, LocationIdentity location, ValueNode value, BarrierType barrierType, boolean compressible)
    {
        super(TYPE, address, location, value, barrierType);
        this.writeKind = writeKind;
        this.compressible = compressible;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public boolean canNullCheck()
    {
        return true;
    }

    public JavaKind getWriteKind()
    {
        return writeKind;
    }

    public boolean isCompressible()
    {
        return compressible;
    }
}
