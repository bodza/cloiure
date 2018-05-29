package giraaff.nodes.spi;

import giraaff.core.common.type.Stamp;
import giraaff.nodes.ValueNode;

// @iface StampInverter
public interface StampInverter
{
    /**
     * Computes the stamp of the input for the given output stamp.
     */
    Stamp invertStamp(Stamp outStamp);

    /**
     * Gets the input node.
     */
    ValueNode getValue();
}
