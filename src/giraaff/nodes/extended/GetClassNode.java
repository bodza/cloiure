package giraaff.nodes.extended;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * Loads an object's class (i.e., this node can be created for {@code object.getClass()}).
 */
public final class GetClassNode extends FloatingNode implements Lowerable, Canonicalizable, Virtualizable
{
    public static final NodeClass<GetClassNode> TYPE = NodeClass.create(GetClassNode.class);

    @Input ValueNode object;

    public ValueNode getObject()
    {
        return object;
    }

    public GetClassNode(Stamp stamp, ValueNode object)
    {
        super(TYPE, stamp);
        this.object = object;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public static ValueNode tryFold(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, NodeView view, ValueNode object)
    {
        if (metaAccess != null && object != null && object.stamp(view) instanceof ObjectStamp)
        {
            ObjectStamp objectStamp = (ObjectStamp) object.stamp(view);
            if (objectStamp.isExactType())
            {
                return ConstantNode.forConstant(constantReflection.asJavaClass(objectStamp.type()), metaAccess);
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool)
    {
        NodeView view = NodeView.from(tool);
        ValueNode folded = tryFold(tool.getMetaAccess(), tool.getConstantReflection(), view, getObject());
        return folded == null ? this : folded;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(getObject());
        if (alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            Constant javaClass = tool.getConstantReflectionProvider().asJavaClass(virtual.type());
            tool.replaceWithValue(ConstantNode.forConstant(stamp(NodeView.DEFAULT), javaClass, tool.getMetaAccessProvider(), graph()));
        }
    }
}
