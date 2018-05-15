package graalvm.compiler.truffle.compiler.substitutions;

import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

public interface TruffleInvocationPluginProvider {
    void registerInvocationPlugins(MetaAccessProvider metaAccess, InvocationPlugins plugins, boolean canDelayIntrinsification, ConstantReflectionProvider constantReflection);
}
