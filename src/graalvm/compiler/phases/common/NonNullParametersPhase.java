package graalvm.compiler.phases.common;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ParameterNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.Phase;

/**
 * Modifies the stamp of all object {@linkplain ParameterNode parameters} in a graph to denote they
 * are non-null. This can be used for graphs where the caller null checks all arguments.
 */
public class NonNullParametersPhase extends Phase
{
    @Override
    protected void run(StructuredGraph graph)
    {
        Stamp nonNull = StampFactory.objectNonNull();
        for (ParameterNode param : graph.getNodes(ParameterNode.TYPE))
        {
            if (param.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
            {
                ObjectStamp paramStamp = (ObjectStamp) param.stamp(NodeView.DEFAULT);
                param.setStamp(paramStamp.join(nonNull));
            }
        }
    }
}
