package giraaff.replacements.nodes;

import giraaff.api.replacements.Snippet.VarargsParameter;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.ValueNode;

/**
 * Implements the semantics of {@link VarargsParameter}.
 */
// @class LoadSnippetVarargParameterNode
public final class LoadSnippetVarargParameterNode extends FixedWithNextNode implements Canonicalizable
{
    // @def
    public static final NodeClass<LoadSnippetVarargParameterNode> TYPE = NodeClass.create(LoadSnippetVarargParameterNode.class);

    @Input
    // @field
    ValueNode index;

    @Input
    // @field
    NodeInputList<ParameterNode> parameters;

    // @cons
    public LoadSnippetVarargParameterNode(ParameterNode[] __locals, ValueNode __index, Stamp __stamp)
    {
        super(TYPE, __stamp);
        this.index = __index;
        this.parameters = new NodeInputList<>(this, __locals);
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (index.isConstant())
        {
            int __indexValue = index.asJavaConstant().asInt();
            if (__indexValue < parameters.size())
            {
                return parameters.get(__indexValue);
            }
        }
        return this;
    }
}
