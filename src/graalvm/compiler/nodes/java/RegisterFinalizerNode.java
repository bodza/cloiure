package graalvm.compiler.nodes.java;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;

import graalvm.compiler.core.common.spi.ForeignCallLinkage;
import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.AbstractStateSplit;
import graalvm.compiler.nodes.DeoptimizingNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.ForeignCallDescriptors;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

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
