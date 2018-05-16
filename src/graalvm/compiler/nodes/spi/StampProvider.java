package graalvm.compiler.nodes.spi;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.nodes.extended.LoadHubNode;

/**
 * Provides a capability for creating platform dependent stamps.
 */
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
