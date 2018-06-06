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
import giraaff.virtual.phases.ea.ReadEliminationBlockState;

///
// This closure initially handled a set of nodes that is disjunct from {@link PEReadEliminationClosure},
// but over time both have evolved so that there's a significant overlap.
///
// @class ReadEliminationClosure
public final class ReadEliminationClosure extends EffectsClosure<ReadEliminationBlockState>
{
    // @field
    private final boolean ___considerGuards;

    // @cons ReadEliminationClosure
    public ReadEliminationClosure(ControlFlowGraph __cfg, boolean __considerGuards)
    {
        super(null, __cfg);
        this.___considerGuards = __considerGuards;
    }

    @Override
    protected ReadEliminationBlockState getInitialState()
    {
        return new ReadEliminationBlockState();
    }

    @Override
    protected boolean processNode(Node __node, ReadEliminationBlockState __state, GraphEffectList __effects, FixedWithNextNode __lastFixedNode)
    {
        boolean __deleted = false;
        if (__node instanceof AccessFieldNode)
        {
            AccessFieldNode __access = (AccessFieldNode) __node;
            if (__access.isVolatile())
            {
                processIdentity(__state, LocationIdentity.any());
            }
            else
            {
                ValueNode __object = GraphUtil.unproxify(__access.object());
                ReadEliminationBlockState.LoadCacheEntry __identifier = new ReadEliminationBlockState.LoadCacheEntry(__object, new FieldLocationIdentity(__access.field()));
                ValueNode __cachedValue = __state.getCacheEntry(__identifier);
                if (__node instanceof LoadFieldNode)
                {
                    if (__cachedValue != null && __access.stamp(NodeView.DEFAULT).isCompatible(__cachedValue.stamp(NodeView.DEFAULT)))
                    {
                        __effects.replaceAtUsages(__access, __cachedValue, __access);
                        addScalarAlias(__access, __cachedValue);
                        __deleted = true;
                    }
                    else
                    {
                        __state.addCacheEntry(__identifier, __access);
                    }
                }
                else
                {
                    StoreFieldNode __store = (StoreFieldNode) __node;
                    ValueNode __value = getScalarAlias(__store.value());
                    if (GraphUtil.unproxify(__value) == GraphUtil.unproxify(__cachedValue))
                    {
                        __effects.deleteNode(__store);
                        __deleted = true;
                    }
                    __state.killReadCache(__identifier.___identity);
                    __state.addCacheEntry(__identifier, __value);
                }
            }
        }
        else if (__node instanceof ReadNode)
        {
            ReadNode __read = (ReadNode) __node;
            if (__read.getLocationIdentity().isSingle())
            {
                ValueNode __object = GraphUtil.unproxify(__read.getAddress());
                ReadEliminationBlockState.LoadCacheEntry __identifier = new ReadEliminationBlockState.LoadCacheEntry(__object, __read.getLocationIdentity());
                ValueNode __cachedValue = __state.getCacheEntry(__identifier);
                if (__cachedValue != null && areValuesReplaceable(__read, __cachedValue, this.___considerGuards))
                {
                    __effects.replaceAtUsages(__read, __cachedValue, __read);
                    addScalarAlias(__read, __cachedValue);
                    __deleted = true;
                }
                else
                {
                    __state.addCacheEntry(__identifier, __read);
                }
            }
        }
        else if (__node instanceof WriteNode)
        {
            WriteNode __write = (WriteNode) __node;
            if (__write.getLocationIdentity().isSingle())
            {
                ValueNode __object = GraphUtil.unproxify(__write.getAddress());
                ReadEliminationBlockState.LoadCacheEntry __identifier = new ReadEliminationBlockState.LoadCacheEntry(__object, __write.getLocationIdentity());
                ValueNode __cachedValue = __state.getCacheEntry(__identifier);

                ValueNode __value = getScalarAlias(__write.value());
                if (GraphUtil.unproxify(__value) == GraphUtil.unproxify(__cachedValue))
                {
                    __effects.deleteNode(__write);
                    __deleted = true;
                }
                processIdentity(__state, __write.getLocationIdentity());
                __state.addCacheEntry(__identifier, __value);
            }
            else
            {
                processIdentity(__state, __write.getLocationIdentity());
            }
        }
        else if (__node instanceof UnsafeAccessNode)
        {
            ResolvedJavaType __type = StampTool.typeOrNull(((UnsafeAccessNode) __node).object());
            if (__type != null && !__type.isArray())
            {
                if (__node instanceof RawLoadNode)
                {
                    RawLoadNode __load = (RawLoadNode) __node;
                    if (__load.getLocationIdentity().isSingle())
                    {
                        ValueNode __object = GraphUtil.unproxify(__load.object());
                        ReadEliminationBlockState.UnsafeLoadCacheEntry __identifier = new ReadEliminationBlockState.UnsafeLoadCacheEntry(__object, __load.offset(), __load.getLocationIdentity());
                        ValueNode __cachedValue = __state.getCacheEntry(__identifier);
                        if (__cachedValue != null && areValuesReplaceable(__load, __cachedValue, this.___considerGuards))
                        {
                            __effects.replaceAtUsages(__load, __cachedValue, __load);
                            addScalarAlias(__load, __cachedValue);
                            __deleted = true;
                        }
                        else
                        {
                            __state.addCacheEntry(__identifier, __load);
                        }
                    }
                }
                else
                {
                    RawStoreNode __write = (RawStoreNode) __node;
                    if (__write.getLocationIdentity().isSingle())
                    {
                        ValueNode __object = GraphUtil.unproxify(__write.object());
                        ReadEliminationBlockState.UnsafeLoadCacheEntry __identifier = new ReadEliminationBlockState.UnsafeLoadCacheEntry(__object, __write.offset(), __write.getLocationIdentity());
                        ValueNode __cachedValue = __state.getCacheEntry(__identifier);

                        ValueNode __value = getScalarAlias(__write.value());
                        if (GraphUtil.unproxify(__value) == GraphUtil.unproxify(__cachedValue))
                        {
                            __effects.deleteNode(__write);
                            __deleted = true;
                        }
                        processIdentity(__state, __write.getLocationIdentity());
                        __state.addCacheEntry(__identifier, __value);
                    }
                    else
                    {
                        processIdentity(__state, __write.getLocationIdentity());
                    }
                }
            }
        }
        else if (__node instanceof MemoryCheckpoint.Single)
        {
            LocationIdentity __identity = ((MemoryCheckpoint.Single) __node).getLocationIdentity();
            processIdentity(__state, __identity);
        }
        else if (__node instanceof MemoryCheckpoint.Multi)
        {
            for (LocationIdentity __identity : ((MemoryCheckpoint.Multi) __node).getLocationIdentities())
            {
                processIdentity(__state, __identity);
            }
        }
        return __deleted;
    }

    private static boolean areValuesReplaceable(ValueNode __originalValue, ValueNode __replacementValue, boolean __considerGuards)
    {
        return __originalValue.stamp(NodeView.DEFAULT).isCompatible(__replacementValue.stamp(NodeView.DEFAULT)) && (!__considerGuards || (getGuard(__originalValue) == null || getGuard(__originalValue) == getGuard(__replacementValue)));
    }

    private static GuardingNode getGuard(ValueNode __node)
    {
        if (__node instanceof GuardedNode)
        {
            GuardedNode __guardedNode = (GuardedNode) __node;
            return __guardedNode.getGuard();
        }
        return null;
    }

    private static void processIdentity(ReadEliminationBlockState __state, LocationIdentity __identity)
    {
        if (__identity.isAny())
        {
            __state.killReadCache();
            return;
        }
        __state.killReadCache(__identity);
    }

    @Override
    protected void processLoopExit(LoopExitNode __exitNode, ReadEliminationBlockState __initialState, ReadEliminationBlockState __exitState, GraphEffectList __effects)
    {
        if (__exitNode.graph().hasValueProxies())
        {
            MapCursor<ReadEliminationBlockState.CacheEntry<?>, ValueNode> __entry = __exitState.getReadCache().getEntries();
            while (__entry.advance())
            {
                if (__initialState.getReadCache().get(__entry.getKey()) != __entry.getValue())
                {
                    ProxyNode __proxy = new ValueProxyNode(__exitState.getCacheEntry(__entry.getKey()), __exitNode);
                    __effects.addFloatingNode(__proxy, "readCacheProxy");
                    __exitState.getReadCache().put(__entry.getKey(), __proxy);
                }
            }
        }
    }

    @Override
    protected ReadEliminationBlockState cloneState(ReadEliminationBlockState __other)
    {
        return new ReadEliminationBlockState(__other);
    }

    @Override
    protected EffectsClosure<ReadEliminationBlockState>.MergeProcessor createMergeProcessor(Block __merge)
    {
        return new ReadEliminationClosure.ReadEliminationMergeProcessor(__merge);
    }

    // @class ReadEliminationClosure.ReadEliminationMergeProcessor
    // @closure
    private final class ReadEliminationMergeProcessor extends EffectsClosure<ReadEliminationBlockState>.MergeProcessor
    {
        // @field
        private final EconomicMap<Object, ValuePhiNode> ___materializedPhis = EconomicMap.create(Equivalence.DEFAULT);

        // @cons ReadEliminationClosure.ReadEliminationMergeProcessor
        ReadEliminationMergeProcessor(Block __mergeBlock)
        {
            super(__mergeBlock);
        }

        protected ValuePhiNode getCachedPhi(ReadEliminationBlockState.CacheEntry<?> __virtual, Stamp __stamp)
        {
            ValuePhiNode __result = this.___materializedPhis.get(__virtual);
            if (__result == null)
            {
                __result = createValuePhi(__stamp);
                this.___materializedPhis.put(__virtual, __result);
            }
            return __result;
        }

        @Override
        protected void merge(List<ReadEliminationBlockState> __states)
        {
            MapCursor<ReadEliminationBlockState.CacheEntry<?>, ValueNode> __cursor = __states.get(0).___readCache.getEntries();
            while (__cursor.advance())
            {
                ReadEliminationBlockState.CacheEntry<?> __key = __cursor.getKey();
                ValueNode __value = __cursor.getValue();
                boolean __phi = false;
                for (int __i = 1; __i < __states.size(); __i++)
                {
                    ValueNode __otherValue = __states.get(__i).___readCache.get(__key);
                    // E.g. unsafe loads/stores with different access kinds have different stamps although location,
                    // object and offset are the same. In this case we cannot create a phi nor can we set a common value.
                    if (__otherValue == null || !__value.stamp(NodeView.DEFAULT).isCompatible(__otherValue.stamp(NodeView.DEFAULT)))
                    {
                        __value = null;
                        __phi = false;
                        break;
                    }
                    if (!__phi && __otherValue != __value)
                    {
                        __phi = true;
                    }
                }
                if (__phi)
                {
                    PhiNode __phiNode = getCachedPhi(__key, __value.stamp(NodeView.DEFAULT).unrestricted());
                    this.___mergeEffects.addFloatingNode(__phiNode, "mergeReadCache");
                    for (int __i = 0; __i < __states.size(); __i++)
                    {
                        ValueNode __v = __states.get(__i).getCacheEntry(__key);
                        setPhiInput(__phiNode, __i, __v);
                    }
                    this.___newState.addCacheEntry(__key, __phiNode);
                }
                else if (__value != null)
                {
                    // case that there is the same value on all branches
                    this.___newState.addCacheEntry(__key, __value);
                }
            }
            // For object phis, see if there are known reads on all predecessors, for which we could create new phis.
            for (PhiNode __phi : getPhis())
            {
                if (__phi.getStackKind() == JavaKind.Object)
                {
                    for (ReadEliminationBlockState.CacheEntry<?> __entry : __states.get(0).___readCache.getKeys())
                    {
                        if (__entry.___object == getPhiValueAt(__phi, 0))
                        {
                            mergeReadCachePhi(__phi, __entry, __states);
                        }
                    }
                }
            }
        }

        private void mergeReadCachePhi(PhiNode __phi, ReadEliminationBlockState.CacheEntry<?> __identifier, List<ReadEliminationBlockState> __states)
        {
            ValueNode[] __values = new ValueNode[__states.size()];
            __values[0] = __states.get(0).getCacheEntry(__identifier.duplicateWithObject(getPhiValueAt(__phi, 0)));
            if (__values[0] != null)
            {
                for (int __i = 1; __i < __states.size(); __i++)
                {
                    ValueNode __value = __states.get(__i).getCacheEntry(__identifier.duplicateWithObject(getPhiValueAt(__phi, __i)));
                    // e.g. unsafe loads/stores with same identity, but different access kinds, see mergeReadCache(states)
                    if (__value == null || !__values[__i - 1].stamp(NodeView.DEFAULT).isCompatible(__value.stamp(NodeView.DEFAULT)))
                    {
                        return;
                    }
                    __values[__i] = __value;
                }

                ReadEliminationBlockState.CacheEntry<?> __newIdentifier = __identifier.duplicateWithObject(__phi);
                PhiNode __phiNode = getCachedPhi(__newIdentifier, __values[0].stamp(NodeView.DEFAULT).unrestricted());
                this.___mergeEffects.addFloatingNode(__phiNode, "mergeReadCachePhi");
                for (int __i = 0; __i < __values.length; __i++)
                {
                    setPhiInput(__phiNode, __i, __values[__i]);
                }
                this.___newState.addCacheEntry(__newIdentifier, __phiNode);
            }
        }
    }

    @Override
    protected void processKilledLoopLocations(Loop<Block> __loop, ReadEliminationBlockState __initialState, ReadEliminationBlockState __mergedStates)
    {
        if (__initialState.___readCache.size() > 0)
        {
            EffectsClosure.LoopKillCache __loopKilledLocations = this.___loopLocationKillCache.get(__loop);
            // we have fully processed this loop the first time, remember to cache it the next time it is visited
            if (__loopKilledLocations == null)
            {
                __loopKilledLocations = new EffectsClosure.LoopKillCache(1); // 1.visit
                this.___loopLocationKillCache.put(__loop, __loopKilledLocations);
            }
            else
            {
                if (__loopKilledLocations.visits() > GraalOptions.readEliminationMaxLoopVisits)
                {
                    // we have processed the loop too many times: kill all locations, so
                    // the inner loop will never be processed more than once again on visit
                    __loopKilledLocations.setKillsAll();
                }
                else
                {
                    // we have fully processed this loop >1 times, update the killed locations
                    EconomicSet<LocationIdentity> __forwardEndLiveLocations = EconomicSet.create(Equivalence.DEFAULT);
                    for (ReadEliminationBlockState.CacheEntry<?> __entry : __initialState.___readCache.getKeys())
                    {
                        __forwardEndLiveLocations.add(__entry.getIdentity());
                    }
                    for (ReadEliminationBlockState.CacheEntry<?> __entry : __mergedStates.___readCache.getKeys())
                    {
                        __forwardEndLiveLocations.remove(__entry.getIdentity());
                    }
                    // every location that is alive before the loop but not after is killed by the loop
                    for (LocationIdentity __location : __forwardEndLiveLocations)
                    {
                        __loopKilledLocations.rememberLoopKilledLocation(__location);
                    }
                }
                // remember the loop visit
                __loopKilledLocations.visited();
            }
        }
    }

    @Override
    protected ReadEliminationBlockState stripKilledLoopLocations(Loop<Block> __loop, ReadEliminationBlockState __originalInitialState)
    {
        ReadEliminationBlockState __initialState = super.stripKilledLoopLocations(__loop, __originalInitialState);
        EffectsClosure.LoopKillCache __loopKilledLocations = this.___loopLocationKillCache.get(__loop);
        if (__loopKilledLocations != null && __loopKilledLocations.loopKillsLocations())
        {
            Iterator<ReadEliminationBlockState.CacheEntry<?>> __it = __initialState.___readCache.getKeys().iterator();
            while (__it.hasNext())
            {
                ReadEliminationBlockState.CacheEntry<?> __entry = __it.next();
                if (__loopKilledLocations.containsLocation(__entry.getIdentity()))
                {
                    __it.remove();
                }
            }
        }
        return __initialState;
    }
}
