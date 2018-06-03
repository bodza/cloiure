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
    // @def
    public static final NodeClass<EncodedSymbolNode> TYPE = NodeClass.create(EncodedSymbolNode.class);

    @OptionalInput
    // @field
    protected ValueNode ___value;

    // @cons
    public EncodedSymbolNode(@InjectedNodeParameter Stamp __stamp, ValueNode __value)
    {
        super(TYPE, __stamp);
        this.___value = __value;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (this.___value != null)
        {
            Constant __constant = GraphUtil.foldIfConstantAndRemove(this, this.___value);
            if (__constant != null)
            {
                return new ConstantNode(new EncodedSymbolConstant(__constant), StampFactory.pointer());
            }
        }
        return this;
    }

    @NodeIntrinsic
    public static native Word encode(Object __constant);
}
