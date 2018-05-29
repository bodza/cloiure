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
    public static final int DEFAULT_DEBUG_ID = 0;

    public static final NodeClass<DeoptimizeNode> TYPE = NodeClass.create(DeoptimizeNode.class);

    protected DeoptimizationAction action;
    protected DeoptimizationReason reason;
    protected int debugId;
    protected final JavaConstant speculation;

    // @cons
    public DeoptimizeNode(DeoptimizationAction action, DeoptimizationReason reason)
    {
        this(action, reason, DEFAULT_DEBUG_ID, JavaConstant.NULL_POINTER, null);
    }

    // @cons
    public DeoptimizeNode(DeoptimizationAction action, DeoptimizationReason reason, JavaConstant speculation)
    {
        this(action, reason, DEFAULT_DEBUG_ID, speculation, null);
    }

    // @cons
    public DeoptimizeNode(DeoptimizationAction action, DeoptimizationReason reason, int debugId, JavaConstant speculation, FrameState stateBefore)
    {
        super(TYPE, stateBefore);
        this.action = action;
        this.reason = reason;
        this.debugId = debugId;
        this.speculation = speculation;
    }

    @Override
    public DeoptimizationAction getAction()
    {
        return action;
    }

    @Override
    public void setAction(DeoptimizationAction action)
    {
        this.action = action;
    }

    @Override
    public DeoptimizationReason getReason()
    {
        return reason;
    }

    @Override
    public void setReason(DeoptimizationReason reason)
    {
        this.reason = reason;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @SuppressWarnings("deprecation")
    public int getDebugId()
    {
        return debugId;
    }

    public void setDebugId(int debugId)
    {
        this.debugId = debugId;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRGeneratorTool tool = gen.getLIRGeneratorTool();
        Value actionAndReason = tool.emitJavaConstant(tool.getMetaAccess().encodeDeoptActionAndReason(action, reason, getDebugId()));
        Value speculationValue = tool.emitJavaConstant(speculation);
        gen.getLIRGeneratorTool().emitDeoptimize(actionAndReason, speculationValue, gen.state(this));
    }

    @Override
    public ValueNode getActionAndReason(MetaAccessProvider metaAccess)
    {
        return ConstantNode.forConstant(metaAccess.encodeDeoptActionAndReason(action, reason, getDebugId()), metaAccess, graph());
    }

    @Override
    public ValueNode getSpeculation(MetaAccessProvider metaAccess)
    {
        return ConstantNode.forConstant(speculation, metaAccess, graph());
    }

    @Override
    public JavaConstant getSpeculation()
    {
        return speculation;
    }

    @NodeIntrinsic
    public static native void deopt(@ConstantNodeParameter DeoptimizationAction action, @ConstantNodeParameter DeoptimizationReason reason);
}
