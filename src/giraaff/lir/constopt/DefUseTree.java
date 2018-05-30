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
    private final LoadConstantOp instruction;
    private final AbstractBlockBase<?> block;
    private final List<UseEntry> uses;

    // @cons
    DefUseTree(LIRInstruction instruction, AbstractBlockBase<?> block)
    {
        super();
        this.instruction = LoadConstantOp.asLoadConstantOp(instruction);
        this.block = block;
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

    public void addUsage(AbstractBlockBase<?> b, LIRInstruction inst, Value value)
    {
        uses.add(new UseEntry(b, inst, value));
    }

    public int usageCount()
    {
        return uses.size();
    }

    public void forEach(Consumer<? super UseEntry> action)
    {
        uses.forEach(action);
    }
}
