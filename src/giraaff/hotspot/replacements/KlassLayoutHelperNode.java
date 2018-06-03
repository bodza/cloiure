package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

///
// Read {@code Klass::_layout_helper} and incorporate any useful stamp information based on any type
// information in {@code klass}.
///
// @class KlassLayoutHelperNode
public final class KlassLayoutHelperNode extends FloatingNode implements Canonicalizable, Lowerable
{
    // @def
    public static final NodeClass<KlassLayoutHelperNode> TYPE = NodeClass.create(KlassLayoutHelperNode.class);

    @Input
    // @field
    protected ValueNode ___klass;

    // @cons
    public KlassLayoutHelperNode(ValueNode __klass)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.___klass = __klass;
    }

    public static ValueNode create(ValueNode __klass, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess)
    {
        Stamp __stamp = StampFactory.forKind(JavaKind.Int);
        return canonical(null, __klass, __stamp, __constantReflection, __metaAccess);
    }

    @SuppressWarnings("unused")
    public static boolean intrinsify(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode __klass)
    {
        ValueNode __valueNode = create(__klass, __b.getConstantReflection(), __b.getMetaAccess());
        __b.push(JavaKind.Int, __b.append(__valueNode));
        return true;
    }

    @Override
    public boolean inferStamp()
    {
        if (this.___klass instanceof LoadHubNode)
        {
            LoadHubNode __hub = (LoadHubNode) this.___klass;
            Stamp __hubStamp = __hub.getValue().stamp(NodeView.DEFAULT);
            if (__hubStamp instanceof ObjectStamp)
            {
                ObjectStamp __objectStamp = (ObjectStamp) __hubStamp;
                ResolvedJavaType __type = __objectStamp.type();
                if (__type != null && !__type.isJavaLangObject())
                {
                    if (!__type.isArray() && !__type.isInterface())
                    {
                        // Definitely some form of instance type.
                        return updateStamp(StampFactory.forInteger(JavaKind.Int, HotSpotRuntime.klassLayoutHelperNeutralValue, Integer.MAX_VALUE));
                    }
                    if (__type.isArray())
                    {
                        return updateStamp(StampFactory.forInteger(JavaKind.Int, Integer.MIN_VALUE, HotSpotRuntime.klassLayoutHelperNeutralValue - 1));
                    }
                }
            }
        }
        return false;
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
            return canonical(this, this.___klass, stamp(NodeView.DEFAULT), __tool.getConstantReflection(), __tool.getMetaAccess());
        }
    }

    private static ValueNode canonical(KlassLayoutHelperNode __klassLayoutHelperNode, ValueNode __klass, Stamp __stamp, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess)
    {
        KlassLayoutHelperNode __self = __klassLayoutHelperNode;
        if (__klass.isConstant())
        {
            if (!__klass.asConstant().isDefaultForKind())
            {
                Constant __constant = __stamp.readConstant(__constantReflection.getMemoryAccessProvider(), __klass.asConstant(), HotSpotRuntime.klassLayoutHelperOffset);
                return ConstantNode.forConstant(__stamp, __constant, __metaAccess);
            }
        }
        if (__klass instanceof LoadHubNode)
        {
            LoadHubNode __hub = (LoadHubNode) __klass;
            Stamp __hubStamp = __hub.getValue().stamp(NodeView.DEFAULT);
            if (__hubStamp instanceof ObjectStamp)
            {
                ObjectStamp __ostamp = (ObjectStamp) __hubStamp;
                HotSpotResolvedObjectType __type = (HotSpotResolvedObjectType) __ostamp.type();
                if (__type != null && __type.isArray() && !__type.getComponentType().isPrimitive())
                {
                    // The layout for all object arrays is the same.
                    Constant __constant = __stamp.readConstant(__constantReflection.getMemoryAccessProvider(), __type.klass(), HotSpotRuntime.klassLayoutHelperOffset);
                    return ConstantNode.forConstant(__stamp, __constant, __metaAccess);
                }
            }
        }
        if (__self == null)
        {
            __self = new KlassLayoutHelperNode(__klass);
        }
        return __self;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public ValueNode getHub()
    {
        return this.___klass;
    }
}
