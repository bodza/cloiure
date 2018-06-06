package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractWriteNode;
import giraaff.nodes.memory.HeapAccess;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// Write a raw memory location according to Java field or array write semantics. It will perform
// write barriers, implicit conversions and optionally oop compression.
///
// @class JavaWriteNode
public final class JavaWriteNode extends AbstractWriteNode implements Lowerable, StateSplit, MemoryAccess, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<JavaWriteNode> TYPE = NodeClass.create(JavaWriteNode.class);

    // @field
    protected final JavaKind ___writeKind;
    // @field
    protected final boolean ___compressible;

    // @cons JavaWriteNode
    public JavaWriteNode(JavaKind __writeKind, AddressNode __address, LocationIdentity __location, ValueNode __value, HeapAccess.BarrierType __barrierType, boolean __compressible)
    {
        super(TYPE, __address, __location, __value, __barrierType);
        this.___writeKind = __writeKind;
        this.___compressible = __compressible;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public boolean canNullCheck()
    {
        return true;
    }

    public JavaKind getWriteKind()
    {
        return this.___writeKind;
    }

    public boolean isCompressible()
    {
        return this.___compressible;
    }
}
