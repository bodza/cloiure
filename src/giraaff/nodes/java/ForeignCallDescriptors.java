package giraaff.nodes.java;

import giraaff.core.common.spi.ForeignCallDescriptor;

/**
 * The foreign call descriptors used by nodes in this package.
 *
 * Using a separate class for such descriptors prevents an access from triggering unwanted class
 * initialization during runtime initialization.
 */
// @class ForeignCallDescriptors
public final class ForeignCallDescriptors
{
    /**
     * @see RegisterFinalizerNode
     */
    public static final ForeignCallDescriptor REGISTER_FINALIZER = new ForeignCallDescriptor("registerFinalizer", void.class, Object.class);
}
