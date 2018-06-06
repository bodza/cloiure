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

    @Node.Input
    // @field
    ValueNode ___object;
    @Node.Input
    // @field
    ValueNode ___offset;
    // @field
    protected final JavaKind ___accessKind;
    // @field
    protected final LocationIdentity ___locationIdentity;
    // @field
    protected final boolean ___forceAnyLocation;

    // @cons UnsafeAccessNode
    protected UnsafeAccessNode(NodeClass<? extends UnsafeAccessNode> __c, Stamp __stamp, ValueNode __object, ValueNode __offset, JavaKind __accessKind, LocationIdentity __locationIdentity, boolean __forceAnyLocation)
    {
        super(__c, __stamp);
        this.___forceAnyLocation = __forceAnyLocation;
        this.___object = __object;
        this.___offset = __offset;
        this.___accessKind = __accessKind;
        this.___locationIdentity = __locationIdentity;
    }

    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }

    public boolean isAnyLocationForced()
    {
        return this.___forceAnyLocation;
    }

    public ValueNode object()
    {
        return this.___object;
    }

    public ValueNode offset()
    {
        return this.___offset;
    }

    public JavaKind accessKind()
    {
        return this.___accessKind;
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

    protected abstract ValueNode cloneAsFieldAccess(Assumptions __assumptions, ResolvedJavaField __field);

    protected abstract ValueNode cloneAsArrayAccess(ValueNode __location, LocationIdentity __identity);
}
