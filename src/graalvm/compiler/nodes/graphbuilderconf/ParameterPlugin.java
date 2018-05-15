package graalvm.compiler.nodes.graphbuilderconf;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.nodes.calc.FloatingNode;

public interface ParameterPlugin extends GraphBuilderPlugin {

    FloatingNode interceptParameter(GraphBuilderTool b, int index, StampPair stamp);
}
