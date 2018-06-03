package giraaff.nodes.extended;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.StampProvider;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;

/**
 * Loads an object's hub. The object is not null-checked by this operation.
 */
// @class LoadHubNode
public final class LoadHubNode extends FloatingNode implements Lowerable, Canonicalizable, Virtualizable
{
    // @def
    public static final NodeClass<LoadHubNode> TYPE = NodeClass.create(LoadHubNode.class);

    @Input
    // @field
    ValueNode value;

    public ValueNode getValue()
    {
        return value;
    }

    private static Stamp hubStamp(StampProvider __stampProvider, ValueNode __value)
    {
        return __stampProvider.createHubStamp(((ObjectStamp) __value.stamp(NodeView.DEFAULT)));
    }

    public static ValueNode create(ValueNode __value, StampProvider __stampProvider, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection)
    {
        Stamp __stamp = hubStamp(__stampProvider, __value);
        ValueNode __synonym = findSynonym(__value, __stamp, __metaAccess, __constantReflection);
        if (__synonym != null)
        {
            return __synonym;
        }
        return new LoadHubNode(__stamp, __value);
    }

    // @cons
    public LoadHubNode(@InjectedNodeParameter StampProvider __stampProvider, ValueNode __value)
    {
        this(hubStamp(__stampProvider, __value), __value);
    }

    // @cons
    public LoadHubNode(Stamp __stamp, ValueNode __value)
    {
        super(TYPE, __stamp);
        this.value = __value;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        MetaAccessProvider __metaAccess = __tool.getMetaAccess();
        ValueNode __curValue = getValue();
        ValueNode __newNode = findSynonym(__curValue, stamp(__view), __metaAccess, __tool.getConstantReflection());
        if (__newNode != null)
        {
            return __newNode;
        }
        return this;
    }

    public static ValueNode findSynonym(ValueNode __curValue, Stamp __stamp, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection)
    {
        TypeReference __type = StampTool.typeReferenceOrNull(__curValue);
        if (__type != null && __type.isExact())
        {
            return ConstantNode.forConstant(__stamp, __constantReflection.asObjectHub(__type.getType()), __metaAccess);
        }
        return null;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(getValue());
        TypeReference __type = StampTool.typeReferenceOrNull(__alias);
        if (__type != null && __type.isExact())
        {
            __tool.replaceWithValue(ConstantNode.forConstant(stamp(NodeView.DEFAULT), __tool.getConstantReflectionProvider().asObjectHub(__type.getType()), __tool.getMetaAccessProvider(), graph()));
        }
    }
}
