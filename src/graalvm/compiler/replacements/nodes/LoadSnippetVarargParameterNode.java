package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.api.replacements.Snippet.VarargsParameter;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ParameterNode;
import graalvm.compiler.nodes.ValueNode;

/**
 * Implements the semantics of {@link VarargsParameter}.
 */
@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class LoadSnippetVarargParameterNode extends FixedWithNextNode implements Canonicalizable {

    public static final NodeClass<LoadSnippetVarargParameterNode> TYPE = NodeClass.create(LoadSnippetVarargParameterNode.class);
    @Input ValueNode index;

    @Input NodeInputList<ParameterNode> parameters;

    public LoadSnippetVarargParameterNode(ParameterNode[] locals, ValueNode index, Stamp stamp) {
        super(TYPE, stamp);
        this.index = index;
        this.parameters = new NodeInputList<>(this, locals);
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (index.isConstant()) {
            int indexValue = index.asJavaConstant().asInt();
            if (indexValue < parameters.size()) {
                return parameters.get(indexValue);
            }
        }
        return this;
    }
}
