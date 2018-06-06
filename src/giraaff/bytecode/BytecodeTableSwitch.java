package giraaff.bytecode;

///
// A utility for processing {@link Bytecodes#TABLESWITCH} bytecodes.
///
// @class BytecodeTableSwitch
public final class BytecodeTableSwitch extends BytecodeSwitch
{
    // @def
    private static final int OFFSET_TO_LOW_KEY = 4;
    // @def
    private static final int OFFSET_TO_HIGH_KEY = 8;
    // @def
    private static final int OFFSET_TO_FIRST_JUMP_OFFSET = 12;
    // @def
    private static final int JUMP_OFFSET_SIZE = 4;

    ///
    // Constructor for a {@link BytecodeStream}.
    //
    // @param stream the {@code BytecodeStream} containing the switch instruction
    // @param bci the index in the stream of the switch instruction
    ///
    // @cons BytecodeTableSwitch
    public BytecodeTableSwitch(BytecodeStream __stream, int __bci)
    {
        super(__stream, __bci);
    }

    ///
    // Gets the low key of the table switch.
    //
    // @return the low key
    ///
    public int lowKey()
    {
        return this.___stream.readInt(this.___alignedBci + OFFSET_TO_LOW_KEY);
    }

    ///
    // Gets the high key of the table switch.
    //
    // @return the high key
    ///
    public int highKey()
    {
        return this.___stream.readInt(this.___alignedBci + OFFSET_TO_HIGH_KEY);
    }

    @Override
    public int keyAt(int __i)
    {
        return lowKey() + __i;
    }

    @Override
    public int offsetAt(int __i)
    {
        return this.___stream.readInt(this.___alignedBci + OFFSET_TO_FIRST_JUMP_OFFSET + JUMP_OFFSET_SIZE * __i);
    }

    @Override
    public int numberOfCases()
    {
        return highKey() - lowKey() + 1;
    }

    @Override
    public int size()
    {
        return this.___alignedBci + OFFSET_TO_FIRST_JUMP_OFFSET + JUMP_OFFSET_SIZE * numberOfCases() - this.___bci;
    }
}
