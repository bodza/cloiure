package giraaff.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Predicate;

import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Edges.Type;
import giraaff.graph.Graph.NodeEventListener;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.iterators.NodePredicate;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodeinfo.Verbosity;
import giraaff.options.OptionValues;
import giraaff.util.UnsafeAccess;

/**
 * This class is the base class for all nodes. It represents a node that can be inserted in a
 * {@link Graph}.
 *
 * Once a node has been added to a graph, it has a graph-unique {@link #id()}. Edges in the
 * subclasses are represented with annotated fields. There are two kind of edges : {@link Input} and
 * {@link Successor}. If a field, of a type compatible with {@link Node}, annotated with either
 * {@link Input} and {@link Successor} is not null, then there is an edge from this node to the node
 * this field points to.
 *
 * Nodes which are be value numberable should implement the {@link ValueNumberable} interface.
 */
// @class Node
public abstract class Node implements Cloneable, NodeInterface
{
    public static final NodeClass<?> TYPE = null;

    static final int DELETED_ID_START = -1000000000;
    static final int INITIAL_ID = -1;
    static final int ALIVE_ID_START = 0;

    /**
     * Denotes a non-optional (non-null) node input. This should be applied to exactly the fields of
     * a node that are of type {@link Node} or {@link NodeInputList}. Nodes that update fields of
     * type {@link Node} outside of their constructor should call
     * {@link Node#updateUsages(Node, Node)} just prior to doing the update of the input.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Input
    {
        InputType value() default InputType.Value;
    }

    /**
     * Denotes an optional (nullable) node input. This should be applied to exactly the fields of a
     * node that are of type {@link Node} or {@link NodeInputList}. Nodes that update fields of type
     * {@link Node} outside of their constructor should call {@link Node#updateUsages(Node, Node)}
     * just prior to doing the update of the input.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface OptionalInput
    {
        InputType value() default InputType.Value;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Successor
    {
    }

    /**
     * Denotes that a parameter of an {@linkplain NodeIntrinsic intrinsic} method must be a compile
     * time constant at all call sites to the intrinsic method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public static @interface ConstantNodeParameter
    {
    }

    /**
     * Denotes an injected parameter in a {@linkplain NodeIntrinsic node intrinsic} constructor. If
     * the constructor is called as part of node intrinsification, the node intrinsifier will inject
     * an argument for the annotated parameter. Injected parameters must precede all non-injected
     * parameters in a constructor. If the type of the annotated parameter is {@link Stamp}, the
     * {@linkplain Stamp#javaType type} of the injected stamp is the return type of the annotated
     * method (which cannot be {@code void}).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public static @interface InjectedNodeParameter
    {
    }

    /**
     * Annotates a method that can be replaced by a compiler intrinsic. A (resolved) call to the
     * annotated method will be processed by a generated {@code InvocationPlugin} that calls either
     * a factory method or a constructor corresponding with the annotated method.
     *
     * A factory method corresponding to an annotated method is a static method named
     * {@code intrinsify} defined in the class denoted by {@link #value()}. In order, its signature
     * is as follows:
     *
     * <li>A {@code GraphBuilderContext} parameter.</li>
     * <li>A {@code ResolvedJavaMethod} parameter.</li>
     * <li>A sequence of zero or more {@linkplain InjectedNodeParameter injected} parameters.</li>
     * <li>Remaining parameters that match the declared parameters of the annotated method.</li>
     * </ol>
     * A constructor corresponding to an annotated method is defined in the class denoted by
     * {@link #value()}. In order, its signature is as follows:
     * <ol>
     * <li>A sequence of zero or more {@linkplain InjectedNodeParameter injected} parameters.</li>
     * <li>Remaining parameters that match the declared parameters of the annotated method.</li>
     *
     * There must be exactly one such factory method or constructor corresponding to a
     * {@link NodeIntrinsic} annotated method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface NodeIntrinsic
    {
        /**
         * The class declaring the factory method or {@link Node} subclass declaring the constructor
         * used to intrinsify a call to the annotated method. The default value is the class in
         * which the annotated method is declared.
         */
        Class<?> value() default NodeIntrinsic.class;

        /**
         * If {@code true}, the factory method or constructor selected by the annotation must have
         * an {@linkplain InjectedNodeParameter injected} {@link Stamp} parameter. Calling
         * {@link AbstractPointerStamp#nonNull()} on the injected stamp is guaranteed to return {@code true}.
         */
        boolean injectedStampIsNonNull() default false;
    }

    /**
     * Marker for a node that can be replaced by another node via global value numbering. A
     * {@linkplain NodeClass#isLeafNode() leaf} node can be replaced by another node of the same
     * type that has exactly the same {@linkplain NodeClass#getData() data} values. A non-leaf node
     * can be replaced by another node of the same type that has exactly the same data values as
     * well as the same {@linkplain Node#inputs() inputs} and {@linkplain Node#successors() successors}.
     */
    // @iface Node.ValueNumberable
    public interface ValueNumberable
    {
    }

    /**
     * Marker interface for nodes that contains other nodes. When the inputs to this node changes,
     * users of this node should also be placed on the work list for canonicalization.
     */
    // @iface Node.IndirectCanonicalization
    public interface IndirectCanonicalization
    {
    }

    private Graph graph;
    int id;

    // this next pointer is used in Graph to implement fast iteration over NodeClass types, it
    // therefore points to the next Node of the same type.
    Node typeCacheNext;

    static final int INLINE_USAGE_COUNT = 2;
    private static final Node[] NO_NODES = {};

    /**
     * Head of usage list. The elements of the usage list in order are {@link #usage0},
     * {@link #usage1} and {@link #extraUsages}. The first null entry terminates the list.
     */
    Node usage0;
    Node usage1;
    Node[] extraUsages;
    int extraUsagesCount;

    private Node predecessor;
    private NodeClass<? extends Node> nodeClass;

    public static final int NODE_LIST = -2;
    public static final int NOT_ITERABLE = -1;

    // @cons
    public Node(NodeClass<? extends Node> c)
    {
        super();
        init(c);
    }

    final void init(NodeClass<? extends Node> c)
    {
        this.nodeClass = c;
        id = INITIAL_ID;
        extraUsages = NO_NODES;
    }

    final int id()
    {
        return id;
    }

    @Override
    public Node asNode()
    {
        return this;
    }

    /**
     * Gets the graph context of this node.
     */
    public Graph graph()
    {
        return graph;
    }

    /**
     * Gets the option values associated with this node's graph.
     */
    public final OptionValues getOptions()
    {
        return graph == null ? null : graph.getOptions();
    }

    /**
     * Returns an {@link NodeIterable iterable} which can be used to traverse all non-null input
     * edges of this node.
     *
     * @return an {@link NodeIterable iterable} for all non-null input edges.
     */
    public NodeIterable<Node> inputs()
    {
        return nodeClass.getInputIterable(this);
    }

    /**
     * Returns an {@link Iterable iterable} which can be used to traverse all non-null input edges
     * of this node.
     *
     * @return an {@link Iterable iterable} for all non-null input edges.
     */
    public Iterable<Position> inputPositions()
    {
        return nodeClass.getInputEdges().getPositionsIterable(this);
    }

    // @class Node.EdgeVisitor
    public abstract static class EdgeVisitor
    {
        public abstract Node apply(Node source, Node target);
    }

    /**
     * Applies the given visitor to all inputs of this node.
     *
     * @param visitor the visitor to be applied to the inputs
     */
    public void applyInputs(EdgeVisitor visitor)
    {
        nodeClass.applyInputs(this, visitor);
    }

    /**
     * Applies the given visitor to all successors of this node.
     *
     * @param visitor the visitor to be applied to the successors
     */
    public void applySuccessors(EdgeVisitor visitor)
    {
        nodeClass.applySuccessors(this, visitor);
    }

    /**
     * Returns an {@link NodeIterable iterable} which can be used to traverse all non-null successor
     * edges of this node.
     *
     * @return an {@link NodeIterable iterable} for all non-null successor edges.
     */
    public NodeIterable<Node> successors()
    {
        return nodeClass.getSuccessorIterable(this);
    }

    /**
     * Returns an {@link Iterable iterable} which can be used to traverse all successor edge
     * positions of this node.
     *
     * @return an {@link Iterable iterable} for all successor edge positoins.
     */
    public Iterable<Position> successorPositions()
    {
        return nodeClass.getSuccessorEdges().getPositionsIterable(this);
    }

    /**
     * Gets the maximum number of usages this node has had at any point in time.
     */
    public int getUsageCount()
    {
        if (usage0 == null)
        {
            return 0;
        }
        if (usage1 == null)
        {
            return 1;
        }
        return INLINE_USAGE_COUNT + extraUsagesCount;
    }

    /**
     * Gets the list of nodes that use this node (i.e., as an input).
     */
    public final NodeIterable<Node> usages()
    {
        return new NodeUsageIterable(this);
    }

    /**
     * Checks whether this node has no usages.
     */
    public final boolean hasNoUsages()
    {
        return this.usage0 == null;
    }

    /**
     * Checks whether this node has usages.
     */
    public final boolean hasUsages()
    {
        return this.usage0 != null;
    }

    /**
     * Checks whether this node has more than one usages.
     */
    public final boolean hasMoreThanOneUsage()
    {
        return this.usage1 != null;
    }

    /**
     * Checks whether this node has exactly one usgae.
     */
    public final boolean hasExactlyOneUsage()
    {
        return hasUsages() && !hasMoreThanOneUsage();
    }

    /**
     * Adds a given node to this node's {@linkplain #usages() usages}.
     *
     * @param node the node to add
     */
    void addUsage(Node node)
    {
        if (usage0 == null)
        {
            usage0 = node;
        }
        else if (usage1 == null)
        {
            usage1 = node;
        }
        else
        {
            int length = extraUsages.length;
            if (length == 0)
            {
                extraUsages = new Node[4];
            }
            else if (extraUsagesCount == length)
            {
                Node[] newExtraUsages = new Node[length * 2 + 1];
                System.arraycopy(extraUsages, 0, newExtraUsages, 0, length);
                extraUsages = newExtraUsages;
            }
            extraUsages[extraUsagesCount++] = node;
        }
    }

    private void movUsageFromEndTo(int destIndex)
    {
        if (destIndex >= INLINE_USAGE_COUNT)
        {
            movUsageFromEndToExtraUsages(destIndex - INLINE_USAGE_COUNT);
        }
        else if (destIndex == 1)
        {
            movUsageFromEndToIndexOne();
        }
        else
        {
            movUsageFromEndToIndexZero();
        }
    }

    private void movUsageFromEndToExtraUsages(int destExtraIndex)
    {
        this.extraUsagesCount--;
        Node n = extraUsages[extraUsagesCount];
        extraUsages[destExtraIndex] = n;
        extraUsages[extraUsagesCount] = null;
    }

    private void movUsageFromEndToIndexZero()
    {
        if (extraUsagesCount > 0)
        {
            this.extraUsagesCount--;
            usage0 = extraUsages[extraUsagesCount];
            extraUsages[extraUsagesCount] = null;
        }
        else if (usage1 != null)
        {
            usage0 = usage1;
            usage1 = null;
        }
        else
        {
            usage0 = null;
        }
    }

    private void movUsageFromEndToIndexOne()
    {
        if (extraUsagesCount > 0)
        {
            this.extraUsagesCount--;
            usage1 = extraUsages[extraUsagesCount];
            extraUsages[extraUsagesCount] = null;
        }
        else
        {
            usage1 = null;
        }
    }

    /**
     * Removes a given node from this node's {@linkplain #usages() usages}.
     *
     * @param node the node to remove
     * @return whether or not {@code usage} was in the usage list
     */
    public boolean removeUsage(Node node)
    {
        // For large graphs, usage removal is critical for performance.
        // Furthermore, it is critical that this method maintains the invariant,
        // that the usage list has no null element preceding a non-null element.
        if (usage0 == node)
        {
            movUsageFromEndToIndexZero();
            return true;
        }
        if (usage1 == node)
        {
            movUsageFromEndToIndexOne();
            return true;
        }
        for (int i = this.extraUsagesCount - 1; i >= 0; i--)
        {
            if (extraUsages[i] == node)
            {
                movUsageFromEndToExtraUsages(i);
                return true;
            }
        }
        return false;
    }

    public final Node predecessor()
    {
        return predecessor;
    }

    public final int modCount()
    {
        return 0;
    }

    final void incModCount()
    {
    }

    final int usageModCount()
    {
        return 0;
    }

    public final boolean isDeleted()
    {
        return id <= DELETED_ID_START;
    }

    public final boolean isAlive()
    {
        return id >= ALIVE_ID_START;
    }

    public final boolean isUnregistered()
    {
        return id == INITIAL_ID;
    }

    /**
     * Updates the usages sets of the given nodes after an input slot is changed from
     * {@code oldInput} to {@code newInput} by removing this node from {@code oldInput}'s usages and
     * adds this node to {@code newInput}'s usages.
     */
    protected void updateUsages(Node oldInput, Node newInput)
    {
        if (oldInput != newInput)
        {
            if (oldInput != null)
            {
                boolean result = removeThisFromUsages(oldInput);
            }
            maybeNotifyInputChanged(this);
            if (newInput != null)
            {
                newInput.addUsage(this);
            }
            if (oldInput != null && oldInput.hasNoUsages())
            {
                maybeNotifyZeroUsages(oldInput);
            }
        }
    }

    protected void updateUsagesInterface(NodeInterface oldInput, NodeInterface newInput)
    {
        updateUsages(oldInput == null ? null : oldInput.asNode(), newInput == null ? null : newInput.asNode());
    }

    /**
     * Updates the predecessor of the given nodes after a successor slot is changed from
     * oldSuccessor to newSuccessor: removes this node from oldSuccessor's predecessors and adds
     * this node to newSuccessor's predecessors.
     */
    protected void updatePredecessor(Node oldSuccessor, Node newSuccessor)
    {
        if (oldSuccessor != newSuccessor)
        {
            if (oldSuccessor != null)
            {
                oldSuccessor.predecessor = null;
            }
            if (newSuccessor != null)
            {
                newSuccessor.predecessor = this;
            }
        }
    }

    void initialize(Graph newGraph)
    {
        this.graph = newGraph;
        newGraph.register(this);
        NodeClass<? extends Node> nc = nodeClass;
        nc.registerAtInputsAsUsage(this);
        nc.registerAtSuccessorsAsPredecessor(this);
    }

    /**
     * Information associated with this node. A single value is stored directly in the field.
     * Multiple values are stored by creating an Object[].
     */
    private Object annotation;

    private <T> T getNodeInfo(Class<T> clazz)
    {
        if (annotation == null)
        {
            return null;
        }
        if (clazz.isInstance(annotation))
        {
            return clazz.cast(annotation);
        }
        if (annotation.getClass() == Object[].class)
        {
            Object[] annotations = (Object[]) annotation;
            for (Object ann : annotations)
            {
                if (clazz.isInstance(ann))
                {
                    return clazz.cast(ann);
                }
            }
        }
        return null;
    }

    private <T> void setNodeInfo(Class<T> clazz, T value)
    {
        if (annotation == null || clazz.isInstance(annotation))
        {
            // replace the current value
            this.annotation = value;
        }
        else if (annotation.getClass() == Object[].class)
        {
            Object[] annotations = (Object[]) annotation;
            for (int i = 0; i < annotations.length; i++)
            {
                if (clazz.isInstance(annotations[i]))
                {
                    annotations[i] = value;
                    return;
                }
            }
            Object[] newAnnotations = Arrays.copyOf(annotations, annotations.length + 1);
            newAnnotations[annotations.length] = value;
            this.annotation = newAnnotations;
        }
        else
        {
            this.annotation = new Object[] { this.annotation, value };
        }
    }

    public final NodeClass<? extends Node> getNodeClass()
    {
        return nodeClass;
    }

    public boolean isAllowedUsageType(InputType type)
    {
        if (type == InputType.Value)
        {
            return false;
        }
        return getNodeClass().getAllowedUsageTypes().contains(type);
    }

    private boolean checkReplaceWith(Node other)
    {
        if (graph != null && graph.isFrozen())
        {
            fail("cannot modify frozen graph");
        }
        if (other == this)
        {
            fail("cannot replace a node with itself");
        }
        if (isDeleted())
        {
            fail("cannot replace deleted node");
        }
        if (other != null && other.isDeleted())
        {
            fail("cannot replace with deleted node %s", other);
        }
        return true;
    }

    public final void replaceAtUsages(Node other)
    {
        replaceAtAllUsages(other, (Node) null);
    }

    public final void replaceAtUsages(Node other, Predicate<Node> filter)
    {
        replaceAtUsages(other, filter, null);
    }

    public final void replaceAtUsagesAndDelete(Node other)
    {
        replaceAtUsages(other, null, this);
        safeDelete();
    }

    public final void replaceAtUsagesAndDelete(Node other, Predicate<Node> filter)
    {
        replaceAtUsages(other, filter, this);
        safeDelete();
    }

    protected void replaceAtUsages(Node other, Predicate<Node> filter, Node toBeDeleted)
    {
        if (filter == null)
        {
            replaceAtAllUsages(other, toBeDeleted);
        }
        else
        {
            replaceAtMatchingUsages(other, filter, toBeDeleted);
        }
    }

    protected void replaceAtAllUsages(Node other, Node toBeDeleted)
    {
        checkReplaceWith(other);
        if (usage0 == null)
        {
            return;
        }
        replaceAtUsage(other, toBeDeleted, usage0);
        usage0 = null;

        if (usage1 == null)
        {
            return;
        }
        replaceAtUsage(other, toBeDeleted, usage1);
        usage1 = null;

        if (extraUsagesCount <= 0)
        {
            return;
        }
        for (int i = 0; i < extraUsagesCount; i++)
        {
            Node usage = extraUsages[i];
            replaceAtUsage(other, toBeDeleted, usage);
        }
        this.extraUsages = NO_NODES;
        this.extraUsagesCount = 0;
    }

    private void replaceAtUsage(Node other, Node toBeDeleted, Node usage)
    {
        boolean result = usage.getNodeClass().replaceFirstInput(usage, this, other);
        // Don't notify for nodes which are about to be deleted.
        if (toBeDeleted == null || usage != toBeDeleted)
        {
            maybeNotifyInputChanged(usage);
        }
        if (other != null)
        {
            other.addUsage(usage);
        }
    }

    private void replaceAtMatchingUsages(Node other, Predicate<Node> filter, Node toBeDeleted)
    {
        if (filter == null)
        {
            fail("filter cannot be null");
        }
        checkReplaceWith(other);
        int i = 0;
        while (i < this.getUsageCount())
        {
            Node usage = this.getUsageAt(i);
            if (filter.test(usage))
            {
                replaceAtUsage(other, toBeDeleted, usage);
                this.movUsageFromEndTo(i);
            }
            else
            {
                ++i;
            }
        }
    }

    public Node getUsageAt(int index)
    {
        if (index == 0)
        {
            return this.usage0;
        }
        else if (index == 1)
        {
            return this.usage1;
        }
        else
        {
            return this.extraUsages[index - INLINE_USAGE_COUNT];
        }
    }

    public void replaceAtMatchingUsages(Node other, NodePredicate usagePredicate)
    {
        checkReplaceWith(other);
        replaceAtMatchingUsages(other, usagePredicate, null);
    }

    public void replaceAtUsages(InputType type, Node other)
    {
        checkReplaceWith(other);
        for (Node usage : usages().snapshot())
        {
            for (Position pos : usage.inputPositions())
            {
                if (pos.getInputType() == type && pos.get(usage) == this)
                {
                    pos.set(usage, other);
                }
            }
        }
    }

    private void maybeNotifyInputChanged(Node node)
    {
        if (graph != null)
        {
            NodeEventListener listener = graph.nodeEventListener;
            if (listener != null)
            {
                listener.event(Graph.NodeEvent.INPUT_CHANGED, node);
            }
        }
    }

    public void maybeNotifyZeroUsages(Node node)
    {
        if (graph != null)
        {
            NodeEventListener listener = graph.nodeEventListener;
            if (listener != null && node.isAlive())
            {
                listener.event(Graph.NodeEvent.ZERO_USAGES, node);
            }
        }
    }

    public void replaceAtPredecessor(Node other)
    {
        checkReplaceWith(other);
        if (predecessor != null)
        {
            if (!predecessor.getNodeClass().replaceFirstSuccessor(predecessor, this, other))
            {
                fail("not found in successors, predecessor: %s", predecessor);
            }
            predecessor.updatePredecessor(this, other);
        }
    }

    public void replaceAndDelete(Node other)
    {
        checkReplaceWith(other);
        if (other == null)
        {
            fail("cannot replace with null");
        }
        if (this.hasUsages())
        {
            replaceAtUsages(other);
        }
        replaceAtPredecessor(other);
        this.safeDelete();
    }

    public void replaceFirstSuccessor(Node oldSuccessor, Node newSuccessor)
    {
        if (nodeClass.replaceFirstSuccessor(this, oldSuccessor, newSuccessor))
        {
            updatePredecessor(oldSuccessor, newSuccessor);
        }
    }

    public void replaceFirstInput(Node oldInput, Node newInput)
    {
        if (nodeClass.replaceFirstInput(this, oldInput, newInput))
        {
            updateUsages(oldInput, newInput);
        }
    }

    public void clearInputs()
    {
        getNodeClass().unregisterAtInputsAsUsage(this);
    }

    boolean removeThisFromUsages(Node n)
    {
        return n.removeUsage(this);
    }

    public void clearSuccessors()
    {
        getNodeClass().unregisterAtSuccessorsAsPredecessor(this);
    }

    /**
     * Removes this node from its graph. This node must have no {@linkplain Node#usages() usages}
     * and no {@linkplain #predecessor() predecessor}.
     */
    public void safeDelete()
    {
        this.clearInputs();
        this.clearSuccessors();
        markDeleted();
    }

    public void markDeleted()
    {
        graph.unregister(this);
        id = DELETED_ID_START - id;
    }

    public final Node copyWithInputs()
    {
        return copyWithInputs(true);
    }

    public final Node copyWithInputs(boolean insertIntoGraph)
    {
        Node newNode = clone(insertIntoGraph ? graph : null, WithOnlyInputEdges);
        if (insertIntoGraph)
        {
            for (Node input : inputs())
            {
                input.addUsage(newNode);
            }
        }
        return newNode;
    }

    /**
     * Must be overridden by subclasses that implement {@link Simplifiable}. The implementation in
     * {@link Node} exists to obviate the need to cast a node before invoking
     * {@link Simplifiable#simplify(SimplifierTool)}.
     */
    public void simplify(SimplifierTool tool)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @param newNode the result of cloning this node or {@link Unsafe#allocateInstance(Class) raw
     *            allocating} a copy of this node
     * @param type the type of edges to process
     * @param edgesToCopy if {@code type} is in this set, the edges are copied otherwise they are cleared
     */
    private void copyOrClearEdgesForClone(Node newNode, Edges.Type type, EnumSet<Edges.Type> edgesToCopy)
    {
        if (edgesToCopy.contains(type))
        {
            getNodeClass().getEdges(type).copy(this, newNode);
        }
        else
        {
            // the direct edges are already null
            getNodeClass().getEdges(type).initializeLists(newNode, this);
        }
    }

    public static final EnumSet<Edges.Type> WithNoEdges = EnumSet.noneOf(Edges.Type.class);
    public static final EnumSet<Edges.Type> WithAllEdges = EnumSet.allOf(Edges.Type.class);
    public static final EnumSet<Edges.Type> WithOnlyInputEdges = EnumSet.of(Type.Inputs);
    public static final EnumSet<Edges.Type> WithOnlySucessorEdges = EnumSet.of(Type.Successors);

    /**
     * Makes a copy of this node in(to) a given graph.
     *
     * @param into the graph in which the copy will be registered (which may be this node's graph)
     *            or null if the copy should not be registered in a graph
     * @param edgesToCopy specifies the edges to be copied. The edges not specified in this set are
     *            initialized to their default value (i.e., {@code null} for a direct edge, an empty
     *            list for an edge list)
     * @return the copy of this node
     */
    final Node clone(Graph into, EnumSet<Edges.Type> edgesToCopy)
    {
        final NodeClass<? extends Node> nodeClassTmp = getNodeClass();
        boolean useIntoLeafNodeCache = false;
        if (into != null)
        {
            if (nodeClassTmp.valueNumberable() && nodeClassTmp.isLeafNode())
            {
                useIntoLeafNodeCache = true;
                Node otherNode = into.findNodeInCache(this);
                if (otherNode != null)
                {
                    return otherNode;
                }
            }
        }

        Node newNode = null;
        try
        {
            newNode = (Node) UnsafeAccess.UNSAFE.allocateInstance(getClass());
            newNode.nodeClass = nodeClassTmp;
            nodeClassTmp.getData().copy(this, newNode);
            copyOrClearEdgesForClone(newNode, Type.Inputs, edgesToCopy);
            copyOrClearEdgesForClone(newNode, Type.Successors, edgesToCopy);
        }
        catch (Exception e)
        {
            throw new GraalGraphError(e).addContext(this);
        }
        newNode.graph = into;
        newNode.id = INITIAL_ID;
        if (into != null)
        {
            into.register(newNode);
        }
        newNode.extraUsages = NO_NODES;

        if (into != null && useIntoLeafNodeCache)
        {
            into.putNodeIntoCache(newNode);
        }
        newNode.afterClone(this);
        return newNode;
    }

    protected void afterClone(@SuppressWarnings("unused") Node other)
    {
    }

    protected VerificationError fail(String message, Object... args) throws GraalGraphError
    {
        throw new VerificationError(message, args).addContext(this);
    }

    public Iterable<? extends Node> cfgPredecessors()
    {
        if (predecessor == null)
        {
            return Collections.emptySet();
        }
        else
        {
            return Collections.singleton(predecessor);
        }
    }

    /**
     * Returns an iterator that will provide all control-flow successors of this node. Normally this
     * will be the contents of all fields annotated with {@link Successor}, but some node classes
     * (like EndNode) may return different nodes.
     */
    public Iterable<? extends Node> cfgSuccessors()
    {
        return successors();
    }

    /**
     * Nodes using their {@link #id} as the hash code. This works very well when nodes of the same
     * graph are stored in sets. It can give bad behavior when storing nodes of different graphs in
     * the same set.
     */
    @Override
    public final int hashCode()
    {
        if (this.isDeleted())
        {
            return -id + DELETED_ID_START;
        }
        return id;
    }

    /**
     * Do not overwrite the equality test of a node in subclasses. Equality tests must rely solely
     * on identity.
     */

    /**
     * This method is a shortcut for {@link #toString(Verbosity)} with {@link Verbosity#Short}.
     */
    @Override
    public final String toString()
    {
        return toString(Verbosity.Short);
    }

    /**
     * Creates a String representation for this node with a given {@link Verbosity}.
     */
    public String toString(Verbosity verbosity)
    {
        switch (verbosity)
        {
            case Id:
                return Integer.toString(id);
            case Name:
                return getNodeClass().shortName();
            case Short:
                return toString(Verbosity.Id) + "|" + toString(Verbosity.Name);
            case Long:
            case Debugger:
            case All:
                return toString(Verbosity.Short);
            default:
                throw new RuntimeException("unknown verbosity: " + verbosity);
        }
    }

    @Deprecated
    public int getId()
    {
        return id;
    }

    /**
     * Determines if this node's {@link NodeClass#getData() data} fields are equal to the data
     * fields of another node of the same type. Primitive fields are compared by value and
     * non-primitive fields are compared by {@link Objects#equals(Object, Object)}.
     *
     * The result of this method undefined if {@code other.getClass() != this.getClass()}.
     *
     * @param other a node of exactly the same type as this node
     * @return true if the data fields of this object and {@code other} are equal
     */
    public boolean valueEquals(Node other)
    {
        return getNodeClass().dataEquals(this, other);
    }

    /**
     * Determines if this node is equal to the other node while ignoring differences in
     * {@linkplain Successor control-flow} edges.
     */
    public boolean dataFlowEquals(Node other)
    {
        return this == other || nodeClass == other.getNodeClass() && this.valueEquals(other) && nodeClass.equalInputs(this, other);
    }

    public final void pushInputs(NodeStack stack)
    {
        getNodeClass().pushInputs(this, stack);
    }
}
