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
    public static final NodeClass<DynamicDeoptimizeNode> TYPE = NodeClass.create(DynamicDeoptimizeNode.class);

    @Input ValueNode actionAndReason;
    @Input ValueNode speculation;

    // @cons
    public DynamicDeoptimizeNode(ValueNode actionAndReason, ValueNode speculation)
    {
        super(TYPE, null);
        this.actionAndReason = actionAndReason;
        this.speculation = speculation;
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
    public ValueNode getActionAndReason(MetaAccessProvider metaAccess)
    {
        return getActionAndReason();
    }

    @Override
    public ValueNode getSpeculation(MetaAccessProvider metaAccess)
    {
        return getSpeculation();
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().emitDeoptimize(gen.operand(actionAndReason), gen.operand(speculation), gen.state(this));
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (actionAndReason.isConstant() && speculation.isConstant())
        {
            JavaConstant constant = actionAndReason.asJavaConstant();
            JavaConstant speculationConstant = speculation.asJavaConstant();
            return new DeoptimizeNode(tool.getMetaAccess().decodeDeoptAction(constant), tool.getMetaAccess().decodeDeoptReason(constant), tool.getMetaAccess().decodeDebugId(constant), speculationConstant, stateBefore());
        }
        return this;
    }
}
