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
import giraaff.graph.Edges;
import giraaff.graph.Graph;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.iterators.NodePredicate;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;

///
// This class is the base class for all nodes. It represents a node that can be inserted in a {@link Graph}.
//
// Once a node has been added to a graph, it has a graph-unique {@link #id()}. Edges in the subclasses are
// represented with annotated fields. There are two kind of edges : {@link Node.Input} and {@link Node.Successor}.
// If a field, of a type compatible with {@link Node}, annotated with either {@link Node.Input} and
// {@link Node.Successor} is not null, then there is an edge from this node to the node this field points to.
//
// Nodes which are be value numberable should implement the {@link Node.ValueNumberable} interface.
///
// @class Node
public abstract class Node implements Cloneable, NodeInterface
{
    // @def
    public static final NodeClass<?> TYPE = null;

    // @def
    static final int DELETED_ID_START = -1000000000;
    // @def
    static final int INITIAL_ID = -1;
    // @def
    static final int ALIVE_ID_START = 0;

    ///
    // Denotes a non-optional (non-null) node input. This should be applied to exactly the fields of
    // a node that are of type {@link Node} or {@link NodeInputList}. Nodes that update fields of
    // type {@link Node} outside of their constructor should call
    // {@link Node#updateUsages(Node, Node)} just prior to doing the update of the input.
    ///
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    // @iface Node.Input
    public static @interface Input
    {
        InputType value() default InputType.Value;
    }

    ///
    // Denotes an optional (nullable) node input. This should be applied to exactly the fields of a
    // node that are of type {@link Node} or {@link NodeInputList}. Nodes that update fields of type
    // {@link Node} outside of their constructor should call {@link Node#updateUsages(Node, Node)}
    // just prior to doing the update of the input.
    ///
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    // @iface Node.OptionalInput
    public static @interface OptionalInput
    {
        InputType value() default InputType.Value;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    // @iface Node.Successor
    public static @interface Successor
    {
    }

    ///
    // Denotes that a parameter of an {@linkplain Node.NodeIntrinsic intrinsic} method must be a compile
    // time constant at all call sites to the intrinsic method.
    ///
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    // @iface Node.ConstantNodeParameter
    public static @interface ConstantNodeParameter
    {
    }

    ///
    // Denotes an injected parameter in a {@linkplain Node.NodeIntrinsic node intrinsic} constructor.
    // If the constructor is called as part of node intrinsification, the node intrinsifier will inject
    // an argument for the annotated parameter. Injected parameters must precede all non-injected
    // parameters in a constructor. If the type of the annotated parameter is {@link Stamp}, the
    // {@linkplain Stamp#javaType type} of the injected stamp is the return type of the annotated
    // method (which cannot be {@code void}).
    ///
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    // @iface Node.InjectedNodeParameter
    public static @interface InjectedNodeParameter
    {
    }

    ///
    // Annotates a method that can be replaced by a compiler intrinsic. A (resolved) call to the
    // annotated method will be processed by a generated {@code InvocationPlugin} that calls either
    // a factory method or a constructor corresponding with the annotated method.
    //
    // A factory method corresponding to an annotated method is a static method named
    // {@code intrinsify} defined in the class denoted by {@link #value()}. In order, its signature
    // is as follows:
    //
    // <li>A {@code GraphBuilderContext} parameter.</li>
    // <li>A {@code ResolvedJavaMethod} parameter.</li>
    // <li>A sequence of zero or more {@linkplain Node.InjectedNodeParameter injected} parameters.</li>
    // <li>Remaining parameters that match the declared parameters of the annotated method.</li>
    // </ol>
    // A constructor corresponding to an annotated method is defined in the class denoted by
    // {@link #value()}. In order, its signature is as follows:
    // <ol>
    // <li>A sequence of zero or more {@linkplain Node.InjectedNodeParameter injected} parameters.</li>
    // <li>Remaining parameters that match the declared parameters of the annotated method.</li>
    //
    // There must be exactly one such factory method or constructor corresponding to a
    // {@link Node.NodeIntrinsic} annotated method.
    ///
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    // @iface Node.NodeIntrinsic
    public static @interface NodeIntrinsic
    {
        ///
        // The class declaring the factory method or {@link Node} subclass declaring the constructor
        // used to intrinsify a call to the annotated method. The default value is the class in
        // which the annotated method is declared.
        ///
        Class<?> value() default Node.NodeIntrinsic.class;

        ///
        // If {@code true}, the factory method or constructor selected by the annotation must have
        // an {@linkplain Node.InjectedNodeParameter injected} {@link Stamp} parameter. Calling
        // {@link AbstractPointerStamp#nonNull()} on the injected stamp is guaranteed to return {@code true}.
        ///
        boolean injectedStampIsNonNull() default false;
    }

    ///
    // Marker for a node that can be replaced by another node via global value numbering. A
    // {@linkplain NodeClass#isLeafNode() leaf} node can be replaced by another node of the same
    // type that has exactly the same {@linkplain NodeClass#getData() data} values. A non-leaf node
    // can be replaced by another node of the same type that has exactly the same data values as
    // well as the same {@linkplain Node#inputs() inputs} and {@linkplain Node#successors() successors}.
    ///
    // @iface Node.ValueNumberable
    public interface ValueNumberable
    {
    }

    ///
    // Marker interface for nodes that contains other nodes. When the inputs to this node changes,
    // users of this node should also be placed on the work list for canonicalization.
    ///
    // @iface Node.IndirectCanonicalization
    public interface IndirectCanonicalization
    {
    }

    // @field
    private Graph ___graph;
    // @field
    int ___id;

    // this next pointer is used in Graph to implement fast iteration over NodeClass types, it
    // therefore points to the next Node of the same type.
    // @field
    Node ___typeCacheNext;

    // @def
    static final int INLINE_USAGE_COUNT = 2;
    // @def
    private static final Node[] NO_NODES = {};

    ///
    // Head of usage list. The elements of the usage list in order are {@link #usage0},
    // {@link #usage1} and {@link #extraUsages}. The first null entry terminates the list.
    ///
    // @field
    Node ___usage0;
    // @field
    Node ___usage1;
    // @field
    Node[] ___extraUsages;
    // @field
    int ___extraUsagesCount;

    // @field
    private Node ___predecessor;
    // @field
    private NodeClass<? extends Node> ___nodeClass;

    // @def
    public static final int NODE_LIST = -2;
    // @def
    public static final int NOT_ITERABLE = -1;

    // @cons Node
    public Node(NodeClass<? extends Node> __c)
    {
        super();
        init(__c);
    }

    final void init(NodeClass<? extends Node> __c)
    {
        this.___nodeClass = __c;
        this.___id = INITIAL_ID;
        this.___extraUsages = NO_NODES;
    }

    final int id()
    {
        return this.___id;
    }

    @Override
    public Node asNode()
    {
        return this;
    }

    ///
    // Gets the graph context of this node.
    ///
    public Graph graph()
    {
        return this.___graph;
    }

    ///
    // Returns an {@link NodeIterable iterable} which can be used to traverse all non-null input
    // edges of this node.
    //
    // @return an {@link NodeIterable iterable} for all non-null input edges.
    ///
    public NodeIterable<Node> inputs()
    {
        return this.___nodeClass.getInputIterable(this);
    }

    ///
    // Returns an {@link Iterable iterable} which can be used to traverse all non-null input edges
    // of this node.
    //
    // @return an {@link Iterable iterable} for all non-null input edges.
    ///
    public Iterable<Position> inputPositions()
    {
        return this.___nodeClass.getInputEdges().getPositionsIterable(this);
    }

    // @class Node.EdgeVisitor
    public abstract static class EdgeVisitor
    {
        public abstract Node apply(Node __source, Node __target);
    }

    ///
    // Applies the given visitor to all inputs of this node.
    //
    // @param visitor the visitor to be applied to the inputs
    ///
    public void applyInputs(Node.EdgeVisitor __visitor)
    {
        this.___nodeClass.applyInputs(this, __visitor);
    }

    ///
    // Applies the given visitor to all successors of this node.
    //
    // @param visitor the visitor to be applied to the successors
    ///
    public void applySuccessors(Node.EdgeVisitor __visitor)
    {
        this.___nodeClass.applySuccessors(this, __visitor);
    }

    ///
    // Returns an {@link NodeIterable iterable} which can be used to traverse all non-null successor
    // edges of this node.
    //
    // @return an {@link NodeIterable iterable} for all non-null successor edges.
    ///
    public NodeIterable<Node> successors()
    {
        return this.___nodeClass.getSuccessorIterable(this);
    }

    ///
    // Returns an {@link Iterable iterable} which can be used to traverse all successor edge
    // positions of this node.
    //
    // @return an {@link Iterable iterable} for all successor edge positoins.
    ///
    public Iterable<Position> successorPositions()
    {
        return this.___nodeClass.getSuccessorEdges().getPositionsIterable(this);
    }

    ///
    // Gets the maximum number of usages this node has had at any point in time.
    ///
    public int getUsageCount()
    {
        if (this.___usage0 == null)
        {
            return 0;
        }
        if (this.___usage1 == null)
        {
            return 1;
        }
        return INLINE_USAGE_COUNT + this.___extraUsagesCount;
    }

    ///
    // Gets the list of nodes that use this node (i.e., as an input).
    ///
    public final NodeIterable<Node> usages()
    {
        return new NodeUsageIterable(this);
    }

    ///
    // Checks whether this node has no usages.
    ///
    public final boolean hasNoUsages()
    {
        return this.___usage0 == null;
    }

    ///
    // Checks whether this node has usages.
    ///
    public final boolean hasUsages()
    {
        return this.___usage0 != null;
    }

    ///
    // Checks whether this node has more than one usages.
    ///
    public final boolean hasMoreThanOneUsage()
    {
        return this.___usage1 != null;
    }

    ///
    // Checks whether this node has exactly one usgae.
    ///
    public final boolean hasExactlyOneUsage()
    {
        return hasUsages() && !hasMoreThanOneUsage();
    }

    ///
    // Adds a given node to this node's {@linkplain #usages() usages}.
    //
    // @param node the node to add
    ///
    void addUsage(Node __node)
    {
        if (this.___usage0 == null)
        {
            this.___usage0 = __node;
        }
        else if (this.___usage1 == null)
        {
            this.___usage1 = __node;
        }
        else
        {
            int __length = this.___extraUsages.length;
            if (__length == 0)
            {
                this.___extraUsages = new Node[4];
            }
            else if (this.___extraUsagesCount == __length)
            {
                Node[] __newExtraUsages = new Node[__length * 2 + 1];
                System.arraycopy(this.___extraUsages, 0, __newExtraUsages, 0, __length);
                this.___extraUsages = __newExtraUsages;
            }
            this.___extraUsages[this.___extraUsagesCount++] = __node;
        }
    }

    private void movUsageFromEndTo(int __destIndex)
    {
        if (__destIndex >= INLINE_USAGE_COUNT)
        {
            movUsageFromEndToExtraUsages(__destIndex - INLINE_USAGE_COUNT);
        }
        else if (__destIndex == 1)
        {
            movUsageFromEndToIndexOne();
        }
        else
        {
            movUsageFromEndToIndexZero();
        }
    }

    private void movUsageFromEndToExtraUsages(int __destExtraIndex)
    {
        this.___extraUsagesCount--;
        Node __n = this.___extraUsages[this.___extraUsagesCount];
        this.___extraUsages[__destExtraIndex] = __n;
        this.___extraUsages[this.___extraUsagesCount] = null;
    }

    private void movUsageFromEndToIndexZero()
    {
        if (this.___extraUsagesCount > 0)
        {
            this.___extraUsagesCount--;
            this.___usage0 = this.___extraUsages[this.___extraUsagesCount];
            this.___extraUsages[this.___extraUsagesCount] = null;
        }
        else if (this.___usage1 != null)
        {
            this.___usage0 = this.___usage1;
            this.___usage1 = null;
        }
        else
        {
            this.___usage0 = null;
        }
    }

    private void movUsageFromEndToIndexOne()
    {
        if (this.___extraUsagesCount > 0)
        {
            this.___extraUsagesCount--;
            this.___usage1 = this.___extraUsages[this.___extraUsagesCount];
            this.___extraUsages[this.___extraUsagesCount] = null;
        }
        else
        {
            this.___usage1 = null;
        }
    }

    ///
    // Removes a given node from this node's {@linkplain #usages() usages}.
    //
    // @param node the node to remove
    // @return whether or not {@code usage} was in the usage list
    ///
    public boolean removeUsage(Node __node)
    {
        // For large graphs, usage removal is critical for performance.
        // Furthermore, it is critical that this method maintains the invariant,
        // that the usage list has no null element preceding a non-null element.
        if (this.___usage0 == __node)
        {
            movUsageFromEndToIndexZero();
            return true;
        }
        if (this.___usage1 == __node)
        {
            movUsageFromEndToIndexOne();
            return true;
        }
        for (int __i = this.___extraUsagesCount - 1; __i >= 0; __i--)
        {
            if (this.___extraUsages[__i] == __node)
            {
                movUsageFromEndToExtraUsages(__i);
                return true;
            }
        }
        return false;
    }

    public final Node predecessor()
    {
        return this.___predecessor;
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
        return this.___id <= DELETED_ID_START;
    }

    public final boolean isAlive()
    {
        return this.___id >= ALIVE_ID_START;
    }

    public final boolean isUnregistered()
    {
        return this.___id == INITIAL_ID;
    }

    ///
    // Updates the usages sets of the given nodes after an input slot is changed from
    // {@code oldInput} to {@code newInput} by removing this node from {@code oldInput}'s usages and
    // adds this node to {@code newInput}'s usages.
    ///
    protected void updateUsages(Node __oldInput, Node __newInput)
    {
        if (__oldInput != __newInput)
        {
            if (__oldInput != null)
            {
                boolean __result = removeThisFromUsages(__oldInput);
            }
            maybeNotifyInputChanged(this);
            if (__newInput != null)
            {
                __newInput.addUsage(this);
            }
            if (__oldInput != null && __oldInput.hasNoUsages())
            {
                maybeNotifyZeroUsages(__oldInput);
            }
        }
    }

    protected void updateUsagesInterface(NodeInterface __oldInput, NodeInterface __newInput)
    {
        updateUsages(__oldInput == null ? null : __oldInput.asNode(), __newInput == null ? null : __newInput.asNode());
    }

    ///
    // Updates the predecessor of the given nodes after a successor slot is changed from
    // oldSuccessor to newSuccessor: removes this node from oldSuccessor's predecessors and adds
    // this node to newSuccessor's predecessors.
    ///
    protected void updatePredecessor(Node __oldSuccessor, Node __newSuccessor)
    {
        if (__oldSuccessor != __newSuccessor)
        {
            if (__oldSuccessor != null)
            {
                __oldSuccessor.___predecessor = null;
            }
            if (__newSuccessor != null)
            {
                __newSuccessor.___predecessor = this;
            }
        }
    }

    void initialize(Graph __newGraph)
    {
        this.___graph = __newGraph;
        __newGraph.register(this);
        NodeClass<? extends Node> __nc = this.___nodeClass;
        __nc.registerAtInputsAsUsage(this);
        __nc.registerAtSuccessorsAsPredecessor(this);
    }

    ///
    // Information associated with this node. A single value is stored directly in the field.
    // Multiple values are stored by creating an Object[].
    ///
    // @field
    private Object ___annotation;

    private <T> T getNodeInfo(Class<T> __clazz)
    {
        if (this.___annotation == null)
        {
            return null;
        }
        if (__clazz.isInstance(this.___annotation))
        {
            return __clazz.cast(this.___annotation);
        }
        if (this.___annotation.getClass() == Object[].class)
        {
            Object[] __annotations = (Object[]) this.___annotation;
            for (Object __ann : __annotations)
            {
                if (__clazz.isInstance(__ann))
                {
                    return __clazz.cast(__ann);
                }
            }
        }
        return null;
    }

    private <T> void setNodeInfo(Class<T> __clazz, T __value)
    {
        if (this.___annotation == null || __clazz.isInstance(this.___annotation))
        {
            // replace the current value
            this.___annotation = __value;
        }
        else if (this.___annotation.getClass() == Object[].class)
        {
            Object[] __annotations = (Object[]) this.___annotation;
            for (int __i = 0; __i < __annotations.length; __i++)
            {
                if (__clazz.isInstance(__annotations[__i]))
                {
                    __annotations[__i] = __value;
                    return;
                }
            }
            Object[] __newAnnotations = Arrays.copyOf(__annotations, __annotations.length + 1);
            __newAnnotations[__annotations.length] = __value;
            this.___annotation = __newAnnotations;
        }
        else
        {
            this.___annotation = new Object[] { this.___annotation, __value };
        }
    }

    public final NodeClass<? extends Node> getNodeClass()
    {
        return this.___nodeClass;
    }

    public boolean isAllowedUsageType(InputType __type)
    {
        if (__type == InputType.Value)
        {
            return false;
        }
        return getNodeClass().getAllowedUsageTypes().contains(__type);
    }

    private boolean checkReplaceWith(Node __other)
    {
        if (this.___graph != null && this.___graph.isFrozen())
        {
            throw new GraalError("cannot modify frozen graph");
        }
        if (__other == this)
        {
            throw new GraalError("cannot replace a node with itself");
        }
        if (isDeleted())
        {
            throw new GraalError("cannot replace deleted node");
        }
        if (__other != null && __other.isDeleted())
        {
            throw new GraalError("cannot replace with deleted node %s", __other);
        }
        return true;
    }

    public final void replaceAtUsages(Node __other)
    {
        replaceAtAllUsages(__other, (Node) null);
    }

    public final void replaceAtUsages(Node __other, Predicate<Node> __filter)
    {
        replaceAtUsages(__other, __filter, null);
    }

    public final void replaceAtUsagesAndDelete(Node __other)
    {
        replaceAtUsages(__other, null, this);
        safeDelete();
    }

    public final void replaceAtUsagesAndDelete(Node __other, Predicate<Node> __filter)
    {
        replaceAtUsages(__other, __filter, this);
        safeDelete();
    }

    protected void replaceAtUsages(Node __other, Predicate<Node> __filter, Node __toBeDeleted)
    {
        if (__filter == null)
        {
            replaceAtAllUsages(__other, __toBeDeleted);
        }
        else
        {
            replaceAtMatchingUsages(__other, __filter, __toBeDeleted);
        }
    }

    protected void replaceAtAllUsages(Node __other, Node __toBeDeleted)
    {
        checkReplaceWith(__other);
        if (this.___usage0 == null)
        {
            return;
        }
        replaceAtUsage(__other, __toBeDeleted, this.___usage0);
        this.___usage0 = null;

        if (this.___usage1 == null)
        {
            return;
        }
        replaceAtUsage(__other, __toBeDeleted, this.___usage1);
        this.___usage1 = null;

        if (this.___extraUsagesCount <= 0)
        {
            return;
        }
        for (int __i = 0; __i < this.___extraUsagesCount; __i++)
        {
            Node __usage = this.___extraUsages[__i];
            replaceAtUsage(__other, __toBeDeleted, __usage);
        }
        this.___extraUsages = NO_NODES;
        this.___extraUsagesCount = 0;
    }

    private void replaceAtUsage(Node __other, Node __toBeDeleted, Node __usage)
    {
        boolean __result = __usage.getNodeClass().replaceFirstInput(__usage, this, __other);
        // Don't notify for nodes which are about to be deleted.
        if (__toBeDeleted == null || __usage != __toBeDeleted)
        {
            maybeNotifyInputChanged(__usage);
        }
        if (__other != null)
        {
            __other.addUsage(__usage);
        }
    }

    private void replaceAtMatchingUsages(Node __other, Predicate<Node> __filter, Node __toBeDeleted)
    {
        if (__filter == null)
        {
            throw new GraalError("filter cannot be null");
        }
        checkReplaceWith(__other);
        int __i = 0;
        while (__i < this.getUsageCount())
        {
            Node __usage = this.getUsageAt(__i);
            if (__filter.test(__usage))
            {
                replaceAtUsage(__other, __toBeDeleted, __usage);
                this.movUsageFromEndTo(__i);
            }
            else
            {
                ++__i;
            }
        }
    }

    public Node getUsageAt(int __index)
    {
        if (__index == 0)
        {
            return this.___usage0;
        }
        else if (__index == 1)
        {
            return this.___usage1;
        }
        else
        {
            return this.___extraUsages[__index - INLINE_USAGE_COUNT];
        }
    }

    public void replaceAtMatchingUsages(Node __other, NodePredicate __usagePredicate)
    {
        checkReplaceWith(__other);
        replaceAtMatchingUsages(__other, __usagePredicate, null);
    }

    public void replaceAtUsages(InputType __type, Node __other)
    {
        checkReplaceWith(__other);
        for (Node __usage : usages().snapshot())
        {
            for (Position __pos : __usage.inputPositions())
            {
                if (__pos.getInputType() == __type && __pos.get(__usage) == this)
                {
                    __pos.set(__usage, __other);
                }
            }
        }
    }

    private void maybeNotifyInputChanged(Node __node)
    {
        if (this.___graph != null)
        {
            Graph.NodeEventListener __listener = this.___graph.___nodeEventListener;
            if (__listener != null)
            {
                __listener.event(Graph.NodeEvent.INPUT_CHANGED, __node);
            }
        }
    }

    public void maybeNotifyZeroUsages(Node __node)
    {
        if (this.___graph != null)
        {
            Graph.NodeEventListener __listener = this.___graph.___nodeEventListener;
            if (__listener != null && __node.isAlive())
            {
                __listener.event(Graph.NodeEvent.ZERO_USAGES, __node);
            }
        }
    }

    public void replaceAtPredecessor(Node __other)
    {
        checkReplaceWith(__other);
        if (this.___predecessor != null)
        {
            if (!this.___predecessor.getNodeClass().replaceFirstSuccessor(this.___predecessor, this, __other))
            {
                throw new GraalError("not found in successors, predecessor: %s", this.___predecessor);
            }
            this.___predecessor.updatePredecessor(this, __other);
        }
    }

    public void replaceAndDelete(Node __other)
    {
        checkReplaceWith(__other);
        if (__other == null)
        {
            throw new GraalError("cannot replace with null");
        }
        if (this.hasUsages())
        {
            replaceAtUsages(__other);
        }
        replaceAtPredecessor(__other);
        this.safeDelete();
    }

    public void replaceFirstSuccessor(Node __oldSuccessor, Node __newSuccessor)
    {
        if (this.___nodeClass.replaceFirstSuccessor(this, __oldSuccessor, __newSuccessor))
        {
            updatePredecessor(__oldSuccessor, __newSuccessor);
        }
    }

    public void replaceFirstInput(Node __oldInput, Node __newInput)
    {
        if (this.___nodeClass.replaceFirstInput(this, __oldInput, __newInput))
        {
            updateUsages(__oldInput, __newInput);
        }
    }

    public void clearInputs()
    {
        getNodeClass().unregisterAtInputsAsUsage(this);
    }

    boolean removeThisFromUsages(Node __n)
    {
        return __n.removeUsage(this);
    }

    public void clearSuccessors()
    {
        getNodeClass().unregisterAtSuccessorsAsPredecessor(this);
    }

    ///
    // Removes this node from its graph. This node must have no {@linkplain Node#usages() usages}
    // and no {@linkplain #predecessor() predecessor}.
    ///
    public void safeDelete()
    {
        this.clearInputs();
        this.clearSuccessors();
        markDeleted();
    }

    public void markDeleted()
    {
        this.___graph.unregister(this);
        this.___id = DELETED_ID_START - this.___id;
    }

    public final Node copyWithInputs()
    {
        return copyWithInputs(true);
    }

    public final Node copyWithInputs(boolean __insertIntoGraph)
    {
        Node __newNode = clone(__insertIntoGraph ? this.___graph : null, WithOnlyInputEdges);
        if (__insertIntoGraph)
        {
            for (Node __input : inputs())
            {
                __input.addUsage(__newNode);
            }
        }
        return __newNode;
    }

    ///
    // Must be overridden by subclasses that implement {@link Simplifiable}. The implementation in
    // {@link Node} exists to obviate the need to cast a node before invoking
    // {@link Simplifiable#simplify(SimplifierTool)}.
    ///
    public void simplify(SimplifierTool __tool)
    {
        throw new UnsupportedOperationException();
    }

    ///
    // @param newNode the result of cloning this node or {@link Unsafe#allocateInstance(Class) raw
    //            allocating} a copy of this node
    // @param type the type of edges to process
    // @param edgesToCopy if {@code type} is in this set, the edges are copied otherwise they are cleared
    ///
    private void copyOrClearEdgesForClone(Node __newNode, Edges.EdgesType __type, EnumSet<Edges.EdgesType> __edgesToCopy)
    {
        if (__edgesToCopy.contains(__type))
        {
            getNodeClass().getEdges(__type).copy(this, __newNode);
        }
        else
        {
            // the direct edges are already null
            getNodeClass().getEdges(__type).initializeLists(__newNode, this);
        }
    }

    // @def
    public static final EnumSet<Edges.EdgesType> WithNoEdges = EnumSet.noneOf(Edges.EdgesType.class);
    // @def
    public static final EnumSet<Edges.EdgesType> WithAllEdges = EnumSet.allOf(Edges.EdgesType.class);
    // @def
    public static final EnumSet<Edges.EdgesType> WithOnlyInputEdges = EnumSet.of(Edges.EdgesType.Inputs);
    // @def
    public static final EnumSet<Edges.EdgesType> WithOnlySucessorEdges = EnumSet.of(Edges.EdgesType.Successors);

    ///
    // Makes a copy of this node in(to) a given graph.
    //
    // @param into the graph in which the copy will be registered (which may be this node's graph)
    //            or null if the copy should not be registered in a graph
    // @param edgesToCopy specifies the edges to be copied. The edges not specified in this set are
    //            initialized to their default value (i.e., {@code null} for a direct edge, an empty
    //            list for an edge list)
    // @return the copy of this node
    ///
    final Node clone(Graph __into, EnumSet<Edges.EdgesType> __edgesToCopy)
    {
        final NodeClass<? extends Node> __nodeClassTmp = getNodeClass();
        boolean __useIntoLeafNodeCache = false;
        if (__into != null)
        {
            if (__nodeClassTmp.valueNumberable() && __nodeClassTmp.isLeafNode())
            {
                __useIntoLeafNodeCache = true;
                Node __otherNode = __into.findNodeInCache(this);
                if (__otherNode != null)
                {
                    return __otherNode;
                }
            }
        }

        Node __newNode = null;
        try
        {
            __newNode = (Node) UnsafeAccess.UNSAFE.allocateInstance(getClass());
            __newNode.___nodeClass = __nodeClassTmp;
            __nodeClassTmp.getData().copy(this, __newNode);
            copyOrClearEdgesForClone(__newNode, Edges.EdgesType.Inputs, __edgesToCopy);
            copyOrClearEdgesForClone(__newNode, Edges.EdgesType.Successors, __edgesToCopy);
        }
        catch (Exception __e)
        {
            throw new GraalError(__e);
        }
        __newNode.___graph = __into;
        __newNode.___id = INITIAL_ID;
        if (__into != null)
        {
            __into.register(__newNode);
        }
        __newNode.___extraUsages = NO_NODES;

        if (__into != null && __useIntoLeafNodeCache)
        {
            __into.putNodeIntoCache(__newNode);
        }
        __newNode.afterClone(this);
        return __newNode;
    }

    protected void afterClone(@SuppressWarnings("unused") Node __other)
    {
    }

    public Iterable<? extends Node> cfgPredecessors()
    {
        if (this.___predecessor == null)
        {
            return Collections.emptySet();
        }
        else
        {
            return Collections.singleton(this.___predecessor);
        }
    }

    ///
    // Returns an iterator that will provide all control-flow successors of this node. Normally this
    // will be the contents of all fields annotated with {@link Node.Successor}, but some node classes
    // (like EndNode) may return different nodes.
    ///
    public Iterable<? extends Node> cfgSuccessors()
    {
        return successors();
    }

    ///
    // Nodes using their {@link #id} as the hash code. This works very well when nodes of the same
    // graph are stored in sets. It can give bad behavior when storing nodes of different graphs in
    // the same set.
    ///
    @Override
    public final int hashCode()
    {
        if (this.isDeleted())
        {
            return -this.___id + DELETED_ID_START;
        }
        return this.___id;
    }

    ///
    // Do not overwrite the equality test of a node in subclasses.
    // Equality tests must rely solely on identity.
    ///

    @Deprecated
    public int getId()
    {
        return this.___id;
    }

    ///
    // Determines if this node's {@link NodeClass#getData() data} fields are equal to the
    // data fields of another node of the same type. Primitive fields are compared by value
    // and non-primitive fields are compared by {@link Objects#equals(Object, Object)}.
    //
    // The result of this method undefined if {@code other.getClass() != this.getClass()}.
    //
    // @param other a node of exactly the same type as this node
    // @return true if the data fields of this object and {@code other} are equal
    ///
    public boolean valueEquals(Node __other)
    {
        return getNodeClass().dataEquals(this, __other);
    }

    ///
    // Determines if this node is equal to the other node while ignoring differences in
    // {@linkplain Node.Successor control-flow} edges.
    ///
    public boolean dataFlowEquals(Node __other)
    {
        return this == __other || this.___nodeClass == __other.getNodeClass() && this.valueEquals(__other) && this.___nodeClass.equalInputs(this, __other);
    }

    public final void pushInputs(NodeStack __stack)
    {
        getNodeClass().pushInputs(this, __stack);
    }
}
