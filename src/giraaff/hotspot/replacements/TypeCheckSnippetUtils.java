package giraaff.hotspot.replacements;

import java.util.Arrays;

import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.TypeCheckHints;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.word.Word;

/**
 * Utilities and common code paths used by the type check snippets.
 */
// @class TypeCheckSnippetUtils
public final class TypeCheckSnippetUtils
{
    static boolean checkSecondarySubType(KlassPointer __t, KlassPointer __sNonNull)
    {
        // if (S.cache == T) return true
        if (__sNonNull.readKlassPointer(HotSpotRuntime.secondarySuperCacheOffset, HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION).equal(__t))
        {
            return true;
        }

        return checkSelfAndSupers(__t, __sNonNull);
    }

    static boolean checkUnknownSubType(KlassPointer __t, KlassPointer __sNonNull)
    {
        // int off = T.offset
        int __superCheckOffset = __t.readInt(HotSpotRuntime.superCheckOffsetOffset, HotSpotReplacementsUtil.KLASS_SUPER_CHECK_OFFSET_LOCATION);
        boolean __primary = __superCheckOffset != HotSpotRuntime.secondarySuperCacheOffset;

        // if (T = S[off]) return true
        if (__sNonNull.readKlassPointer(__superCheckOffset, HotSpotReplacementsUtil.PRIMARY_SUPERS_LOCATION).equal(__t))
        {
            return true;
        }

        // if (off != &cache) return false
        if (__primary)
        {
            return false;
        }

        return checkSelfAndSupers(__t, __sNonNull);
    }

    private static boolean checkSelfAndSupers(KlassPointer __t, KlassPointer __s)
    {
        // if (T == S) return true
        if (__s.equal(__t))
        {
            return true;
        }

        // if (S.scan_s_s_array(T)) { S.cache = T; return true; }
        Word __secondarySupers = __s.readWord(HotSpotRuntime.secondarySupersOffset, HotSpotReplacementsUtil.SECONDARY_SUPERS_LOCATION);
        int __length = __secondarySupers.readInt(HotSpotRuntime.metaspaceArrayLengthOffset, HotSpotReplacementsUtil.METASPACE_ARRAY_LENGTH_LOCATION);
        for (int __i = 0; __i < __length; __i++)
        {
            if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, __t.equal(loadSecondarySupersElement(__secondarySupers, __i))))
            {
                __s.writeKlassPointer(HotSpotRuntime.secondarySuperCacheOffset, __t, HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
                return true;
            }
        }
        return false;
    }

    /**
     * A set of type check hints ordered by decreasing probabilities.
     */
    // @class TypeCheckSnippetUtils.Hints
    public static final class Hints
    {
        /**
         * The hubs of the hint types.
         */
        // @field
        public final ConstantNode[] hubs;

        /**
         * A predicate over {@link #hubs} specifying whether the corresponding hint type is a
         * sub-type of the checked type.
         */
        // @field
        public final boolean[] isPositive;

        // @cons
        Hints(ConstantNode[] __hints, boolean[] __hintIsPositive)
        {
            super();
            this.hubs = __hints;
            this.isPositive = __hintIsPositive;
        }
    }

    static Hints createHints(TypeCheckHints __hints, MetaAccessProvider __metaAccess, boolean __positiveOnly, StructuredGraph __graph)
    {
        ConstantNode[] __hubs = new ConstantNode[__hints.hints.length];
        boolean[] __isPositive = new boolean[__hints.hints.length];
        int __index = 0;
        for (int __i = 0; __i < __hubs.length; __i++)
        {
            if (!__positiveOnly || __hints.hints[__i].positive)
            {
                __hubs[__index] = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), ((HotSpotResolvedObjectType) __hints.hints[__i].type).klass(), __metaAccess, __graph);
                __isPositive[__index] = __hints.hints[__i].positive;
                __index++;
            }
        }
        if (__positiveOnly && __index != __hubs.length)
        {
            __hubs = Arrays.copyOf(__hubs, __index);
            __isPositive = Arrays.copyOf(__isPositive, __index);
        }
        return new Hints(__hubs, __isPositive);
    }

    static KlassPointer loadSecondarySupersElement(Word __metaspaceArray, int __index)
    {
        return KlassPointer.fromWord(__metaspaceArray.readWord(HotSpotRuntime.metaspaceArrayBaseOffset + __index * HotSpotReplacementsUtil.wordSize(), HotSpotReplacementsUtil.SECONDARY_SUPERS_ELEMENT_LOCATION));
    }
}
