package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.java.ArrayLengthNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.util.GraphUtil;

///
// A {@link PiNode} that also provides an array length in addition to a more refined stamp. A usage
// that reads the array length, such as an {@link ArrayLengthNode}, can be canonicalized based on
// this information.
///
// @class PiArrayNode
public final class PiArrayNode extends PiNode implements ArrayLengthProvider
{
    // @def
    public static final NodeClass<PiArrayNode> TYPE = NodeClass.create(PiArrayNode.class);

    @Node.Input
    // @field
    ValueNode ___length;

    @Override
    public ValueNode length()
    {
        return this.___length;
    }

    // @cons PiArrayNode
    public PiArrayNode(ValueNode __object, ValueNode __length, Stamp __stamp)
    {
        super(TYPE, __object, __stamp, null);
        this.___length = __length;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (GraphUtil.arrayLength(object()) != length())
        {
            return this;
        }
        return super.canonical(__tool);
    }

    ///
    // Changes the stamp of an object inside a snippet to be the stamp of the node replaced by the snippet.
    ///
    @Node.NodeIntrinsic(PiArrayNode.ArrayPlaceholder.class)
    public static native Object piArrayCastToSnippetReplaceeStamp(Object __object, int __length);

    ///
    // A placeholder node in a snippet that will be replaced with a {@link PiArrayNode} when the
    // snippet is instantiated.
    ///
    // @class PiArrayNode.ArrayPlaceholder
    public static final class ArrayPlaceholder extends PiNode.Placeholder
    {
        // @def
        public static final NodeClass<PiArrayNode.ArrayPlaceholder> TYPE = NodeClass.create(PiArrayNode.ArrayPlaceholder.class);

        @Node.Input
        // @field
        ValueNode ___length;

        // @cons PiArrayNode.ArrayPlaceholder
        protected ArrayPlaceholder(ValueNode __object, ValueNode __length)
        {
            super(TYPE, __object);
            this.___length = __length;
        }

        @Override
        public void makeReplacement(Stamp __snippetReplaceeStamp)
        {
            PiArrayNode __piArray = graph().addOrUnique(new PiArrayNode(object(), this.___length, __snippetReplaceeStamp));
            replaceAndDelete(__piArray);
        }
    }
}
