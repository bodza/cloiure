package giraaff.hotspot.word;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;
import org.graalvm.word.SignedWord;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordBase;

import giraaff.hotspot.word.HotSpotOperation.HotspotOpcode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.word.Word;
import giraaff.word.Word.Opcode;
import giraaff.word.Word.Operation;

///
// Marker type for a metaspace pointer.
///
// @class MetaspacePointer
public abstract class MetaspacePointer
{
    @HotSpotOperation(opcode = HotspotOpcode.IS_NULL)
    public abstract boolean isNull();

    @HotSpotOperation(opcode = HotspotOpcode.FROM_POINTER)
    public abstract Pointer asWord();

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract byte readByte(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract char readChar(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract short readShort(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract int readInt(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract long readLong(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract float readFloat(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract double readDouble(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Word readWord(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Object readObject(WordBase __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract byte readByte(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract char readChar(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract short readShort(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract int readInt(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract long readLong(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract float readFloat(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract double readDouble(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Word readWord(int __offset, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the read
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Object readObject(int __offset, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeByte(WordBase __offset, byte __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeChar(WordBase __offset, char __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeShort(WordBase __offset, short __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeInt(WordBase __offset, int __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeLong(WordBase __offset, long __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeFloat(WordBase __offset, float __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeDouble(WordBase __offset, double __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeWord(WordBase __offset, WordBase __val, LocationIdentity __locationIdentity);

    ///
    // Initializes the memory at address {@code (this + offset)}. Both the base address and offset
    // are in bytes. The memory must be uninitialized or zero prior to this operation.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.INITIALIZE)
    public abstract void initializeLong(WordBase __offset, long __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeObject(WordBase __offset, Object __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeByte(int __offset, byte __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeChar(int __offset, char __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeShort(int __offset, short __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeInt(int __offset, int __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeLong(int __offset, long __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeFloat(int __offset, float __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeDouble(int __offset, double __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeWord(int __offset, WordBase __val, LocationIdentity __locationIdentity);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param locationIdentity the identity of the write
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeObject(int __offset, Object __val, LocationIdentity __locationIdentity);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract byte readByte(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract char readChar(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract short readShort(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract int readInt(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract long readLong(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract float readFloat(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract double readDouble(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Word readWord(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Object readObject(WordBase __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. This access will decompress the oop if
    // the VM uses compressed oops, and it can be parameterized to allow read barriers (G1 referent field).
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param barrierType the type of the read barrier to be added
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Object readObject(WordBase __offset, BarrierType __barrierType);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract byte readByte(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract char readChar(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract short readShort(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract int readInt(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract long readLong(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract float readFloat(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract double readDouble(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Word readWord(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Object readObject(int __offset);

    ///
    // Reads the memory at address {@code (this + offset)}. This access will decompress the oop if
    // the VM uses compressed oops, and it can be parameterized to allow read barriers (G1 referent field).
    //
    // @param offset the signed offset for the memory access
    // @param barrierType the type of the read barrier to be added
    // @return the result of the memory access
    ///
    @Operation(opcode = Opcode.READ_POINTER)
    public abstract Object readObject(int __offset, BarrierType __barrierType);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeByte(WordBase __offset, byte __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeChar(WordBase __offset, char __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeShort(WordBase __offset, short __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeInt(WordBase __offset, int __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeLong(WordBase __offset, long __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeFloat(WordBase __offset, float __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeDouble(WordBase __offset, double __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeWord(WordBase __offset, WordBase __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // The offset is always treated as a {@link SignedWord} value. However, the static type is
    // {@link WordBase} to avoid the frequent casts of {@link UnsignedWord} values (where the caller
    // knows that the highest-order bit of the unsigned value is never used).
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeObject(WordBase __offset, Object __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeByte(int __offset, byte __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeChar(int __offset, char __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeShort(int __offset, short __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeInt(int __offset, int __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeLong(int __offset, long __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeFloat(int __offset, float __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeDouble(int __offset, double __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeWord(int __offset, WordBase __val);

    ///
    // Writes the memory at address {@code (this + offset)}. Both the base address and offset are in bytes.
    //
    // @param offset the signed offset for the memory access
    // @param val the value to be written to memory
    ///
    @Operation(opcode = Opcode.WRITE_POINTER)
    public abstract void writeObject(int __offset, Object __val);
}
