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
public class LoadIndexedNode extends AccessIndexedNode implements Virtualizable, Canonicalizable
{
    public static final NodeClass<LoadIndexedNode> TYPE = NodeClass.create(LoadIndexedNode.class);

    /**
     * Creates a new LoadIndexedNode.
     *
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param elementKind the element type
     */
    public LoadIndexedNode(Assumptions assumptions, ValueNode array, ValueNode index, JavaKind elementKind)
    {
        this(TYPE, createStamp(assumptions, array, elementKind), array, index, elementKind);
    }

    public static ValueNode create(Assumptions assumptions, ValueNode array, ValueNode index, JavaKind elementKind, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection)
    {
        ValueNode constant = tryConstantFold(array, index, metaAccess, constantReflection);
        if (constant != null)
        {
            return constant;
        }
        return new LoadIndexedNode(assumptions, array, index, elementKind);
    }

    protected LoadIndexedNode(NodeClass<? extends LoadIndexedNode> c, Stamp stamp, ValueNode array, ValueNode index, JavaKind elementKind)
    {
        super(c, stamp, array, index, elementKind);
    }

    private static Stamp createStamp(Assumptions assumptions, ValueNode array, JavaKind kind)
    {
        ResolvedJavaType type = StampTool.typeOrNull(array);
        if (kind == JavaKind.Object && type != null && type.isArray())
        {
            return StampFactory.object(TypeReference.createTrusted(assumptions, type.getComponentType()));
        }
        else
        {
            JavaKind preciseKind = determinePreciseArrayElementType(array, kind);
            return StampFactory.forKind(preciseKind);
        }
    }

    private static JavaKind determinePreciseArrayElementType(ValueNode array, JavaKind kind)
    {
        if (kind == JavaKind.Byte)
        {
            ResolvedJavaType javaType = ((ObjectStamp) array.stamp(NodeView.DEFAULT)).type();
            if (javaType != null && javaType.isArray() && javaType.getComponentType() != null && javaType.getComponentType().getJavaKind() == JavaKind.Boolean)
            {
                return JavaKind.Boolean;
            }
        }
        return kind;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(stamp.improveWith(createStamp(graph().getAssumptions(), array(), elementKind())));
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(array());
        if (alias instanceof VirtualObjectNode)
        {
            VirtualArrayNode virtual = (VirtualArrayNode) alias;
            ValueNode indexValue = tool.getAlias(index());
            int idx = indexValue.isConstant() ? indexValue.asJavaConstant().asInt() : -1;
            if (idx >= 0 && idx < virtual.entryCount())
            {
                ValueNode entry = tool.getEntry(virtual, idx);
                if (stamp.isCompatible(entry.stamp(NodeView.DEFAULT)))
                {
                    tool.replaceWith(entry);
                }
            }
        }
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        ValueNode constant = tryConstantFold(array(), index(), tool.getMetaAccess(), tool.getConstantReflection());
        if (constant != null)
        {
            return constant;
        }
        return this;
    }

    private static ValueNode tryConstantFold(ValueNode array, ValueNode index, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection)
    {
        if (array.isConstant() && !array.isNullConstant() && index.isConstant())
        {
            JavaConstant arrayConstant = array.asJavaConstant();
            if (arrayConstant != null)
            {
                int stableDimension = ((ConstantNode) array).getStableDimension();
                if (stableDimension > 0)
                {
                    JavaConstant constant = constantReflection.readArrayElement(arrayConstant, index.asJavaConstant().asInt());
                    boolean isDefaultStable = ((ConstantNode) array).isDefaultStable();
                    if (constant != null && (isDefaultStable || !constant.isDefaultForKind()))
                    {
                        return ConstantNode.forConstant(constant, stableDimension - 1, isDefaultStable, metaAccess);
                    }
                }
            }
        }
        return null;
    }
}
