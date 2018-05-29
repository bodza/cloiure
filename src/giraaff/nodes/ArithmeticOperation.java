package giraaff.nodes;

import giraaff.core.common.type.ArithmeticOpTable.Op;

/**
 * An {@code ArithmeticOperation} is an operation that does primitive value arithmetic without side effect.
 */
// @iface ArithmeticOperation
public interface ArithmeticOperation
{
    Op getArithmeticOp();
}
