package giraaff.lir.constopt;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.ValueProcedure;

/**
 * Represents a usage of a constant.
 */
// @class UseEntry
final class UseEntry
{
    // @field
    private final AbstractBlockBase<?> block;
    // @field
    private final LIRInstruction instruction;
    // @field
    private final Value value;

    // @cons
    UseEntry(AbstractBlockBase<?> __block, LIRInstruction __instruction, Value __value)
    {
        super();
        this.block = __block;
        this.instruction = __instruction;
        this.value = __value;
    }

    public LIRInstruction getInstruction()
    {
        return instruction;
    }

    public AbstractBlockBase<?> getBlock()
    {
        return block;
    }

    public void setValue(Value __newValue)
    {
        replaceValue(instruction, value, __newValue);
    }

    private static void replaceValue(LIRInstruction __op, Value __oldValue, Value __newValue)
    {
        ValueProcedure __proc = (__value, __mode, __flags) -> __value.identityEquals(__oldValue) ? __newValue : __value;

        __op.forEachAlive(__proc);
        __op.forEachInput(__proc);
        __op.forEachOutput(__proc);
        __op.forEachTemp(__proc);
    }

    public Value getValue()
    {
        return value;
    }
}
