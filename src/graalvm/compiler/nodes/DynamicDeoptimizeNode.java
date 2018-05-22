package graalvm.compiler.nodes;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class DynamicDeoptimizeNode extends AbstractDeoptimizeNode implements LIRLowerable, Lowerable, Canonicalizable
{
    public static final NodeClass<DynamicDeoptimizeNode> TYPE = NodeClass.create(DynamicDeoptimizeNode.class);
    @Input ValueNode actionAndReason;
    @Input ValueNode speculation;

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
    public void generate(NodeLIRBuilderTool generator)
    {
        generator.getLIRGeneratorTool().emitDeoptimize(generator.operand(actionAndReason), generator.operand(speculation), generator.state(this));
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (actionAndReason.isConstant() && speculation.isConstant())
        {
            JavaConstant constant = actionAndReason.asJavaConstant();
            JavaConstant speculationConstant = speculation.asJavaConstant();
            DeoptimizeNode newDeopt = new DeoptimizeNode(tool.getMetaAccess().decodeDeoptAction(constant), tool.getMetaAccess().decodeDeoptReason(constant), tool.getMetaAccess().decodeDebugId(constant), speculationConstant, stateBefore());
            return newDeopt;
        }
        return this;
    }
}
