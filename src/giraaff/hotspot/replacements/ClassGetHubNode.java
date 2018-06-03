package giraaff.hotspot.replacements;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConvertNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.GetClassNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Read {@code Class::_klass} to get the hub for a {@link java.lang.Class}. This node mostly exists
 * to replace {@code _klass._java_mirror._klass} with {@code _klass}. The constant folding could be
 * handled by
 * {@link ReadNode#canonicalizeRead(ValueNode, AddressNode, LocationIdentity, CanonicalizerTool)}.
 */
// @class ClassGetHubNode
public final class ClassGetHubNode extends FloatingNode implements Lowerable, Canonicalizable, ConvertNode
{
    // @def
    public static final NodeClass<ClassGetHubNode> TYPE = NodeClass.create(ClassGetHubNode.class);

    @Input
    // @field
    protected ValueNode clazz;

    // @cons
    public ClassGetHubNode(ValueNode __clazz)
    {
        super(TYPE, KlassPointerStamp.klass());
        this.clazz = __clazz;
    }

    public static ValueNode create(ValueNode __clazz, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, boolean __allUsagesAvailable)
    {
        return canonical(null, __metaAccess, __constantReflection, __allUsagesAvailable, KlassPointerStamp.klass(), __clazz);
    }

    @SuppressWarnings("unused")
    public static boolean intrinsify(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode __clazz)
    {
        ValueNode __clazzValue = create(__clazz, __b.getMetaAccess(), __b.getConstantReflection(), false);
        __b.push(JavaKind.Object, __b.append(__clazzValue));
        return true;
    }

    public static ValueNode canonical(ClassGetHubNode __classGetHubNode, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, boolean __allUsagesAvailable, Stamp __stamp, ValueNode __clazz)
    {
        ClassGetHubNode __self = __classGetHubNode;
        if (__allUsagesAvailable && __self != null && __self.hasNoUsages())
        {
            return null;
        }
        else
        {
            if (__clazz.isConstant())
            {
                if (__metaAccess != null)
                {
                    ResolvedJavaType __exactType = __constantReflection.asJavaType(__clazz.asJavaConstant());
                    if (__exactType.isPrimitive())
                    {
                        return ConstantNode.forConstant(__stamp, JavaConstant.NULL_POINTER, __metaAccess);
                    }
                    else
                    {
                        return ConstantNode.forConstant(__stamp, __constantReflection.asObjectHub(__exactType), __metaAccess);
                    }
                }
            }
            if (__clazz instanceof GetClassNode)
            {
                GetClassNode __getClass = (GetClassNode) __clazz;
                return new LoadHubNode(KlassPointerStamp.klassNonNull(), __getClass.getObject());
            }
            if (__clazz instanceof HubGetClassNode)
            {
                // replace: _klass._java_mirror._klass -> _klass
                return ((HubGetClassNode) __clazz).getHub();
            }
            if (__self == null)
            {
                __self = new ClassGetHubNode(__clazz);
            }
            return __self;
        }
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        return canonical(this, __tool.getMetaAccess(), __tool.getConstantReflection(), __tool.allUsagesAvailable(), stamp(NodeView.DEFAULT), clazz);
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @NodeIntrinsic
    public static native KlassPointer readClass(Class<?> clazzNonNull);

    @NodeIntrinsic(PiNode.class)
    public static native KlassPointer piCastNonNull(Object object, GuardingNode anchor);

    @Override
    public ValueNode getValue()
    {
        return clazz;
    }

    @Override
    public Constant convert(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        ResolvedJavaType __exactType = __constantReflection.asJavaType(__c);
        if (__exactType.isPrimitive())
        {
            return JavaConstant.NULL_POINTER;
        }
        else
        {
            return __constantReflection.asObjectHub(__exactType);
        }
    }

    @Override
    public Constant reverse(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        ResolvedJavaType __objectType = __constantReflection.asJavaType(__c);
        return __constantReflection.asJavaClass(__objectType);
    }

    @Override
    public boolean isLossless()
    {
        return false;
    }

    /**
     * There is more than one {@link java.lang.Class} value that has a NULL hub.
     */
    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return false;
    }

    @Override
    public boolean preservesOrder(CanonicalCondition __op, Constant __value, ConstantReflectionProvider __constantReflection)
    {
        ResolvedJavaType __exactType = __constantReflection.asJavaType(__value);
        return !__exactType.isPrimitive();
    }
}
