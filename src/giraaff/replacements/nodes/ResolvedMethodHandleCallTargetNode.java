package giraaff.replacements.nodes;

import java.lang.invoke.MethodHandle;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.util.GraalError;

///
// A call target that replaces itself in the graph when being lowered by restoring the original
// {@link MethodHandle} invocation target. Prior to
// https://bugs.openjdk.java.net/browse/JDK-8072008, this is required for when a
// {@link MethodHandle} call is resolved to a constant target but the target was not inlined. In
// that case, the original invocation must be restored with all of its original arguments. Why?
// HotSpot linkage for {@link MethodHandle} intrinsics (see
// {@code MethodHandles::generate_method_handle_dispatch}) expects certain implicit arguments to be
// on the stack such as the MemberName suffix argument for a call to one of the MethodHandle.linkTo*
// methods. An {@linkplain MethodHandleNode#tryResolveTargetInvoke resolved} {@link MethodHandle}
// invocation drops these arguments which means the interpreter won't find them.
///
// @class ResolvedMethodHandleCallTargetNode
public final class ResolvedMethodHandleCallTargetNode extends MethodCallTargetNode implements Lowerable
{
    // @def
    public static final NodeClass<ResolvedMethodHandleCallTargetNode> TYPE = NodeClass.create(ResolvedMethodHandleCallTargetNode.class);

    ///
    // Creates a call target for an invocation on a direct target derived by resolving a constant
    // {@link MethodHandle}.
    ///
    public static MethodCallTargetNode create(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __arguments, StampPair __returnStamp, ResolvedJavaMethod __originalTargetMethod, ValueNode[] __originalArguments, StampPair __originalReturnStamp)
    {
        return new ResolvedMethodHandleCallTargetNode(__invokeKind, __targetMethod, __arguments, __returnStamp, __originalTargetMethod, __originalArguments, __originalReturnStamp);
    }

    // @field
    protected final ResolvedJavaMethod ___originalTargetMethod;
    // @field
    protected final StampPair ___originalReturnStamp;
    @Input
    // @field
    NodeInputList<ValueNode> ___originalArguments;

    // @cons
    protected ResolvedMethodHandleCallTargetNode(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __arguments, StampPair __returnStamp, ResolvedJavaMethod __originalTargetMethod, ValueNode[] __originalArguments, StampPair __originalReturnStamp)
    {
        super(TYPE, __invokeKind, __targetMethod, __arguments, __returnStamp, null);
        this.___originalTargetMethod = __originalTargetMethod;
        this.___originalReturnStamp = __originalReturnStamp;
        this.___originalArguments = new NodeInputList<>(this, __originalArguments);
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        InvokeKind __replacementInvokeKind = this.___originalTargetMethod.isStatic() ? InvokeKind.Static : InvokeKind.Special;
        MethodCallTargetNode __replacement = graph().add(new MethodCallTargetNode(__replacementInvokeKind, this.___originalTargetMethod, this.___originalArguments.toArray(new ValueNode[this.___originalArguments.size()]), this.___originalReturnStamp, null));

        // Replace myself...
        this.replaceAndDelete(__replacement);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        throw GraalError.shouldNotReachHere("should have replaced itself");
    }
}
