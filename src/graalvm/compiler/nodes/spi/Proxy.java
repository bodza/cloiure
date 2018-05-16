package graalvm.compiler.nodes.spi;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeInterface;

/**
 * This interface marks nodes whose result is the same as one of their inputs. Such nodes are used
 * to add type information, to introduce scheduling restrictions, etc.
 *
 * For some algorithms it is necessary or advantageous to see through these proxies.
 */
public interface Proxy extends NodeInterface
{
    Node getOriginalNode();
}
