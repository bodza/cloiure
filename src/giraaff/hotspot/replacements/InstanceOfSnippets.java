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
import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.StampFactory;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.TypeCheckSnippetUtils;
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
import giraaff.replacements.InstanceOfSnippetsTemplates;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.ExplodeLoopNode;
import giraaff.util.GraalError;

///
// Snippets used for implementing the type test of an instanceof instruction. Since instanceof is a
// floating node, it is lowered separately for each of its usages.
//
// The type tests implemented are described in the paper
// <a href="http://dl.acm.org/citation.cfm?id=583821"> Fast subtype checking in the HotSpot JVM</a>
// by Cliff Click and John Rose.
///
// @class InstanceOfSnippets
public final class InstanceOfSnippets implements Snippets
{
    // @cons InstanceOfSnippets
    private InstanceOfSnippets()
    {
        super();
    }

    ///
    // A test against a set of hints derived from a profile with 100% precise coverage of seen
    // types. This snippet deoptimizes on hint miss paths.
    ///
    @Snippet
    public static Object instanceofWithProfile(Object __object, @Snippet.VarargsParameter KlassPointer[] __hints, @Snippet.VarargsParameter boolean[] __hintIsPositive, Object __trueValue, Object __falseValue, @Snippet.ConstantParameter boolean __nullSeen)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __object == null))
        {
            if (!__nullSeen)
            {
                // See comment below for other deoptimization path, the same reasoning applies here.
                DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.OptimizedTypeCheckViolated);
            }
            return __falseValue;
        }
        GuardingNode __anchorNode = SnippetAnchorNode.anchor();
        KlassPointer __objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(__object, __anchorNode));
        // if we get an exact match: succeed immediately
        ExplodeLoopNode.explodeLoop();
        for (int __i = 0; __i < __hints.length; __i++)
        {
            KlassPointer __hintHub = __hints[__i];
            boolean __positive = __hintIsPositive[__i];
            if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, __hintHub.equal(__objectHub)))
            {
                return __positive ? __trueValue : __falseValue;
            }
        }
        // This maybe just be a rare event but it might also indicate a phase change in the application.
        // Ideally we want to use DeoptimizationAction.None for the former but the cost is too high if
        // indeed it is the latter. As such, we defensively opt for InvalidateReprofile.
        DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.OptimizedTypeCheckViolated);
        return __falseValue;
    }

    ///
    // A test against a final type.
    ///
    @Snippet
    public static Object instanceofExact(Object __object, KlassPointer __exactHub, Object __trueValue, Object __falseValue)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __object == null))
        {
            return __falseValue;
        }
        GuardingNode __anchorNode = SnippetAnchorNode.anchor();
        KlassPointer __objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(__object, __anchorNode));
        if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, __objectHub.notEqual(__exactHub)))
        {
            return __falseValue;
        }
        return __trueValue;
    }

    ///
    // A test against a primary type.
    ///
    @Snippet
    public static Object instanceofPrimary(KlassPointer __hub, Object __object, @Snippet.ConstantParameter int __superCheckOffset, Object __trueValue, Object __falseValue)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __object == null))
        {
            return __falseValue;
        }
        GuardingNode __anchorNode = SnippetAnchorNode.anchor();
        KlassPointer __objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(__object, __anchorNode));
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, __objectHub.readKlassPointer(__superCheckOffset, HotSpotReplacementsUtil.PRIMARY_SUPERS_LOCATION).notEqual(__hub)))
        {
            return __falseValue;
        }
        return __trueValue;
    }

    ///
    // A test against a restricted secondary type type.
    ///
    @Snippet
    public static Object instanceofSecondary(KlassPointer __hub, Object __object, @Snippet.VarargsParameter KlassPointer[] __hints, @Snippet.VarargsParameter boolean[] __hintIsPositive, Object __trueValue, Object __falseValue)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __object == null))
        {
            return __falseValue;
        }
        GuardingNode __anchorNode = SnippetAnchorNode.anchor();
        KlassPointer __objectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(__object, __anchorNode));
        // if we get an exact match: succeed immediately
        ExplodeLoopNode.explodeLoop();
        for (int __i = 0; __i < __hints.length; __i++)
        {
            KlassPointer __hintHub = __hints[__i];
            boolean __positive = __hintIsPositive[__i];
            if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __hintHub.equal(__objectHub)))
            {
                return __positive ? __trueValue : __falseValue;
            }
        }
        if (!TypeCheckSnippetUtils.checkSecondarySubType(__hub, __objectHub))
        {
            return __falseValue;
        }
        return __trueValue;
    }

    ///
    // Type test used when the type being tested against is not known at compile time.
    ///
    @Snippet
    public static Object instanceofDynamic(KlassPointer __hub, Object __object, Object __trueValue, Object __falseValue, @Snippet.ConstantParameter boolean __allowNull)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __object == null))
        {
            if (__allowNull)
            {
                return __trueValue;
            }
            else
            {
                return __falseValue;
            }
        }
        GuardingNode __anchorNode = SnippetAnchorNode.anchor();
        KlassPointer __nonNullObjectHub = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(__object, __anchorNode));
        // The hub of a primitive type can be null => always return false in this case.
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !__hub.isNull()))
        {
            if (TypeCheckSnippetUtils.checkUnknownSubType(__hub, __nonNullObjectHub))
            {
                return __trueValue;
            }
        }
        return __falseValue;
    }

    @Snippet
    public static Object isAssignableFrom(@Snippet.NonNullParameter Class<?> __thisClassNonNull, Class<?> __otherClass, Object __trueValue, Object __falseValue)
    {
        if (__otherClass == null)
        {
            DeoptimizeNode.deopt(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.NullCheckException);
            return false;
        }
        GuardingNode __anchorNode = SnippetAnchorNode.anchor();
        Class<?> __otherClassNonNull = PiNode.piCastNonNullClass(__otherClass, __anchorNode);

        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_LIKELY_PROBABILITY, __thisClassNonNull == __otherClassNonNull))
        {
            return __trueValue;
        }

        KlassPointer __thisHub = ClassGetHubNode.readClass(__thisClassNonNull);
        KlassPointer __otherHub = ClassGetHubNode.readClass(__otherClassNonNull);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !__thisHub.isNull()))
        {
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !__otherHub.isNull()))
            {
                GuardingNode __guardNonNull = SnippetAnchorNode.anchor();
                KlassPointer __nonNullOtherHub = ClassGetHubNode.piCastNonNull(__otherHub, __guardNonNull);
                if (TypeCheckSnippetUtils.checkUnknownSubType(__thisHub, __nonNullOtherHub))
                {
                    return __trueValue;
                }
            }
        }

        // If either hub is null, one of them is a primitive type and given that the class is not equal, return false.
        return __falseValue;
    }

    // @class InstanceOfSnippets.InstanceOfTemplates
    public static final class InstanceOfTemplates extends InstanceOfSnippetsTemplates
    {
        // @field
        private final SnippetTemplate.SnippetInfo ___instanceofWithProfile = snippet(InstanceOfSnippets.class, "instanceofWithProfile");
        // @field
        private final SnippetTemplate.SnippetInfo ___instanceofExact = snippet(InstanceOfSnippets.class, "instanceofExact");
        // @field
        private final SnippetTemplate.SnippetInfo ___instanceofPrimary = snippet(InstanceOfSnippets.class, "instanceofPrimary");
        // @field
        private final SnippetTemplate.SnippetInfo ___instanceofSecondary = snippet(InstanceOfSnippets.class, "instanceofSecondary", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___instanceofDynamic = snippet(InstanceOfSnippets.class, "instanceofDynamic", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___isAssignableFrom = snippet(InstanceOfSnippets.class, "isAssignableFrom", HotSpotReplacementsUtil.SECONDARY_SUPER_CACHE_LOCATION);

        // @cons InstanceOfSnippets.InstanceOfTemplates
        public InstanceOfTemplates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
        }

        @Override
        protected SnippetTemplate.Arguments makeArguments(InstanceOfSnippetsTemplates.InstanceOfUsageReplacer __replacer, LoweringTool __tool)
        {
            if (__replacer.___instanceOf instanceof InstanceOfNode)
            {
                InstanceOfNode __instanceOf = (InstanceOfNode) __replacer.___instanceOf;
                ValueNode __object = __instanceOf.getValue();
                Assumptions __assumptions = __instanceOf.graph().getAssumptions();

                JavaTypeProfile __profile = __instanceOf.profile();
                TypeCheckHints __hintInfo = new TypeCheckHints(__instanceOf.type(), __profile, __assumptions, GraalOptions.typeCheckMinProfileHitProbability, GraalOptions.typeCheckMaxHints);
                final HotSpotResolvedObjectType __type = (HotSpotResolvedObjectType) __instanceOf.type().getType();
                ConstantNode __hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), __type.klass(), this.___providers.getMetaAccess(), __instanceOf.graph());

                SnippetTemplate.Arguments __args;

                StructuredGraph __graph = __instanceOf.graph();
                if (__hintInfo.___hintHitProbability >= 1.0 && __hintInfo.___exact == null)
                {
                    TypeCheckSnippetUtils.Hints __hints = TypeCheckSnippetUtils.createHints(__hintInfo, this.___providers.getMetaAccess(), false, __graph);
                    __args = new SnippetTemplate.Arguments(this.___instanceofWithProfile, __graph.getGuardsStage(), __tool.getLoweringStage());
                    __args.add("object", __object);
                    __args.addVarargs("hints", KlassPointer.class, KlassPointerStamp.klassNonNull(), __hints.___hubs);
                    __args.addVarargs("hintIsPositive", boolean.class, StampFactory.forKind(JavaKind.Boolean), __hints.___isPositive);
                }
                else if (__hintInfo.___exact != null)
                {
                    __args = new SnippetTemplate.Arguments(this.___instanceofExact, __graph.getGuardsStage(), __tool.getLoweringStage());
                    __args.add("object", __object);
                    __args.add("exactHub", ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), ((HotSpotResolvedObjectType) __hintInfo.___exact).klass(), this.___providers.getMetaAccess(), __graph));
                }
                else if (__type.isPrimaryType())
                {
                    __args = new SnippetTemplate.Arguments(this.___instanceofPrimary, __graph.getGuardsStage(), __tool.getLoweringStage());
                    __args.add("hub", __hub);
                    __args.add("object", __object);
                    __args.addConst("superCheckOffset", __type.superCheckOffset());
                }
                else
                {
                    TypeCheckSnippetUtils.Hints __hints = TypeCheckSnippetUtils.createHints(__hintInfo, this.___providers.getMetaAccess(), false, __graph);
                    __args = new SnippetTemplate.Arguments(this.___instanceofSecondary, __graph.getGuardsStage(), __tool.getLoweringStage());
                    __args.add("hub", __hub);
                    __args.add("object", __object);
                    __args.addVarargs("hints", KlassPointer.class, KlassPointerStamp.klassNonNull(), __hints.___hubs);
                    __args.addVarargs("hintIsPositive", boolean.class, StampFactory.forKind(JavaKind.Boolean), __hints.___isPositive);
                }
                __args.add("trueValue", __replacer.___trueValue);
                __args.add("falseValue", __replacer.___falseValue);
                if (__hintInfo.___hintHitProbability >= 1.0 && __hintInfo.___exact == null)
                {
                    __args.addConst("nullSeen", __hintInfo.___profile.getNullSeen() != TriState.FALSE);
                }
                return __args;
            }
            else if (__replacer.___instanceOf instanceof InstanceOfDynamicNode)
            {
                InstanceOfDynamicNode __instanceOf = (InstanceOfDynamicNode) __replacer.___instanceOf;
                ValueNode __object = __instanceOf.getObject();

                SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___instanceofDynamic, __instanceOf.graph().getGuardsStage(), __tool.getLoweringStage());
                __args.add("hub", __instanceOf.getMirrorOrHub());
                __args.add("object", __object);
                __args.add("trueValue", __replacer.___trueValue);
                __args.add("falseValue", __replacer.___falseValue);
                __args.addConst("allowNull", __instanceOf.allowsNull());
                return __args;
            }
            else if (__replacer.___instanceOf instanceof ClassIsAssignableFromNode)
            {
                ClassIsAssignableFromNode __isAssignable = (ClassIsAssignableFromNode) __replacer.___instanceOf;
                SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___isAssignableFrom, __isAssignable.graph().getGuardsStage(), __tool.getLoweringStage());
                __args.add("thisClassNonNull", __isAssignable.getThisClass());
                __args.add("otherClass", __isAssignable.getOtherClass());
                __args.add("trueValue", __replacer.___trueValue);
                __args.add("falseValue", __replacer.___falseValue);
                return __args;
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
    }
}
