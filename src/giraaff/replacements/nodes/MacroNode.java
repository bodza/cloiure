package giraaff.replacements.nodes;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.api.replacements.MethodSubstitution;
import giraaff.api.replacements.Snippet;
import giraaff.core.common.type.StampPair;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.Invokable;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.GuardsStage;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.FrameStateAssignmentPhase;
import giraaff.phases.common.GuardLoweringPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.tiers.PhaseContext;
import giraaff.util.GraalError;

/**
 * Macro nodes can be used to temporarily replace an invoke. They can, for example, be used to
 * implement constant folding for known JDK functions like {@link Class#isInterface()}.
 *
 * During lowering, multiple sources are queried in order to look for a replacement:
 *
 * <li>If {@link #getLoweredSnippetGraph(LoweringTool)} returns a non-null result, this graph is
 * used as a replacement.</li>
 * <li>If a {@link MethodSubstitution} for the target method is found, this substitution is used as
 * a replacement.</li>
 * <li>Otherwise, the macro node is replaced with an {@link InvokeNode}. Note that this is only
 * possible if the macro node is a {@link MacroStateSplitNode}.</li>
 */
public abstract class MacroNode extends FixedWithNextNode implements Lowerable, Invokable
{
    public static final NodeClass<MacroNode> TYPE = NodeClass.create(MacroNode.class);

    @Input protected NodeInputList<ValueNode> arguments;

    protected final int bci;
    protected final ResolvedJavaMethod targetMethod;
    protected final StampPair returnStamp;
    protected final InvokeKind invokeKind;

    protected MacroNode(NodeClass<? extends MacroNode> c, InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments)
    {
        super(c, returnStamp.getTrustedStamp());
        this.arguments = new NodeInputList<>(this, arguments);
        this.bci = bci;
        this.targetMethod = targetMethod;
        this.returnStamp = returnStamp;
        this.invokeKind = invokeKind;
    }

    public ValueNode getArgument(int i)
    {
        return arguments.get(i);
    }

    public int getArgumentCount()
    {
        return arguments.size();
    }

    public ValueNode[] toArgumentArray()
    {
        return arguments.toArray(new ValueNode[0]);
    }

    @Override
    public int bci()
    {
        return bci;
    }

    @Override
    public ResolvedJavaMethod getTargetMethod()
    {
        return targetMethod;
    }

    protected FrameState stateAfter()
    {
        return null;
    }

    @Override
    protected void afterClone(Node other)
    {
    }

    @Override
    public FixedNode asFixedNode()
    {
        return this;
    }

    /**
     * Gets a snippet to be used for lowering this macro node. The returned graph (if non-null) must
     * have been {@linkplain #lowerReplacement(StructuredGraph, LoweringTool) lowered}.
     */
    @SuppressWarnings("unused")
    protected StructuredGraph getLoweredSnippetGraph(LoweringTool tool)
    {
        return null;
    }

    /**
     * Applies {@linkplain LoweringPhase lowering} to a replacement graph.
     *
     * @param replacementGraph a replacement (i.e., snippet or method substitution) graph
     */
    protected StructuredGraph lowerReplacement(final StructuredGraph replacementGraph, LoweringTool tool)
    {
        final PhaseContext c = new PhaseContext(tool.getMetaAccess(), tool.getConstantReflection(), tool.getConstantFieldProvider(), tool.getLowerer(), tool.getReplacements(), tool.getStampProvider());
        if (!graph().hasValueProxies())
        {
            new RemoveValueProxyPhase().apply(replacementGraph);
        }
        GuardsStage guardsStage = graph().getGuardsStage();
        if (!guardsStage.allowsFloatingGuards())
        {
            new GuardLoweringPhase().apply(replacementGraph, null);
            if (guardsStage.areFrameStatesAtDeopts())
            {
                new FrameStateAssignmentPhase().apply(replacementGraph);
            }
        }
        new LoweringPhase(new CanonicalizerPhase(), tool.getLoweringStage()).apply(replacementGraph, c);
        return replacementGraph;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        StructuredGraph replacementGraph = getLoweredSnippetGraph(tool);

        InvokeNode invoke = replaceWithInvoke();

        if (replacementGraph != null)
        {
            // pull out the receiver null check, so that a replaced receiver can be lowered if necessary
            if (!targetMethod.isStatic())
            {
                ValueNode nonNullReceiver = InliningUtil.nonNullReceiver(invoke);
                if (nonNullReceiver instanceof Lowerable)
                {
                    ((Lowerable) nonNullReceiver).lower(tool);
                }
            }
            InliningUtil.inline(invoke, replacementGraph, false, targetMethod, "Replace with graph.", "LoweringPhase");
        }
        else
        {
            if (BytecodeFrame.isPlaceholderBci(invoke.bci()))
            {
                throw new GraalError("%s: cannot lower to invoke with placeholder BCI: %s", graph(), this);
            }

            if (invoke.stateAfter() == null)
            {
                ResolvedJavaMethod method = graph().method();
                if (method.getAnnotation(MethodSubstitution.class) != null || method.getAnnotation(Snippet.class) != null)
                {
                    // One cause for this is that a MacroNode is created for a method that no longer
                    // needs a MacroNode. For example, Class.getComponentType() only needs a MacroNode
                    // prior to JDK9 as it was given a non-native implementation in JDK9.
                    throw new GraalError("%s macro created for call to %s in %s must be lowerable to a snippet or intrinsic graph. Maybe a macro node is not needed for this method in the current JDK?", getClass().getSimpleName(), targetMethod.format("%h.%n(%p)"), graph());
                }
                throw new GraalError("%s: cannot lower to invoke without state: %s", graph(), this);
            }
            invoke.lower(tool);
        }
    }

    public InvokeNode replaceWithInvoke()
    {
        InvokeNode invoke = createInvoke();
        graph().replaceFixedWithFixed(this, invoke);
        return invoke;
    }

    protected InvokeNode createInvoke()
    {
        MethodCallTargetNode callTarget = graph().add(new MethodCallTargetNode(invokeKind, targetMethod, arguments.toArray(new ValueNode[arguments.size()]), returnStamp, null));
        InvokeNode invoke = graph().add(new InvokeNode(callTarget, bci));
        if (stateAfter() != null)
        {
            invoke.setStateAfter(stateAfter().duplicate());
            if (getStackKind() != JavaKind.Void)
            {
                invoke.stateAfter().replaceFirstInput(this, invoke);
            }
        }
        return invoke;
    }
}
