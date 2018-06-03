package giraaff.word;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;
import org.graalvm.word.SignedWord;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordBase;

import giraaff.word.Word.Opcode;
import giraaff.word.Word.Operation;

///
// Low-level memory access for Objects. Similarly to the readXxx and writeXxx methods defined for
// {@link Pointer}, these methods access the raw memory without any null checks, read- or write
// barriers. When the VM uses compressed pointers, then readObject and writeObject methods access
// compressed pointers.
///
// @class ObjectAccess
public final class ObjectAccess
{
    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native byte readByte(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native char readChar(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native short readShort(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native int readInt(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native long readLong(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native float readFloat(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native double readDouble(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native <T extends WordBase> T readWord(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native Object readObject(Object __object, WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native byte readByte(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native char readChar(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native short readShort(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native int readInt(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native long readLong(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native float readFloat(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native double readDouble(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native <T extends WordBase> T readWord(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native Object readObject(Object __object, int __offset, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeByte(Object __object, WordBase __offset, byte __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeChar(Object __object, WordBase __offset, char __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeShort(Object __object, WordBase __offset, short __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeInt(Object __object, WordBase __offset, int __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeLong(Object __object, WordBase __offset, long __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeFloat(Object __object, WordBase __offset, float __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeDouble(Object __object, WordBase __offset, double __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeWord(Object __object, WordBase __offset, WordBase __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeObject(Object __object, WordBase __offset, Object __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeByte(Object __object, int __offset, byte __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeChar(Object __object, int __offset, char __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeShort(Object __object, int __offset, short __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeInt(Object __object, int __offset, int __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeLong(Object __object, int __offset, long __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeFloat(Object __object, int __offset, float __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeDouble(Object __object, int __offset, double __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeWord(Object __object, int __offset, WordBase __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeObject(Object __object, int __offset, Object __val, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native byte readByte(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native char readChar(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native short readShort(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native int readInt(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native long readLong(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native float readFloat(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native double readDouble(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native <T extends WordBase> T readWord(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native Object readObject(Object __object, WordBase __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native byte readByte(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native char readChar(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native short readShort(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native int readInt(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native long readLong(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native float readFloat(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native double readDouble(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native <T extends WordBase> T readWord(Object __object, int __offset);

    ///
    // Reads the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_OBJECT)
    public static native Object readObject(Object __object, int __offset);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeByte(Object __object, WordBase __offset, byte __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeChar(Object __object, WordBase __offset, char __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeShort(Object __object, WordBase __offset, short __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeInt(Object __object, WordBase __offset, int __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeLong(Object __object, WordBase __offset, long __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeFloat(Object __object, WordBase __offset, float __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeDouble(Object __object, WordBase __offset, double __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeWord(Object __object, WordBase __offset, WordBase __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeObject(Object __object, WordBase __offset, Object __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeByte(Object __object, int __offset, byte __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeChar(Object __object, int __offset, char __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeShort(Object __object, int __offset, short __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeInt(Object __object, int __offset, int __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeLong(Object __object, int __offset, long __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeFloat(Object __object, int __offset, float __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeDouble(Object __object, int __offset, double __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeWord(Object __object, int __offset, WordBase __val);

    ///
    // Writes the memory at address {@code (object + offset)}. The offset is in bytes.
    //
    // @param object the base object for the memory access
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_OBJECT)
    public static native void writeObject(Object __object, int __offset, Object __val);
}
