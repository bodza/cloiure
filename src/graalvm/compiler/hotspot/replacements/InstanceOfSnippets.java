package graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.TriState;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.api.replacements.Snippet.NonNullParameter;
import graalvm.compiler.api.replacements.Snippet.VarargsParameter;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.type.KlassPointerStamp;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.replacements.HotspotSnippetsOptions;
import graalvm.compiler.hotspot.replacements.TypeCheckSnippetUtils;
import graalvm.compiler.hotspot.replacements.TypeCheckSnippetUtils.Counters;
import graalvm.compiler.hotspot.replacements.TypeCheckSnippetUtils.Hints;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.SnippetAnchorNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.TypeCheckHints;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.BranchProbabilityNode;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.java.ClassIsAssignableFromNode;
import graalvm.compiler.nodes.java.InstanceOfDynamicNode;
import graalvm.compiler.nodes.java.InstanceOfNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.InstanceOfSnippetsTemplates;
import graalvm.compiler.replacements.SnippetCounter;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.replacements.nodes.ExplodeLoopNode;

/**
 * Snippets used for implementing the type test of an instanceof instruction. Since instanceof is a
 * floating node, it is lowered separately for each of its usages.
 *
 * The type tests implemented are described in the paper
 * <a href="http://dl.acm.org/citation.cfm?id=583821"> Fast subtype checking in the HotSpot JVM</a>
 * by Cliff Click and John Rose.
 */
public class InstanceOfSnippets implements Snippets
{
    /**
     * A test against a set of hints derived from a profile with 100% precise coverage of seen
     * types. This snippet deoptimizes on hint miss paths.
     */
    @Snippet
    public static Object instanceofWithProfile(Object object, @VarargsParameter KlassPointer[] hints, @VarargsParameter boolean[] hintIsPositive, Object trueValue, Object falseValue, @ConstantParameter boolean nullSeen, @ConstantParameter Counters counters)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            counters.isNull.inc();
            if (!nullSeen)
            {
                // See comment below for other deoptimization path; the
                // same reasoning applies here.
                DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.OptimizedTypeCheckViolated);
            }
            return falseValue;
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        KlassPointer objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
        // if we get an exact match: succeed immediately
        ExplodeLoopNode.explodeLoop();
        for (int i = 0; i < hints.length; i++)
        {
            KlassPointer hintHub = hints[i];
            boolean positive = hintIsPositive[i];
            if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, hintHub.equal(objectHub)))
            {
                counters.hintsHit.inc();
                return positive ? trueValue : falseValue;
            }
            counters.hintsMiss.inc();
        }
        // This maybe just be a rare event but it might also indicate a phase change
        // in the application. Ideally we want to use DeoptimizationAction.None for
        // the former but the cost is too high if indeed it is the latter. As such,
        // we defensively opt for InvalidateReprofile.
        DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.OptimizedTypeCheckViolated);
        return falseValue;
    }

    /**
     * A test against a final type.
     */
    @Snippet
    public static Object instanceofExact(Object object, KlassPointer exactHub, Object trueValue, Object falseValue, @ConstantParameter Counters counters)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            counters.isNull.inc();
            return falseValue;
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        KlassPointer objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
        if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, objectHub.notEqual(exactHub)))
        {
            counters.exactMiss.inc();
            return falseValue;
        }
        counters.exactHit.inc();
        return trueValue;
    }

    /**
     * A test against a primary type.
     */
    @Snippet
    public static Object instanceofPrimary(KlassPointer hub, Object object, @ConstantParameter int superCheckOffset, Object trueValue, Object falseValue, @ConstantParameter Counters counters)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            counters.isNull.inc();
            return falseValue;
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        KlassPointer objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, objectHub.readKlassPointer(superCheckOffset, HotSpotReplacementsUtil.PRIMARY_SUPERS_LOCATION).notEqual(hub)))
        {
            counters.displayMiss.inc();
            return falseValue;
        }
        counters.displayHit.inc();
        return trueValue;
    }

    /**
     * A test against a restricted secondary type type.
     */
    @Snippet
    public static Object instanceofSecondary(KlassPointer hub, Object object, @VarargsParameter KlassPointer[] hints, @VarargsParameter boolean[] hintIsPositive, Object trueValue, Object falseValue, @ConstantParameter Counters counters)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            counters.isNull.inc();
            return falseValue;
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        KlassPointer objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
        // if we get an exact match: succeed immediately
        ExplodeLoopNode.explodeLoop();
        for (int i = 0; i < hints.length; i++)
        {
            KlassPointer hintHub = hints[i];
            boolean positive = hintIsPositive[i];
            if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, hintHub.equal(objectHub)))
            {
                counters.hintsHit.inc();
                return positive ? trueValue : falseValue;
            }
        }
        counters.hintsMiss.inc();
        if (!TypeCheckSnippetUtils.checkSecondarySubType(hub, objectHub, counters))
        {
            return falseValue;
        }
        return trueValue;
    }

    /**
     * Type test used when the type being tested against is not known at compile time.
     */
    @Snippet
    public static Object instanceofDynamic(KlassPointer hub, Object object, Object trueValue, Object falseValue, @ConstantParameter boolean allowNull, @ConstantParameter Counters counters)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            counters.isNull.inc();
            if (allowNull)
            {
                return trueValue;
            }
            else
            {
                return falseValue;
            }
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        KlassPointer nonNullObjectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
        // The hub of a primitive type can be null => always return false in this case.
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !hub.isNull()))
        {
            if (TypeCheckSnippetUtils.checkUnknownSubType(hub, nonNullObjectHub, counters))
            {
                return trueValue;
            }
        }
        return falseValue;
    }

    @Snippet
    public static Object isAssignableFrom(@NonNullParameter Class<?> thisClassNonNull, Class<?> otherClass, Object trueValue, Object falseValue, @ConstantParameter Counters counters)
    {
        if (otherClass == null)
        {
            DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.NullCheckException);
            return false;
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        Class<?> otherClassNonNull = PiNode.piCastNonNullClass(otherClass, anchorNode);

        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, thisClassNonNull == otherClassNonNull))
        {
            return trueValue;
        }

        KlassPointer thisHub = ClassGetHubNode.readClass(thisClassNonNull);
        KlassPointer otherHub = ClassGetHubNode.readClass(otherClassNonNull);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !thisHub.isNull()))
        {
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !otherHub.isNull()))
            {
                GuardingNode guardNonNull = SnippetAnchorNode.anchor();
                KlassPointer nonNullOtherHub = ClassGetHubNode.piCastNonNull(otherHub, guardNonNull);
                if (TypeCheckSnippetUtils.checkUnknownSubType(thisHub, nonNullOtherHub, counters))
                {
                    return trueValue;
                }
            }
        }

        // If either hub is null, one of them is a primitive type and given that the class is not
        // equal, return false.
        return falseValue;
    }

    public static class Templates extends InstanceOfSnippetsTemplates
    {
        private final SnippetInfo instanceofWithProfile = snippet(InstanceOfSnippets.class, "instanceofWithProfile");
        private final SnippetInfo instanceofExact = snippet(InstanceOfSnippets.class, "instanceofExact");
        private final SnippetInfo instanceofPrimary = snippet(InstanceOfSnippets.class, "instanceofPrimary");
        private final SnippetInfo instanceofSecondary = snippet(InstanceOfSnippets.class, "instanceofSecondary", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
        private final SnippetInfo instanceofDynamic = snippet(InstanceOfSnippets.class, "instanceofDynamic", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
        private final SnippetInfo isAssignableFrom = snippet(InstanceOfSnippets.class, "isAssignableFrom", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);

        private final Counters counters;

        public Templates(OptionValues options, SnippetCounter.Group.Factory factory, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
            this.counters = new Counters(factory);
        }

        @Override
        protected Arguments makeArguments(InstanceOfUsageReplacer replacer, LoweringTool tool)
        {
            if (replacer.instanceOf instanceof InstanceOfNode)
            {
                InstanceOfNode instanceOf = (InstanceOfNode) replacer.instanceOf;
                ValueNode object = instanceOf.getValue();
                Assumptions assumptions = instanceOf.graph().getAssumptions();

                OptionValues localOptions = instanceOf.getOptions();
                JavaTypeProfile profile = instanceOf.profile();
                if (GraalOptions.GeneratePIC.getValue(localOptions))
                {
                    // FIXME: We can't embed constants in hints. We can't really load them from GOT
                    // either. Hard problem.
                    profile = null;
                }
                TypeCheckHints hintInfo = new TypeCheckHints(instanceOf.type(), profile, assumptions, HotspotSnippetsOptions.TypeCheckMinProfileHitProbability.getValue(localOptions), HotspotSnippetsOptions.TypeCheckMaxHints.getValue(localOptions));
                final HotSpotResolvedObjectType type = (HotSpotResolvedObjectType) instanceOf.type().getType();
                ConstantNode hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), type.klass(), providers.getMetaAccess(), instanceOf.graph());

                Arguments args;

                StructuredGraph graph = instanceOf.graph();
                if (hintInfo.hintHitProbability >= 1.0 && hintInfo.exact == null)
                {
                    Hints hints = TypeCheckSnippetUtils.createHints(hintInfo, providers.getMetaAccess(), false, graph);
                    args = new Arguments(instanceofWithProfile, graph.getGuardsStage(), tool.getLoweringStage());
                    args.add("object", object);
                    args.addVarargs("hints", KlassPointer.class, KlassPointerStamp.klassNonNull(), hints.hubs);
                    args.addVarargs("hintIsPositive", boolean.class, StampFactory.forKind(JavaKind.Boolean), hints.isPositive);
                }
                else if (hintInfo.exact != null)
                {
                    args = new Arguments(instanceofExact, graph.getGuardsStage(), tool.getLoweringStage());
                    args.add("object", object);
                    args.add("exactHub", ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), ((HotSpotResolvedObjectType) hintInfo.exact).klass(), providers.getMetaAccess(), graph));
                }
                else if (type.isPrimaryType())
                {
                    args = new Arguments(instanceofPrimary, graph.getGuardsStage(), tool.getLoweringStage());
                    args.add("hub", hub);
                    args.add("object", object);
                    args.addConst("superCheckOffset", type.superCheckOffset());
                }
                else
                {
                    Hints hints = TypeCheckSnippetUtils.createHints(hintInfo, providers.getMetaAccess(), false, graph);
                    args = new Arguments(instanceofSecondary, graph.getGuardsStage(), tool.getLoweringStage());
                    args.add("hub", hub);
                    args.add("object", object);
                    args.addVarargs("hints", KlassPointer.class, KlassPointerStamp.klassNonNull(), hints.hubs);
                    args.addVarargs("hintIsPositive", boolean.class, StampFactory.forKind(JavaKind.Boolean), hints.isPositive);
                }
                args.add("trueValue", replacer.trueValue);
                args.add("falseValue", replacer.falseValue);
                if (hintInfo.hintHitProbability >= 1.0 && hintInfo.exact == null)
                {
                    args.addConst("nullSeen", hintInfo.profile.getNullSeen() != TriState.FALSE);
                }
                args.addConst("counters", counters);
                return args;
            }
            else if (replacer.instanceOf instanceof InstanceOfDynamicNode)
            {
                InstanceOfDynamicNode instanceOf = (InstanceOfDynamicNode) replacer.instanceOf;
                ValueNode object = instanceOf.getObject();

                Arguments args = new Arguments(instanceofDynamic, instanceOf.graph().getGuardsStage(), tool.getLoweringStage());
                args.add("hub", instanceOf.getMirrorOrHub());
                args.add("object", object);
                args.add("trueValue", replacer.trueValue);
                args.add("falseValue", replacer.falseValue);
                args.addConst("allowNull", instanceOf.allowsNull());
                args.addConst("counters", counters);
                return args;
            }
            else if (replacer.instanceOf instanceof ClassIsAssignableFromNode)
            {
                ClassIsAssignableFromNode isAssignable = (ClassIsAssignableFromNode) replacer.instanceOf;
                Arguments args = new Arguments(isAssignableFrom, isAssignable.graph().getGuardsStage(), tool.getLoweringStage());
                args.add("thisClassNonNull", isAssignable.getThisClass());
                args.add("otherClass", isAssignable.getOtherClass());
                args.add("trueValue", replacer.trueValue);
                args.add("falseValue", replacer.falseValue);
                args.addConst("counters", counters);
                return args;
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
    }
}
