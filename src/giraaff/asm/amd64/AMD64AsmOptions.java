package giraaff.asm.amd64;

// @class AMD64AsmOptions
public final class AMD64AsmOptions
{
    public static final boolean UseNormalNop = false;
    public static final boolean UseAddressNop = true;
    public static final boolean UseIncDec = true;
    public static final boolean UseXmmLoadAndClearUpper = true;
    public static final boolean UseXmmRegToRegMoveAll = true;

    // @cons
    private AMD64AsmOptions()
    {
        super();
    }
}
