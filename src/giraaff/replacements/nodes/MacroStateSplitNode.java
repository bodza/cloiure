package giraaff.replacements.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.FrameState;
import giraaff.nodes.Invoke;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.util.GraalError;

/**
 * This is an extension of {@link MacroNode} that is a {@link StateSplit} and a
 * {@link MemoryCheckpoint}.
 */
// @class MacroStateSplitNode
public abstract class MacroStateSplitNode extends MacroNode implements StateSplit, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<MacroStateSplitNode> TYPE = NodeClass.create(MacroStateSplitNode.class);

    @OptionalInput(InputType.State)
    // @field
    protected FrameState stateAfter;

    // @cons
    protected MacroStateSplitNode(NodeClass<? extends MacroNode> __c, InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        super(__c, __invokeKind, __targetMethod, __bci, __returnStamp, __arguments);
    }

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(stateAfter, __x);
        stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    protected void replaceSnippetInvokes(StructuredGraph __snippetGraph)
    {
        for (MethodCallTargetNode __call : __snippetGraph.getNodes(MethodCallTargetNode.TYPE))
        {
            Invoke __invoke = __call.invoke();
            if (!__call.targetMethod().equals(getTargetMethod()))
            {
                throw new GraalError("unexpected invoke %s in snippet", getClass().getSimpleName());
            }
            // here we need to fix the bci of the invoke
            InvokeNode __newInvoke = __snippetGraph.add(new InvokeNode(__invoke.callTarget(), bci()));
            __newInvoke.setStateAfter(__invoke.stateAfter());
            __snippetGraph.replaceFixedWithFixed((InvokeNode) __invoke.asNode(), __newInvoke);
        }
    }
}
