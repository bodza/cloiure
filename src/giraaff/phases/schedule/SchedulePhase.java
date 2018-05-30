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
import giraaff.options.OptionValues;
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

    private final SchedulingStrategy selectedStrategy;

    private final boolean immutableGraph;

    // @cons
    public SchedulePhase(OptionValues options)
    {
        this(false, options);
    }

    // @cons
    public SchedulePhase(boolean immutableGraph, OptionValues options)
    {
        this(GraalOptions.OptScheduleOutOfLoops.getValue(options) ? SchedulingStrategy.LATEST_OUT_OF_LOOPS : SchedulingStrategy.LATEST, immutableGraph);
    }

    // @cons
    public SchedulePhase(SchedulingStrategy strategy)
    {
        this(strategy, false);
    }

    // @cons
    public SchedulePhase(SchedulingStrategy strategy, boolean immutableGraph)
    {
        super();
        this.selectedStrategy = strategy;
        this.immutableGraph = immutableGraph;
    }

    @Override
    protected void run(StructuredGraph graph)
    {
        Instance inst = new Instance();
        inst.run(graph, selectedStrategy, immutableGraph);
    }

    public static void run(StructuredGraph graph, SchedulingStrategy strategy, ControlFlowGraph cfg)
    {
        Instance inst = new Instance(cfg);
        inst.run(graph, strategy, false);
    }

    // @class SchedulePhase.Instance
    public static final class Instance
    {
        private static final double IMPLICIT_NULL_CHECK_OPPORTUNITY_PROBABILITY_FACTOR = 2;
        /**
         * Map from blocks to the nodes in each block.
         */
        protected ControlFlowGraph cfg;
        protected BlockMap<List<Node>> blockToNodesMap;
        protected NodeMap<Block> nodeToBlockMap;

        // @cons
        public Instance()
        {
            this(null);
        }

        // @cons
        public Instance(ControlFlowGraph cfg)
        {
            super();
            this.cfg = cfg;
        }

        public void run(StructuredGraph graph, SchedulingStrategy selectedStrategy, boolean immutableGraph)
        {
            if (this.cfg == null)
            {
                this.cfg = ControlFlowGraph.compute(graph, true, true, true, false);
            }

            NodeMap<Block> currentNodeMap = graph.createNodeMap();
            NodeBitMap visited = graph.createNodeBitMap();
            BlockMap<List<Node>> earliestBlockToNodesMap = new BlockMap<>(cfg);
            this.nodeToBlockMap = currentNodeMap;
            this.blockToNodesMap = earliestBlockToNodesMap;

            scheduleEarliestIterative(earliestBlockToNodesMap, currentNodeMap, visited, graph, immutableGraph, selectedStrategy == SchedulingStrategy.EARLIEST_WITH_GUARD_ORDER);

            if (!selectedStrategy.isEarliest())
            {
                // For non-earliest schedules, we need to do a second pass.
                BlockMap<List<Node>> latestBlockToNodesMap = new BlockMap<>(cfg);
                for (Block b : cfg.getBlocks())
                {
                    latestBlockToNodesMap.put(b, new ArrayList<>());
                }

                BlockMap<ArrayList<FloatingReadNode>> watchListMap = calcLatestBlocks(selectedStrategy, currentNodeMap, earliestBlockToNodesMap, visited, latestBlockToNodesMap, immutableGraph);
                sortNodesLatestWithinBlock(cfg, earliestBlockToNodesMap, latestBlockToNodesMap, currentNodeMap, watchListMap, visited);

                this.blockToNodesMap = latestBlockToNodesMap;
            }
            cfg.setNodeToBlock(currentNodeMap);

            graph.setLastSchedule(new ScheduleResult(this.cfg, this.nodeToBlockMap, this.blockToNodesMap));
        }

        private BlockMap<ArrayList<FloatingReadNode>> calcLatestBlocks(SchedulingStrategy strategy, NodeMap<Block> currentNodeMap, BlockMap<List<Node>> earliestBlockToNodesMap, NodeBitMap visited, BlockMap<List<Node>> latestBlockToNodesMap, boolean immutableGraph)
        {
            BlockMap<ArrayList<FloatingReadNode>> watchListMap = new BlockMap<>(cfg);
            Block[] reversePostOrder = cfg.reversePostOrder();
            for (int j = reversePostOrder.length - 1; j >= 0; --j)
            {
                Block currentBlock = reversePostOrder[j];
                List<Node> blockToNodes = earliestBlockToNodesMap.get(currentBlock);
                LocationSet killed = null;
                int previousIndex = blockToNodes.size();
                for (int i = blockToNodes.size() - 1; i >= 0; --i)
                {
                    Node currentNode = blockToNodes.get(i);
                    if (currentNode instanceof FixedNode)
                    {
                        // For these nodes, the earliest is at the same time the latest block.
                    }
                    else
                    {
                        Block latestBlock = null;

                        LocationIdentity constrainingLocation = null;
                        if (currentNode instanceof FloatingReadNode)
                        {
                            // We are scheduling a floating read node => check memory anti-dependencies.
                            FloatingReadNode floatingReadNode = (FloatingReadNode) currentNode;
                            LocationIdentity location = floatingReadNode.getLocationIdentity();
                            if (location.isMutable())
                            {
                                // Location can be killed.
                                constrainingLocation = location;
                                if (currentBlock.canKill(location))
                                {
                                    if (killed == null)
                                    {
                                        killed = new LocationSet();
                                    }
                                    fillKillSet(killed, blockToNodes.subList(i + 1, previousIndex));
                                    previousIndex = i;
                                    if (killed.contains(location))
                                    {
                                        // Earliest block kills location => we need to stay within earliest block.
                                        latestBlock = currentBlock;
                                    }
                                }
                            }
                        }

                        if (latestBlock == null)
                        {
                            // We are not constraint within earliest block => calculate optimized schedule.
                            calcLatestBlock(currentBlock, strategy, currentNode, currentNodeMap, constrainingLocation, watchListMap, latestBlockToNodesMap, visited, immutableGraph);
                        }
                        else
                        {
                            selectLatestBlock(currentNode, currentBlock, latestBlock, currentNodeMap, watchListMap, constrainingLocation, latestBlockToNodesMap);
                        }
                    }
                }
            }
            return watchListMap;
        }

        protected static void selectLatestBlock(Node currentNode, Block currentBlock, Block latestBlock, NodeMap<Block> currentNodeMap, BlockMap<ArrayList<FloatingReadNode>> watchListMap, LocationIdentity constrainingLocation, BlockMap<List<Node>> latestBlockToNodesMap)
        {
            if (currentBlock != latestBlock)
            {
                currentNodeMap.setAndGrow(currentNode, latestBlock);

                if (constrainingLocation != null && latestBlock.canKill(constrainingLocation))
                {
                    if (watchListMap.get(latestBlock) == null)
                    {
                        watchListMap.put(latestBlock, new ArrayList<>());
                    }
                    watchListMap.get(latestBlock).add((FloatingReadNode) currentNode);
                }
            }

            latestBlockToNodesMap.get(latestBlock).add(currentNode);
        }

        public static Block checkKillsBetween(Block earliestBlock, Block latestBlock, LocationIdentity location)
        {
            Block current = latestBlock.getDominator();

            // Collect dominator chain that needs checking.
            List<Block> dominatorChain = new ArrayList<>();
            dominatorChain.add(latestBlock);
            while (current != earliestBlock)
            {
                // Current is an intermediate dominator between earliestBlock and latestBlock.
                if (current.canKill(location))
                {
                    dominatorChain.clear();
                }
                dominatorChain.add(current);
                current = current.getDominator();
            }

            // The first element of dominatorChain now contains the latest possible block.
            Block lastBlock = earliestBlock;
            for (int i = dominatorChain.size() - 1; i >= 0; --i)
            {
                Block currentBlock = dominatorChain.get(i);
                if (currentBlock.getLoopDepth() > lastBlock.getLoopDepth())
                {
                    // We are entering a loop boundary. The new loops must not kill the location for the crossing to be safe.
                    if (currentBlock.getLoop() != null && ((HIRLoop) currentBlock.getLoop()).canKill(location))
                    {
                        break;
                    }
                }

                if (currentBlock.canKillBetweenThisAndDominator(location))
                {
                    break;
                }
                lastBlock = currentBlock;
            }

            if (lastBlock.getBeginNode() instanceof KillingBeginNode)
            {
                LocationIdentity locationIdentity = ((KillingBeginNode) lastBlock.getBeginNode()).getLocationIdentity();
                if ((locationIdentity.isAny() || locationIdentity.equals(location)) && lastBlock != earliestBlock)
                {
                    // The begin of this block kills the location, so we *have* to schedule the node in the dominating block.
                    lastBlock = lastBlock.getDominator();
                }
            }

            return lastBlock;
        }

        private static void fillKillSet(LocationSet killed, List<Node> subList)
        {
            if (!killed.isAny())
            {
                for (Node n : subList)
                {
                    // Check if this node kills a node in the watch list.
                    if (n instanceof MemoryCheckpoint.Single)
                    {
                        LocationIdentity identity = ((MemoryCheckpoint.Single) n).getLocationIdentity();
                        killed.add(identity);
                        if (killed.isAny())
                        {
                            return;
                        }
                    }
                    else if (n instanceof MemoryCheckpoint.Multi)
                    {
                        for (LocationIdentity identity : ((MemoryCheckpoint.Multi) n).getLocationIdentities())
                        {
                            killed.add(identity);
                            if (killed.isAny())
                            {
                                return;
                            }
                        }
                    }
                }
            }
        }

        private static void sortNodesLatestWithinBlock(ControlFlowGraph cfg, BlockMap<List<Node>> earliestBlockToNodesMap, BlockMap<List<Node>> latestBlockToNodesMap, NodeMap<Block> currentNodeMap, BlockMap<ArrayList<FloatingReadNode>> watchListMap, NodeBitMap visited)
        {
            for (Block b : cfg.getBlocks())
            {
                sortNodesLatestWithinBlock(b, earliestBlockToNodesMap, latestBlockToNodesMap, currentNodeMap, watchListMap, visited);
            }
        }

        private static void sortNodesLatestWithinBlock(Block b, BlockMap<List<Node>> earliestBlockToNodesMap, BlockMap<List<Node>> latestBlockToNodesMap, NodeMap<Block> nodeMap, BlockMap<ArrayList<FloatingReadNode>> watchListMap, NodeBitMap unprocessed)
        {
            List<Node> earliestSorting = earliestBlockToNodesMap.get(b);
            ArrayList<Node> result = new ArrayList<>(earliestSorting.size());
            ArrayList<FloatingReadNode> watchList = null;
            if (watchListMap != null)
            {
                watchList = watchListMap.get(b);
            }
            AbstractBeginNode beginNode = b.getBeginNode();
            if (beginNode instanceof LoopExitNode)
            {
                LoopExitNode loopExitNode = (LoopExitNode) beginNode;
                for (ProxyNode proxy : loopExitNode.proxies())
                {
                    unprocessed.clear(proxy);
                    ValueNode value = proxy.value();
                    // if multiple proxies reference the same value, schedule the value of a proxy once
                    if (value != null && nodeMap.get(value) == b && unprocessed.isMarked(value))
                    {
                        sortIntoList(value, b, result, nodeMap, unprocessed, null);
                    }
                }
            }
            FixedNode endNode = b.getEndNode();
            FixedNode fixedEndNode = null;
            if (isFixedEnd(endNode))
            {
                // Only if the end node is either a control split or an end node, we need to force
                // it to be the last node in the schedule.
                fixedEndNode = endNode;
            }
            for (Node n : earliestSorting)
            {
                if (n != fixedEndNode)
                {
                    if (n instanceof FixedNode)
                    {
                        checkWatchList(b, nodeMap, unprocessed, result, watchList, n);
                        sortIntoList(n, b, result, nodeMap, unprocessed, null);
                    }
                    else if (nodeMap.get(n) == b && n instanceof FloatingReadNode)
                    {
                        FloatingReadNode floatingReadNode = (FloatingReadNode) n;
                        if (isImplicitNullOpportunity(floatingReadNode, b))
                        {
                            // Schedule at the beginning of the block.
                            sortIntoList(floatingReadNode, b, result, nodeMap, unprocessed, null);
                        }
                        else
                        {
                            LocationIdentity location = floatingReadNode.getLocationIdentity();
                            if (b.canKill(location))
                            {
                                // This read can be killed in this block, add to watch list.
                                if (watchList == null)
                                {
                                    watchList = new ArrayList<>();
                                }
                                watchList.add(floatingReadNode);
                            }
                        }
                    }
                }
            }

            for (Node n : latestBlockToNodesMap.get(b))
            {
                if (unprocessed.isMarked(n))
                {
                    sortIntoList(n, b, result, nodeMap, unprocessed, fixedEndNode);
                }
            }

            if (endNode != null && unprocessed.isMarked(endNode))
            {
                sortIntoList(endNode, b, result, nodeMap, unprocessed, null);
            }

            latestBlockToNodesMap.put(b, result);
        }

        private static void checkWatchList(Block b, NodeMap<Block> nodeMap, NodeBitMap unprocessed, ArrayList<Node> result, ArrayList<FloatingReadNode> watchList, Node n)
        {
            if (watchList != null && !watchList.isEmpty())
            {
                // Check if this node kills a node in the watch list.
                if (n instanceof MemoryCheckpoint.Single)
                {
                    LocationIdentity identity = ((MemoryCheckpoint.Single) n).getLocationIdentity();
                    checkWatchList(watchList, identity, b, result, nodeMap, unprocessed);
                }
                else if (n instanceof MemoryCheckpoint.Multi)
                {
                    for (LocationIdentity identity : ((MemoryCheckpoint.Multi) n).getLocationIdentities())
                    {
                        checkWatchList(watchList, identity, b, result, nodeMap, unprocessed);
                    }
                }
            }
        }

        private static void checkWatchList(ArrayList<FloatingReadNode> watchList, LocationIdentity identity, Block b, ArrayList<Node> result, NodeMap<Block> nodeMap, NodeBitMap unprocessed)
        {
            if (identity.isImmutable())
            {
                // Nothing to do. This can happen for an initialization write.
            }
            else if (identity.isAny())
            {
                for (FloatingReadNode r : watchList)
                {
                    if (unprocessed.isMarked(r))
                    {
                        sortIntoList(r, b, result, nodeMap, unprocessed, null);
                    }
                }
                watchList.clear();
            }
            else
            {
                int index = 0;
                while (index < watchList.size())
                {
                    FloatingReadNode r = watchList.get(index);
                    LocationIdentity locationIdentity = r.getLocationIdentity();
                    if (unprocessed.isMarked(r))
                    {
                        if (identity.overlaps(locationIdentity))
                        {
                            sortIntoList(r, b, result, nodeMap, unprocessed, null);
                        }
                        else
                        {
                            ++index;
                            continue;
                        }
                    }
                    int lastIndex = watchList.size() - 1;
                    watchList.set(index, watchList.get(lastIndex));
                    watchList.remove(lastIndex);
                }
            }
        }

        private static void sortIntoList(Node n, Block b, ArrayList<Node> result, NodeMap<Block> nodeMap, NodeBitMap unprocessed, Node excludeNode)
        {
            if (n instanceof PhiNode)
            {
                return;
            }

            unprocessed.clear(n);

            for (Node input : n.inputs())
            {
                if (nodeMap.get(input) == b && unprocessed.isMarked(input) && input != excludeNode)
                {
                    sortIntoList(input, b, result, nodeMap, unprocessed, excludeNode);
                }
            }

            if (n instanceof ProxyNode)
            {
                // Skip proxy nodes.
            }
            else
            {
                result.add(n);
            }
        }

        protected void calcLatestBlock(Block earliestBlock, SchedulingStrategy strategy, Node currentNode, NodeMap<Block> currentNodeMap, LocationIdentity constrainingLocation, BlockMap<ArrayList<FloatingReadNode>> watchListMap, BlockMap<List<Node>> latestBlockToNodesMap, NodeBitMap visited, boolean immutableGraph)
        {
            Block latestBlock = null;
            if (!currentNode.hasUsages())
            {
                latestBlock = earliestBlock;
            }
            else
            {
                for (Node usage : currentNode.usages())
                {
                    if (immutableGraph && !visited.contains(usage))
                    {
                        /*
                         * Normally, dead nodes are deleted by the scheduler before we reach this point.
                         * Only when the scheduler is asked to not modify a graph, we can see dead nodes here.
                         */
                        continue;
                    }
                    latestBlock = calcBlockForUsage(currentNode, usage, latestBlock, currentNodeMap);
                }

                if (strategy == SchedulingStrategy.FINAL_SCHEDULE || strategy == SchedulingStrategy.LATEST_OUT_OF_LOOPS)
                {
                    Block currentBlock = latestBlock;
                    while (currentBlock.getLoopDepth() > earliestBlock.getLoopDepth() && currentBlock != earliestBlock.getDominator())
                    {
                        Block previousCurrentBlock = currentBlock;
                        currentBlock = currentBlock.getDominator();
                        if (previousCurrentBlock.isLoopHeader())
                        {
                            if (currentBlock.probability() < latestBlock.probability() || ((StructuredGraph) currentNode.graph()).hasValueProxies())
                            {
                                // Only assign new latest block if frequency is actually lower or if
                                // loop proxies would be required otherwise.
                                latestBlock = currentBlock;
                            }
                        }
                    }
                }

                if (latestBlock != earliestBlock && latestBlock != earliestBlock.getDominator() && constrainingLocation != null)
                {
                    latestBlock = checkKillsBetween(earliestBlock, latestBlock, constrainingLocation);
                }
            }

            if (latestBlock != earliestBlock && currentNode instanceof FloatingReadNode)
            {
                FloatingReadNode floatingReadNode = (FloatingReadNode) currentNode;
                if (isImplicitNullOpportunity(floatingReadNode, earliestBlock) && earliestBlock.probability() < latestBlock.probability() * IMPLICIT_NULL_CHECK_OPPORTUNITY_PROBABILITY_FACTOR)
                {
                    latestBlock = earliestBlock;
                }
            }

            selectLatestBlock(currentNode, earliestBlock, latestBlock, currentNodeMap, watchListMap, constrainingLocation, latestBlockToNodesMap);
        }

        private static boolean isImplicitNullOpportunity(FloatingReadNode floatingReadNode, Block block)
        {
            Node pred = block.getBeginNode().predecessor();
            if (pred instanceof IfNode)
            {
                IfNode ifNode = (IfNode) pred;
                if (ifNode.condition() instanceof IsNullNode)
                {
                    IsNullNode isNullNode = (IsNullNode) ifNode.condition();
                    if (getUnproxifiedUncompressed(floatingReadNode.getAddress().getBase()) == getUnproxifiedUncompressed(isNullNode.getValue()))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        private static Node getUnproxifiedUncompressed(Node node)
        {
            Node result = node;
            while (true)
            {
                if (result instanceof ValueProxy)
                {
                    ValueProxy valueProxy = (ValueProxy) result;
                    result = valueProxy.getOriginalNode();
                }
                else if (result instanceof ConvertNode)
                {
                    ConvertNode convertNode = (ConvertNode) result;
                    if (convertNode.mayNullCheckSkipConversion())
                    {
                        result = convertNode.getValue();
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
            return result;
        }

        private static Block calcBlockForUsage(Node node, Node usage, Block startBlock, NodeMap<Block> currentNodeMap)
        {
            Block currentBlock = startBlock;
            if (usage instanceof PhiNode)
            {
                // An input to a PhiNode is used at the end of the predecessor block that
                // corresponds to the PhiNode input. One PhiNode can use an input multiple times.
                PhiNode phi = (PhiNode) usage;
                AbstractMergeNode merge = phi.merge();
                Block mergeBlock = currentNodeMap.get(merge);
                for (int i = 0; i < phi.valueCount(); ++i)
                {
                    if (phi.valueAt(i) == node)
                    {
                        Block otherBlock = mergeBlock.getPredecessors()[i];
                        currentBlock = AbstractControlFlowGraph.commonDominatorTyped(currentBlock, otherBlock);
                    }
                }
            }
            else if (usage instanceof AbstractBeginNode)
            {
                AbstractBeginNode abstractBeginNode = (AbstractBeginNode) usage;
                if (abstractBeginNode instanceof StartNode)
                {
                    currentBlock = AbstractControlFlowGraph.commonDominatorTyped(currentBlock, currentNodeMap.get(abstractBeginNode));
                }
                else
                {
                    Block otherBlock = currentNodeMap.get(abstractBeginNode).getDominator();
                    currentBlock = AbstractControlFlowGraph.commonDominatorTyped(currentBlock, otherBlock);
                }
            }
            else
            {
                // All other types of usages: Put the input into the same block as the usage.
                Block otherBlock = currentNodeMap.get(usage);
                if (usage instanceof ProxyNode)
                {
                    ProxyNode proxyNode = (ProxyNode) usage;
                    otherBlock = currentNodeMap.get(proxyNode.proxyPoint());
                }
                currentBlock = AbstractControlFlowGraph.commonDominatorTyped(currentBlock, otherBlock);
            }
            return currentBlock;
        }

        /**
         * Micro block that is allocated for each fixed node and captures all floating nodes that
         * need to be scheduled immediately after the corresponding fixed node.
         */
        // @class SchedulePhase.Instance.MicroBlock
        private static final class MicroBlock
        {
            private final int id;
            private int nodeCount;
            private NodeEntry head;
            private NodeEntry tail;

            // @cons
            MicroBlock(int id)
            {
                super();
                this.id = id;
            }

            /**
             * Adds a new floating node into the micro block.
             */
            public void add(Node node)
            {
                NodeEntry newTail = new NodeEntry(node);
                if (tail == null)
                {
                    tail = head = newTail;
                }
                else
                {
                    tail.next = newTail;
                    tail = newTail;
                }
                nodeCount++;
            }

            /**
             * Number of nodes in this micro block.
             */
            public int getNodeCount()
            {
                return nodeCount;
            }

            private int getActualNodeCount()
            {
                int count = 0;
                for (NodeEntry e = head; e != null; e = e.next)
                {
                    count++;
                }
                return count;
            }

            /**
             * The id of the micro block, with a block always associated with a lower id than its successors.
             */
            public int getId()
            {
                return id;
            }

            /**
             * First node of the linked list of nodes of this micro block.
             */
            public NodeEntry getFirstNode()
            {
                return head;
            }

            /**
             * Takes all nodes in this micro blocks and prepends them to the nodes of the given parameter.
             *
             * @param newBlock the new block for the nodes
             */
            public void prependChildrenTo(MicroBlock newBlock)
            {
                if (tail != null)
                {
                    tail.next = newBlock.head;
                    newBlock.head = head;
                    head = tail = null;
                    newBlock.nodeCount += nodeCount;
                    nodeCount = 0;
                }
            }

            @Override
            public int hashCode()
            {
                return id;
            }
        }

        /**
         * Entry in the linked list of nodes.
         */
        // @class SchedulePhase.Instance.NodeEntry
        private static final class NodeEntry
        {
            private final Node node;
            private NodeEntry next;

            // @cons
            NodeEntry(Node node)
            {
                super();
                this.node = node;
                this.next = null;
            }

            public NodeEntry getNext()
            {
                return next;
            }

            public Node getNode()
            {
                return node;
            }
        }

        private void scheduleEarliestIterative(BlockMap<List<Node>> blockToNodes, NodeMap<Block> nodeToBlock, NodeBitMap visited, StructuredGraph graph, boolean immutableGraph, boolean withGuardOrder)
        {
            NodeMap<MicroBlock> entries = graph.createNodeMap();
            NodeStack stack = new NodeStack();

            // Initialize with fixed nodes.
            MicroBlock startBlock = null;
            int nextId = 1;
            for (Block b : cfg.reversePostOrder())
            {
                for (FixedNode current : b.getBeginNode().getBlockNodes())
                {
                    MicroBlock microBlock = new MicroBlock(nextId++);
                    entries.set(current, microBlock);
                    boolean isNew = visited.checkAndMarkInc(current);
                    if (startBlock == null)
                    {
                        startBlock = microBlock;
                    }
                }
            }

            if (graph.getGuardsStage().allowsFloatingGuards() && graph.getNodes(GuardNode.TYPE).isNotEmpty())
            {
                // Now process guards.
                if (GraalOptions.GuardPriorities.getValue(graph.getOptions()) && withGuardOrder)
                {
                    EnumMap<GuardPriority, List<GuardNode>> guardsByPriority = new EnumMap<>(GuardPriority.class);
                    for (GuardNode guard : graph.getNodes(GuardNode.TYPE))
                    {
                        guardsByPriority.computeIfAbsent(guard.computePriority(), p -> new ArrayList<>()).add(guard);
                    }
                    // 'EnumMap.values' returns values in "natural" key order
                    for (List<GuardNode> guards : guardsByPriority.values())
                    {
                        processNodes(visited, entries, stack, startBlock, guards);
                    }
                    GuardOrder.resortGuards(graph, entries, stack);
                }
                else
                {
                    processNodes(visited, entries, stack, startBlock, graph.getNodes(GuardNode.TYPE));
                }
            }

            // Now process inputs of fixed nodes.
            for (Block b : cfg.reversePostOrder())
            {
                for (FixedNode current : b.getBeginNode().getBlockNodes())
                {
                    processNodes(visited, entries, stack, startBlock, current.inputs());
                }
            }

            if (visited.getCounter() < graph.getNodeCount())
            {
                // Visit back input edges of loop phis.
                boolean changed;
                boolean unmarkedPhi;
                do
                {
                    changed = false;
                    unmarkedPhi = false;
                    for (LoopBeginNode loopBegin : graph.getNodes(LoopBeginNode.TYPE))
                    {
                        for (PhiNode phi : loopBegin.phis())
                        {
                            if (visited.isMarked(phi))
                            {
                                for (int i = 0; i < loopBegin.getLoopEndCount(); ++i)
                                {
                                    Node node = phi.valueAt(i + loopBegin.forwardEndCount());
                                    if (node != null && entries.get(node) == null)
                                    {
                                        changed = true;
                                        processStack(node, startBlock, entries, visited, stack);
                                    }
                                }
                            }
                            else
                            {
                                unmarkedPhi = true;
                            }
                        }
                    }

                    // the processing of one loop phi could have marked a previously checked loop phi, therefore this needs to be iterative.
                } while (unmarkedPhi && changed);
            }

            // Check for dead nodes.
            if (!immutableGraph && visited.getCounter() < graph.getNodeCount())
            {
                for (Node n : graph.getNodes())
                {
                    if (!visited.isMarked(n))
                    {
                        n.clearInputs();
                        n.markDeleted();
                    }
                }
            }

            for (Block b : cfg.reversePostOrder())
            {
                FixedNode fixedNode = b.getEndNode();
                if (fixedNode instanceof ControlSplitNode)
                {
                    ControlSplitNode controlSplitNode = (ControlSplitNode) fixedNode;
                    MicroBlock endBlock = entries.get(fixedNode);
                    AbstractBeginNode primarySuccessor = controlSplitNode.getPrimarySuccessor();
                    if (primarySuccessor != null)
                    {
                        endBlock.prependChildrenTo(entries.get(primarySuccessor));
                    }
                }
            }

            // create lists for each block
            for (Block b : cfg.reversePostOrder())
            {
                // count nodes in block
                int totalCount = 0;
                for (FixedNode current : b.getBeginNode().getBlockNodes())
                {
                    MicroBlock microBlock = entries.get(current);
                    totalCount += microBlock.getNodeCount() + 1;
                }

                // initialize with begin node, it is always the first node
                ArrayList<Node> nodes = new ArrayList<>(totalCount);
                blockToNodes.put(b, nodes);

                for (FixedNode current : b.getBeginNode().getBlockNodes())
                {
                    MicroBlock microBlock = entries.get(current);
                    nodeToBlock.set(current, b);
                    nodes.add(current);
                    NodeEntry next = microBlock.getFirstNode();
                    while (next != null)
                    {
                        Node nextNode = next.getNode();
                        nodeToBlock.set(nextNode, b);
                        nodes.add(nextNode);
                        next = next.getNext();
                    }
                }
            }
        }

        private static void processNodes(NodeBitMap visited, NodeMap<MicroBlock> entries, NodeStack stack, MicroBlock startBlock, Iterable<? extends Node> nodes)
        {
            for (Node node : nodes)
            {
                if (entries.get(node) == null)
                {
                    processStack(node, startBlock, entries, visited, stack);
                }
            }
        }

        private static void processStackPhi(NodeStack stack, PhiNode phiNode, NodeMap<MicroBlock> nodeToBlock, NodeBitMap visited)
        {
            stack.pop();
            if (visited.checkAndMarkInc(phiNode))
            {
                MicroBlock mergeBlock = nodeToBlock.get(phiNode.merge());
                nodeToBlock.set(phiNode, mergeBlock);
                AbstractMergeNode merge = phiNode.merge();
                for (int i = 0; i < merge.forwardEndCount(); ++i)
                {
                    Node input = phiNode.valueAt(i);
                    if (input != null && nodeToBlock.get(input) == null)
                    {
                        stack.push(input);
                    }
                }
            }
        }

        private static void processStackProxy(NodeStack stack, ProxyNode proxyNode, NodeMap<MicroBlock> nodeToBlock, NodeBitMap visited)
        {
            stack.pop();
            if (visited.checkAndMarkInc(proxyNode))
            {
                nodeToBlock.set(proxyNode, nodeToBlock.get(proxyNode.proxyPoint()));
                Node input = proxyNode.value();
                if (input != null && nodeToBlock.get(input) == null)
                {
                    stack.push(input);
                }
            }
        }

        private static void processStack(Node first, MicroBlock startBlock, NodeMap<MicroBlock> nodeToMicroBlock, NodeBitMap visited, NodeStack stack)
        {
            stack.push(first);
            Node current = first;
            while (true)
            {
                if (current instanceof PhiNode)
                {
                    processStackPhi(stack, (PhiNode) current, nodeToMicroBlock, visited);
                }
                else if (current instanceof ProxyNode)
                {
                    processStackProxy(stack, (ProxyNode) current, nodeToMicroBlock, visited);
                }
                else
                {
                    MicroBlock currentBlock = nodeToMicroBlock.get(current);
                    if (currentBlock == null)
                    {
                        MicroBlock earliestBlock = processInputs(nodeToMicroBlock, stack, startBlock, current);
                        if (earliestBlock == null)
                        {
                            // We need to delay until inputs are processed.
                        }
                        else
                        {
                            // Can immediately process and pop.
                            stack.pop();
                            visited.checkAndMarkInc(current);
                            nodeToMicroBlock.set(current, earliestBlock);
                            earliestBlock.add(current);
                        }
                    }
                    else
                    {
                        stack.pop();
                    }
                }

                if (stack.isEmpty())
                {
                    break;
                }
                current = stack.peek();
            }
        }

        // @class SchedulePhase.Instance.GuardOrder
        private static final class GuardOrder
        {
            /**
             * After an earliest schedule, this will re-sort guards to honor their
             * {@linkplain StaticDeoptimizingNode#computePriority() priority}.
             *
             * Note that this only changes the order of nodes within {@linkplain MicroBlock
             * micro-blocks}, nodes will not be moved from one micro-block to another.
             */
            private static void resortGuards(StructuredGraph graph, NodeMap<MicroBlock> entries, NodeStack stack)
            {
                EconomicSet<MicroBlock> blocksWithGuards = EconomicSet.create(Equivalence.IDENTITY);
                for (GuardNode guard : graph.getNodes(GuardNode.TYPE))
                {
                    MicroBlock block = entries.get(guard);
                    blocksWithGuards.add(block);
                }
                NodeMap<GuardPriority> priorities = graph.createNodeMap();
                NodeBitMap blockNodes = graph.createNodeBitMap();
                for (MicroBlock block : blocksWithGuards)
                {
                    MicroBlock newBlock = resortGuards(block, stack, blockNodes, priorities);
                    if (newBlock != null)
                    {
                        block.head = newBlock.head;
                        block.tail = newBlock.tail;
                    }
                }
            }

            /**
             * This resorts guards within one micro-block.
             *
             * {@code stack}, {@code blockNodes} and {@code priorities} are just temporary
             * data-structures which are allocated once by the callers of this method. They should
             * be in their "initial"/"empty" state when calling this method and when it returns.
             */
            private static MicroBlock resortGuards(MicroBlock block, NodeStack stack, NodeBitMap blockNodes, NodeMap<GuardPriority> priorities)
            {
                if (!propagatePriority(block, stack, priorities, blockNodes))
                {
                    return null;
                }

                Function<GuardNode, GuardPriority> transitiveGuardPriorityGetter = priorities::get;
                Comparator<GuardNode> globalGuardPriorityComparator = Comparator.comparing(transitiveGuardPriorityGetter).thenComparing(GuardNode::computePriority).thenComparingInt(Node::hashCode);

                SortedSet<GuardNode> availableGuards = new TreeSet<>(globalGuardPriorityComparator);
                MicroBlock newBlock = new MicroBlock(block.getId());

                NodeBitMap sorted = blockNodes;
                sorted.invert();

                for (NodeEntry e = block.head; e != null; e = e.next)
                {
                    checkIfAvailable(e.node, stack, sorted, newBlock, availableGuards, false);
                }
                do
                {
                    while (!stack.isEmpty())
                    {
                        checkIfAvailable(stack.pop(), stack, sorted, newBlock, availableGuards, true);
                    }
                    Iterator<GuardNode> iterator = availableGuards.iterator();
                    if (iterator.hasNext())
                    {
                        addNodeToResort(iterator.next(), stack, sorted, newBlock, true);
                        iterator.remove();
                    }
                } while (!stack.isEmpty() || !availableGuards.isEmpty());

                blockNodes.clearAll();
                return newBlock;
            }

            /**
             * This checks if {@code n} can be scheduled, if it is the case, it schedules it now by
             * calling {@link #addNodeToResort(Node, NodeStack, NodeBitMap, MicroBlock, boolean)}.
             */
            private static void checkIfAvailable(Node n, NodeStack stack, NodeBitMap sorted, Instance.MicroBlock newBlock, SortedSet<GuardNode> availableGuardNodes, boolean pushUsages)
            {
                if (sorted.isMarked(n))
                {
                    return;
                }
                for (Node in : n.inputs())
                {
                    if (!sorted.isMarked(in))
                    {
                        return;
                    }
                }
                if (n instanceof GuardNode)
                {
                    availableGuardNodes.add((GuardNode) n);
                }
                else
                {
                    addNodeToResort(n, stack, sorted, newBlock, pushUsages);
                }
            }

            /**
             * Add a node to the re-sorted micro-block. This also pushes nodes that need to be
             * (re-)examined on the stack.
             */
            private static void addNodeToResort(Node n, NodeStack stack, NodeBitMap sorted, MicroBlock newBlock, boolean pushUsages)
            {
                sorted.mark(n);
                newBlock.add(n);
                if (pushUsages)
                {
                    for (Node u : n.usages())
                    {
                        if (!sorted.isMarked(u))
                        {
                            stack.push(u);
                        }
                    }
                }
            }

            /**
             * This fills in a map of transitive priorities ({@code priorities}). It also marks the
             * nodes from this micro-block in {@code blockNodes}.
             *
             * The transitive priority of a guard is the highest of its priority and the priority of
             * the guards that depend on it (transitively).
             *
             * This method returns {@code false} if no re-ordering is necessary in this micro-block.
             */
            private static boolean propagatePriority(MicroBlock block, NodeStack stack, NodeMap<GuardPriority> priorities, NodeBitMap blockNodes)
            {
                GuardPriority lowestPriority = GuardPriority.highest();
                for (NodeEntry e = block.head; e != null; e = e.next)
                {
                    blockNodes.mark(e.node);
                    if (e.node instanceof GuardNode)
                    {
                        GuardNode guard = (GuardNode) e.node;
                        GuardPriority priority = guard.computePriority();
                        if (lowestPriority != null)
                        {
                            if (priority.isLowerPriorityThan(lowestPriority))
                            {
                                lowestPriority = priority;
                            }
                            else if (priority.isHigherPriorityThan(lowestPriority))
                            {
                                lowestPriority = null;
                            }
                        }
                        stack.push(guard);
                        priorities.set(guard, priority);
                    }
                }
                if (lowestPriority != null)
                {
                    stack.clear();
                    blockNodes.clearAll();
                    return false;
                }

                do
                {
                    Node current = stack.pop();
                    GuardPriority priority = priorities.get(current);
                    for (Node input : current.inputs())
                    {
                        if (!blockNodes.isMarked(input))
                        {
                            continue;
                        }
                        GuardPriority inputPriority = priorities.get(input);
                        if (inputPriority == null || inputPriority.isLowerPriorityThan(priority))
                        {
                            priorities.set(input, priority);
                            stack.push(input);
                        }
                    }
                } while (!stack.isEmpty());
                return true;
            }
        }

        /**
         * Processes the inputs of given block. Pushes unprocessed inputs onto the stack. Returns
         * null if there were still unprocessed inputs, otherwise returns the earliest block given
         * node can be scheduled in.
         */
        private static MicroBlock processInputs(NodeMap<MicroBlock> nodeToBlock, NodeStack stack, MicroBlock startBlock, Node current)
        {
            if (current.getNodeClass().isLeafNode())
            {
                return startBlock;
            }

            MicroBlock earliestBlock = startBlock;
            for (Node input : current.inputs())
            {
                MicroBlock inputBlock = nodeToBlock.get(input);
                if (inputBlock == null)
                {
                    earliestBlock = null;
                    stack.push(input);
                }
                else if (earliestBlock != null && inputBlock.getId() > earliestBlock.getId())
                {
                    earliestBlock = inputBlock;
                }
            }
            return earliestBlock;
        }

        private static boolean isFixedEnd(FixedNode endNode)
        {
            return endNode instanceof ControlSplitNode || endNode instanceof ControlSinkNode || endNode instanceof AbstractEndNode;
        }

        public ControlFlowGraph getCFG()
        {
            return cfg;
        }

        /**
         * Gets the nodes in a given block.
         */
        public List<Node> nodesFor(Block block)
        {
            return blockToNodesMap.get(block);
        }
    }
}
