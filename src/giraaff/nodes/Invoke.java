package giraaff.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.graph.Node;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.type.StampTool;

public interface Invoke extends StateSplit, Lowerable, DeoptimizingNode.DeoptDuring, FixedNodeInterface, Invokable
{
    FixedNode next();

    void setNext(FixedNode x);

    CallTargetNode callTarget();

    @Override
    int bci();

    Node predecessor();

    ValueNode classInit();

    void setClassInit(ValueNode node);

    void intrinsify(Node node);

    boolean useForInlining();

    void setUseForInlining(boolean value);

    /**
     * True if this invocation is almost certainly polymorphic, false when in doubt.
     */
    boolean isPolymorphic();

    void setPolymorphic(boolean value);

    @Override
    default ResolvedJavaMethod getTargetMethod()
    {
        return callTarget() != null ? callTarget().targetMethod() : null;
    }

    /**
     * Returns the {@linkplain ResolvedJavaMethod method} from which this invoke is executed. This
     * is the caller method and in the case of inlining may be different from the method of the
     * graph this node is in.
     *
     * @return the method from which this invoke is executed.
     */
    default ResolvedJavaMethod getContextMethod()
    {
        FrameState state = stateAfter();
        if (state == null)
        {
            state = stateDuring();
        }
        return state.getMethod();
    }

    /**
     * Returns the {@linkplain ResolvedJavaType type} from which this invoke is executed. This is
     * the declaring type of the caller method.
     *
     * @return the type from which this invoke is executed.
     */
    default ResolvedJavaType getContextType()
    {
        ResolvedJavaMethod contextMethod = getContextMethod();
        if (contextMethod == null)
        {
            return null;
        }
        return contextMethod.getDeclaringClass();
    }

    @Override
    default void computeStateDuring(FrameState stateAfter)
    {
        FrameState newStateDuring = stateAfter.duplicateModifiedDuringCall(bci(), asNode().getStackKind());
        setStateDuring(newStateDuring);
    }

    default ValueNode getReceiver()
    {
        return callTarget().arguments().get(0);
    }

    default ResolvedJavaType getReceiverType()
    {
        ResolvedJavaType receiverType = StampTool.typeOrNull(getReceiver());
        if (receiverType == null)
        {
            receiverType = ((MethodCallTargetNode) callTarget()).targetMethod().getDeclaringClass();
        }
        return receiverType;
    }

    default InvokeKind getInvokeKind()
    {
        return callTarget().invokeKind();
    }
}
