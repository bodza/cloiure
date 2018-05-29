package giraaff.nodes.graphbuilderconf;

import giraaff.core.common.type.StampPair;
import giraaff.nodes.calc.FloatingNode;

// @iface ParameterPlugin
public interface ParameterPlugin extends GraphBuilderPlugin
{
    FloatingNode interceptParameter(GraphBuilderTool b, int index, StampPair stamp);
}
