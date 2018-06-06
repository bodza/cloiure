package giraaff.lir;

import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;

///
// Functional interface for iterating over a list of values, possibly returning a value to replace
// the old value.
///
@FunctionalInterface
// @iface InstructionValueProcedure
public interface InstructionValueProcedure
{
    ///
    // Iterator method to be overwritten.
    //
    // @param instruction The current instruction.
    // @param value The value that is iterated.
    // @param mode The operand mode for the value.
    // @param flags A set of flags for the value.
    // @return The new value to replace the value that was passed in.
    ///
    Value doValue(LIRInstruction __instruction, Value __value, LIRInstruction.OperandMode __mode, EnumSet<LIRInstruction.OperandFlag> __flags);
}
