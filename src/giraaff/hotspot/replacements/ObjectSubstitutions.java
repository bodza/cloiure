package giraaff.hotspot.replacements;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.nodes.extended.ForeignCallNode;

/**
 * Substitutions for {@link java.lang.Object} methods.
 */
@ClassSubstitution(Object.class)
// @class ObjectSubstitutions
public final class ObjectSubstitutions
{
    @MethodSubstitution(isStatic = false)
    public static int hashCode(final Object __thisObj)
    {
        return IdentityHashCodeNode.identityHashCode(__thisObj);
    }
}
