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
    public static final NodeClass<ClassGetHubNode> TYPE = NodeClass.create(ClassGetHubNode.class);

    @Input protected ValueNode clazz;

    // @cons
    public ClassGetHubNode(ValueNode clazz)
    {
        super(TYPE, KlassPointerStamp.klass());
        this.clazz = clazz;
    }

    public static ValueNode create(ValueNode clazz, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, boolean allUsagesAvailable)
    {
        return canonical(null, metaAccess, constantReflection, allUsagesAvailable, KlassPointerStamp.klass(), clazz);
    }

    @SuppressWarnings("unused")
    public static boolean intrinsify(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode clazz)
    {
        ValueNode clazzValue = create(clazz, b.getMetaAccess(), b.getConstantReflection(), false);
        b.push(JavaKind.Object, b.append(clazzValue));
        return true;
    }

    public static ValueNode canonical(ClassGetHubNode classGetHubNode, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, boolean allUsagesAvailable, Stamp stamp, ValueNode clazz)
    {
        ClassGetHubNode self = classGetHubNode;
        if (allUsagesAvailable && self != null && self.hasNoUsages())
        {
            return null;
        }
        else
        {
            if (clazz.isConstant())
            {
                if (metaAccess != null)
                {
                    ResolvedJavaType exactType = constantReflection.asJavaType(clazz.asJavaConstant());
                    if (exactType.isPrimitive())
                    {
                        return ConstantNode.forConstant(stamp, JavaConstant.NULL_POINTER, metaAccess);
                    }
                    else
                    {
                        return ConstantNode.forConstant(stamp, constantReflection.asObjectHub(exactType), metaAccess);
                    }
                }
            }
            if (clazz instanceof GetClassNode)
            {
                GetClassNode getClass = (GetClassNode) clazz;
                return new LoadHubNode(KlassPointerStamp.klassNonNull(), getClass.getObject());
            }
            if (clazz instanceof HubGetClassNode)
            {
                // replace: _klass._java_mirror._klass -> _klass
                return ((HubGetClassNode) clazz).getHub();
            }
            if (self == null)
            {
                self = new ClassGetHubNode(clazz);
            }
            return self;
        }
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        return canonical(this, tool.getMetaAccess(), tool.getConstantReflection(), tool.allUsagesAvailable(), stamp(NodeView.DEFAULT), clazz);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
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
    public Constant convert(Constant c, ConstantReflectionProvider constantReflection)
    {
        ResolvedJavaType exactType = constantReflection.asJavaType(c);
        if (exactType.isPrimitive())
        {
            return JavaConstant.NULL_POINTER;
        }
        else
        {
            return constantReflection.asObjectHub(exactType);
        }
    }

    @Override
    public Constant reverse(Constant c, ConstantReflectionProvider constantReflection)
    {
        ResolvedJavaType objectType = constantReflection.asJavaType(c);
        return constantReflection.asJavaClass(objectType);
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
    public boolean preservesOrder(CanonicalCondition op, Constant value, ConstantReflectionProvider constantReflection)
    {
        ResolvedJavaType exactType = constantReflection.asJavaType(value);
        return !exactType.isPrimitive();
    }
}
