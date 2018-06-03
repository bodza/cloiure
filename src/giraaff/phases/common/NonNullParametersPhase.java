package giraaff.phases.common;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.Phase;

///
// Modifies the stamp of all object {@linkplain ParameterNode parameters} in a graph to denote they
// are non-null. This can be used for graphs where the caller null checks all arguments.
///
// @class NonNullParametersPhase
public final class NonNullParametersPhase extends Phase
{
    @Override
    protected void run(StructuredGraph __graph)
    {
        Stamp __nonNull = StampFactory.objectNonNull();
        for (ParameterNode __param : __graph.getNodes(ParameterNode.TYPE))
        {
            if (__param.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
            {
                ObjectStamp __paramStamp = (ObjectStamp) __param.stamp(NodeView.DEFAULT);
                __param.setStamp(__paramStamp.join(__nonNull));
            }
        }
    }
}
