package giraaff.virtual.phases.ea;

import java.util.Iterator;
import java.util.List;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.Loop;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.nodes.FieldLocationIdentity;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.ValueProxyNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.extended.GuardedNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.extended.RawStoreNode;
import giraaff.nodes.extended.UnsafeAccessNode;
import giraaff.nodes.java.AccessFieldNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.java.StoreFieldNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.WriteNode;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.virtual.phases.ea.ReadEliminationBlockState.CacheEntry;
import giraaff.virtual.phases.ea.ReadEliminationBlockState.LoadCacheEntry;
import giraaff.virtual.phases.ea.ReadEliminationBlockState.UnsafeLoadCacheEntry;

/**
 * This closure initially handled a set of nodes that is disjunct from {@link PEReadEliminationClosure},
 * but over time both have evolved so that there's a significant overlap.
 */
// @class ReadEliminationClosure
public final class ReadEliminationClosure extends EffectsClosure<ReadEliminationBlockState>
{
    private final boolean considerGuards;

    // @cons
    public ReadEliminationClosure(ControlFlowGraph cfg, boolean considerGuards)
    {
        super(null, cfg);
        this.considerGuards = considerGuards;
    }

    @Override
    protected ReadEliminationBlockState getInitialState()
    {
        return new ReadEliminationBlockState();
    }

    @Override
    protected boolean processNode(Node node, ReadEliminationBlockState state, GraphEffectList effects, FixedWithNextNode lastFixedNode)
    {
        boolean deleted = false;
        if (node instanceof AccessFieldNode)
        {
            AccessFieldNode access = (AccessFieldNode) node;
            if (access.isVolatile())
            {
                processIdentity(state, LocationIdentity.any());
            }
            else
            {
                ValueNode object = GraphUtil.unproxify(access.object());
                LoadCacheEntry identifier = new LoadCacheEntry(object, new FieldLocationIdentity(access.field()));
                ValueNode cachedValue = state.getCacheEntry(identifier);
                if (node instanceof LoadFieldNode)
                {
                    if (cachedValue != null && access.stamp(NodeView.DEFAULT).isCompatible(cachedValue.stamp(NodeView.DEFAULT)))
                    {
                        effects.replaceAtUsages(access, cachedValue, access);
                        addScalarAlias(access, cachedValue);
                        deleted = true;
                    }
                    else
                    {
                        state.addCacheEntry(identifier, access);
                    }
                }
                else
                {
                    StoreFieldNode store = (StoreFieldNode) node;
                    ValueNode value = getScalarAlias(store.value());
                    if (GraphUtil.unproxify(value) == GraphUtil.unproxify(cachedValue))
                    {
                        effects.deleteNode(store);
                        deleted = true;
                    }
                    state.killReadCache(identifier.identity);
                    state.addCacheEntry(identifier, value);
                }
            }
        }
        else if (node instanceof ReadNode)
        {
            ReadNode read = (ReadNode) node;
            if (read.getLocationIdentity().isSingle())
            {
                ValueNode object = GraphUtil.unproxify(read.getAddress());
                LoadCacheEntry identifier = new LoadCacheEntry(object, read.getLocationIdentity());
                ValueNode cachedValue = state.getCacheEntry(identifier);
                if (cachedValue != null && areValuesReplaceable(read, cachedValue, considerGuards))
                {
                    effects.replaceAtUsages(read, cachedValue, read);
                    addScalarAlias(read, cachedValue);
                    deleted = true;
                }
                else
                {
                    state.addCacheEntry(identifier, read);
                }
            }
        }
        else if (node instanceof WriteNode)
        {
            WriteNode write = (WriteNode) node;
            if (write.getLocationIdentity().isSingle())
            {
                ValueNode object = GraphUtil.unproxify(write.getAddress());
                LoadCacheEntry identifier = new LoadCacheEntry(object, write.getLocationIdentity());
                ValueNode cachedValue = state.getCacheEntry(identifier);

                ValueNode value = getScalarAlias(write.value());
                if (GraphUtil.unproxify(value) == GraphUtil.unproxify(cachedValue))
                {
                    effects.deleteNode(write);
                    deleted = true;
                }
                processIdentity(state, write.getLocationIdentity());
                state.addCacheEntry(identifier, value);
            }
            else
            {
                processIdentity(state, write.getLocationIdentity());
            }
        }
        else if (node instanceof UnsafeAccessNode)
        {
            ResolvedJavaType type = StampTool.typeOrNull(((UnsafeAccessNode) node).object());
            if (type != null && !type.isArray())
            {
                if (node instanceof RawLoadNode)
                {
                    RawLoadNode load = (RawLoadNode) node;
                    if (load.getLocationIdentity().isSingle())
                    {
                        ValueNode object = GraphUtil.unproxify(load.object());
                        UnsafeLoadCacheEntry identifier = new UnsafeLoadCacheEntry(object, load.offset(), load.getLocationIdentity());
                        ValueNode cachedValue = state.getCacheEntry(identifier);
                        if (cachedValue != null && areValuesReplaceable(load, cachedValue, considerGuards))
                        {
                            effects.replaceAtUsages(load, cachedValue, load);
                            addScalarAlias(load, cachedValue);
                            deleted = true;
                        }
                        else
                        {
                            state.addCacheEntry(identifier, load);
                        }
                    }
                }
                else
                {
                    RawStoreNode write = (RawStoreNode) node;
                    if (write.getLocationIdentity().isSingle())
                    {
                        ValueNode object = GraphUtil.unproxify(write.object());
                        UnsafeLoadCacheEntry identifier = new UnsafeLoadCacheEntry(object, write.offset(), write.getLocationIdentity());
                        ValueNode cachedValue = state.getCacheEntry(identifier);

                        ValueNode value = getScalarAlias(write.value());
                        if (GraphUtil.unproxify(value) == GraphUtil.unproxify(cachedValue))
                        {
                            effects.deleteNode(write);
                            deleted = true;
                        }
                        processIdentity(state, write.getLocationIdentity());
                        state.addCacheEntry(identifier, value);
                    }
                    else
                    {
                        processIdentity(state, write.getLocationIdentity());
                    }
                }
            }
        }
        else if (node instanceof MemoryCheckpoint.Single)
        {
            LocationIdentity identity = ((MemoryCheckpoint.Single) node).getLocationIdentity();
            processIdentity(state, identity);
        }
        else if (node instanceof MemoryCheckpoint.Multi)
        {
            for (LocationIdentity identity : ((MemoryCheckpoint.Multi) node).getLocationIdentities())
            {
                processIdentity(state, identity);
            }
        }
        return deleted;
    }

    private static boolean areValuesReplaceable(ValueNode originalValue, ValueNode replacementValue, boolean considerGuards)
    {
        return originalValue.stamp(NodeView.DEFAULT).isCompatible(replacementValue.stamp(NodeView.DEFAULT)) && (!considerGuards || (getGuard(originalValue) == null || getGuard(originalValue) == getGuard(replacementValue)));
    }

    private static GuardingNode getGuard(ValueNode node)
    {
        if (node instanceof GuardedNode)
        {
            GuardedNode guardedNode = (GuardedNode) node;
            return guardedNode.getGuard();
        }
        return null;
    }

    private static void processIdentity(ReadEliminationBlockState state, LocationIdentity identity)
    {
        if (identity.isAny())
        {
            state.killReadCache();
            return;
        }
        state.killReadCache(identity);
    }

    @Override
    protected void processLoopExit(LoopExitNode exitNode, ReadEliminationBlockState initialState, ReadEliminationBlockState exitState, GraphEffectList effects)
    {
        if (exitNode.graph().hasValueProxies())
        {
            MapCursor<CacheEntry<?>, ValueNode> entry = exitState.getReadCache().getEntries();
            while (entry.advance())
            {
                if (initialState.getReadCache().get(entry.getKey()) != entry.getValue())
                {
                    ProxyNode proxy = new ValueProxyNode(exitState.getCacheEntry(entry.getKey()), exitNode);
                    effects.addFloatingNode(proxy, "readCacheProxy");
                    exitState.getReadCache().put(entry.getKey(), proxy);
                }
            }
        }
    }

    @Override
    protected ReadEliminationBlockState cloneState(ReadEliminationBlockState other)
    {
        return new ReadEliminationBlockState(other);
    }

    @Override
    protected MergeProcessor createMergeProcessor(Block merge)
    {
        return new ReadEliminationMergeProcessor(merge);
    }

    // @class ReadEliminationClosure.ReadEliminationMergeProcessor
    // @closure
    private final class ReadEliminationMergeProcessor extends EffectsClosure<ReadEliminationBlockState>.MergeProcessor
    {
        private final EconomicMap<Object, ValuePhiNode> materializedPhis = EconomicMap.create(Equivalence.DEFAULT);

        // @cons
        ReadEliminationMergeProcessor(Block mergeBlock)
        {
            super(mergeBlock);
        }

        protected ValuePhiNode getCachedPhi(CacheEntry<?> virtual, Stamp stamp)
        {
            ValuePhiNode result = materializedPhis.get(virtual);
            if (result == null)
            {
                result = createValuePhi(stamp);
                materializedPhis.put(virtual, result);
            }
            return result;
        }

        @Override
        protected void merge(List<ReadEliminationBlockState> states)
        {
            MapCursor<CacheEntry<?>, ValueNode> cursor = states.get(0).readCache.getEntries();
            while (cursor.advance())
            {
                CacheEntry<?> key = cursor.getKey();
                ValueNode value = cursor.getValue();
                boolean phi = false;
                for (int i = 1; i < states.size(); i++)
                {
                    ValueNode otherValue = states.get(i).readCache.get(key);
                    // E.g. unsafe loads/stores with different access kinds have different stamps although location,
                    // object and offset are the same. In this case we cannot create a phi nor can we set a common value.
                    if (otherValue == null || !value.stamp(NodeView.DEFAULT).isCompatible(otherValue.stamp(NodeView.DEFAULT)))
                    {
                        value = null;
                        phi = false;
                        break;
                    }
                    if (!phi && otherValue != value)
                    {
                        phi = true;
                    }
                }
                if (phi)
                {
                    PhiNode phiNode = getCachedPhi(key, value.stamp(NodeView.DEFAULT).unrestricted());
                    mergeEffects.addFloatingNode(phiNode, "mergeReadCache");
                    for (int i = 0; i < states.size(); i++)
                    {
                        ValueNode v = states.get(i).getCacheEntry(key);
                        setPhiInput(phiNode, i, v);
                    }
                    newState.addCacheEntry(key, phiNode);
                }
                else if (value != null)
                {
                    // case that there is the same value on all branches
                    newState.addCacheEntry(key, value);
                }
            }
            // For object phis, see if there are known reads on all predecessors, for which we could create new phis.
            for (PhiNode phi : getPhis())
            {
                if (phi.getStackKind() == JavaKind.Object)
                {
                    for (CacheEntry<?> entry : states.get(0).readCache.getKeys())
                    {
                        if (entry.object == getPhiValueAt(phi, 0))
                        {
                            mergeReadCachePhi(phi, entry, states);
                        }
                    }
                }
            }
        }

        private void mergeReadCachePhi(PhiNode phi, CacheEntry<?> identifier, List<ReadEliminationBlockState> states)
        {
            ValueNode[] values = new ValueNode[states.size()];
            values[0] = states.get(0).getCacheEntry(identifier.duplicateWithObject(getPhiValueAt(phi, 0)));
            if (values[0] != null)
            {
                for (int i = 1; i < states.size(); i++)
                {
                    ValueNode value = states.get(i).getCacheEntry(identifier.duplicateWithObject(getPhiValueAt(phi, i)));
                    // e.g. unsafe loads/stores with same identity, but different access kinds, see mergeReadCache(states)
                    if (value == null || !values[i - 1].stamp(NodeView.DEFAULT).isCompatible(value.stamp(NodeView.DEFAULT)))
                    {
                        return;
                    }
                    values[i] = value;
                }

                CacheEntry<?> newIdentifier = identifier.duplicateWithObject(phi);
                PhiNode phiNode = getCachedPhi(newIdentifier, values[0].stamp(NodeView.DEFAULT).unrestricted());
                mergeEffects.addFloatingNode(phiNode, "mergeReadCachePhi");
                for (int i = 0; i < values.length; i++)
                {
                    setPhiInput(phiNode, i, values[i]);
                }
                newState.addCacheEntry(newIdentifier, phiNode);
            }
        }
    }

    @Override
    protected void processKilledLoopLocations(Loop<Block> loop, ReadEliminationBlockState initialState, ReadEliminationBlockState mergedStates)
    {
        if (initialState.readCache.size() > 0)
        {
            LoopKillCache loopKilledLocations = loopLocationKillCache.get(loop);
            // we have fully processed this loop the first time, remember to cache it the next time it is visited
            if (loopKilledLocations == null)
            {
                loopKilledLocations = new LoopKillCache(1/* 1.visit */);
                loopLocationKillCache.put(loop, loopKilledLocations);
            }
            else
            {
                if (loopKilledLocations.visits() > GraalOptions.readEliminationMaxLoopVisits)
                {
                    // we have processed the loop too many times: kill all locations, so
                    // the inner loop will never be processed more than once again on visit
                    loopKilledLocations.setKillsAll();
                }
                else
                {
                    // we have fully processed this loop >1 times, update the killed locations
                    EconomicSet<LocationIdentity> forwardEndLiveLocations = EconomicSet.create(Equivalence.DEFAULT);
                    for (CacheEntry<?> entry : initialState.readCache.getKeys())
                    {
                        forwardEndLiveLocations.add(entry.getIdentity());
                    }
                    for (CacheEntry<?> entry : mergedStates.readCache.getKeys())
                    {
                        forwardEndLiveLocations.remove(entry.getIdentity());
                    }
                    // every location that is alive before the loop but not after is killed by the loop
                    for (LocationIdentity location : forwardEndLiveLocations)
                    {
                        loopKilledLocations.rememberLoopKilledLocation(location);
                    }
                }
                // remember the loop visit
                loopKilledLocations.visited();
            }
        }
    }

    @Override
    protected ReadEliminationBlockState stripKilledLoopLocations(Loop<Block> loop, ReadEliminationBlockState originalInitialState)
    {
        ReadEliminationBlockState initialState = super.stripKilledLoopLocations(loop, originalInitialState);
        LoopKillCache loopKilledLocations = loopLocationKillCache.get(loop);
        if (loopKilledLocations != null && loopKilledLocations.loopKillsLocations())
        {
            Iterator<CacheEntry<?>> it = initialState.readCache.getKeys().iterator();
            while (it.hasNext())
            {
                CacheEntry<?> entry = it.next();
                if (loopKilledLocations.containsLocation(entry.getIdentity()))
                {
                    it.remove();
                }
            }
        }
        return initialState;
    }
}
