package graalvm.compiler.truffle.compiler.hotspot;

import java.lang.ref.Reference;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;

final class HotSpotTruffleGraphBuilderPlugins {
    static void registerCompilationFinalReferencePlugins(InvocationPlugins plugins, boolean canDelayIntrinsification, HotSpotKnownTruffleTypes types) {
        InvocationPlugins.Registration r = new InvocationPlugins.Registration(plugins, Reference.class);
        r.register1("get", InvocationPlugin.Receiver.class, new InvocationPlugin() {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver) {
                if (!canDelayIntrinsification && receiver.isConstant()) {
                    JavaConstant constant = (JavaConstant) receiver.get().asConstant();
                    if (constant.isNonNull()) {
                        if (types.classWeakReference.isInstance(constant) || types.classSoftReference.isInstance(constant)) {
                            JavaConstant referent = b.getConstantReflection().readFieldValue(types.referenceReferent, constant);
                            b.addPush(JavaKind.Object, ConstantNode.forConstant(referent, b.getMetaAccess()));
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }
}
