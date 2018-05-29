package giraaff.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.TriState;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.api.replacements.Snippet.NonNullParameter;
import giraaff.api.replacements.Snippet.VarargsParameter;
import giraaff.core.common.type.StampFactory;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.HotspotSnippetsOptions;
import giraaff.hotspot.replacements.TypeCheckSnippetUtils;
import giraaff.hotspot.replacements.TypeCheckSnippetUtils.Hints;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.SnippetAnchorNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.TypeCheckHints;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.java.ClassIsAssignableFromNode;
import giraaff.nodes.java.InstanceOfDynamicNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.replacements.InstanceOfSnippetsTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.ExplodeLoopNode;
import giraaff.util.GraalError;

/**
 * Snippets used for implementing the type test of an instanceof instruction. Since instanceof is a
 * floating node, it is lowered separately for each of its usages.
 *
 * The type tests implemented are described in the paper
 * <a href="http://dl.acm.org/citation.cfm?id=583821"> Fast subtype checking in the HotSpot JVM</a>
 * by Cliff Click and John Rose.
 */
// @class InstanceOfSnippets
public final class InstanceOfSnippets implements Snippets
{
    /**
     * A test against a set of hints derived from a profile with 100% precise coverage of seen
     * types. This snippet deoptimizes on hint miss paths.
     */
    @Snippet
    public static Object instanceofWithProfile(Object object, @VarargsParameter KlassPointer[] hints, @VarargsParameter boolean[] hintIsPositive, Object trueValue, Object falseValue, @ConstantParameter boolean nullSeen)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            if (!nullSeen)
            {
                // See comment below for other deoptimization path, the same reasoning applies here.
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
                return positive ? trueValue : falseValue;
            }
        }
        // This maybe just be a rare event but it might also indicate a phase change in the application.
        // Ideally we want to use DeoptimizationAction.None for the former but the cost is too high if
        // indeed it is the latter. As such, we defensively opt for InvalidateReprofile.
        DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.OptimizedTypeCheckViolated);
        return falseValue;
    }

    /**
     * A test against a final type.
     */
    @Snippet
    public static Object instanceofExact(Object object, KlassPointer exactHub, Object trueValue, Object falseValue)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            return falseValue;
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        KlassPointer objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
        if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, objectHub.notEqual(exactHub)))
        {
            return falseValue;
        }
        return trueValue;
    }

    /**
     * A test against a primary type.
     */
    @Snippet
    public static Object instanceofPrimary(KlassPointer hub, Object object, @ConstantParameter int superCheckOffset, Object trueValue, Object falseValue)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
            return falseValue;
        }
        GuardingNode anchorNode = SnippetAnchorNode.anchor();
        KlassPointer objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, objectHub.readKlassPointer(superCheckOffset, HotSpotReplacementsUtil.PRIMARY_SUPERS_LOCATION).notEqual(hub)))
        {
            return falseValue;
        }
        return trueValue;
    }

    /**
     * A test against a restricted secondary type type.
     */
    @Snippet
    public static Object instanceofSecondary(KlassPointer hub, Object object, @VarargsParameter KlassPointer[] hints, @VarargsParameter boolean[] hintIsPositive, Object trueValue, Object falseValue)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
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
                return positive ? trueValue : falseValue;
            }
        }
        if (!TypeCheckSnippetUtils.checkSecondarySubType(hub, objectHub))
        {
            return falseValue;
        }
        return trueValue;
    }

    /**
     * Type test used when the type being tested against is not known at compile time.
     */
    @Snippet
    public static Object instanceofDynamic(KlassPointer hub, Object object, Object trueValue, Object falseValue, @ConstantParameter boolean allowNull)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, object == null))
        {
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
            if (TypeCheckSnippetUtils.checkUnknownSubType(hub, nonNullObjectHub))
            {
                return trueValue;
            }
        }
        return falseValue;
    }

    @Snippet
    public static Object isAssignableFrom(@NonNullParameter Class<?> thisClassNonNull, Class<?> otherClass, Object trueValue, Object falseValue)
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
                if (TypeCheckSnippetUtils.checkUnknownSubType(thisHub, nonNullOtherHub))
                {
                    return trueValue;
                }
            }
        }

        // If either hub is null, one of them is a primitive type and given that the class is not equal, return false.
        return falseValue;
    }

    // @class InstanceOfSnippets.Templates
    public static final class Templates extends InstanceOfSnippetsTemplates
    {
        private final SnippetInfo instanceofWithProfile = snippet(InstanceOfSnippets.class, "instanceofWithProfile");
        private final SnippetInfo instanceofExact = snippet(InstanceOfSnippets.class, "instanceofExact");
        private final SnippetInfo instanceofPrimary = snippet(InstanceOfSnippets.class, "instanceofPrimary");
        private final SnippetInfo instanceofSecondary = snippet(InstanceOfSnippets.class, "instanceofSecondary", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
        private final SnippetInfo instanceofDynamic = snippet(InstanceOfSnippets.class, "instanceofDynamic", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
        private final SnippetInfo isAssignableFrom = snippet(InstanceOfSnippets.class, "isAssignableFrom", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);

        // @cons
        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
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
                return args;
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
    }

    // @cons
    private InstanceOfSnippets()
    {
        super();
    }
}
