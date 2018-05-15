package graalvm.compiler.lir;

import java.util.EnumSet;

import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;

import jdk.vm.ci.meta.Value;

/**
 * Functional interface for iterating over a list of values, possibly returning a value to replace
 * the old value.
 */
@FunctionalInterface
public interface InstructionValueProcedure {

    /**
     * Iterator method to be overwritten.
     *
     * @param instruction The current instruction.
     * @param value The value that is iterated.
     * @param mode The operand mode for the value.
     * @param flags A set of flags for the value.
     * @return The new value to replace the value that was passed in.
     */
    Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags);

}
