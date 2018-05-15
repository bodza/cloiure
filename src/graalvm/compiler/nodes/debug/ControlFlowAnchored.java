package graalvm.compiler.nodes.debug;

/**
 * Marker interface for nodes that prevents control flow optimizations. The node should never be
 * duplicated.
 */
public interface ControlFlowAnchored {

}
