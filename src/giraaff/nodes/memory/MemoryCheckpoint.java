package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.Node;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedNodeInterface;

/**
 * This interface marks subclasses of {@link FixedNode} that kill a set of memory locations
 * represented by location identities (i.e. change a value at one or more locations that belong to
 * these location identities).
 */
// @iface MemoryCheckpoint
public interface MemoryCheckpoint extends MemoryNode, FixedNodeInterface
{
    // @iface MemoryCheckpoint.Single
    interface Single extends MemoryCheckpoint
    {
        /**
         * This method is used to determine which memory location is killed by this node. Returning
         * the special value {@link LocationIdentity#any()} will kill all memory locations.
         *
         * @return the identity of the location killed by this node.
         */
        LocationIdentity getLocationIdentity();
    }

    // @iface MemoryCheckpoint.Multi
    interface Multi extends MemoryCheckpoint
    {
        /**
         * This method is used to determine which set of memory locations is killed by this node.
         * Returning the special value {@link LocationIdentity#any()} will kill all memory locations.
         *
         * @return the identities of all locations killed by this node.
         */
        LocationIdentity[] getLocationIdentities();
    }

    // @class MemoryCheckpoint.TypeAssertion
    final class TypeAssertion
    {
        public static boolean correctType(Node node)
        {
            return !(node instanceof MemoryCheckpoint) || (node instanceof MemoryCheckpoint.Single ^ node instanceof MemoryCheckpoint.Multi);
        }
    }
}
