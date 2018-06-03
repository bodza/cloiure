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

///
// Loads an object's class (i.e., this node can be created for {@code object.getClass()}).
///
// @class GetClassNode
public final class GetClassNode extends FloatingNode implements Lowerable, Canonicalizable, Virtualizable
{
    // @def
    public static final NodeClass<GetClassNode> TYPE = NodeClass.create(GetClassNode.class);

    @Input
    // @field
    ValueNode ___object;

    public ValueNode getObject()
    {
        return this.___object;
    }

    // @cons
    public GetClassNode(Stamp __stamp, ValueNode __object)
    {
        super(TYPE, __stamp);
        this.___object = __object;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public static ValueNode tryFold(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, NodeView __view, ValueNode __object)
    {
        if (__metaAccess != null && __object != null && __object.stamp(__view) instanceof ObjectStamp)
        {
            ObjectStamp __objectStamp = (ObjectStamp) __object.stamp(__view);
            if (__objectStamp.isExactType())
            {
                return ConstantNode.forConstant(__constantReflection.asJavaClass(__objectStamp.type()), __metaAccess);
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __folded = tryFold(__tool.getMetaAccess(), __tool.getConstantReflection(), __view, getObject());
        return __folded == null ? this : __folded;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(getObject());
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
            Constant __javaClass = __tool.getConstantReflectionProvider().asJavaClass(__virtual.type());
            __tool.replaceWithValue(ConstantNode.forConstant(stamp(NodeView.DEFAULT), __javaClass, __tool.getMetaAccessProvider(), graph()));
        }
    }
}
