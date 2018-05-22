package giraaff.phases.common;

import giraaff.nodes.ReturnNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.debug.VerifyHeapNode;
import giraaff.phases.Phase;

public class VerifyHeapAtReturnPhase extends Phase
{
    @Override
    protected void run(StructuredGraph graph)
    {
        for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE))
        {
            VerifyHeapNode.addBefore(returnNode);
        }
    }
}
