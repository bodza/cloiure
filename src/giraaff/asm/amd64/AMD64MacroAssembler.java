package giraaff.asm.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;

import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.asm.amd64.AMD64AsmOptions;
import giraaff.core.common.NumUtil;

///
// This class implements commonly used X86 code patterns.
///
// @class AMD64MacroAssembler
public final class AMD64MacroAssembler extends AMD64Assembler
{
    // @cons
    public AMD64MacroAssembler(TargetDescription __target)
    {
        super(__target);
    }

    public final void decrementq(Register __reg, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            subq(__reg, __value);
            return;
        }
        if (__value < 0)
        {
            incrementq(__reg, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decq(__reg);
        }
        else
        {
            subq(__reg, __value);
        }
    }

    public final void decrementq(AMD64Address __dst, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            subq(__dst, __value);
            return;
        }
        if (__value < 0)
        {
            incrementq(__dst, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decq(__dst);
        }
        else
        {
            subq(__dst, __value);
        }
    }

    public void incrementq(Register __reg, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            addq(__reg, __value);
            return;
        }
        if (__value < 0)
        {
            decrementq(__reg, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incq(__reg);
        }
        else
        {
            addq(__reg, __value);
        }
    }

    public final void incrementq(AMD64Address __dst, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            addq(__dst, __value);
            return;
        }
        if (__value < 0)
        {
            decrementq(__dst, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incq(__dst);
        }
        else
        {
            addq(__dst, __value);
        }
    }

    public final void movptr(Register __dst, AMD64Address __src)
    {
        movq(__dst, __src);
    }

    public final void movptr(AMD64Address __dst, Register __src)
    {
        movq(__dst, __src);
    }

    public final void movptr(AMD64Address __dst, int __src)
    {
        movslq(__dst, __src);
    }

    public final void cmpptr(Register __src1, Register __src2)
    {
        cmpq(__src1, __src2);
    }

    public final void cmpptr(Register __src1, AMD64Address __src2)
    {
        cmpq(__src1, __src2);
    }

    public final void decrementl(Register __reg)
    {
        decrementl(__reg, 1);
    }

    public final void decrementl(Register __reg, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            subl(__reg, __value);
            return;
        }
        if (__value < 0)
        {
            incrementl(__reg, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decl(__reg);
        }
        else
        {
            subl(__reg, __value);
        }
    }

    public final void decrementl(AMD64Address __dst, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            subl(__dst, __value);
            return;
        }
        if (__value < 0)
        {
            incrementl(__dst, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decl(__dst);
        }
        else
        {
            subl(__dst, __value);
        }
    }

    public final void incrementl(Register __reg, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            addl(__reg, __value);
            return;
        }
        if (__value < 0)
        {
            decrementl(__reg, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incl(__reg);
        }
        else
        {
            addl(__reg, __value);
        }
    }

    public final void incrementl(AMD64Address __dst, int __value)
    {
        if (__value == Integer.MIN_VALUE)
        {
            addl(__dst, __value);
            return;
        }
        if (__value < 0)
        {
            decrementl(__dst, -__value);
            return;
        }
        if (__value == 0)
        {
            return;
        }
        if (__value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incl(__dst);
        }
        else
        {
            addl(__dst, __value);
        }
    }

    ///
    // Non-atomic write of a 64-bit constant to memory.
    // Do not use if the address might be a volatile field!
    ///
    public final void movlong(AMD64Address __dst, long __src)
    {
        if (NumUtil.isInt(__src))
        {
            AMD64MIOp.MOV.emit(this, OperandSize.QWORD, __dst, (int) __src);
        }
        else
        {
            AMD64Address __high = new AMD64Address(__dst.getBase(), __dst.getIndex(), __dst.getScale(), __dst.getDisplacement() + 4);
            movl(__dst, (int) (__src & 0xFFFFFFFF));
            movl(__high, (int) (__src >> 32));
        }
    }

    public final void setl(ConditionFlag __cc, Register __dst)
    {
        setb(__cc, __dst);
        movzbl(__dst, __dst);
    }

    public final void setq(ConditionFlag __cc, Register __dst)
    {
        setb(__cc, __dst);
        movzbq(__dst, __dst);
    }
}
