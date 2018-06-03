package giraaff.core.common.calc;

import giraaff.util.GraalError;

// @enum FloatConvert
public enum FloatConvert
{
    F2I(FloatConvertCategory.FloatingPointToInteger, 32),
    D2I(FloatConvertCategory.FloatingPointToInteger, 64),
    F2L(FloatConvertCategory.FloatingPointToInteger, 32),
    D2L(FloatConvertCategory.FloatingPointToInteger, 64),
    I2F(FloatConvertCategory.IntegerToFloatingPoint, 32),
    L2F(FloatConvertCategory.IntegerToFloatingPoint, 64),
    D2F(FloatConvertCategory.FloatingPointToFloatingPoint, 64),
    I2D(FloatConvertCategory.IntegerToFloatingPoint, 32),
    L2D(FloatConvertCategory.IntegerToFloatingPoint, 64),
    F2D(FloatConvertCategory.FloatingPointToFloatingPoint, 32);

    // @field
    private final FloatConvertCategory category;
    // @field
    private final int inputBits;

    FloatConvert(FloatConvertCategory __category, int __inputBits)
    {
        this.category = __category;
        this.inputBits = __inputBits;
    }

    public FloatConvertCategory getCategory()
    {
        return category;
    }

    public FloatConvert reverse()
    {
        switch (this)
        {
            case D2F:
                return F2D;
            case D2I:
                return I2D;
            case D2L:
                return L2D;
            case F2D:
                return D2F;
            case F2I:
                return I2F;
            case F2L:
                return L2F;
            case I2D:
                return D2I;
            case I2F:
                return F2I;
            case L2D:
                return D2L;
            case L2F:
                return F2L;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public int getInputBits()
    {
        return inputBits;
    }
}
