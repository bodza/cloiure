package graalvm.compiler.nodes.extended;

import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Copy a value at a location specified as an offset relative to a source object to another location
 * specified as an offset relative to destination object. No null checks are performed.
 */
public final class UnsafeCopyNode {

    public static boolean intrinsify(GraphBuilderContext b, @SuppressWarnings("unused") ResolvedJavaMethod targetMethod, ValueNode sourceObject, ValueNode sourceOffset, ValueNode destinationObject,
                    ValueNode destinationOffset, JavaKind accessKind, LocationIdentity locationIdentity) {
        RawLoadNode value = b.add(new RawLoadNode(sourceObject, sourceOffset, accessKind, locationIdentity));
        b.add(new RawStoreNode(destinationObject, destinationOffset, value, accessKind, locationIdentity));
        return true;
    }

    @NodeIntrinsic
    public static native void copy(Object srcObject, long srcOffset, Object destObject, long destOffset, @ConstantNodeParameter JavaKind kind,
                    @ConstantNodeParameter LocationIdentity locationIdentity);
}
