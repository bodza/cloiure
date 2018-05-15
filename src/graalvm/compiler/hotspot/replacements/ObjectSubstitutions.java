package graalvm.compiler.hotspot.replacements;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.nodes.extended.ForeignCallNode;

/**
 * Substitutions for {@link java.lang.Object} methods.
 */
@ClassSubstitution(Object.class)
public class ObjectSubstitutions {

    @MethodSubstitution(isStatic = false)
    public static int hashCode(final Object thisObj) {
        return IdentityHashCodeNode.identityHashCode(thisObj);
    }

    @MethodSubstitution(isStatic = false)
    public static void notify(final Object thisObj) {
        if (!fastNotifyStub(HotSpotHostForeignCallsProvider.NOTIFY, thisObj)) {
            notify(thisObj);
        }
    }

    @MethodSubstitution(isStatic = false)
    public static void notifyAll(final Object thisObj) {
        if (!fastNotifyStub(HotSpotHostForeignCallsProvider.NOTIFY_ALL, thisObj)) {
            notifyAll(thisObj);
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native boolean fastNotifyStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object o);
}
