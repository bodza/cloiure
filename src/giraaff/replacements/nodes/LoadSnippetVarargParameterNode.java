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
    public static final NodeClass<LoadSnippetVarargParameterNode> TYPE = NodeClass.create(LoadSnippetVarargParameterNode.class);

    @Input ValueNode index;

    @Input NodeInputList<ParameterNode> parameters;

    // @cons
    public LoadSnippetVarargParameterNode(ParameterNode[] locals, ValueNode index, Stamp stamp)
    {
        super(TYPE, stamp);
        this.index = index;
        this.parameters = new NodeInputList<>(this, locals);
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (index.isConstant())
        {
            int indexValue = index.asJavaConstant().asInt();
            if (indexValue < parameters.size())
            {
                return parameters.get(indexValue);
            }
        }
        return this;
    }
}
