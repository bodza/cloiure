package giraaff.virtual.phases.ea;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import org.graalvm.collections.Pair;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.Loop;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.FieldLocationIdentity;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueProxyNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.extended.RawStoreNode;
import giraaff.nodes.extended.UnboxNode;
import giraaff.nodes.java.ArrayLengthNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.java.StoreFieldNode;
import giraaff.nodes.java.StoreIndexedNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.VirtualArrayNode;
import giraaff.options.OptionValues;
import giraaff.virtual.phases.ea.PEReadEliminationBlockState.ReadCacheEntry;

public final class PEReadEliminationClosure extends PartialEscapeClosure<PEReadEliminationBlockState>
{
    private static final EnumMap<JavaKind, LocationIdentity> UNBOX_LOCATIONS;

    static
    {
        UNBOX_LOCATIONS = new EnumMap<>(JavaKind.class);
        for (JavaKind kind : JavaKind.values())
        {
            UNBOX_LOCATIONS.put(kind, NamedLocationIdentity.immutable("PEA unbox " + kind.getJavaName()));
        }
    }

    public PEReadEliminationClosure(ScheduleResult schedule, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, LoweringProvider loweringProvider)
    {
        super(schedule, metaAccess, constantReflection, constantFieldProvider, loweringProvider);
    }

    @Override
    protected PEReadEliminationBlockState getInitialState()
    {
        return new PEReadEliminationBlockState(tool.getOptions());
    }

    @Override
    protected boolean processNode(Node node, PEReadEliminationBlockState state, GraphEffectList effects, FixedWithNextNode lastFixedNode)
    {
        if (super.processNode(node, state, effects, lastFixedNode))
        {
            return true;
        }

        if (node instanceof LoadFieldNode)
        {
            return processLoadField((LoadFieldNode) node, state, effects);
        }
        else if (node instanceof StoreFieldNode)
        {
            return processStoreField((StoreFieldNode) node, state, effects);
        }
        else if (node instanceof LoadIndexedNode)
        {
            return processLoadIndexed((LoadIndexedNode) node, state, effects);
        }
        else if (node instanceof StoreIndexedNode)
        {
            return processStoreIndexed((StoreIndexedNode) node, state, effects);
        }
        else if (node instanceof ArrayLengthNode)
        {
            return processArrayLength((ArrayLengthNode) node, state, effects);
        }
        else if (node instanceof UnboxNode)
        {
            return processUnbox((UnboxNode) node, state, effects);
        }
        else if (node instanceof RawLoadNode)
        {
            return processUnsafeLoad((RawLoadNode) node, state, effects);
        }
        else if (node instanceof RawStoreNode)
        {
            return processUnsafeStore((RawStoreNode) node, state, effects);
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

        return false;
    }

    private boolean processStore(FixedNode store, ValueNode object, LocationIdentity identity, int index, JavaKind accessKind, boolean overflowAccess, ValueNode value, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        ValueNode unproxiedObject = GraphUtil.unproxify(object);
        ValueNode cachedValue = state.getReadCache(object, identity, index, accessKind, this);

        ValueNode finalValue = getScalarAlias(value);
        boolean result = false;
        if (GraphUtil.unproxify(finalValue) == GraphUtil.unproxify(cachedValue))
        {
            effects.deleteNode(store);
            result = true;
        }
        state.killReadCache(identity, index);
        state.addReadCache(unproxiedObject, identity, index, accessKind, overflowAccess, finalValue, this);
        return result;
    }

    private boolean processLoad(FixedNode load, ValueNode object, LocationIdentity identity, int index, JavaKind kind, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        ValueNode unproxiedObject = GraphUtil.unproxify(object);
        ValueNode cachedValue = state.getReadCache(unproxiedObject, identity, index, kind, this);
        if (cachedValue != null)
        {
            // perform the read elimination
            effects.replaceAtUsages(load, cachedValue, load);
            addScalarAlias(load, cachedValue);
            return true;
        }
        else
        {
            state.addReadCache(unproxiedObject, identity, index, kind, false, load, this);
            return false;
        }
    }

    private static boolean isOverflowAccess(JavaKind accessKind, JavaKind declaredKind)
    {
        if (accessKind == declaredKind)
        {
            return false;
        }
        if (accessKind == JavaKind.Object)
        {
            switch (declaredKind)
            {
                case Object:
                case Double:
                case Long:
                    return false;
                default:
                    return true;
            }
        }
        return declaredKind.isPrimitive() ? accessKind.getBitCount() > declaredKind.getBitCount() : true;
    }

    private boolean processUnsafeLoad(RawLoadNode load, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        if (load.offset().isConstant())
        {
            ResolvedJavaType type = StampTool.typeOrNull(load.object());
            if (type != null && type.isArray())
            {
                JavaKind accessKind = load.accessKind();
                JavaKind componentKind = type.getComponentType().getJavaKind();
                long offset = load.offset().asJavaConstant().asLong();
                int index = VirtualArrayNode.entryIndexForOffset(tool.getArrayOffsetProvider(), offset, accessKind, type.getComponentType(), Integer.MAX_VALUE);
                ValueNode object = GraphUtil.unproxify(load.object());
                LocationIdentity location = NamedLocationIdentity.getArrayLocation(componentKind);
                ValueNode cachedValue = state.getReadCache(object, location, index, accessKind, this);
                if (cachedValue != null)
                {
                    effects.replaceAtUsages(load, cachedValue, load);
                    addScalarAlias(load, cachedValue);
                    return true;
                }
                else
                {
                    state.addReadCache(object, location, index, accessKind, isOverflowAccess(accessKind, componentKind), load, this);
                }
            }
        }
        return false;
    }

    private boolean processUnsafeStore(RawStoreNode store, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        ResolvedJavaType type = StampTool.typeOrNull(store.object());
        if (type != null && type.isArray())
        {
            JavaKind accessKind = store.accessKind();
            JavaKind componentKind = type.getComponentType().getJavaKind();
            LocationIdentity location = NamedLocationIdentity.getArrayLocation(componentKind);
            if (store.offset().isConstant())
            {
                long offset = store.offset().asJavaConstant().asLong();
                boolean overflowAccess = isOverflowAccess(accessKind, componentKind);
                int index = overflowAccess ? -1 : VirtualArrayNode.entryIndexForOffset(tool.getArrayOffsetProvider(), offset, accessKind, type.getComponentType(), Integer.MAX_VALUE);
                return processStore(store, store.object(), location, index, accessKind, overflowAccess, store.value(), state, effects);
            }
            else
            {
                processIdentity(state, location);
            }
        }
        else
        {
            state.killReadCache();
        }
        return false;
    }

    private boolean processArrayLength(ArrayLengthNode length, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        return processLoad(length, length.array(), NamedLocationIdentity.ARRAY_LENGTH_LOCATION, -1, JavaKind.Int, state, effects);
    }

    private boolean processStoreField(StoreFieldNode store, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        if (store.isVolatile())
        {
            state.killReadCache();
            return false;
        }
        JavaKind kind = store.field().getJavaKind();
        return processStore(store, store.object(), new FieldLocationIdentity(store.field()), -1, kind, false, store.value(), state, effects);
    }

    private boolean processLoadField(LoadFieldNode load, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        if (load.isVolatile())
        {
            state.killReadCache();
            return false;
        }
        return processLoad(load, load.object(), new FieldLocationIdentity(load.field()), -1, load.field().getJavaKind(), state, effects);
    }

    private static JavaKind getElementKindFromStamp(ValueNode array)
    {
        ResolvedJavaType type = StampTool.typeOrNull(array);
        if (type != null && type.isArray())
        {
            return type.getComponentType().getJavaKind();
        }
        else
        {
            // it is likely an OSRLocal without valid stamp
            return JavaKind.Illegal;
        }
    }

    private boolean processStoreIndexed(StoreIndexedNode store, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        int index = store.index().isConstant() ? ((JavaConstant) store.index().asConstant()).asInt() : -1;
        // BASTORE (with elementKind being Byte) can be used to store values in boolean arrays.
        JavaKind elementKind = store.elementKind();
        if (elementKind == JavaKind.Byte)
        {
            elementKind = getElementKindFromStamp(store.array());
            if (elementKind == JavaKind.Illegal)
            {
                // Could not determine the actual access kind from stamp. Hence kill both.
                state.killReadCache(NamedLocationIdentity.getArrayLocation(JavaKind.Boolean), index);
                state.killReadCache(NamedLocationIdentity.getArrayLocation(JavaKind.Byte), index);
                return false;
            }
        }
        LocationIdentity arrayLocation = NamedLocationIdentity.getArrayLocation(elementKind);
        if (index != -1)
        {
            return processStore(store, store.array(), arrayLocation, index, elementKind, false, store.value(), state, effects);
        }
        else
        {
            state.killReadCache(arrayLocation, -1);
        }
        return false;
    }

    private boolean processLoadIndexed(LoadIndexedNode load, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        if (load.index().isConstant())
        {
            int index = ((JavaConstant) load.index().asConstant()).asInt();
            // BALOAD (with elementKind being Byte) can be used to retrieve values from boolean arrays.
            JavaKind elementKind = load.elementKind();
            if (elementKind == JavaKind.Byte)
            {
                elementKind = getElementKindFromStamp(load.array());
                if (elementKind == JavaKind.Illegal)
                {
                    return false;
                }
            }
            LocationIdentity arrayLocation = NamedLocationIdentity.getArrayLocation(elementKind);
            return processLoad(load, load.array(), arrayLocation, index, elementKind, state, effects);
        }
        return false;
    }

    private boolean processUnbox(UnboxNode unbox, PEReadEliminationBlockState state, GraphEffectList effects)
    {
        return processLoad(unbox, unbox.getValue(), UNBOX_LOCATIONS.get(unbox.getBoxingKind()), -1, unbox.getBoxingKind(), state, effects);
    }

    private static void processIdentity(PEReadEliminationBlockState state, LocationIdentity identity)
    {
        if (identity.isAny())
        {
            state.killReadCache();
        }
        else
        {
            state.killReadCache(identity, -1);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processInitialLoopState(Loop<Block> loop, PEReadEliminationBlockState initialState)
    {
        super.processInitialLoopState(loop, initialState);

        if (!initialState.getReadCache().isEmpty())
        {
            EconomicMap<ValueNode, Pair<ValueNode, Object>> firstValueSet = null;
            for (PhiNode phi : ((LoopBeginNode) loop.getHeader().getBeginNode()).phis())
            {
                ValueNode firstValue = phi.valueAt(0);
                if (firstValue != null && phi.getStackKind().isObject())
                {
                    ValueNode unproxified = GraphUtil.unproxify(firstValue);
                    if (firstValueSet == null)
                    {
                        firstValueSet = EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);
                    }
                    Pair<ValueNode, Object> pair = Pair.create(unproxified, firstValueSet.get(unproxified));
                    firstValueSet.put(unproxified, pair);
                }
            }

            if (firstValueSet != null)
            {
                ReadCacheEntry[] entries = new ReadCacheEntry[initialState.getReadCache().size()];
                int z = 0;
                for (ReadCacheEntry entry : initialState.getReadCache().getKeys())
                {
                    entries[z++] = entry;
                }

                for (ReadCacheEntry entry : entries)
                {
                    ValueNode object = entry.object;
                    if (object != null)
                    {
                        Pair<ValueNode, Object> pair = firstValueSet.get(object);
                        while (pair != null)
                        {
                            initialState.addReadCache(pair.getLeft(), entry.identity, entry.index, entry.kind, entry.overflowAccess, initialState.getReadCache().get(entry), this);
                            pair = (Pair<ValueNode, Object>) pair.getRight();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void processLoopExit(LoopExitNode exitNode, PEReadEliminationBlockState initialState, PEReadEliminationBlockState exitState, GraphEffectList effects)
    {
        super.processLoopExit(exitNode, initialState, exitState, effects);

        if (exitNode.graph().hasValueProxies())
        {
            MapCursor<ReadCacheEntry, ValueNode> entry = exitState.getReadCache().getEntries();
            while (entry.advance())
            {
                if (initialState.getReadCache().get(entry.getKey()) != entry.getValue())
                {
                    ValueNode value = exitState.getReadCache(entry.getKey().object, entry.getKey().identity, entry.getKey().index, entry.getKey().kind, this);
                    if (!(value instanceof ProxyNode) || ((ProxyNode) value).proxyPoint() != exitNode)
                    {
                        ProxyNode proxy = new ValueProxyNode(value, exitNode);
                        effects.addFloatingNode(proxy, "readCacheProxy");
                        exitState.getReadCache().put(entry.getKey(), proxy);
                    }
                }
            }
        }
    }

    @Override
    protected PEReadEliminationBlockState cloneState(PEReadEliminationBlockState other)
    {
        return new PEReadEliminationBlockState(other);
    }

    @Override
    protected MergeProcessor createMergeProcessor(Block merge)
    {
        return new ReadEliminationMergeProcessor(merge);
    }

    private class ReadEliminationMergeProcessor extends MergeProcessor
    {
        ReadEliminationMergeProcessor(Block mergeBlock)
        {
            super(mergeBlock);
        }

        @Override
        protected void merge(List<PEReadEliminationBlockState> states)
        {
            super.merge(states);

            mergeReadCache(states);
        }

        private void mergeReadCache(List<PEReadEliminationBlockState> states)
        {
            MapCursor<ReadCacheEntry, ValueNode> cursor = states.get(0).readCache.getEntries();
            while (cursor.advance())
            {
                ReadCacheEntry key = cursor.getKey();
                ValueNode value = cursor.getValue();
                boolean phi = false;
                for (int i = 1; i < states.size(); i++)
                {
                    ValueNode otherValue = states.get(i).readCache.get(key);
                    // e.g. unsafe loads / stores with different access kinds have different stamps
                    // although location, object and offset are the same, in this case we cannot
                    // create a phi nor can we set a common value
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
                    PhiNode phiNode = getPhi(key, value.stamp(NodeView.DEFAULT).unrestricted());
                    mergeEffects.addFloatingNode(phiNode, "mergeReadCache");
                    for (int i = 0; i < states.size(); i++)
                    {
                        ValueNode v = states.get(i).getReadCache(key.object, key.identity, key.index, key.kind, PEReadEliminationClosure.this);
                        setPhiInput(phiNode, i, v);
                    }
                    newState.readCache.put(key, phiNode);
                }
                else if (value != null)
                {
                    newState.readCache.put(key, value);
                }
            }
            // For object phis, see if there are known reads on all predecessors, for which we could create new phis.
            for (PhiNode phi : getPhis())
            {
                if (phi.getStackKind() == JavaKind.Object)
                {
                    for (ReadCacheEntry entry : states.get(0).readCache.getKeys())
                    {
                        if (entry.object == getPhiValueAt(phi, 0))
                        {
                            mergeReadCachePhi(phi, entry.identity, entry.index, entry.kind, entry.overflowAccess, states);
                        }
                    }
                }
            }
        }

        private void mergeReadCachePhi(PhiNode phi, LocationIdentity identity, int index, JavaKind kind, boolean overflowAccess, List<PEReadEliminationBlockState> states)
        {
            ValueNode[] values = new ValueNode[states.size()];
            values[0] = states.get(0).getReadCache(getPhiValueAt(phi, 0), identity, index, kind, PEReadEliminationClosure.this);
            if (values[0] != null)
            {
                for (int i = 1; i < states.size(); i++)
                {
                    ValueNode value = states.get(i).getReadCache(getPhiValueAt(phi, i), identity, index, kind, PEReadEliminationClosure.this);
                    // e.g. unsafe loads/stores with same identity, but different access kinds, see mergeReadCache(states)
                    if (value == null || !values[i - 1].stamp(NodeView.DEFAULT).isCompatible(value.stamp(NodeView.DEFAULT)))
                    {
                        return;
                    }
                    values[i] = value;
                }

                PhiNode phiNode = getPhi(new ReadCacheEntry(identity, phi, index, kind, overflowAccess), values[0].stamp(NodeView.DEFAULT).unrestricted());
                mergeEffects.addFloatingNode(phiNode, "mergeReadCachePhi");
                for (int i = 0; i < values.length; i++)
                {
                    setPhiInput(phiNode, i, values[i]);
                }
                newState.readCache.put(new ReadCacheEntry(identity, phi, index, kind, overflowAccess), phiNode);
            }
        }
    }

    @Override
    protected void processKilledLoopLocations(Loop<Block> loop, PEReadEliminationBlockState initialState, PEReadEliminationBlockState mergedStates)
    {
        if (initialState.readCache.size() > 0)
        {
            LoopKillCache loopKilledLocations = loopLocationKillCache.get(loop);
            // we have fully processed this loop the first time, remember to cache it the next time
            // it is visited
            if (loopKilledLocations == null)
            {
                loopKilledLocations = new LoopKillCache(1/* 1.visit */);
                loopLocationKillCache.put(loop, loopKilledLocations);
            }
            else
            {
                AbstractBeginNode beginNode = loop.getHeader().getBeginNode();
                OptionValues options = beginNode.getOptions();
                if (loopKilledLocations.visits() > GraalOptions.ReadEliminationMaxLoopVisits.getValue(options))
                {
                    // we have processed the loop too many times, kill all locations so the inner
                    // loop will never be processed more than once again on visit
                    loopKilledLocations.setKillsAll();
                }
                else
                {
                    // we have fully processed this loop >1 times, update the killed locations
                    EconomicSet<LocationIdentity> forwardEndLiveLocations = EconomicSet.create(Equivalence.DEFAULT);
                    for (ReadCacheEntry entry : initialState.readCache.getKeys())
                    {
                        forwardEndLiveLocations.add(entry.identity);
                    }
                    for (ReadCacheEntry entry : mergedStates.readCache.getKeys())
                    {
                        forwardEndLiveLocations.remove(entry.identity);
                    }
                    // every location that is alive before the loop but not after is killed by the
                    // loop
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
    protected PEReadEliminationBlockState stripKilledLoopLocations(Loop<Block> loop, PEReadEliminationBlockState originalInitialState)
    {
        PEReadEliminationBlockState initialState = super.stripKilledLoopLocations(loop, originalInitialState);
        LoopKillCache loopKilledLocations = loopLocationKillCache.get(loop);
        if (loopKilledLocations != null && loopKilledLocations.loopKillsLocations())
        {
            Iterator<ReadCacheEntry> it = initialState.readCache.getKeys().iterator();
            while (it.hasNext())
            {
                ReadCacheEntry entry = it.next();
                if (loopKilledLocations.containsLocation(entry.identity))
                {
                    it.remove();
                }
            }
        }
        return initialState;
    }
}
