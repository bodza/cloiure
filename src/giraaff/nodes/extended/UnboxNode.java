package giraaff.nodes.extended;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.virtual.VirtualObjectNode;

// @class UnboxNode
public final class UnboxNode extends FixedWithNextNode implements Virtualizable, Lowerable, Canonicalizable.Unary<ValueNode>
{
    // @def
    public static final NodeClass<UnboxNode> TYPE = NodeClass.create(UnboxNode.class);

    @Node.Input
    // @field
    protected ValueNode ___value;
    // @field
    protected final JavaKind ___boxingKind;

    @Override
    public ValueNode getValue()
    {
        return this.___value;
    }

    // @cons UnboxNode
    public UnboxNode(ValueNode __value, JavaKind __boxingKind)
    {
        super(TYPE, StampFactory.forKind(__boxingKind.getStackKind()));
        this.___value = __value;
        this.___boxingKind = __boxingKind;
    }

    public static ValueNode create(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ValueNode __value, JavaKind __boxingKind)
    {
        ValueNode __synonym = findSynonym(__metaAccess, __constantReflection, __value, __boxingKind);
        if (__synonym != null)
        {
            return __synonym;
        }
        return new UnboxNode(__value, __boxingKind);
    }

    public JavaKind getBoxingKind()
    {
        return this.___boxingKind;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(getValue());
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
            ResolvedJavaType __objectType = __virtual.type();
            ResolvedJavaType __expectedType = __tool.getMetaAccessProvider().lookupJavaType(this.___boxingKind.toBoxedJavaClass());
            if (__objectType.equals(__expectedType))
            {
                __tool.replaceWithValue(__tool.getEntry(__virtual, 0));
            }
        }
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages() && StampTool.isPointerNonNull(__forValue))
        {
            return null;
        }
        ValueNode __synonym = findSynonym(__tool.getMetaAccess(), __tool.getConstantReflection(), __forValue, this.___boxingKind);
        if (__synonym != null)
        {
            return __synonym;
        }
        return this;
    }

    private static ValueNode findSynonym(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ValueNode __forValue, JavaKind __boxingKind)
    {
        if (__forValue.isConstant())
        {
            JavaConstant __constant = __forValue.asJavaConstant();
            JavaConstant __unboxed = __constantReflection.unboxPrimitive(__constant);
            if (__unboxed != null && __unboxed.getJavaKind() == __boxingKind)
            {
                return ConstantNode.forConstant(__unboxed, __metaAccess);
            }
        }
        else if (__forValue instanceof BoxNode)
        {
            BoxNode __box = (BoxNode) __forValue;
            if (__boxingKind == __box.getBoxingKind())
            {
                return __box.getValue();
            }
        }
        return null;
    }
}
