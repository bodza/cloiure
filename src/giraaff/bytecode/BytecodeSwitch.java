package giraaff.bytecode;

///
// An abstract class that provides the state and methods common to {@link Bytecodes#LOOKUPSWITCH}
// and {@link Bytecodes#TABLESWITCH} instructions.
///
// @class BytecodeSwitch
public abstract class BytecodeSwitch
{
    ///
    // The {@link BytecodeStream} containing the bytecode array.
    ///
    // @field
    protected final BytecodeStream ___stream;
    ///
    // Index of start of switch instruction.
    ///
    // @field
    protected final int ___bci;
    ///
    // Index of the start of the additional data for the switch instruction, aligned to a multiple
    // of four from the method start.
    ///
    // @field
    protected final int ___alignedBci;

    ///
    // Constructor for a {@link BytecodeStream}.
    //
    // @param stream the {@code BytecodeStream} containing the switch instruction
    // @param bci the index in the stream of the switch instruction
    ///
    // @cons
    public BytecodeSwitch(BytecodeStream __stream, int __bci)
    {
        super();
        this.___stream = __stream;
        this.___bci = __bci;
        this.___alignedBci = (__bci + 4) & 0xfffffffc;
    }

    ///
    // Gets the current bytecode index.
    //
    // @return the current bytecode index
    ///
    public int bci()
    {
        return this.___bci;
    }

    ///
    // Gets the index of the instruction denoted by the {@code i}'th switch target.
    //
    // @param i index of the switch target
    // @return the index of the instruction denoted by the {@code i}'th switch target
    ///
    public int targetAt(int __i)
    {
        return this.___bci + offsetAt(__i);
    }

    ///
    // Gets the index of the instruction for the default switch target.
    //
    // @return the index of the instruction for the default switch target
    ///
    public int defaultTarget()
    {
        return this.___bci + defaultOffset();
    }

    ///
    // Gets the offset from the start of the switch instruction to the default switch target.
    //
    // @return the offset to the default switch target
    ///
    public int defaultOffset()
    {
        return this.___stream.readInt(this.___alignedBci);
    }

    ///
    // Gets the key at {@code i}'th switch target index.
    //
    // @param i the switch target index
    // @return the key at {@code i}'th switch target index
    ///
    public abstract int keyAt(int __i);

    ///
    // Gets the offset from the start of the switch instruction for the {@code i}'th switch target.
    //
    // @param i the switch target index
    // @return the offset to the {@code i}'th switch target
    ///
    public abstract int offsetAt(int __i);

    ///
    // Gets the number of switch targets.
    //
    // @return the number of switch targets
    ///
    public abstract int numberOfCases();

    ///
    // Gets the total size in bytes of the switch instruction.
    //
    // @return the total size in bytes of the switch instruction
    ///
    public abstract int size();
}
