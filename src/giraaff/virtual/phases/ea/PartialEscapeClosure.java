package giraaff.virtual.phases.ea;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntUnaryOperator;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.Loop;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.Position;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.ValueProxyNode;
import giraaff.nodes.VirtualState;
import giraaff.nodes.VirtualState.NodeClosure;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.NodeWithState;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.AllocatedObjectNode;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.virtual.nodes.VirtualObjectState;

// @class PartialEscapeClosure
public abstract class PartialEscapeClosure<BlockT extends PartialEscapeBlockState<BlockT>> extends EffectsClosure<BlockT>
{
    /**
     * Nodes with inputs that were modified during analysis are marked in this bitset - this way
     * nodes that are not influenced at all by analysis can be rejected quickly.
     */
    // @field
    private final NodeBitMap hasVirtualInputs;

    /**
     * This is handed out to implementers of {@link Virtualizable}.
     */
    // @field
    protected final VirtualizerToolImpl tool;

    /**
     * The indexes into this array correspond to {@link VirtualObjectNode#getObjectId()}.
     */
    // @field
    public final ArrayList<VirtualObjectNode> virtualObjects = new ArrayList<>();

    @Override
    public boolean needsApplyEffects()
    {
        if (hasChanged())
        {
            return true;
        }
        /*
         * If there is a mismatch between the number of materializations and the number of virtualizations,
         * we need to apply effects, even if there were no other significant changes to the graph.
         */
        int __delta = 0;
        for (Block __block : cfg.getBlocks())
        {
            GraphEffectList __effects = blockEffects.get(__block);
            if (__effects != null)
            {
                __delta += __effects.getVirtualizationDelta();
            }
        }
        for (Loop<Block> __loop : cfg.getLoops())
        {
            GraphEffectList __effects = loopMergeEffects.get(__loop);
            if (__effects != null)
            {
                __delta += __effects.getVirtualizationDelta();
            }
        }
        return __delta != 0;
    }

    // @class PartialEscapeClosure.CollectVirtualObjectsClosure
    // @closure
    private final class CollectVirtualObjectsClosure extends NodeClosure<ValueNode>
    {
        // @field
        private final EconomicSet<VirtualObjectNode> virtual;
        // @field
        private final GraphEffectList effects;
        // @field
        private final BlockT state;

        // @cons
        private CollectVirtualObjectsClosure(EconomicSet<VirtualObjectNode> __virtual, GraphEffectList __effects, BlockT __state)
        {
            super();
            this.virtual = __virtual;
            this.effects = __effects;
            this.state = __state;
        }

        @Override
        public void apply(Node __usage, ValueNode __value)
        {
            if (__value instanceof VirtualObjectNode)
            {
                VirtualObjectNode __object = (VirtualObjectNode) __value;
                if (__object.getObjectId() != -1 && state.getObjectStateOptional(__object) != null)
                {
                    virtual.add(__object);
                }
            }
            else
            {
                ValueNode __alias = getAlias(__value);
                if (__alias instanceof VirtualObjectNode)
                {
                    VirtualObjectNode __object = (VirtualObjectNode) __alias;
                    virtual.add(__object);
                    effects.replaceFirstInput(__usage, __value, __object);
                }
            }
        }
    }

    /**
     * Final subclass of PartialEscapeClosure, for performance and to make everything behave nicely
     * with generics.
     */
    // @class PartialEscapeClosure.Final
    public static final class Final extends PartialEscapeClosure<PartialEscapeBlockState.Final>
    {
        // @cons
        public Final(ScheduleResult __schedule, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, LoweringProvider __loweringProvider)
        {
            super(__schedule, __metaAccess, __constantReflection, __constantFieldProvider, __loweringProvider);
        }

        @Override
        protected PartialEscapeBlockState.Final getInitialState()
        {
            return new PartialEscapeBlockState.Final();
        }

        @Override
        protected PartialEscapeBlockState.Final cloneState(PartialEscapeBlockState.Final __oldState)
        {
            return new PartialEscapeBlockState.Final(__oldState);
        }
    }

    // @cons
    public PartialEscapeClosure(ScheduleResult __schedule, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider)
    {
        this(__schedule, __metaAccess, __constantReflection, __constantFieldProvider, null);
    }

    // @cons
    public PartialEscapeClosure(ScheduleResult __schedule, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, LoweringProvider __loweringProvider)
    {
        super(__schedule, __schedule.getCFG());
        StructuredGraph __graph = __schedule.getCFG().graph;
        this.hasVirtualInputs = __graph.createNodeBitMap();
        this.tool = new VirtualizerToolImpl(__metaAccess, __constantReflection, __constantFieldProvider, this, __graph.getAssumptions(), __loweringProvider);
    }

    /**
     * @return true if the node was deleted, false otherwise
     */
    @Override
    protected boolean processNode(Node __node, BlockT __state, GraphEffectList __effects, FixedWithNextNode __lastFixedNode)
    {
        /*
         * These checks make up for the fact that an earliest schedule moves CallTargetNodes upwards
         * and thus materializes virtual objects needlessly. Also, FrameStates and ConstantNodes are
         * scheduled, but can safely be ignored.
         */
        if (__node instanceof CallTargetNode || __node instanceof FrameState || __node instanceof ConstantNode)
        {
            return false;
        }
        else if (__node instanceof Invoke)
        {
            processNodeInternal(((Invoke) __node).callTarget(), __state, __effects, __lastFixedNode);
        }
        return processNodeInternal(__node, __state, __effects, __lastFixedNode);
    }

    private boolean processNodeInternal(Node __node, BlockT __state, GraphEffectList __effects, FixedWithNextNode __lastFixedNode)
    {
        FixedNode __nextFixedNode = __lastFixedNode == null ? null : __lastFixedNode.next();

        if (requiresProcessing(__node))
        {
            if (processVirtualizable((ValueNode) __node, __nextFixedNode, __state, __effects) == false)
            {
                return false;
            }
            if (tool.isDeleted())
            {
                return true;
            }
        }
        if (hasVirtualInputs.isMarked(__node) && __node instanceof ValueNode)
        {
            if (__node instanceof Virtualizable)
            {
                if (processVirtualizable((ValueNode) __node, __nextFixedNode, __state, __effects) == false)
                {
                    return false;
                }
                if (tool.isDeleted())
                {
                    return true;
                }
            }
            processNodeInputs((ValueNode) __node, __nextFixedNode, __state, __effects);
        }

        if (hasScalarReplacedInputs(__node) && __node instanceof ValueNode)
        {
            if (processNodeWithScalarReplacedInputs((ValueNode) __node, __nextFixedNode, __state, __effects))
            {
                return true;
            }
        }
        return false;
    }

    protected boolean requiresProcessing(Node __node)
    {
        return __node instanceof VirtualizableAllocation;
    }

    private boolean processVirtualizable(ValueNode __node, FixedNode __insertBefore, BlockT __state, GraphEffectList __effects)
    {
        tool.reset(__state, __node, __insertBefore, __effects);
        return virtualize(__node, tool);
    }

    protected boolean virtualize(ValueNode __node, VirtualizerTool __vt)
    {
        ((Virtualizable) __node).virtualize(__vt);
        return true; // request further processing
    }

    /**
     * This tries to canonicalize the node based on improved (replaced) inputs.
     */
    @SuppressWarnings("unchecked")
    private boolean processNodeWithScalarReplacedInputs(ValueNode __node, FixedNode __insertBefore, BlockT __state, GraphEffectList __effects)
    {
        ValueNode __canonicalizedValue = __node;
        if (__node instanceof Canonicalizable.Unary<?>)
        {
            Canonicalizable.Unary<ValueNode> __canonicalizable = (Canonicalizable.Unary<ValueNode>) __node;
            ObjectState __valueObj = getObjectState(__state, __canonicalizable.getValue());
            ValueNode __valueAlias = __valueObj != null ? __valueObj.getMaterializedValue() : getScalarAlias(__canonicalizable.getValue());
            if (__valueAlias != __canonicalizable.getValue())
            {
                __canonicalizedValue = (ValueNode) __canonicalizable.canonical(tool, __valueAlias);
            }
        }
        else if (__node instanceof Canonicalizable.Binary<?>)
        {
            Canonicalizable.Binary<ValueNode> __canonicalizable = (Canonicalizable.Binary<ValueNode>) __node;
            ObjectState __xObj = getObjectState(__state, __canonicalizable.getX());
            ValueNode __xAlias = __xObj != null ? __xObj.getMaterializedValue() : getScalarAlias(__canonicalizable.getX());
            ObjectState __yObj = getObjectState(__state, __canonicalizable.getY());
            ValueNode __yAlias = __yObj != null ? __yObj.getMaterializedValue() : getScalarAlias(__canonicalizable.getY());
            if (__xAlias != __canonicalizable.getX() || __yAlias != __canonicalizable.getY())
            {
                __canonicalizedValue = (ValueNode) __canonicalizable.canonical(tool, __xAlias, __yAlias);
            }
        }
        else
        {
            return false;
        }
        if (__canonicalizedValue != __node && __canonicalizedValue != null)
        {
            if (__canonicalizedValue.isAlive())
            {
                ValueNode __alias = getAliasAndResolve(__state, __canonicalizedValue);
                if (__alias instanceof VirtualObjectNode)
                {
                    addVirtualAlias((VirtualObjectNode) __alias, __node);
                    __effects.deleteNode(__node);
                }
                else
                {
                    __effects.replaceAtUsages(__node, __alias, __insertBefore);
                    addScalarAlias(__node, __alias);
                }
            }
            else
            {
                if (!prepareCanonicalNode(__canonicalizedValue, __state, __effects))
                {
                    return false;
                }
                if (__canonicalizedValue instanceof ControlSinkNode)
                {
                    __effects.replaceWithSink((FixedWithNextNode) __node, (ControlSinkNode) __canonicalizedValue);
                    __state.markAsDead();
                }
                else
                {
                    __effects.replaceAtUsages(__node, __canonicalizedValue, __insertBefore);
                    addScalarAlias(__node, __canonicalizedValue);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Nodes created during canonicalizations need to be scanned for values that were replaced.
     */
    private boolean prepareCanonicalNode(ValueNode __node, BlockT __state, GraphEffectList __effects)
    {
        for (Position __pos : __node.inputPositions())
        {
            Node __input = __pos.get(__node);
            if (__input instanceof ValueNode)
            {
                if (__input.isAlive())
                {
                    if (!(__input instanceof VirtualObjectNode))
                    {
                        ObjectState __obj = getObjectState(__state, (ValueNode) __input);
                        if (__obj != null)
                        {
                            if (__obj.isVirtual())
                            {
                                return false;
                            }
                            else
                            {
                                __pos.initialize(__node, __obj.getMaterializedValue());
                            }
                        }
                        else
                        {
                            __pos.initialize(__node, getScalarAlias((ValueNode) __input));
                        }
                    }
                }
                else
                {
                    if (!prepareCanonicalNode((ValueNode) __input, __state, __effects))
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * This replaces all inputs that point to virtual or materialized values with the actual value,
     * materializing if necessary.
     * Also takes care of frame states, adding the necessary {@link VirtualObjectState}.
     */
    private void processNodeInputs(ValueNode __node, FixedNode __insertBefore, BlockT __state, GraphEffectList __effects)
    {
        for (Node __input : __node.inputs())
        {
            if (__input instanceof ValueNode)
            {
                ValueNode __alias = getAlias((ValueNode) __input);
                if (__alias instanceof VirtualObjectNode)
                {
                    int __id = ((VirtualObjectNode) __alias).getObjectId();
                    ensureMaterialized(__state, __id, __insertBefore, __effects);
                    __effects.replaceFirstInput(__node, __input, __state.getObjectState(__id).getMaterializedValue());
                }
            }
        }
        if (__node instanceof NodeWithState)
        {
            processNodeWithState((NodeWithState) __node, __state, __effects);
        }
    }

    private void processNodeWithState(NodeWithState __nodeWithState, BlockT __state, GraphEffectList __effects)
    {
        for (FrameState __fs : __nodeWithState.states())
        {
            FrameState __frameState = getUniqueFramestate(__nodeWithState, __fs);
            EconomicSet<VirtualObjectNode> __virtual = EconomicSet.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);
            __frameState.applyToNonVirtual(new CollectVirtualObjectsClosure(__virtual, __effects, __state));
            collectLockedVirtualObjects(__state, __virtual);
            collectReferencedVirtualObjects(__state, __virtual);
            addVirtualMappings(__frameState, __virtual, __state, __effects);
        }
    }

    private static FrameState getUniqueFramestate(NodeWithState __nodeWithState, FrameState __frameState)
    {
        if (__frameState.hasMoreThanOneUsage())
        {
            // Can happen for example from inlined snippets with multiple state split nodes.
            FrameState __copy = (FrameState) __frameState.copyWithInputs();
            __nodeWithState.asNode().replaceFirstInput(__frameState, __copy);
            return __copy;
        }
        return __frameState;
    }

    private void addVirtualMappings(FrameState __frameState, EconomicSet<VirtualObjectNode> __virtual, BlockT __state, GraphEffectList __effects)
    {
        for (VirtualObjectNode __obj : __virtual)
        {
            __effects.addVirtualMapping(__frameState, __state.getObjectState(__obj).createEscapeObjectState(__obj));
        }
    }

    private void collectReferencedVirtualObjects(BlockT __state, EconomicSet<VirtualObjectNode> __virtual)
    {
        Iterator<VirtualObjectNode> __iterator = __virtual.iterator();
        while (__iterator.hasNext())
        {
            VirtualObjectNode __object = __iterator.next();
            int __id = __object.getObjectId();
            if (__id != -1)
            {
                ObjectState __objState = __state.getObjectStateOptional(__id);
                if (__objState != null && __objState.isVirtual())
                {
                    for (ValueNode __entry : __objState.getEntries())
                    {
                        if (__entry instanceof VirtualObjectNode)
                        {
                            VirtualObjectNode __entryVirtual = (VirtualObjectNode) __entry;
                            if (!__virtual.contains(__entryVirtual))
                            {
                                __virtual.add(__entryVirtual);
                            }
                        }
                    }
                }
            }
        }
    }

    private void collectLockedVirtualObjects(BlockT __state, EconomicSet<VirtualObjectNode> __virtual)
    {
        for (int __i = 0; __i < __state.getStateCount(); __i++)
        {
            ObjectState __objState = __state.getObjectStateOptional(__i);
            if (__objState != null && __objState.isVirtual() && __objState.hasLocks())
            {
                __virtual.add(virtualObjects.get(__i));
            }
        }
    }

    /**
     * @return true if materialization happened, false if not.
     */
    protected boolean ensureMaterialized(PartialEscapeBlockState<?> __state, int __object, FixedNode __materializeBefore, GraphEffectList __effects)
    {
        if (__state.getObjectState(__object).isVirtual())
        {
            VirtualObjectNode __virtual = virtualObjects.get(__object);
            __state.materializeBefore(__materializeBefore, __virtual, __effects);
            return true;
        }
        else
        {
            return false;
        }
    }

    public static boolean updateStatesForMaterialized(PartialEscapeBlockState<?> __state, VirtualObjectNode __virtual, ValueNode __materializedValue)
    {
        // update all existing states with the newly materialized object
        boolean __change = false;
        for (int __i = 0; __i < __state.getStateCount(); __i++)
        {
            ObjectState __objState = __state.getObjectStateOptional(__i);
            if (__objState != null && __objState.isVirtual())
            {
                ValueNode[] __entries = __objState.getEntries();
                for (int __i2 = 0; __i2 < __entries.length; __i2++)
                {
                    if (__entries[__i2] == __virtual)
                    {
                        __state.setEntry(__i, __i2, __materializedValue);
                        __change = true;
                    }
                }
            }
        }
        return __change;
    }

    @Override
    protected BlockT stripKilledLoopLocations(Loop<Block> __loop, BlockT __originalInitialState)
    {
        BlockT __initialState = super.stripKilledLoopLocations(__loop, __originalInitialState);
        if (__loop.getDepth() > GraalOptions.escapeAnalysisLoopCutoff)
        {
            /*
             * After we've reached the maximum loop nesting, we'll simply materialize everything we
             * can to make sure that the loops only need to be iterated one time. Care is taken here
             * to not materialize virtual objects that have the "ensureVirtualized" flag set.
             */
            LoopBeginNode __loopBegin = (LoopBeginNode) __loop.getHeader().getBeginNode();
            AbstractEndNode __end = __loopBegin.forwardEnd();
            Block __loopPredecessor = __loop.getHeader().getFirstPredecessor();
            int __length = __initialState.getStateCount();

            boolean __change;
            BitSet __ensureVirtualized = new BitSet(__length);
            for (int __i = 0; __i < __length; __i++)
            {
                ObjectState __state = __initialState.getObjectStateOptional(__i);
                if (__state != null && __state.isVirtual() && __state.getEnsureVirtualized())
                {
                    __ensureVirtualized.set(__i);
                }
            }
            do
            {
                // propagate "ensureVirtualized" flag
                __change = false;
                for (int __i = 0; __i < __length; __i++)
                {
                    if (!__ensureVirtualized.get(__i))
                    {
                        ObjectState __state = __initialState.getObjectStateOptional(__i);
                        if (__state != null && __state.isVirtual())
                        {
                            for (ValueNode __entry : __state.getEntries())
                            {
                                if (__entry instanceof VirtualObjectNode)
                                {
                                    if (__ensureVirtualized.get(((VirtualObjectNode) __entry).getObjectId()))
                                    {
                                        __change = true;
                                        __ensureVirtualized.set(__i);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } while (__change);

            for (int __i = 0; __i < __length; __i++)
            {
                ObjectState __state = __initialState.getObjectStateOptional(__i);
                if (__state != null && __state.isVirtual() && !__ensureVirtualized.get(__i))
                {
                    __initialState.materializeBefore(__end, virtualObjects.get(__i), blockEffects.get(__loopPredecessor));
                }
            }
        }
        return __initialState;
    }

    @Override
    protected void processInitialLoopState(Loop<Block> __loop, BlockT __initialState)
    {
        for (PhiNode __phi : ((LoopBeginNode) __loop.getHeader().getBeginNode()).phis())
        {
            if (__phi.valueAt(0) != null)
            {
                ValueNode __alias = getAliasAndResolve(__initialState, __phi.valueAt(0));
                if (__alias instanceof VirtualObjectNode)
                {
                    VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
                    addVirtualAlias(__virtual, __phi);
                }
                else
                {
                    aliases.set(__phi, null);
                }
            }
        }
    }

    @Override
    protected void processLoopExit(LoopExitNode __exitNode, BlockT __initialState, BlockT __exitState, GraphEffectList __effects)
    {
        if (__exitNode.graph().hasValueProxies())
        {
            EconomicMap<Integer, ProxyNode> __proxies = EconomicMap.create(Equivalence.DEFAULT);
            for (ProxyNode __proxy : __exitNode.proxies())
            {
                ValueNode __alias = getAlias(__proxy.value());
                if (__alias instanceof VirtualObjectNode)
                {
                    VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
                    __proxies.put(__virtual.getObjectId(), __proxy);
                }
            }
            for (int __i = 0; __i < __exitState.getStateCount(); __i++)
            {
                ObjectState __exitObjState = __exitState.getObjectStateOptional(__i);
                if (__exitObjState != null)
                {
                    ObjectState __initialObjState = __initialState.getObjectStateOptional(__i);

                    if (__exitObjState.isVirtual())
                    {
                        processVirtualAtLoopExit(__exitNode, __effects, __i, __exitObjState, __initialObjState, __exitState);
                    }
                    else
                    {
                        processMaterializedAtLoopExit(__exitNode, __effects, __proxies, __i, __exitObjState, __initialObjState, __exitState);
                    }
                }
            }
        }
    }

    private static void processMaterializedAtLoopExit(LoopExitNode __exitNode, GraphEffectList __effects, EconomicMap<Integer, ProxyNode> __proxies, int __object, ObjectState __exitObjState, ObjectState __initialObjState, PartialEscapeBlockState<?> __exitState)
    {
        if (__initialObjState == null || __initialObjState.isVirtual())
        {
            ProxyNode __proxy = __proxies.get(__object);
            if (__proxy == null)
            {
                __proxy = new ValueProxyNode(__exitObjState.getMaterializedValue(), __exitNode);
                __effects.addFloatingNode(__proxy, "proxy");
            }
            else
            {
                __effects.replaceFirstInput(__proxy, __proxy.value(), __exitObjState.getMaterializedValue());
                // nothing to do - will be handled in processNode
            }
            __exitState.updateMaterializedValue(__object, __proxy);
        }
    }

    private static void processVirtualAtLoopExit(LoopExitNode __exitNode, GraphEffectList __effects, int __object, ObjectState __exitObjState, ObjectState __initialObjState, PartialEscapeBlockState<?> __exitState)
    {
        for (int __i = 0; __i < __exitObjState.getEntries().length; __i++)
        {
            ValueNode __value = __exitState.getObjectState(__object).getEntry(__i);
            if (!(__value instanceof VirtualObjectNode || __value.isConstant()))
            {
                if (__exitNode.loopBegin().isPhiAtMerge(__value) || __initialObjState == null || !__initialObjState.isVirtual() || __initialObjState.getEntry(__i) != __value)
                {
                    ProxyNode __proxy = new ValueProxyNode(__value, __exitNode);
                    __exitState.setEntry(__object, __i, __proxy);
                    __effects.addFloatingNode(__proxy, "virtualProxy");
                }
            }
        }
    }

    @Override
    protected MergeProcessor createMergeProcessor(Block __merge)
    {
        return new MergeProcessor(__merge);
    }

    // @class PartialEscapeClosure.MergeProcessor
    // @closure
    protected class MergeProcessor extends EffectsClosure<BlockT>.MergeProcessor
    {
        // @field
        private EconomicMap<Object, ValuePhiNode> materializedPhis;
        // @field
        private EconomicMap<ValueNode, ValuePhiNode[]> valuePhis;
        // @field
        private EconomicMap<ValuePhiNode, VirtualObjectNode> valueObjectVirtuals;
        // @field
        private final boolean needsCaching;

        // @cons
        public MergeProcessor(Block __mergeBlock)
        {
            super(__mergeBlock);
            // merge will only be called multiple times for loop headers
            needsCaching = __mergeBlock.isLoopHeader();
        }

        protected <T> PhiNode getPhi(T __virtual, Stamp __stamp)
        {
            if (needsCaching)
            {
                return getPhiCached(__virtual, __stamp);
            }
            else
            {
                return createValuePhi(__stamp);
            }
        }

        private <T> PhiNode getPhiCached(T __virtual, Stamp __stamp)
        {
            if (materializedPhis == null)
            {
                materializedPhis = EconomicMap.create(Equivalence.DEFAULT);
            }
            ValuePhiNode __result = materializedPhis.get(__virtual);
            if (__result == null)
            {
                __result = createValuePhi(__stamp);
                materializedPhis.put(__virtual, __result);
            }
            return __result;
        }

        private PhiNode[] getValuePhis(ValueNode __key, int __entryCount)
        {
            if (needsCaching)
            {
                return getValuePhisCached(__key, __entryCount);
            }
            else
            {
                return new ValuePhiNode[__entryCount];
            }
        }

        private PhiNode[] getValuePhisCached(ValueNode __key, int __entryCount)
        {
            if (valuePhis == null)
            {
                valuePhis = EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);
            }
            ValuePhiNode[] __result = valuePhis.get(__key);
            if (__result == null)
            {
                __result = new ValuePhiNode[__entryCount];
                valuePhis.put(__key, __result);
            }
            return __result;
        }

        private VirtualObjectNode getValueObjectVirtual(ValuePhiNode __phi, VirtualObjectNode __virtual)
        {
            if (needsCaching)
            {
                return getValueObjectVirtualCached(__phi, __virtual);
            }
            else
            {
                return __virtual.duplicate();
            }
        }

        private VirtualObjectNode getValueObjectVirtualCached(ValuePhiNode __phi, VirtualObjectNode __virtual)
        {
            if (valueObjectVirtuals == null)
            {
                valueObjectVirtuals = EconomicMap.create(Equivalence.IDENTITY);
            }
            VirtualObjectNode __result = valueObjectVirtuals.get(__phi);
            if (__result == null)
            {
                __result = __virtual.duplicate();
                valueObjectVirtuals.put(__phi, __result);
            }
            return __result;
        }

        /**
         * Merge all predecessor block states into one block state. This is an iterative process,
         * because merging states can lead to materializations which make previous parts of the
         * merging operation invalid. The merging process is executed until a stable state has been
         * reached. This method needs to be careful to place the effects of the merging operation
         * into the correct blocks.
         *
         * @param statesList the predecessor block states of the merge
         */
        @Override
        protected void merge(List<BlockT> __statesList)
        {
            PartialEscapeBlockState<?>[] __states = new PartialEscapeBlockState<?>[__statesList.size()];
            for (int __i = 0; __i < __statesList.size(); __i++)
            {
                __states[__i] = __statesList.get(__i);
            }

            // calculate the set of virtual objects that exist in all predecessors
            int[] __virtualObjTemp = intersectVirtualObjects(__states);

            boolean __materialized;
            do
            {
                __materialized = false;

                if (PartialEscapeBlockState.identicalObjectStates(__states))
                {
                    newState.adoptAddObjectStates(__states[0]);
                }
                else
                {
                    for (int __object : __virtualObjTemp)
                    {
                        if (PartialEscapeBlockState.identicalObjectStates(__states, __object))
                        {
                            newState.addObject(__object, __states[0].getObjectState(__object).share());
                            continue;
                        }

                        // determine if all inputs are virtual or the same materialized value
                        int __virtualCount = 0;
                        ObjectState __startObj = __states[0].getObjectState(__object);
                        boolean __locksMatch = true;
                        boolean __ensureVirtual = true;
                        ValueNode __uniqueMaterializedValue = __startObj.isVirtual() ? null : __startObj.getMaterializedValue();
                        for (int __i = 0; __i < __states.length; __i++)
                        {
                            ObjectState __obj = __states[__i].getObjectState(__object);
                            __ensureVirtual &= __obj.getEnsureVirtualized();
                            if (__obj.isVirtual())
                            {
                                __virtualCount++;
                                __uniqueMaterializedValue = null;
                                __locksMatch &= __obj.locksEqual(__startObj);
                            }
                            else if (__obj.getMaterializedValue() != __uniqueMaterializedValue)
                            {
                                __uniqueMaterializedValue = null;
                            }
                        }

                        if (__virtualCount == __states.length && __locksMatch)
                        {
                            __materialized |= mergeObjectStates(__object, null, __states);
                        }
                        else
                        {
                            if (__uniqueMaterializedValue != null)
                            {
                                newState.addObject(__object, new ObjectState(__uniqueMaterializedValue, null, __ensureVirtual));
                            }
                            else
                            {
                                PhiNode __materializedValuePhi = getPhi(__object, StampFactory.forKind(JavaKind.Object));
                                mergeEffects.addFloatingNode(__materializedValuePhi, "materializedPhi");
                                for (int __i = 0; __i < __states.length; __i++)
                                {
                                    ObjectState __obj = __states[__i].getObjectState(__object);
                                    if (__obj.isVirtual())
                                    {
                                        Block __predecessor = getPredecessor(__i);
                                        if (!__ensureVirtual && __obj.isVirtual())
                                        {
                                            // we can materialize if not all inputs are "ensureVirtualized"
                                            __obj.setEnsureVirtualized(false);
                                        }
                                        __materialized |= ensureMaterialized(__states[__i], __object, __predecessor.getEndNode(), blockEffects.get(__predecessor));
                                        __obj = __states[__i].getObjectState(__object);
                                    }
                                    setPhiInput(__materializedValuePhi, __i, __obj.getMaterializedValue());
                                }
                                newState.addObject(__object, new ObjectState(__materializedValuePhi, null, false));
                            }
                        }
                    }
                }

                for (PhiNode __phi : getPhis())
                {
                    aliases.set(__phi, null);
                    if (hasVirtualInputs.isMarked(__phi) && __phi instanceof ValuePhiNode)
                    {
                        __materialized |= processPhi((ValuePhiNode) __phi, __states);
                    }
                }
                if (__materialized)
                {
                    newState.resetObjectStates(virtualObjects.size());
                    mergeEffects.clear();
                    afterMergeEffects.clear();
                }
            } while (__materialized);
        }

        private int[] intersectVirtualObjects(PartialEscapeBlockState<?>[] __states)
        {
            int __length = __states[0].getStateCount();
            for (int __i = 1; __i < __states.length; __i++)
            {
                __length = Math.min(__length, __states[__i].getStateCount());
            }

            int __count = 0;
            for (int __objectIndex = 0; __objectIndex < __length; __objectIndex++)
            {
                if (intersectObjectState(__states, __objectIndex))
                {
                    __count++;
                }
            }

            int __index = 0;
            int[] __resultInts = new int[__count];
            for (int __objectIndex = 0; __objectIndex < __length; __objectIndex++)
            {
                if (intersectObjectState(__states, __objectIndex))
                {
                    __resultInts[__index++] = __objectIndex;
                }
            }
            return __resultInts;
        }

        private boolean intersectObjectState(PartialEscapeBlockState<?>[] __states, int __objectIndex)
        {
            for (int __i = 0; __i < __states.length; __i++)
            {
                PartialEscapeBlockState<?> __state = __states[__i];
                if (__state.getObjectStateOptional(__objectIndex) == null)
                {
                    return false;
                }
            }
            return true;
        }

        /**
         * Try to merge multiple virtual object states into a single object state. If the incoming
         * object states are compatible, then this method will create PhiNodes for the object's
         * entries where needed. If they are incompatible, then all incoming virtual objects will be
         * materialized, and a PhiNode for the materialized values will be created. Object states
         * can be incompatible if they contain {@code long} or {@code double} values occupying two
         * {@code int} slots in such a way that that their values cannot be merged using PhiNodes.
         *
         * @param states the predecessor block states of the merge
         * @return true if materialization happened during the merge, false otherwise
         */
        private boolean mergeObjectStates(int __resultObject, int[] __sourceObjects, PartialEscapeBlockState<?>[] __states)
        {
            boolean __compatible = true;
            boolean __ensureVirtual = true;
            IntUnaryOperator __getObject = __index -> __sourceObjects == null ? __resultObject : __sourceObjects[__index];

            VirtualObjectNode __virtual = virtualObjects.get(__resultObject);
            int __entryCount = __virtual.entryCount();

            // determine all entries that have a two-slot value
            JavaKind[] __twoSlotKinds = null;
            outer: for (int i = 0; i < __states.length; i++)
            {
                ObjectState __objectState = __states[i].getObjectState(__getObject.applyAsInt(i));
                ValueNode[] __entries = __objectState.getEntries();
                int __valueIndex = 0;
                __ensureVirtual &= __objectState.getEnsureVirtualized();
                while (__valueIndex < __entryCount)
                {
                    JavaKind __otherKind = __entries[__valueIndex].getStackKind();
                    JavaKind __entryKind = __virtual.entryKind(__valueIndex);
                    if (__entryKind == JavaKind.Int && __otherKind.needsTwoSlots())
                    {
                        if (__twoSlotKinds == null)
                        {
                            __twoSlotKinds = new JavaKind[__entryCount];
                        }
                        if (__twoSlotKinds[__valueIndex] != null && __twoSlotKinds[__valueIndex] != __otherKind)
                        {
                            __compatible = false;
                            break outer;
                        }
                        __twoSlotKinds[__valueIndex] = __otherKind;
                        // skip the next entry
                        __valueIndex++;
                    }
                    __valueIndex++;
                }
            }
            if (__compatible && __twoSlotKinds != null)
            {
                // if there are two-slot values then make sure the incoming states can be merged
                outer: for (int valueIndex = 0; valueIndex < __entryCount; valueIndex++)
                {
                    if (__twoSlotKinds[valueIndex] != null)
                    {
                        for (int __i = 0; __i < __states.length; __i++)
                        {
                            int __object = __getObject.applyAsInt(__i);
                            ObjectState __objectState = __states[__i].getObjectState(__object);
                            ValueNode __value = __objectState.getEntry(valueIndex);
                            JavaKind __valueKind = __value.getStackKind();
                            if (__valueKind != __twoSlotKinds[valueIndex])
                            {
                                ValueNode __nextValue = __objectState.getEntry(valueIndex + 1);
                                if (__value.isConstant() && __value.asConstant().equals(JavaConstant.INT_0) && __nextValue.isConstant() && __nextValue.asConstant().equals(JavaConstant.INT_0))
                                {
                                    // rewrite to a zero constant of the larger kind
                                    __states[__i].setEntry(__object, valueIndex, ConstantNode.defaultForKind(__twoSlotKinds[valueIndex], graph()));
                                    __states[__i].setEntry(__object, valueIndex + 1, ConstantNode.forConstant(JavaConstant.forIllegal(), tool.getMetaAccessProvider(), graph()));
                                }
                                else
                                {
                                    __compatible = false;
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }

            if (__compatible)
            {
                // virtual objects are compatible: create phis for all entries that need them
                ValueNode[] __values = __states[0].getObjectState(__getObject.applyAsInt(0)).getEntries().clone();
                PhiNode[] __phis = getValuePhis(__virtual, __virtual.entryCount());
                int __valueIndex = 0;
                while (__valueIndex < __values.length)
                {
                    for (int __i = 1; __i < __states.length; __i++)
                    {
                        if (__phis[__valueIndex] == null)
                        {
                            ValueNode __field = __states[__i].getObjectState(__getObject.applyAsInt(__i)).getEntry(__valueIndex);
                            if (__values[__valueIndex] != __field)
                            {
                                __phis[__valueIndex] = createValuePhi(__values[__valueIndex].stamp(NodeView.DEFAULT).unrestricted());
                            }
                        }
                    }
                    if (__phis[__valueIndex] != null && !__phis[__valueIndex].stamp(NodeView.DEFAULT).isCompatible(__values[__valueIndex].stamp(NodeView.DEFAULT)))
                    {
                        __phis[__valueIndex] = createValuePhi(__values[__valueIndex].stamp(NodeView.DEFAULT).unrestricted());
                    }
                    if (__twoSlotKinds != null && __twoSlotKinds[__valueIndex] != null)
                    {
                        // skip an entry after a long/double value that occupies two int slots
                        __valueIndex++;
                        __phis[__valueIndex] = null;
                        __values[__valueIndex] = ConstantNode.forConstant(JavaConstant.forIllegal(), tool.getMetaAccessProvider(), graph());
                    }
                    __valueIndex++;
                }

                boolean __materialized = false;
                for (int __i = 0; __i < __values.length; __i++)
                {
                    PhiNode __phi = __phis[__i];
                    if (__phi != null)
                    {
                        mergeEffects.addFloatingNode(__phi, "virtualMergePhi");
                        if (__virtual.entryKind(__i) == JavaKind.Object)
                        {
                            __materialized |= mergeObjectEntry(__getObject, __states, __phi, __i);
                        }
                        else
                        {
                            for (int __i2 = 0; __i2 < __states.length; __i2++)
                            {
                                ObjectState __state = __states[__i2].getObjectState(__getObject.applyAsInt(__i2));
                                if (!__state.isVirtual())
                                {
                                    break;
                                }
                                setPhiInput(__phi, __i2, __state.getEntry(__i));
                            }
                        }
                        __values[__i] = __phi;
                    }
                }
                newState.addObject(__resultObject, new ObjectState(__values, __states[0].getObjectState(__getObject.applyAsInt(0)).getLocks(), __ensureVirtual));
                return __materialized;
            }
            else
            {
                // not compatible: materialize in all predecessors
                PhiNode __materializedValuePhi = getPhi(__resultObject, StampFactory.forKind(JavaKind.Object));
                for (int __i = 0; __i < __states.length; __i++)
                {
                    Block __predecessor = getPredecessor(__i);
                    if (!__ensureVirtual && __states[__i].getObjectState(__getObject.applyAsInt(__i)).isVirtual())
                    {
                        // we can materialize if not all inputs are "ensureVirtualized"
                        __states[__i].getObjectState(__getObject.applyAsInt(__i)).setEnsureVirtualized(false);
                    }
                    ensureMaterialized(__states[__i], __getObject.applyAsInt(__i), __predecessor.getEndNode(), blockEffects.get(__predecessor));
                    setPhiInput(__materializedValuePhi, __i, __states[__i].getObjectState(__getObject.applyAsInt(__i)).getMaterializedValue());
                }
                newState.addObject(__resultObject, new ObjectState(__materializedValuePhi, null, __ensureVirtual));
                return true;
            }
        }

        /**
         * Fill the inputs of the PhiNode corresponding to one {@link JavaKind#Object} entry in the virtual object.
         *
         * @return true if materialization happened during the merge, false otherwise
         */
        private boolean mergeObjectEntry(IntUnaryOperator __objectIdFunc, PartialEscapeBlockState<?>[] __states, PhiNode __phi, int __entryIndex)
        {
            boolean __materialized = false;
            for (int __i = 0; __i < __states.length; __i++)
            {
                int __object = __objectIdFunc.applyAsInt(__i);
                ObjectState __objectState = __states[__i].getObjectState(__object);
                if (!__objectState.isVirtual())
                {
                    break;
                }
                ValueNode __entry = __objectState.getEntry(__entryIndex);
                if (__entry instanceof VirtualObjectNode)
                {
                    VirtualObjectNode __entryVirtual = (VirtualObjectNode) __entry;
                    Block __predecessor = getPredecessor(__i);
                    __materialized |= ensureMaterialized(__states[__i], __entryVirtual.getObjectId(), __predecessor.getEndNode(), blockEffects.get(__predecessor));
                    __objectState = __states[__i].getObjectState(__object);
                    if (__objectState.isVirtual())
                    {
                        __states[__i].setEntry(__object, __entryIndex, __entry = __states[__i].getObjectState(__entryVirtual.getObjectId()).getMaterializedValue());
                    }
                }
                setPhiInput(__phi, __i, __entry);
            }
            return __materialized;
        }

        /**
         * Examine a PhiNode and try to replace it with merging of virtual objects if all its
         * inputs refer to virtual object states. In order for the merging to happen, all incoming
         * object states need to be compatible and without object identity (meaning that their
         * object identity if not used later on).
         *
         * @param phi the PhiNode that should be processed
         * @param states the predecessor block states of the merge
         * @return true if materialization happened during the merge, false otherwise
         */
        private boolean processPhi(ValuePhiNode __phi, PartialEscapeBlockState<?>[] __states)
        {
            // determine how many inputs are virtual and if they're all the same virtual object
            int __virtualInputs = 0;
            boolean __uniqueVirtualObject = true;
            boolean __ensureVirtual = true;
            VirtualObjectNode[] __virtualObjs = new VirtualObjectNode[__states.length];
            for (int __i = 0; __i < __states.length; __i++)
            {
                ValueNode __alias = getAlias(getPhiValueAt(__phi, __i));
                if (__alias instanceof VirtualObjectNode)
                {
                    VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
                    __virtualObjs[__i] = __virtual;
                    ObjectState __objectState = __states[__i].getObjectStateOptional(__virtual);
                    if (__objectState == null)
                    {
                        return false;
                    }
                    if (__objectState.isVirtual())
                    {
                        if (__virtualObjs[0] != __alias)
                        {
                            __uniqueVirtualObject = false;
                        }
                        __ensureVirtual &= __objectState.getEnsureVirtualized();
                        __virtualInputs++;
                    }
                }
            }
            if (__virtualInputs == __states.length)
            {
                if (__uniqueVirtualObject)
                {
                    // all inputs refer to the same object: just make the phi node an alias
                    addVirtualAlias(__virtualObjs[0], __phi);
                    mergeEffects.deleteNode(__phi);
                    return false;
                }
                else
                {
                    // all inputs are virtual: check if they're compatible and without identity
                    boolean __compatible = true;
                    VirtualObjectNode __firstVirtual = __virtualObjs[0];
                    for (int __i = 0; __i < __states.length; __i++)
                    {
                        VirtualObjectNode __virtual = __virtualObjs[__i];

                        if (!__firstVirtual.type().equals(__virtual.type()) || __firstVirtual.entryCount() != __virtual.entryCount())
                        {
                            __compatible = false;
                            break;
                        }
                        if (!__states[0].getObjectState(__firstVirtual).locksEqual(__states[__i].getObjectState(__virtual)))
                        {
                            __compatible = false;
                            break;
                        }
                    }
                    if (__compatible)
                    {
                        for (int __i = 0; __i < __states.length; __i++)
                        {
                            VirtualObjectNode __virtual = __virtualObjs[__i];
                            // check whether we trivially see that this is the only reference to this allocation
                            if (__virtual.hasIdentity() && !isSingleUsageAllocation(getPhiValueAt(__phi, __i), __virtualObjs, __states[__i]))
                            {
                                __compatible = false;
                            }
                        }
                    }
                    if (__compatible)
                    {
                        VirtualObjectNode __virtual = getValueObjectVirtual(__phi, __virtualObjs[0]);
                        mergeEffects.addFloatingNode(__virtual, "valueObjectNode");
                        mergeEffects.deleteNode(__phi);
                        if (__virtual.getObjectId() == -1)
                        {
                            int __id = virtualObjects.size();
                            virtualObjects.add(__virtual);
                            __virtual.setObjectId(__id);
                        }

                        int[] __virtualObjectIds = new int[__states.length];
                        for (int __i = 0; __i < __states.length; __i++)
                        {
                            __virtualObjectIds[__i] = __virtualObjs[__i].getObjectId();
                        }
                        boolean __materialized = mergeObjectStates(__virtual.getObjectId(), __virtualObjectIds, __states);
                        addVirtualAlias(__virtual, __virtual);
                        addVirtualAlias(__virtual, __phi);
                        return __materialized;
                    }
                }
            }

            // otherwise: materialize all phi inputs
            boolean __materialized = false;
            if (__virtualInputs > 0)
            {
                for (int __i = 0; __i < __states.length; __i++)
                {
                    VirtualObjectNode __virtual = __virtualObjs[__i];
                    if (__virtual != null)
                    {
                        Block __predecessor = getPredecessor(__i);
                        if (!__ensureVirtual && __states[__i].getObjectState(__virtual).isVirtual())
                        {
                            // we can materialize if not all inputs are "ensureVirtualized"
                            __states[__i].getObjectState(__virtual).setEnsureVirtualized(false);
                        }
                        __materialized |= ensureMaterialized(__states[__i], __virtual.getObjectId(), __predecessor.getEndNode(), blockEffects.get(__predecessor));
                    }
                }
            }
            for (int __i = 0; __i < __states.length; __i++)
            {
                VirtualObjectNode __virtual = __virtualObjs[__i];
                if (__virtual != null)
                {
                    setPhiInput(__phi, __i, getAliasAndResolve(__states[__i], __virtual));
                }
            }
            return __materialized;
        }

        private boolean isSingleUsageAllocation(ValueNode __value, VirtualObjectNode[] __virtualObjs, PartialEscapeBlockState<?> __state)
        {
            /*
             * If the phi input is an allocation, we know that it is a "fresh" value, i.e., that
             * this is a value that will only appear through this source, and cannot appear anywhere
             * else. If the phi is also the only usage of this input, we know that no other place
             * can check object identity against it, so it is safe to lose the object identity here.
             */
            if (!(__value instanceof AllocatedObjectNode && __value.hasExactlyOneUsage()))
            {
                return false;
            }

            // Check that the state only references the one virtual object from the Phi.
            VirtualObjectNode __singleVirtual = null;
            for (int __v = 0; __v < __virtualObjs.length; __v++)
            {
                if (__state.contains(__virtualObjs[__v]))
                {
                    if (__singleVirtual == null)
                    {
                        __singleVirtual = __virtualObjs[__v];
                    }
                    else if (__singleVirtual != __virtualObjs[__v])
                    {
                        // More than one virtual object is visible in the object state.
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public ObjectState getObjectState(PartialEscapeBlockState<?> __state, ValueNode __value)
    {
        if (__value == null)
        {
            return null;
        }
        if (__value.isAlive() && !aliases.isNew(__value))
        {
            ValueNode __object = aliases.get(__value);
            return __object instanceof VirtualObjectNode ? __state.getObjectStateOptional((VirtualObjectNode) __object) : null;
        }
        else
        {
            if (__value instanceof VirtualObjectNode)
            {
                return __state.getObjectStateOptional((VirtualObjectNode) __value);
            }
            return null;
        }
    }

    public ValueNode getAlias(ValueNode __value)
    {
        if (__value != null && !(__value instanceof VirtualObjectNode))
        {
            if (__value.isAlive() && !aliases.isNew(__value))
            {
                ValueNode __result = aliases.get(__value);
                if (__result != null)
                {
                    return __result;
                }
            }
        }
        return __value;
    }

    public ValueNode getAliasAndResolve(PartialEscapeBlockState<?> __state, ValueNode __value)
    {
        ValueNode __result = getAlias(__value);
        if (__result instanceof VirtualObjectNode)
        {
            int __id = ((VirtualObjectNode) __result).getObjectId();
            if (__id != -1 && !__state.getObjectState(__id).isVirtual())
            {
                __result = __state.getObjectState(__id).getMaterializedValue();
            }
        }
        return __result;
    }

    void addVirtualAlias(VirtualObjectNode __virtual, ValueNode __node)
    {
        if (__node.isAlive())
        {
            aliases.set(__node, __virtual);
            for (Node __usage : __node.usages())
            {
                markVirtualUsages(__usage);
            }
        }
    }

    private void markVirtualUsages(Node __node)
    {
        if (!hasVirtualInputs.isNew(__node) && !hasVirtualInputs.isMarked(__node))
        {
            hasVirtualInputs.mark(__node);
            if (__node instanceof VirtualState)
            {
                for (Node __usage : __node.usages())
                {
                    markVirtualUsages(__usage);
                }
            }
        }
    }
}
