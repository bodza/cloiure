package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.type.StampTool;

/**
 * Used by a {@link GraphBuilderPlugin} to interface with an object that parses the bytecode of a
 * single {@linkplain #getMethod() method} as part of building a {@linkplain #getGraph() graph}.
 */
// @iface GraphBuilderContext
public interface GraphBuilderContext extends GraphBuilderTool
{
    /**
     * Pushes a given value to the frame state stack using an explicit kind. This should be used
     * when {@code value.getJavaKind()} is different from the kind that the bytecode instruction
     * currently being parsed pushes to the stack.
     *
     * @param kind the kind to use when type checking this operation
     * @param value the value to push to the stack. The value must already have been
     *            {@linkplain #append(ValueNode) appended}.
     */
    void push(JavaKind kind, ValueNode value);

    /**
     * Adds a node to the graph. If the node is in the graph, returns immediately. If the node is a
     * {@link StateSplit} with a null {@linkplain StateSplit#stateAfter() frame state}, the frame
     * state is initialized.
     *
     * @param value the value to add to the graph and push to the stack. The
     *            {@code value.getJavaKind()} kind is used when type checking this operation.
     * @return a node equivalent to {@code value} in the graph
     */
    default <T extends ValueNode> T add(T value)
    {
        if (value.graph() != null)
        {
            return value;
        }
        T equivalentValue = append(value);
        if (equivalentValue instanceof StateSplit)
        {
            StateSplit stateSplit = (StateSplit) equivalentValue;
            if (stateSplit.stateAfter() == null && stateSplit.hasSideEffect())
            {
                setStateAfter(stateSplit);
            }
        }
        return equivalentValue;
    }

    /**
     * Adds a node and its inputs to the graph. If the node is in the graph, returns immediately. If
     * the node is a {@link StateSplit} with a null {@linkplain StateSplit#stateAfter() frame state}
     * , the frame state is initialized.
     *
     * @param value the value to add to the graph and push to the stack. The
     *            {@code value.getJavaKind()} kind is used when type checking this operation.
     * @return a node equivalent to {@code value} in the graph
     */
    default <T extends ValueNode> T addWithInputs(T value)
    {
        if (value.graph() != null)
        {
            return value;
        }
        T equivalentValue = append(value);
        if (equivalentValue instanceof StateSplit)
        {
            StateSplit stateSplit = (StateSplit) equivalentValue;
            if (stateSplit.stateAfter() == null && stateSplit.hasSideEffect())
            {
                setStateAfter(stateSplit);
            }
        }
        return equivalentValue;
    }

    default ValueNode addNonNullCast(ValueNode value)
    {
        AbstractPointerStamp valueStamp = (AbstractPointerStamp) value.stamp(NodeView.DEFAULT);
        if (valueStamp.nonNull())
        {
            return value;
        }
        else
        {
            LogicNode isNull = add(IsNullNode.create(value));
            FixedGuardNode fixedGuard = add(new FixedGuardNode(isNull, DeoptimizationReason.NullCheckException, DeoptimizationAction.None, true));
            Stamp newStamp = valueStamp.improveWith(StampFactory.objectNonNull());
            return add(PiNode.create(value, newStamp, fixedGuard));
        }
    }

    /**
     * Adds a node with a non-void kind to the graph, pushes it to the stack. If the returned node
     * is a {@link StateSplit} with a null {@linkplain StateSplit#stateAfter() frame state}, the
     * frame state is initialized.
     *
     * @param kind the kind to use when type checking this operation
     * @param value the value to add to the graph and push to the stack
     * @return a node equivalent to {@code value} in the graph
     */
    default <T extends ValueNode> T addPush(JavaKind kind, T value)
    {
        T equivalentValue = value.graph() != null ? value : append(value);
        push(kind, equivalentValue);
        if (equivalentValue instanceof StateSplit)
        {
            StateSplit stateSplit = (StateSplit) equivalentValue;
            if (stateSplit.stateAfter() == null && stateSplit.hasSideEffect())
            {
                setStateAfter(stateSplit);
            }
        }
        return equivalentValue;
    }

    /**
     * Handles an invocation that a plugin determines can replace the original invocation (i.e., the
     * one for which the plugin was applied). This applies all standard graph builder processing to
     * the replaced invocation including applying any relevant plugins.
     *
     * @param invokeKind the kind of the replacement invocation
     * @param targetMethod the target of the replacement invocation
     * @param args the arguments to the replacement invocation
     * @param forceInlineEverything specifies if all invocations encountered in the scope of
     *            handling the replaced invoke are to be force inlined
     */
    void handleReplacedInvoke(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] args, boolean forceInlineEverything);

    void handleReplacedInvoke(CallTargetNode callTarget, JavaKind resultType);

    /**
     * Intrinsifies an invocation of a given method by inlining the bytecodes of a given
     * substitution method.
     *
     * @param bytecodeProvider used to get the bytecodes to parse for the substitution method
     * @param targetMethod the method being intrinsified
     * @param substitute the intrinsic implementation
     * @param receiver the receiver, or null for static methods
     * @param argsIncludingReceiver the arguments with which to inline the invocation
     *
     * @return whether the intrinsification was successful
     */
    boolean intrinsify(BytecodeProvider bytecodeProvider, ResolvedJavaMethod targetMethod, ResolvedJavaMethod substitute, InvocationPlugin.Receiver receiver, ValueNode[] argsIncludingReceiver);

    /**
     * Creates a snap shot of the current frame state with the BCI of the instruction after the one
     * currently being parsed and assigns it to a given {@linkplain StateSplit#hasSideEffect() side
     * effect} node.
     *
     * @param sideEffect a side effect node just appended to the graph
     */
    void setStateAfter(StateSplit sideEffect);

    /**
     * Gets the parsing context for the method that inlines the method being parsed by this context.
     */
    GraphBuilderContext getParent();

    /**
     * Gets the first ancestor parsing context that is not parsing a {@linkplain #parsingIntrinsic() intrinsic}.
     */
    default GraphBuilderContext getNonIntrinsicAncestor()
    {
        GraphBuilderContext ancestor = getParent();
        while (ancestor != null && ancestor.parsingIntrinsic())
        {
            ancestor = ancestor.getParent();
        }
        return ancestor;
    }

    /**
     * Gets the code being parsed.
     */
    Bytecode getCode();

    /**
     * Gets the method being parsed by this context.
     */
    ResolvedJavaMethod getMethod();

    /**
     * Gets the index of the bytecode instruction currently being parsed.
     */
    int bci();

    /**
     * Gets the kind of invocation currently being parsed.
     */
    InvokeKind getInvokeKind();

    /**
     * Gets the return type of the invocation currently being parsed.
     */
    JavaType getInvokeReturnType();

    default StampPair getInvokeReturnStamp(Assumptions assumptions)
    {
        JavaType returnType = getInvokeReturnType();
        return StampFactory.forDeclaredType(assumptions, returnType, false);
    }

    /**
     * Gets the inline depth of this context. A return value of 0 implies that this is the context
     * for the parse root.
     */
    default int getDepth()
    {
        GraphBuilderContext parent = getParent();
        int result = 0;
        while (parent != null)
        {
            result++;
            parent = parent.getParent();
        }
        return result;
    }

    /**
     * Determines if this parsing context is within the bytecode of an intrinsic or a method inlined
     * by an intrinsic.
     */
    @Override
    default boolean parsingIntrinsic()
    {
        return getIntrinsic() != null;
    }

    /**
     * Gets the intrinsic of the current parsing context or {@code null} if not
     * {@link #parsingIntrinsic() parsing an intrinsic}.
     */
    IntrinsicContext getIntrinsic();

    BailoutException bailout(String string);

    default ValueNode nullCheckedValue(ValueNode value)
    {
        return nullCheckedValue(value, DeoptimizationAction.InvalidateReprofile);
    }

    /**
     * Gets a version of a given value that has a {@linkplain StampTool#isPointerNonNull(ValueNode)
     * non-null} stamp.
     */
    default ValueNode nullCheckedValue(ValueNode value, DeoptimizationAction action)
    {
        if (!StampTool.isPointerNonNull(value))
        {
            LogicNode condition = getGraph().unique(IsNullNode.create(value));
            ObjectStamp receiverStamp = (ObjectStamp) value.stamp(NodeView.DEFAULT);
            Stamp stamp = receiverStamp.join(StampFactory.objectNonNull());
            FixedGuardNode fixedGuard = append(new FixedGuardNode(condition, DeoptimizationReason.NullCheckException, action, true));
            ValueNode nonNullReceiver = getGraph().addOrUniqueWithInputs(PiNode.create(value, stamp, fixedGuard));
            // TODO Propogating the non-null into the frame state would remove subsequent null-checks on the same value.
            // However, it currently causes an assertion failure when merging states.
            //
            // frameState.replace(value, nonNullReceiver);
            return nonNullReceiver;
        }
        return value;
    }

    @SuppressWarnings("unused")
    default void notifyReplacedCall(ResolvedJavaMethod targetMethod, ConstantNode node)
    {
    }

    /**
     * Interface whose instances hold inlining information about the current context, in a wider
     * sense. The wider sense in this case concerns graph building approaches that don't necessarily
     * keep a chain of {@link GraphBuilderContext} instances normally available through
     * {@linkplain #getParent()}. Examples of such approaches are partial evaluation and incremental inlining.
     */
    // @iface GraphBuilderContext.ExternalInliningContext
    interface ExternalInliningContext
    {
        int getInlinedDepth();
    }

    default ExternalInliningContext getExternalInliningContext()
    {
        return null;
    }
}
