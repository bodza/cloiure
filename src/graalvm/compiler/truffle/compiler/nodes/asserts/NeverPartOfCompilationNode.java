package graalvm.compiler.truffle.compiler.nodes.asserts;

import static graalvm.compiler.nodeinfo.InputType.State;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.VerificationError;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ControlSinkNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.util.GraphUtil;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class NeverPartOfCompilationNode extends ControlSinkNode implements StateSplit, IterableNodeType {

    public static final NodeClass<NeverPartOfCompilationNode> TYPE = NodeClass.create(NeverPartOfCompilationNode.class);
    protected final String message;
    @OptionalInput(State) protected FrameState stateAfter;

    public NeverPartOfCompilationNode(String message) {
        super(TYPE, StampFactory.forVoid());
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public FrameState stateAfter() {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x) {
        assert x == null || x.isAlive() : "frame state must be in a graph";
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    @Override
    public boolean hasSideEffect() {
        return true;
    }

    public static void verifyNotFoundIn(final StructuredGraph graph) {
        for (NeverPartOfCompilationNode neverPartOfCompilationNode : graph.getNodes(NeverPartOfCompilationNode.TYPE)) {
            Throwable exception = new VerificationError(neverPartOfCompilationNode.getMessage());
            throw GraphUtil.approxSourceException(neverPartOfCompilationNode, exception);
        }
    }
}
