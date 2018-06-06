package giraaff.bytecode;

///
// A utility for processing {@link Bytecodes#LOOKUPSWITCH} bytecodes.
///
// @class BytecodeLookupSwitch
public final class BytecodeLookupSwitch extends BytecodeSwitch
{
    // @def
    private static final int OFFSET_TO_NUMBER_PAIRS = 4;
    // @def
    private static final int OFFSET_TO_FIRST_PAIR_MATCH = 8;
    // @def
    private static final int OFFSET_TO_FIRST_PAIR_OFFSET = 12;
    // @def
    private static final int PAIR_SIZE = 8;

    ///
    // Constructor for a {@link BytecodeStream}.
    //
    // @param stream the {@code BytecodeStream} containing the switch instruction
    // @param bci the index in the stream of the switch instruction
    ///
    // @cons BytecodeLookupSwitch
    public BytecodeLookupSwitch(BytecodeStream __stream, int __bci)
    {
        super(__stream, __bci);
    }

    @Override
    public int offsetAt(int __i)
    {
        return this.___stream.readInt(this.___alignedBci + OFFSET_TO_FIRST_PAIR_OFFSET + PAIR_SIZE * __i);
    }

    @Override
    public int keyAt(int __i)
    {
        return this.___stream.readInt(this.___alignedBci + OFFSET_TO_FIRST_PAIR_MATCH + PAIR_SIZE * __i);
    }

    @Override
    public int numberOfCases()
    {
        return this.___stream.readInt(this.___alignedBci + OFFSET_TO_NUMBER_PAIRS);
    }

    @Override
    public int size()
    {
        return this.___alignedBci + OFFSET_TO_FIRST_PAIR_MATCH + PAIR_SIZE * numberOfCases() - this.___bci;
    }
}
