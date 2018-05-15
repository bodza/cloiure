package graalvm.compiler.nodes.memory;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.nodes.spi.LIRLowerable;

public interface LIRLowerableAccess extends LIRLowerable, Access {
    Stamp getAccessStamp();
}
