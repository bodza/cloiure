package giraaff.core.common;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.alloc.RegisterAllocationConfig;

///
// Represents the type of values in the LIR. It is composed of a {@link PlatformKind} that gives the
// low level representation of the value, a {@link #referenceMask} that describes the location of
// object references in the value, a {@link #referenceCompressionMask} that indicates which of these
// references are compressed references, and for derived references a {@link #derivedReferenceBase}.
//
// <h2>Constructing {@link LIRKind} instances</h2>
//
// During LIR generation, every new {@link Value} should get a {@link LIRKind} of the correct
// {@link PlatformKind} that also contains the correct reference information. {@linkplain LIRKind
// LIRKinds} should be created as follows:
//
// If the result value is created from one or more input values, the {@link LIRKind} should be
// created with {@link LIRKind#combine}(inputs). If the result has a different {@link PlatformKind}
// than the inputs, {@link LIRKind#combine}(inputs).{@link #changeType}(resultKind) should be used.
//
// If the result is an exact copy of one of the inputs, {@link Value#getValueKind()} can be used.
// Note that this is only correct for move-like operations, like conditional move or
// compare-and-swap. For convert operations, {@link LIRKind#combine} should be used.
//
// If it is known that the result will be a reference (e.g. pointer arithmetic where the end result
// is a valid oop), {@link #reference} or {@link LIRKind#compressedReference} should be used.
//
// If it is known that the result will neither be a reference nor be derived from a reference,
// {@link LIRKind#value} can be used. If the operation producing this value has inputs, this is very
// likely wrong, and {@link LIRKind#combine} should be used instead.
//
// If it is known that the result is derived from a reference in a way that the garbage collector
// can not track, {@link LIRKind#unknownReference} can be used. In most cases,
// {@link LIRKind#combine} should be used instead, since it is able to detect this automatically.
///
// @class LIRKind
public final class LIRKind extends ValueKind<LIRKind>
{
    ///
    // The location of object references in the value. If the value is a vector type, each bit
    // represents one component of the vector.
    ///
    // @field
    private final int ___referenceMask;

    ///
    // Mask with 1-bits indicating which references in {@link #referenceMask} are compressed.
    ///
    // @field
    private final int ___referenceCompressionMask;

    // @field
    private AllocatableValue ___derivedReferenceBase;

    // @def
    private static final int UNKNOWN_REFERENCE = -1;

    // @def
    public static final LIRKind Illegal = unknownReference(ValueKind.Illegal.getPlatformKind());

    // @cons
    private LIRKind(PlatformKind __platformKind, int __referenceMask, int __referenceCompressionMask, AllocatableValue __derivedReferenceBase)
    {
        super(__platformKind);
        this.___referenceMask = __referenceMask;
        this.___referenceCompressionMask = __referenceCompressionMask;
        this.___derivedReferenceBase = __derivedReferenceBase;
    }

    ///
    // Create a {@link LIRKind} of type {@code platformKind} that contains a primitive value. Should
    // be only used when it's guaranteed that the value is not even indirectly derived from a
    // reference. Otherwise, {@link #combine(Value...)} should be used instead.
    ///
    public static LIRKind value(PlatformKind __platformKind)
    {
        return new LIRKind(__platformKind, 0, 0, null);
    }

    ///
    // Create a {@link LIRKind} of type {@code platformKind} that contains a single, tracked,
    // uncompressed oop reference.
    ///
    public static LIRKind reference(PlatformKind __platformKind)
    {
        return derivedReference(__platformKind, null, false);
    }

    ///
    // Create a {@link LIRKind} of type {@code platformKind} that contains a single, tracked,
    // compressed oop reference.
    ///
    public static LIRKind compressedReference(PlatformKind __platformKind)
    {
        return derivedReference(__platformKind, null, true);
    }

    ///
    // Create the correct {@link LIRKind} for a given {@link Architecture} and {@link JavaKind}.
    ///
    public static LIRKind fromJavaKind(Architecture __arch, JavaKind __javaKind)
    {
        PlatformKind __platformKind = __arch.getPlatformKind(__javaKind);
        if (__javaKind.isObject())
        {
            return LIRKind.reference(__platformKind);
        }
        else
        {
            return LIRKind.value(__platformKind);
        }
    }

    ///
    // Create a {@link LIRKind} of type {@code platformKind} that contains a derived reference.
    ///
    public static LIRKind derivedReference(PlatformKind __platformKind, AllocatableValue __base, boolean __compressed)
    {
        int __length = __platformKind.getVectorLength();
        int __referenceMask = (1 << __length) - 1;
        int __referenceCompressionMask = (__compressed ? __referenceMask : 0);
        return new LIRKind(__platformKind, __referenceMask, __referenceCompressionMask, __base);
    }

    ///
    // Create a {@link LIRKind} of type {@code platformKind} that contains a value that is derived
    // from a reference in a non-linear way. Values of this {@link LIRKind} can not be live at
    // safepoints. In most cases, this should not be called directly. {@link #combine} should be
    // used instead to automatically propagate this information.
    ///
    public static LIRKind unknownReference(PlatformKind __platformKind)
    {
        return new LIRKind(__platformKind, UNKNOWN_REFERENCE, UNKNOWN_REFERENCE, null);
    }

    ///
    // Create a derived reference.
    //
    // @param base An {@link AllocatableValue} containing the base pointer of the derived reference.
    ///
    public LIRKind makeDerivedReference(AllocatableValue __base)
    {
        if (Value.ILLEGAL.equals(__base))
        {
            return makeUnknownReference();
        }
        else
        {
            if (isValue())
            {
                return derivedReference(getPlatformKind(), __base, false);
            }
            else
            {
                return new LIRKind(getPlatformKind(), this.___referenceMask, this.___referenceCompressionMask, __base);
            }
        }
    }

    ///
    // Derive a new type from inputs. The result will have the {@link PlatformKind} of one of the inputs.
    // If all inputs are values, the result is a value. Otherwise, the result is an unknown reference.
    //
    // This method should be used to construct the result {@link LIRKind} of any operation that
    // modifies values (e.g. arithmetics).
    ///
    public static LIRKind combine(Value... __inputs)
    {
        for (Value __input : __inputs)
        {
            LIRKind __kind = __input.getValueKind(LIRKind.class);
            if (__kind.isUnknownReference())
            {
                return __kind;
            }
            else if (!__kind.isValue())
            {
                return __kind.makeUnknownReference();
            }
        }

        // all inputs are values, just return one of them
        return __inputs[0].getValueKind(LIRKind.class);
    }

    ///
    // Helper method to construct derived reference kinds. Returns the base value of a reference or
    // derived reference. For values it returns {@code null}, and for unknown references it returns
    // {@link Value#ILLEGAL}.
    ///
    public static AllocatableValue derivedBaseFromValue(AllocatableValue __value)
    {
        ValueKind<?> __valueKind = __value.getValueKind();
        if (__valueKind instanceof LIRKind)
        {
            LIRKind __kind = __value.getValueKind(LIRKind.class);
            if (__kind.isValue())
            {
                return null;
            }
            else if (__kind.isDerivedReference())
            {
                return __kind.getDerivedReferenceBase();
            }
            else if (__kind.isUnknownReference())
            {
                return Value.ILLEGAL;
            }
            else
            {
                // kind is a reference
                return __value;
            }
        }
        else
        {
            return Value.ILLEGAL;
        }
    }

    ///
    // Helper method to construct derived reference kinds. If one of {@code base1} or {@code base2}
    // are set, it creates a derived reference using it as the base. If both are set, the result is
    // an unknown reference.
    ///
    public static LIRKind combineDerived(LIRKind __kind, AllocatableValue __base1, AllocatableValue __base2)
    {
        if (__base1 == null && __base2 == null)
        {
            return __kind;
        }
        else if (__base1 == null)
        {
            return __kind.makeDerivedReference(__base2);
        }
        else if (__base2 == null)
        {
            return __kind.makeDerivedReference(__base1);
        }
        else
        {
            return __kind.makeUnknownReference();
        }
    }

    ///
    // Merges the reference information of the inputs. The result will have the {@link PlatformKind}
    // of {@code mergeKind}. If all inputs are values (references), the result is a value (reference).
    // Otherwise, the result is an unknown reference.
    //
    // The correctness of the {@link PlatformKind} is not verified.
    ///
    public static LIRKind mergeReferenceInformation(LIRKind __mergeKind, LIRKind __inputKind)
    {
        if (__mergeKind.isUnknownReference())
        {
            // mergeKind is an unknown reference: the result should be also an unknown reference
            return __mergeKind;
        }

        if (__mergeKind.isValue())
        {
            // mergeKind is a value
            if (!__inputKind.isValue())
            {
                // inputs consists of values and references: make the result an unknown reference
                return __mergeKind.makeUnknownReference();
            }
            return __mergeKind;
        }
        // mergeKind is a reference
        if (__mergeKind.___referenceMask != __inputKind.___referenceMask || __mergeKind.___referenceCompressionMask != __inputKind.___referenceCompressionMask)
        {
            // reference masks do not match: the result can only be an unknown reference
            return __mergeKind.makeUnknownReference();
        }

        // both are references
        if (__mergeKind.isDerivedReference())
        {
            if (__inputKind.isDerivedReference() && __mergeKind.getDerivedReferenceBase().equals(__inputKind.getDerivedReferenceBase()))
            {
                // same reference base: they must be equal
                return __mergeKind;
            }
            // base pointers differ: make the result an unknown reference
            return __mergeKind.makeUnknownReference();
        }
        if (__inputKind.isDerivedReference())
        {
            // mergeKind is not derived, but inputKind is: make the result an unknown reference
            return __mergeKind.makeUnknownReference();
        }
        // both are not derived references: they must be equal
        return __mergeKind;
    }

    ///
    // Create a new {@link LIRKind} with the same reference information and a new
    // {@linkplain #getPlatformKind platform kind}. If the new kind is a longer vector than this,
    // the new elements are marked as untracked values.
    ///
    @Override
    public LIRKind changeType(PlatformKind __newPlatformKind)
    {
        if (__newPlatformKind == getPlatformKind())
        {
            return this;
        }
        else if (isUnknownReference())
        {
            return unknownReference(__newPlatformKind);
        }
        else if (this.___referenceMask == 0)
        {
            // value type
            return LIRKind.value(__newPlatformKind);
        }
        else
        {
            // reference type
            int __newLength = Math.min(32, __newPlatformKind.getVectorLength());
            int __lengthMask = 0xFFFFFFFF >>> (32 - __newLength);
            int __newReferenceMask = this.___referenceMask & __lengthMask;
            int __newReferenceCompressionMask = this.___referenceCompressionMask & __lengthMask;
            return new LIRKind(__newPlatformKind, __newReferenceMask, __newReferenceCompressionMask, this.___derivedReferenceBase);
        }
    }

    ///
    // Create a new {@link LIRKind} with a new {@linkplain #getPlatformKind platform kind}. If the
    // new kind is longer than this, the reference positions are repeated to fill the vector.
    ///
    public LIRKind repeat(PlatformKind __newPlatformKind)
    {
        if (isUnknownReference())
        {
            return unknownReference(__newPlatformKind);
        }
        else if (this.___referenceMask == 0)
        {
            // value type
            return LIRKind.value(__newPlatformKind);
        }
        else
        {
            // reference type
            int __oldLength = getPlatformKind().getVectorLength();
            int __newLength = __newPlatformKind.getVectorLength();

            // repeat reference mask to fill new kind
            int __newReferenceMask = 0;
            int __newReferenceCompressionMask = 0;
            for (int __i = 0; __i < __newLength; __i += getPlatformKind().getVectorLength())
            {
                __newReferenceMask |= this.___referenceMask << __i;
                __newReferenceCompressionMask |= this.___referenceCompressionMask << __i;
            }

            return new LIRKind(__newPlatformKind, __newReferenceMask, __newReferenceCompressionMask, this.___derivedReferenceBase);
        }
    }

    ///
    // Create a new {@link LIRKind} with the same type, but marked as containing an
    // {@link LIRKind#unknownReference}.
    ///
    public LIRKind makeUnknownReference()
    {
        return new LIRKind(getPlatformKind(), UNKNOWN_REFERENCE, UNKNOWN_REFERENCE, null);
    }

    ///
    // Check whether this value is a derived reference.
    ///
    public boolean isDerivedReference()
    {
        return getDerivedReferenceBase() != null;
    }

    ///
    // Get the base value of a derived reference.
    ///
    public AllocatableValue getDerivedReferenceBase()
    {
        return this.___derivedReferenceBase;
    }

    ///
    // Change the base value of a derived reference. This must be called on derived references only.
    ///
    public void setDerivedReferenceBase(AllocatableValue __derivedReferenceBase)
    {
        this.___derivedReferenceBase = __derivedReferenceBase;
    }

    ///
    // Check whether this value is derived from a reference in a non-linear way. If this returns
    // {@code true}, this value must not be live at safepoints.
    ///
    public boolean isUnknownReference()
    {
        return this.___referenceMask == UNKNOWN_REFERENCE;
    }

    public static boolean isUnknownReference(ValueKind<?> __kind)
    {
        if (__kind instanceof LIRKind)
        {
            return ((LIRKind) __kind).isUnknownReference();
        }
        else
        {
            return true;
        }
    }

    public static boolean isUnknownReference(Value __value)
    {
        return isUnknownReference(__value.getValueKind());
    }

    public int getReferenceCount()
    {
        return Integer.bitCount(this.___referenceMask);
    }

    ///
    // Check whether the {@code idx}th part of this value is a reference that must be tracked at safepoints.
    //
    // @param idx The index into the vector if this is a vector kind. Must be 0 if this is a scalar kind.
    ///
    public boolean isReference(int __idx)
    {
        return !isUnknownReference() && (this.___referenceMask & 1 << __idx) != 0;
    }

    ///
    // Check whether the {@code idx}th part of this value is a <b>compressed</b> reference.
    //
    // @param idx The index into the vector if this is a vector kind. Must be 0 if this is a scalar kind.
    ///
    public boolean isCompressedReference(int __idx)
    {
        return !isUnknownReference() && (this.___referenceCompressionMask & (1 << __idx)) != 0;
    }

    ///
    // Check whether this kind is a value type that doesn't need to be tracked at safepoints.
    ///
    public boolean isValue()
    {
        return this.___referenceMask == 0;
    }

    public static boolean isValue(ValueKind<?> __kind)
    {
        if (__kind instanceof LIRKind)
        {
            return ((LIRKind) __kind).isValue();
        }
        else
        {
            return false;
        }
    }

    public static boolean isValue(Value __value)
    {
        return isValue(__value.getValueKind());
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + ((getPlatformKind() == null) ? 0 : getPlatformKind().hashCode());
        __result = __prime * __result + ((getDerivedReferenceBase() == null) ? 0 : getDerivedReferenceBase().hashCode());
        __result = __prime * __result + this.___referenceMask;
        __result = __prime * __result + this.___referenceCompressionMask;
        return __result;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (!(__obj instanceof LIRKind))
        {
            return false;
        }

        LIRKind __other = (LIRKind) __obj;
        if (getPlatformKind() != __other.getPlatformKind() || this.___referenceMask != __other.___referenceMask || this.___referenceCompressionMask != __other.___referenceCompressionMask)
        {
            return false;
        }
        if (isDerivedReference())
        {
            if (!__other.isDerivedReference())
            {
                return false;
            }
            return getDerivedReferenceBase().equals(__other.getDerivedReferenceBase());
        }
        // 'this' is not a derived reference
        if (__other.isDerivedReference())
        {
            return false;
        }
        return true;
    }

    public static boolean verifyMoveKinds(ValueKind<?> __dst, ValueKind<?> __src, RegisterAllocationConfig __config)
    {
        if (__src.equals(__dst))
        {
            return true;
        }
        if (isUnknownReference(__dst) || isValue(__dst) && isValue(__src))
        {
            PlatformKind __srcPlatformKind = __src.getPlatformKind();
            PlatformKind __dstPlatformKind = __dst.getPlatformKind();
            if (__srcPlatformKind.equals(__dstPlatformKind))
            {
                return true;
            }
            // if the register category matches it should be fine, although the kind is different
            return __config.getRegisterCategory(__srcPlatformKind).equals(__config.getRegisterCategory(__dstPlatformKind));
        }
        // reference information mismatch
        return false;
    }
}
