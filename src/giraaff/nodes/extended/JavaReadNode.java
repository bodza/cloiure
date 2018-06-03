package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.memory.FixedAccessNode;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// Read a raw memory location according to Java field or array read semantics. It will perform read
// barriers, implicit conversions and optionally oop uncompression.
///
// @class JavaReadNode
public final class JavaReadNode extends FixedAccessNode implements Lowerable, GuardingNode, Canonicalizable
{
    // @def
    public static final NodeClass<JavaReadNode> TYPE = NodeClass.create(JavaReadNode.class);

    // @field
    protected final JavaKind ___readKind;
    // @field
    protected final boolean ___compressible;

    // @cons
    public JavaReadNode(JavaKind __readKind, AddressNode __address, LocationIdentity __location, BarrierType __barrierType, boolean __compressible)
    {
        super(TYPE, __address, __location, StampFactory.forKind(__readKind), __barrierType);
        this.___readKind = __readKind;
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

    public JavaKind getReadKind()
    {
        return this.___readKind;
    }

    public boolean isCompressible()
    {
        return this.___compressible;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        return ReadNode.canonicalizeRead(this, getAddress(), getLocationIdentity(), __tool);
    }
}
