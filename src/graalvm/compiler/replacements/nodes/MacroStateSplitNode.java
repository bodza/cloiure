package graalvm.compiler.replacements.nodes;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * This is an extension of {@link MacroNode} that is a {@link StateSplit} and a
 * {@link MemoryCheckpoint}.
 */
@NodeInfo
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
            // Here we need to fix the bci of the invoke
            InvokeNode newInvoke = snippetGraph.add(new InvokeNode(invoke.callTarget(), bci()));
            newInvoke.setStateAfter(invoke.stateAfter());
            snippetGraph.replaceFixedWithFixed((InvokeNode) invoke.asNode(), newInvoke);
        }
    }
}
