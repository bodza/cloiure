package giraaff.lir;

import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;

///
// Non-modifying version of {@link ValueProcedure}.
///
@FunctionalInterface
// @iface ValueConsumer
public interface ValueConsumer extends InstructionValueConsumer
{
    ///
    // Iterator method to be overwritten.
    //
    // @param value The value that is iterated.
    // @param mode The operand mode for the value.
    // @param flags A set of flags for the value.
    ///
    void visitValue(Value __value, LIRInstruction.OperandMode __mode, EnumSet<LIRInstruction.OperandFlag> __flags);

    @Override
    default void visitValue(LIRInstruction __instruction, Value __value, LIRInstruction.OperandMode __mode, EnumSet<LIRInstruction.OperandFlag> __flags)
    {
        visitValue(__value, __mode, __flags);
    }
}
