package giraaff.lir;

import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;

///
// Similar to {@link InstructionValueProcedure} but without an {@link LIRInstruction} parameter.
///
@FunctionalInterface
// @iface ValueProcedure
public interface ValueProcedure extends InstructionValueProcedure
{
    ///
    // Iterator method to be overwritten.
    //
    // @param value The value that is iterated.
    // @param mode The operand mode for the value.
    // @param flags A set of flags for the value.
    // @return The new value to replace the value that was passed in.
    ///
    Value doValue(Value __value, OperandMode __mode, EnumSet<OperandFlag> __flags);

    @Override
    default Value doValue(LIRInstruction __instruction, Value __value, OperandMode __mode, EnumSet<OperandFlag> __flags)
    {
        return doValue(__value, __mode, __flags);
    }
}
