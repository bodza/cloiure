package giraaff.nodes.memory;

import giraaff.core.common.type.Stamp;
import giraaff.nodes.spi.LIRLowerable;

public interface LIRLowerableAccess extends LIRLowerable, Access
{
    Stamp getAccessStamp();
}
