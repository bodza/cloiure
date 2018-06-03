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
 * This node is used to perform the finalizer registration at the end of the java.lang.Object constructor.
 */
// @class RegisterFinalizerNode
public final class RegisterFinalizerNode extends AbstractStateSplit implements Canonicalizable.Unary<ValueNode>, LIRLowerable, Virtualizable, DeoptimizingNode.DeoptAfter
{
    // @def
    public static final NodeClass<RegisterFinalizerNode> TYPE = NodeClass.create(RegisterFinalizerNode.class);

    @OptionalInput(InputType.State)
    // @field
    FrameState deoptState;
    @Input
    // @field
    ValueNode value;

    // @cons
    public RegisterFinalizerNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = __value;
    }

    @Override
    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // Note that an unconditional call to the runtime routine is made without
        // checking that the object actually has a finalizer. This requires the
        // runtime routine to do the check.
        ForeignCallLinkage __linkage = __gen.getLIRGeneratorTool().getForeignCalls().lookupForeignCall(ForeignCallDescriptors.REGISTER_FINALIZER);
        __gen.getLIRGeneratorTool().emitForeignCall(__linkage, __gen.state(this), __gen.operand(getValue()));
    }

    /**
     * Determines if the compiler should emit code to test whether a given object has a finalizer
     * that must be registered with the runtime upon object initialization.
     */
    public static boolean mayHaveFinalizer(ValueNode __object, Assumptions __assumptions)
    {
        ObjectStamp __objectStamp = (ObjectStamp) __object.stamp(NodeView.DEFAULT);
        if (__objectStamp.isExactType())
        {
            return __objectStamp.type().hasFinalizer();
        }
        else if (__objectStamp.type() != null)
        {
            AssumptionResult<Boolean> __result = __objectStamp.type().hasFinalizableSubclass();
            if (__result.canRecordTo(__assumptions))
            {
                __result.recordTo(__assumptions);
                return __result.getResult();
            }
        }
        return true;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        NodeView __view = NodeView.from(__tool);
        if (!(__forValue.stamp(__view) instanceof ObjectStamp))
        {
            return this;
        }
        if (!mayHaveFinalizer(__forValue, graph().getAssumptions()))
        {
            return null;
        }

        return this;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(getValue());
        if (__alias instanceof VirtualObjectNode && !((VirtualObjectNode) __alias).type().hasFinalizer())
        {
            __tool.delete();
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
