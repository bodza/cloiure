package cloiure.asm.commons;

import cloiure.asm.Handle;
import cloiure.asm.Label;
import cloiure.asm.MethodVisitor;
import cloiure.asm.Opcodes;

/**
 * A {@link MethodVisitor} that can be used to approximate method size.
 *
 * @author Eugene Kuleshov
 */
public class CodeSizeEvaluator extends MethodVisitor implements Opcodes
{
    private int minSize;

    private int maxSize;

    public CodeSizeEvaluator(final MethodVisitor mv)
    {
        this(Opcodes.ASM4, mv);
    }

    protected CodeSizeEvaluator(final int api, final MethodVisitor mv)
    {
        super(api, mv);
    }

    public int getMinSize()
    {
        return this.minSize;
    }

    public int getMaxSize()
    {
        return this.maxSize;
    }

    @Override
    public void visitInsn(final int opcode)
    {
        minSize += 1;
        maxSize += 1;
        if (mv != null)
        {
            mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand)
    {
        if (opcode == SIPUSH)
        {
            minSize += 3;
            maxSize += 3;
        }
        else
        {
            minSize += 2;
            maxSize += 2;
        }
        if (mv != null)
        {
            mv.visitIntInsn(opcode, operand);
        }
    }

    @Override
    public void visitVarInsn(final int opcode, final int var)
    {
        if (var < 4 && opcode != RET)
        {
            minSize += 1;
            maxSize += 1;
        }
        else if (var >= 256)
        {
            minSize += 4;
            maxSize += 4;
        }
        else
        {
            minSize += 2;
            maxSize += 2;
        }
        if (mv != null)
        {
            mv.visitVarInsn(opcode, var);
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type)
    {
        minSize += 3;
        maxSize += 3;
        if (mv != null)
        {
            mv.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc)
    {
        minSize += 3;
        maxSize += 3;
        if (mv != null)
        {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc)
    {
        if (opcode == INVOKEINTERFACE)
        {
            minSize += 5;
            maxSize += 5;
        }
        else
        {
            minSize += 3;
            maxSize += 3;
        }
        if (mv != null)
        {
            mv.visitMethodInsn(opcode, owner, name, desc);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs)
    {
        minSize += 5;
        maxSize += 5;
        if (mv != null)
        {
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label)
    {
        minSize += 3;
        if (opcode == GOTO || opcode == JSR)
        {
            maxSize += 5;
        }
        else
        {
            maxSize += 8;
        }
        if (mv != null)
        {
            mv.visitJumpInsn(opcode, label);
        }
    }

    @Override
    public void visitLdcInsn(final Object cst)
    {
        if (cst instanceof Long || cst instanceof Double)
        {
            minSize += 3;
            maxSize += 3;
        }
        else
        {
            minSize += 2;
            maxSize += 3;
        }
        if (mv != null)
        {
            mv.visitLdcInsn(cst);
        }
    }

    @Override
    public void visitIincInsn(final int var, final int increment)
    {
        if (var > 255 || increment > 127 || increment < -128)
        {
            minSize += 6;
            maxSize += 6;
        }
        else
        {
            minSize += 3;
            maxSize += 3;
        }
        if (mv != null)
        {
            mv.visitIincInsn(var, increment);
        }
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels)
    {
        minSize += 13 + labels.length * 4;
        maxSize += 16 + labels.length * 4;
        if (mv != null)
        {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels)
    {
        minSize += 9 + keys.length * 8;
        maxSize += 12 + keys.length * 8;
        if (mv != null)
        {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims)
    {
        minSize += 4;
        maxSize += 4;
        if (mv != null)
        {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
    }
}
