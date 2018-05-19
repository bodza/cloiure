package graalvm.compiler.nodes.extended;

import static graalvm.compiler.core.common.GraalOptions.GeneratePIC;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.StampProvider;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.type.StampTool;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

/**
 * Loads an object's hub. The object is not null-checked by this operation.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
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

    public LoadHubNode(@InjectedNodeParameter StampProvider stampProvider, ValueNode value)
    {
        this(hubStamp(stampProvider, value), value);
    }

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
        if (!GeneratePIC.getValue(tool.getOptions()))
        {
            NodeView view = NodeView.from(tool);
            MetaAccessProvider metaAccess = tool.getMetaAccess();
            ValueNode curValue = getValue();
            ValueNode newNode = findSynonym(curValue, stamp(view), metaAccess, tool.getConstantReflection());
            if (newNode != null)
            {
                return newNode;
            }
        }
        return this;
    }

    public static ValueNode findSynonym(ValueNode curValue, Stamp stamp, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection)
    {
        if (!GeneratePIC.getValue(curValue.getOptions()))
        {
            TypeReference type = StampTool.typeReferenceOrNull(curValue);
            if (type != null && type.isExact())
            {
                return ConstantNode.forConstant(stamp, constantReflection.asObjectHub(type.getType()), metaAccess);
            }
        }
        return null;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        if (!GeneratePIC.getValue(tool.getOptions()))
        {
            ValueNode alias = tool.getAlias(getValue());
            TypeReference type = StampTool.typeReferenceOrNull(alias);
            if (type != null && type.isExact())
            {
                tool.replaceWithValue(ConstantNode.forConstant(stamp(NodeView.DEFAULT), tool.getConstantReflectionProvider().asObjectHub(type.getType()), tool.getMetaAccessProvider(), graph()));
            }
        }
    }
}
