package giraaff.lir;

import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;

///
// Functional interface for iterating over a list of values without modifying them. See
// {@link InstructionValueProcedure} for a version that can modify values.
///
@FunctionalInterface
// @iface InstructionValueConsumer
public interface InstructionValueConsumer
{
    ///
    // Iterator method to be overwritten.
    //
    // @param instruction The current instruction.
    // @param value The value that is iterated.
    // @param mode The operand mode for the value.
    // @param flags A set of flags for the value.
    ///
    void visitValue(LIRInstruction __instruction, Value __value, OperandMode __mode, EnumSet<OperandFlag> __flags);
}
