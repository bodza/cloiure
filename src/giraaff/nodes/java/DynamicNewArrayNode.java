package giraaff.nodes.java;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;

/**
 * The {@code DynamicNewArrayNode} is used for allocation of arrays when the type is not a
 * compile-time constant.
 */
// @class DynamicNewArrayNode
public final class DynamicNewArrayNode extends AbstractNewArrayNode implements Canonicalizable
{
    // @def
    public static final NodeClass<DynamicNewArrayNode> TYPE = NodeClass.create(DynamicNewArrayNode.class);

    @Input
    // @field
    ValueNode elementType;

    /**
     * Class pointer to void.class needs to be exposed earlier than this node is lowered so that it
     * can be replaced by the AOT machinery. If it's not needed for lowering this input can be ignored.
     */
    @OptionalInput
    // @field
    ValueNode voidClass;

    /**
     * A non-null value indicating the worst case element type. Mainly useful for distinguishing
     * Object arrays from primitive arrays.
     */
    // @field
    protected final JavaKind knownElementKind;

    // @cons
    public DynamicNewArrayNode(ValueNode __elementType, ValueNode __length, boolean __fillContents)
    {
        this(TYPE, __elementType, __length, __fillContents, null, null, null);
    }

    // @cons
    public DynamicNewArrayNode(@InjectedNodeParameter MetaAccessProvider __metaAccess, ValueNode __elementType, ValueNode __length, boolean __fillContents, JavaKind __knownElementKind)
    {
        this(TYPE, __elementType, __length, __fillContents, __knownElementKind, null, __metaAccess);
    }

    private static Stamp computeStamp(JavaKind __knownElementKind, MetaAccessProvider __metaAccess)
    {
        if (__knownElementKind != null && __metaAccess != null)
        {
            ResolvedJavaType __arrayType = __metaAccess.lookupJavaType(__knownElementKind == JavaKind.Object ? Object.class : __knownElementKind.toJavaClass()).getArrayClass();
            return StampFactory.objectNonNull(TypeReference.createWithoutAssumptions(__arrayType));
        }
        return StampFactory.objectNonNull();
    }

    // @cons
    protected DynamicNewArrayNode(NodeClass<? extends DynamicNewArrayNode> __c, ValueNode __elementType, ValueNode __length, boolean __fillContents, JavaKind __knownElementKind, FrameState __stateBefore, MetaAccessProvider __metaAccess)
    {
        super(__c, computeStamp(__knownElementKind, __metaAccess), __length, __fillContents, __stateBefore);
        this.elementType = __elementType;
        this.knownElementKind = __knownElementKind;
    }

    public ValueNode getElementType()
    {
        return elementType;
    }

    public JavaKind getKnownElementKind()
    {
        return knownElementKind;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (elementType.isConstant())
        {
            ResolvedJavaType __type = __tool.getConstantReflection().asJavaType(elementType.asConstant());
            if (__type != null && !throwsIllegalArgumentException(__type))
            {
                return createNewArrayNode(__type);
            }
        }
        return this;
    }

    /**
     * Hook for subclasses to instantiate a subclass of {@link NewArrayNode}.
     */
    protected NewArrayNode createNewArrayNode(ResolvedJavaType __type)
    {
        return new NewArrayNode(__type, length(), fillContents(), stateBefore());
    }

    public static boolean throwsIllegalArgumentException(Class<?> __elementType, Class<?> __voidClass)
    {
        return __elementType == __voidClass;
    }

    public static boolean throwsIllegalArgumentException(ResolvedJavaType __elementType)
    {
        return __elementType.getJavaKind() == JavaKind.Void;
    }

    @NodeIntrinsic
    private static native Object newArray(Class<?> componentType, int length, @ConstantNodeParameter boolean fillContents);

    public static Object newArray(Class<?> __componentType, int __length)
    {
        return newArray(__componentType, __length, true);
    }

    @NodeIntrinsic
    private static native Object newArray(Class<?> componentType, int length, @ConstantNodeParameter boolean fillContents, @ConstantNodeParameter JavaKind knownElementKind);

    public static Object newArray(Class<?> __componentType, int __length, JavaKind __knownElementKind)
    {
        return newArray(__componentType, __length, true, __knownElementKind);
    }

    public static Object newUninitializedArray(Class<?> __componentType, int __length, JavaKind __knownElementKind)
    {
        return newArray(__componentType, __length, false, __knownElementKind);
    }

    public ValueNode getVoidClass()
    {
        return voidClass;
    }

    public void setVoidClass(ValueNode __newVoidClass)
    {
        updateUsages(voidClass, __newVoidClass);
        voidClass = __newVoidClass;
    }
}
