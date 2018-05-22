package graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.memory.FixedAccessNode;
import graalvm.compiler.nodes.memory.ReadNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

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
