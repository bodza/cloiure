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
    public static final NodeClass<LoadHubNode> TYPE = NodeClass.create(LoadHubNode.class);

    @Input ValueNode value;

    public ValueNode getValue()
    {
        return value;
    }

    private static Stamp hubStamp(StampProvider stampProvider, ValueNode value)
    {
        return stampProvider.createHubStamp(((ObjectStamp) value.stamp(NodeView.DEFAULT)));
    }

    public static ValueNode create(ValueNode value, StampProvider stampProvider, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection)
    {
        Stamp stamp = hubStamp(stampProvider, value);
        ValueNode synonym = findSynonym(value, stamp, metaAccess, constantReflection);
        if (synonym != null)
        {
            return synonym;
        }
        return new LoadHubNode(stamp, value);
    }

    // @cons
    public LoadHubNode(@InjectedNodeParameter StampProvider stampProvider, ValueNode value)
    {
        this(hubStamp(stampProvider, value), value);
    }

    // @cons
    public LoadHubNode(Stamp stamp, ValueNode value)
    {
        super(TYPE, stamp);
        this.value = value;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool)
    {
        NodeView view = NodeView.from(tool);
        MetaAccessProvider metaAccess = tool.getMetaAccess();
        ValueNode curValue = getValue();
        ValueNode newNode = findSynonym(curValue, stamp(view), metaAccess, tool.getConstantReflection());
        if (newNode != null)
        {
            return newNode;
        }
        return this;
    }

    public static ValueNode findSynonym(ValueNode curValue, Stamp stamp, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection)
    {
        TypeReference type = StampTool.typeReferenceOrNull(curValue);
        if (type != null && type.isExact())
        {
            return ConstantNode.forConstant(stamp, constantReflection.asObjectHub(type.getType()), metaAccess);
        }
        return null;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(getValue());
        TypeReference type = StampTool.typeReferenceOrNull(alias);
        if (type != null && type.isExact())
        {
            tool.replaceWithValue(ConstantNode.forConstant(stamp(NodeView.DEFAULT), tool.getConstantReflectionProvider().asObjectHub(type.getType()), tool.getMetaAccessProvider(), graph()));
        }
    }
}
