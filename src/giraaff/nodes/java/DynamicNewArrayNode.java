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
    public static final NodeClass<DynamicNewArrayNode> TYPE = NodeClass.create(DynamicNewArrayNode.class);

    @Input ValueNode elementType;

    /**
     * Class pointer to void.class needs to be exposed earlier than this node is lowered so that it
     * can be replaced by the AOT machinery. If it's not needed for lowering this input can be ignored.
     */
    @OptionalInput ValueNode voidClass;

    /**
     * A non-null value indicating the worst case element type. Mainly useful for distinguishing
     * Object arrays from primitive arrays.
     */
    protected final JavaKind knownElementKind;

    // @cons
    public DynamicNewArrayNode(ValueNode elementType, ValueNode length, boolean fillContents)
    {
        this(TYPE, elementType, length, fillContents, null, null, null);
    }

    // @cons
    public DynamicNewArrayNode(@InjectedNodeParameter MetaAccessProvider metaAccess, ValueNode elementType, ValueNode length, boolean fillContents, JavaKind knownElementKind)
    {
        this(TYPE, elementType, length, fillContents, knownElementKind, null, metaAccess);
    }

    private static Stamp computeStamp(JavaKind knownElementKind, MetaAccessProvider metaAccess)
    {
        if (knownElementKind != null && metaAccess != null)
        {
            ResolvedJavaType arrayType = metaAccess.lookupJavaType(knownElementKind == JavaKind.Object ? Object.class : knownElementKind.toJavaClass()).getArrayClass();
            return StampFactory.objectNonNull(TypeReference.createWithoutAssumptions(arrayType));
        }
        return StampFactory.objectNonNull();
    }

    // @cons
    protected DynamicNewArrayNode(NodeClass<? extends DynamicNewArrayNode> c, ValueNode elementType, ValueNode length, boolean fillContents, JavaKind knownElementKind, FrameState stateBefore, MetaAccessProvider metaAccess)
    {
        super(c, computeStamp(knownElementKind, metaAccess), length, fillContents, stateBefore);
        this.elementType = elementType;
        this.knownElementKind = knownElementKind;
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
    public Node canonical(CanonicalizerTool tool)
    {
        if (elementType.isConstant())
        {
            ResolvedJavaType type = tool.getConstantReflection().asJavaType(elementType.asConstant());
            if (type != null && !throwsIllegalArgumentException(type))
            {
                return createNewArrayNode(type);
            }
        }
        return this;
    }

    /**
     * Hook for subclasses to instantiate a subclass of {@link NewArrayNode}.
     */
    protected NewArrayNode createNewArrayNode(ResolvedJavaType type)
    {
        return new NewArrayNode(type, length(), fillContents(), stateBefore());
    }

    public static boolean throwsIllegalArgumentException(Class<?> elementType, Class<?> voidClass)
    {
        return elementType == voidClass;
    }

    public static boolean throwsIllegalArgumentException(ResolvedJavaType elementType)
    {
        return elementType.getJavaKind() == JavaKind.Void;
    }

    @NodeIntrinsic
    private static native Object newArray(Class<?> componentType, int length, @ConstantNodeParameter boolean fillContents);

    public static Object newArray(Class<?> componentType, int length)
    {
        return newArray(componentType, length, true);
    }

    @NodeIntrinsic
    private static native Object newArray(Class<?> componentType, int length, @ConstantNodeParameter boolean fillContents, @ConstantNodeParameter JavaKind knownElementKind);

    public static Object newArray(Class<?> componentType, int length, JavaKind knownElementKind)
    {
        return newArray(componentType, length, true, knownElementKind);
    }

    public static Object newUninitializedArray(Class<?> componentType, int length, JavaKind knownElementKind)
    {
        return newArray(componentType, length, false, knownElementKind);
    }

    public ValueNode getVoidClass()
    {
        return voidClass;
    }

    public void setVoidClass(ValueNode newVoidClass)
    {
        updateUsages(voidClass, newVoidClass);
        voidClass = newVoidClass;
    }
}
