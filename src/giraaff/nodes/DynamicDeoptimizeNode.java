package giraaff.nodes;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class DynamicDeoptimizeNode
public final class DynamicDeoptimizeNode extends AbstractDeoptimizeNode implements LIRLowerable, Lowerable, Canonicalizable
{
    // @def
    public static final NodeClass<DynamicDeoptimizeNode> TYPE = NodeClass.create(DynamicDeoptimizeNode.class);

    @Input
    // @field
    ValueNode actionAndReason;
    @Input
    // @field
    ValueNode speculation;

    // @cons
    public DynamicDeoptimizeNode(ValueNode __actionAndReason, ValueNode __speculation)
    {
        super(TYPE, null);
        this.actionAndReason = __actionAndReason;
        this.speculation = __speculation;
    }

    public ValueNode getActionAndReason()
    {
        return actionAndReason;
    }

    public ValueNode getSpeculation()
    {
        return speculation;
    }

    @Override
    public ValueNode getActionAndReason(MetaAccessProvider __metaAccess)
    {
        return getActionAndReason();
    }

    @Override
    public ValueNode getSpeculation(MetaAccessProvider __metaAccess)
    {
        return getSpeculation();
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitDeoptimize(__gen.operand(actionAndReason), __gen.operand(speculation), __gen.state(this));
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (actionAndReason.isConstant() && speculation.isConstant())
        {
            JavaConstant __constant = actionAndReason.asJavaConstant();
            JavaConstant __speculationConstant = speculation.asJavaConstant();
            return new DeoptimizeNode(__tool.getMetaAccess().decodeDeoptAction(__constant), __tool.getMetaAccess().decodeDeoptReason(__constant), __tool.getMetaAccess().decodeDebugId(__constant), __speculationConstant, stateBefore());
        }
        return this;
    }
}
