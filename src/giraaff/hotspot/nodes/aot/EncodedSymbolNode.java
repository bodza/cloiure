package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.meta.Constant;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.replacements.EncodedSymbolConstant;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.word.Word;

// @class EncodedSymbolNode
public final class EncodedSymbolNode extends FloatingNode implements Canonicalizable
{
    public static final NodeClass<EncodedSymbolNode> TYPE = NodeClass.create(EncodedSymbolNode.class);

    @OptionalInput protected ValueNode value;

    // @cons
    public EncodedSymbolNode(@InjectedNodeParameter Stamp stamp, ValueNode value)
    {
        super(TYPE, stamp);
        this.value = value;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (value != null)
        {
            Constant constant = GraphUtil.foldIfConstantAndRemove(this, value);
            if (constant != null)
            {
                return new ConstantNode(new EncodedSymbolConstant(constant), StampFactory.pointer());
            }
        }
        return this;
    }

    @NodeIntrinsic
    public static native Word encode(Object constant);
}
