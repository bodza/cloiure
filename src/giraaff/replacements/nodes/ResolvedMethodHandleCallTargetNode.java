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

/**
 * A call target that replaces itself in the graph when being lowered by restoring the original
 * {@link MethodHandle} invocation target. Prior to
 * https://bugs.openjdk.java.net/browse/JDK-8072008, this is required for when a
 * {@link MethodHandle} call is resolved to a constant target but the target was not inlined. In
 * that case, the original invocation must be restored with all of its original arguments. Why?
 * HotSpot linkage for {@link MethodHandle} intrinsics (see
 * {@code MethodHandles::generate_method_handle_dispatch}) expects certain implicit arguments to be
 * on the stack such as the MemberName suffix argument for a call to one of the MethodHandle.linkTo*
 * methods. An {@linkplain MethodHandleNode#tryResolveTargetInvoke resolved} {@link MethodHandle}
 * invocation drops these arguments which means the interpreter won't find them.
 */
public final class ResolvedMethodHandleCallTargetNode extends MethodCallTargetNode implements Lowerable
{
    public static final NodeClass<ResolvedMethodHandleCallTargetNode> TYPE = NodeClass.create(ResolvedMethodHandleCallTargetNode.class);

    /**
     * Creates a call target for an invocation on a direct target derived by resolving a constant
     * {@link MethodHandle}.
     */
    public static MethodCallTargetNode create(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] arguments, StampPair returnStamp, ResolvedJavaMethod originalTargetMethod, ValueNode[] originalArguments, StampPair originalReturnStamp)
    {
        return new ResolvedMethodHandleCallTargetNode(invokeKind, targetMethod, arguments, returnStamp, originalTargetMethod, originalArguments, originalReturnStamp);
    }

    protected final ResolvedJavaMethod originalTargetMethod;
    protected final StampPair originalReturnStamp;
    @Input NodeInputList<ValueNode> originalArguments;

    protected ResolvedMethodHandleCallTargetNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] arguments, StampPair returnStamp, ResolvedJavaMethod originalTargetMethod, ValueNode[] originalArguments, StampPair originalReturnStamp)
    {
        super(TYPE, invokeKind, targetMethod, arguments, returnStamp, null);
        this.originalTargetMethod = originalTargetMethod;
        this.originalReturnStamp = originalReturnStamp;
        this.originalArguments = new NodeInputList<>(this, originalArguments);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        InvokeKind replacementInvokeKind = originalTargetMethod.isStatic() ? InvokeKind.Static : InvokeKind.Special;
        MethodCallTargetNode replacement = graph().add(new MethodCallTargetNode(replacementInvokeKind, originalTargetMethod, originalArguments.toArray(new ValueNode[originalArguments.size()]), originalReturnStamp, null));

        // Replace myself...
        this.replaceAndDelete(replacement);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        throw GraalError.shouldNotReachHere("should have replaced itself");
    }
}
