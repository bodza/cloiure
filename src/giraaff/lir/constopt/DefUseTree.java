package giraaff.lir.constopt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp.LoadConstantOp;
import giraaff.lir.Variable;

/**
 * Represents def-use tree of a constant.
 */
// @class DefUseTree
final class DefUseTree
{
    // @field
    private final LoadConstantOp instruction;
    // @field
    private final AbstractBlockBase<?> block;
    // @field
    private final List<UseEntry> uses;

    // @cons
    DefUseTree(LIRInstruction __instruction, AbstractBlockBase<?> __block)
    {
        super();
        this.instruction = LoadConstantOp.asLoadConstantOp(__instruction);
        this.block = __block;
        this.uses = new ArrayList<>();
    }

    public Variable getVariable()
    {
        return (Variable) instruction.getResult();
    }

    public Constant getConstant()
    {
        return instruction.getConstant();
    }

    public LIRInstruction getInstruction()
    {
        return (LIRInstruction) instruction;
    }

    public AbstractBlockBase<?> getBlock()
    {
        return block;
    }

    public void addUsage(AbstractBlockBase<?> __b, LIRInstruction __inst, Value __value)
    {
        uses.add(new UseEntry(__b, __inst, __value));
    }

    public int usageCount()
    {
        return uses.size();
    }

    public void forEach(Consumer<? super UseEntry> __action)
    {
        uses.forEach(__action);
    }
}
