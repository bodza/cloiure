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
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.type.StampTool;

///
// Used by a {@link GraphBuilderPlugin} to interface with an object that parses the bytecode of a
// single {@linkplain #getMethod() method} as part of building a {@linkplain #getGraph() graph}.
///
// @iface GraphBuilderContext
public interface GraphBuilderContext extends GraphBuilderTool
{
    ///
    // Pushes a given value to the frame state stack using an explicit kind. This should be used
    // when {@code value.getJavaKind()} is different from the kind that the bytecode instruction
    // currently being parsed pushes to the stack.
    //
    // @param kind the kind to use when type checking this operation
    // @param value the value to push to the stack. The value must already have been
    //            {@linkplain #append(ValueNode) appended}.
    ///
    void push(JavaKind __kind, ValueNode __value);

    ///
    // Adds a node to the graph. If the node is in the graph, returns immediately. If the node is a
    // {@link StateSplit} with a null {@linkplain StateSplit#stateAfter() frame state}, the frame
    // state is initialized.
    //
    // @param value the value to add to the graph and push to the stack. The
    //            {@code value.getJavaKind()} kind is used when type checking this operation.
    // @return a node equivalent to {@code value} in the graph
    ///
    default <T extends ValueNode> T add(T __value)
    {
        if (__value.graph() != null)
        {
            return __value;
        }
        T __equivalentValue = append(__value);
        if (__equivalentValue instanceof StateSplit)
        {
            StateSplit __stateSplit = (StateSplit) __equivalentValue;
            if (__stateSplit.stateAfter() == null && __stateSplit.hasSideEffect())
            {
                setStateAfter(__stateSplit);
            }
        }
        return __equivalentValue;
    }

    ///
    // Adds a node and its inputs to the graph. If the node is in the graph, returns immediately. If
    // the node is a {@link StateSplit} with a null {@linkplain StateSplit#stateAfter() frame state}
    // , the frame state is initialized.
    //
    // @param value the value to add to the graph and push to the stack. The
    //            {@code value.getJavaKind()} kind is used when type checking this operation.
    // @return a node equivalent to {@code value} in the graph
    ///
    default <T extends ValueNode> T addWithInputs(T __value)
    {
        if (__value.graph() != null)
        {
            return __value;
        }
        T __equivalentValue = append(__value);
        if (__equivalentValue instanceof StateSplit)
        {
            StateSplit __stateSplit = (StateSplit) __equivalentValue;
            if (__stateSplit.stateAfter() == null && __stateSplit.hasSideEffect())
            {
                setStateAfter(__stateSplit);
            }
        }
        return __equivalentValue;
    }

    default ValueNode addNonNullCast(ValueNode __value)
    {
        AbstractPointerStamp __valueStamp = (AbstractPointerStamp) __value.stamp(NodeView.DEFAULT);
        if (__valueStamp.nonNull())
        {
            return __value;
        }
        else
        {
            LogicNode __isNull = add(IsNullNode.create(__value));
            FixedGuardNode __fixedGuard = add(new FixedGuardNode(__isNull, DeoptimizationReason.NullCheckException, DeoptimizationAction.None, true));
            Stamp __newStamp = __valueStamp.improveWith(StampFactory.objectNonNull());
            return add(PiNode.create(__value, __newStamp, __fixedGuard));
        }
    }

    ///
    // Adds a node with a non-void kind to the graph, pushes it to the stack. If the returned node
    // is a {@link StateSplit} with a null {@linkplain StateSplit#stateAfter() frame state}, the
    // frame state is initialized.
    //
    // @param kind the kind to use when type checking this operation
    // @param value the value to add to the graph and push to the stack
    // @return a node equivalent to {@code value} in the graph
    ///
    default <T extends ValueNode> T addPush(JavaKind __kind, T __value)
    {
        T __equivalentValue = __value.graph() != null ? __value : append(__value);
        push(__kind, __equivalentValue);
        if (__equivalentValue instanceof StateSplit)
        {
            StateSplit __stateSplit = (StateSplit) __equivalentValue;
            if (__stateSplit.stateAfter() == null && __stateSplit.hasSideEffect())
            {
                setStateAfter(__stateSplit);
            }
        }
        return __equivalentValue;
    }

    ///
    // Handles an invocation that a plugin determines can replace the original invocation (i.e., the
    // one for which the plugin was applied). This applies all standard graph builder processing to
    // the replaced invocation including applying any relevant plugins.
    //
    // @param invokeKind the kind of the replacement invocation
    // @param targetMethod the target of the replacement invocation
    // @param args the arguments to the replacement invocation
    // @param forceInlineEverything specifies if all invocations encountered in the scope of
    //            handling the replaced invoke are to be force inlined
    ///
    void handleReplacedInvoke(CallTargetNode.InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __args, boolean __forceInlineEverything);

    void handleReplacedInvoke(CallTargetNode __callTarget, JavaKind __resultType);

    ///
    // Intrinsifies an invocation of a given method by inlining the bytecodes of a given
    // substitution method.
    //
    // @param bytecodeProvider used to get the bytecodes to parse for the substitution method
    // @param targetMethod the method being intrinsified
    // @param substitute the intrinsic implementation
    // @param receiver the receiver, or null for static methods
    // @param argsIncludingReceiver the arguments with which to inline the invocation
    //
    // @return whether the intrinsification was successful
    ///
    boolean intrinsify(BytecodeProvider __bytecodeProvider, ResolvedJavaMethod __targetMethod, ResolvedJavaMethod __substitute, InvocationPlugin.Receiver __receiver, ValueNode[] __argsIncludingReceiver);

    ///
    // Creates a snap shot of the current frame state with the BCI of the instruction after the one
    // currently being parsed and assigns it to a given {@linkplain StateSplit#hasSideEffect() side
    // effect} node.
    //
    // @param sideEffect a side effect node just appended to the graph
    ///
    void setStateAfter(StateSplit __sideEffect);

    ///
    // Gets the parsing context for the method that inlines the method being parsed by this context.
    ///
    GraphBuilderContext getParent();

    ///
    // Gets the first ancestor parsing context that is not parsing a {@linkplain #parsingIntrinsic() intrinsic}.
    ///
    default GraphBuilderContext getNonIntrinsicAncestor()
    {
        GraphBuilderContext __ancestor = getParent();
        while (__ancestor != null && __ancestor.parsingIntrinsic())
        {
            __ancestor = __ancestor.getParent();
        }
        return __ancestor;
    }

    ///
    // Gets the code being parsed.
    ///
    Bytecode getCode();

    ///
    // Gets the method being parsed by this context.
    ///
    ResolvedJavaMethod getMethod();

    ///
    // Gets the index of the bytecode instruction currently being parsed.
    ///
    int bci();

    ///
    // Gets the kind of invocation currently being parsed.
    ///
    CallTargetNode.InvokeKind getInvokeKind();

    ///
    // Gets the return type of the invocation currently being parsed.
    ///
    JavaType getInvokeReturnType();

    default StampPair getInvokeReturnStamp(Assumptions __assumptions)
    {
        return StampFactory.forDeclaredType(__assumptions, getInvokeReturnType(), false);
    }

    ///
    // Gets the inline depth of this context. A return value of 0 implies that this is the context
    // for the parse root.
    ///
    default int getDepth()
    {
        GraphBuilderContext __parent = getParent();
        int __result = 0;
        while (__parent != null)
        {
            __result++;
            __parent = __parent.getParent();
        }
        return __result;
    }

    ///
    // Determines if this parsing context is within the bytecode of an intrinsic or a method inlined
    // by an intrinsic.
    ///
    @Override
    default boolean parsingIntrinsic()
    {
        return getIntrinsic() != null;
    }

    ///
    // Gets the intrinsic of the current parsing context or {@code null} if not
    // {@link #parsingIntrinsic() parsing an intrinsic}.
    ///
    IntrinsicContext getIntrinsic();

    BailoutException bailout(String __msg);

    default ValueNode nullCheckedValue(ValueNode __value)
    {
        return nullCheckedValue(__value, DeoptimizationAction.InvalidateReprofile);
    }

    ///
    // Gets a version of a given value that has a {@linkplain StampTool#isPointerNonNull(ValueNode)
    // non-null} stamp.
    ///
    default ValueNode nullCheckedValue(ValueNode __value, DeoptimizationAction __action)
    {
        if (!StampTool.isPointerNonNull(__value))
        {
            LogicNode __condition = getGraph().unique(IsNullNode.create(__value));
            ObjectStamp __receiverStamp = (ObjectStamp) __value.stamp(NodeView.DEFAULT);
            Stamp __stamp = __receiverStamp.join(StampFactory.objectNonNull());
            FixedGuardNode __fixedGuard = append(new FixedGuardNode(__condition, DeoptimizationReason.NullCheckException, __action, true));
            ValueNode __nonNullReceiver = getGraph().addOrUniqueWithInputs(PiNode.create(__value, __stamp, __fixedGuard));
            // TODO Propogating the non-null into the frame state would remove subsequent null-checks on the same value.
            // However, it currently causes an assertion failure when merging states.
            //
            // frameState.replace(value, nonNullReceiver);
            return __nonNullReceiver;
        }
        return __value;
    }

    @SuppressWarnings("unused")
    default void notifyReplacedCall(ResolvedJavaMethod __targetMethod, ConstantNode __node)
    {
    }

    ///
    // Interface whose instances hold inlining information about the current context, in a wider
    // sense. The wider sense in this case concerns graph building approaches that don't necessarily
    // keep a chain of {@link GraphBuilderContext} instances normally available through
    // {@linkplain #getParent()}. Examples of such approaches are partial evaluation and incremental inlining.
    ///
    // @iface GraphBuilderContext.ExternalInliningContext
    interface ExternalInliningContext
    {
        int getInlinedDepth();
    }

    default GraphBuilderContext.ExternalInliningContext getExternalInliningContext()
    {
        return null;
    }
}
