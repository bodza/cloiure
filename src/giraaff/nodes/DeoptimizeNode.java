package giraaff.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.Value;

import giraaff.graph.NodeClass;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class DeoptimizeNode
public final class DeoptimizeNode extends AbstractDeoptimizeNode implements Lowerable, LIRLowerable, StaticDeoptimizingNode
{
    // @def
    public static final int DEFAULT_DEBUG_ID = 0;

    // @def
    public static final NodeClass<DeoptimizeNode> TYPE = NodeClass.create(DeoptimizeNode.class);

    // @field
    protected DeoptimizationAction ___action;
    // @field
    protected DeoptimizationReason ___reason;
    // @field
    protected int ___debugId;
    // @field
    protected final JavaConstant ___speculation;

    // @cons
    public DeoptimizeNode(DeoptimizationAction __action, DeoptimizationReason __reason)
    {
        this(__action, __reason, DEFAULT_DEBUG_ID, JavaConstant.NULL_POINTER, null);
    }

    // @cons
    public DeoptimizeNode(DeoptimizationAction __action, DeoptimizationReason __reason, JavaConstant __speculation)
    {
        this(__action, __reason, DEFAULT_DEBUG_ID, __speculation, null);
    }

    // @cons
    public DeoptimizeNode(DeoptimizationAction __action, DeoptimizationReason __reason, int __debugId, JavaConstant __speculation, FrameState __stateBefore)
    {
        super(TYPE, __stateBefore);
        this.___action = __action;
        this.___reason = __reason;
        this.___debugId = __debugId;
        this.___speculation = __speculation;
    }

    @Override
    public DeoptimizationAction getAction()
    {
        return this.___action;
    }

    @Override
    public void setAction(DeoptimizationAction __action)
    {
        this.___action = __action;
    }

    @Override
    public DeoptimizationReason getReason()
    {
        return this.___reason;
    }

    @Override
    public void setReason(DeoptimizationReason __reason)
    {
        this.___reason = __reason;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @SuppressWarnings("deprecation")
    public int getDebugId()
    {
        return this.___debugId;
    }

    public void setDebugId(int __debugId)
    {
        this.___debugId = __debugId;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRGeneratorTool __tool = __gen.getLIRGeneratorTool();
        Value __actionAndReason = __tool.emitJavaConstant(__tool.getMetaAccess().encodeDeoptActionAndReason(this.___action, this.___reason, getDebugId()));
        Value __speculationValue = __tool.emitJavaConstant(this.___speculation);
        __gen.getLIRGeneratorTool().emitDeoptimize(__actionAndReason, __speculationValue, __gen.state(this));
    }

    @Override
    public ValueNode getActionAndReason(MetaAccessProvider __metaAccess)
    {
        return ConstantNode.forConstant(__metaAccess.encodeDeoptActionAndReason(this.___action, this.___reason, getDebugId()), __metaAccess, graph());
    }

    @Override
    public ValueNode getSpeculation(MetaAccessProvider __metaAccess)
    {
        return ConstantNode.forConstant(this.___speculation, __metaAccess, graph());
    }

    @Override
    public JavaConstant getSpeculation()
    {
        return this.___speculation;
    }

    @NodeIntrinsic
    public static native void deopt(@ConstantNodeParameter DeoptimizationAction __action, @ConstantNodeParameter DeoptimizationReason __reason);
}
