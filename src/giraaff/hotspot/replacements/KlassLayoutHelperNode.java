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
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Read {@code Klass::_layout_helper} and incorporate any useful stamp information based on any type
 * information in {@code klass}.
 */
public final class KlassLayoutHelperNode extends FloatingNode implements Canonicalizable, Lowerable
{
    public static final NodeClass<KlassLayoutHelperNode> TYPE = NodeClass.create(KlassLayoutHelperNode.class);

    @Input protected ValueNode klass;

    public KlassLayoutHelperNode(ValueNode klass)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.klass = klass;
    }

    public static ValueNode create(ValueNode klass, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess)
    {
        Stamp stamp = StampFactory.forKind(JavaKind.Int);
        return canonical(null, klass, stamp, constantReflection, metaAccess);
    }

    @SuppressWarnings("unused")
    public static boolean intrinsify(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode klass)
    {
        ValueNode valueNode = create(klass, b.getConstantReflection(), b.getMetaAccess());
        b.push(JavaKind.Int, b.append(valueNode));
        return true;
    }

    @Override
    public boolean inferStamp()
    {
        if (klass instanceof LoadHubNode)
        {
            LoadHubNode hub = (LoadHubNode) klass;
            Stamp hubStamp = hub.getValue().stamp(NodeView.DEFAULT);
            if (hubStamp instanceof ObjectStamp)
            {
                ObjectStamp objectStamp = (ObjectStamp) hubStamp;
                ResolvedJavaType type = objectStamp.type();
                if (type != null && !type.isJavaLangObject())
                {
                    if (!type.isArray() && !type.isInterface())
                    {
                        // Definitely some form of instance type.
                        return updateStamp(StampFactory.forInteger(JavaKind.Int, GraalHotSpotVMConfig.klassLayoutHelperNeutralValue, Integer.MAX_VALUE));
                    }
                    if (type.isArray())
                    {
                        return updateStamp(StampFactory.forInteger(JavaKind.Int, Integer.MIN_VALUE, GraalHotSpotVMConfig.klassLayoutHelperNeutralValue - 1));
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        else
        {
            return canonical(this, klass, stamp(NodeView.DEFAULT), tool.getConstantReflection(), tool.getMetaAccess());
        }
    }

    private static ValueNode canonical(KlassLayoutHelperNode klassLayoutHelperNode, ValueNode klass, Stamp stamp, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess)
    {
        KlassLayoutHelperNode self = klassLayoutHelperNode;
        if (klass.isConstant())
        {
            if (!klass.asConstant().isDefaultForKind())
            {
                Constant constant = stamp.readConstant(constantReflection.getMemoryAccessProvider(), klass.asConstant(), GraalHotSpotVMConfig.klassLayoutHelperOffset);
                return ConstantNode.forConstant(stamp, constant, metaAccess);
            }
        }
        if (klass instanceof LoadHubNode)
        {
            LoadHubNode hub = (LoadHubNode) klass;
            Stamp hubStamp = hub.getValue().stamp(NodeView.DEFAULT);
            if (hubStamp instanceof ObjectStamp)
            {
                ObjectStamp ostamp = (ObjectStamp) hubStamp;
                HotSpotResolvedObjectType type = (HotSpotResolvedObjectType) ostamp.type();
                if (type != null && type.isArray() && !type.getComponentType().isPrimitive())
                {
                    // The layout for all object arrays is the same.
                    Constant constant = stamp.readConstant(constantReflection.getMemoryAccessProvider(), type.klass(), GraalHotSpotVMConfig.klassLayoutHelperOffset);
                    return ConstantNode.forConstant(stamp, constant, metaAccess);
                }
            }
        }
        if (self == null)
        {
            self = new KlassLayoutHelperNode(klass);
        }
        return self;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public ValueNode getHub()
    {
        return klass;
    }
}
