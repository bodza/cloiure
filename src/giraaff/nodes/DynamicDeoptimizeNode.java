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

    @Node.Input
    // @field
    ValueNode ___actionAndReason;
    @Node.Input
    // @field
    ValueNode ___speculation;

    // @cons DynamicDeoptimizeNode
    public DynamicDeoptimizeNode(ValueNode __actionAndReason, ValueNode __speculation)
    {
        super(TYPE, null);
        this.___actionAndReason = __actionAndReason;
        this.___speculation = __speculation;
    }

    public ValueNode getActionAndReason()
    {
        return this.___actionAndReason;
    }

    public ValueNode getSpeculation()
    {
        return this.___speculation;
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
        __gen.getLIRGeneratorTool().emitDeoptimize(__gen.operand(this.___actionAndReason), __gen.operand(this.___speculation), __gen.state(this));
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (this.___actionAndReason.isConstant() && this.___speculation.isConstant())
        {
            JavaConstant __constant = this.___actionAndReason.asJavaConstant();
            JavaConstant __speculationConstant = this.___speculation.asJavaConstant();
            return new DeoptimizeNode(__tool.getMetaAccess().decodeDeoptAction(__constant), __tool.getMetaAccess().decodeDeoptReason(__constant), __tool.getMetaAccess().decodeDebugId(__constant), __speculationConstant, stateBefore());
        }
        return this;
    }
}
