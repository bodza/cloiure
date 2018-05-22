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
public final class PiArrayNode extends PiNode implements ArrayLengthProvider
{
    public static final NodeClass<PiArrayNode> TYPE = NodeClass.create(PiArrayNode.class);
    @Input ValueNode length;

    @Override
    public ValueNode length()
    {
        return length;
    }

    public PiArrayNode(ValueNode object, ValueNode length, Stamp stamp)
    {
        super(TYPE, object, stamp, null);
        this.length = length;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (GraphUtil.arrayLength(object()) != length())
        {
            return this;
        }
        return super.canonical(tool);
    }

    /**
     * Changes the stamp of an object inside a snippet to be the stamp of the node replaced by the
     * snippet.
     */
    @NodeIntrinsic(Placeholder.class)
    public static native Object piArrayCastToSnippetReplaceeStamp(Object object, int length);

    /**
     * A placeholder node in a snippet that will be replaced with a {@link PiArrayNode} when the
     * snippet is instantiated.
     */
    public static class Placeholder extends PiNode.Placeholder
    {
        public static final NodeClass<Placeholder> TYPE = NodeClass.create(Placeholder.class);
        @Input ValueNode length;

        protected Placeholder(ValueNode object, ValueNode length)
        {
            super(TYPE, object);
            this.length = length;
        }

        @Override
        public void makeReplacement(Stamp snippetReplaceeStamp)
        {
            PiArrayNode piArray = graph().addOrUnique(new PiArrayNode(object(), length, snippetReplaceeStamp));
            replaceAndDelete(piArray);
        }
    }
}
