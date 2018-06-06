package giraaff.word;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.graalvm.word.ComparableWord;
import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;
import org.graalvm.word.SignedWord;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;
import org.graalvm.word.impl.WordBoxFactory;

import giraaff.core.common.calc.Condition;
import giraaff.core.common.calc.UnsignedMath;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.AndNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.calc.MulNode;
import giraaff.nodes.calc.OrNode;
import giraaff.nodes.calc.RightShiftNode;
import giraaff.nodes.calc.SignedDivNode;
import giraaff.nodes.calc.SignedRemNode;
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.calc.UnsignedDivNode;
import giraaff.nodes.calc.UnsignedRemNode;
import giraaff.nodes.calc.UnsignedRightShiftNode;
import giraaff.nodes.calc.XorNode;
import giraaff.nodes.memory.HeapAccess;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;

// @class Word
public abstract class Word implements SignedWord, UnsignedWord, Pointer
{
    static
    {
        Word.BoxFactoryImpl.initialize();
    }

    public static void ensureInitialized()
    {
        // Calling this method ensures that the static initializer has been executed.
    }

    ///
    // Links a method to a canonical operation represented by an {@link Word.WordOpcode} val.
    ///
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    // @iface Word.Operation
    public @interface Operation
    {
        Class<? extends ValueNode> node() default ValueNode.class;

        boolean rightOperandIsInt() default false;

        Word.WordOpcode opcode() default Word.WordOpcode.NODE_CLASS;

        Condition condition() default Condition.EQ;
    }

    ///
    // The canonical {@link Word.Operation} represented by a method in the {@link Word} class.
    ///
    // @enum Word.WordOpcode
    public enum WordOpcode
    {
        NODE_CLASS,
        COMPARISON,
        IS_NULL,
        IS_NON_NULL,
        NOT,
        READ_POINTER,
        READ_OBJECT,
        READ_BARRIERED,
        READ_HEAP,
        WRITE_POINTER,
        WRITE_OBJECT,
        WRITE_BARRIERED,
        CAS_POINTER,
        INITIALIZE,
        FROM_ADDRESS,
        OBJECT_TO_TRACKED,
        OBJECT_TO_UNTRACKED,
        TO_OBJECT,
        TO_OBJECT_NON_NULL,
        TO_RAW_VALUE,
    }

    // @class Word.BoxFactoryImpl
    static final class BoxFactoryImpl extends WordBoxFactory
    {
        static void initialize()
        {
            boxFactory = new Word.BoxFactoryImpl();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends WordBase> T boxImpl(long __val)
        {
            return (T) HostedWord.boxLong(__val);
        }
    }

    // Outside users must use the different signed() and unsigned() methods to ensure proper
    // expansion of 32-bit values on 64-bit systems.
    @SuppressWarnings("unchecked")
    private static <T extends WordBase> T box(long __val)
    {
        return (T) HostedWord.boxLong(__val);
    }

    protected abstract long unbox();

    private static Word intParam(int __val)
    {
        return box(__val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.TO_RAW_VALUE)
    public long rawValue()
    {
        return unbox();
    }

    ///
    // Convert an {@link Object} to a {@link Pointer}, keeping the reference information. If the
    // returned pointer or any value derived from it is alive across a safepoint, it will be
    // tracked. Depending on the arithmetic on the pointer and the capabilities of the backend to
    // deal with derived references, this may work correctly, or result in a compiler error.
    ///
    @Word.Operation(opcode = Word.WordOpcode.OBJECT_TO_TRACKED)
    public static native Word objectToTrackedPointer(Object __val);

    ///
    // Convert an {@link Object} to a {@link Pointer}, dropping the reference information. If the
    // returned pointer or any value derived from it is alive across a safepoint, it will be treated
    // as a simple integer and not tracked by the garbage collector.
    //
    // This is a dangerous operation, the GC could move the object without updating the pointer! Use
    // only in combination with some mechanism to prevent the GC from moving or freeing the object
    // as long as the pointer is in use.
    //
    // If the result value should not be alive across a safepoint, it's better to use
    // {@link #objectToTrackedPointer(Object)} instead.
    ///
    @Word.Operation(opcode = Word.WordOpcode.OBJECT_TO_UNTRACKED)
    public static native Word objectToUntrackedPointer(Object __val);

    @Word.Operation(opcode = Word.WordOpcode.FROM_ADDRESS)
    public static native Word fromAddress(AddressNode.Address __address);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.TO_OBJECT)
    public native Object toObject();

    @Override
    @Word.Operation(opcode = Word.WordOpcode.TO_OBJECT_NON_NULL)
    public native Object toObjectNonNull();

    @Override
    @Word.Operation(node = AddNode.class)
    public Word add(SignedWord __val)
    {
        return add((Word) __val);
    }

    @Override
    @Word.Operation(node = AddNode.class)
    public Word add(UnsignedWord __val)
    {
        return add((Word) __val);
    }

    @Override
    @Word.Operation(node = AddNode.class)
    public Word add(int __val)
    {
        return add(intParam(__val));
    }

    @Word.Operation(node = AddNode.class)
    public Word add(Word __val)
    {
        return box(unbox() + __val.unbox());
    }

    @Override
    @Word.Operation(node = SubNode.class)
    public Word subtract(SignedWord __val)
    {
        return subtract((Word) __val);
    }

    @Override
    @Word.Operation(node = SubNode.class)
    public Word subtract(UnsignedWord __val)
    {
        return subtract((Word) __val);
    }

    @Override
    @Word.Operation(node = SubNode.class)
    public Word subtract(int __val)
    {
        return subtract(intParam(__val));
    }

    @Word.Operation(node = SubNode.class)
    public Word subtract(Word __val)
    {
        return box(unbox() - __val.unbox());
    }

    @Override
    @Word.Operation(node = MulNode.class)
    public Word multiply(SignedWord __val)
    {
        return multiply((Word) __val);
    }

    @Override
    @Word.Operation(node = MulNode.class)
    public Word multiply(UnsignedWord __val)
    {
        return multiply((Word) __val);
    }

    @Override
    @Word.Operation(node = MulNode.class)
    public Word multiply(int __val)
    {
        return multiply(intParam(__val));
    }

    @Word.Operation(node = MulNode.class)
    public Word multiply(Word __val)
    {
        return box(unbox() * __val.unbox());
    }

    @Override
    @Word.Operation(node = SignedDivNode.class)
    public Word signedDivide(SignedWord __val)
    {
        return signedDivide((Word) __val);
    }

    @Override
    @Word.Operation(node = SignedDivNode.class)
    public Word signedDivide(int __val)
    {
        return signedDivide(intParam(__val));
    }

    @Word.Operation(node = SignedDivNode.class)
    public Word signedDivide(Word __val)
    {
        return box(unbox() / __val.unbox());
    }

    @Override
    @Word.Operation(node = UnsignedDivNode.class)
    public Word unsignedDivide(UnsignedWord __val)
    {
        return unsignedDivide((Word) __val);
    }

    @Override
    @Word.Operation(node = UnsignedDivNode.class)
    public Word unsignedDivide(int __val)
    {
        return signedDivide(intParam(__val));
    }

    @Word.Operation(node = UnsignedDivNode.class)
    public Word unsignedDivide(Word __val)
    {
        return box(Long.divideUnsigned(unbox(), __val.unbox()));
    }

    @Override
    @Word.Operation(node = SignedRemNode.class)
    public Word signedRemainder(SignedWord __val)
    {
        return signedRemainder((Word) __val);
    }

    @Override
    @Word.Operation(node = SignedRemNode.class)
    public Word signedRemainder(int __val)
    {
        return signedRemainder(intParam(__val));
    }

    @Word.Operation(node = SignedRemNode.class)
    public Word signedRemainder(Word __val)
    {
        return box(unbox() % __val.unbox());
    }

    @Override
    @Word.Operation(node = UnsignedRemNode.class)
    public Word unsignedRemainder(UnsignedWord __val)
    {
        return unsignedRemainder((Word) __val);
    }

    @Override
    @Word.Operation(node = UnsignedRemNode.class)
    public Word unsignedRemainder(int __val)
    {
        return signedRemainder(intParam(__val));
    }

    @Word.Operation(node = UnsignedRemNode.class)
    public Word unsignedRemainder(Word __val)
    {
        return box(Long.remainderUnsigned(unbox(), __val.unbox()));
    }

    @Override
    @Word.Operation(node = LeftShiftNode.class, rightOperandIsInt = true)
    public Word shiftLeft(UnsignedWord __val)
    {
        return shiftLeft((Word) __val);
    }

    @Override
    @Word.Operation(node = LeftShiftNode.class, rightOperandIsInt = true)
    public Word shiftLeft(int __val)
    {
        return shiftLeft(intParam(__val));
    }

    @Word.Operation(node = LeftShiftNode.class, rightOperandIsInt = true)
    public Word shiftLeft(Word __val)
    {
        return box(unbox() << __val.unbox());
    }

    @Override
    @Word.Operation(node = RightShiftNode.class, rightOperandIsInt = true)
    public Word signedShiftRight(UnsignedWord __val)
    {
        return signedShiftRight((Word) __val);
    }

    @Override
    @Word.Operation(node = RightShiftNode.class, rightOperandIsInt = true)
    public Word signedShiftRight(int __val)
    {
        return signedShiftRight(intParam(__val));
    }

    @Word.Operation(node = RightShiftNode.class, rightOperandIsInt = true)
    public Word signedShiftRight(Word __val)
    {
        return box(unbox() >> __val.unbox());
    }

    @Override
    @Word.Operation(node = UnsignedRightShiftNode.class, rightOperandIsInt = true)
    public Word unsignedShiftRight(UnsignedWord __val)
    {
        return unsignedShiftRight((Word) __val);
    }

    @Override
    @Word.Operation(node = UnsignedRightShiftNode.class, rightOperandIsInt = true)
    public Word unsignedShiftRight(int __val)
    {
        return unsignedShiftRight(intParam(__val));
    }

    @Word.Operation(node = UnsignedRightShiftNode.class, rightOperandIsInt = true)
    public Word unsignedShiftRight(Word __val)
    {
        return box(unbox() >>> __val.unbox());
    }

    @Override
    @Word.Operation(node = AndNode.class)
    public Word and(SignedWord __val)
    {
        return and((Word) __val);
    }

    @Override
    @Word.Operation(node = AndNode.class)
    public Word and(UnsignedWord __val)
    {
        return and((Word) __val);
    }

    @Override
    @Word.Operation(node = AndNode.class)
    public Word and(int __val)
    {
        return and(intParam(__val));
    }

    @Word.Operation(node = AndNode.class)
    public Word and(Word __val)
    {
        return box(unbox() & __val.unbox());
    }

    @Override
    @Word.Operation(node = OrNode.class)
    public Word or(SignedWord __val)
    {
        return or((Word) __val);
    }

    @Override
    @Word.Operation(node = OrNode.class)
    public Word or(UnsignedWord __val)
    {
        return or((Word) __val);
    }

    @Override
    @Word.Operation(node = OrNode.class)
    public Word or(int __val)
    {
        return or(intParam(__val));
    }

    @Word.Operation(node = OrNode.class)
    public Word or(Word __val)
    {
        return box(unbox() | __val.unbox());
    }

    @Override
    @Word.Operation(node = XorNode.class)
    public Word xor(SignedWord __val)
    {
        return xor((Word) __val);
    }

    @Override
    @Word.Operation(node = XorNode.class)
    public Word xor(UnsignedWord __val)
    {
        return xor((Word) __val);
    }

    @Override
    @Word.Operation(node = XorNode.class)
    public Word xor(int __val)
    {
        return xor(intParam(__val));
    }

    @Word.Operation(node = XorNode.class)
    public Word xor(Word __val)
    {
        return box(unbox() ^ __val.unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.NOT)
    public Word not()
    {
        return box(~unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.IS_NULL)
    public boolean isNull()
    {
        return equal(WordFactory.zero());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.IS_NON_NULL)
    public boolean isNonNull()
    {
        return notEqual(WordFactory.zero());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.EQ)
    public boolean equal(ComparableWord __val)
    {
        return equal((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.EQ)
    public boolean equal(SignedWord __val)
    {
        return equal((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.EQ)
    public boolean equal(UnsignedWord __val)
    {
        return equal((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.EQ)
    public boolean equal(int __val)
    {
        return equal(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.EQ)
    public boolean equal(Word __val)
    {
        return unbox() == __val.unbox();
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.NE)
    public boolean notEqual(ComparableWord __val)
    {
        return notEqual((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.NE)
    public boolean notEqual(SignedWord __val)
    {
        return notEqual((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.NE)
    public boolean notEqual(UnsignedWord __val)
    {
        return notEqual((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.NE)
    public boolean notEqual(int __val)
    {
        return notEqual(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.NE)
    public boolean notEqual(Word __val)
    {
        return unbox() != __val.unbox();
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.LT)
    public boolean lessThan(SignedWord __val)
    {
        return lessThan((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.LT)
    public boolean lessThan(int __val)
    {
        return lessThan(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.LT)
    public boolean lessThan(Word __val)
    {
        return unbox() < __val.unbox();
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.LE)
    public boolean lessOrEqual(SignedWord __val)
    {
        return lessOrEqual((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.LE)
    public boolean lessOrEqual(int __val)
    {
        return lessOrEqual(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.LE)
    public boolean lessOrEqual(Word __val)
    {
        return unbox() <= __val.unbox();
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.GT)
    public boolean greaterThan(SignedWord __val)
    {
        return greaterThan((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.GT)
    public boolean greaterThan(int __val)
    {
        return greaterThan(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.GT)
    public boolean greaterThan(Word __val)
    {
        return unbox() > __val.unbox();
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.GE)
    public boolean greaterOrEqual(SignedWord __val)
    {
        return greaterOrEqual((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.GE)
    public boolean greaterOrEqual(int __val)
    {
        return greaterOrEqual(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.GE)
    public boolean greaterOrEqual(Word __val)
    {
        return unbox() >= __val.unbox();
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.BT)
    public boolean belowThan(UnsignedWord __val)
    {
        return belowThan((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.BT)
    public boolean belowThan(int __val)
    {
        return belowThan(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.BT)
    public boolean belowThan(Word __val)
    {
        return UnsignedMath.belowThan(unbox(), __val.unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.BE)
    public boolean belowOrEqual(UnsignedWord __val)
    {
        return belowOrEqual((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.BE)
    public boolean belowOrEqual(int __val)
    {
        return belowOrEqual(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.BE)
    public boolean belowOrEqual(Word __val)
    {
        return UnsignedMath.belowOrEqual(unbox(), __val.unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.AT)
    public boolean aboveThan(UnsignedWord __val)
    {
        return aboveThan((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.AT)
    public boolean aboveThan(int __val)
    {
        return aboveThan(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.AT)
    public boolean aboveThan(Word __val)
    {
        return UnsignedMath.aboveThan(unbox(), __val.unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.AE)
    public boolean aboveOrEqual(UnsignedWord __val)
    {
        return aboveOrEqual((Word) __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.AE)
    public boolean aboveOrEqual(int __val)
    {
        return aboveOrEqual(intParam(__val));
    }

    @Word.Operation(opcode = Word.WordOpcode.COMPARISON, condition = Condition.AE)
    public boolean aboveOrEqual(Word __val)
    {
        return UnsignedMath.aboveOrEqual(unbox(), __val.unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public byte readByte(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.getByte(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public char readChar(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.getChar(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public short readShort(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.getShort(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public int readInt(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.getInt(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public long readLong(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.getLong(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public float readFloat(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.getFloat(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public double readDouble(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.getDouble(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public <T extends WordBase> T readWord(WordBase __offset, LocationIdentity __locationIdentity)
    {
        return box(UnsafeAccess.UNSAFE.getAddress(add((Word) __offset).unbox()));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public native Object readObject(WordBase __offset, LocationIdentity __locationIdentity);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public byte readByte(int __offset, LocationIdentity __locationIdentity)
    {
        return readByte(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public char readChar(int __offset, LocationIdentity __locationIdentity)
    {
        return readChar(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public short readShort(int __offset, LocationIdentity __locationIdentity)
    {
        return readShort(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public int readInt(int __offset, LocationIdentity __locationIdentity)
    {
        return readInt(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public long readLong(int __offset, LocationIdentity __locationIdentity)
    {
        return readLong(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public float readFloat(int __offset, LocationIdentity __locationIdentity)
    {
        return readFloat(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public double readDouble(int __offset, LocationIdentity __locationIdentity)
    {
        return readDouble(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public <T extends WordBase> T readWord(int __offset, LocationIdentity __locationIdentity)
    {
        return readWord(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public Object readObject(int __offset, LocationIdentity __locationIdentity)
    {
        return readObject(WordFactory.signed(__offset), __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeByte(WordBase __offset, byte __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putByte(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeChar(WordBase __offset, char __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putChar(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeShort(WordBase __offset, short __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putShort(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeInt(WordBase __offset, int __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putInt(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeLong(WordBase __offset, long __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putLong(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeFloat(WordBase __offset, float __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putFloat(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeDouble(WordBase __offset, double __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putDouble(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeWord(WordBase __offset, WordBase __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putAddress(add((Word) __offset).unbox(), ((Word) __val).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.INITIALIZE)
    public void initializeLong(WordBase __offset, long __val, LocationIdentity __locationIdentity)
    {
        UnsafeAccess.UNSAFE.putLong(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public native void writeObject(WordBase __offset, Object __val, LocationIdentity __locationIdentity);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeByte(int __offset, byte __val, LocationIdentity __locationIdentity)
    {
        writeByte(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeChar(int __offset, char __val, LocationIdentity __locationIdentity)
    {
        writeChar(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeShort(int __offset, short __val, LocationIdentity __locationIdentity)
    {
        writeShort(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeInt(int __offset, int __val, LocationIdentity __locationIdentity)
    {
        writeInt(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeLong(int __offset, long __val, LocationIdentity __locationIdentity)
    {
        writeLong(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeFloat(int __offset, float __val, LocationIdentity __locationIdentity)
    {
        writeFloat(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeDouble(int __offset, double __val, LocationIdentity __locationIdentity)
    {
        writeDouble(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeWord(int __offset, WordBase __val, LocationIdentity __locationIdentity)
    {
        writeWord(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.INITIALIZE)
    public void initializeLong(int __offset, long __val, LocationIdentity __locationIdentity)
    {
        initializeLong(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeObject(int __offset, Object __val, LocationIdentity __locationIdentity)
    {
        writeObject(WordFactory.signed(__offset), __val, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public byte readByte(WordBase __offset)
    {
        return UnsafeAccess.UNSAFE.getByte(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public char readChar(WordBase __offset)
    {
        return UnsafeAccess.UNSAFE.getChar(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public short readShort(WordBase __offset)
    {
        return UnsafeAccess.UNSAFE.getShort(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public int readInt(WordBase __offset)
    {
        return UnsafeAccess.UNSAFE.getInt(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public long readLong(WordBase __offset)
    {
        return UnsafeAccess.UNSAFE.getLong(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public float readFloat(WordBase __offset)
    {
        return UnsafeAccess.UNSAFE.getFloat(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public double readDouble(WordBase __offset)
    {
        return UnsafeAccess.UNSAFE.getDouble(add((Word) __offset).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public <T extends WordBase> T readWord(WordBase __offset)
    {
        return box(UnsafeAccess.UNSAFE.getAddress(add((Word) __offset).unbox()));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public native Object readObject(WordBase __offset);

    @Word.Operation(opcode = Word.WordOpcode.READ_HEAP)
    public native Object readObject(WordBase __offset, HeapAccess.BarrierType __barrierType);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public byte readByte(int __offset)
    {
        return readByte(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public char readChar(int __offset)
    {
        return readChar(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public short readShort(int __offset)
    {
        return readShort(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public int readInt(int __offset)
    {
        return readInt(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public long readLong(int __offset)
    {
        return readLong(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public float readFloat(int __offset)
    {
        return readFloat(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public double readDouble(int __offset)
    {
        return readDouble(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public <T extends WordBase> T readWord(int __offset)
    {
        return readWord(WordFactory.signed(__offset));
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.READ_POINTER)
    public Object readObject(int __offset)
    {
        return readObject(WordFactory.signed(__offset));
    }

    @Word.Operation(opcode = Word.WordOpcode.READ_HEAP)
    public Object readObject(int __offset, HeapAccess.BarrierType __barrierType)
    {
        return readObject(WordFactory.signed(__offset), __barrierType);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeByte(WordBase __offset, byte __val)
    {
        UnsafeAccess.UNSAFE.putByte(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeChar(WordBase __offset, char __val)
    {
        UnsafeAccess.UNSAFE.putChar(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeShort(WordBase __offset, short __val)
    {
        UnsafeAccess.UNSAFE.putShort(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeInt(WordBase __offset, int __val)
    {
        UnsafeAccess.UNSAFE.putInt(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeLong(WordBase __offset, long __val)
    {
        UnsafeAccess.UNSAFE.putLong(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeFloat(WordBase __offset, float __val)
    {
        UnsafeAccess.UNSAFE.putFloat(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeDouble(WordBase __offset, double __val)
    {
        UnsafeAccess.UNSAFE.putDouble(add((Word) __offset).unbox(), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public native int compareAndSwapInt(WordBase __offset, int __expectedValue, int __newValue, LocationIdentity __locationIdentity);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public native long compareAndSwapLong(WordBase __offset, long __expectedValue, long __newValue, LocationIdentity __locationIdentity);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public native <T extends WordBase> T compareAndSwapWord(WordBase __offset, T __expectedValue, T __newValue, LocationIdentity __locationIdentity);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public native Object compareAndSwapObject(WordBase __offset, Object __expectedValue, Object __newValue, LocationIdentity __locationIdentity);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public boolean logicCompareAndSwapInt(WordBase __offset, int __expectedValue, int __newValue, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.compareAndSwapInt(this.toObject(), ((Word) __offset).unbox(), __expectedValue, __newValue);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public boolean logicCompareAndSwapLong(WordBase __offset, long __expectedValue, long __newValue, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.compareAndSwapLong(this.toObject(), ((Word) __offset).unbox(), __expectedValue, __newValue);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public native boolean logicCompareAndSwapWord(WordBase __offset, WordBase __expectedValue, WordBase __newValue, LocationIdentity __locationIdentity);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public boolean logicCompareAndSwapObject(WordBase __offset, Object __expectedValue, Object __newValue, LocationIdentity __locationIdentity)
    {
        return UnsafeAccess.UNSAFE.compareAndSwapObject(this.toObject(), ((Word) __offset).unbox(), __expectedValue, __newValue);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeWord(WordBase __offset, WordBase __val)
    {
        UnsafeAccess.UNSAFE.putAddress(add((Word) __offset).unbox(), ((Word) __val).unbox());
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public native void writeObject(WordBase __offset, Object __val);

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeByte(int __offset, byte __val)
    {
        writeByte(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeChar(int __offset, char __val)
    {
        writeChar(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeShort(int __offset, short __val)
    {
        writeShort(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeInt(int __offset, int __val)
    {
        writeInt(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeLong(int __offset, long __val)
    {
        writeLong(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeFloat(int __offset, float __val)
    {
        writeFloat(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeDouble(int __offset, double __val)
    {
        writeDouble(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeWord(int __offset, WordBase __val)
    {
        writeWord(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.WRITE_POINTER)
    public void writeObject(int __offset, Object __val)
    {
        writeObject(WordFactory.signed(__offset), __val);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public int compareAndSwapInt(int __offset, int __expectedValue, int __newValue, LocationIdentity __locationIdentity)
    {
        return compareAndSwapInt(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public long compareAndSwapLong(int __offset, long __expectedValue, long __newValue, LocationIdentity __locationIdentity)
    {
        return compareAndSwapLong(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public <T extends WordBase> T compareAndSwapWord(int __offset, T __expectedValue, T __newValue, LocationIdentity __locationIdentity)
    {
        return compareAndSwapWord(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public Object compareAndSwapObject(int __offset, Object __expectedValue, Object __newValue, LocationIdentity __locationIdentity)
    {
        return compareAndSwapObject(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public boolean logicCompareAndSwapInt(int __offset, int __expectedValue, int __newValue, LocationIdentity __locationIdentity)
    {
        return logicCompareAndSwapInt(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public boolean logicCompareAndSwapLong(int __offset, long __expectedValue, long __newValue, LocationIdentity __locationIdentity)
    {
        return logicCompareAndSwapLong(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public boolean logicCompareAndSwapWord(int __offset, WordBase __expectedValue, WordBase __newValue, LocationIdentity __locationIdentity)
    {
        return logicCompareAndSwapWord(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    @Override
    @Word.Operation(opcode = Word.WordOpcode.CAS_POINTER)
    public boolean logicCompareAndSwapObject(int __offset, Object __expectedValue, Object __newValue, LocationIdentity __locationIdentity)
    {
        return logicCompareAndSwapObject(WordFactory.signed(__offset), __expectedValue, __newValue, __locationIdentity);
    }

    ///
    // This is deprecated because of the easy to mistype name collision between {@link #equals} and
    // the other equals routines like {@link #equal(Word)}. In general you should never be
    // statically calling this method for Word types.
    ///
    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public final boolean equals(Object __obj)
    {
        throw GraalError.shouldNotReachHere("equals must not be called on words");
    }

    @Override
    public final int hashCode()
    {
        throw GraalError.shouldNotReachHere("hashCode must not be called on words");
    }
}

// @class HostedWord
final class HostedWord extends Word
{
    // @def
    private static final int SMALL_FROM = -1;
    // @def
    private static final int SMALL_TO = 100;

    // @def
    private static final HostedWord[] smallCache = new HostedWord[SMALL_TO - SMALL_FROM + 1];

    static
    {
        for (int __i = SMALL_FROM; __i <= SMALL_TO; __i++)
        {
            smallCache[__i - SMALL_FROM] = new HostedWord(__i);
        }
    }

    // @field
    private final long ___rawValue;

    // @cons HostedWord
    private HostedWord(long __rawValue)
    {
        super();
        this.___rawValue = __rawValue;
    }

    protected static Word boxLong(long __val)
    {
        if (__val >= SMALL_FROM && __val <= SMALL_TO)
        {
            return smallCache[(int) __val - SMALL_FROM];
        }
        return new HostedWord(__val);
    }

    @Override
    protected long unbox()
    {
        return this.___rawValue;
    }
}
