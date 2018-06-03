package giraaff.replacements;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MethodHandleAccessProvider;
import jdk.vm.ci.meta.MethodHandleAccessProvider.IntrinsicMethod;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeInputList;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.NodePlugin;
import giraaff.replacements.nodes.MethodHandleNode;

// @class MethodHandlePlugin
public final class MethodHandlePlugin implements NodePlugin
{
    // @field
    private final MethodHandleAccessProvider ___methodHandleAccess;
    // @field
    private final boolean ___safeForDeoptimization;

    // @cons
    public MethodHandlePlugin(MethodHandleAccessProvider __methodHandleAccess, boolean __safeForDeoptimization)
    {
        super();
        this.___methodHandleAccess = __methodHandleAccess;
        this.___safeForDeoptimization = __safeForDeoptimization;
    }

    private static int countRecursiveInlining(GraphBuilderContext __b, ResolvedJavaMethod __method)
    {
        int __count = 0;
        for (GraphBuilderContext __c = __b.getParent(); __c != null; __c = __c.getParent())
        {
            if (__method.equals(__c.getMethod()))
            {
                __count++;
            }
        }
        return __count;
    }

    @Override
    public boolean handleInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        IntrinsicMethod __intrinsicMethod = this.___methodHandleAccess.lookupMethodHandleIntrinsic(__method);
        if (__intrinsicMethod != null)
        {
            InvokeKind __invokeKind = __b.getInvokeKind();
            if (__invokeKind != InvokeKind.Static)
            {
                __args[0] = __b.nullCheckedValue(__args[0]);
            }
            StampPair __invokeReturnStamp = __b.getInvokeReturnStamp(__b.getAssumptions());
            // @closure
            MethodHandleNode.GraphAdder adder = new MethodHandleNode.GraphAdder(__b.getGraph())
            {
                @Override
                public <T extends ValueNode> T add(T __node)
                {
                    return __b.add(__node);
                }
            };
            InvokeNode __invoke = MethodHandleNode.tryResolveTargetInvoke(adder, this.___methodHandleAccess, __intrinsicMethod, __method, __b.bci(), __invokeReturnStamp, __args);
            if (__invoke == null)
            {
                MethodHandleNode __methodHandleNode = new MethodHandleNode(__intrinsicMethod, __invokeKind, __method, __b.bci(), __invokeReturnStamp, __args);
                if (__invokeReturnStamp.getTrustedStamp().getStackKind() == JavaKind.Void)
                {
                    __b.add(__methodHandleNode);
                }
                else
                {
                    __b.addPush(__invokeReturnStamp.getTrustedStamp().getStackKind(), __methodHandleNode);
                }
            }
            else
            {
                CallTargetNode __callTarget = __invoke.callTarget();
                NodeInputList<ValueNode> __argumentsList = __callTarget.arguments();
                for (int __i = 0; __i < __argumentsList.size(); ++__i)
                {
                    __argumentsList.initialize(__i, __b.append(__argumentsList.get(__i)));
                }

                boolean __inlineEverything = false;
                if (this.___safeForDeoptimization)
                {
                    // If a MemberName suffix argument is dropped, the replaced call cannot
                    // deoptimized since the necessary frame state cannot be reconstructed.
                    // As such, it needs to recursively inline everything.
                    __inlineEverything = __args.length != __argumentsList.size();
                }
                ResolvedJavaMethod __targetMethod = __callTarget.targetMethod();
                if (__inlineEverything && !__targetMethod.hasBytecodes())
                {
                    // we need to force-inline but we can not, leave the invoke as-is
                    return false;
                }

                int __recursionDepth = countRecursiveInlining(__b, __targetMethod);
                int __maxRecursionDepth = GraalOptions.maximumRecursiveInlining;
                if (__recursionDepth > __maxRecursionDepth)
                {
                    return false;
                }

                __b.handleReplacedInvoke(__invoke.getInvokeKind(), __targetMethod, __argumentsList.toArray(new ValueNode[__argumentsList.size()]), __inlineEverything);
            }
            return true;
        }
        return false;
    }
}
