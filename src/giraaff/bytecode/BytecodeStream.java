package giraaff.bytecode;

/**
 * A utility class that makes iterating over bytecodes and reading operands simpler and less error
 * prone. For example, it handles the {@link Bytecodes#WIDE} instruction and wide variants of
 * instructions internally.
 */
// @class BytecodeStream
public final class BytecodeStream
{
    // @field
    private final byte[] code;
    // @field
    private int opcode;
    // @field
    private int curBCI;
    // @field
    private int nextBCI;

    /**
     * Creates a new {@code BytecodeStream} for the specified bytecode.
     *
     * @param code the array of bytes that contains the bytecode
     */
    // @cons
    public BytecodeStream(byte[] __code)
    {
        super();
        this.code = __code;
        setBCI(0);
    }

    /**
     * Advances to the next bytecode.
     */
    public void next()
    {
        setBCI(nextBCI);
    }

    /**
     * Gets the next bytecode index (no side-effects).
     *
     * @return the next bytecode index
     */
    public int nextBCI()
    {
        return nextBCI;
    }

    /**
     * Gets the current bytecode index.
     *
     * @return the current bytecode index
     */
    public int currentBCI()
    {
        return curBCI;
    }

    /**
     * Gets the bytecode index of the end of the code.
     *
     * @return the index of the end of the code
     */
    public int endBCI()
    {
        return code.length;
    }

    /**
     * Gets the current opcode. This method will never return the {@link Bytecodes#WIDE WIDE}
     * opcode, but will instead return the opcode that is modified by the {@code WIDE} opcode.
     *
     * @return the current opcode; {@link Bytecodes#END} if at or beyond the end of the code
     */
    public int currentBC()
    {
        if (opcode == Bytecodes.WIDE)
        {
            return Bytes.beU1(code, curBCI + 1);
        }
        else
        {
            return opcode;
        }
    }

    /**
     * Reads the index of a local variable for one of the load or store instructions. The WIDE
     * modifier is handled internally.
     *
     * @return the index of the local variable
     */
    public int readLocalIndex()
    {
        // read local variable index for load/store
        if (opcode == Bytecodes.WIDE)
        {
            return Bytes.beU2(code, curBCI + 2);
        }
        return Bytes.beU1(code, curBCI + 1);
    }

    /**
     * Read the delta for an {@link Bytecodes#IINC} bytecode.
     *
     * @return the delta for the {@code IINC}
     */
    public int readIncrement()
    {
        // read the delta for the iinc bytecode
        if (opcode == Bytecodes.WIDE)
        {
            return Bytes.beS2(code, curBCI + 4);
        }
        return Bytes.beS1(code, curBCI + 2);
    }

    /**
     * Read the destination of a {@link Bytecodes#GOTO} or {@code IF} instructions.
     *
     * @return the destination bytecode index
     */
    public int readBranchDest()
    {
        // reads the destination for a branch bytecode
        if (opcode == Bytecodes.GOTO_W || opcode == Bytecodes.JSR_W)
        {
            return curBCI + Bytes.beS4(code, curBCI + 1);
        }
        else
        {
            return curBCI + Bytes.beS2(code, curBCI + 1);
        }
    }

    /**
     * Read a signed 4-byte integer from the bytecode stream at the specified bytecode index.
     *
     * @param bci the bytecode index
     * @return the integer value
     */
    public int readInt(int __bci)
    {
        // reads a 4-byte signed value
        return Bytes.beS4(code, __bci);
    }

    /**
     * Reads an unsigned, 1-byte value from the bytecode stream at the specified bytecode index.
     *
     * @param bci the bytecode index
     * @return the byte
     */
    public int readUByte(int __bci)
    {
        return Bytes.beU1(code, __bci);
    }

    /**
     * Reads a constant pool index for the current instruction.
     *
     * @return the constant pool index
     */
    public char readCPI()
    {
        if (opcode == Bytecodes.LDC)
        {
            return (char) Bytes.beU1(code, curBCI + 1);
        }
        return (char) Bytes.beU2(code, curBCI + 1);
    }

    /**
     * Reads a constant pool index for an invokedynamic instruction.
     *
     * @return the constant pool index
     */
    public int readCPI4()
    {
        return Bytes.beS4(code, curBCI + 1);
    }

    /**
     * Reads a signed, 1-byte value for the current instruction (e.g. BIPUSH).
     *
     * @return the byte
     */
    public byte readByte()
    {
        return code[curBCI + 1];
    }

    /**
     * Reads a signed, 2-byte short for the current instruction (e.g. SIPUSH).
     *
     * @return the short value
     */
    public short readShort()
    {
        return (short) Bytes.beS2(code, curBCI + 1);
    }

    /**
     * Sets the bytecode index to the specified value. If {@code bci} is beyond the end of the
     * array, {@link #currentBC} will return {@link Bytecodes#END} and other methods may throw
     * {@link ArrayIndexOutOfBoundsException}.
     *
     * @param bci the new bytecode index
     */
    public void setBCI(int __bci)
    {
        curBCI = __bci;
        if (curBCI < code.length)
        {
            opcode = Bytes.beU1(code, __bci);
            nextBCI = __bci + lengthOf();
        }
        else
        {
            opcode = Bytecodes.END;
            nextBCI = curBCI;
        }
    }

    /**
     * Gets the length of the current bytecode.
     */
    private int lengthOf()
    {
        int __length = Bytecodes.lengthOf(opcode);
        if (__length == 0)
        {
            switch (opcode)
            {
                case Bytecodes.TABLESWITCH:
                {
                    return new BytecodeTableSwitch(this, curBCI).size();
                }
                case Bytecodes.LOOKUPSWITCH:
                {
                    return new BytecodeLookupSwitch(this, curBCI).size();
                }
                case Bytecodes.WIDE:
                {
                    int __opc = Bytes.beU1(code, curBCI + 1);
                    if (__opc == Bytecodes.RET)
                    {
                        return 4;
                    }
                    else if (__opc == Bytecodes.IINC)
                    {
                        return 6;
                    }
                    else
                    {
                        return 4; // a load or store bytecode
                    }
                }
                default:
                    throw new Error("unknown variable-length bytecode: " + opcode);
            }
        }
        return __length;
    }
}
