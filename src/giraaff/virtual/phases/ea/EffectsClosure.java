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
import giraaff.options.OptionValues;
import giraaff.phases.graph.ReentrantBlockIterator;
import giraaff.phases.graph.ReentrantBlockIterator.BlockIteratorClosure;
import giraaff.phases.graph.ReentrantBlockIterator.LoopInfo;
import giraaff.util.GraalError;

// @class EffectsClosure
public abstract class EffectsClosure<BlockT extends EffectsBlockState<BlockT>> extends EffectsPhase.Closure<BlockT>
{
    protected final ControlFlowGraph cfg;
    protected final ScheduleResult schedule;

    /**
     * If a node has an alias, this means that it was replaced with another node during analysis.
     * Nodes can be replaced by normal ("scalar") nodes, e.g. a LoadIndexedNode with a ConstantNode,
     * or by virtual nodes, e.g. a NewInstanceNode with a VirtualInstanceNode. A node was replaced
     * with a virtual value iff the alias is a subclass of VirtualObjectNode.
     *
     * This alias map exists only once and is not part of the block state, so that during iterative
     * loop processing the alias of a node may be changed to another value.
     */
    protected final NodeMap<ValueNode> aliases;

    /**
     * This set allows for a quick check whether a node has inputs that were replaced with "scalar" values.
     */
    private final NodeBitMap hasScalarReplacedInputs;

    /*
     * TODO if it was possible to introduce your own subclasses of Block and Loop, these maps would
     * not be necessary. We could merge the GraphEffectsList logic into them.
     */

    /**
     * The effects accumulated during analysis of nodes. They may be cleared and re-filled during
     * iterative loop processing.
     */
    protected final BlockMap<GraphEffectList> blockEffects;

    /**
     * Effects that can only be applied after the effects from within the loop have been applied and
     * that must be applied before any effect from after the loop is applied. E.g., updating phis.
     */
    protected final EconomicMap<Loop<Block>, GraphEffectList> loopMergeEffects = EconomicMap.create(Equivalence.IDENTITY);

    /**
     * The entry state of loops is needed when loop proxies are processed.
     */
    private final EconomicMap<LoopBeginNode, BlockT> loopEntryStates = EconomicMap.create(Equivalence.IDENTITY);

    // Intended to be used by read-eliminating phases based on the effects phase.
    protected final EconomicMap<Loop<Block>, LoopKillCache> loopLocationKillCache = EconomicMap.create(Equivalence.IDENTITY);

    protected boolean changed;

    // @cons
    public EffectsClosure(ScheduleResult schedule, ControlFlowGraph cfg)
    {
        super();
        this.schedule = schedule;
        this.cfg = cfg;
        this.aliases = cfg.graph.createNodeMap();
        this.hasScalarReplacedInputs = cfg.graph.createNodeBitMap();
        this.blockEffects = new BlockMap<>(cfg);
        for (Block block : cfg.getBlocks())
        {
            blockEffects.put(block, new GraphEffectList());
        }
    }

    @Override
    public boolean hasChanged()
    {
        return changed;
    }

    @Override
    public boolean needsApplyEffects()
    {
        return true;
    }

    @Override
    public void applyEffects()
    {
        final StructuredGraph graph = cfg.graph;
        final ArrayList<Node> obsoleteNodes = new ArrayList<>(0);
        final ArrayList<GraphEffectList> effectList = new ArrayList<>();

        // Effects are applied during a ordered iteration over the blocks to apply them in the correct
        // order, e.g. apply the effect that adds a node to the graph before the node is used.
        BlockIteratorClosure<Void> closure = new BlockIteratorClosure<Void>()
        {
            @Override
            protected Void getInitialState()
            {
                return null;
            }

            private void apply(GraphEffectList effects)
            {
                if (effects != null && !effects.isEmpty())
                {
                    effectList.add(effects);
                }
            }

            @Override
            protected Void processBlock(Block block, Void currentState)
            {
                apply(blockEffects.get(block));
                return currentState;
            }

            @Override
            protected Void merge(Block merge, List<Void> states)
            {
                return null;
            }

            @Override
            protected Void cloneState(Void oldState)
            {
                return oldState;
            }

            @Override
            protected List<Void> processLoop(Loop<Block> loop, Void initialState)
            {
                LoopInfo<Void> info = ReentrantBlockIterator.processLoop(this, loop, initialState);
                apply(loopMergeEffects.get(loop));
                return info.exitStates;
            }
        };
        ReentrantBlockIterator.apply(closure, cfg.getStartBlock());
        for (GraphEffectList effects : effectList)
        {
            effects.apply(graph, obsoleteNodes, false);
        }
        // Effects that modify the cfg (e.g. removing a branch for an if that got a constant condition)
        // need to be performed after all other effects, because they change phi value indexes.
        for (GraphEffectList effects : effectList)
        {
            effects.apply(graph, obsoleteNodes, true);
        }
        for (Node node : obsoleteNodes)
        {
            if (node.isAlive() && node.hasNoUsages())
            {
                node.replaceAtUsages(null);
                GraphUtil.killWithUnusedFloatingInputs(node);
            }
        }
    }

    @Override
    protected BlockT processBlock(Block block, BlockT state)
    {
        if (!state.isDead())
        {
            GraphEffectList effects = blockEffects.get(block);

            /*
             * If we enter an if branch that is known to be unreachable, we mark it as dead and
             * cease to do any more analysis on it. At merges, these dead branches will be ignored.
             */
            if (block.getBeginNode().predecessor() instanceof IfNode)
            {
                IfNode ifNode = (IfNode) block.getBeginNode().predecessor();
                LogicNode condition = ifNode.condition();
                Node alias = getScalarAlias(condition);
                if (alias instanceof LogicConstantNode)
                {
                    LogicConstantNode constant = (LogicConstantNode) alias;
                    boolean isTrueSuccessor = block.getBeginNode() == ifNode.trueSuccessor();

                    if (constant.getValue() != isTrueSuccessor)
                    {
                        state.markAsDead();
                        effects.killIfBranch(ifNode, constant.getValue());
                        return state;
                    }
                }
            }

            OptionValues options = block.getBeginNode().getOptions();

            // a lastFixedNode is needed in case we want to insert fixed nodes
            FixedWithNextNode lastFixedNode = null;
            Iterable<? extends Node> nodes = schedule != null ? schedule.getBlockToNodesMap().get(block) : block.getNodes();
            for (Node node : nodes)
            {
                // reset the aliases (may be non-null due to iterative loop processing)
                aliases.set(node, null);
                if (node instanceof LoopExitNode)
                {
                    LoopExitNode loopExit = (LoopExitNode) node;
                    for (ProxyNode proxy : loopExit.proxies())
                    {
                        aliases.set(proxy, null);
                        changed |= processNode(proxy, state, effects, lastFixedNode) && isSignificantNode(node);
                    }
                    processLoopExit(loopExit, loopEntryStates.get(loopExit.loopBegin()), state, blockEffects.get(block));
                }
                changed |= processNode(node, state, effects, lastFixedNode) && isSignificantNode(node);
                if (node instanceof FixedWithNextNode)
                {
                    lastFixedNode = (FixedWithNextNode) node;
                }
                if (state.isDead())
                {
                    break;
                }
            }
        }
        return state;
    }

    /**
     * Changes to {@link CommitAllocationNode}s, {@link AllocatedObjectNode}s and {@link BoxNode}s
     * are not considered to be "important". If only changes to those nodes are discovered during
     * analysis, the effects need not be applied.
     */
    private static boolean isSignificantNode(Node node)
    {
        return !(node instanceof CommitAllocationNode || node instanceof AllocatedObjectNode || node instanceof BoxNode);
    }

    /**
     * Collects the effects of virtualizing the given node.
     *
     * @return {@code true} if the effects include removing the node, {@code false} otherwise.
     */
    protected abstract boolean processNode(Node node, BlockT state, GraphEffectList effects, FixedWithNextNode lastFixedNode);

    @Override
    protected BlockT merge(Block merge, List<BlockT> states)
    {
        MergeProcessor processor = createMergeProcessor(merge);
        doMergeWithoutDead(processor, states);
        blockEffects.get(merge).addAll(processor.mergeEffects);
        blockEffects.get(merge).addAll(processor.afterMergeEffects);
        return processor.newState;
    }

    @Override
    protected final List<BlockT> processLoop(Loop<Block> loop, BlockT initialState)
    {
        if (initialState.isDead())
        {
            ArrayList<BlockT> states = new ArrayList<>();
            for (int i = 0; i < loop.getExits().size(); i++)
            {
                states.add(initialState);
            }
            return states;
        }
        /*
         * Special case nested loops: To avoid an exponential runtime for nested loops we try to
         * only process them as little times as possible.
         *
         * In the first iteration of an outer most loop we go into the inner most loop(s). We run
         * the first iteration of the inner most loop and then, if necessary, a second iteration.
         *
         * We return from the recursion and finish the first iteration of the outermost loop. If we
         * have to do a second iteration in the outer most loop we go again into the inner most
         * loop(s) but this time we already know all states that are killed by the loop so inside
         * the loop we will only have those changes that propagate from the first iteration of the
         * outer most loop into the current loop. We strip the initial loop state for the inner most
         * loops and do the first iteration with the (possible) changes from outer loops. If there
         * are no changes we only have to do 1 iteration and are done.
         */
        BlockT initialStateRemovedKilledLocations = stripKilledLoopLocations(loop, cloneState(initialState));
        BlockT loopEntryState = initialStateRemovedKilledLocations;
        BlockT lastMergedState = cloneState(initialStateRemovedKilledLocations);
        processInitialLoopState(loop, lastMergedState);
        MergeProcessor mergeProcessor = createMergeProcessor(loop.getHeader());
        /*
         * Iterative loop processing: we take the predecessor state as the loop's starting state,
         * processing the loop contents, merge the states of all loop ends, and check whether the
         * resulting state is equal to the starting state. If it is, the loop processing has
         * finished, if not, another iteration is needed.
         *
         * This processing converges because the merge processing always makes the starting state
         * more generic, e.g. adding phis instead of non-phi values.
         */
        for (int iteration = 0; iteration < 10; iteration++)
        {
            LoopInfo<BlockT> info = ReentrantBlockIterator.processLoop(this, loop, cloneState(lastMergedState));

            List<BlockT> states = new ArrayList<>();
            states.add(initialStateRemovedKilledLocations);
            states.addAll(info.endStates);
            doMergeWithoutDead(mergeProcessor, states);

            if (mergeProcessor.newState.equivalentTo(lastMergedState))
            {
                blockEffects.get(loop.getHeader()).insertAll(mergeProcessor.mergeEffects, 0);
                loopMergeEffects.put(loop, mergeProcessor.afterMergeEffects);

                loopEntryStates.put((LoopBeginNode) loop.getHeader().getBeginNode(), loopEntryState);

                processKilledLoopLocations(loop, initialStateRemovedKilledLocations, mergeProcessor.newState);
                return info.exitStates;
            }
            else
            {
                lastMergedState = mergeProcessor.newState;
                for (Block block : loop.getBlocks())
                {
                    blockEffects.get(block).clear();
                }
            }
        }
        throw new GraalError("too many iterations at %s", loop);
    }

    @SuppressWarnings("unused")
    protected BlockT stripKilledLoopLocations(Loop<Block> loop, BlockT initialState)
    {
        return initialState;
    }

    @SuppressWarnings("unused")
    protected void processKilledLoopLocations(Loop<Block> loop, BlockT initialState, BlockT mergedStates)
    {
        // nothing to do
    }

    @SuppressWarnings("unused")
    protected void processInitialLoopState(Loop<Block> loop, BlockT initialState)
    {
        // nothing to do
    }

    private void doMergeWithoutDead(MergeProcessor mergeProcessor, List<BlockT> states)
    {
        int alive = 0;
        for (BlockT state : states)
        {
            if (!state.isDead())
            {
                alive++;
            }
        }
        if (alive == 0)
        {
            mergeProcessor.setNewState(states.get(0));
        }
        else if (alive == states.size())
        {
            int[] stateIndexes = new int[states.size()];
            for (int i = 0; i < stateIndexes.length; i++)
            {
                stateIndexes[i] = i;
            }
            mergeProcessor.setStateIndexes(stateIndexes);
            mergeProcessor.setNewState(getInitialState());
            mergeProcessor.merge(states);
        }
        else
        {
            ArrayList<BlockT> aliveStates = new ArrayList<>(alive);
            int[] stateIndexes = new int[alive];
            for (int i = 0; i < states.size(); i++)
            {
                if (!states.get(i).isDead())
                {
                    stateIndexes[aliveStates.size()] = i;
                    aliveStates.add(states.get(i));
                }
            }
            mergeProcessor.setStateIndexes(stateIndexes);
            mergeProcessor.setNewState(getInitialState());
            mergeProcessor.merge(aliveStates);
        }
    }

    protected abstract void processLoopExit(LoopExitNode exitNode, BlockT initialState, BlockT exitState, GraphEffectList effects);

    protected abstract MergeProcessor createMergeProcessor(Block merge);

    /**
     * The main workhorse for merging states, both for loops and for normal merges.
     */
    // @class EffectsClosure.MergeProcessor
    protected abstract class MergeProcessor
    {
        private final Block mergeBlock;
        private final AbstractMergeNode merge;

        protected final GraphEffectList mergeEffects;
        protected final GraphEffectList afterMergeEffects;

        /**
         * The indexes are used to map from an index in the list of active (non-dead) predecessors
         * to an index in the list of all predecessors (the latter may be larger).
         */
        private int[] stateIndexes;
        protected BlockT newState;

        // @cons
        public MergeProcessor(Block mergeBlock)
        {
            super();
            this.mergeBlock = mergeBlock;
            this.merge = (AbstractMergeNode) mergeBlock.getBeginNode();
            this.mergeEffects = new GraphEffectList();
            this.afterMergeEffects = new GraphEffectList();
        }

        /**
         * @param states the states that should be merged.
         */
        protected abstract void merge(List<BlockT> states);

        private void setNewState(BlockT state)
        {
            newState = state;
            mergeEffects.clear();
            afterMergeEffects.clear();
        }

        private void setStateIndexes(int[] stateIndexes)
        {
            this.stateIndexes = stateIndexes;
        }

        protected final Block getPredecessor(int index)
        {
            return mergeBlock.getPredecessors()[stateIndexes[index]];
        }

        protected final NodeIterable<PhiNode> getPhis()
        {
            return merge.phis();
        }

        protected final ValueNode getPhiValueAt(PhiNode phi, int index)
        {
            return phi.valueAt(stateIndexes[index]);
        }

        protected final ValuePhiNode createValuePhi(Stamp stamp)
        {
            return new ValuePhiNode(stamp, merge, new ValueNode[mergeBlock.getPredecessorCount()]);
        }

        protected final void setPhiInput(PhiNode phi, int index, ValueNode value)
        {
            afterMergeEffects.initializePhiInput(phi, stateIndexes[index], value);
        }

        protected final StructuredGraph graph()
        {
            return merge.graph();
        }

        @Override
        public String toString()
        {
            return "MergeProcessor@" + merge;
        }
    }

    public void addScalarAlias(ValueNode node, ValueNode alias)
    {
        aliases.set(node, alias);
        for (Node usage : node.usages())
        {
            if (!hasScalarReplacedInputs.isNew(usage))
            {
                hasScalarReplacedInputs.mark(usage);
            }
        }
    }

    protected final boolean hasScalarReplacedInputs(Node node)
    {
        return hasScalarReplacedInputs.isMarked(node);
    }

    public ValueNode getScalarAlias(ValueNode node)
    {
        if (node == null || !node.isAlive() || aliases.isNew(node))
        {
            return node;
        }
        ValueNode result = aliases.get(node);
        return (result == null || result instanceof VirtualObjectNode) ? node : result;
    }

    // @class EffectsClosure.LoopKillCache
    protected static final class LoopKillCache
    {
        private int visits;
        private LocationIdentity firstLocation;
        private EconomicSet<LocationIdentity> killedLocations;
        private boolean killsAll;

        // @cons
        protected LoopKillCache(int visits)
        {
            super();
            this.visits = visits;
        }

        protected void visited()
        {
            visits++;
        }

        protected int visits()
        {
            return visits;
        }

        protected void setKillsAll()
        {
            killsAll = true;
            firstLocation = null;
            killedLocations = null;
        }

        protected boolean containsLocation(LocationIdentity locationIdentity)
        {
            if (killsAll)
            {
                return true;
            }
            if (firstLocation == null)
            {
                return false;
            }
            if (!firstLocation.equals(locationIdentity))
            {
                return killedLocations != null ? killedLocations.contains(locationIdentity) : false;
            }
            return true;
        }

        protected void rememberLoopKilledLocation(LocationIdentity locationIdentity)
        {
            if (killsAll)
            {
                return;
            }
            if (firstLocation == null || firstLocation.equals(locationIdentity))
            {
                firstLocation = locationIdentity;
            }
            else
            {
                if (killedLocations == null)
                {
                    killedLocations = EconomicSet.create(Equivalence.IDENTITY);
                }
                killedLocations.add(locationIdentity);
            }
        }

        protected boolean loopKillsLocations()
        {
            if (killsAll)
            {
                return true;
            }
            return firstLocation != null;
        }
    }
}
