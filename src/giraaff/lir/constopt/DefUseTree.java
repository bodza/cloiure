package giraaff.lir.constopt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp;
import giraaff.lir.Variable;

///
// Represents def-use tree of a constant.
///
// @class DefUseTree
final class DefUseTree
{
    // @field
    private final StandardOp.LoadConstantOp ___instruction;
    // @field
    private final AbstractBlockBase<?> ___block;
    // @field
    private final List<UseEntry> ___uses;

    // @cons DefUseTree
    DefUseTree(LIRInstruction __instruction, AbstractBlockBase<?> __block)
    {
        super();
        this.___instruction = StandardOp.LoadConstantOp.asLoadConstantOp(__instruction);
        this.___block = __block;
        this.___uses = new ArrayList<>();
    }

    public Variable getVariable()
    {
        return (Variable) this.___instruction.getResult();
    }

    public Constant getConstant()
    {
        return this.___instruction.getConstant();
    }

    public LIRInstruction getInstruction()
    {
        return (LIRInstruction) this.___instruction;
    }

    public AbstractBlockBase<?> getBlock()
    {
        return this.___block;
    }

    public void addUsage(AbstractBlockBase<?> __b, LIRInstruction __inst, Value __value)
    {
        this.___uses.add(new UseEntry(__b, __inst, __value));
    }

    public int usageCount()
    {
        return this.___uses.size();
    }

    public void forEach(Consumer<? super UseEntry> __action)
    {
        this.___uses.forEach(__action);
    }
}
