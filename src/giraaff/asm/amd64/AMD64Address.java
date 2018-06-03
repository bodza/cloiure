package giraaff.asm.amd64;

import jdk.vm.ci.code.Register;

import giraaff.asm.AbstractAddress;

/**
 * Represents an address in target machine memory, specified via some combination of a base
 * register, an index register, a displacement and a scale. Note that the base and index registers
 * may be a variable that will get a register assigned later by the register allocator.
 */
// @class AMD64Address
public final class AMD64Address extends AbstractAddress
{
    // @field
    private final Register base;
    // @field
    private final Register index;
    // @field
    private final Scale scale;
    // @field
    private final int displacement;

    /**
     * The start of the instruction, i.e., the value that is used as the key for looking up
     * placeholder patching information. Only used for {@link AMD64Assembler#getPlaceholder
     * placeholder addresses}.
     */
    // @field
    final int instructionStartPosition;

    /**
     * Creates an {@link AMD64Address} with given base register, no scaling and no displacement.
     *
     * @param base the base register
     */
    // @cons
    public AMD64Address(Register __base)
    {
        this(__base, Register.None, Scale.Times1, 0);
    }

    /**
     * Creates an {@link AMD64Address} with given base register, no scaling and a given displacement.
     *
     * @param base the base register
     * @param displacement the displacement
     */
    // @cons
    public AMD64Address(Register __base, int __displacement)
    {
        this(__base, Register.None, Scale.Times1, __displacement);
    }

    /**
     * Creates an {@link AMD64Address} with given base and index registers, scaling and 0 displacement.
     *
     * @param base the base register
     * @param index the index register
     * @param scale the scaling factor
     */
    // @cons
    public AMD64Address(Register __base, Register __index, Scale __scale)
    {
        this(__base, __index, __scale, 0, -1);
    }

    /**
     * Creates an {@link AMD64Address} with given base and index registers, scaling and
     * displacement. This is the most general constructor.
     *
     * @param base the base register
     * @param index the index register
     * @param scale the scaling factor
     * @param displacement the displacement
     */
    // @cons
    public AMD64Address(Register __base, Register __index, Scale __scale, int __displacement)
    {
        this(__base, __index, __scale, __displacement, -1);
    }

    // @cons
    AMD64Address(Register __base, Register __index, Scale __scale, int __displacement, int __instructionStartPosition)
    {
        super();
        this.base = __base;
        this.index = __index;
        this.scale = __scale;
        this.displacement = __displacement;
        this.instructionStartPosition = __instructionStartPosition;
    }

    /**
     * A scaling factor used in the SIB addressing mode.
     */
    // @enum AMD64Address.Scale
    public enum Scale
    {
        Times1(1, 0),
        Times2(2, 1),
        Times4(4, 2),
        Times8(8, 3);

        Scale(int __value, int __log2)
        {
            this.value = __value;
            this.log2 = __log2;
        }

        /**
         * The value (or multiplier) of this scale.
         */
        // @field
        public final int value;

        /**
         * The {@linkplain #value value} of this scale log 2.
         */
        // @field
        public final int log2;

        public static Scale fromInt(int __scale)
        {
            switch (__scale)
            {
                case 1:
                    return Times1;
                case 2:
                    return Times2;
                case 4:
                    return Times4;
                case 8:
                    return Times8;
                default:
                    return null;
            }
        }

        public static Scale fromShift(int __shift)
        {
            switch (__shift)
            {
                case 0:
                    return Times1;
                case 1:
                    return Times2;
                case 2:
                    return Times4;
                case 3:
                    return Times8;
                default:
                    return null;
            }
        }
    }

    /**
     * @return Base register that defines the start of the address computation. If not present, is
     *         denoted by {@link Register#None}.
     */
    public Register getBase()
    {
        return base;
    }

    /**
     * @return Index register, the value of which (possibly scaled by {@link #getScale}) is added to
     *         {@link #getBase}. If not present, is denoted by {@link Register#None}.
     */
    public Register getIndex()
    {
        return index;
    }

    /**
     * @return Scaling factor for indexing, dependent on target operand size.
     */
    public Scale getScale()
    {
        return scale;
    }

    /**
     * @return Optional additive displacement.
     */
    public int getDisplacement()
    {
        return displacement;
    }
}
