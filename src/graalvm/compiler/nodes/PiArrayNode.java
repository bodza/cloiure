package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.java.ArrayLengthNode;
import graalvm.compiler.nodes.spi.ArrayLengthProvider;
import graalvm.compiler.nodes.util.GraphUtil;

/**
 * A {@link PiNode} that also provides an array length in addition to a more refined stamp. A usage
 * that reads the array length, such as an {@link ArrayLengthNode}, can be canonicalized based on
 * this information.
 */
@NodeInfo
public final class PiArrayNode extends PiNode implements ArrayLengthProvider {

    public static final NodeClass<PiArrayNode> TYPE = NodeClass.create(PiArrayNode.class);
    @Input ValueNode length;

    @Override
    public ValueNode length() {
        return length;
    }

    public PiArrayNode(ValueNode object, ValueNode length, Stamp stamp) {
        super(TYPE, object, stamp, null);
        this.length = length;
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (GraphUtil.arrayLength(object()) != length()) {
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
    @NodeInfo(cycles = CYCLES_0, size = SIZE_0)
    public static class Placeholder extends PiNode.Placeholder {

        public static final NodeClass<Placeholder> TYPE = NodeClass.create(Placeholder.class);
        @Input ValueNode length;

        protected Placeholder(ValueNode object, ValueNode length) {
            super(TYPE, object);
            this.length = length;
        }

        @Override
        public void makeReplacement(Stamp snippetReplaceeStamp) {
            PiArrayNode piArray = graph().addOrUnique(new PiArrayNode(object(), length, snippetReplaceeStamp));
            replaceAndDelete(piArray);
        }
    }
}
