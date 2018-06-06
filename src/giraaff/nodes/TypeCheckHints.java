package giraaff.nodes;

import java.util.Arrays;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.JavaTypeProfile.ProfiledType;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.TypeReference;

///
// Utility for deriving hint types for a type check instruction (e.g. checkcast or instanceof) based
// on the target type of the check and any profiling information available for the instruction.
///
// @class TypeCheckHints
public final class TypeCheckHints
{
    ///
    // A receiver type profiled in a type check instruction.
    ///
    // @class TypeCheckHints.Hint
    public static final class Hint
    {
        ///
        // A type seen while profiling a type check instruction.
        ///
        // @field
        public final ResolvedJavaType ___type;

        ///
        // Specifies if {@link #type} is a sub-type of the checked type.
        ///
        // @field
        public final boolean ___positive;

        // @cons TypeCheckHints.Hint
        Hint(ResolvedJavaType __type, boolean __positive)
        {
            super();
            this.___type = __type;
            this.___positive = __positive;
        }
    }

    // @def
    private static final TypeCheckHints.Hint[] NO_HINTS = {};

    ///
    // If non-null, then this is the only type that could pass the type check because the target of
    // the type check is a final class or has been speculated to be a final class and this value is
    // the only concrete subclass of the target type.
    ///
    // @field
    public final ResolvedJavaType ___exact;

    ///
    // The most likely types that the type check instruction will see.
    ///
    // @field
    public final TypeCheckHints.Hint[] ___hints;

    ///
    // The profile from which this information was derived.
    ///
    // @field
    public final JavaTypeProfile ___profile;

    ///
    // The total probability that the type check will hit one of the types in {@link #hints}.
    ///
    // @field
    public final double ___hintHitProbability;

    ///
    // Derives hint information for use when generating the code for a type check instruction.
    //
    // @param targetType the target type of the type check
    // @param profile the profiling information available for the instruction (if any)
    // @param assumptions the object in which speculations are recorded. This is null if
    //            speculations are not supported.
    // @param minHintHitProbability if the probability that the type check will hit one of the
    //            profiled types (up to {@code maxHints}) is below this value, then {@link #hints}
    //            will be null
    // @param maxHints the maximum length of {@link #hints}
    ///
    // @cons TypeCheckHints
    public TypeCheckHints(TypeReference __targetType, JavaTypeProfile __profile, Assumptions __assumptions, double __minHintHitProbability, int __maxHints)
    {
        super();
        this.___profile = __profile;
        if (__targetType != null && __targetType.isExact())
        {
            this.___exact = __targetType.getType();
        }
        else
        {
            this.___exact = null;
        }
        Double[] __hitProbability = { null };
        this.___hints = makeHints(__targetType, __profile, __minHintHitProbability, __maxHints, __hitProbability);
        this.___hintHitProbability = __hitProbability[0];
    }

    private static TypeCheckHints.Hint[] makeHints(TypeReference __targetType, JavaTypeProfile __profile, double __minHintHitProbability, int __maxHints, Double[] __hitProbability)
    {
        double __hitProb = 0.0d;
        TypeCheckHints.Hint[] __hintsBuf = NO_HINTS;
        if (__profile != null)
        {
            double __notRecordedTypes = __profile.getNotRecordedProbability();
            ProfiledType[] __ptypes = __profile.getTypes();
            if (__notRecordedTypes < (1D - __minHintHitProbability) && __ptypes != null && __ptypes.length > 0)
            {
                __hintsBuf = new TypeCheckHints.Hint[__ptypes.length];
                int __hintCount = 0;
                for (ProfiledType __ptype : __ptypes)
                {
                    if (__targetType != null)
                    {
                        ResolvedJavaType __hintType = __ptype.getType();
                        __hintsBuf[__hintCount++] = new TypeCheckHints.Hint(__hintType, __targetType.getType().isAssignableFrom(__hintType));
                        __hitProb += __ptype.getProbability();
                    }
                    if (__hintCount == __maxHints)
                    {
                        break;
                    }
                }
                if (__hitProb >= __minHintHitProbability)
                {
                    if (__hintsBuf.length != __hintCount || __hintCount > __maxHints)
                    {
                        __hintsBuf = Arrays.copyOf(__hintsBuf, Math.min(__maxHints, __hintCount));
                    }
                }
                else
                {
                    __hintsBuf = NO_HINTS;
                    __hitProb = 0.0d;
                }
            }
        }
        __hitProbability[0] = __hitProb;
        return __hintsBuf;
    }
}
