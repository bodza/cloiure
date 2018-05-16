package graalvm.compiler.nodes.spi;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.nodes.ValueNode;

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
