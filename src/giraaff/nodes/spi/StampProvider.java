package giraaff.nodes.spi;

import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.nodes.extended.LoadHubNode;

/**
 * Provides a capability for creating platform dependent stamps.
 */
// @iface StampProvider
public interface StampProvider
{
    /**
     * Create the stamp of the {@link LoadHubNode hub} of an object.
     */
    Stamp createHubStamp(ObjectStamp object);

    /**
     * Create the stamp of a pointer to a method.
     */
    Stamp createMethodStamp();
}
