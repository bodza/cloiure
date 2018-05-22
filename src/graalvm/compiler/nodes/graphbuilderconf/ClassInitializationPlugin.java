package graalvm.compiler.nodes.graphbuilderconf;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ResolvedJavaType;

import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;

public interface ClassInitializationPlugin extends GraphBuilderPlugin
{
    boolean shouldApply(GraphBuilderContext builder, ResolvedJavaType type);

    ValueNode apply(GraphBuilderContext builder, ResolvedJavaType type, FrameState frameState);

    boolean supportsLazyInitialization(ConstantPool cp);

    void loadReferencedType(GraphBuilderContext builder, ConstantPool cp, int cpi, int bytecode);
}
