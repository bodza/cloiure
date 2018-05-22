package graalvm.compiler.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableEconomicMap;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node.ValueNumberable;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;

/**
 * This class is a graph container, it contains the set of nodes that belong to this graph.
 */
public class Graph
{
    public static class Options
    {
        // Option "Graal graph compression is performed when percent of live nodes falls below this value."
        public static final OptionKey<Integer> GraphCompressionThreshold = new OptionKey<>(70);
    }

    private enum FreezeState
    {
        Unfrozen,
        TemporaryFreeze,
        DeepFreeze
    }

    public final String name;

    /**
     * The set of nodes in the graph, ordered by {@linkplain #register(Node) registration} time.
     */
    Node[] nodes;

    /**
     * The number of valid entries in {@link #nodes}.
     */
    int nodesSize;

    // these two arrays contain one entry for each NodeClass, indexed by NodeClass.iterableId.
    // they contain the first and last pointer to a linked list of all nodes with this type.
    private final ArrayList<Node> iterableNodesFirst;
    private final ArrayList<Node> iterableNodesLast;

    private int nodesDeletedSinceLastCompression;
    private int nodesDeletedBeforeLastCompression;

    /**
     * The number of times this graph has been compressed.
     */
    int compressions;

    NodeEventListener nodeEventListener;

    /**
     * Used to global value number {@link ValueNumberable} {@linkplain NodeClass#isLeafNode() leaf}
     * nodes.
     */
    private EconomicMap<Node, Node>[] cachedLeafNodes;

    private static final Equivalence NODE_VALUE_COMPARE = new Equivalence()
    {
        @Override
        public boolean equals(Object a, Object b)
        {
            if (a == b)
            {
                return true;
            }

            return ((Node) a).valueEquals((Node) b);
        }

        @Override
        public int hashCode(Object k)
        {
            return ((Node) k).getNodeClass().valueNumber((Node) k);
        }
    };

    /**
     * Indicates that the graph should no longer be modified. Frozen graphs can be used by multiple
     * threads so it's only safe to read them.
     */
    private FreezeState freezeState = FreezeState.Unfrozen;

    /**
     * The option values used while compiling this graph.
     */
    private final OptionValues options;

    /**
     * Creates an empty Graph with no name.
     */
    public Graph(OptionValues options)
    {
        this(null, options);
    }

    private static final int INITIAL_NODES_SIZE = 32;

    /**
     * Creates an empty Graph with a given name.
     *
     * @param name the name of the graph, used for debugging purposes
     */
    public Graph(String name, OptionValues options)
    {
        nodes = new Node[INITIAL_NODES_SIZE];
        iterableNodesFirst = new ArrayList<>(NodeClass.allocatedNodeIterabledIds());
        iterableNodesLast = new ArrayList<>(NodeClass.allocatedNodeIterabledIds());
        this.name = name;
        this.options = options;
    }

    /**
     * Creates a copy of this graph.
     */
    public final Graph copy()
    {
        return copy(name, null);
    }

    /**
     * Creates a copy of this graph.
     *
     * @param duplicationMapCallback consumer of the duplication map created during the copying
     */
    public final Graph copy(Consumer<UnmodifiableEconomicMap<Node, Node>> duplicationMapCallback)
    {
        return copy(name, duplicationMapCallback);
    }

    /**
     * Creates a copy of this graph.
     *
     * @param newName the name of the copy, used for debugging purposes (can be null)
     */
    public final Graph copy(String newName)
    {
        return copy(newName, null);
    }

    /**
     * Creates a copy of this graph.
     *
     * @param newName the name of the copy, used for debugging purposes (can be null)
     * @param duplicationMapCallback consumer of the duplication map created during the copying
     */
    protected Graph copy(String newName, Consumer<UnmodifiableEconomicMap<Node, Node>> duplicationMapCallback)
    {
        Graph copy = new Graph(newName, options);
        UnmodifiableEconomicMap<Node, Node> duplicates = copy.addDuplicates(getNodes(), this, this.getNodeCount(), (EconomicMap<Node, Node>) null);
        if (duplicationMapCallback != null)
        {
            duplicationMapCallback.accept(duplicates);
        }
        return copy;
    }

    public final OptionValues getOptions()
    {
        return options;
    }

    @Override
    public String toString()
    {
        return name == null ? super.toString() : "Graph " + name;
    }

    /**
     * Gets the number of live nodes in this graph. That is the number of nodes which have been
     * added to the graph minus the number of deleted nodes.
     *
     * @return the number of live nodes in this graph
     */
    public int getNodeCount()
    {
        return nodesSize - getNodesDeletedSinceLastCompression();
    }

    /**
     * Gets the number of times this graph has been {@linkplain #maybeCompress() compressed}. Node
     * identifiers are only stable between compressions. To ensure this constraint is observed, any
     * entity relying upon stable node identifiers should use {@link NodeIdAccessor}.
     */
    public int getCompressions()
    {
        return compressions;
    }

    /**
     * Gets the number of nodes which have been deleted from this graph since it was last
     * {@linkplain #maybeCompress() compressed}.
     */
    public int getNodesDeletedSinceLastCompression()
    {
        return nodesDeletedSinceLastCompression;
    }

    /**
     * Gets the total number of nodes which have been deleted from this graph.
     */
    public int getTotalNodesDeleted()
    {
        return nodesDeletedSinceLastCompression + nodesDeletedBeforeLastCompression;
    }

    /**
     * Adds a new node to the graph.
     *
     * @param node the node to be added
     * @return the node which was added to the graph
     */
    public <T extends Node> T add(T node)
    {
        if (node.getNodeClass().valueNumberable())
        {
            throw new IllegalStateException("Using add for value numberable node. Consider using either unique or addWithoutUnique.");
        }
        return addHelper(node);
    }

    public <T extends Node> T addWithoutUnique(T node)
    {
        return addHelper(node);
    }

    public <T extends Node> T addOrUnique(T node)
    {
        if (node.getNodeClass().valueNumberable())
        {
            return uniqueHelper(node);
        }
        return add(node);
    }

    public <T extends Node> T maybeAddOrUnique(T node)
    {
        if (node.isAlive())
        {
            return node;
        }
        return addOrUnique(node);
    }

    public <T extends Node> T addOrUniqueWithInputs(T node)
    {
        if (node.isAlive())
        {
            return node;
        }
        else
        {
            addInputs(node);
            if (node.getNodeClass().valueNumberable())
            {
                return uniqueHelper(node);
            }
            return add(node);
        }
    }

    public <T extends Node> T addWithoutUniqueWithInputs(T node)
    {
        addInputs(node);
        return addHelper(node);
    }

    private final class AddInputsFilter extends Node.EdgeVisitor
    {
        @Override
        public Node apply(Node self, Node input)
        {
            if (!input.isAlive())
            {
                return addOrUniqueWithInputs(input);
            }
            else
            {
                return input;
            }
        }
    }

    private AddInputsFilter addInputsFilter = new AddInputsFilter();

    private <T extends Node> void addInputs(T node)
    {
        node.applyInputs(addInputsFilter);
    }

    private <T extends Node> T addHelper(T node)
    {
        node.initialize(this);
        return node;
    }

    /**
     * The type of events sent to a {@link NodeEventListener}.
     */
    public enum NodeEvent
    {
        /**
         * A node's input is changed.
         */
        INPUT_CHANGED,

        /**
         * A node's {@linkplain Node#usages() usages} count dropped to zero.
         */
        ZERO_USAGES,

        /**
         * A node was added to a graph.
         */
        NODE_ADDED,

        /**
         * A node was removed from the graph.
         */
        NODE_REMOVED;
    }

    /**
     * Client interested in one or more node related events.
     */
    public abstract static class NodeEventListener
    {
        /**
         * A method called when a change event occurs.
         *
         * This method dispatches the event to user-defined triggers. The methods that change the
         * graph (typically in Graph and Node) must call this method to dispatch the event.
         *
         * @param e an event
         * @param node the node related to {@code e}
         */
        final void event(NodeEvent e, Node node)
        {
            switch (e)
            {
                case INPUT_CHANGED:
                    inputChanged(node);
                    break;
                case ZERO_USAGES:
                    usagesDroppedToZero(node);
                    break;
                case NODE_ADDED:
                    nodeAdded(node);
                    break;
                case NODE_REMOVED:
                    nodeRemoved(node);
                    break;
            }
            changed(e, node);
        }

        /**
         * Notifies this listener about any change event in the graph.
         *
         * @param e an event
         * @param node the node related to {@code e}
         */
        public void changed(NodeEvent e, Node node)
        {
        }

        /**
         * Notifies this listener about a change in a node's inputs.
         *
         * @param node a node who has had one of its inputs changed
         */
        public void inputChanged(Node node)
        {
        }

        /**
         * Notifies this listener of a node becoming unused.
         *
         * @param node a node whose {@link Node#usages()} just became empty
         */
        public void usagesDroppedToZero(Node node)
        {
        }

        /**
         * Notifies this listener of an added node.
         *
         * @param node a node that was just added to the graph
         */
        public void nodeAdded(Node node)
        {
        }

        /**
         * Notifies this listener of a removed node.
         */
        public void nodeRemoved(Node node)
        {
        }
    }

    /**
     * Registers a given {@link NodeEventListener} with the enclosing graph until this object is
     * {@linkplain #close() closed}.
     */
    public final class NodeEventScope implements AutoCloseable
    {
        NodeEventScope(NodeEventListener listener)
        {
            if (nodeEventListener == null)
            {
                nodeEventListener = listener;
            }
            else
            {
                nodeEventListener = new ChainedNodeEventListener(listener, nodeEventListener);
            }
        }

        @Override
        public void close()
        {
            if (nodeEventListener instanceof ChainedNodeEventListener)
            {
                nodeEventListener = ((ChainedNodeEventListener) nodeEventListener).next;
            }
            else
            {
                nodeEventListener = null;
            }
        }
    }

    private static class ChainedNodeEventListener extends NodeEventListener
    {
        NodeEventListener head;
        NodeEventListener next;

        ChainedNodeEventListener(NodeEventListener head, NodeEventListener next)
        {
            this.head = head;
            this.next = next;
        }

        @Override
        public void nodeAdded(Node node)
        {
            head.event(NodeEvent.NODE_ADDED, node);
            next.event(NodeEvent.NODE_ADDED, node);
        }

        @Override
        public void inputChanged(Node node)
        {
            head.event(NodeEvent.INPUT_CHANGED, node);
            next.event(NodeEvent.INPUT_CHANGED, node);
        }

        @Override
        public void usagesDroppedToZero(Node node)
        {
            head.event(NodeEvent.ZERO_USAGES, node);
            next.event(NodeEvent.ZERO_USAGES, node);
        }

        @Override
        public void nodeRemoved(Node node)
        {
            head.event(NodeEvent.NODE_REMOVED, node);
            next.event(NodeEvent.NODE_REMOVED, node);
        }

        @Override
        public void changed(NodeEvent e, Node node)
        {
            head.event(e, node);
            next.event(e, node);
        }
    }

    /**
     * Registers a given {@link NodeEventListener} with this graph. This should be used in
     * conjunction with try-with-resources statement as follows:
     *
     * <pre>
     * try (NodeEventScope nes = graph.trackNodeEvents(listener)) {
     *     // make changes to the graph
     * }
     * </pre>
     */
    public NodeEventScope trackNodeEvents(NodeEventListener listener)
    {
        return new NodeEventScope(listener);
    }

    /**
     * Looks for a node <i>similar</i> to {@code node} and returns it if found. Otherwise
     * {@code node} is added to this graph and returned.
     *
     * @return a node similar to {@code node} if one exists, otherwise {@code node}
     */
    public <T extends Node & ValueNumberable> T unique(T node)
    {
        return uniqueHelper(node);
    }

    <T extends Node> T uniqueHelper(T node)
    {
        T other = this.findDuplicate(node);
        if (other != null)
        {
            return other;
        }
        else
        {
            T result = addHelper(node);
            if (node.getNodeClass().isLeafNode())
            {
                putNodeIntoCache(result);
            }
            return result;
        }
    }

    void removeNodeFromCache(Node node)
    {
        int leafId = node.getNodeClass().getLeafId();
        if (cachedLeafNodes != null && cachedLeafNodes.length > leafId && cachedLeafNodes[leafId] != null)
        {
            cachedLeafNodes[leafId].removeKey(node);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    void putNodeIntoCache(Node node)
    {
        int leafId = node.getNodeClass().getLeafId();
        if (cachedLeafNodes == null || cachedLeafNodes.length <= leafId)
        {
            EconomicMap[] newLeafNodes = new EconomicMap[leafId + 1];
            if (cachedLeafNodes != null)
            {
                System.arraycopy(cachedLeafNodes, 0, newLeafNodes, 0, cachedLeafNodes.length);
            }
            cachedLeafNodes = newLeafNodes;
        }

        if (cachedLeafNodes[leafId] == null)
        {
            cachedLeafNodes[leafId] = EconomicMap.create(NODE_VALUE_COMPARE);
        }

        cachedLeafNodes[leafId].put(node, node);
    }

    Node findNodeInCache(Node node)
    {
        int leafId = node.getNodeClass().getLeafId();
        if (cachedLeafNodes == null || cachedLeafNodes.length <= leafId || cachedLeafNodes[leafId] == null)
        {
            return null;
        }

        Node result = cachedLeafNodes[leafId].get(node);
        return result;
    }

    /**
     * Returns a possible duplicate for the given node in the graph or {@code null} if no such
     * duplicate exists.
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T findDuplicate(T node)
    {
        NodeClass<?> nodeClass = node.getNodeClass();
        if (nodeClass.isLeafNode())
        {
            // Leaf node: look up in cache
            Node cachedNode = findNodeInCache(node);
            if (cachedNode != null && cachedNode != node)
            {
                return (T) cachedNode;
            }
            else
            {
                return null;
            }
        }
        else
        {
            /*
             * Non-leaf node: look for another usage of the node's inputs that has the same data,
             * inputs and successors as the node. To reduce the cost of this computation, only the
             * input with lowest usage count is considered. If this node is the only user of any
             * input then the search can terminate early. The usage count is only incremented once
             * the Node is in the Graph, so account for that in the test.
             */
            final int earlyExitUsageCount = node.graph() != null ? 1 : 0;
            int minCount = Integer.MAX_VALUE;
            Node minCountNode = null;
            for (Node input : node.inputs())
            {
                int usageCount = input.getUsageCount();
                if (usageCount == earlyExitUsageCount)
                {
                    return null;
                }
                else if (usageCount < minCount)
                {
                    minCount = usageCount;
                    minCountNode = input;
                }
            }
            if (minCountNode != null)
            {
                for (Node usage : minCountNode.usages())
                {
                    if (usage != node && nodeClass == usage.getNodeClass() && node.valueEquals(usage) && nodeClass.equalInputs(node, usage) && nodeClass.equalSuccessors(node, usage))
                    {
                        return (T) usage;
                    }
                }
                return null;
            }
            return null;
        }
    }

    public boolean isNew(Mark mark, Node node)
    {
        return node.id >= mark.getValue();
    }

    /**
     * A snapshot of the {@linkplain Graph#getNodeCount() live node count} in a graph.
     */
    public static class Mark extends NodeIdAccessor
    {
        private final int value;

        Mark(Graph graph)
        {
            super(graph);
            this.value = graph.nodeIdCount();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Mark)
            {
                Mark other = (Mark) obj;
                return other.getValue() == getValue() && other.getGraph() == getGraph();
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return value ^ (epoch + 11);
        }

        /**
         * Determines if this mark is positioned at the first live node in the graph.
         */
        public boolean isStart()
        {
            return value == 0;
        }

        /**
         * Gets the {@linkplain Graph#getNodeCount() live node count} of the associated graph when
         * this object was created.
         */
        int getValue()
        {
            return value;
        }

        /**
         * Determines if this mark still represents the {@linkplain Graph#getNodeCount() live node
         * count} of the graph.
         */
        public boolean isCurrent()
        {
            return value == graph.nodeIdCount();
        }
    }

    /**
     * Gets a mark that can be used with {@link #getNewNodes}.
     */
    public Mark getMark()
    {
        return new Mark(this);
    }

    /**
     * Returns an {@link Iterable} providing all nodes added since the last {@link Graph#getMark()
     * mark}.
     */
    public NodeIterable<Node> getNewNodes(Mark mark)
    {
        final int index = mark == null ? 0 : mark.getValue();
        return new NodeIterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new GraphNodeIterator(Graph.this, index);
            }
        };
    }

    /**
     * Returns an {@link Iterable} providing all the live nodes.
     *
     * @return an {@link Iterable} providing all the live nodes.
     */
    public NodeIterable<Node> getNodes()
    {
        return new NodeIterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new GraphNodeIterator(Graph.this);
            }

            @Override
            public int count()
            {
                return getNodeCount();
            }
        };
    }

    static final class PlaceHolderNode extends Node
    {
        public static final NodeClass<PlaceHolderNode> TYPE = NodeClass.create(PlaceHolderNode.class);

        protected PlaceHolderNode()
        {
            super(TYPE);
        }
    }

    /**
     * If the {@linkplain Options#GraphCompressionThreshold compression threshold} is met, the list
     * of nodes is compressed such that all non-null entries precede all null entries while
     * preserving the ordering between the nodes within the list.
     */
    public boolean maybeCompress()
    {
        int liveNodeCount = getNodeCount();
        int liveNodePercent = liveNodeCount * 100 / nodesSize;
        int compressionThreshold = Options.GraphCompressionThreshold.getValue(options);
        if (compressionThreshold == 0 || liveNodePercent >= compressionThreshold)
        {
            return false;
        }
        int nextId = 0;
        for (int i = 0; nextId < liveNodeCount; i++)
        {
            Node n = nodes[i];
            if (n != null)
            {
                if (i != nextId)
                {
                    n.id = nextId;
                    nodes[nextId] = n;
                    nodes[i] = null;
                }
                nextId++;
            }
        }
        nodesSize = nextId;
        compressions++;
        nodesDeletedBeforeLastCompression += nodesDeletedSinceLastCompression;
        nodesDeletedSinceLastCompression = 0;
        return true;
    }

    /**
     * Returns an {@link Iterable} providing all the live nodes whose type is compatible with
     * {@code type}.
     *
     * @param nodeClass the type of node to return
     * @return an {@link Iterable} providing all the matching nodes
     */
    public <T extends Node & IterableNodeType> NodeIterable<T> getNodes(final NodeClass<T> nodeClass)
    {
        return new NodeIterable<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                return new TypedGraphNodeIterator<>(nodeClass, Graph.this);
            }
        };
    }

    /**
     * Returns whether the graph contains at least one node of the given type.
     *
     * @param type the type of node that is checked for occurrence
     * @return whether there is at least one such node
     */
    public <T extends Node & IterableNodeType> boolean hasNode(final NodeClass<T> type)
    {
        return getNodes(type).iterator().hasNext();
    }

    /**
     * @return the first live Node with a matching iterableId
     */
    Node getIterableNodeStart(int iterableId)
    {
        if (iterableNodesFirst.size() <= iterableId)
        {
            return null;
        }
        Node start = iterableNodesFirst.get(iterableId);
        if (start == null || !start.isDeleted())
        {
            return start;
        }
        return findFirstLiveIterable(iterableId, start);
    }

    private Node findFirstLiveIterable(int iterableId, Node node)
    {
        Node start = node;
        while (start != null && start.isDeleted())
        {
            start = start.typeCacheNext;
        }
        /*
         * Multiple threads iterating nodes can update this cache simultaneously. This is a benign
         * race, since all threads update it to the same value.
         */
        iterableNodesFirst.set(iterableId, start);
        if (start == null)
        {
            iterableNodesLast.set(iterableId, start);
        }
        return start;
    }

    /**
     * @return return the first live Node with a matching iterableId starting from {@code node}
     */
    Node getIterableNodeNext(Node node)
    {
        if (node == null)
        {
            return null;
        }
        Node n = node;
        if (n == null || !n.isDeleted())
        {
            return n;
        }

        return findNextLiveiterable(node);
    }

    private Node findNextLiveiterable(Node start)
    {
        Node n = start;
        while (n != null && n.isDeleted())
        {
            n = n.typeCacheNext;
        }
        if (n == null)
        {
            // Only dead nodes after this one
            start.typeCacheNext = null;
            int nodeClassId = start.getNodeClass().iterableId();
            iterableNodesLast.set(nodeClassId, start);
        }
        else
        {
            // Everything in between is dead
            start.typeCacheNext = n;
        }
        return n;
    }

    public NodeBitMap createNodeBitMap()
    {
        return new NodeBitMap(this);
    }

    public <T> NodeMap<T> createNodeMap()
    {
        return new NodeMap<>(this);
    }

    public NodeFlood createNodeFlood()
    {
        return new NodeFlood(this);
    }

    public NodeWorkList createNodeWorkList()
    {
        return new NodeWorkList.SingletonNodeWorkList(this);
    }

    public NodeWorkList createIterativeNodeWorkList(boolean fill, int iterationLimitPerNode)
    {
        return new NodeWorkList.IterativeNodeWorkList(this, fill, iterationLimitPerNode);
    }

    void register(Node node)
    {
        if (nodes.length == nodesSize)
        {
            grow();
        }
        int id = nodesSize++;
        nodes[id] = node;
        node.id = id;

        updateNodeCaches(node);

        if (nodeEventListener != null)
        {
            nodeEventListener.event(NodeEvent.NODE_ADDED, node);
        }
        afterRegister(node);
    }

    private void grow()
    {
        Node[] newNodes = new Node[(nodesSize * 2) + 1];
        System.arraycopy(nodes, 0, newNodes, 0, nodesSize);
        nodes = newNodes;
    }

    @SuppressWarnings("unused")
    protected void afterRegister(Node node)
    {
    }

    @SuppressWarnings("unused")
    private void postDeserialization()
    {
        recomputeIterableNodeLists();
    }

    /**
     * Rebuilds the lists used to support {@link #getNodes(NodeClass)}. This is useful for
     * serialization where the underlying {@linkplain NodeClass#iterableId() iterable ids} may have
     * changed.
     */
    private void recomputeIterableNodeLists()
    {
        iterableNodesFirst.clear();
        iterableNodesLast.clear();
        for (Node node : nodes)
        {
            if (node != null && node.isAlive())
            {
                updateNodeCaches(node);
            }
        }
    }

    private void updateNodeCaches(Node node)
    {
        int nodeClassId = node.getNodeClass().iterableId();
        if (nodeClassId != Node.NOT_ITERABLE)
        {
            while (iterableNodesFirst.size() <= nodeClassId)
            {
                iterableNodesFirst.add(null);
                iterableNodesLast.add(null);
            }
            Node prev = iterableNodesLast.get(nodeClassId);
            if (prev != null)
            {
                prev.typeCacheNext = node;
            }
            else
            {
                iterableNodesFirst.set(nodeClassId, node);
            }
            iterableNodesLast.set(nodeClassId, node);
        }
    }

    void unregister(Node node)
    {
        if (node.getNodeClass().isLeafNode() && node.getNodeClass().valueNumberable())
        {
            removeNodeFromCache(node);
        }
        nodes[node.id] = null;
        nodesDeletedSinceLastCompression++;

        if (nodeEventListener != null)
        {
            nodeEventListener.event(NodeEvent.NODE_ADDED, node);
        }

        // nodes aren't removed from the type cache here - they will be removed during iteration
    }

    public Node getNode(int id)
    {
        return nodes[id];
    }

    /**
     * Returns the number of node ids generated so far.
     *
     * @return the number of node ids generated so far
     */
    int nodeIdCount()
    {
        return nodesSize;
    }

    /**
     * Adds duplicates of the nodes in {@code newNodes} to this graph. This will recreate any edges
     * between the duplicate nodes. The {@code replacement} map can be used to replace a node from
     * the source graph by a given node (which must already be in this graph). Edges between
     * duplicate and replacement nodes will also be recreated so care should be taken regarding the
     * matching of node types in the replacement map.
     *
     * @param newNodes the nodes to be duplicated
     * @param replacementsMap the replacement map (can be null if no replacement is to be performed)
     * @return a map which associates the original nodes from {@code nodes} to their duplicates
     */
    public UnmodifiableEconomicMap<Node, Node> addDuplicates(Iterable<? extends Node> newNodes, final Graph oldGraph, int estimatedNodeCount, EconomicMap<Node, Node> replacementsMap)
    {
        DuplicationReplacement replacements;
        if (replacementsMap == null)
        {
            replacements = null;
        }
        else
        {
            replacements = new MapReplacement(replacementsMap);
        }
        return addDuplicates(newNodes, oldGraph, estimatedNodeCount, replacements);
    }

    public interface DuplicationReplacement
    {
        Node replacement(Node original);
    }

    private static final class MapReplacement implements DuplicationReplacement
    {
        private final EconomicMap<Node, Node> map;

        MapReplacement(EconomicMap<Node, Node> map)
        {
            this.map = map;
        }

        @Override
        public Node replacement(Node original)
        {
            Node replacement = map.get(original);
            return replacement != null ? replacement : original;
        }
    }

    public EconomicMap<Node, Node> addDuplicates(Iterable<? extends Node> newNodes, final Graph oldGraph, int estimatedNodeCount, DuplicationReplacement replacements)
    {
        return NodeClass.addGraphDuplicate(this, oldGraph, estimatedNodeCount, newNodes, replacements);
    }

    public boolean isFrozen()
    {
        return freezeState != FreezeState.Unfrozen;
    }

    public void freeze()
    {
        this.freezeState = FreezeState.DeepFreeze;
    }

    public void temporaryFreeze()
    {
        if (this.freezeState == FreezeState.DeepFreeze)
        {
            throw new GraalError("Graph was permanetly frozen.");
        }
        this.freezeState = FreezeState.TemporaryFreeze;
    }

    public void unfreeze()
    {
        if (this.freezeState == FreezeState.DeepFreeze)
        {
            throw new GraalError("Graph was permanetly frozen.");
        }
        this.freezeState = FreezeState.Unfrozen;
    }
}
