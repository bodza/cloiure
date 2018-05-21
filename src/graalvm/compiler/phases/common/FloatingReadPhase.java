package graalvm.compiler.phases.common;

import static graalvm.compiler.graph.Graph.NodeEvent.NODE_ADDED;
import static graalvm.compiler.graph.Graph.NodeEvent.ZERO_USAGES;
import static org.graalvm.word.LocationIdentity.any;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableMapCursor;
import graalvm.compiler.core.common.cfg.Loop;
import graalvm.compiler.graph.Graph.NodeEventScope;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.InvokeWithExceptionNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.ReturnNode;
import graalvm.compiler.nodes.StartNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNodeUtil;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.cfg.HIRLoop;
import graalvm.compiler.nodes.memory.FloatableAccessNode;
import graalvm.compiler.nodes.memory.FloatingAccessNode;
import graalvm.compiler.nodes.memory.FloatingReadNode;
import graalvm.compiler.nodes.memory.MemoryAccess;
import graalvm.compiler.nodes.memory.MemoryAnchorNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryMap;
import graalvm.compiler.nodes.memory.MemoryMapNode;
import graalvm.compiler.nodes.memory.MemoryNode;
import graalvm.compiler.nodes.memory.MemoryPhiNode;
import graalvm.compiler.nodes.memory.ReadNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.Phase;
import graalvm.compiler.phases.common.util.HashSetNodeEventListener;
import graalvm.compiler.phases.graph.ReentrantNodeIterator;
import graalvm.compiler.phases.graph.ReentrantNodeIterator.LoopInfo;
import graalvm.compiler.phases.graph.ReentrantNodeIterator.NodeIteratorClosure;
import org.graalvm.word.LocationIdentity;

public class FloatingReadPhase extends Phase
{
    private boolean createFloatingReads;
    private boolean createMemoryMapNodes;

    public static class MemoryMapImpl implements MemoryMap
    {
        private final EconomicMap<LocationIdentity, MemoryNode> lastMemorySnapshot;

        public MemoryMapImpl(MemoryMapImpl memoryMap)
        {
            lastMemorySnapshot = EconomicMap.create(Equivalence.DEFAULT, memoryMap.lastMemorySnapshot);
        }

        public MemoryMapImpl(StartNode start)
        {
            this();
            lastMemorySnapshot.put(any(), start);
        }

        public MemoryMapImpl()
        {
            lastMemorySnapshot = EconomicMap.create(Equivalence.DEFAULT);
        }

        @Override
        public MemoryNode getLastLocationAccess(LocationIdentity locationIdentity)
        {
            MemoryNode lastLocationAccess;
            if (locationIdentity.isImmutable())
            {
                return null;
            }
            else
            {
                lastLocationAccess = lastMemorySnapshot.get(locationIdentity);
                if (lastLocationAccess == null)
                {
                    lastLocationAccess = lastMemorySnapshot.get(any());
                }
                return lastLocationAccess;
            }
        }

        @Override
        public Iterable<LocationIdentity> getLocations()
        {
            return lastMemorySnapshot.getKeys();
        }

        public EconomicMap<LocationIdentity, MemoryNode> getMap()
        {
            return lastMemorySnapshot;
        }
    }

    public FloatingReadPhase()
    {
        this(true, false);
    }

    /**
     * @param createFloatingReads specifies whether {@link FloatableAccessNode}s like
     *            {@link ReadNode} should be converted into floating nodes (e.g.,
     *            {@link FloatingReadNode}s) where possible
     * @param createMemoryMapNodes a {@link MemoryMapNode} will be created for each return if this
     *            is true
     */
    public FloatingReadPhase(boolean createFloatingReads, boolean createMemoryMapNodes)
    {
        this.createFloatingReads = createFloatingReads;
        this.createMemoryMapNodes = createMemoryMapNodes;
    }

    /**
     * Removes nodes from a given set that (transitively) have a usage outside the set.
     */
    private static EconomicSet<Node> removeExternallyUsedNodes(EconomicSet<Node> set)
    {
        boolean change;
        do
        {
            change = false;
            for (Iterator<Node> iter = set.iterator(); iter.hasNext();)
            {
                Node node = iter.next();
                for (Node usage : node.usages())
                {
                    if (!set.contains(usage))
                    {
                        change = true;
                        iter.remove();
                        break;
                    }
                }
            }
        } while (change);
        return set;
    }

    protected void processNode(FixedNode node, EconomicSet<LocationIdentity> currentState)
    {
        if (node instanceof MemoryCheckpoint.Single)
        {
            processIdentity(currentState, ((MemoryCheckpoint.Single) node).getLocationIdentity());
        }
        else if (node instanceof MemoryCheckpoint.Multi)
        {
            for (LocationIdentity identity : ((MemoryCheckpoint.Multi) node).getLocationIdentities())
            {
                processIdentity(currentState, identity);
            }
        }
    }

    private static void processIdentity(EconomicSet<LocationIdentity> currentState, LocationIdentity identity)
    {
        if (identity.isMutable())
        {
            currentState.add(identity);
        }
    }

    protected void processBlock(Block b, EconomicSet<LocationIdentity> currentState)
    {
        for (FixedNode n : b.getNodes())
        {
            processNode(n, currentState);
        }
    }

    private EconomicSet<LocationIdentity> processLoop(HIRLoop loop, EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> modifiedInLoops)
    {
        LoopBeginNode loopBegin = (LoopBeginNode) loop.getHeader().getBeginNode();
        EconomicSet<LocationIdentity> result = modifiedInLoops.get(loopBegin);
        if (result != null)
        {
            return result;
        }

        result = EconomicSet.create(Equivalence.DEFAULT);
        for (Loop<Block> inner : loop.getChildren())
        {
            result.addAll(processLoop((HIRLoop) inner, modifiedInLoops));
        }

        for (Block b : loop.getBlocks())
        {
            if (b.getLoop() == loop)
            {
                processBlock(b, result);
            }
        }

        modifiedInLoops.put(loopBegin, result);
        return result;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph)
    {
        EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> modifiedInLoops = null;
        if (graph.hasLoops())
        {
            modifiedInLoops = EconomicMap.create(Equivalence.IDENTITY);
            ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, false, false);
            for (Loop<?> l : cfg.getLoops())
            {
                HIRLoop loop = (HIRLoop) l;
                processLoop(loop, modifiedInLoops);
            }
        }

        HashSetNodeEventListener listener = new HashSetNodeEventListener(EnumSet.of(NODE_ADDED, ZERO_USAGES));
        try (NodeEventScope nes = graph.trackNodeEvents(listener))
        {
            ReentrantNodeIterator.apply(new FloatingReadClosure(modifiedInLoops, createFloatingReads, createMemoryMapNodes), graph.start(), new MemoryMapImpl(graph.start()));
        }

        for (Node n : removeExternallyUsedNodes(listener.getNodes()))
        {
            if (n.isAlive() && n instanceof FloatingNode)
            {
                n.replaceAtUsages(null);
                GraphUtil.killWithUnusedFloatingInputs(n);
            }
        }
        if (createFloatingReads)
        {
            graph.setAfterFloatingReadPhase(true);
        }
    }

    public static MemoryMapImpl mergeMemoryMaps(AbstractMergeNode merge, List<? extends MemoryMap> states)
    {
        MemoryMapImpl newState = new MemoryMapImpl();

        EconomicSet<LocationIdentity> keys = EconomicSet.create(Equivalence.DEFAULT);
        for (MemoryMap other : states)
        {
            keys.addAll(other.getLocations());
        }

        for (LocationIdentity key : keys)
        {
            int mergedStatesCount = 0;
            boolean isPhi = false;
            MemoryNode merged = null;
            for (MemoryMap state : states)
            {
                MemoryNode last = state.getLastLocationAccess(key);
                if (isPhi)
                {
                    ((MemoryPhiNode) merged).addInput(ValueNodeUtil.asNode(last));
                }
                else
                {
                    if (merged == last)
                    {
                        // nothing to do
                    }
                    else if (merged == null)
                    {
                        merged = last;
                    }
                    else
                    {
                        MemoryPhiNode phi = merge.graph().addWithoutUnique(new MemoryPhiNode(merge, key));
                        for (int j = 0; j < mergedStatesCount; j++)
                        {
                            phi.addInput(ValueNodeUtil.asNode(merged));
                        }
                        phi.addInput(ValueNodeUtil.asNode(last));
                        merged = phi;
                        isPhi = true;
                    }
                }
                mergedStatesCount++;
            }
            newState.lastMemorySnapshot.put(key, merged);
        }
        return newState;
    }

    public static class FloatingReadClosure extends NodeIteratorClosure<MemoryMapImpl>
    {
        private final EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> modifiedInLoops;
        private boolean createFloatingReads;
        private boolean createMemoryMapNodes;

        public FloatingReadClosure(EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> modifiedInLoops, boolean createFloatingReads, boolean createMemoryMapNodes)
        {
            this.modifiedInLoops = modifiedInLoops;
            this.createFloatingReads = createFloatingReads;
            this.createMemoryMapNodes = createMemoryMapNodes;
        }

        @Override
        protected MemoryMapImpl processNode(FixedNode node, MemoryMapImpl state)
        {
            if (node instanceof MemoryAnchorNode)
            {
                processAnchor((MemoryAnchorNode) node, state);
                return state;
            }

            if (node instanceof MemoryAccess)
            {
                processAccess((MemoryAccess) node, state);
            }

            if (createFloatingReads & node instanceof FloatableAccessNode)
            {
                processFloatable((FloatableAccessNode) node, state);
            }
            else if (node instanceof MemoryCheckpoint.Single)
            {
                processCheckpoint((MemoryCheckpoint.Single) node, state);
            }
            else if (node instanceof MemoryCheckpoint.Multi)
            {
                processCheckpoint((MemoryCheckpoint.Multi) node, state);
            }

            if (createMemoryMapNodes && node instanceof ReturnNode)
            {
                ((ReturnNode) node).setMemoryMap(node.graph().unique(new MemoryMapNode(state.lastMemorySnapshot)));
            }
            return state;
        }

        /**
         * Improve the memory graph by re-wiring all usages of a {@link MemoryAnchorNode} to the
         * real last access location.
         */
        private static void processAnchor(MemoryAnchorNode anchor, MemoryMapImpl state)
        {
            for (Node node : anchor.usages().snapshot())
            {
                if (node instanceof MemoryAccess)
                {
                    MemoryAccess access = (MemoryAccess) node;
                    if (access.getLastLocationAccess() == anchor)
                    {
                        MemoryNode lastLocationAccess = state.getLastLocationAccess(access.getLocationIdentity());
                        access.setLastLocationAccess(lastLocationAccess);
                    }
                }
            }

            if (anchor.hasNoUsages())
            {
                anchor.graph().removeFixed(anchor);
            }
        }

        private static void processAccess(MemoryAccess access, MemoryMapImpl state)
        {
            LocationIdentity locationIdentity = access.getLocationIdentity();
            if (!locationIdentity.equals(LocationIdentity.any()))
            {
                MemoryNode lastLocationAccess = state.getLastLocationAccess(locationIdentity);
                access.setLastLocationAccess(lastLocationAccess);
            }
        }

        private static void processCheckpoint(MemoryCheckpoint.Single checkpoint, MemoryMapImpl state)
        {
            processIdentity(checkpoint.getLocationIdentity(), checkpoint, state);
        }

        private static void processCheckpoint(MemoryCheckpoint.Multi checkpoint, MemoryMapImpl state)
        {
            for (LocationIdentity identity : checkpoint.getLocationIdentities())
            {
                processIdentity(identity, checkpoint, state);
            }
        }

        private static void processIdentity(LocationIdentity identity, MemoryCheckpoint checkpoint, MemoryMapImpl state)
        {
            if (identity.isAny())
            {
                state.lastMemorySnapshot.clear();
            }
            if (identity.isMutable())
            {
                state.lastMemorySnapshot.put(identity, checkpoint);
            }
        }

        private static void processFloatable(FloatableAccessNode accessNode, MemoryMapImpl state)
        {
            StructuredGraph graph = accessNode.graph();
            LocationIdentity locationIdentity = accessNode.getLocationIdentity();
            if (accessNode.canFloat())
            {
                MemoryNode lastLocationAccess = state.getLastLocationAccess(locationIdentity);
                FloatingAccessNode floatingNode = accessNode.asFloatingNode(lastLocationAccess);
                graph.replaceFixedWithFloating(accessNode, floatingNode);
            }
        }

        @Override
        protected MemoryMapImpl merge(AbstractMergeNode merge, List<MemoryMapImpl> states)
        {
            return mergeMemoryMaps(merge, states);
        }

        @Override
        protected MemoryMapImpl afterSplit(AbstractBeginNode node, MemoryMapImpl oldState)
        {
            MemoryMapImpl result = new MemoryMapImpl(oldState);
            if (node.predecessor() instanceof InvokeWithExceptionNode)
            {
                /*
                 * InvokeWithException cannot be the lastLocationAccess for a FloatingReadNode.
                 * Since it is both the invoke and a control flow split, the scheduler cannot
                 * schedule anything immediately after the invoke. It can only schedule in the
                 * normal or exceptional successor - and we have to tell the scheduler here which
                 * side it needs to choose by putting in the location identity on both successors.
                 */
                InvokeWithExceptionNode invoke = (InvokeWithExceptionNode) node.predecessor();
                result.lastMemorySnapshot.put(invoke.getLocationIdentity(), (MemoryCheckpoint) node);
            }
            return result;
        }

        @Override
        protected EconomicMap<LoopExitNode, MemoryMapImpl> processLoop(LoopBeginNode loop, MemoryMapImpl initialState)
        {
            EconomicSet<LocationIdentity> modifiedLocations = modifiedInLoops.get(loop);
            EconomicMap<LocationIdentity, MemoryPhiNode> phis = EconomicMap.create(Equivalence.DEFAULT);
            if (modifiedLocations.contains(LocationIdentity.any()))
            {
                // create phis for all locations if ANY is modified in the loop
                modifiedLocations = EconomicSet.create(Equivalence.DEFAULT, modifiedLocations);
                modifiedLocations.addAll(initialState.lastMemorySnapshot.getKeys());
            }

            for (LocationIdentity location : modifiedLocations)
            {
                createMemoryPhi(loop, initialState, phis, location);
            }
            initialState.lastMemorySnapshot.putAll(phis);

            LoopInfo<MemoryMapImpl> loopInfo = ReentrantNodeIterator.processLoop(this, loop, initialState);

            UnmodifiableMapCursor<LoopEndNode, MemoryMapImpl> endStateCursor = loopInfo.endStates.getEntries();
            while (endStateCursor.advance())
            {
                int endIndex = loop.phiPredecessorIndex(endStateCursor.getKey());
                UnmodifiableMapCursor<LocationIdentity, MemoryPhiNode> phiCursor = phis.getEntries();
                while (phiCursor.advance())
                {
                    LocationIdentity key = phiCursor.getKey();
                    PhiNode phi = phiCursor.getValue();
                    phi.initializeValueAt(endIndex, ValueNodeUtil.asNode(endStateCursor.getValue().getLastLocationAccess(key)));
                }
            }
            return loopInfo.exitStates;
        }

        private static void createMemoryPhi(LoopBeginNode loop, MemoryMapImpl initialState, EconomicMap<LocationIdentity, MemoryPhiNode> phis, LocationIdentity location)
        {
            MemoryPhiNode phi = loop.graph().addWithoutUnique(new MemoryPhiNode(loop, location));
            phi.addInput(ValueNodeUtil.asNode(initialState.getLastLocationAccess(location)));
            phis.put(location, phi);
        }
    }
}
