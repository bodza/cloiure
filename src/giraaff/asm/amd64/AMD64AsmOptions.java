package giraaff.asm.amd64;

// @class AMD64AsmOptions
public final class AMD64AsmOptions
{
    // @cons
    private AMD64AsmOptions()
    {
        super();
    }

    // @def
    public static final boolean UseNormalNop = false;
    // @def
    public static final boolean UseAddressNop = true;
    // @def
    public static final boolean UseIncDec = true;
    // @def
    public static final boolean UseXmmLoadAndClearUpper = true;
    // @def
    public static final boolean UseXmmRegToRegMoveAll = true;
}
