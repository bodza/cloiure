package giraaff.nodes.java;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodePredicates;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.spi.UncheckedInterfaceProvider;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.ConstantFoldUtil;
import giraaff.nodes.virtual.VirtualInstanceNode;
import giraaff.nodes.virtual.VirtualObjectNode;

///
// The {@code LoadFieldNode} represents a read of a static or instance field.
///
// @class LoadFieldNode
public final class LoadFieldNode extends AccessFieldNode implements Canonicalizable.Unary<ValueNode>, Virtualizable, UncheckedInterfaceProvider
{
    // @def
    public static final NodeClass<LoadFieldNode> TYPE = NodeClass.create(LoadFieldNode.class);

    // @field
    private final Stamp ___uncheckedStamp;

    // @cons LoadFieldNode
    protected LoadFieldNode(StampPair __stamp, ValueNode __object, ResolvedJavaField __field)
    {
        super(TYPE, __stamp.getTrustedStamp(), __object, __field);
        this.___uncheckedStamp = __stamp.getUncheckedStamp();
    }

    public static LoadFieldNode create(Assumptions __assumptions, ValueNode __object, ResolvedJavaField __field)
    {
        return new LoadFieldNode(StampFactory.forDeclaredType(__assumptions, __field.getType(), false), __object, __field);
    }

    public static ValueNode create(ConstantFieldProvider __constantFields, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Assumptions __assumptions, ValueNode __object, ResolvedJavaField __field, boolean __canonicalizeReads, boolean __allUsagesAvailable)
    {
        return canonical(null, StampFactory.forDeclaredType(__assumptions, __field.getType(), false), __object, __field, __constantFields, __constantReflection, __metaAccess, __canonicalizeReads, __allUsagesAvailable);
    }

    public static LoadFieldNode createOverrideStamp(StampPair __stamp, ValueNode __object, ResolvedJavaField __field)
    {
        return new LoadFieldNode(__stamp, __object, __field);
    }

    public static ValueNode createOverrideStamp(ConstantFieldProvider __constantFields, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, StampPair __stamp, ValueNode __object, ResolvedJavaField __field, boolean __canonicalizeReads, boolean __allUsagesAvailable)
    {
        return canonical(null, __stamp, __object, __field, __constantFields, __constantReflection, __metaAccess, __canonicalizeReads, __allUsagesAvailable);
    }

    @Override
    public ValueNode getValue()
    {
        return object();
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forObject)
    {
        NodeView __view = NodeView.from(__tool);
        if (__tool.allUsagesAvailable() && hasNoUsages() && !isVolatile() && (isStatic() || StampTool.isPointerNonNull(__forObject.stamp(__view))))
        {
            return null;
        }
        return canonical(this, StampPair.create(this.___stamp, this.___uncheckedStamp), __forObject, this.___field, __tool.getConstantFieldProvider(), __tool.getConstantReflection(), __tool.getMetaAccess(), __tool.canonicalizeReads(), __tool.allUsagesAvailable());
    }

    private static ValueNode canonical(LoadFieldNode __loadFieldNode, StampPair __stamp, ValueNode __forObject, ResolvedJavaField __field, ConstantFieldProvider __constantFields, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, boolean __canonicalizeReads, boolean __allUsagesAvailable)
    {
        LoadFieldNode __self = __loadFieldNode;
        if (__canonicalizeReads && __metaAccess != null)
        {
            ConstantNode __constant = asConstant(__constantFields, __constantReflection, __metaAccess, __forObject, __field);
            if (__constant != null)
            {
                return __constant;
            }
            if (__allUsagesAvailable)
            {
                PhiNode __phi = asPhi(__constantFields, __constantReflection, __metaAccess, __forObject, __field, __stamp.getTrustedStamp());
                if (__phi != null)
                {
                    return __phi;
                }
            }
        }
        if (__self != null && !__field.isStatic() && __forObject.isNullConstant())
        {
            return new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.NullCheckException);
        }
        if (__self == null)
        {
            __self = new LoadFieldNode(__stamp, __forObject, __field);
        }
        return __self;
    }

    ///
    // Gets a constant value for this load if possible.
    ///
    public ConstantNode asConstant(CanonicalizerTool __tool, ValueNode __forObject)
    {
        return asConstant(__tool.getConstantFieldProvider(), __tool.getConstantReflection(), __tool.getMetaAccess(), __forObject, this.___field);
    }

    private static ConstantNode asConstant(ConstantFieldProvider __constantFields, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, ValueNode __forObject, ResolvedJavaField __field)
    {
        if (__field.isStatic())
        {
            return ConstantFoldUtil.tryConstantFold(__constantFields, __constantReflection, __metaAccess, __field, null);
        }
        else if (__forObject.isConstant() && !__forObject.isNullConstant())
        {
            return ConstantFoldUtil.tryConstantFold(__constantFields, __constantReflection, __metaAccess, __field, __forObject.asJavaConstant());
        }
        return null;
    }

    public ConstantNode asConstant(CanonicalizerTool __tool, JavaConstant __constant)
    {
        return ConstantFoldUtil.tryConstantFold(__tool.getConstantFieldProvider(), __tool.getConstantReflection(), __tool.getMetaAccess(), field(), __constant);
    }

    private static PhiNode asPhi(ConstantFieldProvider __constantFields, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAcccess, ValueNode __forObject, ResolvedJavaField __field, Stamp __stamp)
    {
        if (!__field.isStatic() && __field.isFinal() && __forObject instanceof ValuePhiNode && ((ValuePhiNode) __forObject).values().filter(NodePredicates.isNotA(ConstantNode.class)).isEmpty())
        {
            PhiNode __phi = (PhiNode) __forObject;
            ConstantNode[] __constantNodes = new ConstantNode[__phi.valueCount()];
            for (int __i = 0; __i < __phi.valueCount(); __i++)
            {
                ConstantNode __constant = ConstantFoldUtil.tryConstantFold(__constantFields, __constantReflection, __metaAcccess, __field, __phi.valueAt(__i).asJavaConstant());
                if (__constant == null)
                {
                    return null;
                }
                __constantNodes[__i] = __constant;
            }
            return new ValuePhiNode(__stamp, __phi.merge(), __constantNodes);
        }
        return null;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(object());
        if (__alias instanceof VirtualObjectNode)
        {
            int __fieldIndex = ((VirtualInstanceNode) __alias).fieldIndex(field());
            if (__fieldIndex != -1)
            {
                ValueNode __entry = __tool.getEntry((VirtualObjectNode) __alias, __fieldIndex);
                if (this.___stamp.isCompatible(__entry.stamp(NodeView.DEFAULT)))
                {
                    __tool.replaceWith(__entry);
                }
            }
        }
    }

    @Override
    public Stamp uncheckedStamp()
    {
        return this.___uncheckedStamp;
    }

    public void setObject(ValueNode __newObject)
    {
        this.updateUsages(this.___object, __newObject);
        this.___object = __newObject;
    }
}
