package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

///
// This interface marks nodes that access some memory location, and that have an edge to the last
// node that kills this location.
///
// @iface MemoryAccess
public interface MemoryAccess
{
    LocationIdentity getLocationIdentity();

    MemoryNode getLastLocationAccess();

    ///
    // @param lla the {@link MemoryNode} that represents the last kill of the location
    ///
    void setLastLocationAccess(MemoryNode __lla);
}
