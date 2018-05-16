package graalvm.compiler.nodes.graphbuilderconf;

import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.LogicNode;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public interface ProfilingPlugin extends GraphBuilderPlugin
{
    boolean shouldProfile(GraphBuilderContext builder, ResolvedJavaMethod method);

    void profileInvoke(GraphBuilderContext builder, ResolvedJavaMethod method, FrameState frameState);

    void profileGoto(GraphBuilderContext builder, ResolvedJavaMethod method, int bci, int targetBci, FrameState frameState);

    void profileIf(GraphBuilderContext builder, ResolvedJavaMethod method, int bci, LogicNode condition, int trueBranchBci, int falseBranchBci, FrameState frameState);
}
