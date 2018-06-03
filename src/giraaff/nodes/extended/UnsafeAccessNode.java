package giraaff.nodes.extended;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.ValueNode;
import giraaff.nodes.type.StampTool;

// @class UnsafeAccessNode
public abstract class UnsafeAccessNode extends FixedWithNextNode implements Canonicalizable
{
    // @def
    public static final NodeClass<UnsafeAccessNode> TYPE = NodeClass.create(UnsafeAccessNode.class);

    @Input
    // @field
    ValueNode object;
    @Input
    // @field
    ValueNode offset;
    // @field
    protected final JavaKind accessKind;
    // @field
    protected final LocationIdentity locationIdentity;
    // @field
    protected final boolean forceAnyLocation;

    // @cons
    protected UnsafeAccessNode(NodeClass<? extends UnsafeAccessNode> __c, Stamp __stamp, ValueNode __object, ValueNode __offset, JavaKind __accessKind, LocationIdentity __locationIdentity, boolean __forceAnyLocation)
    {
        super(__c, __stamp);
        this.forceAnyLocation = __forceAnyLocation;
        this.object = __object;
        this.offset = __offset;
        this.accessKind = __accessKind;
        this.locationIdentity = __locationIdentity;
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
    public Node canonical(CanonicalizerTool __tool)
    {
        if (!isAnyLocationForced() && getLocationIdentity().isAny())
        {
            if (offset().isConstant())
            {
                long __constantOffset = offset().asJavaConstant().asLong();

                // Try to canonicalize to a field access.
                ResolvedJavaType __receiverType = StampTool.typeOrNull(object());
                if (__receiverType != null)
                {
                    ResolvedJavaField __field = __receiverType.findInstanceFieldWithOffset(__constantOffset, accessKind());
                    // No need for checking that the receiver is non-null. The field access includes
                    // the null check and if a field is found, the offset is so small that this is
                    // never a valid access of an arbitrary address.
                    if (__field != null && __field.getJavaKind() == this.accessKind())
                    {
                        return cloneAsFieldAccess(graph().getAssumptions(), __field);
                    }
                }
            }
            ResolvedJavaType __receiverType = StampTool.typeOrNull(object());
            // Try to build a better location identity.
            if (__receiverType != null && __receiverType.isArray())
            {
                LocationIdentity __identity = NamedLocationIdentity.getArrayLocation(__receiverType.getComponentType().getJavaKind());
                return cloneAsArrayAccess(offset(), __identity);
            }
        }

        return this;
    }

    protected abstract ValueNode cloneAsFieldAccess(Assumptions assumptions, ResolvedJavaField field);

    protected abstract ValueNode cloneAsArrayAccess(ValueNode location, LocationIdentity identity);
}
