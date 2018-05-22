package graalvm.compiler.replacements.nodes;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.ValueNode;

/**
 * This node class can be used to create {@link MacroNode}s for simple pure functions like
 * {@link System#identityHashCode(Object)}.
 */
public abstract class PureFunctionMacroNode extends MacroStateSplitNode implements Canonicalizable
{
    public static final NodeClass<PureFunctionMacroNode> TYPE = NodeClass.create(PureFunctionMacroNode.class);

    public PureFunctionMacroNode(NodeClass<? extends MacroNode> c, InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments)
    {
        super(c, invokeKind, targetMethod, bci, returnStamp, arguments);
    }

    /**
     * This method should return either a constant that represents the result of the function, or
     * null if no such result could be determined.
     */
    protected abstract JavaConstant evaluate(JavaConstant param, MetaAccessProvider metaAccess);

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        else
        {
            ValueNode param = arguments.get(0);
            if (param.isConstant())
            {
                JavaConstant constant = evaluate(param.asJavaConstant(), tool.getMetaAccess());
                if (constant != null)
                {
                    return ConstantNode.forConstant(constant, tool.getMetaAccess());
                }
            }
        }
        return this;
    }
}
