package giraaff.nodes.java;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.virtual.VirtualArrayNode;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * The {@code LoadIndexedNode} represents a read from an element of an array.
 */
// @class LoadIndexedNode
public class LoadIndexedNode extends AccessIndexedNode implements Virtualizable, Canonicalizable
{
    // @def
    public static final NodeClass<LoadIndexedNode> TYPE = NodeClass.create(LoadIndexedNode.class);

    /**
     * Creates a new LoadIndexedNode.
     *
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param elementKind the element type
     */
    // @cons
    public LoadIndexedNode(Assumptions __assumptions, ValueNode __array, ValueNode __index, JavaKind __elementKind)
    {
        this(TYPE, createStamp(__assumptions, __array, __elementKind), __array, __index, __elementKind);
    }

    public static ValueNode create(Assumptions __assumptions, ValueNode __array, ValueNode __index, JavaKind __elementKind, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection)
    {
        ValueNode __constant = tryConstantFold(__array, __index, __metaAccess, __constantReflection);
        if (__constant != null)
        {
            return __constant;
        }
        return new LoadIndexedNode(__assumptions, __array, __index, __elementKind);
    }

    // @cons
    protected LoadIndexedNode(NodeClass<? extends LoadIndexedNode> __c, Stamp __stamp, ValueNode __array, ValueNode __index, JavaKind __elementKind)
    {
        super(__c, __stamp, __array, __index, __elementKind);
    }

    private static Stamp createStamp(Assumptions __assumptions, ValueNode __array, JavaKind __kind)
    {
        ResolvedJavaType __type = StampTool.typeOrNull(__array);
        if (__kind == JavaKind.Object && __type != null && __type.isArray())
        {
            return StampFactory.object(TypeReference.createTrusted(__assumptions, __type.getComponentType()));
        }
        else
        {
            JavaKind __preciseKind = determinePreciseArrayElementType(__array, __kind);
            return StampFactory.forKind(__preciseKind);
        }
    }

    private static JavaKind determinePreciseArrayElementType(ValueNode __array, JavaKind __kind)
    {
        if (__kind == JavaKind.Byte)
        {
            ResolvedJavaType __javaType = ((ObjectStamp) __array.stamp(NodeView.DEFAULT)).type();
            if (__javaType != null && __javaType.isArray() && __javaType.getComponentType() != null && __javaType.getComponentType().getJavaKind() == JavaKind.Boolean)
            {
                return JavaKind.Boolean;
            }
        }
        return __kind;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(stamp.improveWith(createStamp(graph().getAssumptions(), array(), elementKind())));
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(array());
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualArrayNode __virtual = (VirtualArrayNode) __alias;
            ValueNode __indexValue = __tool.getAlias(index());
            int __idx = __indexValue.isConstant() ? __indexValue.asJavaConstant().asInt() : -1;
            if (__idx >= 0 && __idx < __virtual.entryCount())
            {
                ValueNode __entry = __tool.getEntry(__virtual, __idx);
                if (stamp.isCompatible(__entry.stamp(NodeView.DEFAULT)))
                {
                    __tool.replaceWith(__entry);
                }
            }
        }
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        ValueNode __constant = tryConstantFold(array(), index(), __tool.getMetaAccess(), __tool.getConstantReflection());
        if (__constant != null)
        {
            return __constant;
        }
        return this;
    }

    private static ValueNode tryConstantFold(ValueNode __array, ValueNode __index, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection)
    {
        if (__array.isConstant() && !__array.isNullConstant() && __index.isConstant())
        {
            JavaConstant __arrayConstant = __array.asJavaConstant();
            if (__arrayConstant != null)
            {
                int __stableDimension = ((ConstantNode) __array).getStableDimension();
                if (__stableDimension > 0)
                {
                    JavaConstant __constant = __constantReflection.readArrayElement(__arrayConstant, __index.asJavaConstant().asInt());
                    boolean __isDefaultStable = ((ConstantNode) __array).isDefaultStable();
                    if (__constant != null && (__isDefaultStable || !__constant.isDefaultForKind()))
                    {
                        return ConstantNode.forConstant(__constant, __stableDimension - 1, __isDefaultStable, __metaAccess);
                    }
                }
            }
        }
        return null;
    }
}
