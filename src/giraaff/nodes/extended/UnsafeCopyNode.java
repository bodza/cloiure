package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.Node;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;

///
// Copy a value at a location specified as an offset relative to a source object to another location
// specified as an offset relative to destination object. No null checks are performed.
///
// @class UnsafeCopyNode
public final class UnsafeCopyNode
{
    public static boolean intrinsify(GraphBuilderContext __b, @SuppressWarnings("unused") ResolvedJavaMethod __targetMethod, ValueNode __sourceObject, ValueNode __sourceOffset, ValueNode __destinationObject, ValueNode __destinationOffset, JavaKind __accessKind, LocationIdentity __locationIdentity)
    {
        RawLoadNode __value = __b.add(new RawLoadNode(__sourceObject, __sourceOffset, __accessKind, __locationIdentity));
        __b.add(new RawStoreNode(__destinationObject, __destinationOffset, __value, __accessKind, __locationIdentity));
        return true;
    }

    @Node.NodeIntrinsic
    public static native void copy(Object __srcObject, long __srcOffset, Object __destObject, long __destOffset, @Node.ConstantNodeParameter JavaKind __kind, @Node.ConstantNodeParameter LocationIdentity __locationIdentity);
}
