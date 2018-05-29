package giraaff.core.amd64;

import jdk.vm.ci.amd64.AMD64Kind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.util.GraalError;

// @class AMD64LIRKindTool
public abstract class AMD64LIRKindTool implements LIRKindTool
{
    @Override
    public LIRKind getIntegerKind(int bits)
    {
        if (bits <= 8)
        {
            return LIRKind.value(AMD64Kind.BYTE);
        }
        else if (bits <= 16)
        {
            return LIRKind.value(AMD64Kind.WORD);
        }
        else if (bits <= 32)
        {
            return LIRKind.value(AMD64Kind.DWORD);
        }
        else
        {
            return LIRKind.value(AMD64Kind.QWORD);
        }
    }

    @Override
    public LIRKind getFloatingKind(int bits)
    {
        switch (bits)
        {
            case 32:
                return LIRKind.value(AMD64Kind.SINGLE);
            case 64:
                return LIRKind.value(AMD64Kind.DOUBLE);
            default:
                throw GraalError.shouldNotReachHere();
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
