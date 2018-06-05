package giraaff.core.amd64;

import jdk.vm.ci.amd64.AMD64Kind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.util.GraalError;

// @class AMD64LIRKindTool
public abstract class AMD64LIRKindTool implements LIRKindTool
{
    @Override
    public LIRKind getIntegerKind(int __bits)
    {
        if (__bits <= 8)
        {
            return LIRKind.value(AMD64Kind.BYTE);
        }
        else if (__bits <= 16)
        {
            return LIRKind.value(AMD64Kind.WORD);
        }
        else if (__bits <= 32)
        {
            return LIRKind.value(AMD64Kind.DWORD);
        }
        else
        {
            return LIRKind.value(AMD64Kind.QWORD);
        }
    }

    @Override
    public LIRKind getObjectKind()
    {
        return LIRKind.reference(AMD64Kind.QWORD);
    }

    @Override
    public LIRKind getWordKind()
    {
        return LIRKind.value(AMD64Kind.QWORD);
    }

    @Override
    public abstract LIRKind getNarrowOopKind();

    @Override
    public abstract LIRKind getNarrowPointerKind();
}
