package giraaff.asm.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;

import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.asm.amd64.AMD64AsmOptions;
import giraaff.core.common.NumUtil;

/**
 * This class implements commonly used X86 code patterns.
 */
// @class AMD64MacroAssembler
public final class AMD64MacroAssembler extends AMD64Assembler
{
    // @cons
    public AMD64MacroAssembler(TargetDescription target)
    {
        super(target);
    }

    public final void decrementq(Register reg, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            subq(reg, value);
            return;
        }
        if (value < 0)
        {
            incrementq(reg, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decq(reg);
        }
        else
        {
            subq(reg, value);
        }
    }

    public final void decrementq(AMD64Address dst, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            subq(dst, value);
            return;
        }
        if (value < 0)
        {
            incrementq(dst, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decq(dst);
        }
        else
        {
            subq(dst, value);
        }
    }

    public void incrementq(Register reg, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            addq(reg, value);
            return;
        }
        if (value < 0)
        {
            decrementq(reg, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incq(reg);
        }
        else
        {
            addq(reg, value);
        }
    }

    public final void incrementq(AMD64Address dst, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            addq(dst, value);
            return;
        }
        if (value < 0)
        {
            decrementq(dst, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incq(dst);
        }
        else
        {
            addq(dst, value);
        }
    }

    public final void movptr(Register dst, AMD64Address src)
    {
        movq(dst, src);
    }

    public final void movptr(AMD64Address dst, Register src)
    {
        movq(dst, src);
    }

    public final void movptr(AMD64Address dst, int src)
    {
        movslq(dst, src);
    }

    public final void cmpptr(Register src1, Register src2)
    {
        cmpq(src1, src2);
    }

    public final void cmpptr(Register src1, AMD64Address src2)
    {
        cmpq(src1, src2);
    }

    public final void decrementl(Register reg)
    {
        decrementl(reg, 1);
    }

    public final void decrementl(Register reg, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            subl(reg, value);
            return;
        }
        if (value < 0)
        {
            incrementl(reg, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decl(reg);
        }
        else
        {
            subl(reg, value);
        }
    }

    public final void decrementl(AMD64Address dst, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            subl(dst, value);
            return;
        }
        if (value < 0)
        {
            incrementl(dst, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            decl(dst);
        }
        else
        {
            subl(dst, value);
        }
    }

    public final void incrementl(Register reg, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            addl(reg, value);
            return;
        }
        if (value < 0)
        {
            decrementl(reg, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incl(reg);
        }
        else
        {
            addl(reg, value);
        }
    }

    public final void incrementl(AMD64Address dst, int value)
    {
        if (value == Integer.MIN_VALUE)
        {
            addl(dst, value);
            return;
        }
        if (value < 0)
        {
            decrementl(dst, -value);
            return;
        }
        if (value == 0)
        {
            return;
        }
        if (value == 1 && AMD64AsmOptions.UseIncDec)
        {
            incl(dst);
        }
        else
        {
            addl(dst, value);
        }
    }

    public void movflt(Register dst, Register src)
    {
        if (AMD64AsmOptions.UseXmmRegToRegMoveAll)
        {
            movaps(dst, src);
        }
        else
        {
            movss(dst, src);
        }
    }

    public void movflt(Register dst, AMD64Address src)
    {
        movss(dst, src);
    }

    public void movflt(AMD64Address dst, Register src)
    {
        movss(dst, src);
    }

    public void movdbl(Register dst, Register src)
    {
        if (AMD64AsmOptions.UseXmmRegToRegMoveAll)
        {
            movapd(dst, src);
        }
        else
        {
            movsd(dst, src);
        }
    }

    public void movdbl(Register dst, AMD64Address src)
    {
        if (AMD64AsmOptions.UseXmmLoadAndClearUpper)
        {
            movsd(dst, src);
        }
        else
        {
            movlpd(dst, src);
        }
    }

    public void movdbl(AMD64Address dst, Register src)
    {
        movsd(dst, src);
    }

    /**
     * Non-atomic write of a 64-bit constant to memory.
     * Do not use if the address might be a volatile field!
     */
    public final void movlong(AMD64Address dst, long src)
    {
        if (NumUtil.isInt(src))
        {
            AMD64MIOp.MOV.emit(this, OperandSize.QWORD, dst, (int) src);
        }
        else
        {
            AMD64Address high = new AMD64Address(dst.getBase(), dst.getIndex(), dst.getScale(), dst.getDisplacement() + 4);
            movl(dst, (int) (src & 0xFFFFFFFF));
            movl(high, (int) (src >> 32));
        }
    }

    public final void setl(ConditionFlag cc, Register dst)
    {
        setb(cc, dst);
        movzbl(dst, dst);
    }

    public final void setq(ConditionFlag cc, Register dst)
    {
        setb(cc, dst);
        movzbq(dst, dst);
    }

    public final void fpop()
    {
        ffree(0);
        fincstp();
    }
}
