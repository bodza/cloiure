package giraaff.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.ValueProxy;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * A node that changes the type of its input, usually narrowing it. For example, a {@link PiNode}
 * refines the type of a receiver during type-guarded inlining to be the type tested by the guard.
 *
 * In contrast to a {@link GuardedValueNode}, a {@link PiNode} is useless as soon as the type of its
 * input is as narrow or narrower than the {@link PiNode}'s type. The {@link PiNode}, and therefore
 * also the scheduling restriction enforced by the guard, will go away.
 */
// @class PiNode
public class PiNode extends FloatingGuardedNode implements LIRLowerable, Virtualizable, IterableNodeType, Canonicalizable, ValueProxy
{
    // @def
    public static final NodeClass<PiNode> TYPE = NodeClass.create(PiNode.class);

    @Input
    // @field
    ValueNode object;
    // @field
    protected Stamp piStamp;

    public ValueNode object()
    {
        return object;
    }

    // @cons
    protected PiNode(NodeClass<? extends PiNode> __c, ValueNode __object, Stamp __stamp, GuardingNode __guard)
    {
        super(__c, __stamp, __guard);
        this.object = __object;
        this.piStamp = __stamp;
        inferStamp();
    }

    // @cons
    public PiNode(ValueNode __object, Stamp __stamp)
    {
        this(__object, __stamp, null);
    }

    // @cons
    public PiNode(ValueNode __object, Stamp __stamp, ValueNode __guard)
    {
        this(TYPE, __object, __stamp, (GuardingNode) __guard);
    }

    // @cons
    public PiNode(ValueNode __object, ValueNode __guard)
    {
        this(__object, AbstractPointerStamp.pointerNonNull(__object.stamp(NodeView.DEFAULT)), __guard);
    }

    // @cons
    public PiNode(ValueNode __object, ResolvedJavaType __toType, boolean __exactType, boolean __nonNull)
    {
        this(__object, StampFactory.object(__exactType ? TypeReference.createExactTrusted(__toType) : TypeReference.createWithoutAssumptions(__toType), __nonNull || StampTool.isPointerNonNull(__object.stamp(NodeView.DEFAULT))));
    }

    public static ValueNode create(ValueNode __object, Stamp __stamp)
    {
        ValueNode __value = canonical(__object, __stamp, null);
        if (__value != null)
        {
            return __value;
        }
        return new PiNode(__object, __stamp);
    }

    public static ValueNode create(ValueNode __object, Stamp __stamp, ValueNode __guard)
    {
        ValueNode __value = canonical(__object, __stamp, (GuardingNode) __guard);
        if (__value != null)
        {
            return __value;
        }
        return new PiNode(__object, __stamp, __guard);
    }

    public static ValueNode create(ValueNode __object, ValueNode __guard)
    {
        Stamp __stamp = AbstractPointerStamp.pointerNonNull(__object.stamp(NodeView.DEFAULT));
        ValueNode __value = canonical(__object, __stamp, (GuardingNode) __guard);
        if (__value != null)
        {
            return __value;
        }
        return new PiNode(__object, __stamp, __guard);
    }

    @SuppressWarnings("unused")
    public static boolean intrinsify(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode __object, ValueNode __guard)
    {
        Stamp __stamp = AbstractPointerStamp.pointerNonNull(__object.stamp(NodeView.DEFAULT));
        ValueNode __value = canonical(__object, __stamp, (GuardingNode) __guard);
        if (__value == null)
        {
            __value = new PiNode(__object, __stamp, __guard);
        }
        __b.push(JavaKind.Object, __b.append(__value));
        return true;
    }

    @SuppressWarnings("unused")
    public static boolean intrinsify(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode __object, ResolvedJavaType __toType, boolean __exactType, boolean __nonNull)
    {
        Stamp __stamp = StampFactory.object(__exactType ? TypeReference.createExactTrusted(__toType) : TypeReference.createWithoutAssumptions(__toType), __nonNull || StampTool.isPointerNonNull(__object.stamp(NodeView.DEFAULT)));
        ValueNode __value = canonical(__object, __stamp, null);
        if (__value == null)
        {
            __value = new PiNode(__object, __stamp);
        }
        __b.push(JavaKind.Object, __b.append(__value));
        return true;
    }

    public final Stamp piStamp()
    {
        return piStamp;
    }

    public void strengthenPiStamp(Stamp __newPiStamp)
    {
        this.piStamp = __newPiStamp;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        if (__gen.hasOperand(object))
        {
            __gen.setResult(this, __gen.operand(object));
        }
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(computeStamp());
    }

    private Stamp computeStamp()
    {
        return piStamp.improveWith(object().stamp(NodeView.DEFAULT));
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(object());
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
            if (StampTool.typeOrNull(this) != null && StampTool.typeOrNull(this).isAssignableFrom(__virtual.type()))
            {
                __tool.replaceWithVirtual(__virtual);
            }
        }
    }

    public static ValueNode canonical(ValueNode __object, Stamp __stamp, GuardingNode __guard)
    {
        // Use most up to date stamp.
        Stamp __computedStamp = __stamp.improveWith(__object.stamp(NodeView.DEFAULT));

        // The pi node does not give any additional information => skip it.
        if (__computedStamp.equals(__object.stamp(NodeView.DEFAULT)))
        {
            return __object;
        }

        if (__guard == null)
        {
            // Try to merge the pi node with a load node.
            if (__object instanceof ReadNode && !__object.hasMoreThanOneUsage())
            {
                ReadNode __readNode = (ReadNode) __object;
                __readNode.setStamp(__readNode.stamp(NodeView.DEFAULT).improveWith(__stamp));
                return __readNode;
            }
        }
        else
        {
            for (Node __n : __guard.asNode().usages())
            {
                if (__n instanceof PiNode)
                {
                    PiNode __otherPi = (PiNode) __n;
                    if (__object == __otherPi.object() && __computedStamp.equals(__otherPi.stamp(NodeView.DEFAULT)))
                    {
                        // Two PiNodes with the same guard and same result, so return the one with the more precise piStamp.
                        Stamp __newStamp = __stamp.join(__otherPi.piStamp);
                        if (__newStamp.equals(__otherPi.piStamp))
                        {
                            return __otherPi;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        Node __value = canonical(object(), stamp(NodeView.DEFAULT), getGuard());
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    @Override
    public ValueNode getOriginalNode()
    {
        return object;
    }

    public void setOriginalNode(ValueNode __newNode)
    {
        this.updateUsages(object, __newNode);
        this.object = __newNode;
    }

    /**
     * Casts an object to have an exact, non-null stamp representing {@link Class}.
     */
    public static Class<?> asNonNullClass(Object __object)
    {
        return asNonNullClassIntrinsic(__object, Class.class, true, true);
    }

    /**
     * Casts an object to have an exact, non-null stamp representing {@link Class}.
     */
    public static Class<?> asNonNullObject(Object __object)
    {
        return asNonNullClassIntrinsic(__object, Object.class, false, true);
    }

    @NodeIntrinsic(PiNode.class)
    private static native Class<?> asNonNullClassIntrinsic(Object object, @ConstantNodeParameter Class<?> toType, @ConstantNodeParameter boolean exactType, @ConstantNodeParameter boolean nonNull);

    /**
     * Changes the stamp of an object inside a snippet to be the stamp of the node replaced by the snippet.
     */
    @NodeIntrinsic(PiNode.Placeholder.class)
    public static native Object piCastToSnippetReplaceeStamp(Object object);

    /**
     * Changes the stamp of an object and ensures the newly stamped value is non-null and does not
     * float above a given guard.
     */
    @NodeIntrinsic
    public static native Object piCastNonNull(Object object, GuardingNode guard);

    /**
     * Changes the stamp of an object and ensures the newly stamped value is non-null and does not
     * float above a given guard.
     */
    @NodeIntrinsic
    public static native Class<?> piCastNonNullClass(Class<?> type, GuardingNode guard);

    /**
     * Changes the stamp of an object to represent a given type and to indicate that the object is
     * not null.
     */
    public static Object piCastNonNull(Object __object, @ConstantNodeParameter Class<?> __toType)
    {
        return piCast(__object, __toType, false, true);
    }

    @NodeIntrinsic
    public static native Object piCast(Object object, @ConstantNodeParameter Class<?> toType, @ConstantNodeParameter boolean exactType, @ConstantNodeParameter boolean nonNull);

    /**
     * A placeholder node in a snippet that will be replaced with a {@link PiNode} when the snippet
     * is instantiated.
     */
    // @class PiNode.Placeholder
    public static class Placeholder extends FloatingGuardedNode
    {
        // @def
        public static final NodeClass<Placeholder> TYPE = NodeClass.create(Placeholder.class);

        @Input
        // @field
        ValueNode object;

        public ValueNode object()
        {
            return object;
        }

        // @cons
        protected Placeholder(NodeClass<? extends Placeholder> __c, ValueNode __object)
        {
            super(__c, PlaceholderStamp.SINGLETON, null);
            this.object = __object;
        }

        // @cons
        public Placeholder(ValueNode __object)
        {
            this(TYPE, __object);
        }

        /**
         * Replaces this node with a {@link PiNode} during snippet instantiation.
         *
         * @param snippetReplaceeStamp the stamp of the node being replace by the snippet
         */
        public void makeReplacement(Stamp __snippetReplaceeStamp)
        {
            ValueNode __value = graph().maybeAddOrUnique(PiNode.create(object(), __snippetReplaceeStamp, null));
            replaceAndDelete(__value);
        }
    }

    /**
     * A stamp for {@link Placeholder} nodes which are only used in snippets. It is replaced by an
     * actual stamp when the snippet is instantiated.
     */
    // @class PiNode.PlaceholderStamp
    public static final class PlaceholderStamp extends ObjectStamp
    {
        // @def
        private static final PlaceholderStamp SINGLETON = new PlaceholderStamp();

        public static PlaceholderStamp singleton()
        {
            return SINGLETON;
        }

        // @cons
        private PlaceholderStamp()
        {
            super(null, false, false, false);
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object __obj)
        {
            return this == __obj;
        }
    }
}
