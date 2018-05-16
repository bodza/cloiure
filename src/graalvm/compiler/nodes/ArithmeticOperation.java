package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.ArithmeticOpTable.Op;

/**
 * An {@code ArithmeticOperation} is an operation that does primitive value arithmetic without side
 * effect.
 */
public interface ArithmeticOperation
{
    Op getArithmeticOp();
}
