package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NamedLocationIdentity;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.type.StampTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public abstract class UnsafeAccessNode extends FixedWithNextNode implements Canonicalizable
{
    public static final NodeClass<UnsafeAccessNode> TYPE = NodeClass.create(UnsafeAccessNode.class);
    @Input ValueNode object;
    @Input ValueNode offset;
    protected final JavaKind accessKind;
    protected final LocationIdentity locationIdentity;
    protected final boolean forceAnyLocation;

    protected UnsafeAccessNode(NodeClass<? extends UnsafeAccessNode> c, Stamp stamp, ValueNode object, ValueNode offset, JavaKind accessKind, LocationIdentity locationIdentity, boolean forceAnyLocation)
    {
        super(c, stamp);
        this.forceAnyLocation = forceAnyLocation;
        this.object = object;
        this.offset = offset;
        this.accessKind = accessKind;
        this.locationIdentity = locationIdentity;
    }

    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }

    public boolean isAnyLocationForced()
    {
        return forceAnyLocation;
    }

    public ValueNode object()
    {
        return object;
    }

    public ValueNode offset()
    {
        return offset;
    }

    public JavaKind accessKind()
    {
        return accessKind;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (!isAnyLocationForced() && getLocationIdentity().isAny())
        {
            if (offset().isConstant())
            {
                long constantOffset = offset().asJavaConstant().asLong();

                // Try to canonicalize to a field access.
                ResolvedJavaType receiverType = StampTool.typeOrNull(object());
                if (receiverType != null)
                {
                    ResolvedJavaField field = receiverType.findInstanceFieldWithOffset(constantOffset, accessKind());
                    // No need for checking that the receiver is non-null. The field access includes
                    // the null check and if a field is found, the offset is so small that this is
                    // never a valid access of an arbitrary address.
                    if (field != null && field.getJavaKind() == this.accessKind())
                    {
                        return cloneAsFieldAccess(graph().getAssumptions(), field);
                    }
                }
            }
            ResolvedJavaType receiverType = StampTool.typeOrNull(object());
            // Try to build a better location identity.
            if (receiverType != null && receiverType.isArray())
            {
                LocationIdentity identity = NamedLocationIdentity.getArrayLocation(receiverType.getComponentType().getJavaKind());
                return cloneAsArrayAccess(offset(), identity);
            }
        }

        return this;
    }

    protected abstract ValueNode cloneAsFieldAccess(Assumptions assumptions, ResolvedJavaField field);

    protected abstract ValueNode cloneAsArrayAccess(ValueNode location, LocationIdentity identity);
}
