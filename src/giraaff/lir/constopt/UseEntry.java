package giraaff.lir.constopt;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.ValueProcedure;

/**
 * Represents a usage of a constant.
 */
class UseEntry
{
    private final AbstractBlockBase<?> block;
    private final LIRInstruction instruction;
    private final Value value;

    UseEntry(AbstractBlockBase<?> block, LIRInstruction instruction, Value value)
    {
        this.block = block;
        this.instruction = instruction;
        this.value = value;
    }

    public LIRInstruction getInstruction()
    {
        return instruction;
    }

    public AbstractBlockBase<?> getBlock()
    {
        return block;
    }

    public void setValue(Value newValue)
    {
        replaceValue(instruction, value, newValue);
    }

    private static void replaceValue(LIRInstruction op, Value oldValue, Value newValue)
    {
        ValueProcedure proc = (value, mode, flags) -> value.identityEquals(oldValue) ? newValue : value;

        op.forEachAlive(proc);
        op.forEachInput(proc);
        op.forEachOutput(proc);
        op.forEachTemp(proc);
    }

    public Value getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "Use[" + getValue() + ":" + instruction + ":" + block + "]";
    }
}
