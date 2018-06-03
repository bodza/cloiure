package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.java.ArrayLengthNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.util.GraphUtil;

/**
 * A {@link PiNode} that also provides an array length in addition to a more refined stamp. A usage
 * that reads the array length, such as an {@link ArrayLengthNode}, can be canonicalized based on
 * this information.
 */
// @class PiArrayNode
public final class PiArrayNode extends PiNode implements ArrayLengthProvider
{
    // @def
    public static final NodeClass<PiArrayNode> TYPE = NodeClass.create(PiArrayNode.class);

    @Input
    // @field
    ValueNode length;

    @Override
    public ValueNode length()
    {
        return length;
    }

    // @cons
    public PiArrayNode(ValueNode __object, ValueNode __length, Stamp __stamp)
    {
        super(TYPE, __object, __stamp, null);
        this.length = __length;
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

    /**
     * Changes the stamp of an object inside a snippet to be the stamp of the node replaced by the snippet.
     */
    @NodeIntrinsic(Placeholder.class)
    public static native Object piArrayCastToSnippetReplaceeStamp(Object object, int length);

    /**
     * A placeholder node in a snippet that will be replaced with a {@link PiArrayNode} when the
     * snippet is instantiated.
     */
    // @class PiArrayNode.Placeholder
    public static final class Placeholder extends PiNode.Placeholder
    {
        // @def
        public static final NodeClass<Placeholder> TYPE = NodeClass.create(Placeholder.class);

        @Input
        // @field
        ValueNode length;

        // @cons
        protected Placeholder(ValueNode __object, ValueNode __length)
        {
            super(TYPE, __object);
            this.length = __length;
        }

        @Override
        public void makeReplacement(Stamp __snippetReplaceeStamp)
        {
            PiArrayNode __piArray = graph().addOrUnique(new PiArrayNode(object(), length, __snippetReplaceeStamp));
            replaceAndDelete(__piArray);
        }
    }
}
