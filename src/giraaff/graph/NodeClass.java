package giraaff.graph;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.FieldIntrospection;
import giraaff.core.common.Fields;
import giraaff.core.common.FieldsScanner;
import giraaff.graph.Edges;
import giraaff.graph.Graph;
import giraaff.graph.InputEdges;
import giraaff.graph.Node;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.Simplifiable;
import giraaff.nodeinfo.InputType;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;

///
// Metadata for every {@link Node} type. The metadata includes:
//
// <li>The offsets of fields annotated with {@link Node.Input} and {@link Node.Successor} as well as methods
// for iterating over such fields.</li>
// <li>The identifier for an {@link IterableNodeType} class.</li>
///
// @class NodeClass
public final class NodeClass<T> extends FieldIntrospection<T>
{
    // @def
    public static final long MAX_EDGES = 8;
    // @def
    public static final long MAX_LIST_EDGES = 6;
    // @def
    public static final long OFFSET_MASK = 0xFC;
    // @def
    public static final long LIST_MASK = 0x01;
    // @def
    public static final long NEXT_EDGE = 0x08;

    private static <T extends Annotation> T getAnnotationTimed(AnnotatedElement __e, Class<T> __annotationClass)
    {
        return __e.getAnnotation(__annotationClass);
    }

    ///
    // Gets the {@link NodeClass} associated with a given {@link Class}.
    ///
    public static <T> NodeClass<T> create(Class<T> __c)
    {
        Class<? super T> __superclass = __c.getSuperclass();
        NodeClass<? super T> __nodeSuperclass = null;
        if (__superclass != NODE_CLASS)
        {
            __nodeSuperclass = get(__superclass);
        }
        return new NodeClass<>(__c, __nodeSuperclass);
    }

    @SuppressWarnings("unchecked")
    private static <T> NodeClass<T> getUnchecked(Class<T> __clazz)
    {
        try
        {
            Field __field = __clazz.getDeclaredField("TYPE");
            __field.setAccessible(true);
            return (NodeClass<T>) __field.get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException __e)
        {
            throw new RuntimeException(__e);
        }
    }

    public static <T> NodeClass<T> get(Class<T> __clazz)
    {
        NodeClass<T> __result = getUnchecked(__clazz);
        if (__result != null || __clazz == NODE_CLASS)
        {
            return __result;
        }

        throw GraalError.shouldNotReachHere("Reflective field access of TYPE field returned null. This is probably a bug in HotSpot class initialization.");
    }

    // @def
    private static final Class<?> NODE_CLASS = Node.class;
    // @def
    private static final Class<?> INPUT_LIST_CLASS = NodeInputList.class;
    // @def
    private static final Class<?> SUCCESSOR_LIST_CLASS = NodeSuccessorList.class;

    // @def
    private static AtomicInteger nextIterableId = new AtomicInteger();
    // @def
    private static AtomicInteger nextLeafId = new AtomicInteger();

    // @field
    private final InputEdges ___inputs;
    // @field
    private final SuccessorEdges ___successors;
    // @field
    private final NodeClass<? super T> ___superNodeClass;

    // @field
    private final boolean ___canGVN;
    // @field
    private final int ___startGVNNumber;
    // @field
    private final int ___iterableId;
    // @field
    private final EnumSet<InputType> ___allowedUsageTypes;
    // @field
    private int[] ___iterableIds;
    // @field
    private final long ___inputsIteration;
    // @field
    private final long ___successorIteration;

    ///
    // Determines if this node type implements {@link Canonicalizable}.
    ///
    // @field
    private final boolean ___isCanonicalizable;

    ///
    // Determines if this node type implements {@link Canonicalizable.BinaryCommutative}.
    ///
    // @field
    private final boolean ___isCommutative;

    ///
    // Determines if this node type implements {@link Simplifiable}.
    ///
    // @field
    private final boolean ___isSimplifiable;
    // @field
    private final boolean ___isLeafNode;

    // @field
    private final int ___leafId;

    // @cons NodeClass
    public NodeClass(Class<T> __clazz, NodeClass<? super T> __superNodeClass)
    {
        this(__clazz, __superNodeClass, new FieldsScanner.DefaultCalcOffset(), null, 0);
    }

    // @cons NodeClass
    public NodeClass(Class<T> __clazz, NodeClass<? super T> __superNodeClass, FieldsScanner.CalcOffset __calcOffset, int[] __presetIterableIds, int __presetIterableId)
    {
        super(__clazz);
        this.___superNodeClass = __superNodeClass;

        this.___isCanonicalizable = Canonicalizable.class.isAssignableFrom(__clazz);
        this.___isCommutative = Canonicalizable.BinaryCommutative.class.isAssignableFrom(__clazz);

        this.___isSimplifiable = Simplifiable.class.isAssignableFrom(__clazz);

        NodeClass.NodeFieldsScanner __fs = new NodeClass.NodeFieldsScanner(__calcOffset, __superNodeClass);
        __fs.scan(__clazz, __clazz.getSuperclass(), false);

        this.___successors = new SuccessorEdges(__fs.___directSuccessors, __fs.___successors);
        this.___successorIteration = computeIterationMask(this.___successors.type(), this.___successors.getDirectCount(), this.___successors.getOffsets());
        this.___inputs = new InputEdges(__fs.___directInputs, __fs.___inputs);
        this.___inputsIteration = computeIterationMask(this.___inputs.type(), this.___inputs.getDirectCount(), this.___inputs.getOffsets());

        this.___data = new Fields(__fs.___data);

        this.___isLeafNode = this.___inputs.getCount() + this.___successors.getCount() == 0;
        if (this.___isLeafNode)
        {
            this.___leafId = nextLeafId.getAndIncrement();
        }
        else
        {
            this.___leafId = -1;
        }

        this.___canGVN = Node.ValueNumberable.class.isAssignableFrom(__clazz);
        this.___startGVNNumber = __clazz.getName().hashCode();

        this.___allowedUsageTypes = __superNodeClass == null ? EnumSet.noneOf(InputType.class) : __superNodeClass.___allowedUsageTypes.clone();

        if (__presetIterableIds != null)
        {
            this.___iterableIds = __presetIterableIds;
            this.___iterableId = __presetIterableId;
        }
        else if (IterableNodeType.class.isAssignableFrom(__clazz))
        {
            this.___iterableId = nextIterableId.getAndIncrement();

            NodeClass<?> __snc = __superNodeClass;
            while (__snc != null && IterableNodeType.class.isAssignableFrom(__snc.getClazz()))
            {
                __snc.addIterableId(this.___iterableId);
                __snc = __snc.___superNodeClass;
            }

            this.___iterableIds = new int[] { this.___iterableId };
        }
        else
        {
            this.___iterableId = Node.NOT_ITERABLE;
            this.___iterableIds = null;
        }
    }

    public static long computeIterationMask(Edges.EdgesType __type, int __directCount, long[] __offsets)
    {
        long __mask = 0;
        if (__offsets.length > NodeClass.MAX_EDGES)
        {
            throw new GraalError("Exceeded maximum of %d edges (%s)", NodeClass.MAX_EDGES, __type);
        }
        if (__offsets.length - __directCount > NodeClass.MAX_LIST_EDGES)
        {
            throw new GraalError("Exceeded maximum of %d list edges (%s)", NodeClass.MAX_LIST_EDGES, __type);
        }

        for (int __i = __offsets.length - 1; __i >= 0; __i--)
        {
            long __offset = __offsets[__i];
            __mask <<= NodeClass.NEXT_EDGE;
            __mask |= __offset;
            if (__i >= __directCount)
            {
                __mask |= 0x3;
            }
        }
        return __mask;
    }

    private synchronized void addIterableId(int __newIterableId)
    {
        int[] __copy = Arrays.copyOf(this.___iterableIds, this.___iterableIds.length + 1);
        __copy[this.___iterableIds.length] = __newIterableId;
        this.___iterableIds = __copy;
    }

    private boolean verifyIterableIds()
    {
        NodeClass<?> __snc = this.___superNodeClass;
        while (__snc != null && IterableNodeType.class.isAssignableFrom(__snc.getClazz()))
        {
            __snc = __snc.___superNodeClass;
        }
        return true;
    }

    private static boolean containsId(int __iterableId, int[] __iterableIds)
    {
        for (int __i : __iterableIds)
        {
            if (__i == __iterableId)
            {
                return true;
            }
        }
        return false;
    }

    // @field
    private String ___shortName;

    public String shortName()
    {
        if (this.___shortName == null)
        {
            String __localShortName = getClazz().getSimpleName();
            if (__localShortName.endsWith("Node") && !__localShortName.equals("StartNode") && !__localShortName.equals("EndNode"))
            {
                this.___shortName = __localShortName.substring(0, __localShortName.length() - 4);
            }
            else
            {
                this.___shortName = __localShortName;
            }
        }
        return this.___shortName;
    }

    @Override
    public Fields[] getAllFields()
    {
        return new Fields[] { this.___data, this.___inputs, this.___successors };
    }

    int[] iterableIds()
    {
        return this.___iterableIds;
    }

    public int iterableId()
    {
        return this.___iterableId;
    }

    public boolean valueNumberable()
    {
        return this.___canGVN;
    }

    ///
    // Determines if this node type implements {@link Canonicalizable}.
    ///
    public boolean isCanonicalizable()
    {
        return this.___isCanonicalizable;
    }

    ///
    // Determines if this node type implements {@link Canonicalizable.BinaryCommutative}.
    ///
    public boolean isCommutative()
    {
        return this.___isCommutative;
    }

    ///
    // Determines if this node type implements {@link Simplifiable}.
    ///
    public boolean isSimplifiable()
    {
        return this.___isSimplifiable;
    }

    static int allocatedNodeIterabledIds()
    {
        return nextIterableId.get();
    }

    public EnumSet<InputType> getAllowedUsageTypes()
    {
        return this.___allowedUsageTypes;
    }

    ///
    // Describes a field representing an input or successor edge in a node.
    ///
    // @class NodeClass.EdgeInfo
    protected static class EdgeInfo extends FieldsScanner.FieldInfo
    {
        // @cons NodeClass.EdgeInfo
        public EdgeInfo(long __offset, String __name, Class<?> __type, Class<?> __declaringClass)
        {
            super(__offset, __name, __type, __declaringClass);
        }

        ///
        // Sorts non-list edges before list edges.
        ///
        @Override
        public int compareTo(FieldsScanner.FieldInfo __o)
        {
            if (NodeList.class.isAssignableFrom(__o.___type))
            {
                if (!NodeList.class.isAssignableFrom(this.___type))
                {
                    return -1;
                }
            }
            else
            {
                if (NodeList.class.isAssignableFrom(this.___type))
                {
                    return 1;
                }
            }
            return super.compareTo(__o);
        }
    }

    ///
    // Describes a field representing an {@linkplain Edges.EdgesType#Inputs input} edge in a node.
    ///
    // @class NodeClass.InputInfo
    protected static final class InputInfo extends NodeClass.EdgeInfo
    {
        // @field
        final InputType ___inputType;
        // @field
        final boolean ___optional;

        // @cons NodeClass.InputInfo
        public InputInfo(long __offset, String __name, Class<?> __type, Class<?> __declaringClass, InputType __inputType, boolean __optional)
        {
            super(__offset, __name, __type, __declaringClass);
            this.___inputType = __inputType;
            this.___optional = __optional;
        }
    }

    // @class NodeClass.NodeFieldsScanner
    protected static final class NodeFieldsScanner extends FieldsScanner
    {
        // @field
        public final ArrayList<NodeClass.InputInfo> ___inputs = new ArrayList<>();
        // @field
        public final ArrayList<NodeClass.EdgeInfo> ___successors = new ArrayList<>();
        // @field
        int ___directInputs;
        // @field
        int ___directSuccessors;

        // @cons NodeClass.NodeFieldsScanner
        protected NodeFieldsScanner(FieldsScanner.CalcOffset __calc, NodeClass<?> __superNodeClass)
        {
            super(__calc);
            if (__superNodeClass != null)
            {
                InputEdges.translateInto(__superNodeClass.___inputs, this.___inputs);
                Edges.translateInto(__superNodeClass.___successors, this.___successors);
                Fields.translateInto(__superNodeClass.___data, this.___data);
                this.___directInputs = __superNodeClass.___inputs.getDirectCount();
                this.___directSuccessors = __superNodeClass.___successors.getDirectCount();
            }
        }

        @Override
        protected void scanField(Field __field, long __offset)
        {
            Node.Input __inputAnnotation = getAnnotationTimed(__field, Node.Input.class);
            Node.OptionalInput __optionalInputAnnotation = getAnnotationTimed(__field, Node.OptionalInput.class);
            Node.Successor __successorAnnotation = getAnnotationTimed(__field, Node.Successor.class);

            Class<?> __type = __field.getType();
            int __modifiers = __field.getModifiers();

            if (__inputAnnotation != null || __optionalInputAnnotation != null)
            {
                if (INPUT_LIST_CLASS.isAssignableFrom(__type))
                {
                    // NodeInputList fields should not be final, since they are written (via Unsafe) in clearInputs()
                    GraalError.guarantee(!Modifier.isFinal(__modifiers), "NodeInputList input field %s should not be final", __field);
                    GraalError.guarantee(!Modifier.isPublic(__modifiers), "NodeInputList input field %s should not be public", __field);
                }
                else
                {
                    GraalError.guarantee(NODE_CLASS.isAssignableFrom(__type) || __type.isInterface(), "invalid input type: %s", __type);
                    GraalError.guarantee(!Modifier.isFinal(__modifiers), "Node input field %s should not be final", __field);
                    this.___directInputs++;
                }
                InputType __inputType;
                if (__inputAnnotation != null)
                {
                    __inputType = __inputAnnotation.value();
                }
                else
                {
                    __inputType = __optionalInputAnnotation.value();
                }
                this.___inputs.add(new NodeClass.InputInfo(__offset, __field.getName(), __type, __field.getDeclaringClass(), __inputType, __field.isAnnotationPresent(Node.OptionalInput.class)));
            }
            else if (__successorAnnotation != null)
            {
                if (SUCCESSOR_LIST_CLASS.isAssignableFrom(__type))
                {
                    // NodeSuccessorList fields should not be final, since they are written (via Unsafe) in clearSuccessors()
                    GraalError.guarantee(!Modifier.isFinal(__modifiers), "NodeSuccessorList successor field % should not be final", __field);
                    GraalError.guarantee(!Modifier.isPublic(__modifiers), "NodeSuccessorList successor field %s should not be public", __field);
                }
                else
                {
                    GraalError.guarantee(NODE_CLASS.isAssignableFrom(__type), "invalid successor type: %s", __type);
                    GraalError.guarantee(!Modifier.isFinal(__modifiers), "Node successor field %s should not be final", __field);
                    this.___directSuccessors++;
                }
                this.___successors.add(new NodeClass.EdgeInfo(__offset, __field.getName(), __type, __field.getDeclaringClass()));
            }
            else
            {
                GraalError.guarantee(!NODE_CLASS.isAssignableFrom(__type) || __field.getName().equals("Null"), "suspicious node field: %s", __field);
                GraalError.guarantee(!INPUT_LIST_CLASS.isAssignableFrom(__type), "suspicious node input list field: %s", __field);
                GraalError.guarantee(!SUCCESSOR_LIST_CLASS.isAssignableFrom(__type), "suspicious node successor list field: %s", __field);
                super.scanField(__field, __offset);
            }
        }
    }

    private static int deepHashCode0(Object __o)
    {
        if (__o == null)
        {
            return 0;
        }
        else if (!__o.getClass().isArray())
        {
            return __o.hashCode();
        }
        else if (__o instanceof Object[])
        {
            return Arrays.deepHashCode((Object[]) __o);
        }
        else if (__o instanceof byte[])
        {
            return Arrays.hashCode((byte[]) __o);
        }
        else if (__o instanceof short[])
        {
            return Arrays.hashCode((short[]) __o);
        }
        else if (__o instanceof int[])
        {
            return Arrays.hashCode((int[]) __o);
        }
        else if (__o instanceof long[])
        {
            return Arrays.hashCode((long[]) __o);
        }
        else if (__o instanceof char[])
        {
            return Arrays.hashCode((char[]) __o);
        }
        else if (__o instanceof float[])
        {
            return Arrays.hashCode((float[]) __o);
        }
        else if (__o instanceof double[])
        {
            return Arrays.hashCode((double[]) __o);
        }
        else if (__o instanceof boolean[])
        {
            return Arrays.hashCode((boolean[]) __o);
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
    }

    public int valueNumber(Node __n)
    {
        int __number = 0;
        if (this.___canGVN)
        {
            __number = this.___startGVNNumber;
            for (int __i = 0; __i < this.___data.getCount(); ++__i)
            {
                Class<?> __type = this.___data.getType(__i);
                if (__type.isPrimitive())
                {
                    if (__type == Integer.TYPE)
                    {
                        int __intValue = this.___data.getInt(__n, __i);
                        __number += __intValue;
                    }
                    else if (__type == Long.TYPE)
                    {
                        long __longValue = this.___data.getLong(__n, __i);
                        __number += __longValue ^ (__longValue >>> 32);
                    }
                    else if (__type == Boolean.TYPE)
                    {
                        boolean __booleanValue = this.___data.getBoolean(__n, __i);
                        if (__booleanValue)
                        {
                            __number += 7;
                        }
                    }
                    else if (__type == Short.TYPE)
                    {
                        short __shortValue = this.___data.getShort(__n, __i);
                        __number += __shortValue;
                    }
                    else if (__type == Character.TYPE)
                    {
                        char __charValue = this.___data.getChar(__n, __i);
                        __number += __charValue;
                    }
                    else if (__type == Byte.TYPE)
                    {
                        byte __byteValue = this.___data.getByte(__n, __i);
                        __number += __byteValue;
                    }
                    else
                    {
                        throw GraalError.shouldNotReachHere();
                    }
                }
                else
                {
                    Object __o = this.___data.getObject(__n, __i);
                    __number += deepHashCode0(__o);
                }
                __number *= 13;
            }
        }
        return __number;
    }

    private static boolean deepEquals0(Object __e1, Object __e2)
    {
        if (__e1 == __e2)
        {
            return true;
        }
        else if (__e1 == null || __e2 == null)
        {
            return false;
        }
        else if (!__e1.getClass().isArray() || __e1.getClass() != __e2.getClass())
        {
            return __e1.equals(__e2);
        }
        else if (__e1 instanceof Object[] && __e2 instanceof Object[])
        {
            return deepEquals((Object[]) __e1, (Object[]) __e2);
        }
        else if (__e1 instanceof int[])
        {
            return Arrays.equals((int[]) __e1, (int[]) __e2);
        }
        else if (__e1 instanceof long[])
        {
            return Arrays.equals((long[]) __e1, (long[]) __e2);
        }
        else if (__e1 instanceof byte[])
        {
            return Arrays.equals((byte[]) __e1, (byte[]) __e2);
        }
        else if (__e1 instanceof char[])
        {
            return Arrays.equals((char[]) __e1, (char[]) __e2);
        }
        else if (__e1 instanceof short[])
        {
            return Arrays.equals((short[]) __e1, (short[]) __e2);
        }
        else if (__e1 instanceof float[])
        {
            return Arrays.equals((float[]) __e1, (float[]) __e2);
        }
        else if (__e1 instanceof double[])
        {
            return Arrays.equals((double[]) __e1, (double[]) __e2);
        }
        else if (__e1 instanceof boolean[])
        {
            return Arrays.equals((boolean[]) __e1, (boolean[]) __e2);
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
    }

    private static boolean deepEquals(Object[] __a1, Object[] __a2)
    {
        int __length = __a1.length;
        if (__a2.length != __length)
        {
            return false;
        }

        for (int __i = 0; __i < __length; __i++)
        {
            if (!deepEquals0(__a1[__i], __a2[__i]))
            {
                return false;
            }
        }
        return true;
    }

    public boolean dataEquals(Node __a, Node __b)
    {
        for (int __i = 0; __i < this.___data.getCount(); ++__i)
        {
            Class<?> __type = this.___data.getType(__i);
            if (__type.isPrimitive())
            {
                if (__type == Integer.TYPE)
                {
                    int __aInt = this.___data.getInt(__a, __i);
                    int __bInt = this.___data.getInt(__b, __i);
                    if (__aInt != __bInt)
                    {
                        return false;
                    }
                }
                else if (__type == Boolean.TYPE)
                {
                    boolean __aBoolean = this.___data.getBoolean(__a, __i);
                    boolean __bBoolean = this.___data.getBoolean(__b, __i);
                    if (__aBoolean != __bBoolean)
                    {
                        return false;
                    }
                }
                else if (__type == Long.TYPE)
                {
                    long __aLong = this.___data.getLong(__a, __i);
                    long __bLong = this.___data.getLong(__b, __i);
                    if (__aLong != __bLong)
                    {
                        return false;
                    }
                }
                else if (__type == Short.TYPE)
                {
                    short __aShort = this.___data.getShort(__a, __i);
                    short __bShort = this.___data.getShort(__b, __i);
                    if (__aShort != __bShort)
                    {
                        return false;
                    }
                }
                else if (__type == Character.TYPE)
                {
                    char __aChar = this.___data.getChar(__a, __i);
                    char __bChar = this.___data.getChar(__b, __i);
                    if (__aChar != __bChar)
                    {
                        return false;
                    }
                }
                else if (__type == Byte.TYPE)
                {
                    byte __aByte = this.___data.getByte(__a, __i);
                    byte __bByte = this.___data.getByte(__b, __i);
                    if (__aByte != __bByte)
                    {
                        return false;
                    }
                }
                else
                {
                    throw GraalError.shouldNotReachHere();
                }
            }
            else
            {
                Object __objectA = this.___data.getObject(__a, __i);
                Object __objectB = this.___data.getObject(__b, __i);
                if (__objectA != __objectB)
                {
                    if (__objectA != null && __objectB != null)
                    {
                        if (!deepEquals0(__objectA, __objectB))
                        {
                            return false;
                        }
                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean isValid(Position __pos, NodeClass<?> __from, Edges __fromEdges)
    {
        if (this == __from)
        {
            return true;
        }
        Edges __toEdges = getEdges(__fromEdges.type());
        if (__pos.getIndex() >= __toEdges.getCount())
        {
            return false;
        }
        if (__pos.getIndex() >= __fromEdges.getCount())
        {
            return false;
        }
        return __toEdges.isSame(__fromEdges, __pos.getIndex());
    }

    static void updateEdgesInPlace(Node __node, NodeClass.InplaceUpdateClosure __duplicationReplacement, Edges __edges)
    {
        int __index = 0;
        Edges.EdgesType __curType = __edges.type();
        int __directCount = __edges.getDirectCount();
        final long[] __curOffsets = __edges.getOffsets();
        while (__index < __directCount)
        {
            Node __edge = Edges.getNode(__node, __curOffsets, __index);
            if (__edge != null)
            {
                Node __newEdge = __duplicationReplacement.replacement(__edge, __curType);
                if (__curType == Edges.EdgesType.Inputs)
                {
                    __node.updateUsages(null, __newEdge);
                }
                else
                {
                    __node.updatePredecessor(null, __newEdge);
                }
                __edges.initializeNode(__node, __index, __newEdge);
            }
            __index++;
        }

        while (__index < __edges.getCount())
        {
            NodeList<Node> __list = Edges.getNodeList(__node, __curOffsets, __index);
            if (__list != null)
            {
                __edges.initializeList(__node, __index, updateEdgeListCopy(__node, __list, __duplicationReplacement, __curType));
            }
            __index++;
        }
    }

    void updateInputSuccInPlace(Node __node, NodeClass.InplaceUpdateClosure __duplicationReplacement)
    {
        updateEdgesInPlace(__node, __duplicationReplacement, this.___inputs);
        updateEdgesInPlace(__node, __duplicationReplacement, this.___successors);
    }

    private static NodeList<Node> updateEdgeListCopy(Node __node, NodeList<Node> __list, NodeClass.InplaceUpdateClosure __duplicationReplacement, Edges.EdgesType __type)
    {
        NodeList<Node> __result = __type == Edges.EdgesType.Inputs ? new NodeInputList<>(__node, __list.size()) : new NodeSuccessorList<>(__node, __list.size());

        for (int __i = 0; __i < __list.count(); ++__i)
        {
            Node __oldNode = __list.get(__i);
            if (__oldNode != null)
            {
                Node __newNode = __duplicationReplacement.replacement(__oldNode, __type);
                __result.set(__i, __newNode);
            }
        }
        return __result;
    }

    ///
    // Gets the input or successor edges defined by this node class.
    ///
    public Edges getEdges(Edges.EdgesType __type)
    {
        return __type == Edges.EdgesType.Inputs ? this.___inputs : this.___successors;
    }

    public Edges getInputEdges()
    {
        return this.___inputs;
    }

    public Edges getSuccessorEdges()
    {
        return this.___successors;
    }

    ///
    // Returns a newly allocated node for which no subclass-specific constructor has been called.
    ///
    @SuppressWarnings("unchecked")
    public Node allocateInstance()
    {
        try
        {
            Node __node = (Node) UnsafeAccess.UNSAFE.allocateInstance(getJavaClass());
            __node.init((NodeClass<? extends Node>) this);
            return __node;
        }
        catch (InstantiationException __ex)
        {
            throw GraalError.shouldNotReachHere(__ex);
        }
    }

    public Class<T> getJavaClass()
    {
        return getClazz();
    }

    // @iface NodeClass.InplaceUpdateClosure
    interface InplaceUpdateClosure
    {
        Node replacement(Node __node, Edges.EdgesType __type);
    }

    static EconomicMap<Node, Node> addGraphDuplicate(final Graph __graph, final Graph __oldGraph, int __estimatedNodeCount, Iterable<? extends Node> __nodes, final Graph.DuplicationReplacement __replacements)
    {
        final EconomicMap<Node, Node> __newNodes;
        int __denseThreshold = __oldGraph.getNodeCount() + __oldGraph.getNodesDeletedSinceLastCompression() >> 4;
        if (__estimatedNodeCount > __denseThreshold)
        {
            // use dense map
            __newNodes = new NodeMap<>(__oldGraph);
        }
        else
        {
            // use sparse map
            __newNodes = EconomicMap.create(Equivalence.IDENTITY);
        }
        createNodeDuplicates(__graph, __nodes, __replacements, __newNodes);

        // @closure
        NodeClass.InplaceUpdateClosure replacementClosure = new NodeClass.InplaceUpdateClosure()
        {
            @Override
            public Node replacement(Node __node, Edges.EdgesType __type)
            {
                Node __target = __newNodes.get(__node);
                if (__target == null)
                {
                    Node __replacement = __node;
                    if (__replacements != null)
                    {
                        __replacement = __replacements.replacement(__node);
                    }
                    if (__replacement != __node)
                    {
                        __target = __replacement;
                    }
                    else if (__node.graph() == __graph && __type == Edges.EdgesType.Inputs)
                    {
                        // patch to the outer world
                        __target = __node;
                    }
                }
                return __target;
            }
        };

        // re-wire inputs
        for (Node __oldNode : __nodes)
        {
            Node __node = __newNodes.get(__oldNode);
            NodeClass<?> __nodeClass = __node.getNodeClass();
            if (__replacements == null || __replacements.replacement(__oldNode) == __oldNode)
            {
                __nodeClass.updateInputSuccInPlace(__node, replacementClosure);
            }
            else
            {
                transferEdgesDifferentNodeClass(__graph, __replacements, __newNodes, __oldNode, __node);
            }
        }

        return __newNodes;
    }

    private static void createNodeDuplicates(final Graph __graph, Iterable<? extends Node> __nodes, final Graph.DuplicationReplacement __replacements, final EconomicMap<Node, Node> __newNodes)
    {
        for (Node __node : __nodes)
        {
            if (__node != null)
            {
                Node __replacement = __node;
                if (__replacements != null)
                {
                    __replacement = __replacements.replacement(__node);
                }
                if (__replacement != __node)
                {
                    __newNodes.put(__node, __replacement);
                }
                else
                {
                    Node __newNode = __node.clone(__graph, Node.WithAllEdges);
                    __newNodes.put(__node, __newNode);
                }
            }
        }
    }

    private static void transferEdgesDifferentNodeClass(final Graph __graph, final Graph.DuplicationReplacement __replacements, final EconomicMap<Node, Node> __newNodes, Node __oldNode, Node __node)
    {
        transferEdges(__graph, __replacements, __newNodes, __oldNode, __node, Edges.EdgesType.Inputs);
        transferEdges(__graph, __replacements, __newNodes, __oldNode, __node, Edges.EdgesType.Successors);
    }

    private static void transferEdges(final Graph __graph, final Graph.DuplicationReplacement __replacements, final EconomicMap<Node, Node> __newNodes, Node __oldNode, Node __node, Edges.EdgesType __type)
    {
        NodeClass<?> __nodeClass = __node.getNodeClass();
        NodeClass<?> __oldNodeClass = __oldNode.getNodeClass();
        Edges __oldEdges = __oldNodeClass.getEdges(__type);
        for (Position __pos : __oldEdges.getPositionsIterable(__oldNode))
        {
            if (!__nodeClass.isValid(__pos, __oldNodeClass, __oldEdges))
            {
                continue;
            }
            Node __oldEdge = __pos.get(__oldNode);
            if (__oldEdge != null)
            {
                Node __target = __newNodes.get(__oldEdge);
                if (__target == null)
                {
                    Node __replacement = __oldEdge;
                    if (__replacements != null)
                    {
                        __replacement = __replacements.replacement(__oldEdge);
                    }
                    if (__replacement != __oldEdge)
                    {
                        __target = __replacement;
                    }
                    else if (__type == Edges.EdgesType.Inputs && __oldEdge.graph() == __graph)
                    {
                        // patch to the outer world
                        __target = __oldEdge;
                    }
                }
                __pos.set(__node, __target);
            }
        }
    }

    ///
    // @return true if the node has no inputs and no successors
    ///
    public boolean isLeafNode()
    {
        return this.___isLeafNode;
    }

    public int getLeafId()
    {
        return this.___leafId;
    }

    public NodeClass<? super T> getSuperNodeClass()
    {
        return this.___superNodeClass;
    }

    public long inputsIteration()
    {
        return this.___inputsIteration;
    }

    ///
    // An iterator that will iterate over edges.
    //
    // An iterator of this type will not return null values, unless edges are modified concurrently.
    // Concurrent modifications are detected by an assertion on a best-effort basis.
    ///
    // @class NodeClass.RawEdgesIterator
    private static final class RawEdgesIterator implements Iterator<Node>
    {
        // @field
        protected final Node ___node;
        // @field
        protected long ___mask;
        // @field
        protected Node ___nextValue;

        // @cons NodeClass.RawEdgesIterator
        RawEdgesIterator(Node __node, long __mask)
        {
            super();
            this.___node = __node;
            this.___mask = __mask;
        }

        @Override
        public boolean hasNext()
        {
            Node __next = this.___nextValue;
            if (__next != null)
            {
                return true;
            }
            else
            {
                this.___nextValue = forward();
                return this.___nextValue != null;
            }
        }

        private Node forward()
        {
            while (this.___mask != 0)
            {
                Node __next = getInput();
                this.___mask = advanceInput();
                if (__next != null)
                {
                    return __next;
                }
            }
            return null;
        }

        @Override
        public Node next()
        {
            Node __next = this.___nextValue;
            if (__next == null)
            {
                __next = forward();
                if (__next == null)
                {
                    throw new NoSuchElementException();
                }
                else
                {
                    return __next;
                }
            }
            else
            {
                this.___nextValue = null;
                return __next;
            }
        }

        public final long advanceInput()
        {
            int __state = (int) this.___mask & 0x03;
            if (__state == 0)
            {
                // Skip normal field.
                return this.___mask >>> NEXT_EDGE;
            }
            else if (__state == 1)
            {
                // We are iterating a node list.
                if ((this.___mask & 0xFFFF00) != 0)
                {
                    // Node list count is non-zero, decrease by 1.
                    return this.___mask - 0x100;
                }
                else
                {
                    // Node list is finished => go to next input.
                    return this.___mask >>> 24;
                }
            }
            else
            {
                // Need to expand node list.
                NodeList<?> __nodeList = Edges.getNodeListUnsafe(this.___node, this.___mask & 0xFC);
                if (__nodeList != null)
                {
                    int __size = __nodeList.size();
                    if (__size != 0)
                    {
                        // Set pointer to upper most index of node list.
                        return ((this.___mask >>> NEXT_EDGE) << 24) | (this.___mask & 0xFD) | ((__size - 1) << NEXT_EDGE);
                    }
                }
                // Node list is empty or null => skip.
                return this.___mask >>> NEXT_EDGE;
            }
        }

        public Node getInput()
        {
            int __state = (int) this.___mask & 0x03;
            if (__state == 0)
            {
                return Edges.getNodeUnsafe(this.___node, this.___mask & 0xFC);
            }
            else if (__state == 1)
            {
                // We are iterating a node list.
                NodeList<?> __nodeList = Edges.getNodeListUnsafe(this.___node, this.___mask & 0xFC);
                return __nodeList.___nodes[__nodeList.size() - 1 - (int) ((this.___mask >>> NEXT_EDGE) & 0xFFFF)];
            }
            else
            {
                // Node list needs to expand first.
                return null;
            }
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public Position nextPosition()
        {
            return null;
        }
    }

    public NodeIterable<Node> getSuccessorIterable(final Node __node)
    {
        long __mask = this.___successorIteration;
        // @closure
        return new NodeIterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new NodeClass.RawEdgesIterator(__node, __mask);
            }
        };
    }

    public NodeIterable<Node> getInputIterable(final Node __node)
    {
        long __mask = this.___inputsIteration;
        // @closure
        return new NodeIterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new NodeClass.RawEdgesIterator(__node, __mask);
            }
        };
    }

    public boolean equalSuccessors(Node __node, Node __other)
    {
        return equalEdges(__node, __other, this.___successorIteration);
    }

    public boolean equalInputs(Node __node, Node __other)
    {
        return equalEdges(__node, __other, this.___inputsIteration);
    }

    private boolean equalEdges(Node __node, Node __other, long __mask)
    {
        long __myMask = __mask;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Object __v1 = Edges.getNodeUnsafe(__node, __offset);
                Object __v2 = Edges.getNodeUnsafe(__other, __offset);
                if (__v1 != __v2)
                {
                    return false;
                }
            }
            else
            {
                Object __v1 = Edges.getNodeListUnsafe(__node, __offset);
                Object __v2 = Edges.getNodeListUnsafe(__other, __offset);
                if (!Objects.equals(__v1, __v2))
                {
                    return false;
                }
            }
            __myMask >>>= NEXT_EDGE;
        }
        return true;
    }

    public void pushInputs(Node __node, NodeStack __stack)
    {
        long __myMask = this.___inputsIteration;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Node __curNode = Edges.getNodeUnsafe(__node, __offset);
                if (__curNode != null)
                {
                    __stack.push(__curNode);
                }
            }
            else
            {
                pushAllHelper(__stack, __node, __offset);
            }
            __myMask >>>= NEXT_EDGE;
        }
    }

    private static void pushAllHelper(NodeStack __stack, Node __node, long __offset)
    {
        NodeList<Node> __list = Edges.getNodeListUnsafe(__node, __offset);
        if (__list != null)
        {
            for (int __i = 0; __i < __list.size(); ++__i)
            {
                Node __curNode = __list.get(__i);
                if (__curNode != null)
                {
                    __stack.push(__curNode);
                }
            }
        }
    }

    public void applySuccessors(Node __node, Node.EdgeVisitor __consumer)
    {
        applyEdges(__node, __consumer, this.___successorIteration);
    }

    public void applyInputs(Node __node, Node.EdgeVisitor __consumer)
    {
        applyEdges(__node, __consumer, this.___inputsIteration);
    }

    private static void applyEdges(Node __node, Node.EdgeVisitor __consumer, long __mask)
    {
        long __myMask = __mask;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Node __curNode = Edges.getNodeUnsafe(__node, __offset);
                if (__curNode != null)
                {
                    Node __newNode = __consumer.apply(__node, __curNode);
                    if (__newNode != __curNode)
                    {
                        Edges.putNodeUnsafe(__node, __offset, __newNode);
                    }
                }
            }
            else
            {
                applyHelper(__node, __consumer, __offset);
            }
            __myMask >>>= NEXT_EDGE;
        }
    }

    private static void applyHelper(Node __node, Node.EdgeVisitor __consumer, long __offset)
    {
        NodeList<Node> __list = Edges.getNodeListUnsafe(__node, __offset);
        if (__list != null)
        {
            for (int __i = 0; __i < __list.size(); ++__i)
            {
                Node __curNode = __list.get(__i);
                if (__curNode != null)
                {
                    Node __newNode = __consumer.apply(__node, __curNode);
                    if (__newNode != __curNode)
                    {
                        __list.initialize(__i, __newNode);
                    }
                }
            }
        }
    }

    public void unregisterAtSuccessorsAsPredecessor(Node __node)
    {
        long __myMask = this.___successorIteration;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Node __curNode = Edges.getNodeUnsafe(__node, __offset);
                if (__curNode != null)
                {
                    __node.updatePredecessor(__curNode, null);
                    Edges.putNodeUnsafe(__node, __offset, null);
                }
            }
            else
            {
                unregisterAtSuccessorsAsPredecessorHelper(__node, __offset);
            }
            __myMask >>>= NEXT_EDGE;
        }
    }

    private static void unregisterAtSuccessorsAsPredecessorHelper(Node __node, long __offset)
    {
        NodeList<Node> __list = Edges.getNodeListUnsafe(__node, __offset);
        if (__list != null)
        {
            for (int __i = 0; __i < __list.size(); ++__i)
            {
                Node __curNode = __list.get(__i);
                if (__curNode != null)
                {
                    __node.updatePredecessor(__curNode, null);
                }
            }
            __list.clearWithoutUpdate();
        }
    }

    public void registerAtSuccessorsAsPredecessor(Node __node)
    {
        long __myMask = this.___successorIteration;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Node __curNode = Edges.getNodeUnsafe(__node, __offset);
                if (__curNode != null)
                {
                    __node.updatePredecessor(null, __curNode);
                }
            }
            else
            {
                registerAtSuccessorsAsPredecessorHelper(__node, __offset);
            }
            __myMask >>>= NEXT_EDGE;
        }
    }

    private static void registerAtSuccessorsAsPredecessorHelper(Node __node, long __offset)
    {
        NodeList<Node> __list = Edges.getNodeListUnsafe(__node, __offset);
        if (__list != null)
        {
            for (int __i = 0; __i < __list.size(); ++__i)
            {
                Node __curNode = __list.get(__i);
                if (__curNode != null)
                {
                    __node.updatePredecessor(null, __curNode);
                }
            }
        }
    }

    public boolean replaceFirstInput(Node __node, Node __key, Node __replacement)
    {
        return replaceFirstEdge(__node, __key, __replacement, this.___inputsIteration);
    }

    public boolean replaceFirstSuccessor(Node __node, Node __key, Node __replacement)
    {
        return replaceFirstEdge(__node, __key, __replacement, this.___successorIteration);
    }

    public static boolean replaceFirstEdge(Node __node, Node __key, Node __replacement, long __mask)
    {
        long __myMask = __mask;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Object __curNode = Edges.getNodeUnsafe(__node, __offset);
                if (__curNode == __key)
                {
                    Edges.putNodeUnsafe(__node, __offset, __replacement);
                    return true;
                }
            }
            else
            {
                NodeList<Node> __list = Edges.getNodeListUnsafe(__node, __offset);
                if (__list != null && __list.replaceFirst(__key, __replacement))
                {
                    return true;
                }
            }
            __myMask >>>= NEXT_EDGE;
        }
        return false;
    }

    public void registerAtInputsAsUsage(Node __node)
    {
        long __myMask = this.___inputsIteration;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Node __curNode = Edges.getNodeUnsafe(__node, __offset);
                if (__curNode != null)
                {
                    __curNode.addUsage(__node);
                }
            }
            else
            {
                registerAtInputsAsUsageHelper(__node, __offset);
            }
            __myMask >>>= NEXT_EDGE;
        }
    }

    private static void registerAtInputsAsUsageHelper(Node __node, long __offset)
    {
        NodeList<Node> __list = Edges.getNodeListUnsafe(__node, __offset);
        if (__list != null)
        {
            for (int __i = 0; __i < __list.size(); ++__i)
            {
                Node __curNode = __list.get(__i);
                if (__curNode != null)
                {
                    __curNode.addUsage(__node);
                }
            }
        }
    }

    public void unregisterAtInputsAsUsage(Node __node)
    {
        long __myMask = this.___inputsIteration;
        while (__myMask != 0)
        {
            long __offset = (__myMask & OFFSET_MASK);
            if ((__myMask & LIST_MASK) == 0)
            {
                Node __curNode = Edges.getNodeUnsafe(__node, __offset);
                if (__curNode != null)
                {
                    __node.removeThisFromUsages(__curNode);
                    if (__curNode.hasNoUsages())
                    {
                        __node.maybeNotifyZeroUsages(__curNode);
                    }
                    Edges.putNodeUnsafe(__node, __offset, null);
                }
            }
            else
            {
                unregisterAtInputsAsUsageHelper(__node, __offset);
            }
            __myMask >>>= NEXT_EDGE;
        }
    }

    private static void unregisterAtInputsAsUsageHelper(Node __node, long __offset)
    {
        NodeList<Node> __list = Edges.getNodeListUnsafe(__node, __offset);
        if (__list != null)
        {
            for (int __i = 0; __i < __list.size(); ++__i)
            {
                Node __curNode = __list.get(__i);
                if (__curNode != null)
                {
                    __node.removeThisFromUsages(__curNode);
                    if (__curNode.hasNoUsages())
                    {
                        __node.maybeNotifyZeroUsages(__curNode);
                    }
                }
            }
            __list.clearWithoutUpdate();
        }
    }
}
