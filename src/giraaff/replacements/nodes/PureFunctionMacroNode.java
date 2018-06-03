package giraaff.replacements.nodes;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ValueNode;

/**
 * This node class can be used to create {@link MacroNode}s for simple pure functions like
 * {@link System#identityHashCode(Object)}.
 */
// @class PureFunctionMacroNode
public abstract class PureFunctionMacroNode extends MacroStateSplitNode implements Canonicalizable
{
    // @def
    public static final NodeClass<PureFunctionMacroNode> TYPE = NodeClass.create(PureFunctionMacroNode.class);

    // @cons
    public PureFunctionMacroNode(NodeClass<? extends MacroNode> __c, InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        super(__c, __invokeKind, __targetMethod, __bci, __returnStamp, __arguments);
    }

    /**
     * This method should return either a constant that represents the result of the function, or
     * null if no such result could be determined.
     */
    protected abstract JavaConstant evaluate(JavaConstant param, MetaAccessProvider metaAccess);

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        else
        {
            ValueNode __param = arguments.get(0);
            if (__param.isConstant())
            {
                JavaConstant __constant = evaluate(__param.asJavaConstant(), __tool.getMetaAccess());
                if (__constant != null)
                {
                    return ConstantNode.forConstant(__constant, __tool.getMetaAccess());
                }
            }
        }
        return this;
    }
}
