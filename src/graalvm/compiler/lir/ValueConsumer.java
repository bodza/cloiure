package graalvm.compiler.lir;

import java.util.EnumSet;

import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;

import jdk.vm.ci.meta.Value;

/**
 * Non-modifying version of {@link ValueProcedure}.
 */
@FunctionalInterface
public interface ValueConsumer extends InstructionValueConsumer
{
    /**
     * Iterator method to be overwritten.
     *
     * @param value The value that is iterated.
     * @param mode The operand mode for the value.
     * @param flags A set of flags for the value.
     */
    void visitValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags);

    @Override
    default void visitValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
    {
        visitValue(value, mode, flags);
    }
}
