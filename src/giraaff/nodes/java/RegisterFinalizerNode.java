package giraaff.nodes.java;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;

import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractStateSplit;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.ForeignCallDescriptors;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * This node is used to perform the finalizer registration at the end of the java.lang.Object
 * constructor.
 */
public final class RegisterFinalizerNode extends AbstractStateSplit implements Canonicalizable.Unary<ValueNode>, LIRLowerable, Virtualizable, DeoptimizingNode.DeoptAfter
{
    public static final NodeClass<RegisterFinalizerNode> TYPE = NodeClass.create(RegisterFinalizerNode.class);
    @OptionalInput(InputType.State) FrameState deoptState;
    @Input ValueNode value;

    public RegisterFinalizerNode(ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = value;
    }

    @Override
    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // Note that an unconditional call to the runtime routine is made without
        // checking that the object actually has a finalizer. This requires the
        // runtime routine to do the check.
        ForeignCallLinkage linkage = gen.getLIRGeneratorTool().getForeignCalls().lookupForeignCall(ForeignCallDescriptors.REGISTER_FINALIZER);
        gen.getLIRGeneratorTool().emitForeignCall(linkage, gen.state(this), gen.operand(getValue()));
    }

    /**
     * Determines if the compiler should emit code to test whether a given object has a finalizer
     * that must be registered with the runtime upon object initialization.
     */
    public static boolean mayHaveFinalizer(ValueNode object, Assumptions assumptions)
    {
        ObjectStamp objectStamp = (ObjectStamp) object.stamp(NodeView.DEFAULT);
        if (objectStamp.isExactType())
        {
            return objectStamp.type().hasFinalizer();
        }
        else if (objectStamp.type() != null)
        {
            AssumptionResult<Boolean> result = objectStamp.type().hasFinalizableSubclass();
            if (result.canRecordTo(assumptions))
            {
                result.recordTo(assumptions);
                return result.getResult();
            }
        }
        return true;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        NodeView view = NodeView.from(tool);
        if (!(forValue.stamp(view) instanceof ObjectStamp))
        {
            return this;
        }
        if (!mayHaveFinalizer(forValue, graph().getAssumptions()))
        {
            return null;
        }

        return this;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(getValue());
        if (alias instanceof VirtualObjectNode && !((VirtualObjectNode) alias).type().hasFinalizer())
        {
            tool.delete();
        }
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @NodeIntrinsic
    public static native void register(Object thisObj);
}
