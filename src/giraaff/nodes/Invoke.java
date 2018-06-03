package giraaff.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.graph.Node;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.type.StampTool;

// @iface Invoke
public interface Invoke extends StateSplit, Lowerable, DeoptimizingNode.DeoptDuring, FixedNodeInterface, Invokable
{
    FixedNode next();

    void setNext(FixedNode __x);

    CallTargetNode callTarget();

    @Override
    int bci();

    Node predecessor();

    ValueNode classInit();

    void setClassInit(ValueNode __node);

    void intrinsify(Node __node);

    boolean useForInlining();

    void setUseForInlining(boolean __value);

    ///
    // True if this invocation is almost certainly polymorphic, false when in doubt.
    ///
    boolean isPolymorphic();

    void setPolymorphic(boolean __value);

    @Override
    default ResolvedJavaMethod getTargetMethod()
    {
        return callTarget() != null ? callTarget().targetMethod() : null;
    }

    ///
    // Returns the {@linkplain ResolvedJavaMethod method} from which this invoke is executed. This
    // is the caller method and in the case of inlining may be different from the method of the
    // graph this node is in.
    //
    // @return the method from which this invoke is executed.
    ///
    default ResolvedJavaMethod getContextMethod()
    {
        FrameState __state = stateAfter();
        if (__state == null)
        {
            __state = stateDuring();
        }
        return __state.getMethod();
    }

    ///
    // Returns the {@linkplain ResolvedJavaType type} from which this invoke is executed. This is
    // the declaring type of the caller method.
    //
    // @return the type from which this invoke is executed.
    ///
    default ResolvedJavaType getContextType()
    {
        ResolvedJavaMethod __contextMethod = getContextMethod();
        if (__contextMethod == null)
        {
            return null;
        }
        return __contextMethod.getDeclaringClass();
    }

    @Override
    default void computeStateDuring(FrameState __stateAfter)
    {
        FrameState __newStateDuring = __stateAfter.duplicateModifiedDuringCall(bci(), asNode().getStackKind());
        setStateDuring(__newStateDuring);
    }

    default ValueNode getReceiver()
    {
        return callTarget().arguments().get(0);
    }

    default ResolvedJavaType getReceiverType()
    {
        ResolvedJavaType __receiverType = StampTool.typeOrNull(getReceiver());
        if (__receiverType == null)
        {
            __receiverType = ((MethodCallTargetNode) callTarget()).targetMethod().getDeclaringClass();
        }
        return __receiverType;
    }

    default InvokeKind getInvokeKind()
    {
        return callTarget().invokeKind();
    }
}
