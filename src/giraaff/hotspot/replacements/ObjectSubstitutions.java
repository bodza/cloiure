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
public class ObjectSubstitutions
{
    @MethodSubstitution(isStatic = false)
    public static int hashCode(final Object thisObj)
    {
        return IdentityHashCodeNode.identityHashCode(thisObj);
    }

    @MethodSubstitution(isStatic = false)
    public static void notify(final Object thisObj)
    {
        if (!fastNotifyStub(HotSpotHostForeignCallsProvider.NOTIFY, thisObj))
        {
            notify(thisObj);
        }
    }

    @MethodSubstitution(isStatic = false)
    public static void notifyAll(final Object thisObj)
    {
        if (!fastNotifyStub(HotSpotHostForeignCallsProvider.NOTIFY_ALL, thisObj))
        {
            notifyAll(thisObj);
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native boolean fastNotifyStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object o);
}
