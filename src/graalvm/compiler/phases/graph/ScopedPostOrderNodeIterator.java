package graalvm.compiler.phases.graph;

import java.util.ArrayDeque;
import java.util.Deque;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeBitMap;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.ControlSinkNode;
import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.EndNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.StructuredGraph;

public abstract class ScopedPostOrderNodeIterator {

    private final Deque<FixedNode> nodeQueue;
    private final NodeBitMap queuedNodes;
    private final Deque<FixedNode> scopes;

    protected FixedNode currentScopeStart;

    public ScopedPostOrderNodeIterator(StructuredGraph graph) {
        this.queuedNodes = graph.createNodeBitMap();
        this.nodeQueue = new ArrayDeque<>();
        this.scopes = getScopes(graph);
    }

    public void apply() {
        while (!scopes.isEmpty()) {
            queuedNodes.clearAll();
            this.currentScopeStart = scopes.pop();
            initializeScope();
            processScope();
        }
    }

    public void processScope() {
        FixedNode current;
        queue(currentScopeStart);

        while ((current = nextQueuedNode()) != null) {
            assert current.isAlive();

            if (current instanceof Invoke) {
                invoke((Invoke) current);
                queueSuccessors(current);
            } else if (current instanceof LoopBeginNode) {
                queueLoopBeginSuccessors((LoopBeginNode) current);
            } else if (current instanceof LoopExitNode) {
                queueLoopExitSuccessors((LoopExitNode) current);
            } else if (current instanceof LoopEndNode) {
                // nothing todo
            } else if (current instanceof AbstractMergeNode) {
                queueSuccessors(current);
            } else if (current instanceof FixedWithNextNode) {
                queueSuccessors(current);
            } else if (current instanceof EndNode) {
                queueMerge((EndNode) current);
            } else if (current instanceof ControlSinkNode) {
                // nothing todo
            } else if (current instanceof ControlSplitNode) {
                queueSuccessors(current);
            } else {
                assert false : current;
            }
        }
    }

    protected void queueLoopBeginSuccessors(LoopBeginNode node) {
        if (currentScopeStart == node) {
            queue(node.next());
        } else if (currentScopeStart instanceof LoopBeginNode) {
            // so we are currently processing loop A and found another loop B
            // -> queue all loop exits of B except those that also exit loop A
            for (LoopExitNode loopExit : node.loopExits()) {
                if (!((LoopBeginNode) currentScopeStart).loopExits().contains(loopExit)) {
                    queue(loopExit);
                }
            }
        } else {
            queue(node.loopExits());
        }
    }

    protected void queueLoopExitSuccessors(LoopExitNode node) {
        if (!(currentScopeStart instanceof LoopBeginNode) || !((LoopBeginNode) currentScopeStart).loopExits().contains(node)) {
            queueSuccessors(node);
        }
    }

    protected Deque<FixedNode> getScopes(StructuredGraph graph) {
        Deque<FixedNode> result = new ArrayDeque<>();
        result.push(graph.start());
        for (LoopBeginNode loopBegin : graph.getNodes(LoopBeginNode.TYPE)) {
            result.push(loopBegin);
        }
        return result;
    }

    private void queueSuccessors(FixedNode x) {
        queue(x.successors());
    }

    private void queue(NodeIterable<? extends Node> iter) {
        for (Node node : iter) {
            queue(node);
        }
    }

    private void queue(Node node) {
        if (node != null && !queuedNodes.isMarked(node)) {
            queuedNodes.mark(node);
            nodeQueue.addFirst((FixedNode) node);
        }
    }

    private FixedNode nextQueuedNode() {
        if (nodeQueue.isEmpty()) {
            return null;
        }

        FixedNode result = nodeQueue.removeFirst();
        assert queuedNodes.isMarked(result);
        return result;
    }

    private void queueMerge(AbstractEndNode end) {
        AbstractMergeNode merge = end.merge();
        if (!queuedNodes.isMarked(merge) && visitedAllEnds(merge)) {
            queue(merge);
        }
    }

    private boolean visitedAllEnds(AbstractMergeNode merge) {
        for (int i = 0; i < merge.forwardEndCount(); i++) {
            if (!queuedNodes.isMarked(merge.forwardEndAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected abstract void initializeScope();

    protected abstract void invoke(Invoke invoke);
}
