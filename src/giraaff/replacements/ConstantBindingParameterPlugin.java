package giraaff.replacements;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.type.StampPair;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderTool;
import giraaff.nodes.graphbuilderconf.ParameterPlugin;

/**
 * A {@link ParameterPlugin} that binds constant values to some parameters.
 */
// @class ConstantBindingParameterPlugin
public final class ConstantBindingParameterPlugin implements ParameterPlugin
{
    // @field
    private final Object[] constantArgs;
    // @field
    private final MetaAccessProvider metaAccess;
    // @field
    private final SnippetReflectionProvider snippetReflection;

    /**
     * Creates a plugin that will create {@link ConstantNode}s for each parameter with an index
     * equal to that of a non-null object in {@code constantArgs} (from which the
     * {@link ConstantNode} is created if it isn't already a {@link ConstantNode}).
     */
    // @cons
    public ConstantBindingParameterPlugin(Object[] __constantArgs, MetaAccessProvider __metaAccess, SnippetReflectionProvider __snippetReflection)
    {
        super();
        this.constantArgs = __constantArgs;
        this.metaAccess = __metaAccess;
        this.snippetReflection = __snippetReflection;
    }

    @Override
    public FloatingNode interceptParameter(GraphBuilderTool __b, int __index, StampPair __stamp)
    {
        Object __arg = constantArgs[__index];
        if (__arg != null)
        {
            ConstantNode __constantNode;
            if (__arg instanceof ConstantNode)
            {
                ConstantNode __otherCon = (ConstantNode) __arg;
                if (__otherCon.graph() != __b.getGraph())
                {
                    // This is a node from another graph, so copy over extra state into a new ConstantNode.
                    __constantNode = ConstantNode.forConstant(__stamp.getTrustedStamp(), __otherCon.getValue(), __otherCon.getStableDimension(), __otherCon.isDefaultStable(), metaAccess);
                }
                else
                {
                    __constantNode = __otherCon;
                }
            }
            else if (__arg instanceof Constant)
            {
                __constantNode = ConstantNode.forConstant(__stamp.getTrustedStamp(), (Constant) __arg, metaAccess);
            }
            else
            {
                __constantNode = ConstantNode.forConstant(snippetReflection.forBoxed(__stamp.getTrustedStamp().getStackKind(), __arg), metaAccess);
            }
            return __constantNode;
        }
        return null;
    }
}
