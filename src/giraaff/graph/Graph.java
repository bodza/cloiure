package giraaff.graph;

import java.util.ArrayList;
import java.util.Iterator;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableEconomicMap;

import giraaff.core.common.GraalOptions;
import giraaff.graph.Node;
import giraaff.graph.iterators.NodeIterable;

///
// This class is a graph container, it contains the set of nodes that belong to this graph.
///
// @class Graph
public class Graph
{
    // @enum Graph.FreezeState
    private enum FreezeState
    {
        Unfrozen,
        DeepFreeze
    }

    ///
    // The set of nodes in the graph, ordered by {@linkplain #register(Node) registration} time.
    ///
    // @field
    Node[] ___nodes;

    ///
    // The number of valid entries in {@link #nodes}.
    ///
    // @field
    int ___nodesSize;

    // these two arrays contain one entry for each NodeClass, indexed by NodeClass.iterableId.
    // they contain the first and last pointer to a linked list of all nodes with this type.
    // @field
    private final ArrayList<Node> ___iterableNodesFirst;
    // @field
    private final ArrayList<Node> ___iterableNodesLast;

    // @field
    private int ___nodesDeletedSinceLastCompression;
    // @field
    private int ___nodesDeletedBeforeLastCompression;

    ///
    // The number of times this graph has been compressed.
    ///
    // @field
    int ___compressions;

    // @field
    Graph.NodeEventListener ___nodeEventListener;

    ///
    // Used to global value number {@link Node.ValueNumberable} {@linkplain NodeClass#isLeafNode() leaf} nodes.
    ///
    // @field
    private EconomicMap<Node, Node>[] ___cachedLeafNodes;

    // @closure
    private static final Equivalence NODE_VALUE_COMPARE = new Equivalence()
    {
        @Override
        public boolean equals(Object __a, Object __b)
        {
            if (__a == __b)
            {
                return true;
            }

            return ((Node) __a).valueEquals((Node) __b);
        }

        @Override
        public int hashCode(Object __k)
        {
            return ((Node) __k).getNodeClass().valueNumber((Node) __k);
        }
    };

    ///
    // Indicates that the graph should no longer be modified. Frozen graphs can be used by multiple
    // threads so it's only safe to read them.
    ///
    // @field
    private Graph.FreezeState ___freezeState = Graph.FreezeState.Unfrozen;

    // @def
    private static final int INITIAL_NODES_SIZE = 32;

    ///
    // Creates an empty Graph.
    ///
    // @cons Graph
    public Graph()
    {
        super();
        this.___nodes = new Node[INITIAL_NODES_SIZE];
        this.___iterableNodesFirst = new ArrayList<>(NodeClass.allocatedNodeIterabledIds());
        this.___iterableNodesLast = new ArrayList<>(NodeClass.allocatedNodeIterabledIds());
    }

    ///
    // Creates a copy of this graph.
    ///
    public Graph copy()
    {
        return new Graph();
    }

    ///
    // Gets the number of live nodes in this graph. That is the number of nodes which have been
    // added to the graph minus the number of deleted nodes.
    //
    // @return the number of live nodes in this graph
    ///
    public int getNodeCount()
    {
        return this.___nodesSize - getNodesDeletedSinceLastCompression();
    }

    ///
    // Gets the number of times this graph has been {@linkplain #maybeCompress() compressed}. Node
    // identifiers are only stable between compressions. To ensure this constraint is observed, any
    // entity relying upon stable node identifiers should use {@link NodeIdAccessor}.
    ///
    public int getCompressions()
    {
        return this.___compressions;
    }

    ///
    // Gets the number of nodes which have been deleted from this graph since it was last
    // {@linkplain #maybeCompress() compressed}.
    ///
    public int getNodesDeletedSinceLastCompression()
    {
        return this.___nodesDeletedSinceLastCompression;
    }

    ///
    // Gets the total number of nodes which have been deleted from this graph.
    ///
    public int getTotalNodesDeleted()
    {
        return this.___nodesDeletedSinceLastCompression + this.___nodesDeletedBeforeLastCompression;
    }

    ///
    // Adds a new node to the graph.
    //
    // @param node the node to be added
    // @return the node which was added to the graph
    ///
    public <T extends Node> T add(T __node)
    {
        if (__node.getNodeClass().valueNumberable())
        {
            throw new IllegalStateException("Using add for value numberable node. Consider using either unique or addWithoutUnique.");
        }
        return addHelper(__node);
    }

    public <T extends Node> T addWithoutUnique(T __node)
    {
        return addHelper(__node);
    }

    public <T extends Node> T addOrUnique(T __node)
    {
        if (__node.getNodeClass().valueNumberable())
        {
            return uniqueHelper(__node);
        }
        return add(__node);
    }

    public <T extends Node> T maybeAddOrUnique(T __node)
    {
        if (__node.isAlive())
        {
            return __node;
        }
        return addOrUnique(__node);
    }

    public <T extends Node> T addOrUniqueWithInputs(T __node)
    {
        if (__node.isAlive())
        {
            return __node;
        }
        else
        {
            addInputs(__node);
            if (__node.getNodeClass().valueNumberable())
            {
                return uniqueHelper(__node);
            }
            return add(__node);
        }
    }

    public <T extends Node> T addWithoutUniqueWithInputs(T __node)
    {
        addInputs(__node);
        return addHelper(__node);
    }

    // @closure
    private Node.EdgeVisitor addInputsFilter = new Node.EdgeVisitor()
    {
        @Override
        public Node apply(Node __self, Node __input)
        {
            if (!__input.isAlive())
            {
                return addOrUniqueWithInputs(__input);
            }
            else
            {
                return __input;
            }
        }
    };

    private <T extends Node> void addInputs(T __node)
    {
        __node.applyInputs(addInputsFilter);
    }

    private <T extends Node> T addHelper(T __node)
    {
        __node.initialize(this);
        return __node;
    }

    ///
    // The type of events sent to a {@link Graph.NodeEventListener}.
    ///
    // @enum Graph.NodeEvent
    public enum NodeEvent
    {
        ///
        // A node's input is changed.
        ///
        INPUT_CHANGED,

        ///
        // A node's {@linkplain Node#usages() usages} count dropped to zero.
        ///
        ZERO_USAGES,

        ///
        // A node was added to a graph.
        ///
        NODE_ADDED,

        ///
        // A node was removed from the graph.
        ///
        NODE_REMOVED;
    }

    ///
    // Client interested in one or more node related events.
    ///
    // @class Graph.NodeEventListener
    public abstract static class NodeEventListener
    {
        ///
        // A method called when a change event occurs.
        //
        // This method dispatches the event to user-defined triggers. The methods that change the
        // graph (typically in Graph and Node) must call this method to dispatch the event.
        //
        // @param e an event
        // @param node the node related to {@code e}
        ///
        final void event(Graph.NodeEvent __e, Node __node)
        {
            switch (__e)
            {
                case INPUT_CHANGED:
                {
                    inputChanged(__node);
                    break;
                }
                case ZERO_USAGES:
                {
                    usagesDroppedToZero(__node);
                    break;
                }
                case NODE_ADDED:
                {
                    nodeAdded(__node);
                    break;
                }
                case NODE_REMOVED:
                {
                    nodeRemoved(__node);
                    break;
                }
            }
            changed(__e, __node);
        }

        ///
        // Notifies this listener about any change event in the graph.
        //
        // @param e an event
        // @param node the node related to {@code e}
        ///
        public void changed(Graph.NodeEvent __e, Node __node)
        {
        }

        ///
        // Notifies this listener about a change in a node's inputs.
        //
        // @param node a node who has had one of its inputs changed
        ///
        public void inputChanged(Node __node)
        {
        }

        ///
        // Notifies this listener of a node becoming unused.
        //
        // @param node a node whose {@link Node#usages()} just became empty
        ///
        public void usagesDroppedToZero(Node __node)
        {
        }

        ///
        // Notifies this listener of an added node.
        //
        // @param node a node that was just added to the graph
        ///
        public void nodeAdded(Node __node)
        {
        }

        ///
        // Notifies this listener of a removed node.
        ///
        public void nodeRemoved(Node __node)
        {
        }
    }

    // @class Graph.ChainedNodeEventListener
    private static final class ChainedNodeEventListener extends Graph.NodeEventListener
    {
        // @field
        Graph.NodeEventListener ___head;
        // @field
        Graph.NodeEventListener ___next;

        // @cons Graph.ChainedNodeEventListener
        ChainedNodeEventListener(Graph.NodeEventListener __head, Graph.NodeEventListener __next)
        {
            super();
            this.___head = __head;
            this.___next = __next;
        }

        @Override
        public void nodeAdded(Node __node)
        {
            this.___head.event(Graph.NodeEvent.NODE_ADDED, __node);
            this.___next.event(Graph.NodeEvent.NODE_ADDED, __node);
        }

        @Override
        public void inputChanged(Node __node)
        {
            this.___head.event(Graph.NodeEvent.INPUT_CHANGED, __node);
            this.___next.event(Graph.NodeEvent.INPUT_CHANGED, __node);
        }

        @Override
        public void usagesDroppedToZero(Node __node)
        {
            this.___head.event(Graph.NodeEvent.ZERO_USAGES, __node);
            this.___next.event(Graph.NodeEvent.ZERO_USAGES, __node);
        }

        @Override
        public void nodeRemoved(Node __node)
        {
            this.___head.event(Graph.NodeEvent.NODE_REMOVED, __node);
            this.___next.event(Graph.NodeEvent.NODE_REMOVED, __node);
        }

        @Override
        public void changed(Graph.NodeEvent __e, Node __node)
        {
            this.___head.event(__e, __node);
            this.___next.event(__e, __node);
        }
    }

    ///
    // Registers a given {@link Graph.NodeEventListener} with the enclosing graph until this object is {@linkplain #close() closed}.
    ///
    // @class Graph.NodeEventScope
    // @closure
    public final class NodeEventScope implements AutoCloseable
    {
        // @cons Graph.NodeEventScope
        NodeEventScope(Graph.NodeEventListener __listener)
        {
            super();
            if (Graph.this.___nodeEventListener == null)
            {
                Graph.this.___nodeEventListener = __listener;
            }
            else
            {
                Graph.this.___nodeEventListener = new Graph.ChainedNodeEventListener(__listener, Graph.this.___nodeEventListener);
            }
        }

        @Override
        public void close()
        {
            if (Graph.this.___nodeEventListener instanceof Graph.ChainedNodeEventListener)
            {
                Graph.this.___nodeEventListener = ((Graph.ChainedNodeEventListener) Graph.this.___nodeEventListener).___next;
            }
            else
            {
                Graph.this.___nodeEventListener = null;
            }
        }
    }

    ///
    // Registers a given {@link Graph.NodeEventListener} with this graph. This should be used in
    // conjunction with try-with-resources statement as follows:
    //
    // <pre>
    // try (Graph.NodeEventScope nes = graph.trackNodeEvents(listener)) {
    //     // make changes to the graph
    // }
    // </pre>
    ///
    public Graph.NodeEventScope trackNodeEvents(Graph.NodeEventListener __listener)
    {
        return new Graph.NodeEventScope(__listener);
    }

    ///
    // Looks for a node <i>similar</i> to {@code node} and returns it if found. Otherwise
    // {@code node} is added to this graph and returned.
    //
    // @return a node similar to {@code node} if one exists, otherwise {@code node}
    ///
    public <T extends Node & Node.ValueNumberable> T unique(T __node)
    {
        return uniqueHelper(__node);
    }

    <T extends Node> T uniqueHelper(T __node)
    {
        T __other = this.findDuplicate(__node);
        if (__other != null)
        {
            return __other;
        }
        else
        {
            T __result = addHelper(__node);
            if (__node.getNodeClass().isLeafNode())
            {
                putNodeIntoCache(__result);
            }
            return __result;
        }
    }

    void removeNodeFromCache(Node __node)
    {
        int __leafId = __node.getNodeClass().getLeafId();
        if (this.___cachedLeafNodes != null && this.___cachedLeafNodes.length > __leafId && this.___cachedLeafNodes[__leafId] != null)
        {
            this.___cachedLeafNodes[__leafId].removeKey(__node);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    void putNodeIntoCache(Node __node)
    {
        int __leafId = __node.getNodeClass().getLeafId();
        if (this.___cachedLeafNodes == null || this.___cachedLeafNodes.length <= __leafId)
        {
            EconomicMap[] __newLeafNodes = new EconomicMap[__leafId + 1];
            if (this.___cachedLeafNodes != null)
            {
                System.arraycopy(this.___cachedLeafNodes, 0, __newLeafNodes, 0, this.___cachedLeafNodes.length);
            }
            this.___cachedLeafNodes = __newLeafNodes;
        }

        if (this.___cachedLeafNodes[__leafId] == null)
        {
            this.___cachedLeafNodes[__leafId] = EconomicMap.create(NODE_VALUE_COMPARE);
        }

        this.___cachedLeafNodes[__leafId].put(__node, __node);
    }

    Node findNodeInCache(Node __node)
    {
        int __leafId = __node.getNodeClass().getLeafId();
        if (this.___cachedLeafNodes == null || this.___cachedLeafNodes.length <= __leafId || this.___cachedLeafNodes[__leafId] == null)
        {
            return null;
        }

        return this.___cachedLeafNodes[__leafId].get(__node);
    }

    ///
    // Returns a possible duplicate for the given node in the graph or {@code null} if no such
    // duplicate exists.
    ///
    @SuppressWarnings("unchecked")
    public <T extends Node> T findDuplicate(T __node)
    {
        NodeClass<?> __nodeClass = __node.getNodeClass();
        if (__nodeClass.isLeafNode())
        {
            // leaf node: look up in cache
            Node __cachedNode = findNodeInCache(__node);
            if (__cachedNode != null && __cachedNode != __node)
            {
                return (T) __cachedNode;
            }
            else
            {
                return null;
            }
        }
        else
        {
            // Non-leaf node: look for another usage of the node's inputs that has the same data,
            // inputs and successors as the node. To reduce the cost of this computation, only the
            // input with lowest usage count is considered. If this node is the only user of any
            // input then the search can terminate early. The usage count is only incremented once
            // the Node is in the Graph, so account for that in the test.
            final int __earlyExitUsageCount = __node.graph() != null ? 1 : 0;
            int __minCount = Integer.MAX_VALUE;
            Node __minCountNode = null;
            for (Node __input : __node.inputs())
            {
                int __usageCount = __input.getUsageCount();
                if (__usageCount == __earlyExitUsageCount)
                {
                    return null;
                }
                else if (__usageCount < __minCount)
                {
                    __minCount = __usageCount;
                    __minCountNode = __input;
                }
            }
            if (__minCountNode != null)
            {
                for (Node __usage : __minCountNode.usages())
                {
                    if (__usage != __node && __nodeClass == __usage.getNodeClass() && __node.valueEquals(__usage) && __nodeClass.equalInputs(__node, __usage) && __nodeClass.equalSuccessors(__node, __usage))
                    {
                        return (T) __usage;
                    }
                }
                return null;
            }
            return null;
        }
    }

    public boolean isNew(Graph.NodeMark __mark, Node __node)
    {
        return __node.___id >= __mark.getValue();
    }

    ///
    // A snapshot of the {@linkplain Graph#getNodeCount() live node count} in a graph.
    ///
    // @class Graph.NodeMark
    public static final class NodeMark extends NodeIdAccessor
    {
        // @field
        private final int ___value;

        // @cons Graph.NodeMark
        NodeMark(Graph __graph)
        {
            super(__graph);
            this.___value = __graph.nodeIdCount();
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj instanceof Graph.NodeMark)
            {
                Graph.NodeMark __other = (Graph.NodeMark) __obj;
                return __other.getValue() == getValue() && __other.getGraph() == getGraph();
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return this.___value ^ (this.___epoch + 11);
        }

        ///
        // Determines if this mark is positioned at the first live node in the graph.
        ///
        public boolean isStart()
        {
            return this.___value == 0;
        }

        ///
        // Gets the {@linkplain Graph#getNodeCount() live node count} of the associated graph when
        // this object was created.
        ///
        int getValue()
        {
            return this.___value;
        }

        ///
        // Determines if this mark still represents the {@linkplain Graph#getNodeCount() live node
        // count} of the graph.
        ///
        public boolean isCurrent()
        {
            return this.___value == this.___graph.nodeIdCount();
        }
    }

    ///
    // Gets a mark that can be used with {@link #getNewNodes}.
    ///
    public Graph.NodeMark getMark()
    {
        return new Graph.NodeMark(this);
    }

    ///
    // Returns an {@link Iterable} providing all nodes added since the last {@link Graph#getMark() mark}.
    ///
    public NodeIterable<Node> getNewNodes(Graph.NodeMark __mark)
    {
        final int __index = __mark == null ? 0 : __mark.getValue();
        // @closure
        return new NodeIterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new GraphNodeIterator(Graph.this, __index);
            }
        };
    }

    ///
    // Returns an {@link Iterable} providing all the live nodes.
    //
    // @return an {@link Iterable} providing all the live nodes.
    ///
    public NodeIterable<Node> getNodes()
    {
        // @closure
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

    // @class Graph.PlaceHolderNode
    static final class PlaceHolderNode extends Node
    {
        // @def
        public static final NodeClass<Graph.PlaceHolderNode> TYPE = NodeClass.create(Graph.PlaceHolderNode.class);

        // @cons Graph.PlaceHolderNode
        protected PlaceHolderNode()
        {
            super(TYPE);
        }
    }

    ///
    // If the {@linkplain GraalOptions#graphCompressionThreshold compression threshold} is met, the list
    // of nodes is compressed such that all non-null entries precede all null entries while
    // preserving the ordering between the nodes within the list.
    ///
    public boolean maybeCompress()
    {
        int __liveNodeCount = getNodeCount();
        int __liveNodePercent = __liveNodeCount * 100 / this.___nodesSize;
        int __compressionThreshold = GraalOptions.graphCompressionThreshold;
        if (__compressionThreshold == 0 || __liveNodePercent >= __compressionThreshold)
        {
            return false;
        }
        int __nextId = 0;
        for (int __i = 0; __nextId < __liveNodeCount; __i++)
        {
            Node __n = this.___nodes[__i];
            if (__n != null)
            {
                if (__i != __nextId)
                {
                    __n.___id = __nextId;
                    this.___nodes[__nextId] = __n;
                    this.___nodes[__i] = null;
                }
                __nextId++;
            }
        }
        this.___nodesSize = __nextId;
        this.___compressions++;
        this.___nodesDeletedBeforeLastCompression += this.___nodesDeletedSinceLastCompression;
        this.___nodesDeletedSinceLastCompression = 0;
        return true;
    }

    ///
    // Returns an {@link Iterable} providing all the live nodes whose type is compatible with
    // {@code type}.
    //
    // @param nodeClass the type of node to return
    // @return an {@link Iterable} providing all the matching nodes
    ///
    public <T extends Node & IterableNodeType> NodeIterable<T> getNodes(final NodeClass<T> __nodeClass)
    {
        // @closure
        return new NodeIterable<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                return new TypedGraphNodeIterator<>(__nodeClass, Graph.this);
            }
        };
    }

    ///
    // Returns whether the graph contains at least one node of the given type.
    //
    // @param type the type of node that is checked for occurrence
    // @return whether there is at least one such node
    ///
    public <T extends Node & IterableNodeType> boolean hasNode(final NodeClass<T> __type)
    {
        return getNodes(__type).iterator().hasNext();
    }

    ///
    // @return the first live Node with a matching iterableId
    ///
    Node getIterableNodeStart(int __iterableId)
    {
        if (this.___iterableNodesFirst.size() <= __iterableId)
        {
            return null;
        }
        Node __start = this.___iterableNodesFirst.get(__iterableId);
        if (__start == null || !__start.isDeleted())
        {
            return __start;
        }
        return findFirstLiveIterable(__iterableId, __start);
    }

    private Node findFirstLiveIterable(int __iterableId, Node __node)
    {
        Node __start = __node;
        while (__start != null && __start.isDeleted())
        {
            __start = __start.___typeCacheNext;
        }
        // Multiple threads iterating nodes can update this cache simultaneously. This is a benign
        // race, since all threads update it to the same value.
        this.___iterableNodesFirst.set(__iterableId, __start);
        if (__start == null)
        {
            this.___iterableNodesLast.set(__iterableId, __start);
        }
        return __start;
    }

    ///
    // @return return the first live Node with a matching iterableId starting from {@code node}
    ///
    Node getIterableNodeNext(Node __node)
    {
        if (__node == null)
        {
            return null;
        }
        Node __n = __node;
        if (__n == null || !__n.isDeleted())
        {
            return __n;
        }

        return findNextLiveiterable(__node);
    }

    private Node findNextLiveiterable(Node __start)
    {
        Node __n = __start;
        while (__n != null && __n.isDeleted())
        {
            __n = __n.___typeCacheNext;
        }
        if (__n == null)
        {
            // only dead nodes after this one
            __start.___typeCacheNext = null;
            int __nodeClassId = __start.getNodeClass().iterableId();
            this.___iterableNodesLast.set(__nodeClassId, __start);
        }
        else
        {
            // everything in between is dead
            __start.___typeCacheNext = __n;
        }
        return __n;
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
        return new SingletonNodeWorkList(this);
    }

    public NodeWorkList createIterativeNodeWorkList(boolean __fill, int __iterationLimitPerNode)
    {
        return new IterativeNodeWorkList(this, __fill, __iterationLimitPerNode);
    }

    void register(Node __node)
    {
        if (this.___nodes.length == this.___nodesSize)
        {
            grow();
        }
        int __id = this.___nodesSize++;
        this.___nodes[__id] = __node;
        __node.___id = __id;

        updateNodeCaches(__node);

        if (this.___nodeEventListener != null)
        {
            this.___nodeEventListener.event(Graph.NodeEvent.NODE_ADDED, __node);
        }
        afterRegister(__node);
    }

    private void grow()
    {
        Node[] __newNodes = new Node[(this.___nodesSize * 2) + 1];
        System.arraycopy(this.___nodes, 0, __newNodes, 0, this.___nodesSize);
        this.___nodes = __newNodes;
    }

    @SuppressWarnings("unused")
    protected void afterRegister(Node __node)
    {
    }

    @SuppressWarnings("unused")
    private void postDeserialization()
    {
        recomputeIterableNodeLists();
    }

    ///
    // Rebuilds the lists used to support {@link #getNodes(NodeClass)}. This is useful for serialization
    // where the underlying {@linkplain NodeClass#iterableId() iterable ids} may have changed.
    ///
    private void recomputeIterableNodeLists()
    {
        this.___iterableNodesFirst.clear();
        this.___iterableNodesLast.clear();
        for (Node __node : this.___nodes)
        {
            if (__node != null && __node.isAlive())
            {
                updateNodeCaches(__node);
            }
        }
    }

    private void updateNodeCaches(Node __node)
    {
        int __nodeClassId = __node.getNodeClass().iterableId();
        if (__nodeClassId != Node.NOT_ITERABLE)
        {
            while (this.___iterableNodesFirst.size() <= __nodeClassId)
            {
                this.___iterableNodesFirst.add(null);
                this.___iterableNodesLast.add(null);
            }
            Node __prev = this.___iterableNodesLast.get(__nodeClassId);
            if (__prev != null)
            {
                __prev.___typeCacheNext = __node;
            }
            else
            {
                this.___iterableNodesFirst.set(__nodeClassId, __node);
            }
            this.___iterableNodesLast.set(__nodeClassId, __node);
        }
    }

    void unregister(Node __node)
    {
        if (__node.getNodeClass().isLeafNode() && __node.getNodeClass().valueNumberable())
        {
            removeNodeFromCache(__node);
        }
        this.___nodes[__node.___id] = null;
        this.___nodesDeletedSinceLastCompression++;

        if (this.___nodeEventListener != null)
        {
            this.___nodeEventListener.event(Graph.NodeEvent.NODE_ADDED, __node);
        }

        // nodes aren't removed from the type cache here - they will be removed during iteration
    }

    public Node getNode(int __id)
    {
        return this.___nodes[__id];
    }

    ///
    // Returns the number of node ids generated so far.
    //
    // @return the number of node ids generated so far
    ///
    int nodeIdCount()
    {
        return this.___nodesSize;
    }

    ///
    // Adds duplicates of the nodes in {@code newNodes} to this graph. This will recreate any edges
    // between the duplicate nodes. The {@code replacement} map can be used to replace a node from
    // the source graph by a given node (which must already be in this graph). Edges between
    // duplicate and replacement nodes will also be recreated so care should be taken regarding the
    // matching of node types in the replacement map.
    //
    // @param newNodes the nodes to be duplicated
    // @param replacementsMap the replacement map (can be null if no replacement is to be performed)
    // @return a map which associates the original nodes from {@code nodes} to their duplicates
    ///
    public UnmodifiableEconomicMap<Node, Node> addDuplicates(Iterable<? extends Node> __newNodes, final Graph __oldGraph, int __estimatedNodeCount, EconomicMap<Node, Node> __replacementsMap)
    {
        Graph.DuplicationReplacement __replacements;
        if (__replacementsMap == null)
        {
            __replacements = null;
        }
        else
        {
            __replacements = new Graph.MapReplacement(__replacementsMap);
        }
        return addDuplicates(__newNodes, __oldGraph, __estimatedNodeCount, __replacements);
    }

    // @iface Graph.DuplicationReplacement
    public interface DuplicationReplacement
    {
        Node replacement(Node __original);
    }

    // @class Graph.MapReplacement
    private static final class MapReplacement implements Graph.DuplicationReplacement
    {
        // @field
        private final EconomicMap<Node, Node> ___map;

        // @cons Graph.MapReplacement
        MapReplacement(EconomicMap<Node, Node> __map)
        {
            super();
            this.___map = __map;
        }

        @Override
        public Node replacement(Node __original)
        {
            Node __replacement = this.___map.get(__original);
            return __replacement != null ? __replacement : __original;
        }
    }

    public EconomicMap<Node, Node> addDuplicates(Iterable<? extends Node> __newNodes, final Graph __oldGraph, int __estimatedNodeCount, Graph.DuplicationReplacement __replacements)
    {
        return NodeClass.addGraphDuplicate(this, __oldGraph, __estimatedNodeCount, __newNodes, __replacements);
    }

    public boolean isFrozen()
    {
        return this.___freezeState != Graph.FreezeState.Unfrozen;
    }

    public void freeze()
    {
        this.___freezeState = Graph.FreezeState.DeepFreeze;
    }
}
