package giraaff.lir.constopt;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.ValueProcedure;

///
// Represents a usage of a constant.
///
// @class UseEntry
final class UseEntry
{
    // @field
    private final AbstractBlockBase<?> ___block;
    // @field
    private final LIRInstruction ___instruction;
    // @field
    private final Value ___value;

    // @cons
    UseEntry(AbstractBlockBase<?> __block, LIRInstruction __instruction, Value __value)
    {
        super();
        this.___block = __block;
        this.___instruction = __instruction;
        this.___value = __value;
    }

    public LIRInstruction getInstruction()
    {
        return this.___instruction;
    }

    public AbstractBlockBase<?> getBlock()
    {
        return this.___block;
    }

    public void setValue(Value __newValue)
    {
        replaceValue(this.___instruction, this.___value, __newValue);
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
        return this.___value;
    }
}
