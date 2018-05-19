package graalvm.compiler.replacements.nodes;

import static jdk.vm.ci.code.BytecodeFrame.isPlaceholderBci;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_UNKNOWN;

import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.Invokable;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.GuardsStage;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.FrameStateAssignmentPhase;
import graalvm.compiler.phases.common.GuardLoweringPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.common.RemoveValueProxyPhase;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Macro nodes can be used to temporarily replace an invoke. They can, for example, be used to
 * implement constant folding for known JDK functions like {@link Class#isInterface()}.<br/>
 * <br/>
 * During lowering, multiple sources are queried in order to look for a replacement:
 * <ul>
 * <li>If {@link #getLoweredSnippetGraph(LoweringTool)} returns a non-null result, this graph is
 * used as a replacement.</li>
 * <li>If a {@link MethodSubstitution} for the target method is found, this substitution is used as
 * a replacement.</li>
 * <li>Otherwise, the macro node is replaced with an {@link InvokeNode}. Note that this is only
 * possible if the macro node is a {@link MacroStateSplitNode}.</li>
 * </ul>
 */
@NodeInfo(cycles = CYCLES_UNKNOWN,
          cyclesRationale = "If this node is not optimized away it will be lowered to a call, which we cannot estimate",
          size = SIZE_UNKNOWN,
          sizeRationale = "If this node is not optimized away it will be lowered to a call, which we cannot estimate")
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
            // Pull out the receiver null check so that a replaced
            // receiver can be lowered if necessary
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
            if (isPlaceholderBci(invoke.bci()))
            {
                throw new GraalError("%s: cannot lower to invoke with placeholder BCI: %s", graph(), this);
            }

            if (invoke.stateAfter() == null)
            {
                ResolvedJavaMethod method = graph().method();
                if (method.getAnnotation(MethodSubstitution.class) != null || method.getAnnotation(Snippet.class) != null)
                {
                    // One cause for this is that a MacroNode is created for a method that
                    // no longer needs a MacroNode. For example, Class.getComponentType()
                    // only needs a MacroNode prior to JDK9 as it was given a non-native
                    // implementation in JDK9.
                    throw new GraalError("%s macro created for call to %s in %s must be lowerable to a snippet or intrinsic graph. " + "Maybe a macro node is not needed for this method in the current JDK?", getClass().getSimpleName(), targetMethod.format("%h.%n(%p)"), graph());
                }
                throw new GraalError("%s: cannot lower to invoke without state: %s", graph(), this);
            }
            invoke.lower(tool);
        }
    }

    @SuppressWarnings("try")
    public InvokeNode replaceWithInvoke()
    {
        try (DebugCloseable context = withNodeSourcePosition())
        {
            InvokeNode invoke = createInvoke();
            graph().replaceFixedWithFixed(this, invoke);
            return invoke;
        }
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
