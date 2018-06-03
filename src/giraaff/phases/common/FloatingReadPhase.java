package giraaff.phases.common;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableMapCursor;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.cfg.Loop;
import giraaff.graph.Graph.NodeEvent;
import giraaff.graph.Graph.NodeEventScope;
import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNodeUtil;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.cfg.HIRLoop;
import giraaff.nodes.memory.FloatableAccessNode;
import giraaff.nodes.memory.FloatingAccessNode;
import giraaff.nodes.memory.FloatingReadNode;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryAnchorNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.MemoryMap;
import giraaff.nodes.memory.MemoryMapNode;
import giraaff.nodes.memory.MemoryNode;
import giraaff.nodes.memory.MemoryPhiNode;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.graph.ReentrantNodeIterator;
import giraaff.phases.graph.ReentrantNodeIterator.LoopInfo;
import giraaff.phases.graph.ReentrantNodeIterator.NodeIteratorClosure;

// @class FloatingReadPhase
public final class FloatingReadPhase extends Phase
{
    // @field
    private boolean createFloatingReads;
    // @field
    private boolean createMemoryMapNodes;

    // @class FloatingReadPhase.MemoryMapImpl
    public static final class MemoryMapImpl implements MemoryMap
    {
        // @field
        private final EconomicMap<LocationIdentity, MemoryNode> lastMemorySnapshot;

        // @cons
        public MemoryMapImpl(MemoryMapImpl __memoryMap)
        {
            super();
            lastMemorySnapshot = EconomicMap.create(Equivalence.DEFAULT, __memoryMap.lastMemorySnapshot);
        }

        // @cons
        public MemoryMapImpl(StartNode __start)
        {
            this();
            lastMemorySnapshot.put(LocationIdentity.any(), __start);
        }

        // @cons
        public MemoryMapImpl()
        {
            super();
            lastMemorySnapshot = EconomicMap.create(Equivalence.DEFAULT);
        }

        @Override
        public MemoryNode getLastLocationAccess(LocationIdentity __locationIdentity)
        {
            MemoryNode __lastLocationAccess;
            if (__locationIdentity.isImmutable())
            {
                return null;
            }
            else
            {
                __lastLocationAccess = lastMemorySnapshot.get(__locationIdentity);
                if (__lastLocationAccess == null)
                {
                    __lastLocationAccess = lastMemorySnapshot.get(LocationIdentity.any());
                }
                return __lastLocationAccess;
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

    // @cons
    public FloatingReadPhase()
    {
        this(true, false);
    }

    /**
     * @param createFloatingReads specifies whether {@link FloatableAccessNode}s like {@link ReadNode}
     *            should be converted into floating nodes (e.g. {@link FloatingReadNode}s) where possible
     * @param createMemoryMapNodes a {@link MemoryMapNode} will be created for each return if this is true
     */
    // @cons
    public FloatingReadPhase(boolean __createFloatingReads, boolean __createMemoryMapNodes)
    {
        super();
        this.createFloatingReads = __createFloatingReads;
        this.createMemoryMapNodes = __createMemoryMapNodes;
    }

    /**
     * Removes nodes from a given set that (transitively) have a usage outside the set.
     */
    private static EconomicSet<Node> removeExternallyUsedNodes(EconomicSet<Node> __set)
    {
        boolean __change;
        do
        {
            __change = false;
            for (Iterator<Node> __iter = __set.iterator(); __iter.hasNext(); )
            {
                Node __node = __iter.next();
                for (Node __usage : __node.usages())
                {
                    if (!__set.contains(__usage))
                    {
                        __change = true;
                        __iter.remove();
                        break;
                    }
                }
            }
        } while (__change);
        return __set;
    }

    protected void processNode(FixedNode __node, EconomicSet<LocationIdentity> __currentState)
    {
        if (__node instanceof MemoryCheckpoint.Single)
        {
            processIdentity(__currentState, ((MemoryCheckpoint.Single) __node).getLocationIdentity());
        }
        else if (__node instanceof MemoryCheckpoint.Multi)
        {
            for (LocationIdentity __identity : ((MemoryCheckpoint.Multi) __node).getLocationIdentities())
            {
                processIdentity(__currentState, __identity);
            }
        }
    }

    private static void processIdentity(EconomicSet<LocationIdentity> __currentState, LocationIdentity __identity)
    {
        if (__identity.isMutable())
        {
            __currentState.add(__identity);
        }
    }

    protected void processBlock(Block __b, EconomicSet<LocationIdentity> __currentState)
    {
        for (FixedNode __n : __b.getNodes())
        {
            processNode(__n, __currentState);
        }
    }

    private EconomicSet<LocationIdentity> processLoop(HIRLoop __loop, EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> __modifiedInLoops)
    {
        LoopBeginNode __loopBegin = (LoopBeginNode) __loop.getHeader().getBeginNode();
        EconomicSet<LocationIdentity> __result = __modifiedInLoops.get(__loopBegin);
        if (__result != null)
        {
            return __result;
        }

        __result = EconomicSet.create(Equivalence.DEFAULT);
        for (Loop<Block> __inner : __loop.getChildren())
        {
            __result.addAll(processLoop((HIRLoop) __inner, __modifiedInLoops));
        }

        for (Block __b : __loop.getBlocks())
        {
            if (__b.getLoop() == __loop)
            {
                processBlock(__b, __result);
            }
        }

        __modifiedInLoops.put(__loopBegin, __result);
        return __result;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph __graph)
    {
        EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> __modifiedInLoops = null;
        if (__graph.hasLoops())
        {
            __modifiedInLoops = EconomicMap.create(Equivalence.IDENTITY);
            ControlFlowGraph __cfg = ControlFlowGraph.compute(__graph, true, true, false, false);
            for (Loop<?> __l : __cfg.getLoops())
            {
                HIRLoop __loop = (HIRLoop) __l;
                processLoop(__loop, __modifiedInLoops);
            }
        }

        HashSetNodeEventListener __listener = new HashSetNodeEventListener(EnumSet.of(NodeEvent.NODE_ADDED, NodeEvent.ZERO_USAGES));
        try (NodeEventScope __nes = __graph.trackNodeEvents(__listener))
        {
            ReentrantNodeIterator.apply(new FloatingReadClosure(__modifiedInLoops, createFloatingReads, createMemoryMapNodes), __graph.start(), new MemoryMapImpl(__graph.start()));
        }

        for (Node __n : removeExternallyUsedNodes(__listener.getNodes()))
        {
            if (__n.isAlive() && __n instanceof FloatingNode)
            {
                __n.replaceAtUsages(null);
                GraphUtil.killWithUnusedFloatingInputs(__n);
            }
        }
        if (createFloatingReads)
        {
            __graph.setAfterFloatingReadPhase(true);
        }
    }

    public static MemoryMapImpl mergeMemoryMaps(AbstractMergeNode __merge, List<? extends MemoryMap> __states)
    {
        MemoryMapImpl __newState = new MemoryMapImpl();

        EconomicSet<LocationIdentity> __keys = EconomicSet.create(Equivalence.DEFAULT);
        for (MemoryMap __other : __states)
        {
            __keys.addAll(__other.getLocations());
        }

        for (LocationIdentity __key : __keys)
        {
            int __mergedStatesCount = 0;
            boolean __isPhi = false;
            MemoryNode __merged = null;
            for (MemoryMap __state : __states)
            {
                MemoryNode __last = __state.getLastLocationAccess(__key);
                if (__isPhi)
                {
                    ((MemoryPhiNode) __merged).addInput(ValueNodeUtil.asNode(__last));
                }
                else
                {
                    if (__merged == __last)
                    {
                        // nothing to do
                    }
                    else if (__merged == null)
                    {
                        __merged = __last;
                    }
                    else
                    {
                        MemoryPhiNode __phi = __merge.graph().addWithoutUnique(new MemoryPhiNode(__merge, __key));
                        for (int __j = 0; __j < __mergedStatesCount; __j++)
                        {
                            __phi.addInput(ValueNodeUtil.asNode(__merged));
                        }
                        __phi.addInput(ValueNodeUtil.asNode(__last));
                        __merged = __phi;
                        __isPhi = true;
                    }
                }
                __mergedStatesCount++;
            }
            __newState.lastMemorySnapshot.put(__key, __merged);
        }
        return __newState;
    }

    // @class FloatingReadPhase.FloatingReadClosure
    public static final class FloatingReadClosure extends NodeIteratorClosure<MemoryMapImpl>
    {
        // @field
        private final EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> modifiedInLoops;
        // @field
        private boolean createFloatingReads;
        // @field
        private boolean createMemoryMapNodes;

        // @cons
        public FloatingReadClosure(EconomicMap<LoopBeginNode, EconomicSet<LocationIdentity>> __modifiedInLoops, boolean __createFloatingReads, boolean __createMemoryMapNodes)
        {
            super();
            this.modifiedInLoops = __modifiedInLoops;
            this.createFloatingReads = __createFloatingReads;
            this.createMemoryMapNodes = __createMemoryMapNodes;
        }

        @Override
        protected MemoryMapImpl processNode(FixedNode __node, MemoryMapImpl __state)
        {
            if (__node instanceof MemoryAnchorNode)
            {
                processAnchor((MemoryAnchorNode) __node, __state);
                return __state;
            }

            if (__node instanceof MemoryAccess)
            {
                processAccess((MemoryAccess) __node, __state);
            }

            if (createFloatingReads & __node instanceof FloatableAccessNode)
            {
                processFloatable((FloatableAccessNode) __node, __state);
            }
            else if (__node instanceof MemoryCheckpoint.Single)
            {
                processCheckpoint((MemoryCheckpoint.Single) __node, __state);
            }
            else if (__node instanceof MemoryCheckpoint.Multi)
            {
                processCheckpoint((MemoryCheckpoint.Multi) __node, __state);
            }

            if (createMemoryMapNodes && __node instanceof ReturnNode)
            {
                ((ReturnNode) __node).setMemoryMap(__node.graph().unique(new MemoryMapNode(__state.lastMemorySnapshot)));
            }
            return __state;
        }

        /**
         * Improve the memory graph by re-wiring all usages of a {@link MemoryAnchorNode} to the
         * real last access location.
         */
        private static void processAnchor(MemoryAnchorNode __anchor, MemoryMapImpl __state)
        {
            for (Node __node : __anchor.usages().snapshot())
            {
                if (__node instanceof MemoryAccess)
                {
                    MemoryAccess __access = (MemoryAccess) __node;
                    if (__access.getLastLocationAccess() == __anchor)
                    {
                        MemoryNode __lastLocationAccess = __state.getLastLocationAccess(__access.getLocationIdentity());
                        __access.setLastLocationAccess(__lastLocationAccess);
                    }
                }
            }

            if (__anchor.hasNoUsages())
            {
                __anchor.graph().removeFixed(__anchor);
            }
        }

        private static void processAccess(MemoryAccess __access, MemoryMapImpl __state)
        {
            LocationIdentity __locationIdentity = __access.getLocationIdentity();
            if (!__locationIdentity.equals(LocationIdentity.any()))
            {
                MemoryNode __lastLocationAccess = __state.getLastLocationAccess(__locationIdentity);
                __access.setLastLocationAccess(__lastLocationAccess);
            }
        }

        private static void processCheckpoint(MemoryCheckpoint.Single __checkpoint, MemoryMapImpl __state)
        {
            processIdentity(__checkpoint.getLocationIdentity(), __checkpoint, __state);
        }

        private static void processCheckpoint(MemoryCheckpoint.Multi __checkpoint, MemoryMapImpl __state)
        {
            for (LocationIdentity __identity : __checkpoint.getLocationIdentities())
            {
                processIdentity(__identity, __checkpoint, __state);
            }
        }

        private static void processIdentity(LocationIdentity __identity, MemoryCheckpoint __checkpoint, MemoryMapImpl __state)
        {
            if (__identity.isAny())
            {
                __state.lastMemorySnapshot.clear();
            }
            if (__identity.isMutable())
            {
                __state.lastMemorySnapshot.put(__identity, __checkpoint);
            }
        }

        private static void processFloatable(FloatableAccessNode __accessNode, MemoryMapImpl __state)
        {
            StructuredGraph __graph = __accessNode.graph();
            LocationIdentity __locationIdentity = __accessNode.getLocationIdentity();
            if (__accessNode.canFloat())
            {
                MemoryNode __lastLocationAccess = __state.getLastLocationAccess(__locationIdentity);
                FloatingAccessNode __floatingNode = __accessNode.asFloatingNode(__lastLocationAccess);
                __graph.replaceFixedWithFloating(__accessNode, __floatingNode);
            }
        }

        @Override
        protected MemoryMapImpl merge(AbstractMergeNode __merge, List<MemoryMapImpl> __states)
        {
            return mergeMemoryMaps(__merge, __states);
        }

        @Override
        protected MemoryMapImpl afterSplit(AbstractBeginNode __node, MemoryMapImpl __oldState)
        {
            MemoryMapImpl __result = new MemoryMapImpl(__oldState);
            if (__node.predecessor() instanceof InvokeWithExceptionNode)
            {
                /*
                 * InvokeWithException cannot be the lastLocationAccess for a FloatingReadNode.
                 * Since it is both the invoke and a control flow split, the scheduler cannot
                 * schedule anything immediately after the invoke. It can only schedule in the
                 * normal or exceptional successor - and we have to tell the scheduler here which
                 * side it needs to choose by putting in the location identity on both successors.
                 */
                InvokeWithExceptionNode __invoke = (InvokeWithExceptionNode) __node.predecessor();
                __result.lastMemorySnapshot.put(__invoke.getLocationIdentity(), (MemoryCheckpoint) __node);
            }
            return __result;
        }

        @Override
        protected EconomicMap<LoopExitNode, MemoryMapImpl> processLoop(LoopBeginNode __loop, MemoryMapImpl __initialState)
        {
            EconomicSet<LocationIdentity> __modifiedLocations = modifiedInLoops.get(__loop);
            EconomicMap<LocationIdentity, MemoryPhiNode> __phis = EconomicMap.create(Equivalence.DEFAULT);
            if (__modifiedLocations.contains(LocationIdentity.any()))
            {
                // create phis for all locations if ANY is modified in the loop
                __modifiedLocations = EconomicSet.create(Equivalence.DEFAULT, __modifiedLocations);
                __modifiedLocations.addAll(__initialState.lastMemorySnapshot.getKeys());
            }

            for (LocationIdentity __location : __modifiedLocations)
            {
                createMemoryPhi(__loop, __initialState, __phis, __location);
            }
            __initialState.lastMemorySnapshot.putAll(__phis);

            LoopInfo<MemoryMapImpl> __loopInfo = ReentrantNodeIterator.processLoop(this, __loop, __initialState);

            UnmodifiableMapCursor<LoopEndNode, MemoryMapImpl> __endStateCursor = __loopInfo.endStates.getEntries();
            while (__endStateCursor.advance())
            {
                int __endIndex = __loop.phiPredecessorIndex(__endStateCursor.getKey());
                UnmodifiableMapCursor<LocationIdentity, MemoryPhiNode> __phiCursor = __phis.getEntries();
                while (__phiCursor.advance())
                {
                    LocationIdentity __key = __phiCursor.getKey();
                    PhiNode __phi = __phiCursor.getValue();
                    __phi.initializeValueAt(__endIndex, ValueNodeUtil.asNode(__endStateCursor.getValue().getLastLocationAccess(__key)));
                }
            }
            return __loopInfo.exitStates;
        }

        private static void createMemoryPhi(LoopBeginNode __loop, MemoryMapImpl __initialState, EconomicMap<LocationIdentity, MemoryPhiNode> __phis, LocationIdentity __location)
        {
            MemoryPhiNode __phi = __loop.graph().addWithoutUnique(new MemoryPhiNode(__loop, __location));
            __phi.addInput(ValueNodeUtil.asNode(__initialState.getLastLocationAccess(__location)));
            __phis.put(__location, __phi);
        }
    }
}
