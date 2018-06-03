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
import giraaff.virtual.phases.ea.PEReadEliminationBlockState.ReadCacheEntry;

// @class PEReadEliminationClosure
public final class PEReadEliminationClosure extends PartialEscapeClosure<PEReadEliminationBlockState>
{
    // @def
    private static final EnumMap<JavaKind, LocationIdentity> UNBOX_LOCATIONS;

    static
    {
        UNBOX_LOCATIONS = new EnumMap<>(JavaKind.class);
        for (JavaKind __kind : JavaKind.values())
        {
            UNBOX_LOCATIONS.put(__kind, NamedLocationIdentity.immutable("PEA unbox " + __kind.getJavaName()));
        }
    }

    // @cons
    public PEReadEliminationClosure(ScheduleResult __schedule, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, LoweringProvider __loweringProvider)
    {
        super(__schedule, __metaAccess, __constantReflection, __constantFieldProvider, __loweringProvider);
    }

    @Override
    protected PEReadEliminationBlockState getInitialState()
    {
        return new PEReadEliminationBlockState();
    }

    @Override
    protected boolean processNode(Node __node, PEReadEliminationBlockState __state, GraphEffectList __effects, FixedWithNextNode __lastFixedNode)
    {
        if (super.processNode(__node, __state, __effects, __lastFixedNode))
        {
            return true;
        }

        if (__node instanceof LoadFieldNode)
        {
            return processLoadField((LoadFieldNode) __node, __state, __effects);
        }
        else if (__node instanceof StoreFieldNode)
        {
            return processStoreField((StoreFieldNode) __node, __state, __effects);
        }
        else if (__node instanceof LoadIndexedNode)
        {
            return processLoadIndexed((LoadIndexedNode) __node, __state, __effects);
        }
        else if (__node instanceof StoreIndexedNode)
        {
            return processStoreIndexed((StoreIndexedNode) __node, __state, __effects);
        }
        else if (__node instanceof ArrayLengthNode)
        {
            return processArrayLength((ArrayLengthNode) __node, __state, __effects);
        }
        else if (__node instanceof UnboxNode)
        {
            return processUnbox((UnboxNode) __node, __state, __effects);
        }
        else if (__node instanceof RawLoadNode)
        {
            return processUnsafeLoad((RawLoadNode) __node, __state, __effects);
        }
        else if (__node instanceof RawStoreNode)
        {
            return processUnsafeStore((RawStoreNode) __node, __state, __effects);
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

        return false;
    }

    private boolean processStore(FixedNode __store, ValueNode __object, LocationIdentity __identity, int __index, JavaKind __accessKind, boolean __overflowAccess, ValueNode __value, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        ValueNode __unproxiedObject = GraphUtil.unproxify(__object);
        ValueNode __cachedValue = __state.getReadCache(__object, __identity, __index, __accessKind, this);

        ValueNode __finalValue = getScalarAlias(__value);
        boolean __result = false;
        if (GraphUtil.unproxify(__finalValue) == GraphUtil.unproxify(__cachedValue))
        {
            __effects.deleteNode(__store);
            __result = true;
        }
        __state.killReadCache(__identity, __index);
        __state.addReadCache(__unproxiedObject, __identity, __index, __accessKind, __overflowAccess, __finalValue, this);
        return __result;
    }

    private boolean processLoad(FixedNode __load, ValueNode __object, LocationIdentity __identity, int __index, JavaKind __kind, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        ValueNode __unproxiedObject = GraphUtil.unproxify(__object);
        ValueNode __cachedValue = __state.getReadCache(__unproxiedObject, __identity, __index, __kind, this);
        if (__cachedValue != null)
        {
            // perform the read elimination
            __effects.replaceAtUsages(__load, __cachedValue, __load);
            addScalarAlias(__load, __cachedValue);
            return true;
        }
        else
        {
            __state.addReadCache(__unproxiedObject, __identity, __index, __kind, false, __load, this);
            return false;
        }
    }

    private static boolean isOverflowAccess(JavaKind __accessKind, JavaKind __declaredKind)
    {
        if (__accessKind == __declaredKind)
        {
            return false;
        }
        if (__accessKind == JavaKind.Object)
        {
            switch (__declaredKind)
            {
                case Object:
                case Double:
                case Long:
                    return false;
                default:
                    return true;
            }
        }
        return __declaredKind.isPrimitive() ? __accessKind.getBitCount() > __declaredKind.getBitCount() : true;
    }

    private boolean processUnsafeLoad(RawLoadNode __load, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        if (__load.offset().isConstant())
        {
            ResolvedJavaType __type = StampTool.typeOrNull(__load.object());
            if (__type != null && __type.isArray())
            {
                JavaKind __accessKind = __load.accessKind();
                JavaKind __componentKind = __type.getComponentType().getJavaKind();
                long __offset = __load.offset().asJavaConstant().asLong();
                int __index = VirtualArrayNode.entryIndexForOffset(this.___tool.getArrayOffsetProvider(), __offset, __accessKind, __type.getComponentType(), Integer.MAX_VALUE);
                ValueNode __object = GraphUtil.unproxify(__load.object());
                LocationIdentity __location = NamedLocationIdentity.getArrayLocation(__componentKind);
                ValueNode __cachedValue = __state.getReadCache(__object, __location, __index, __accessKind, this);
                if (__cachedValue != null)
                {
                    __effects.replaceAtUsages(__load, __cachedValue, __load);
                    addScalarAlias(__load, __cachedValue);
                    return true;
                }
                else
                {
                    __state.addReadCache(__object, __location, __index, __accessKind, isOverflowAccess(__accessKind, __componentKind), __load, this);
                }
            }
        }
        return false;
    }

    private boolean processUnsafeStore(RawStoreNode __store, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        ResolvedJavaType __type = StampTool.typeOrNull(__store.object());
        if (__type != null && __type.isArray())
        {
            JavaKind __accessKind = __store.accessKind();
            JavaKind __componentKind = __type.getComponentType().getJavaKind();
            LocationIdentity __location = NamedLocationIdentity.getArrayLocation(__componentKind);
            if (__store.offset().isConstant())
            {
                long __offset = __store.offset().asJavaConstant().asLong();
                boolean __overflowAccess = isOverflowAccess(__accessKind, __componentKind);
                int __index = __overflowAccess ? -1 : VirtualArrayNode.entryIndexForOffset(this.___tool.getArrayOffsetProvider(), __offset, __accessKind, __type.getComponentType(), Integer.MAX_VALUE);
                return processStore(__store, __store.object(), __location, __index, __accessKind, __overflowAccess, __store.value(), __state, __effects);
            }
            else
            {
                processIdentity(__state, __location);
            }
        }
        else
        {
            __state.killReadCache();
        }
        return false;
    }

    private boolean processArrayLength(ArrayLengthNode __length, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        return processLoad(__length, __length.array(), NamedLocationIdentity.ARRAY_LENGTH_LOCATION, -1, JavaKind.Int, __state, __effects);
    }

    private boolean processStoreField(StoreFieldNode __store, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        if (__store.isVolatile())
        {
            __state.killReadCache();
            return false;
        }
        JavaKind __kind = __store.field().getJavaKind();
        return processStore(__store, __store.object(), new FieldLocationIdentity(__store.field()), -1, __kind, false, __store.value(), __state, __effects);
    }

    private boolean processLoadField(LoadFieldNode __load, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        if (__load.isVolatile())
        {
            __state.killReadCache();
            return false;
        }
        return processLoad(__load, __load.object(), new FieldLocationIdentity(__load.field()), -1, __load.field().getJavaKind(), __state, __effects);
    }

    private static JavaKind getElementKindFromStamp(ValueNode __array)
    {
        ResolvedJavaType __type = StampTool.typeOrNull(__array);
        if (__type != null && __type.isArray())
        {
            return __type.getComponentType().getJavaKind();
        }
        else
        {
            // it is likely an OSRLocal without valid stamp
            return JavaKind.Illegal;
        }
    }

    private boolean processStoreIndexed(StoreIndexedNode __store, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        int __index = __store.index().isConstant() ? ((JavaConstant) __store.index().asConstant()).asInt() : -1;
        // BASTORE (with elementKind being Byte) can be used to store values in boolean arrays.
        JavaKind __elementKind = __store.elementKind();
        if (__elementKind == JavaKind.Byte)
        {
            __elementKind = getElementKindFromStamp(__store.array());
            if (__elementKind == JavaKind.Illegal)
            {
                // Could not determine the actual access kind from stamp. Hence kill both.
                __state.killReadCache(NamedLocationIdentity.getArrayLocation(JavaKind.Boolean), __index);
                __state.killReadCache(NamedLocationIdentity.getArrayLocation(JavaKind.Byte), __index);
                return false;
            }
        }
        LocationIdentity __arrayLocation = NamedLocationIdentity.getArrayLocation(__elementKind);
        if (__index != -1)
        {
            return processStore(__store, __store.array(), __arrayLocation, __index, __elementKind, false, __store.value(), __state, __effects);
        }
        else
        {
            __state.killReadCache(__arrayLocation, -1);
        }
        return false;
    }

    private boolean processLoadIndexed(LoadIndexedNode __load, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        if (__load.index().isConstant())
        {
            int __index = ((JavaConstant) __load.index().asConstant()).asInt();
            // BALOAD (with elementKind being Byte) can be used to retrieve values from boolean arrays.
            JavaKind __elementKind = __load.elementKind();
            if (__elementKind == JavaKind.Byte)
            {
                __elementKind = getElementKindFromStamp(__load.array());
                if (__elementKind == JavaKind.Illegal)
                {
                    return false;
                }
            }
            LocationIdentity __arrayLocation = NamedLocationIdentity.getArrayLocation(__elementKind);
            return processLoad(__load, __load.array(), __arrayLocation, __index, __elementKind, __state, __effects);
        }
        return false;
    }

    private boolean processUnbox(UnboxNode __unbox, PEReadEliminationBlockState __state, GraphEffectList __effects)
    {
        return processLoad(__unbox, __unbox.getValue(), UNBOX_LOCATIONS.get(__unbox.getBoxingKind()), -1, __unbox.getBoxingKind(), __state, __effects);
    }

    private static void processIdentity(PEReadEliminationBlockState __state, LocationIdentity __identity)
    {
        if (__identity.isAny())
        {
            __state.killReadCache();
        }
        else
        {
            __state.killReadCache(__identity, -1);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processInitialLoopState(Loop<Block> __loop, PEReadEliminationBlockState __initialState)
    {
        super.processInitialLoopState(__loop, __initialState);

        if (!__initialState.getReadCache().isEmpty())
        {
            EconomicMap<ValueNode, Pair<ValueNode, Object>> __firstValueSet = null;
            for (PhiNode __phi : ((LoopBeginNode) __loop.getHeader().getBeginNode()).phis())
            {
                ValueNode __firstValue = __phi.valueAt(0);
                if (__firstValue != null && __phi.getStackKind().isObject())
                {
                    ValueNode __unproxified = GraphUtil.unproxify(__firstValue);
                    if (__firstValueSet == null)
                    {
                        __firstValueSet = EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);
                    }
                    Pair<ValueNode, Object> __pair = Pair.create(__unproxified, __firstValueSet.get(__unproxified));
                    __firstValueSet.put(__unproxified, __pair);
                }
            }

            if (__firstValueSet != null)
            {
                ReadCacheEntry[] __entries = new ReadCacheEntry[__initialState.getReadCache().size()];
                int __z = 0;
                for (ReadCacheEntry __entry : __initialState.getReadCache().getKeys())
                {
                    __entries[__z++] = __entry;
                }

                for (ReadCacheEntry __entry : __entries)
                {
                    ValueNode __object = __entry.___object;
                    if (__object != null)
                    {
                        Pair<ValueNode, Object> __pair = __firstValueSet.get(__object);
                        while (__pair != null)
                        {
                            __initialState.addReadCache(__pair.getLeft(), __entry.___identity, __entry.___index, __entry.___kind, __entry.___overflowAccess, __initialState.getReadCache().get(__entry), this);
                            __pair = (Pair<ValueNode, Object>) __pair.getRight();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void processLoopExit(LoopExitNode __exitNode, PEReadEliminationBlockState __initialState, PEReadEliminationBlockState __exitState, GraphEffectList __effects)
    {
        super.processLoopExit(__exitNode, __initialState, __exitState, __effects);

        if (__exitNode.graph().hasValueProxies())
        {
            MapCursor<ReadCacheEntry, ValueNode> __entry = __exitState.getReadCache().getEntries();
            while (__entry.advance())
            {
                if (__initialState.getReadCache().get(__entry.getKey()) != __entry.getValue())
                {
                    ValueNode __value = __exitState.getReadCache(__entry.getKey().___object, __entry.getKey().___identity, __entry.getKey().___index, __entry.getKey().___kind, this);
                    if (!(__value instanceof ProxyNode) || ((ProxyNode) __value).proxyPoint() != __exitNode)
                    {
                        ProxyNode __proxy = new ValueProxyNode(__value, __exitNode);
                        __effects.addFloatingNode(__proxy, "readCacheProxy");
                        __exitState.getReadCache().put(__entry.getKey(), __proxy);
                    }
                }
            }
        }
    }

    @Override
    protected PEReadEliminationBlockState cloneState(PEReadEliminationBlockState __other)
    {
        return new PEReadEliminationBlockState(__other);
    }

    @Override
    protected MergeProcessor createMergeProcessor(Block __merge)
    {
        return new ReadEliminationMergeProcessor(__merge);
    }

    // @class PEReadEliminationClosure.ReadEliminationMergeProcessor
    // @closure
    private final class ReadEliminationMergeProcessor extends MergeProcessor
    {
        // @cons
        ReadEliminationMergeProcessor(Block __mergeBlock)
        {
            super(__mergeBlock);
        }

        @Override
        protected void merge(List<PEReadEliminationBlockState> __states)
        {
            super.merge(__states);

            mergeReadCache(__states);
        }

        private void mergeReadCache(List<PEReadEliminationBlockState> __states)
        {
            MapCursor<ReadCacheEntry, ValueNode> __cursor = __states.get(0).___readCache.getEntries();
            while (__cursor.advance())
            {
                ReadCacheEntry __key = __cursor.getKey();
                ValueNode __value = __cursor.getValue();
                boolean __phi = false;
                for (int __i = 1; __i < __states.size(); __i++)
                {
                    ValueNode __otherValue = __states.get(__i).___readCache.get(__key);
                    // e.g. unsafe loads / stores with different access kinds have different stamps
                    // although location, object and offset are the same, in this case we cannot
                    // create a phi nor can we set a common value
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
                    PhiNode __phiNode = getPhi(__key, __value.stamp(NodeView.DEFAULT).unrestricted());
                    this.___mergeEffects.addFloatingNode(__phiNode, "mergeReadCache");
                    for (int __i = 0; __i < __states.size(); __i++)
                    {
                        ValueNode __v = __states.get(__i).getReadCache(__key.___object, __key.___identity, __key.___index, __key.___kind, PEReadEliminationClosure.this);
                        setPhiInput(__phiNode, __i, __v);
                    }
                    this.___newState.___readCache.put(__key, __phiNode);
                }
                else if (__value != null)
                {
                    this.___newState.___readCache.put(__key, __value);
                }
            }
            // For object phis, see if there are known reads on all predecessors, for which we could create new phis.
            for (PhiNode __phi : getPhis())
            {
                if (__phi.getStackKind() == JavaKind.Object)
                {
                    for (ReadCacheEntry __entry : __states.get(0).___readCache.getKeys())
                    {
                        if (__entry.___object == getPhiValueAt(__phi, 0))
                        {
                            mergeReadCachePhi(__phi, __entry.___identity, __entry.___index, __entry.___kind, __entry.___overflowAccess, __states);
                        }
                    }
                }
            }
        }

        private void mergeReadCachePhi(PhiNode __phi, LocationIdentity __identity, int __index, JavaKind __kind, boolean __overflowAccess, List<PEReadEliminationBlockState> __states)
        {
            ValueNode[] __values = new ValueNode[__states.size()];
            __values[0] = __states.get(0).getReadCache(getPhiValueAt(__phi, 0), __identity, __index, __kind, PEReadEliminationClosure.this);
            if (__values[0] != null)
            {
                for (int __i = 1; __i < __states.size(); __i++)
                {
                    ValueNode __value = __states.get(__i).getReadCache(getPhiValueAt(__phi, __i), __identity, __index, __kind, PEReadEliminationClosure.this);
                    // e.g. unsafe loads/stores with same identity, but different access kinds, see mergeReadCache(states)
                    if (__value == null || !__values[__i - 1].stamp(NodeView.DEFAULT).isCompatible(__value.stamp(NodeView.DEFAULT)))
                    {
                        return;
                    }
                    __values[__i] = __value;
                }

                PhiNode __phiNode = getPhi(new ReadCacheEntry(__identity, __phi, __index, __kind, __overflowAccess), __values[0].stamp(NodeView.DEFAULT).unrestricted());
                this.___mergeEffects.addFloatingNode(__phiNode, "mergeReadCachePhi");
                for (int __i = 0; __i < __values.length; __i++)
                {
                    setPhiInput(__phiNode, __i, __values[__i]);
                }
                this.___newState.___readCache.put(new ReadCacheEntry(__identity, __phi, __index, __kind, __overflowAccess), __phiNode);
            }
        }
    }

    @Override
    protected void processKilledLoopLocations(Loop<Block> __loop, PEReadEliminationBlockState __initialState, PEReadEliminationBlockState __mergedStates)
    {
        if (__initialState.___readCache.size() > 0)
        {
            LoopKillCache __loopKilledLocations = this.___loopLocationKillCache.get(__loop);
            // we have fully processed this loop the first time, remember to cache it the next time it is visited
            if (__loopKilledLocations == null)
            {
                __loopKilledLocations = new LoopKillCache(1); // 1.visit
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
                    for (ReadCacheEntry __entry : __initialState.___readCache.getKeys())
                    {
                        __forwardEndLiveLocations.add(__entry.___identity);
                    }
                    for (ReadCacheEntry __entry : __mergedStates.___readCache.getKeys())
                    {
                        __forwardEndLiveLocations.remove(__entry.___identity);
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
    protected PEReadEliminationBlockState stripKilledLoopLocations(Loop<Block> __loop, PEReadEliminationBlockState __originalInitialState)
    {
        PEReadEliminationBlockState __initialState = super.stripKilledLoopLocations(__loop, __originalInitialState);
        LoopKillCache __loopKilledLocations = this.___loopLocationKillCache.get(__loop);
        if (__loopKilledLocations != null && __loopKilledLocations.loopKillsLocations())
        {
            Iterator<ReadCacheEntry> __it = __initialState.___readCache.getKeys().iterator();
            while (__it.hasNext())
            {
                ReadCacheEntry __entry = __it.next();
                if (__loopKilledLocations.containsLocation(__entry.___identity))
                {
                    __it.remove();
                }
            }
        }
        return __initialState;
    }
}
