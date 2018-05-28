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
public abstract class MacroStateSplitNode extends MacroNode implements StateSplit, MemoryCheckpoint.Single
{
    public static final NodeClass<MacroStateSplitNode> TYPE = NodeClass.create(MacroStateSplitNode.class);

    @OptionalInput(InputType.State) protected FrameState stateAfter;

    protected MacroStateSplitNode(NodeClass<? extends MacroNode> c, InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments)
    {
        super(c, invokeKind, targetMethod, bci, returnStamp, arguments);
    }

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        updateUsages(stateAfter, x);
        stateAfter = x;
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

    protected void replaceSnippetInvokes(StructuredGraph snippetGraph)
    {
        for (MethodCallTargetNode call : snippetGraph.getNodes(MethodCallTargetNode.TYPE))
        {
            Invoke invoke = call.invoke();
            if (!call.targetMethod().equals(getTargetMethod()))
            {
                throw new GraalError("unexpected invoke %s in snippet", getClass().getSimpleName());
            }
            // here we need to fix the bci of the invoke
            InvokeNode newInvoke = snippetGraph.add(new InvokeNode(invoke.callTarget(), bci()));
            newInvoke.setStateAfter(invoke.stateAfter());
            snippetGraph.replaceFixedWithFixed((InvokeNode) invoke.asNode(), newInvoke);
        }
    }
}
