package graalvm.compiler.lir;

import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;

/**
 * Similar to {@link InstructionValueProcedure} but without an {@link LIRInstruction} parameter.
 */
@FunctionalInterface
public interface ValueProcedure extends InstructionValueProcedure
{
    /**
     * Iterator method to be overwritten.
     *
     * @param value The value that is iterated.
     * @param mode The operand mode for the value.
     * @param flags A set of flags for the value.
     * @return The new value to replace the value that was passed in.
     */
    Value doValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags);

    @Override
    default Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
    {
        return doValue(value, mode, flags);
    }
}
