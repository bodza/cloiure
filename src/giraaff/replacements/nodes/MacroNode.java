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
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.Invokable;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.StructuredGraph;
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

///
// Macro nodes can be used to temporarily replace an invoke. They can, for example, be used to
// implement constant folding for known JDK functions like {@link Class#isInterface()}.
//
// During lowering, multiple sources are queried in order to look for a replacement:
//
// <li>If {@link #getLoweredSnippetGraph(LoweringTool)} returns a non-null result, this graph is
// used as a replacement.</li>
// <li>If a {@link MethodSubstitution} for the target method is found, this substitution is used as
// a replacement.</li>
// <li>Otherwise, the macro node is replaced with an {@link InvokeNode}. Note that this is only
// possible if the macro node is a {@link MacroStateSplitNode}.</li>
///
// @class MacroNode
public abstract class MacroNode extends FixedWithNextNode implements Lowerable, Invokable
{
    // @def
    public static final NodeClass<MacroNode> TYPE = NodeClass.create(MacroNode.class);

    @Node.Input
    // @field
    protected NodeInputList<ValueNode> ___arguments;

    // @field
    protected final int ___bci;
    // @field
    protected final ResolvedJavaMethod ___targetMethod;
    // @field
    protected final StampPair ___returnStamp;
    // @field
    protected final CallTargetNode.InvokeKind ___invokeKind;

    // @cons MacroNode
    protected MacroNode(NodeClass<? extends MacroNode> __c, CallTargetNode.InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        super(__c, __returnStamp.getTrustedStamp());
        this.___arguments = new NodeInputList<>(this, __arguments);
        this.___bci = __bci;
        this.___targetMethod = __targetMethod;
        this.___returnStamp = __returnStamp;
        this.___invokeKind = __invokeKind;
    }

    public ValueNode getArgument(int __i)
    {
        return this.___arguments.get(__i);
    }

    public int getArgumentCount()
    {
        return this.___arguments.size();
    }

    public ValueNode[] toArgumentArray()
    {
        return this.___arguments.toArray(new ValueNode[0]);
    }

    @Override
    public int bci()
    {
        return this.___bci;
    }

    @Override
    public ResolvedJavaMethod getTargetMethod()
    {
        return this.___targetMethod;
    }

    protected FrameState stateAfter()
    {
        return null;
    }

    @Override
    protected void afterClone(Node __other)
    {
    }

    @Override
    public FixedNode asFixedNode()
    {
        return this;
    }

    ///
    // Gets a snippet to be used for lowering this macro node. The returned graph (if non-null) must
    // have been {@linkplain #lowerReplacement(StructuredGraph, LoweringTool) lowered}.
    ///
    @SuppressWarnings("unused")
    protected StructuredGraph getLoweredSnippetGraph(LoweringTool __tool)
    {
        return null;
    }

    ///
    // Applies {@linkplain LoweringPhase lowering} to a replacement graph.
    //
    // @param replacementGraph a replacement (i.e., snippet or method substitution) graph
    ///
    protected StructuredGraph lowerReplacement(final StructuredGraph __replacementGraph, LoweringTool __tool)
    {
        final PhaseContext __c = new PhaseContext(__tool.getMetaAccess(), __tool.getConstantReflection(), __tool.getConstantFieldProvider(), __tool.getLowerer(), __tool.getReplacements(), __tool.getStampProvider());
        if (!graph().hasValueProxies())
        {
            new RemoveValueProxyPhase().apply(__replacementGraph);
        }
        StructuredGraph.GuardsStage __guardsStage = graph().getGuardsStage();
        if (!__guardsStage.allowsFloatingGuards())
        {
            new GuardLoweringPhase().apply(__replacementGraph, null);
            if (__guardsStage.areFrameStatesAtDeopts())
            {
                new FrameStateAssignmentPhase().apply(__replacementGraph);
            }
        }
        new LoweringPhase(new CanonicalizerPhase(), __tool.getLoweringStage()).apply(__replacementGraph, __c);
        return __replacementGraph;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        StructuredGraph __replacementGraph = getLoweredSnippetGraph(__tool);

        InvokeNode __invoke = replaceWithInvoke();

        if (__replacementGraph != null)
        {
            // pull out the receiver null check, so that a replaced receiver can be lowered if necessary
            if (!this.___targetMethod.isStatic())
            {
                ValueNode __nonNullReceiver = InliningUtil.nonNullReceiver(__invoke);
                if (__nonNullReceiver instanceof Lowerable)
                {
                    ((Lowerable) __nonNullReceiver).lower(__tool);
                }
            }
            InliningUtil.inline(__invoke, __replacementGraph, false, this.___targetMethod, "Replace with graph.", "LoweringPhase");
        }
        else
        {
            if (BytecodeFrame.isPlaceholderBci(__invoke.bci()))
            {
                throw new GraalError("%s: cannot lower to invoke with placeholder BCI: %s", graph(), this);
            }

            if (__invoke.stateAfter() == null)
            {
                ResolvedJavaMethod __method = graph().method();
                if (__method.getAnnotation(MethodSubstitution.class) != null || __method.getAnnotation(Snippet.class) != null)
                {
                    // One cause for this is that a MacroNode is created for a method that no longer
                    // needs a MacroNode. For example, Class.getComponentType() only needs a MacroNode
                    // prior to JDK9 as it was given a non-native implementation in JDK9.
                    throw new GraalError("%s macro created for call to %s in %s must be lowerable to a snippet or intrinsic graph. Maybe a macro node is not needed for this method in the current JDK?", getClass().getSimpleName(), this.___targetMethod.format("%h.%n(%p)"), graph());
                }
                throw new GraalError("%s: cannot lower to invoke without state: %s", graph(), this);
            }
            __invoke.lower(__tool);
        }
    }

    public InvokeNode replaceWithInvoke()
    {
        InvokeNode __invoke = createInvoke();
        graph().replaceFixedWithFixed(this, __invoke);
        return __invoke;
    }

    protected InvokeNode createInvoke()
    {
        MethodCallTargetNode __callTarget = graph().add(new MethodCallTargetNode(this.___invokeKind, this.___targetMethod, this.___arguments.toArray(new ValueNode[this.___arguments.size()]), this.___returnStamp, null));
        InvokeNode __invoke = graph().add(new InvokeNode(__callTarget, this.___bci));
        if (stateAfter() != null)
        {
            __invoke.setStateAfter(stateAfter().duplicate());
            if (getStackKind() != JavaKind.Void)
            {
                __invoke.stateAfter().replaceFirstInput(this, __invoke);
            }
        }
        return __invoke;
    }
}
