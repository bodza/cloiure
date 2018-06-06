package giraaff.bytecode;

///
// A collection of utility methods for dealing with bytes, particularly in byte arrays.
///
// @class Bytes
public final class Bytes
{
    // @cons Bytes
    private Bytes()
    {
        super();
    }

    ///
    // Gets a signed 1-byte value.
    //
    // @param data the array containing the data
    // @param bci the start index of the value to retrieve
    // @return the signed 1-byte value at index {@code bci} in array {@code data}
    ///
    public static int beS1(byte[] __data, int __bci)
    {
        return __data[__bci];
    }

    ///
    // Gets a signed 2-byte big-endian value.
    //
    // @param data the array containing the data
    // @param bci the start index of the value to retrieve
    // @return the signed 2-byte, big-endian, value at index {@code bci} in array {@code data}
    ///
    public static int beS2(byte[] __data, int __bci)
    {
        return (__data[__bci] << 8) | (__data[__bci + 1] & 0xff);
    }

    ///
    // Gets an unsigned 1-byte value.
    //
    // @param data the array containing the data
    // @param bci the start index of the value to retrieve
    // @return the unsigned 1-byte value at index {@code bci} in array {@code data}
    ///
    public static int beU1(byte[] __data, int __bci)
    {
        return __data[__bci] & 0xff;
    }

    ///
    // Gets an unsigned 2-byte big-endian value.
    //
    // @param data the array containing the data
    // @param bci the start index of the value to retrieve
    // @return the unsigned 2-byte, big-endian, value at index {@code bci} in array {@code data}
    ///
    public static int beU2(byte[] __data, int __bci)
    {
        return ((__data[__bci] & 0xff) << 8) | (__data[__bci + 1] & 0xff);
    }

    ///
    // Gets a signed 4-byte big-endian value.
    //
    // @param data the array containing the data
    // @param bci the start index of the value to retrieve
    // @return the signed 4-byte, big-endian, value at index {@code bci} in array {@code data}
    ///
    public static int beS4(byte[] __data, int __bci)
    {
        return (__data[__bci] << 24) | ((__data[__bci + 1] & 0xff) << 16) | ((__data[__bci + 2] & 0xff) << 8) | (__data[__bci + 3] & 0xff);
    }

    ///
    // Gets either a signed 2-byte or a signed 4-byte big-endian value.
    //
    // @param data the array containing the data
    // @param bci the start index of the value to retrieve
    // @param fourByte if true, this method will return a 4-byte value
    // @return the signed 2 or 4-byte, big-endian, value at index {@code bci} in array {@code data}
    ///
    public static int beSVar(byte[] __data, int __bci, boolean __fourByte)
    {
        if (__fourByte)
        {
            return beS4(__data, __bci);
        }
        else
        {
            return beS2(__data, __bci);
        }
    }
}
