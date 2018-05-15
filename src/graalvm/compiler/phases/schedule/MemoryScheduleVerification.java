package graalvm.compiler.phases.schedule;

import java.util.List;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.core.common.cfg.Loop;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.HIRLoop;
import graalvm.compiler.nodes.memory.FloatingReadNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryNode;
import graalvm.compiler.nodes.memory.MemoryPhiNode;
import graalvm.compiler.phases.graph.ReentrantBlockIterator;
import graalvm.compiler.phases.graph.ReentrantBlockIterator.BlockIteratorClosure;
import org.graalvm.word.LocationIdentity;

public final class MemoryScheduleVerification extends BlockIteratorClosure<EconomicSet<FloatingReadNode>> {

    private final BlockMap<List<Node>> blockToNodesMap;

    public static boolean check(Block startBlock, BlockMap<List<Node>> blockToNodesMap) {
        ReentrantBlockIterator.apply(new MemoryScheduleVerification(blockToNodesMap), startBlock);
        return true;
    }

    private MemoryScheduleVerification(BlockMap<List<Node>> blockToNodesMap) {
        this.blockToNodesMap = blockToNodesMap;
    }

    @Override
    protected EconomicSet<FloatingReadNode> getInitialState() {
        return EconomicSet.create(Equivalence.IDENTITY);
    }

    @Override
    protected EconomicSet<FloatingReadNode> processBlock(Block block, EconomicSet<FloatingReadNode> currentState) {
        AbstractBeginNode beginNode = block.getBeginNode();
        if (beginNode instanceof AbstractMergeNode) {
            AbstractMergeNode abstractMergeNode = (AbstractMergeNode) beginNode;
            for (PhiNode phi : abstractMergeNode.phis()) {
                if (phi instanceof MemoryPhiNode) {
                    MemoryPhiNode memoryPhiNode = (MemoryPhiNode) phi;
                    addFloatingReadUsages(currentState, memoryPhiNode);
                }
            }
        }
        for (Node n : blockToNodesMap.get(block)) {
            if (n instanceof MemoryCheckpoint) {
                if (n instanceof MemoryCheckpoint.Single) {
                    MemoryCheckpoint.Single single = (MemoryCheckpoint.Single) n;
                    processLocation(n, single.getLocationIdentity(), currentState);
                } else if (n instanceof MemoryCheckpoint.Multi) {
                    MemoryCheckpoint.Multi multi = (MemoryCheckpoint.Multi) n;
                    for (LocationIdentity location : multi.getLocationIdentities()) {
                        processLocation(n, location, currentState);
                    }
                }

                addFloatingReadUsages(currentState, n);
            } else if (n instanceof MemoryNode) {
                addFloatingReadUsages(currentState, n);
            } else if (n instanceof FloatingReadNode) {
                FloatingReadNode floatingReadNode = (FloatingReadNode) n;
                if (floatingReadNode.getLastLocationAccess() != null && floatingReadNode.getLocationIdentity().isMutable()) {
                    if (currentState.contains(floatingReadNode)) {
                        // Floating read was found in the state.
                        currentState.remove(floatingReadNode);
                    } else {
                        throw new RuntimeException("Floating read node " + n + " was not found in the state, i.e., it was killed by a memory check point before its place in the schedule. Block=" +
                                        block + ", block begin: " + block.getBeginNode() + " block loop: " + block.getLoop() + ", " + blockToNodesMap.get(block).get(0));
                    }
                }

            }
        }
        return currentState;
    }

    private static void addFloatingReadUsages(EconomicSet<FloatingReadNode> currentState, Node n) {
        for (FloatingReadNode read : n.usages().filter(FloatingReadNode.class)) {
            if (read.getLastLocationAccess() == n && read.getLocationIdentity().isMutable()) {
                currentState.add(read);
            }
        }
    }

    private void processLocation(Node n, LocationIdentity location, EconomicSet<FloatingReadNode> currentState) {
        assert n != null;
        if (location.isImmutable()) {
            return;
        }

        for (FloatingReadNode r : cloneState(currentState)) {
            if (r.getLocationIdentity().overlaps(location)) {
                // This read is killed by this location.
                r.getDebug().log(DebugContext.VERBOSE_LEVEL, "%s removing %s from state", n, r);
                currentState.remove(r);
            }
        }
    }

    @Override
    protected EconomicSet<FloatingReadNode> merge(Block merge, List<EconomicSet<FloatingReadNode>> states) {
        EconomicSet<FloatingReadNode> result = states.get(0);
        for (int i = 1; i < states.size(); ++i) {
            result.retainAll(states.get(i));
        }
        return result;
    }

    @Override
    protected EconomicSet<FloatingReadNode> cloneState(EconomicSet<FloatingReadNode> oldState) {
        EconomicSet<FloatingReadNode> result = EconomicSet.create(Equivalence.IDENTITY);
        if (oldState != null) {
            result.addAll(oldState);
        }
        return result;
    }

    @Override
    protected List<EconomicSet<FloatingReadNode>> processLoop(Loop<Block> loop, EconomicSet<FloatingReadNode> initialState) {
        HIRLoop l = (HIRLoop) loop;
        for (MemoryPhiNode memoryPhi : ((LoopBeginNode) l.getHeader().getBeginNode()).memoryPhis()) {
            for (FloatingReadNode r : cloneState(initialState)) {
                if (r.getLocationIdentity().overlaps(memoryPhi.getLocationIdentity())) {
                    initialState.remove(r);
                }
            }
        }
        return ReentrantBlockIterator.processLoop(this, loop, initialState).exitStates;
    }
}
