package giraaff.virtual.phases.ea;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.cfg.BlockMap;
import giraaff.core.common.cfg.Loop;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.NodeMap;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.extended.BoxNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.AllocatedObjectNode;
import giraaff.nodes.virtual.CommitAllocationNode;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.phases.graph.ReentrantBlockIterator;
import giraaff.phases.graph.ReentrantBlockIterator.BlockIteratorClosure;
import giraaff.phases.graph.ReentrantBlockIterator.LoopInfo;
import giraaff.util.GraalError;

// @class EffectsClosure
public abstract class EffectsClosure<BlockT extends EffectsBlockState<BlockT>> extends EffectsPhase.Closure<BlockT>
{
    // @field
    protected final ControlFlowGraph ___cfg;
    // @field
    protected final ScheduleResult ___schedule;

    ///
    // If a node has an alias, this means that it was replaced with another node during analysis.
    // Nodes can be replaced by normal ("scalar") nodes, e.g. a LoadIndexedNode with a ConstantNode,
    // or by virtual nodes, e.g. a NewInstanceNode with a VirtualInstanceNode. A node was replaced
    // with a virtual value iff the alias is a subclass of VirtualObjectNode.
    //
    // This alias map exists only once and is not part of the block state, so that during iterative
    // loop processing the alias of a node may be changed to another value.
    ///
    // @field
    protected final NodeMap<ValueNode> ___aliases;

    ///
    // This set allows for a quick check whether a node has inputs that were replaced with "scalar" values.
    ///
    // @field
    private final NodeBitMap ___hasScalarReplacedInputs;

    // TODO if it was possible to introduce your own subclasses of Block and Loop, these maps would
    // not be necessary. We could merge the GraphEffectsList logic into them.

    ///
    // The effects accumulated during analysis of nodes. They may be cleared and re-filled during
    // iterative loop processing.
    ///
    // @field
    protected final BlockMap<GraphEffectList> ___blockEffects;

    ///
    // Effects that can only be applied after the effects from within the loop have been applied and
    // that must be applied before any effect from after the loop is applied. E.g., updating phis.
    ///
    // @field
    protected final EconomicMap<Loop<Block>, GraphEffectList> ___loopMergeEffects = EconomicMap.create(Equivalence.IDENTITY);

    ///
    // The entry state of loops is needed when loop proxies are processed.
    ///
    // @field
    private final EconomicMap<LoopBeginNode, BlockT> ___loopEntryStates = EconomicMap.create(Equivalence.IDENTITY);

    // Intended to be used by read-eliminating phases based on the effects phase.
    // @field
    protected final EconomicMap<Loop<Block>, LoopKillCache> ___loopLocationKillCache = EconomicMap.create(Equivalence.IDENTITY);

    // @field
    protected boolean ___changed;

    // @cons
    public EffectsClosure(ScheduleResult __schedule, ControlFlowGraph __cfg)
    {
        super();
        this.___schedule = __schedule;
        this.___cfg = __cfg;
        this.___aliases = __cfg.___graph.createNodeMap();
        this.___hasScalarReplacedInputs = __cfg.___graph.createNodeBitMap();
        this.___blockEffects = new BlockMap<>(__cfg);
        for (Block __block : __cfg.getBlocks())
        {
            this.___blockEffects.put(__block, new GraphEffectList());
        }
    }

    @Override
    public boolean hasChanged()
    {
        return this.___changed;
    }

    @Override
    public boolean needsApplyEffects()
    {
        return true;
    }

    @Override
    public void applyEffects()
    {
        final StructuredGraph __graph = this.___cfg.___graph;
        final ArrayList<Node> __obsoleteNodes = new ArrayList<>(0);
        final ArrayList<GraphEffectList> __effectList = new ArrayList<>();

        // Effects are applied during a ordered iteration over the blocks to apply them in the correct
        // order, e.g. apply the effect that adds a node to the graph before the node is used.
        // @closure
        BlockIteratorClosure<Void> __closure = new BlockIteratorClosure<Void>()
        {
            @Override
            protected Void getInitialState()
            {
                return null;
            }

            private void apply(GraphEffectList __effects)
            {
                if (__effects != null && !__effects.isEmpty())
                {
                    __effectList.add(__effects);
                }
            }

            @Override
            protected Void processBlock(Block __block, Void __currentState)
            {
                apply(EffectsClosure.this.___blockEffects.get(__block));
                return __currentState;
            }

            @Override
            protected Void merge(Block __merge, List<Void> __states)
            {
                return null;
            }

            @Override
            protected Void cloneState(Void __oldState)
            {
                return __oldState;
            }

            @Override
            protected List<Void> processLoop(Loop<Block> __loop, Void __initialState)
            {
                LoopInfo<Void> __info = ReentrantBlockIterator.processLoop(this, __loop, __initialState);
                apply(EffectsClosure.this.___loopMergeEffects.get(__loop));
                return __info.___exitStates;
            }
        };
        ReentrantBlockIterator.apply(__closure, this.___cfg.getStartBlock());
        for (GraphEffectList __effects : __effectList)
        {
            __effects.apply(__graph, __obsoleteNodes, false);
        }
        // Effects that modify the cfg (e.g. removing a branch for an if that got a constant condition)
        // need to be performed after all other effects, because they change phi value indexes.
        for (GraphEffectList __effects : __effectList)
        {
            __effects.apply(__graph, __obsoleteNodes, true);
        }
        for (Node __node : __obsoleteNodes)
        {
            if (__node.isAlive() && __node.hasNoUsages())
            {
                __node.replaceAtUsages(null);
                GraphUtil.killWithUnusedFloatingInputs(__node);
            }
        }
    }

    @Override
    protected BlockT processBlock(Block __block, BlockT __state)
    {
        if (!__state.isDead())
        {
            GraphEffectList __effects = this.___blockEffects.get(__block);

            // If we enter an if branch that is known to be unreachable, we mark it as dead and
            // cease to do any more analysis on it. At merges, these dead branches will be ignored.
            if (__block.getBeginNode().predecessor() instanceof IfNode)
            {
                IfNode __ifNode = (IfNode) __block.getBeginNode().predecessor();
                LogicNode __condition = __ifNode.condition();
                Node __alias = getScalarAlias(__condition);
                if (__alias instanceof LogicConstantNode)
                {
                    LogicConstantNode __constant = (LogicConstantNode) __alias;
                    boolean __isTrueSuccessor = __block.getBeginNode() == __ifNode.trueSuccessor();

                    if (__constant.getValue() != __isTrueSuccessor)
                    {
                        __state.markAsDead();
                        __effects.killIfBranch(__ifNode, __constant.getValue());
                        return __state;
                    }
                }
            }

            // a lastFixedNode is needed in case we want to insert fixed nodes
            FixedWithNextNode __lastFixedNode = null;
            Iterable<? extends Node> __nodes = this.___schedule != null ? this.___schedule.getBlockToNodesMap().get(__block) : __block.getNodes();
            for (Node __node : __nodes)
            {
                // reset the aliases (may be non-null due to iterative loop processing)
                this.___aliases.set(__node, null);
                if (__node instanceof LoopExitNode)
                {
                    LoopExitNode __loopExit = (LoopExitNode) __node;
                    for (ProxyNode __proxy : __loopExit.proxies())
                    {
                        this.___aliases.set(__proxy, null);
                        this.___changed |= processNode(__proxy, __state, __effects, __lastFixedNode) && isSignificantNode(__node);
                    }
                    processLoopExit(__loopExit, this.___loopEntryStates.get(__loopExit.loopBegin()), __state, this.___blockEffects.get(__block));
                }
                this.___changed |= processNode(__node, __state, __effects, __lastFixedNode) && isSignificantNode(__node);
                if (__node instanceof FixedWithNextNode)
                {
                    __lastFixedNode = (FixedWithNextNode) __node;
                }
                if (__state.isDead())
                {
                    break;
                }
            }
        }
        return __state;
    }

    ///
    // Changes to {@link CommitAllocationNode}s, {@link AllocatedObjectNode}s and {@link BoxNode}s
    // are not considered to be "important". If only changes to those nodes are discovered during
    // analysis, the effects need not be applied.
    ///
    private static boolean isSignificantNode(Node __node)
    {
        return !(__node instanceof CommitAllocationNode || __node instanceof AllocatedObjectNode || __node instanceof BoxNode);
    }

    ///
    // Collects the effects of virtualizing the given node.
    //
    // @return {@code true} if the effects include removing the node, {@code false} otherwise.
    ///
    protected abstract boolean processNode(Node __node, BlockT __state, GraphEffectList __effects, FixedWithNextNode __lastFixedNode);

    @Override
    protected BlockT merge(Block __merge, List<BlockT> __states)
    {
        MergeProcessor __processor = createMergeProcessor(__merge);
        doMergeWithoutDead(__processor, __states);
        this.___blockEffects.get(__merge).addAll(__processor.___mergeEffects);
        this.___blockEffects.get(__merge).addAll(__processor.___afterMergeEffects);
        return __processor.___newState;
    }

    @Override
    protected final List<BlockT> processLoop(Loop<Block> __loop, BlockT __initialState)
    {
        if (__initialState.isDead())
        {
            ArrayList<BlockT> __states = new ArrayList<>();
            for (int __i = 0; __i < __loop.getExits().size(); __i++)
            {
                __states.add(__initialState);
            }
            return __states;
        }
        // Special case nested loops: To avoid an exponential runtime for nested loops we try to
        // only process them as little times as possible.
        //
        // In the first iteration of an outer most loop we go into the inner most loop(s). We run
        // the first iteration of the inner most loop and then, if necessary, a second iteration.
        //
        // We return from the recursion and finish the first iteration of the outermost loop. If we
        // have to do a second iteration in the outer most loop we go again into the inner most
        // loop(s) but this time we already know all states that are killed by the loop so inside
        // the loop we will only have those changes that propagate from the first iteration of the
        // outer most loop into the current loop. We strip the initial loop state for the inner most
        // loops and do the first iteration with the (possible) changes from outer loops. If there
        // are no changes we only have to do 1 iteration and are done.
        BlockT __initialStateRemovedKilledLocations = stripKilledLoopLocations(__loop, cloneState(__initialState));
        BlockT __loopEntryState = __initialStateRemovedKilledLocations;
        BlockT __lastMergedState = cloneState(__initialStateRemovedKilledLocations);
        processInitialLoopState(__loop, __lastMergedState);
        MergeProcessor __mergeProcessor = createMergeProcessor(__loop.getHeader());
        // Iterative loop processing: we take the predecessor state as the loop's starting state,
        // processing the loop contents, merge the states of all loop ends, and check whether the
        // resulting state is equal to the starting state. If it is, the loop processing has
        // finished, if not, another iteration is needed.
        //
        // This processing converges because the merge processing always makes the starting state
        // more generic, e.g. adding phis instead of non-phi values.
        for (int __iteration = 0; __iteration < 10; __iteration++)
        {
            LoopInfo<BlockT> __info = ReentrantBlockIterator.processLoop(this, __loop, cloneState(__lastMergedState));

            List<BlockT> __states = new ArrayList<>();
            __states.add(__initialStateRemovedKilledLocations);
            __states.addAll(__info.___endStates);
            doMergeWithoutDead(__mergeProcessor, __states);

            if (__mergeProcessor.___newState.equivalentTo(__lastMergedState))
            {
                this.___blockEffects.get(__loop.getHeader()).insertAll(__mergeProcessor.___mergeEffects, 0);
                this.___loopMergeEffects.put(__loop, __mergeProcessor.___afterMergeEffects);

                this.___loopEntryStates.put((LoopBeginNode) __loop.getHeader().getBeginNode(), __loopEntryState);

                processKilledLoopLocations(__loop, __initialStateRemovedKilledLocations, __mergeProcessor.___newState);
                return __info.___exitStates;
            }
            else
            {
                __lastMergedState = __mergeProcessor.___newState;
                for (Block __block : __loop.getBlocks())
                {
                    this.___blockEffects.get(__block).clear();
                }
            }
        }
        throw new GraalError("too many iterations at %s", __loop);
    }

    @SuppressWarnings("unused")
    protected BlockT stripKilledLoopLocations(Loop<Block> __loop, BlockT __initialState)
    {
        return __initialState;
    }

    @SuppressWarnings("unused")
    protected void processKilledLoopLocations(Loop<Block> __loop, BlockT __initialState, BlockT __mergedStates)
    {
        // nothing to do
    }

    @SuppressWarnings("unused")
    protected void processInitialLoopState(Loop<Block> __loop, BlockT __initialState)
    {
        // nothing to do
    }

    private void doMergeWithoutDead(MergeProcessor __mergeProcessor, List<BlockT> __states)
    {
        int __alive = 0;
        for (BlockT __state : __states)
        {
            if (!__state.isDead())
            {
                __alive++;
            }
        }
        if (__alive == 0)
        {
            __mergeProcessor.setNewState(__states.get(0));
        }
        else if (__alive == __states.size())
        {
            int[] __stateIndexes = new int[__states.size()];
            for (int __i = 0; __i < __stateIndexes.length; __i++)
            {
                __stateIndexes[__i] = __i;
            }
            __mergeProcessor.setStateIndexes(__stateIndexes);
            __mergeProcessor.setNewState(getInitialState());
            __mergeProcessor.merge(__states);
        }
        else
        {
            ArrayList<BlockT> __aliveStates = new ArrayList<>(__alive);
            int[] __stateIndexes = new int[__alive];
            for (int __i = 0; __i < __states.size(); __i++)
            {
                if (!__states.get(__i).isDead())
                {
                    __stateIndexes[__aliveStates.size()] = __i;
                    __aliveStates.add(__states.get(__i));
                }
            }
            __mergeProcessor.setStateIndexes(__stateIndexes);
            __mergeProcessor.setNewState(getInitialState());
            __mergeProcessor.merge(__aliveStates);
        }
    }

    protected abstract void processLoopExit(LoopExitNode __exitNode, BlockT __initialState, BlockT __exitState, GraphEffectList __effects);

    protected abstract MergeProcessor createMergeProcessor(Block __merge);

    ///
    // The main workhorse for merging states, both for loops and for normal merges.
    ///
    // @class EffectsClosure.MergeProcessor
    // @closure
    protected abstract class MergeProcessor
    {
        // @field
        private final Block ___mergeBlock;
        // @field
        private final AbstractMergeNode ___merge;

        // @field
        protected final GraphEffectList ___mergeEffects = new GraphEffectList();
        // @field
        protected final GraphEffectList ___afterMergeEffects = new GraphEffectList();

        ///
        // The indexes are used to map from an index in the list of active (non-dead) predecessors
        // to an index in the list of all predecessors (the latter may be larger).
        ///
        // @field
        private int[] ___stateIndexes;
        // @field
        protected BlockT ___newState;

        // @cons
        public MergeProcessor(Block __mergeBlock)
        {
            super();
            this.___mergeBlock = __mergeBlock;
            this.___merge = (AbstractMergeNode) __mergeBlock.getBeginNode();
        }

        ///
        // @param states the states that should be merged.
        ///
        protected abstract void merge(List<BlockT> __states);

        private void setNewState(BlockT __state)
        {
            this.___newState = __state;
            this.___mergeEffects.clear();
            this.___afterMergeEffects.clear();
        }

        private void setStateIndexes(int[] __stateIndexes)
        {
            this.___stateIndexes = __stateIndexes;
        }

        protected final Block getPredecessor(int __index)
        {
            return this.___mergeBlock.getPredecessors()[this.___stateIndexes[__index]];
        }

        protected final NodeIterable<PhiNode> getPhis()
        {
            return this.___merge.phis();
        }

        protected final ValueNode getPhiValueAt(PhiNode __phi, int __index)
        {
            return __phi.valueAt(this.___stateIndexes[__index]);
        }

        protected final ValuePhiNode createValuePhi(Stamp __stamp)
        {
            return new ValuePhiNode(__stamp, this.___merge, new ValueNode[this.___mergeBlock.getPredecessorCount()]);
        }

        protected final void setPhiInput(PhiNode __phi, int __index, ValueNode __value)
        {
            this.___afterMergeEffects.initializePhiInput(__phi, this.___stateIndexes[__index], __value);
        }

        protected final StructuredGraph graph()
        {
            return this.___merge.graph();
        }
    }

    public void addScalarAlias(ValueNode __node, ValueNode __alias)
    {
        this.___aliases.set(__node, __alias);
        for (Node __usage : __node.usages())
        {
            if (!this.___hasScalarReplacedInputs.isNew(__usage))
            {
                this.___hasScalarReplacedInputs.mark(__usage);
            }
        }
    }

    protected final boolean hasScalarReplacedInputs(Node __node)
    {
        return this.___hasScalarReplacedInputs.isMarked(__node);
    }

    public ValueNode getScalarAlias(ValueNode __node)
    {
        if (__node == null || !__node.isAlive() || this.___aliases.isNew(__node))
        {
            return __node;
        }
        ValueNode __result = this.___aliases.get(__node);
        return (__result == null || __result instanceof VirtualObjectNode) ? __node : __result;
    }

    // @class EffectsClosure.LoopKillCache
    protected static final class LoopKillCache
    {
        // @field
        private int ___visits;
        // @field
        private LocationIdentity ___firstLocation;
        // @field
        private EconomicSet<LocationIdentity> ___killedLocations;
        // @field
        private boolean ___killsAll;

        // @cons
        protected LoopKillCache(int __visits)
        {
            super();
            this.___visits = __visits;
        }

        protected void visited()
        {
            this.___visits++;
        }

        protected int visits()
        {
            return this.___visits;
        }

        protected void setKillsAll()
        {
            this.___killsAll = true;
            this.___firstLocation = null;
            this.___killedLocations = null;
        }

        protected boolean containsLocation(LocationIdentity __locationIdentity)
        {
            if (this.___killsAll)
            {
                return true;
            }
            if (this.___firstLocation == null)
            {
                return false;
            }
            if (!this.___firstLocation.equals(__locationIdentity))
            {
                return this.___killedLocations != null ? this.___killedLocations.contains(__locationIdentity) : false;
            }
            return true;
        }

        protected void rememberLoopKilledLocation(LocationIdentity __locationIdentity)
        {
            if (this.___killsAll)
            {
                return;
            }
            if (this.___firstLocation == null || this.___firstLocation.equals(__locationIdentity))
            {
                this.___firstLocation = __locationIdentity;
            }
            else
            {
                if (this.___killedLocations == null)
                {
                    this.___killedLocations = EconomicSet.create(Equivalence.IDENTITY);
                }
                this.___killedLocations.add(__locationIdentity);
            }
        }

        protected boolean loopKillsLocations()
        {
            if (this.___killsAll)
            {
                return true;
            }
            return this.___firstLocation != null;
        }
    }
}
