package giraaff.phases.schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.core.common.cfg.BlockMap;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.NodeMap;
import giraaff.graph.NodeStack;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.KillingBeginNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StaticDeoptimizingNode;
import giraaff.nodes.StaticDeoptimizingNode.GuardPriority;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConvertNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.cfg.HIRLoop;
import giraaff.nodes.cfg.LocationSet;
import giraaff.nodes.memory.FloatingReadNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.ValueProxy;
import giraaff.phases.Phase;

// @class SchedulePhase
public final class SchedulePhase extends Phase
{
    // @enum SchedulePhase.SchedulingStrategy
    public enum SchedulingStrategy
    {
        EARLIEST_WITH_GUARD_ORDER,
        EARLIEST,
        LATEST,
        LATEST_OUT_OF_LOOPS,
        FINAL_SCHEDULE;

        public boolean isEarliest()
        {
            return this == EARLIEST || this == EARLIEST_WITH_GUARD_ORDER;
        }

        public boolean isLatest()
        {
            return !isEarliest();
        }
    }

    // @field
    private final SchedulingStrategy ___selectedStrategy;

    // @field
    private final boolean ___immutableGraph;

    // @cons
    public SchedulePhase()
    {
        this(false);
    }

    // @cons
    public SchedulePhase(boolean __immutableGraph)
    {
        this(GraalOptions.optScheduleOutOfLoops ? SchedulingStrategy.LATEST_OUT_OF_LOOPS : SchedulingStrategy.LATEST, __immutableGraph);
    }

    // @cons
    public SchedulePhase(SchedulingStrategy __strategy)
    {
        this(__strategy, false);
    }

    // @cons
    public SchedulePhase(SchedulingStrategy __strategy, boolean __immutableGraph)
    {
        super();
        this.___selectedStrategy = __strategy;
        this.___immutableGraph = __immutableGraph;
    }

    @Override
    protected void run(StructuredGraph __graph)
    {
        Instance __inst = new Instance();
        __inst.run(__graph, this.___selectedStrategy, this.___immutableGraph);
    }

    public static void run(StructuredGraph __graph, SchedulingStrategy __strategy, ControlFlowGraph __cfg)
    {
        Instance __inst = new Instance(__cfg);
        __inst.run(__graph, __strategy, false);
    }

    // @class SchedulePhase.Instance
    public static final class Instance
    {
        // @def
        private static final double IMPLICIT_NULL_CHECK_OPPORTUNITY_PROBABILITY_FACTOR = 2;
        ///
        // Map from blocks to the nodes in each block.
        ///
        // @field
        protected ControlFlowGraph ___cfg;
        // @field
        protected BlockMap<List<Node>> ___blockToNodesMap;
        // @field
        protected NodeMap<Block> ___nodeToBlockMap;

        // @cons
        public Instance()
        {
            this(null);
        }

        // @cons
        public Instance(ControlFlowGraph __cfg)
        {
            super();
            this.___cfg = __cfg;
        }

        public void run(StructuredGraph __graph, SchedulingStrategy __selectedStrategy, boolean __immutableGraph)
        {
            if (this.___cfg == null)
            {
                this.___cfg = ControlFlowGraph.compute(__graph, true, true, true, false);
            }

            NodeMap<Block> __currentNodeMap = __graph.createNodeMap();
            NodeBitMap __visited = __graph.createNodeBitMap();
            BlockMap<List<Node>> __earliestBlockToNodesMap = new BlockMap<>(this.___cfg);
            this.___nodeToBlockMap = __currentNodeMap;
            this.___blockToNodesMap = __earliestBlockToNodesMap;

            scheduleEarliestIterative(__earliestBlockToNodesMap, __currentNodeMap, __visited, __graph, __immutableGraph, __selectedStrategy == SchedulingStrategy.EARLIEST_WITH_GUARD_ORDER);

            if (!__selectedStrategy.isEarliest())
            {
                // For non-earliest schedules, we need to do a second pass.
                BlockMap<List<Node>> __latestBlockToNodesMap = new BlockMap<>(this.___cfg);
                for (Block __b : this.___cfg.getBlocks())
                {
                    __latestBlockToNodesMap.put(__b, new ArrayList<>());
                }

                BlockMap<ArrayList<FloatingReadNode>> __watchListMap = calcLatestBlocks(__selectedStrategy, __currentNodeMap, __earliestBlockToNodesMap, __visited, __latestBlockToNodesMap, __immutableGraph);
                sortNodesLatestWithinBlock(this.___cfg, __earliestBlockToNodesMap, __latestBlockToNodesMap, __currentNodeMap, __watchListMap, __visited);

                this.___blockToNodesMap = __latestBlockToNodesMap;
            }
            this.___cfg.setNodeToBlock(__currentNodeMap);

            __graph.setLastSchedule(new ScheduleResult(this.___cfg, this.___nodeToBlockMap, this.___blockToNodesMap));
        }

        private BlockMap<ArrayList<FloatingReadNode>> calcLatestBlocks(SchedulingStrategy __strategy, NodeMap<Block> __currentNodeMap, BlockMap<List<Node>> __earliestBlockToNodesMap, NodeBitMap __visited, BlockMap<List<Node>> __latestBlockToNodesMap, boolean __immutableGraph)
        {
            BlockMap<ArrayList<FloatingReadNode>> __watchListMap = new BlockMap<>(this.___cfg);
            Block[] __reversePostOrder = this.___cfg.reversePostOrder();
            for (int __j = __reversePostOrder.length - 1; __j >= 0; --__j)
            {
                Block __currentBlock = __reversePostOrder[__j];
                List<Node> __blockToNodes = __earliestBlockToNodesMap.get(__currentBlock);
                LocationSet __killed = null;
                int __previousIndex = __blockToNodes.size();
                for (int __i = __blockToNodes.size() - 1; __i >= 0; --__i)
                {
                    Node __currentNode = __blockToNodes.get(__i);
                    if (__currentNode instanceof FixedNode)
                    {
                        // For these nodes, the earliest is at the same time the latest block.
                    }
                    else
                    {
                        Block __latestBlock = null;

                        LocationIdentity __constrainingLocation = null;
                        if (__currentNode instanceof FloatingReadNode)
                        {
                            // We are scheduling a floating read node => check memory anti-dependencies.
                            FloatingReadNode __floatingReadNode = (FloatingReadNode) __currentNode;
                            LocationIdentity __location = __floatingReadNode.getLocationIdentity();
                            if (__location.isMutable())
                            {
                                // Location can be killed.
                                __constrainingLocation = __location;
                                if (__currentBlock.canKill(__location))
                                {
                                    if (__killed == null)
                                    {
                                        __killed = new LocationSet();
                                    }
                                    fillKillSet(__killed, __blockToNodes.subList(__i + 1, __previousIndex));
                                    __previousIndex = __i;
                                    if (__killed.contains(__location))
                                    {
                                        // Earliest block kills location => we need to stay within earliest block.
                                        __latestBlock = __currentBlock;
                                    }
                                }
                            }
                        }

                        if (__latestBlock == null)
                        {
                            // We are not constraint within earliest block => calculate optimized schedule.
                            calcLatestBlock(__currentBlock, __strategy, __currentNode, __currentNodeMap, __constrainingLocation, __watchListMap, __latestBlockToNodesMap, __visited, __immutableGraph);
                        }
                        else
                        {
                            selectLatestBlock(__currentNode, __currentBlock, __latestBlock, __currentNodeMap, __watchListMap, __constrainingLocation, __latestBlockToNodesMap);
                        }
                    }
                }
            }
            return __watchListMap;
        }

        protected static void selectLatestBlock(Node __currentNode, Block __currentBlock, Block __latestBlock, NodeMap<Block> __currentNodeMap, BlockMap<ArrayList<FloatingReadNode>> __watchListMap, LocationIdentity __constrainingLocation, BlockMap<List<Node>> __latestBlockToNodesMap)
        {
            if (__currentBlock != __latestBlock)
            {
                __currentNodeMap.setAndGrow(__currentNode, __latestBlock);

                if (__constrainingLocation != null && __latestBlock.canKill(__constrainingLocation))
                {
                    if (__watchListMap.get(__latestBlock) == null)
                    {
                        __watchListMap.put(__latestBlock, new ArrayList<>());
                    }
                    __watchListMap.get(__latestBlock).add((FloatingReadNode) __currentNode);
                }
            }

            __latestBlockToNodesMap.get(__latestBlock).add(__currentNode);
        }

        public static Block checkKillsBetween(Block __earliestBlock, Block __latestBlock, LocationIdentity __location)
        {
            Block __current = __latestBlock.getDominator();

            // Collect dominator chain that needs checking.
            List<Block> __dominatorChain = new ArrayList<>();
            __dominatorChain.add(__latestBlock);
            while (__current != __earliestBlock)
            {
                // Current is an intermediate dominator between earliestBlock and latestBlock.
                if (__current.canKill(__location))
                {
                    __dominatorChain.clear();
                }
                __dominatorChain.add(__current);
                __current = __current.getDominator();
            }

            // The first element of dominatorChain now contains the latest possible block.
            Block __lastBlock = __earliestBlock;
            for (int __i = __dominatorChain.size() - 1; __i >= 0; --__i)
            {
                Block __currentBlock = __dominatorChain.get(__i);
                if (__currentBlock.getLoopDepth() > __lastBlock.getLoopDepth())
                {
                    // We are entering a loop boundary. The new loops must not kill the location for the crossing to be safe.
                    if (__currentBlock.getLoop() != null && ((HIRLoop) __currentBlock.getLoop()).canKill(__location))
                    {
                        break;
                    }
                }

                if (__currentBlock.canKillBetweenThisAndDominator(__location))
                {
                    break;
                }
                __lastBlock = __currentBlock;
            }

            if (__lastBlock.getBeginNode() instanceof KillingBeginNode)
            {
                LocationIdentity __locationIdentity = ((KillingBeginNode) __lastBlock.getBeginNode()).getLocationIdentity();
                if ((__locationIdentity.isAny() || __locationIdentity.equals(__location)) && __lastBlock != __earliestBlock)
                {
                    // The begin of this block kills the location, so we *have* to schedule the node in the dominating block.
                    __lastBlock = __lastBlock.getDominator();
                }
            }

            return __lastBlock;
        }

        private static void fillKillSet(LocationSet __killed, List<Node> __subList)
        {
            if (!__killed.isAny())
            {
                for (Node __n : __subList)
                {
                    // Check if this node kills a node in the watch list.
                    if (__n instanceof MemoryCheckpoint.Single)
                    {
                        LocationIdentity __identity = ((MemoryCheckpoint.Single) __n).getLocationIdentity();
                        __killed.add(__identity);
                        if (__killed.isAny())
                        {
                            return;
                        }
                    }
                    else if (__n instanceof MemoryCheckpoint.Multi)
                    {
                        for (LocationIdentity __identity : ((MemoryCheckpoint.Multi) __n).getLocationIdentities())
                        {
                            __killed.add(__identity);
                            if (__killed.isAny())
                            {
                                return;
                            }
                        }
                    }
                }
            }
        }

        private static void sortNodesLatestWithinBlock(ControlFlowGraph __cfg, BlockMap<List<Node>> __earliestBlockToNodesMap, BlockMap<List<Node>> __latestBlockToNodesMap, NodeMap<Block> __currentNodeMap, BlockMap<ArrayList<FloatingReadNode>> __watchListMap, NodeBitMap __visited)
        {
            for (Block __b : __cfg.getBlocks())
            {
                sortNodesLatestWithinBlock(__b, __earliestBlockToNodesMap, __latestBlockToNodesMap, __currentNodeMap, __watchListMap, __visited);
            }
        }

        private static void sortNodesLatestWithinBlock(Block __b, BlockMap<List<Node>> __earliestBlockToNodesMap, BlockMap<List<Node>> __latestBlockToNodesMap, NodeMap<Block> __nodeMap, BlockMap<ArrayList<FloatingReadNode>> __watchListMap, NodeBitMap __unprocessed)
        {
            List<Node> __earliestSorting = __earliestBlockToNodesMap.get(__b);
            ArrayList<Node> __result = new ArrayList<>(__earliestSorting.size());
            ArrayList<FloatingReadNode> __watchList = null;
            if (__watchListMap != null)
            {
                __watchList = __watchListMap.get(__b);
            }
            AbstractBeginNode __beginNode = __b.getBeginNode();
            if (__beginNode instanceof LoopExitNode)
            {
                LoopExitNode __loopExitNode = (LoopExitNode) __beginNode;
                for (ProxyNode __proxy : __loopExitNode.proxies())
                {
                    __unprocessed.clear(__proxy);
                    ValueNode __value = __proxy.value();
                    // if multiple proxies reference the same value, schedule the value of a proxy once
                    if (__value != null && __nodeMap.get(__value) == __b && __unprocessed.isMarked(__value))
                    {
                        sortIntoList(__value, __b, __result, __nodeMap, __unprocessed, null);
                    }
                }
            }
            FixedNode __endNode = __b.getEndNode();
            FixedNode __fixedEndNode = null;
            if (isFixedEnd(__endNode))
            {
                // Only if the end node is either a control split or an end node, we need to force
                // it to be the last node in the schedule.
                __fixedEndNode = __endNode;
            }
            for (Node __n : __earliestSorting)
            {
                if (__n != __fixedEndNode)
                {
                    if (__n instanceof FixedNode)
                    {
                        checkWatchList(__b, __nodeMap, __unprocessed, __result, __watchList, __n);
                        sortIntoList(__n, __b, __result, __nodeMap, __unprocessed, null);
                    }
                    else if (__nodeMap.get(__n) == __b && __n instanceof FloatingReadNode)
                    {
                        FloatingReadNode __floatingReadNode = (FloatingReadNode) __n;
                        if (isImplicitNullOpportunity(__floatingReadNode, __b))
                        {
                            // Schedule at the beginning of the block.
                            sortIntoList(__floatingReadNode, __b, __result, __nodeMap, __unprocessed, null);
                        }
                        else
                        {
                            LocationIdentity __location = __floatingReadNode.getLocationIdentity();
                            if (__b.canKill(__location))
                            {
                                // This read can be killed in this block, add to watch list.
                                if (__watchList == null)
                                {
                                    __watchList = new ArrayList<>();
                                }
                                __watchList.add(__floatingReadNode);
                            }
                        }
                    }
                }
            }

            for (Node __n : __latestBlockToNodesMap.get(__b))
            {
                if (__unprocessed.isMarked(__n))
                {
                    sortIntoList(__n, __b, __result, __nodeMap, __unprocessed, __fixedEndNode);
                }
            }

            if (__endNode != null && __unprocessed.isMarked(__endNode))
            {
                sortIntoList(__endNode, __b, __result, __nodeMap, __unprocessed, null);
            }

            __latestBlockToNodesMap.put(__b, __result);
        }

        private static void checkWatchList(Block __b, NodeMap<Block> __nodeMap, NodeBitMap __unprocessed, ArrayList<Node> __result, ArrayList<FloatingReadNode> __watchList, Node __n)
        {
            if (__watchList != null && !__watchList.isEmpty())
            {
                // Check if this node kills a node in the watch list.
                if (__n instanceof MemoryCheckpoint.Single)
                {
                    LocationIdentity __identity = ((MemoryCheckpoint.Single) __n).getLocationIdentity();
                    checkWatchList(__watchList, __identity, __b, __result, __nodeMap, __unprocessed);
                }
                else if (__n instanceof MemoryCheckpoint.Multi)
                {
                    for (LocationIdentity __identity : ((MemoryCheckpoint.Multi) __n).getLocationIdentities())
                    {
                        checkWatchList(__watchList, __identity, __b, __result, __nodeMap, __unprocessed);
                    }
                }
            }
        }

        private static void checkWatchList(ArrayList<FloatingReadNode> __watchList, LocationIdentity __identity, Block __b, ArrayList<Node> __result, NodeMap<Block> __nodeMap, NodeBitMap __unprocessed)
        {
            if (__identity.isImmutable())
            {
                // Nothing to do. This can happen for an initialization write.
            }
            else if (__identity.isAny())
            {
                for (FloatingReadNode __r : __watchList)
                {
                    if (__unprocessed.isMarked(__r))
                    {
                        sortIntoList(__r, __b, __result, __nodeMap, __unprocessed, null);
                    }
                }
                __watchList.clear();
            }
            else
            {
                int __index = 0;
                while (__index < __watchList.size())
                {
                    FloatingReadNode __r = __watchList.get(__index);
                    LocationIdentity __locationIdentity = __r.getLocationIdentity();
                    if (__unprocessed.isMarked(__r))
                    {
                        if (__identity.overlaps(__locationIdentity))
                        {
                            sortIntoList(__r, __b, __result, __nodeMap, __unprocessed, null);
                        }
                        else
                        {
                            ++__index;
                            continue;
                        }
                    }
                    int __lastIndex = __watchList.size() - 1;
                    __watchList.set(__index, __watchList.get(__lastIndex));
                    __watchList.remove(__lastIndex);
                }
            }
        }

        private static void sortIntoList(Node __n, Block __b, ArrayList<Node> __result, NodeMap<Block> __nodeMap, NodeBitMap __unprocessed, Node __excludeNode)
        {
            if (__n instanceof PhiNode)
            {
                return;
            }

            __unprocessed.clear(__n);

            for (Node __input : __n.inputs())
            {
                if (__nodeMap.get(__input) == __b && __unprocessed.isMarked(__input) && __input != __excludeNode)
                {
                    sortIntoList(__input, __b, __result, __nodeMap, __unprocessed, __excludeNode);
                }
            }

            if (__n instanceof ProxyNode)
            {
                // Skip proxy nodes.
            }
            else
            {
                __result.add(__n);
            }
        }

        protected void calcLatestBlock(Block __earliestBlock, SchedulingStrategy __strategy, Node __currentNode, NodeMap<Block> __currentNodeMap, LocationIdentity __constrainingLocation, BlockMap<ArrayList<FloatingReadNode>> __watchListMap, BlockMap<List<Node>> __latestBlockToNodesMap, NodeBitMap __visited, boolean __immutableGraph)
        {
            Block __latestBlock = null;
            if (!__currentNode.hasUsages())
            {
                __latestBlock = __earliestBlock;
            }
            else
            {
                for (Node __usage : __currentNode.usages())
                {
                    if (__immutableGraph && !__visited.contains(__usage))
                    {
                        // Normally, dead nodes are deleted by the scheduler before we reach this point.
                        // Only when the scheduler is asked to not modify a graph, we can see dead nodes here.
                        continue;
                    }
                    __latestBlock = calcBlockForUsage(__currentNode, __usage, __latestBlock, __currentNodeMap);
                }

                if (__strategy == SchedulingStrategy.FINAL_SCHEDULE || __strategy == SchedulingStrategy.LATEST_OUT_OF_LOOPS)
                {
                    Block __currentBlock = __latestBlock;
                    while (__currentBlock.getLoopDepth() > __earliestBlock.getLoopDepth() && __currentBlock != __earliestBlock.getDominator())
                    {
                        Block __previousCurrentBlock = __currentBlock;
                        __currentBlock = __currentBlock.getDominator();
                        if (__previousCurrentBlock.isLoopHeader())
                        {
                            if (__currentBlock.probability() < __latestBlock.probability() || ((StructuredGraph) __currentNode.graph()).hasValueProxies())
                            {
                                // Only assign new latest block if frequency is actually lower or if
                                // loop proxies would be required otherwise.
                                __latestBlock = __currentBlock;
                            }
                        }
                    }
                }

                if (__latestBlock != __earliestBlock && __latestBlock != __earliestBlock.getDominator() && __constrainingLocation != null)
                {
                    __latestBlock = checkKillsBetween(__earliestBlock, __latestBlock, __constrainingLocation);
                }
            }

            if (__latestBlock != __earliestBlock && __currentNode instanceof FloatingReadNode)
            {
                FloatingReadNode __floatingReadNode = (FloatingReadNode) __currentNode;
                if (isImplicitNullOpportunity(__floatingReadNode, __earliestBlock) && __earliestBlock.probability() < __latestBlock.probability() * IMPLICIT_NULL_CHECK_OPPORTUNITY_PROBABILITY_FACTOR)
                {
                    __latestBlock = __earliestBlock;
                }
            }

            selectLatestBlock(__currentNode, __earliestBlock, __latestBlock, __currentNodeMap, __watchListMap, __constrainingLocation, __latestBlockToNodesMap);
        }

        private static boolean isImplicitNullOpportunity(FloatingReadNode __floatingReadNode, Block __block)
        {
            Node __pred = __block.getBeginNode().predecessor();
            if (__pred instanceof IfNode)
            {
                IfNode __ifNode = (IfNode) __pred;
                if (__ifNode.condition() instanceof IsNullNode)
                {
                    IsNullNode __isNullNode = (IsNullNode) __ifNode.condition();
                    if (getUnproxifiedUncompressed(__floatingReadNode.getAddress().getBase()) == getUnproxifiedUncompressed(__isNullNode.getValue()))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        private static Node getUnproxifiedUncompressed(Node __node)
        {
            Node __result = __node;
            while (true)
            {
                if (__result instanceof ValueProxy)
                {
                    ValueProxy __valueProxy = (ValueProxy) __result;
                    __result = __valueProxy.getOriginalNode();
                }
                else if (__result instanceof ConvertNode)
                {
                    ConvertNode __convertNode = (ConvertNode) __result;
                    if (__convertNode.mayNullCheckSkipConversion())
                    {
                        __result = __convertNode.getValue();
                    }
                    else
                    {
                        break;
                    }
                }
                else
                {
                    break;
                }
            }
            return __result;
        }

        private static Block calcBlockForUsage(Node __node, Node __usage, Block __startBlock, NodeMap<Block> __currentNodeMap)
        {
            Block __currentBlock = __startBlock;
            if (__usage instanceof PhiNode)
            {
                // An input to a PhiNode is used at the end of the predecessor block that
                // corresponds to the PhiNode input. One PhiNode can use an input multiple times.
                PhiNode __phi = (PhiNode) __usage;
                AbstractMergeNode __merge = __phi.merge();
                Block __mergeBlock = __currentNodeMap.get(__merge);
                for (int __i = 0; __i < __phi.valueCount(); ++__i)
                {
                    if (__phi.valueAt(__i) == __node)
                    {
                        Block __otherBlock = __mergeBlock.getPredecessors()[__i];
                        __currentBlock = AbstractControlFlowGraph.commonDominatorTyped(__currentBlock, __otherBlock);
                    }
                }
            }
            else if (__usage instanceof AbstractBeginNode)
            {
                AbstractBeginNode __abstractBeginNode = (AbstractBeginNode) __usage;
                if (__abstractBeginNode instanceof StartNode)
                {
                    __currentBlock = AbstractControlFlowGraph.commonDominatorTyped(__currentBlock, __currentNodeMap.get(__abstractBeginNode));
                }
                else
                {
                    Block __otherBlock = __currentNodeMap.get(__abstractBeginNode).getDominator();
                    __currentBlock = AbstractControlFlowGraph.commonDominatorTyped(__currentBlock, __otherBlock);
                }
            }
            else
            {
                // All other types of usages: Put the input into the same block as the usage.
                Block __otherBlock = __currentNodeMap.get(__usage);
                if (__usage instanceof ProxyNode)
                {
                    ProxyNode __proxyNode = (ProxyNode) __usage;
                    __otherBlock = __currentNodeMap.get(__proxyNode.proxyPoint());
                }
                __currentBlock = AbstractControlFlowGraph.commonDominatorTyped(__currentBlock, __otherBlock);
            }
            return __currentBlock;
        }

        ///
        // Micro block that is allocated for each fixed node and captures all floating nodes that
        // need to be scheduled immediately after the corresponding fixed node.
        ///
        // @class SchedulePhase.Instance.MicroBlock
        private static final class MicroBlock
        {
            // @field
            private final int ___id;
            // @field
            private int ___nodeCount;
            // @field
            private NodeEntry ___head;
            // @field
            private NodeEntry ___tail;

            // @cons
            MicroBlock(int __id)
            {
                super();
                this.___id = __id;
            }

            ///
            // Adds a new floating node into the micro block.
            ///
            public void add(Node __node)
            {
                NodeEntry __newTail = new NodeEntry(__node);
                if (this.___tail == null)
                {
                    this.___tail = this.___head = __newTail;
                }
                else
                {
                    this.___tail.___next = __newTail;
                    this.___tail = __newTail;
                }
                this.___nodeCount++;
            }

            ///
            // Number of nodes in this micro block.
            ///
            public int getNodeCount()
            {
                return this.___nodeCount;
            }

            private int getActualNodeCount()
            {
                int __count = 0;
                for (NodeEntry __e = this.___head; __e != null; __e = __e.___next)
                {
                    __count++;
                }
                return __count;
            }

            ///
            // The id of the micro block, with a block always associated with a lower id than its successors.
            ///
            public int getId()
            {
                return this.___id;
            }

            ///
            // First node of the linked list of nodes of this micro block.
            ///
            public NodeEntry getFirstNode()
            {
                return this.___head;
            }

            ///
            // Takes all nodes in this micro blocks and prepends them to the nodes of the given parameter.
            //
            // @param newBlock the new block for the nodes
            ///
            public void prependChildrenTo(MicroBlock __newBlock)
            {
                if (this.___tail != null)
                {
                    this.___tail.___next = __newBlock.___head;
                    __newBlock.___head = this.___head;
                    this.___head = this.___tail = null;
                    __newBlock.___nodeCount += this.___nodeCount;
                    this.___nodeCount = 0;
                }
            }

            @Override
            public int hashCode()
            {
                return this.___id;
            }
        }

        ///
        // Entry in the linked list of nodes.
        ///
        // @class SchedulePhase.Instance.NodeEntry
        private static final class NodeEntry
        {
            // @field
            private final Node ___node;
            // @field
            private NodeEntry ___next;

            // @cons
            NodeEntry(Node __node)
            {
                super();
                this.___node = __node;
                this.___next = null;
            }

            public NodeEntry getNext()
            {
                return this.___next;
            }

            public Node getNode()
            {
                return this.___node;
            }
        }

        private void scheduleEarliestIterative(BlockMap<List<Node>> __blockToNodes, NodeMap<Block> __nodeToBlock, NodeBitMap __visited, StructuredGraph __graph, boolean __immutableGraph, boolean __withGuardOrder)
        {
            NodeMap<MicroBlock> __entries = __graph.createNodeMap();
            NodeStack __stack = new NodeStack();

            // Initialize with fixed nodes.
            MicroBlock __startBlock = null;
            int __nextId = 1;
            for (Block __b : this.___cfg.reversePostOrder())
            {
                for (FixedNode __current : __b.getBeginNode().getBlockNodes())
                {
                    MicroBlock __microBlock = new MicroBlock(__nextId++);
                    __entries.set(__current, __microBlock);
                    boolean __isNew = __visited.checkAndMarkInc(__current);
                    if (__startBlock == null)
                    {
                        __startBlock = __microBlock;
                    }
                }
            }

            if (__graph.getGuardsStage().allowsFloatingGuards() && __graph.getNodes(GuardNode.TYPE).isNotEmpty())
            {
                // Now process guards.
                if (GraalOptions.guardPriorities && __withGuardOrder)
                {
                    EnumMap<GuardPriority, List<GuardNode>> __guardsByPriority = new EnumMap<>(GuardPriority.class);
                    for (GuardNode __guard : __graph.getNodes(GuardNode.TYPE))
                    {
                        __guardsByPriority.computeIfAbsent(__guard.computePriority(), __p -> new ArrayList<>()).add(__guard);
                    }
                    // 'EnumMap.values' returns values in "natural" key order
                    for (List<GuardNode> __guards : __guardsByPriority.values())
                    {
                        processNodes(__visited, __entries, __stack, __startBlock, __guards);
                    }
                    GuardOrder.resortGuards(__graph, __entries, __stack);
                }
                else
                {
                    processNodes(__visited, __entries, __stack, __startBlock, __graph.getNodes(GuardNode.TYPE));
                }
            }

            // Now process inputs of fixed nodes.
            for (Block __b : this.___cfg.reversePostOrder())
            {
                for (FixedNode __current : __b.getBeginNode().getBlockNodes())
                {
                    processNodes(__visited, __entries, __stack, __startBlock, __current.inputs());
                }
            }

            if (__visited.getCounter() < __graph.getNodeCount())
            {
                // Visit back input edges of loop phis.
                boolean __changed;
                boolean __unmarkedPhi;
                do
                {
                    __changed = false;
                    __unmarkedPhi = false;
                    for (LoopBeginNode __loopBegin : __graph.getNodes(LoopBeginNode.TYPE))
                    {
                        for (PhiNode __phi : __loopBegin.phis())
                        {
                            if (__visited.isMarked(__phi))
                            {
                                for (int __i = 0; __i < __loopBegin.getLoopEndCount(); ++__i)
                                {
                                    Node __node = __phi.valueAt(__i + __loopBegin.forwardEndCount());
                                    if (__node != null && __entries.get(__node) == null)
                                    {
                                        __changed = true;
                                        processStack(__node, __startBlock, __entries, __visited, __stack);
                                    }
                                }
                            }
                            else
                            {
                                __unmarkedPhi = true;
                            }
                        }
                    }

                    // the processing of one loop phi could have marked a previously checked loop phi, therefore this needs to be iterative.
                } while (__unmarkedPhi && __changed);
            }

            // Check for dead nodes.
            if (!__immutableGraph && __visited.getCounter() < __graph.getNodeCount())
            {
                for (Node __n : __graph.getNodes())
                {
                    if (!__visited.isMarked(__n))
                    {
                        __n.clearInputs();
                        __n.markDeleted();
                    }
                }
            }

            for (Block __b : this.___cfg.reversePostOrder())
            {
                FixedNode __fixedNode = __b.getEndNode();
                if (__fixedNode instanceof ControlSplitNode)
                {
                    ControlSplitNode __controlSplitNode = (ControlSplitNode) __fixedNode;
                    MicroBlock __endBlock = __entries.get(__fixedNode);
                    AbstractBeginNode __primarySuccessor = __controlSplitNode.getPrimarySuccessor();
                    if (__primarySuccessor != null)
                    {
                        __endBlock.prependChildrenTo(__entries.get(__primarySuccessor));
                    }
                }
            }

            // create lists for each block
            for (Block __b : this.___cfg.reversePostOrder())
            {
                // count nodes in block
                int __totalCount = 0;
                for (FixedNode __current : __b.getBeginNode().getBlockNodes())
                {
                    MicroBlock __microBlock = __entries.get(__current);
                    __totalCount += __microBlock.getNodeCount() + 1;
                }

                // initialize with begin node, it is always the first node
                ArrayList<Node> __nodes = new ArrayList<>(__totalCount);
                __blockToNodes.put(__b, __nodes);

                for (FixedNode __current : __b.getBeginNode().getBlockNodes())
                {
                    MicroBlock __microBlock = __entries.get(__current);
                    __nodeToBlock.set(__current, __b);
                    __nodes.add(__current);
                    NodeEntry __next = __microBlock.getFirstNode();
                    while (__next != null)
                    {
                        Node __nextNode = __next.getNode();
                        __nodeToBlock.set(__nextNode, __b);
                        __nodes.add(__nextNode);
                        __next = __next.getNext();
                    }
                }
            }
        }

        private static void processNodes(NodeBitMap __visited, NodeMap<MicroBlock> __entries, NodeStack __stack, MicroBlock __startBlock, Iterable<? extends Node> __nodes)
        {
            for (Node __node : __nodes)
            {
                if (__entries.get(__node) == null)
                {
                    processStack(__node, __startBlock, __entries, __visited, __stack);
                }
            }
        }

        private static void processStackPhi(NodeStack __stack, PhiNode __phiNode, NodeMap<MicroBlock> __nodeToBlock, NodeBitMap __visited)
        {
            __stack.pop();
            if (__visited.checkAndMarkInc(__phiNode))
            {
                MicroBlock __mergeBlock = __nodeToBlock.get(__phiNode.merge());
                __nodeToBlock.set(__phiNode, __mergeBlock);
                AbstractMergeNode __merge = __phiNode.merge();
                for (int __i = 0; __i < __merge.forwardEndCount(); ++__i)
                {
                    Node __input = __phiNode.valueAt(__i);
                    if (__input != null && __nodeToBlock.get(__input) == null)
                    {
                        __stack.push(__input);
                    }
                }
            }
        }

        private static void processStackProxy(NodeStack __stack, ProxyNode __proxyNode, NodeMap<MicroBlock> __nodeToBlock, NodeBitMap __visited)
        {
            __stack.pop();
            if (__visited.checkAndMarkInc(__proxyNode))
            {
                __nodeToBlock.set(__proxyNode, __nodeToBlock.get(__proxyNode.proxyPoint()));
                Node __input = __proxyNode.value();
                if (__input != null && __nodeToBlock.get(__input) == null)
                {
                    __stack.push(__input);
                }
            }
        }

        private static void processStack(Node __first, MicroBlock __startBlock, NodeMap<MicroBlock> __nodeToMicroBlock, NodeBitMap __visited, NodeStack __stack)
        {
            __stack.push(__first);
            Node __current = __first;
            while (true)
            {
                if (__current instanceof PhiNode)
                {
                    processStackPhi(__stack, (PhiNode) __current, __nodeToMicroBlock, __visited);
                }
                else if (__current instanceof ProxyNode)
                {
                    processStackProxy(__stack, (ProxyNode) __current, __nodeToMicroBlock, __visited);
                }
                else
                {
                    MicroBlock __currentBlock = __nodeToMicroBlock.get(__current);
                    if (__currentBlock == null)
                    {
                        MicroBlock __earliestBlock = processInputs(__nodeToMicroBlock, __stack, __startBlock, __current);
                        if (__earliestBlock == null)
                        {
                            // We need to delay until inputs are processed.
                        }
                        else
                        {
                            // Can immediately process and pop.
                            __stack.pop();
                            __visited.checkAndMarkInc(__current);
                            __nodeToMicroBlock.set(__current, __earliestBlock);
                            __earliestBlock.add(__current);
                        }
                    }
                    else
                    {
                        __stack.pop();
                    }
                }

                if (__stack.isEmpty())
                {
                    break;
                }
                __current = __stack.peek();
            }
        }

        // @class SchedulePhase.Instance.GuardOrder
        private static final class GuardOrder
        {
            ///
            // After an earliest schedule, this will re-sort guards to honor their
            // {@linkplain StaticDeoptimizingNode#computePriority() priority}.
            //
            // Note that this only changes the order of nodes within {@linkplain MicroBlock
            // micro-blocks}, nodes will not be moved from one micro-block to another.
            ///
            private static void resortGuards(StructuredGraph __graph, NodeMap<MicroBlock> __entries, NodeStack __stack)
            {
                EconomicSet<MicroBlock> __blocksWithGuards = EconomicSet.create(Equivalence.IDENTITY);
                for (GuardNode __guard : __graph.getNodes(GuardNode.TYPE))
                {
                    MicroBlock __block = __entries.get(__guard);
                    __blocksWithGuards.add(__block);
                }
                NodeMap<GuardPriority> __priorities = __graph.createNodeMap();
                NodeBitMap __blockNodes = __graph.createNodeBitMap();
                for (MicroBlock __block : __blocksWithGuards)
                {
                    MicroBlock __newBlock = resortGuards(__block, __stack, __blockNodes, __priorities);
                    if (__newBlock != null)
                    {
                        __block.___head = __newBlock.___head;
                        __block.___tail = __newBlock.___tail;
                    }
                }
            }

            ///
            // This resorts guards within one micro-block.
            //
            // {@code stack}, {@code blockNodes} and {@code priorities} are just temporary
            // data-structures which are allocated once by the callers of this method. They should
            // be in their "initial"/"empty" state when calling this method and when it returns.
            ///
            private static MicroBlock resortGuards(MicroBlock __block, NodeStack __stack, NodeBitMap __blockNodes, NodeMap<GuardPriority> __priorities)
            {
                if (!propagatePriority(__block, __stack, __priorities, __blockNodes))
                {
                    return null;
                }

                Function<GuardNode, GuardPriority> __transitiveGuardPriorityGetter = __priorities::get;
                Comparator<GuardNode> __globalGuardPriorityComparator = Comparator.comparing(__transitiveGuardPriorityGetter).thenComparing(GuardNode::computePriority).thenComparingInt(Node::hashCode);

                SortedSet<GuardNode> __availableGuards = new TreeSet<>(__globalGuardPriorityComparator);
                MicroBlock __newBlock = new MicroBlock(__block.getId());

                NodeBitMap __sorted = __blockNodes;
                __sorted.invert();

                for (NodeEntry __e = __block.___head; __e != null; __e = __e.___next)
                {
                    checkIfAvailable(__e.___node, __stack, __sorted, __newBlock, __availableGuards, false);
                }
                do
                {
                    while (!__stack.isEmpty())
                    {
                        checkIfAvailable(__stack.pop(), __stack, __sorted, __newBlock, __availableGuards, true);
                    }
                    Iterator<GuardNode> __iterator = __availableGuards.iterator();
                    if (__iterator.hasNext())
                    {
                        addNodeToResort(__iterator.next(), __stack, __sorted, __newBlock, true);
                        __iterator.remove();
                    }
                } while (!__stack.isEmpty() || !__availableGuards.isEmpty());

                __blockNodes.clearAll();
                return __newBlock;
            }

            ///
            // This checks if {@code n} can be scheduled, if it is the case, it schedules it now by
            // calling {@link #addNodeToResort(Node, NodeStack, NodeBitMap, MicroBlock, boolean)}.
            ///
            private static void checkIfAvailable(Node __n, NodeStack __stack, NodeBitMap __sorted, Instance.MicroBlock __newBlock, SortedSet<GuardNode> __availableGuardNodes, boolean __pushUsages)
            {
                if (__sorted.isMarked(__n))
                {
                    return;
                }
                for (Node __in : __n.inputs())
                {
                    if (!__sorted.isMarked(__in))
                    {
                        return;
                    }
                }
                if (__n instanceof GuardNode)
                {
                    __availableGuardNodes.add((GuardNode) __n);
                }
                else
                {
                    addNodeToResort(__n, __stack, __sorted, __newBlock, __pushUsages);
                }
            }

            ///
            // Add a node to the re-sorted micro-block. This also pushes nodes that need to be
            // (re-)examined on the stack.
            ///
            private static void addNodeToResort(Node __n, NodeStack __stack, NodeBitMap __sorted, MicroBlock __newBlock, boolean __pushUsages)
            {
                __sorted.mark(__n);
                __newBlock.add(__n);
                if (__pushUsages)
                {
                    for (Node __u : __n.usages())
                    {
                        if (!__sorted.isMarked(__u))
                        {
                            __stack.push(__u);
                        }
                    }
                }
            }

            ///
            // This fills in a map of transitive priorities ({@code priorities}). It also marks the
            // nodes from this micro-block in {@code blockNodes}.
            //
            // The transitive priority of a guard is the highest of its priority and the priority of
            // the guards that depend on it (transitively).
            //
            // This method returns {@code false} if no re-ordering is necessary in this micro-block.
            ///
            private static boolean propagatePriority(MicroBlock __block, NodeStack __stack, NodeMap<GuardPriority> __priorities, NodeBitMap __blockNodes)
            {
                GuardPriority __lowestPriority = GuardPriority.highest();
                for (NodeEntry __e = __block.___head; __e != null; __e = __e.___next)
                {
                    __blockNodes.mark(__e.___node);
                    if (__e.___node instanceof GuardNode)
                    {
                        GuardNode __guard = (GuardNode) __e.___node;
                        GuardPriority __priority = __guard.computePriority();
                        if (__lowestPriority != null)
                        {
                            if (__priority.isLowerPriorityThan(__lowestPriority))
                            {
                                __lowestPriority = __priority;
                            }
                            else if (__priority.isHigherPriorityThan(__lowestPriority))
                            {
                                __lowestPriority = null;
                            }
                        }
                        __stack.push(__guard);
                        __priorities.set(__guard, __priority);
                    }
                }
                if (__lowestPriority != null)
                {
                    __stack.clear();
                    __blockNodes.clearAll();
                    return false;
                }

                do
                {
                    Node __current = __stack.pop();
                    GuardPriority __priority = __priorities.get(__current);
                    for (Node __input : __current.inputs())
                    {
                        if (!__blockNodes.isMarked(__input))
                        {
                            continue;
                        }
                        GuardPriority __inputPriority = __priorities.get(__input);
                        if (__inputPriority == null || __inputPriority.isLowerPriorityThan(__priority))
                        {
                            __priorities.set(__input, __priority);
                            __stack.push(__input);
                        }
                    }
                } while (!__stack.isEmpty());
                return true;
            }
        }

        ///
        // Processes the inputs of given block. Pushes unprocessed inputs onto the stack. Returns
        // null if there were still unprocessed inputs, otherwise returns the earliest block given
        // node can be scheduled in.
        ///
        private static MicroBlock processInputs(NodeMap<MicroBlock> __nodeToBlock, NodeStack __stack, MicroBlock __startBlock, Node __current)
        {
            if (__current.getNodeClass().isLeafNode())
            {
                return __startBlock;
            }

            MicroBlock __earliestBlock = __startBlock;
            for (Node __input : __current.inputs())
            {
                MicroBlock __inputBlock = __nodeToBlock.get(__input);
                if (__inputBlock == null)
                {
                    __earliestBlock = null;
                    __stack.push(__input);
                }
                else if (__earliestBlock != null && __inputBlock.getId() > __earliestBlock.getId())
                {
                    __earliestBlock = __inputBlock;
                }
            }
            return __earliestBlock;
        }

        private static boolean isFixedEnd(FixedNode __endNode)
        {
            return __endNode instanceof ControlSplitNode || __endNode instanceof ControlSinkNode || __endNode instanceof AbstractEndNode;
        }

        public ControlFlowGraph getCFG()
        {
            return this.___cfg;
        }

        ///
        // Gets the nodes in a given block.
        ///
        public List<Node> nodesFor(Block __block)
        {
            return this.___blockToNodesMap.get(__block);
        }
    }
}
