package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.spi.CanonicalizerTool;

/**
 * Interface that overrides properties of a node, such as the node's stamp.
 *
 * This interface allows richer canonicalizations when the current compilation context can provide a
 * narrower stamp than the one stored in the node itself. One such example is performing
 * canonicalization late in the compilation, when the nodes are already scheduled, and benefit from
 * additional stamp information from conditional checks in branches.
 *
 * For example, in the following code, <code>offset + i</code> can be canonicalized once it is
 * scheduled into the branch:
 *
 * <pre>
 * public void update(int offset, int i) {
 *     if (i == 0) {
 *         array[offset + i];
 *     }
 * }
 * </pre>
 */
public interface NodeView {

    NodeView DEFAULT = new Default();

    class Default implements NodeView {
        @Override
        public Stamp stamp(ValueNode node) {
            return node.stamp;
        }
    }

    /**
     * Return a view-specific stamp of the node.
     *
     * This stamp must be more specific than the default stamp.
     */
    Stamp stamp(ValueNode node);

    static NodeView from(CanonicalizerTool tool) {
        if (tool instanceof NodeView) {
            return (NodeView) tool;
        }
        return DEFAULT;
    }
}
