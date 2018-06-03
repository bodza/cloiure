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
import giraaff.graph.Edges.Type;
import giraaff.graph.Graph.DuplicationReplacement;
import giraaff.graph.InputEdges;
import giraaff.graph.Node;
import giraaff.graph.Node.EdgeVisitor;
import giraaff.graph.Node.Input;
import giraaff.graph.Node.OptionalInput;
import giraaff.graph.Node.Successor;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.Simplifiable;
import giraaff.nodeinfo.InputType;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;

/**
 * Metadata for every {@link Node} type. The metadata includes:
 *
 * <li>The offsets of fields annotated with {@link Input} and {@link Successor} as well as methods
 * for iterating over such fields.</li>
 * <li>The identifier for an {@link IterableNodeType} class.</li>
 */
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

    /**
     * Gets the {@link NodeClass} associated with a given {@link Class}.
     */
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
    private final InputEdges inputs;
    // @field
    private final SuccessorEdges successors;
    // @field
    private final NodeClass<? super T> superNodeClass;

    // @field
    private final boolean canGVN;
    // @field
    private final int startGVNNumber;
    // @field
    private final int iterableId;
    // @field
    private final EnumSet<InputType> allowedUsageTypes;
    // @field
    private int[] iterableIds;
    // @field
    private final long inputsIteration;
    // @field
    private final long successorIteration;

    /**
     * Determines if this node type implements {@link Canonicalizable}.
     */
    // @field
    private final boolean isCanonicalizable;

    /**
     * Determines if this node type implements {@link BinaryCommutative}.
     */
    // @field
    private final boolean isCommutative;

    /**
     * Determines if this node type implements {@link Simplifiable}.
     */
    // @field
    private final boolean isSimplifiable;
    // @field
    private final boolean isLeafNode;

    // @field
    private final int leafId;

    // @cons
    public NodeClass(Class<T> __clazz, NodeClass<? super T> __superNodeClass)
    {
        this(__clazz, __superNodeClass, new FieldsScanner.DefaultCalcOffset(), null, 0);
    }

    // @cons
    public NodeClass(Class<T> __clazz, NodeClass<? super T> __superNodeClass, FieldsScanner.CalcOffset __calcOffset, int[] __presetIterableIds, int __presetIterableId)
    {
        super(__clazz);
        this.superNodeClass = __superNodeClass;

        this.isCanonicalizable = Canonicalizable.class.isAssignableFrom(__clazz);
        this.isCommutative = BinaryCommutative.class.isAssignableFrom(__clazz);

        this.isSimplifiable = Simplifiable.class.isAssignableFrom(__clazz);

        NodeFieldsScanner __fs = new NodeFieldsScanner(__calcOffset, __superNodeClass);
        __fs.scan(__clazz, __clazz.getSuperclass(), false);

        successors = new SuccessorEdges(__fs.directSuccessors, __fs.successors);
        successorIteration = computeIterationMask(successors.type(), successors.getDirectCount(), successors.getOffsets());
        inputs = new InputEdges(__fs.directInputs, __fs.inputs);
        inputsIteration = computeIterationMask(inputs.type(), inputs.getDirectCount(), inputs.getOffsets());

        data = new Fields(__fs.data);

        isLeafNode = inputs.getCount() + successors.getCount() == 0;
        if (isLeafNode)
        {
            this.leafId = nextLeafId.getAndIncrement();
        }
        else
        {
            this.leafId = -1;
        }

        canGVN = Node.ValueNumberable.class.isAssignableFrom(__clazz);
        startGVNNumber = __clazz.getName().hashCode();

        allowedUsageTypes = __superNodeClass == null ? EnumSet.noneOf(InputType.class) : __superNodeClass.allowedUsageTypes.clone();

        if (__presetIterableIds != null)
        {
            this.iterableIds = __presetIterableIds;
            this.iterableId = __presetIterableId;
        }
        else if (IterableNodeType.class.isAssignableFrom(__clazz))
        {
            this.iterableId = nextIterableId.getAndIncrement();

            NodeClass<?> __snc = __superNodeClass;
            while (__snc != null && IterableNodeType.class.isAssignableFrom(__snc.getClazz()))
            {
                __snc.addIterableId(iterableId);
                __snc = __snc.superNodeClass;
            }

            this.iterableIds = new int[] { iterableId };
        }
        else
        {
            this.iterableId = Node.NOT_ITERABLE;
            this.iterableIds = null;
        }
    }

    public static long computeIterationMask(Type __type, int __directCount, long[] __offsets)
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
        int[] __copy = Arrays.copyOf(iterableIds, iterableIds.length + 1);
        __copy[iterableIds.length] = __newIterableId;
        iterableIds = __copy;
    }

    private boolean verifyIterableIds()
    {
        NodeClass<?> __snc = superNodeClass;
        while (__snc != null && IterableNodeType.class.isAssignableFrom(__snc.getClazz()))
        {
            __snc = __snc.superNodeClass;
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
    private String shortName;

    public String shortName()
    {
        if (shortName == null)
        {
            String __localShortName = getClazz().getSimpleName();
            if (__localShortName.endsWith("Node") && !__localShortName.equals("StartNode") && !__localShortName.equals("EndNode"))
            {
                shortName = __localShortName.substring(0, __localShortName.length() - 4);
            }
            else
            {
                shortName = __localShortName;
            }
        }
        return shortName;
    }

    @Override
    public Fields[] getAllFields()
    {
        return new Fields[] { data, inputs, successors };
    }

    int[] iterableIds()
    {
        return iterableIds;
    }

    public int iterableId()
    {
        return iterableId;
    }

    public boolean valueNumberable()
    {
        return canGVN;
    }

    /**
     * Determines if this node type implements {@link Canonicalizable}.
     */
    public boolean isCanonicalizable()
    {
        return isCanonicalizable;
    }

    /**
     * Determines if this node type implements {@link BinaryCommutative}.
     */
    public boolean isCommutative()
    {
        return isCommutative;
    }

    /**
     * Determines if this node type implements {@link Simplifiable}.
     */
    public boolean isSimplifiable()
    {
        return isSimplifiable;
    }

    static int allocatedNodeIterabledIds()
    {
        return nextIterableId.get();
    }

    public EnumSet<InputType> getAllowedUsageTypes()
    {
        return allowedUsageTypes;
    }

    /**
     * Describes a field representing an input or successor edge in a node.
     */
    // @class NodeClass.EdgeInfo
    protected static class EdgeInfo extends FieldsScanner.FieldInfo
    {
        // @cons
        public EdgeInfo(long __offset, String __name, Class<?> __type, Class<?> __declaringClass)
        {
            super(__offset, __name, __type, __declaringClass);
        }

        /**
         * Sorts non-list edges before list edges.
         */
        @Override
        public int compareTo(FieldsScanner.FieldInfo __o)
        {
            if (NodeList.class.isAssignableFrom(__o.type))
            {
                if (!NodeList.class.isAssignableFrom(type))
                {
                    return -1;
                }
            }
            else
            {
                if (NodeList.class.isAssignableFrom(type))
                {
                    return 1;
                }
            }
            return super.compareTo(__o);
        }
    }

    /**
     * Describes a field representing an {@linkplain Type#Inputs input} edge in a node.
     */
    // @class NodeClass.InputInfo
    protected static final class InputInfo extends EdgeInfo
    {
        // @field
        final InputType inputType;
        // @field
        final boolean optional;

        // @cons
        public InputInfo(long __offset, String __name, Class<?> __type, Class<?> __declaringClass, InputType __inputType, boolean __optional)
        {
            super(__offset, __name, __type, __declaringClass);
            this.inputType = __inputType;
            this.optional = __optional;
        }
    }

    // @class NodeClass.NodeFieldsScanner
    protected static final class NodeFieldsScanner extends FieldsScanner
    {
        // @field
        public final ArrayList<InputInfo> inputs = new ArrayList<>();
        // @field
        public final ArrayList<EdgeInfo> successors = new ArrayList<>();
        // @field
        int directInputs;
        // @field
        int directSuccessors;

        // @cons
        protected NodeFieldsScanner(FieldsScanner.CalcOffset __calc, NodeClass<?> __superNodeClass)
        {
            super(__calc);
            if (__superNodeClass != null)
            {
                InputEdges.translateInto(__superNodeClass.inputs, inputs);
                Edges.translateInto(__superNodeClass.successors, successors);
                Fields.translateInto(__superNodeClass.data, data);
                directInputs = __superNodeClass.inputs.getDirectCount();
                directSuccessors = __superNodeClass.successors.getDirectCount();
            }
        }

        @Override
        protected void scanField(Field __field, long __offset)
        {
            Input __inputAnnotation = getAnnotationTimed(__field, Node.Input.class);
            OptionalInput __optionalInputAnnotation = getAnnotationTimed(__field, Node.OptionalInput.class);
            Successor __successorAnnotation = getAnnotationTimed(__field, Successor.class);

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
                    directInputs++;
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
                inputs.add(new InputInfo(__offset, __field.getName(), __type, __field.getDeclaringClass(), __inputType, __field.isAnnotationPresent(Node.OptionalInput.class)));
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
                    directSuccessors++;
                }
                successors.add(new EdgeInfo(__offset, __field.getName(), __type, __field.getDeclaringClass()));
            }
            else
            {
                GraalError.guarantee(!NODE_CLASS.isAssignableFrom(__type) || __field.getName().equals("Null"), "suspicious node __field: %s", __field);
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
        if (canGVN)
        {
            __number = startGVNNumber;
            for (int __i = 0; __i < data.getCount(); ++__i)
            {
                Class<?> __type = data.getType(__i);
                if (__type.isPrimitive())
                {
                    if (__type == Integer.TYPE)
                    {
                        int __intValue = data.getInt(__n, __i);
                        __number += __intValue;
                    }
                    else if (__type == Long.TYPE)
                    {
                        long __longValue = data.getLong(__n, __i);
                        __number += __longValue ^ (__longValue >>> 32);
                    }
                    else if (__type == Boolean.TYPE)
                    {
                        boolean __booleanValue = data.getBoolean(__n, __i);
                        if (__booleanValue)
                        {
                            __number += 7;
                        }
                    }
                    else if (__type == Float.TYPE)
                    {
                        float __floatValue = data.getFloat(__n, __i);
                        __number += Float.floatToRawIntBits(__floatValue);
                    }
                    else if (__type == Double.TYPE)
                    {
                        double __doubleValue = data.getDouble(__n, __i);
                        long __longValue = Double.doubleToRawLongBits(__doubleValue);
                        __number += __longValue ^ (__longValue >>> 32);
                    }
                    else if (__type == Short.TYPE)
                    {
                        short __shortValue = data.getShort(__n, __i);
                        __number += __shortValue;
                    }
                    else if (__type == Character.TYPE)
                    {
                        char __charValue = data.getChar(__n, __i);
                        __number += __charValue;
                    }
                    else if (__type == Byte.TYPE)
                    {
                        byte __byteValue = data.getByte(__n, __i);
                        __number += __byteValue;
                    }
                }
                else
                {
                    Object __o = data.getObject(__n, __i);
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
        for (int __i = 0; __i < data.getCount(); ++__i)
        {
            Class<?> __type = data.getType(__i);
            if (__type.isPrimitive())
            {
                if (__type == Integer.TYPE)
                {
                    int __aInt = data.getInt(__a, __i);
                    int __bInt = data.getInt(__b, __i);
                    if (__aInt != __bInt)
                    {
                        return false;
                    }
                }
                else if (__type == Boolean.TYPE)
                {
                    boolean __aBoolean = data.getBoolean(__a, __i);
                    boolean __bBoolean = data.getBoolean(__b, __i);
                    if (__aBoolean != __bBoolean)
                    {
                        return false;
                    }
                }
                else if (__type == Long.TYPE)
                {
                    long __aLong = data.getLong(__a, __i);
                    long __bLong = data.getLong(__b, __i);
                    if (__aLong != __bLong)
                    {
                        return false;
                    }
                }
                else if (__type == Float.TYPE)
                {
                    float __aFloat = data.getFloat(__a, __i);
                    float __bFloat = data.getFloat(__b, __i);
                    if (__aFloat != __bFloat)
                    {
                        return false;
                    }
                }
                else if (__type == Double.TYPE)
                {
                    double __aDouble = data.getDouble(__a, __i);
                    double __bDouble = data.getDouble(__b, __i);
                    if (__aDouble != __bDouble)
                    {
                        return false;
                    }
                }
                else if (__type == Short.TYPE)
                {
                    short __aShort = data.getShort(__a, __i);
                    short __bShort = data.getShort(__b, __i);
                    if (__aShort != __bShort)
                    {
                        return false;
                    }
                }
                else if (__type == Character.TYPE)
                {
                    char __aChar = data.getChar(__a, __i);
                    char __bChar = data.getChar(__b, __i);
                    if (__aChar != __bChar)
                    {
                        return false;
                    }
                }
                else if (__type == Byte.TYPE)
                {
                    byte __aByte = data.getByte(__a, __i);
                    byte __bByte = data.getByte(__b, __i);
                    if (__aByte != __bByte)
                    {
                        return false;
                    }
                }
            }
            else
            {
                Object __objectA = data.getObject(__a, __i);
                Object __objectB = data.getObject(__b, __i);
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

    static void updateEdgesInPlace(Node __node, InplaceUpdateClosure __duplicationReplacement, Edges __edges)
    {
        int __index = 0;
        Type __curType = __edges.type();
        int __directCount = __edges.getDirectCount();
        final long[] __curOffsets = __edges.getOffsets();
        while (__index < __directCount)
        {
            Node __edge = Edges.getNode(__node, __curOffsets, __index);
            if (__edge != null)
            {
                Node __newEdge = __duplicationReplacement.replacement(__edge, __curType);
                if (__curType == Edges.Type.Inputs)
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

    void updateInputSuccInPlace(Node __node, InplaceUpdateClosure __duplicationReplacement)
    {
        updateEdgesInPlace(__node, __duplicationReplacement, inputs);
        updateEdgesInPlace(__node, __duplicationReplacement, successors);
    }

    private static NodeList<Node> updateEdgeListCopy(Node __node, NodeList<Node> __list, InplaceUpdateClosure __duplicationReplacement, Edges.Type __type)
    {
        NodeList<Node> __result = __type == Edges.Type.Inputs ? new NodeInputList<>(__node, __list.size()) : new NodeSuccessorList<>(__node, __list.size());

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

    /**
     * Gets the input or successor edges defined by this node class.
     */
    public Edges getEdges(Edges.Type __type)
    {
        return __type == Edges.Type.Inputs ? inputs : successors;
    }

    public Edges getInputEdges()
    {
        return inputs;
    }

    public Edges getSuccessorEdges()
    {
        return successors;
    }

    /**
     * Returns a newly allocated node for which no subclass-specific constructor has been called.
     */
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
        Node replacement(Node node, Edges.Type type);
    }

    static EconomicMap<Node, Node> addGraphDuplicate(final Graph __graph, final Graph __oldGraph, int __estimatedNodeCount, Iterable<? extends Node> __nodes, final DuplicationReplacement __replacements)
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
        InplaceUpdateClosure replacementClosure = new InplaceUpdateClosure()
        {
            @Override
            public Node replacement(Node __node, Edges.Type __type)
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
                    else if (__node.graph() == __graph && __type == Edges.Type.Inputs)
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

    private static void createNodeDuplicates(final Graph __graph, Iterable<? extends Node> __nodes, final DuplicationReplacement __replacements, final EconomicMap<Node, Node> __newNodes)
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

    private static void transferEdgesDifferentNodeClass(final Graph __graph, final DuplicationReplacement __replacements, final EconomicMap<Node, Node> __newNodes, Node __oldNode, Node __node)
    {
        transferEdges(__graph, __replacements, __newNodes, __oldNode, __node, Edges.Type.Inputs);
        transferEdges(__graph, __replacements, __newNodes, __oldNode, __node, Edges.Type.Successors);
    }

    private static void transferEdges(final Graph __graph, final DuplicationReplacement __replacements, final EconomicMap<Node, Node> __newNodes, Node __oldNode, Node __node, Edges.Type __type)
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
                    else if (__type == Edges.Type.Inputs && __oldEdge.graph() == __graph)
                    {
                        // patch to the outer world
                        __target = __oldEdge;
                    }
                }
                __pos.set(__node, __target);
            }
        }
    }

    /**
     * @return true if the node has no inputs and no successors
     */
    public boolean isLeafNode()
    {
        return isLeafNode;
    }

    public int getLeafId()
    {
        return this.leafId;
    }

    public NodeClass<? super T> getSuperNodeClass()
    {
        return superNodeClass;
    }

    public long inputsIteration()
    {
        return inputsIteration;
    }

    /**
     * An iterator that will iterate over edges.
     *
     * An iterator of this type will not return null values, unless edges are modified concurrently.
     * Concurrent modifications are detected by an assertion on a best-effort basis.
     */
    // @class NodeClass.RawEdgesIterator
    private static final class RawEdgesIterator implements Iterator<Node>
    {
        // @field
        protected final Node node;
        // @field
        protected long mask;
        // @field
        protected Node nextValue;

        // @cons
        RawEdgesIterator(Node __node, long __mask)
        {
            super();
            this.node = __node;
            this.mask = __mask;
        }

        @Override
        public boolean hasNext()
        {
            Node __next = nextValue;
            if (__next != null)
            {
                return true;
            }
            else
            {
                nextValue = forward();
                return nextValue != null;
            }
        }

        private Node forward()
        {
            while (mask != 0)
            {
                Node __next = getInput();
                mask = advanceInput();
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
            Node __next = nextValue;
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
                nextValue = null;
                return __next;
            }
        }

        public final long advanceInput()
        {
            int __state = (int) mask & 0x03;
            if (__state == 0)
            {
                // Skip normal field.
                return mask >>> NEXT_EDGE;
            }
            else if (__state == 1)
            {
                // We are iterating a node list.
                if ((mask & 0xFFFF00) != 0)
                {
                    // Node list count is non-zero, decrease by 1.
                    return mask - 0x100;
                }
                else
                {
                    // Node list is finished => go to next input.
                    return mask >>> 24;
                }
            }
            else
            {
                // Need to expand node list.
                NodeList<?> __nodeList = Edges.getNodeListUnsafe(node, mask & 0xFC);
                if (__nodeList != null)
                {
                    int __size = __nodeList.size();
                    if (__size != 0)
                    {
                        // Set pointer to upper most index of node list.
                        return ((mask >>> NEXT_EDGE) << 24) | (mask & 0xFD) | ((__size - 1) << NEXT_EDGE);
                    }
                }
                // Node list is empty or null => skip.
                return mask >>> NEXT_EDGE;
            }
        }

        public Node getInput()
        {
            int __state = (int) mask & 0x03;
            if (__state == 0)
            {
                return Edges.getNodeUnsafe(node, mask & 0xFC);
            }
            else if (__state == 1)
            {
                // We are iterating a node list.
                NodeList<?> __nodeList = Edges.getNodeListUnsafe(node, mask & 0xFC);
                return __nodeList.nodes[__nodeList.size() - 1 - (int) ((mask >>> NEXT_EDGE) & 0xFFFF)];
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
        long __mask = this.successorIteration;
        // @closure
        return new NodeIterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new RawEdgesIterator(__node, __mask);
            }
        };
    }

    public NodeIterable<Node> getInputIterable(final Node __node)
    {
        long __mask = this.inputsIteration;
        // @closure
        return new NodeIterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new RawEdgesIterator(__node, __mask);
            }
        };
    }

    public boolean equalSuccessors(Node __node, Node __other)
    {
        return equalEdges(__node, __other, successorIteration);
    }

    public boolean equalInputs(Node __node, Node __other)
    {
        return equalEdges(__node, __other, inputsIteration);
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
        long __myMask = this.inputsIteration;
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

    public void applySuccessors(Node __node, EdgeVisitor __consumer)
    {
        applyEdges(__node, __consumer, this.successorIteration);
    }

    public void applyInputs(Node __node, EdgeVisitor __consumer)
    {
        applyEdges(__node, __consumer, this.inputsIteration);
    }

    private static void applyEdges(Node __node, EdgeVisitor __consumer, long __mask)
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

    private static void applyHelper(Node __node, EdgeVisitor __consumer, long __offset)
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
        long __myMask = this.successorIteration;
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
        long __myMask = this.successorIteration;
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
        return replaceFirstEdge(__node, __key, __replacement, this.inputsIteration);
    }

    public boolean replaceFirstSuccessor(Node __node, Node __key, Node __replacement)
    {
        return replaceFirstEdge(__node, __key, __replacement, this.successorIteration);
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
        long __myMask = this.inputsIteration;
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
        long __myMask = this.inputsIteration;
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
