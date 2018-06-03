package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

///
// Maps a {@linkplain LocationIdentity location} to the last node that (potentially) wrote to the location.
///
// @iface MemoryMap
public interface MemoryMap
{
    ///
    // Gets the last node that that (potentially) wrote to {@code locationIdentity}.
    ///
    MemoryNode getLastLocationAccess(LocationIdentity __locationIdentity);

    ///
    // Gets the location identities in the domain of this map.
    ///
    Iterable<LocationIdentity> getLocations();
}
