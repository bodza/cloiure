package giraaff.hotspot.replacements;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConvertNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Read {@code Klass::_java_mirror} and incorporate non-null type information into stamp. This is
 * also used by {@link ClassGetHubNode} to eliminate chains of {@code klass._java_mirror._klass}.
 */
// @class HubGetClassNode
public final class HubGetClassNode extends FloatingNode implements Lowerable, Canonicalizable, ConvertNode
{
    // @def
    public static final NodeClass<HubGetClassNode> TYPE = NodeClass.create(HubGetClassNode.class);

    @Input
    // @field
    protected ValueNode hub;

    // @cons
    public HubGetClassNode(@InjectedNodeParameter MetaAccessProvider __metaAccess, ValueNode __hub)
    {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createWithoutAssumptions(__metaAccess.lookupJavaType(Class.class))));
        this.hub = __hub;
    }

    public ValueNode getHub()
    {
        return hub;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        else
        {
            MetaAccessProvider __metaAccess = __tool.getMetaAccess();
            if (__metaAccess != null && hub.isConstant())
            {
                ResolvedJavaType __exactType = __tool.getConstantReflection().asJavaType(hub.asConstant());
                if (__exactType != null)
                {
                    return ConstantNode.forConstant(__tool.getConstantReflection().asJavaClass(__exactType), __metaAccess);
                }
            }
            return this;
        }
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @NodeIntrinsic
    public static native Class<?> readClass(KlassPointer hub);

    @Override
    public ValueNode getValue()
    {
        return hub;
    }

    @Override
    public Constant convert(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        if (JavaConstant.NULL_POINTER.equals(__c))
        {
            return __c;
        }
        return __constantReflection.asJavaClass(__constantReflection.asJavaType(__c));
    }

    @Override
    public Constant reverse(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        if (JavaConstant.NULL_POINTER.equals(__c))
        {
            return __c;
        }
        ResolvedJavaType __type = __constantReflection.asJavaType(__c);
        if (__type.isPrimitive())
        {
            return JavaConstant.NULL_POINTER;
        }
        else
        {
            return __constantReflection.asObjectHub(__type);
        }
    }

    /**
     * Any concrete Klass* has a corresponding {@link java.lang.Class}.
     */
    @Override
    public boolean isLossless()
    {
        return true;
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return true;
    }
}
