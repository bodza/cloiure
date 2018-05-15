package graalvm.compiler.phases.common;

import graalvm.compiler.nodes.ReturnNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.debug.VerifyHeapNode;
import graalvm.compiler.phases.Phase;

public class VerifyHeapAtReturnPhase extends Phase {

    @Override
    protected void run(StructuredGraph graph) {
        for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE)) {
            VerifyHeapNode.addBefore(returnNode);
        }
    }
}
