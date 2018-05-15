package graalvm.compiler.nodes.spi;

import graalvm.compiler.core.common.type.Stamp;

public interface UncheckedInterfaceProvider {
    /**
     * Returns a stamp containing information about interface types that has not been verified or
     * null if no such stamp is available. A type check is needed before using informations from
     * this stamp.
     */
    Stamp uncheckedStamp();
}
