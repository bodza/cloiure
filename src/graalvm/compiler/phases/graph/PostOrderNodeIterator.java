package graalvm.compiler.phases.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeBitMap;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.ControlSinkNode;
import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.EndNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.InvokeWithExceptionNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.StructuredGraph;

/**
 * A PostOrderNodeIterator iterates the fixed nodes of the graph in post order starting from a
 * specified fixed node.
 * <p>
 * For this iterator the CFG is defined by the classical CFG nodes ({@link ControlSplitNode},
 * {@link AbstractMergeNode}...) and the {@link FixedWithNextNode#next() next} pointers of
 * {@link FixedWithNextNode}.
 * <p>
 * While iterating it maintains a user-defined state by calling the methods available in
 * {@link MergeableState}.
 *
 * @param <T> the type of {@link MergeableState} handled by this PostOrderNodeIterator
 */
public abstract class PostOrderNodeIterator<T extends MergeableState<T>> {

    private final NodeBitMap visitedEnds;
    private final Deque<AbstractBeginNode> nodeQueue;
    private final EconomicMap<FixedNode, T> nodeStates;
    private final FixedNode start;

    protected T state;

    public PostOrderNodeIterator(FixedNode start, T initialState) {
        StructuredGraph graph = start.graph();
        visitedEnds = graph.createNodeBitMap();
        nodeQueue = new ArrayDeque<>();
        nodeStates = EconomicMap.create(Equivalence.IDENTITY);
        this.start = start;
        this.state = initialState;
    }

    public void apply() {
        FixedNode current = start;

        do {
            if (current instanceof InvokeWithExceptionNode) {
                invoke((Invoke) current);
                queueSuccessors(current, null);
                current = nextQueuedNode();
            } else if (current instanceof LoopBeginNode) {
                state.loopBegin((LoopBeginNode) current);
                nodeStates.put(current, state);
                state = state.clone();
                loopBegin((LoopBeginNode) current);
                current = ((LoopBeginNode) current).next();
                assert current != null;
            } else if (current instanceof LoopEndNode) {
                loopEnd((LoopEndNode) current);
                finishLoopEnds((LoopEndNode) current);
                current = nextQueuedNode();
            } else if (current instanceof AbstractMergeNode) {
                merge((AbstractMergeNode) current);
                current = ((AbstractMergeNode) current).next();
                assert current != null;
            } else if (current instanceof FixedWithNextNode) {
                FixedNode next = ((FixedWithNextNode) current).next();
                assert next != null : current;
                node(current);
                current = next;
            } else if (current instanceof EndNode) {
                end((EndNode) current);
                queueMerge((EndNode) current);
                current = nextQueuedNode();
            } else if (current instanceof ControlSinkNode) {
                node(current);
                current = nextQueuedNode();
            } else if (current instanceof ControlSplitNode) {
                Set<Node> successors = controlSplit((ControlSplitNode) current);
                queueSuccessors(current, successors);
                current = nextQueuedNode();
            } else {
                assert false : current;
            }
        } while (current != null);
        finished();
    }

    private void queueSuccessors(FixedNode x, Set<Node> successors) {
        nodeStates.put(x, state);
        if (successors != null) {
            for (Node node : successors) {
                if (node != null) {
                    nodeStates.put((FixedNode) node.predecessor(), state);
                    nodeQueue.addFirst((AbstractBeginNode) node);
                }
            }
        } else {
            for (Node node : x.successors()) {
                if (node != null) {
                    nodeQueue.addFirst((AbstractBeginNode) node);
                }
            }
        }
    }

    private FixedNode nextQueuedNode() {
        int maxIterations = nodeQueue.size();
        while (maxIterations-- > 0) {
            AbstractBeginNode node = nodeQueue.removeFirst();
            if (node instanceof AbstractMergeNode) {
                AbstractMergeNode merge = (AbstractMergeNode) node;
                state = nodeStates.get(merge.forwardEndAt(0)).clone();
                ArrayList<T> states = new ArrayList<>(merge.forwardEndCount() - 1);
                for (int i = 1; i < merge.forwardEndCount(); i++) {
                    T other = nodeStates.get(merge.forwardEndAt(i));
                    assert other != null;
                    states.add(other);
                }
                boolean ready = state.merge(merge, states);
                if (ready) {
                    return merge;
                } else {
                    nodeQueue.addLast(merge);
                }
            } else {
                assert node.predecessor() != null;
                state = nodeStates.get((FixedNode) node.predecessor()).clone();
                state.afterSplit(node);
                return node;
            }
        }
        return null;
    }

    private void finishLoopEnds(LoopEndNode end) {
        assert !visitedEnds.isMarked(end);
        assert !nodeStates.containsKey(end);
        nodeStates.put(end, state);
        visitedEnds.mark(end);
        LoopBeginNode begin = end.loopBegin();
        boolean endsVisited = true;
        for (LoopEndNode le : begin.loopEnds()) {
            if (!visitedEnds.isMarked(le)) {
                endsVisited = false;
                break;
            }
        }
        if (endsVisited) {
            ArrayList<T> states = new ArrayList<>(begin.loopEnds().count());
            for (LoopEndNode le : begin.orderedLoopEnds()) {
                states.add(nodeStates.get(le));
            }
            T loopBeginState = nodeStates.get(begin);
            if (loopBeginState != null) {
                loopBeginState.loopEnds(begin, states);
            }
        }
    }

    private void queueMerge(EndNode end) {
        assert !visitedEnds.isMarked(end);
        assert !nodeStates.containsKey(end);
        nodeStates.put(end, state);
        visitedEnds.mark(end);
        AbstractMergeNode merge = end.merge();
        boolean endsVisited = true;
        for (int i = 0; i < merge.forwardEndCount(); i++) {
            if (!visitedEnds.isMarked(merge.forwardEndAt(i))) {
                endsVisited = false;
                break;
            }
        }
        if (endsVisited) {
            nodeQueue.add(merge);
        }
    }

    protected abstract void node(FixedNode node);

    protected void end(EndNode endNode) {
        node(endNode);
    }

    protected void merge(AbstractMergeNode merge) {
        node(merge);
    }

    protected void loopBegin(LoopBeginNode loopBegin) {
        node(loopBegin);
    }

    protected void loopEnd(LoopEndNode loopEnd) {
        node(loopEnd);
    }

    protected Set<Node> controlSplit(ControlSplitNode controlSplit) {
        node(controlSplit);
        return null;
    }

    protected void invoke(Invoke invoke) {
        node(invoke.asNode());
    }

    protected void finished() {
        // nothing to do
    }
}
