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

/**
 * The {@code LoadFieldNode} represents a read of a static or instance field.
 */
// @class LoadFieldNode
public final class LoadFieldNode extends AccessFieldNode implements Canonicalizable.Unary<ValueNode>, Virtualizable, UncheckedInterfaceProvider
{
    public static final NodeClass<LoadFieldNode> TYPE = NodeClass.create(LoadFieldNode.class);

    private final Stamp uncheckedStamp;

    // @cons
    protected LoadFieldNode(StampPair stamp, ValueNode object, ResolvedJavaField field)
    {
        super(TYPE, stamp.getTrustedStamp(), object, field);
        this.uncheckedStamp = stamp.getUncheckedStamp();
    }

    public static LoadFieldNode create(Assumptions assumptions, ValueNode object, ResolvedJavaField field)
    {
        return new LoadFieldNode(StampFactory.forDeclaredType(assumptions, field.getType(), false), object, field);
    }

    public static ValueNode create(ConstantFieldProvider constantFields, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, Assumptions assumptions, ValueNode object, ResolvedJavaField field, boolean canonicalizeReads, boolean allUsagesAvailable)
    {
        return canonical(null, StampFactory.forDeclaredType(assumptions, field.getType(), false), object, field, constantFields, constantReflection, metaAccess, canonicalizeReads, allUsagesAvailable);
    }

    public static LoadFieldNode createOverrideStamp(StampPair stamp, ValueNode object, ResolvedJavaField field)
    {
        return new LoadFieldNode(stamp, object, field);
    }

    public static ValueNode createOverrideStamp(ConstantFieldProvider constantFields, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, StampPair stamp, ValueNode object, ResolvedJavaField field, boolean canonicalizeReads, boolean allUsagesAvailable)
    {
        return canonical(null, stamp, object, field, constantFields, constantReflection, metaAccess, canonicalizeReads, allUsagesAvailable);
    }

    @Override
    public ValueNode getValue()
    {
        return object();
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forObject)
    {
        NodeView view = NodeView.from(tool);
        if (tool.allUsagesAvailable() && hasNoUsages() && !isVolatile() && (isStatic() || StampTool.isPointerNonNull(forObject.stamp(view))))
        {
            return null;
        }
        return canonical(this, StampPair.create(stamp, uncheckedStamp), forObject, field, tool.getConstantFieldProvider(), tool.getConstantReflection(), tool.getMetaAccess(), tool.canonicalizeReads(), tool.allUsagesAvailable());
    }

    private static ValueNode canonical(LoadFieldNode loadFieldNode, StampPair stamp, ValueNode forObject, ResolvedJavaField field, ConstantFieldProvider constantFields, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, boolean canonicalizeReads, boolean allUsagesAvailable)
    {
        LoadFieldNode self = loadFieldNode;
        if (canonicalizeReads && metaAccess != null)
        {
            ConstantNode constant = asConstant(constantFields, constantReflection, metaAccess, forObject, field);
            if (constant != null)
            {
                return constant;
            }
            if (allUsagesAvailable)
            {
                PhiNode phi = asPhi(constantFields, constantReflection, metaAccess, forObject, field, stamp.getTrustedStamp());
                if (phi != null)
                {
                    return phi;
                }
            }
        }
        if (self != null && !field.isStatic() && forObject.isNullConstant())
        {
            return new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.NullCheckException);
        }
        if (self == null)
        {
            self = new LoadFieldNode(stamp, forObject, field);
        }
        return self;
    }

    /**
     * Gets a constant value for this load if possible.
     */
    public ConstantNode asConstant(CanonicalizerTool tool, ValueNode forObject)
    {
        return asConstant(tool.getConstantFieldProvider(), tool.getConstantReflection(), tool.getMetaAccess(), forObject, field);
    }

    private static ConstantNode asConstant(ConstantFieldProvider constantFields, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, ValueNode forObject, ResolvedJavaField field)
    {
        if (field.isStatic())
        {
            return ConstantFoldUtil.tryConstantFold(constantFields, constantReflection, metaAccess, field, null);
        }
        else if (forObject.isConstant() && !forObject.isNullConstant())
        {
            return ConstantFoldUtil.tryConstantFold(constantFields, constantReflection, metaAccess, field, forObject.asJavaConstant());
        }
        return null;
    }

    public ConstantNode asConstant(CanonicalizerTool tool, JavaConstant constant)
    {
        return ConstantFoldUtil.tryConstantFold(tool.getConstantFieldProvider(), tool.getConstantReflection(), tool.getMetaAccess(), field(), constant);
    }

    private static PhiNode asPhi(ConstantFieldProvider constantFields, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAcccess, ValueNode forObject, ResolvedJavaField field, Stamp stamp)
    {
        if (!field.isStatic() && field.isFinal() && forObject instanceof ValuePhiNode && ((ValuePhiNode) forObject).values().filter(NodePredicates.isNotA(ConstantNode.class)).isEmpty())
        {
            PhiNode phi = (PhiNode) forObject;
            ConstantNode[] constantNodes = new ConstantNode[phi.valueCount()];
            for (int i = 0; i < phi.valueCount(); i++)
            {
                ConstantNode constant = ConstantFoldUtil.tryConstantFold(constantFields, constantReflection, metaAcccess, field, phi.valueAt(i).asJavaConstant());
                if (constant == null)
                {
                    return null;
                }
                constantNodes[i] = constant;
            }
            return new ValuePhiNode(stamp, phi.merge(), constantNodes);
        }
        return null;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(object());
        if (alias instanceof VirtualObjectNode)
        {
            int fieldIndex = ((VirtualInstanceNode) alias).fieldIndex(field());
            if (fieldIndex != -1)
            {
                ValueNode entry = tool.getEntry((VirtualObjectNode) alias, fieldIndex);
                if (stamp.isCompatible(entry.stamp(NodeView.DEFAULT)))
                {
                    tool.replaceWith(entry);
                }
            }
        }
    }

    @Override
    public Stamp uncheckedStamp()
    {
        return uncheckedStamp;
    }

    public void setObject(ValueNode newObject)
    {
        this.updateUsages(object, newObject);
        this.object = newObject;
    }
}
