package giraaff.hotspot.replacements;

import java.util.Arrays;

import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.hotspot.GraalHotSpotVMConfig;
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
public class TypeCheckSnippetUtils
{
    static boolean checkSecondarySubType(KlassPointer t, KlassPointer sNonNull)
    {
        // if (S.cache == T) return true
        if (sNonNull.readKlassPointer(HotSpotReplacementsUtil.secondarySuperCacheOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION).equal(t))
        {
            return true;
        }

        return checkSelfAndSupers(t, sNonNull);
    }

    static boolean checkUnknownSubType(KlassPointer t, KlassPointer sNonNull)
    {
        // int off = T.offset
        int superCheckOffset = t.readInt(HotSpotReplacementsUtil.superCheckOffsetOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.KLASS_SUPER_CHECK_OFFSET_LOCATION);
        boolean primary = superCheckOffset != HotSpotReplacementsUtil.secondarySuperCacheOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG);

        // if (T = S[off]) return true
        if (sNonNull.readKlassPointer(superCheckOffset, HotSpotReplacementsUtil.PRIMARY_SUPERS_LOCATION).equal(t))
        {
            return true;
        }

        // if (off != &cache) return false
        if (primary)
        {
            return false;
        }

        return checkSelfAndSupers(t, sNonNull);
    }

    private static boolean checkSelfAndSupers(KlassPointer t, KlassPointer s)
    {
        // if (T == S) return true
        if (s.equal(t))
        {
            return true;
        }

        // if (S.scan_s_s_array(T)) { S.cache = T; return true; }
        Word secondarySupers = s.readWord(HotSpotReplacementsUtil.secondarySupersOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.SECONDARY_SUPERS_LOCATION);
        int length = secondarySupers.readInt(HotSpotReplacementsUtil.metaspaceArrayLengthOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.METASPACE_ARRAY_LENGTH_LOCATION);
        for (int i = 0; i < length; i++)
        {
            if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, t.equal(loadSecondarySupersElement(secondarySupers, i))))
            {
                s.writeKlassPointer(HotSpotReplacementsUtil.secondarySuperCacheOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), t, HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
                return true;
            }
        }
        return false;
    }

    /**
     * A set of type check hints ordered by decreasing probabilities.
     */
    public static class Hints
    {
        /**
         * The hubs of the hint types.
         */
        public final ConstantNode[] hubs;

        /**
         * A predicate over {@link #hubs} specifying whether the corresponding hint type is a
         * sub-type of the checked type.
         */
        public final boolean[] isPositive;

        Hints(ConstantNode[] hints, boolean[] hintIsPositive)
        {
            this.hubs = hints;
            this.isPositive = hintIsPositive;
        }
    }

    static Hints createHints(TypeCheckHints hints, MetaAccessProvider metaAccess, boolean positiveOnly, StructuredGraph graph)
    {
        ConstantNode[] hubs = new ConstantNode[hints.hints.length];
        boolean[] isPositive = new boolean[hints.hints.length];
        int index = 0;
        for (int i = 0; i < hubs.length; i++)
        {
            if (!positiveOnly || hints.hints[i].positive)
            {
                hubs[index] = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), ((HotSpotResolvedObjectType) hints.hints[i].type).klass(), metaAccess, graph);
                isPositive[index] = hints.hints[i].positive;
                index++;
            }
        }
        if (positiveOnly && index != hubs.length)
        {
            hubs = Arrays.copyOf(hubs, index);
            isPositive = Arrays.copyOf(isPositive, index);
        }
        return new Hints(hubs, isPositive);
    }

    static KlassPointer loadSecondarySupersElement(Word metaspaceArray, int index)
    {
        return KlassPointer.fromWord(metaspaceArray.readWord(HotSpotReplacementsUtil.metaspaceArrayBaseOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG) + index * HotSpotReplacementsUtil.wordSize(), HotSpotReplacementsUtil.SECONDARY_SUPERS_ELEMENT_LOCATION));
    }
}
