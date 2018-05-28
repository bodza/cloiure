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

/**
 * Read a raw memory location according to Java field or array read semantics. It will perform read
 * barriers, implicit conversions and optionally oop uncompression.
 */
public final class JavaReadNode extends FixedAccessNode implements Lowerable, GuardingNode, Canonicalizable
{
    public static final NodeClass<JavaReadNode> TYPE = NodeClass.create(JavaReadNode.class);

    protected final JavaKind readKind;
    protected final boolean compressible;

    public JavaReadNode(JavaKind readKind, AddressNode address, LocationIdentity location, BarrierType barrierType, boolean compressible)
    {
        super(TYPE, address, location, StampFactory.forKind(readKind), barrierType);
        this.readKind = readKind;
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

    public JavaKind getReadKind()
    {
        return readKind;
    }

    public boolean isCompressible()
    {
        return compressible;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        return ReadNode.canonicalizeRead(this, getAddress(), getLocationIdentity(), tool);
    }
}
