package graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.AbstractWriteNode;
import graalvm.compiler.nodes.memory.MemoryAccess;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

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
