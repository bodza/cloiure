package giraaff.hotspot.replacements.arraycopy;

import java.lang.reflect.Method;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.UnmodifiableEconomicMap;
import org.graalvm.word.LocationIdentity;
import org.graalvm.word.WordFactory;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.graph.Node;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.extended.RawStoreNode;
import giraaff.nodes.java.ArrayLengthNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.BasicArrayCopyNode;
import giraaff.replacements.nodes.ExplodeLoopNode;
import giraaff.util.GraalError;
import giraaff.word.Word;

// @class ArrayCopySnippets
public final class ArrayCopySnippets implements Snippets
{
    // @enum ArrayCopySnippets.ArrayCopyTypeCheck
    private enum ArrayCopyTypeCheck
    {
        UNDEFINED_ARRAY_TYPE_CHECK,
        // either we know that both objects are arrays and have the same type,
        // or we apply generic array copy snippet, which enforces type check
        NO_ARRAY_TYPE_CHECK,
        // can be used when we know that one of the objects is a primitive array
        HUB_BASED_ARRAY_TYPE_CHECK,
        // can be used when we know that one of the objects is an object array
        LAYOUT_HELPER_BASED_ARRAY_TYPE_CHECK
    }

    @Snippet
    public static void arraycopyZeroLengthSnippet(Object src, int srcPos, Object dest, int destPos, int length, @ConstantParameter ArrayCopyTypeCheck arrayTypeCheck)
    {
        Object nonNullSrc = GraalDirectives.guardingNonNull(src);
        Object nonNullDest = GraalDirectives.guardingNonNull(dest);
        checkArrayTypes(nonNullSrc, nonNullDest, arrayTypeCheck);
        checkLimits(nonNullSrc, srcPos, nonNullDest, destPos, length);
    }

    @Snippet
    public static void arraycopyExactSnippet(Object src, int srcPos, Object dest, int destPos, int length, @ConstantParameter ArrayCopyTypeCheck arrayTypeCheck, @ConstantParameter JavaKind elementKind)
    {
        Object nonNullSrc = GraalDirectives.guardingNonNull(src);
        Object nonNullDest = GraalDirectives.guardingNonNull(dest);
        checkArrayTypes(nonNullSrc, nonNullDest, arrayTypeCheck);
        checkLimits(nonNullSrc, srcPos, nonNullDest, destPos, length);

        ArrayCopyCallNode.arraycopy(nonNullSrc, srcPos, nonNullDest, destPos, length, elementKind);
    }

    @Snippet
    public static void arraycopyUnrolledSnippet(Object src, int srcPos, Object dest, int destPos, int length, @ConstantParameter ArrayCopyTypeCheck arrayTypeCheck, @ConstantParameter JavaKind elementKind, @ConstantParameter int unrolledLength)
    {
        Object nonNullSrc = GraalDirectives.guardingNonNull(src);
        Object nonNullDest = GraalDirectives.guardingNonNull(dest);
        checkArrayTypes(nonNullSrc, nonNullDest, arrayTypeCheck);
        checkLimits(nonNullSrc, srcPos, nonNullDest, destPos, length);

        unrolledArraycopyWork(nonNullSrc, srcPos, nonNullDest, destPos, unrolledLength, elementKind);
    }

    @Snippet
    public static void arraycopyCheckcastSnippet(Object src, int srcPos, Object dest, int destPos, int length, @ConstantParameter ArrayCopyTypeCheck arrayTypeCheck, @ConstantParameter SnippetInfo workSnippet, @ConstantParameter JavaKind elementKind)
    {
        Object nonNullSrc = GraalDirectives.guardingNonNull(src);
        Object nonNullDest = GraalDirectives.guardingNonNull(dest);
        checkArrayTypes(nonNullSrc, nonNullDest, arrayTypeCheck);
        checkLimits(nonNullSrc, srcPos, nonNullDest, destPos, length);

        ArrayCopyWithSlowPathNode.arraycopy(nonNullSrc, srcPos, nonNullDest, destPos, length, workSnippet, elementKind);
    }

    @Snippet
    public static void arraycopyGenericSnippet(Object src, int srcPos, Object dest, int destPos, int length, @ConstantParameter ArrayCopyTypeCheck arrayTypeCheck, @ConstantParameter SnippetInfo workSnippet, @ConstantParameter JavaKind elementKind)
    {
        Object nonNullSrc = GraalDirectives.guardingNonNull(src);
        Object nonNullDest = GraalDirectives.guardingNonNull(dest);
        checkArrayTypes(nonNullSrc, nonNullDest, arrayTypeCheck);
        checkLimits(nonNullSrc, srcPos, nonNullDest, destPos, length);

        ArrayCopyWithSlowPathNode.arraycopy(nonNullSrc, srcPos, nonNullDest, destPos, length, workSnippet, elementKind);
    }

    @Snippet
    public static void arraycopyNativeSnippet(Object src, int srcPos, Object dest, int destPos, int length)
    {
        // all checks are done in the native method, so no need to emit additional checks here
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    // @Fold
    static LocationIdentity getArrayLocation(JavaKind kind)
    {
        return NamedLocationIdentity.getArrayLocation(kind);
    }

    private static void unrolledArraycopyWork(Object nonNullSrc, int srcPos, Object nonNullDest, int destPos, int length, JavaKind elementKind)
    {
        int scale = HotSpotReplacementsUtil.arrayIndexScale(elementKind);
        int arrayBaseOffset = HotSpotReplacementsUtil.arrayBaseOffset(elementKind);
        LocationIdentity arrayLocation = getArrayLocation(elementKind);

        long sourceOffset = arrayBaseOffset + (long) srcPos * scale;
        long destOffset = arrayBaseOffset + (long) destPos * scale;
        long position = 0;
        long delta = scale;
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, nonNullSrc == nonNullDest && srcPos < destPos))
        {
            // bad aliased case so we need to copy the array from back to front
            position = (long) (length - 1) * scale;
            delta = -delta;
        }

        // the length was already checked before - we can emit unconditional instructions
        ExplodeLoopNode.explodeLoop();
        for (int iteration = 0; iteration < length; iteration++)
        {
            Object value = RawLoadNode.load(nonNullSrc, sourceOffset + position, elementKind, arrayLocation);
            RawStoreNode.storeObject(nonNullDest, destOffset + position, value, elementKind, arrayLocation, false);
            position += delta;
        }
    }

    @Snippet(allowPartialIntrinsicArgumentMismatch = true)
    public static void checkcastArraycopyWithSlowPathWork(Object src, int srcPos, Object dest, int destPos, int length)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, length > 0))
        {
            Object nonNullSrc = PiNode.asNonNullObject(src);
            Object nonNullDest = PiNode.asNonNullObject(dest);
            KlassPointer srcKlass = HotSpotReplacementsUtil.loadHub(nonNullSrc);
            KlassPointer destKlass = HotSpotReplacementsUtil.loadHub(nonNullDest);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, srcKlass == destKlass))
            {
                // no storecheck required
                ArrayCopyCallNode.arraycopyObjectKillsAny(nonNullSrc, srcPos, nonNullDest, destPos, length);
            }
            else
            {
                KlassPointer destElemKlass = destKlass.readKlassPointer(GraalHotSpotVMConfig.arrayClassElementOffset, HotSpotReplacementsUtil.OBJ_ARRAY_KLASS_ELEMENT_KLASS_LOCATION);
                Word superCheckOffset = WordFactory.signed(destElemKlass.readInt(GraalHotSpotVMConfig.superCheckOffsetOffset, HotSpotReplacementsUtil.KLASS_SUPER_CHECK_OFFSET_LOCATION));

                int copiedElements = CheckcastArrayCopyCallNode.checkcastArraycopy(nonNullSrc, srcPos, nonNullDest, destPos, length, superCheckOffset, destElemKlass, false);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, copiedElements != 0))
                {
                    // the stub doesn't throw the ArrayStoreException, but returns the number of copied elements (xor'd with -1)
                    copiedElements ^= -1;
                    System.arraycopy(nonNullSrc, srcPos + copiedElements, nonNullDest, destPos + copiedElements, length - copiedElements);
                }
            }
        }
    }

    @Snippet(allowPartialIntrinsicArgumentMismatch = true)
    public static void genericArraycopyWithSlowPathWork(Object src, int srcPos, Object dest, int destPos, int length)
    {
        // The length > 0 check should not be placed here because generic array copy stub should
        // enforce type check. This is fine performance-wise because this snippet is rarely used.
        int copiedElements = GenericArrayCopyCallNode.genericArraycopy(src, srcPos, dest, destPos, length);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, copiedElements != 0))
        {
            // the stub doesn't throw the ArrayStoreException, but returns the number of copied elements (xor'd with -1)
            copiedElements ^= -1;
            System.arraycopy(src, srcPos + copiedElements, dest, destPos + copiedElements, length - copiedElements);
        }
    }

    private static void checkLimits(Object src, int srcPos, Object dest, int destPos, int length)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, srcPos < 0)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, destPos < 0)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, length < 0)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, srcPos > ArrayLengthNode.arrayLength(src) - length)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, destPos > ArrayLengthNode.arrayLength(dest) - length))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
    }

    private static void checkArrayTypes(Object nonNullSrc, Object nonNullDest, ArrayCopyTypeCheck arrayTypeCheck)
    {
        if (arrayTypeCheck == ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK)
        {
            // nothing to do
        }
        else if (arrayTypeCheck == ArrayCopyTypeCheck.HUB_BASED_ARRAY_TYPE_CHECK)
        {
            KlassPointer srcHub = HotSpotReplacementsUtil.loadHub(nonNullSrc);
            KlassPointer destHub = HotSpotReplacementsUtil.loadHub(nonNullDest);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, srcHub != destHub))
            {
                DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
            }
        }
        else if (arrayTypeCheck == ArrayCopyTypeCheck.LAYOUT_HELPER_BASED_ARRAY_TYPE_CHECK)
        {
            KlassPointer srcHub = HotSpotReplacementsUtil.loadHub(nonNullSrc);
            KlassPointer destHub = HotSpotReplacementsUtil.loadHub(nonNullDest);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, HotSpotReplacementsUtil.readLayoutHelper(srcHub) != HotSpotReplacementsUtil.readLayoutHelper(destHub)))
            {
                DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
            }
        }
    }

    // @class ArrayCopySnippets.Templates
    public static final class Templates extends SnippetTemplate.AbstractTemplates
    {
        private final SnippetInfo arraycopyGenericSnippet = snippet("arraycopyGenericSnippet");
        private final SnippetInfo arraycopyUnrolledSnippet = snippet("arraycopyUnrolledSnippet");
        private final SnippetInfo arraycopyExactSnippet = snippet("arraycopyExactSnippet");
        private final SnippetInfo arraycopyZeroLengthSnippet = snippet("arraycopyZeroLengthSnippet");
        private final SnippetInfo arraycopyCheckcastSnippet = snippet("arraycopyCheckcastSnippet");
        private final SnippetInfo arraycopyNativeSnippet = snippet("arraycopyNativeSnippet");

        private final SnippetInfo checkcastArraycopyWithSlowPathWork = snippet("checkcastArraycopyWithSlowPathWork");
        private final SnippetInfo genericArraycopyWithSlowPathWork = snippet("genericArraycopyWithSlowPathWork");

        private ResolvedJavaMethod originalArraycopy;

        // @cons
        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
        }

        protected SnippetInfo snippet(String methodName)
        {
            SnippetInfo info = snippet(ArrayCopySnippets.class, methodName, LocationIdentity.any());
            info.setOriginalMethod(originalArraycopy());
            return info;
        }

        public void lower(ArrayCopyNode arraycopy, LoweringTool tool)
        {
            JavaKind elementKind = selectComponentKind(arraycopy);
            SnippetInfo snippetInfo;
            ArrayCopyTypeCheck arrayTypeCheck;

            ResolvedJavaType srcType = StampTool.typeOrNull(arraycopy.getSource().stamp(NodeView.DEFAULT));
            ResolvedJavaType destType = StampTool.typeOrNull(arraycopy.getDestination().stamp(NodeView.DEFAULT));
            if (!canBeArray(srcType) || !canBeArray(destType))
            {
                // at least one of the objects is definitely not an array - use the native call
                // right away as the copying will fail anyways
                snippetInfo = arraycopyNativeSnippet;
                arrayTypeCheck = ArrayCopyTypeCheck.UNDEFINED_ARRAY_TYPE_CHECK;
            }
            else
            {
                ResolvedJavaType srcComponentType = srcType == null ? null : srcType.getComponentType();
                ResolvedJavaType destComponentType = destType == null ? null : destType.getComponentType();

                if (arraycopy.isExact())
                {
                    // there is a sufficient type match - we don't need any additional type checks
                    snippetInfo = arraycopyExactSnippet;
                    arrayTypeCheck = ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK;
                }
                else if (srcComponentType == null && destComponentType == null)
                {
                    // we don't know anything about the types - use the generic copying
                    snippetInfo = arraycopyGenericSnippet;
                    // no need for additional type check to avoid duplicated work
                    arrayTypeCheck = ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK;
                }
                else if (srcComponentType != null && destComponentType != null)
                {
                    if (!srcComponentType.isPrimitive() && !destComponentType.isPrimitive())
                    {
                        // it depends on the array content if the copy succeeds - we need
                        // a type check for every store
                        snippetInfo = arraycopyCheckcastSnippet;
                        arrayTypeCheck = ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK;
                    }
                    else
                    {
                        // one object is an object array, the other one is a primitive array:
                        // this copy will always fail - use the native call right away
                        snippetInfo = arraycopyNativeSnippet;
                        arrayTypeCheck = ArrayCopyTypeCheck.UNDEFINED_ARRAY_TYPE_CHECK;
                    }
                }
                else
                {
                    ResolvedJavaType nonNullComponentType = srcComponentType != null ? srcComponentType : destComponentType;
                    if (nonNullComponentType.isPrimitive())
                    {
                        // one involved object is a primitive array - it is sufficient to directly
                        // compare the hub
                        snippetInfo = arraycopyExactSnippet;
                        arrayTypeCheck = ArrayCopyTypeCheck.HUB_BASED_ARRAY_TYPE_CHECK;
                        elementKind = nonNullComponentType.getJavaKind();
                    }
                    else
                    {
                        // one involved object is an object array - the other array's element type
                        // may be primitive or object, hence we compare the layout helper
                        snippetInfo = arraycopyCheckcastSnippet;
                        arrayTypeCheck = ArrayCopyTypeCheck.LAYOUT_HELPER_BASED_ARRAY_TYPE_CHECK;
                    }
                }
            }

            // a few special cases that are easier to handle when all other variables already have a value
            if (snippetInfo != arraycopyNativeSnippet && snippetInfo != arraycopyGenericSnippet && arraycopy.getLength().isConstant() && arraycopy.getLength().asJavaConstant().asLong() == 0)
            {
                // Copying 0 element between object arrays with conflicting types will not throw an
                // exception - once we pass the preliminary element type checks that we are not
                // mixing arrays of different basic types, ArrayStoreException is only thrown when
                // an *astore would have thrown it. Therefore, copying null between object arrays
                // with conflicting types will also succeed (we do not optimize for such case here).
                snippetInfo = arraycopyZeroLengthSnippet;
            }
            else if (snippetInfo == arraycopyExactSnippet && shouldUnroll(arraycopy.getLength()))
            {
                snippetInfo = arraycopyUnrolledSnippet;
            }

            // create the snippet
            Arguments args = new Arguments(snippetInfo, arraycopy.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("src", arraycopy.getSource());
            args.add("srcPos", arraycopy.getSourcePosition());
            args.add("dest", arraycopy.getDestination());
            args.add("destPos", arraycopy.getDestinationPosition());
            args.add("length", arraycopy.getLength());
            if (snippetInfo != arraycopyNativeSnippet)
            {
                args.addConst("arrayTypeCheck", arrayTypeCheck);
            }
            if (snippetInfo == arraycopyUnrolledSnippet)
            {
                args.addConst("elementKind", elementKind != null ? elementKind : JavaKind.Illegal);
                args.addConst("unrolledLength", arraycopy.getLength().asJavaConstant().asInt());
            }
            if (snippetInfo == arraycopyExactSnippet)
            {
                args.addConst("elementKind", elementKind);
            }
            if (snippetInfo == arraycopyCheckcastSnippet)
            {
                args.addConst("workSnippet", checkcastArraycopyWithSlowPathWork);
                args.addConst("elementKind", JavaKind.Illegal);
            }
            if (snippetInfo == arraycopyGenericSnippet)
            {
                args.addConst("workSnippet", genericArraycopyWithSlowPathWork);
                args.addConst("elementKind", JavaKind.Illegal);
            }

            instantiate(args, arraycopy);
        }

        public void lower(ArrayCopyWithSlowPathNode arraycopy, LoweringTool tool)
        {
            StructuredGraph graph = arraycopy.graph();
            if (!graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                // if an arraycopy contains a slow path, we can't lower it right away
                return;
            }

            SnippetInfo snippetInfo = arraycopy.getSnippet();
            Arguments args = new Arguments(snippetInfo, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("src", arraycopy.getSource());
            args.add("srcPos", arraycopy.getSourcePosition());
            args.add("dest", arraycopy.getDestination());
            args.add("destPos", arraycopy.getDestinationPosition());
            args.add("length", arraycopy.getLength());
            instantiate(args, arraycopy);
        }

        private static boolean canBeArray(ResolvedJavaType type)
        {
            return type == null || type.isJavaLangObject() || type.isArray();
        }

        public static JavaKind selectComponentKind(BasicArrayCopyNode arraycopy)
        {
            ResolvedJavaType srcType = StampTool.typeOrNull(arraycopy.getSource().stamp(NodeView.DEFAULT));
            ResolvedJavaType destType = StampTool.typeOrNull(arraycopy.getDestination().stamp(NodeView.DEFAULT));

            if (srcType == null || !srcType.isArray() || destType == null || !destType.isArray())
            {
                return null;
            }
            if (!destType.getComponentType().isAssignableFrom(srcType.getComponentType()))
            {
                return null;
            }
            if (!arraycopy.isExact())
            {
                return null;
            }
            return srcType.getComponentType().getJavaKind();
        }

        private static boolean shouldUnroll(ValueNode length)
        {
            return length.isConstant() && length.asJavaConstant().asInt() <= 8 && length.asJavaConstant().asInt() != 0;
        }

        /**
         * Instantiate the snippet template and fix up the FrameState of any Invokes of
         * System.arraycopy and propagate the captured bci in the ArrayCopySlowPathNode.
         */
        private void instantiate(Arguments args, BasicArrayCopyNode arraycopy)
        {
            StructuredGraph graph = arraycopy.graph();
            SnippetTemplate template = template(arraycopy, args);
            UnmodifiableEconomicMap<Node, Node> replacements = template.instantiate(providers.getMetaAccess(), arraycopy, SnippetTemplate.DEFAULT_REPLACER, args, false);
            for (Node originalNode : replacements.getKeys())
            {
                if (originalNode instanceof Invoke)
                {
                    Invoke invoke = (Invoke) replacements.get(originalNode);
                    CallTargetNode call = invoke.callTarget();

                    if (!call.targetMethod().equals(originalArraycopy))
                    {
                        throw new GraalError("unexpected invoke %s in snippet", call.targetMethod());
                    }
                    // here we need to fix the bci of the invoke
                    InvokeNode newInvoke = graph.add(new InvokeNode(invoke.callTarget(), arraycopy.getBci()));
                    if (arraycopy.stateDuring() != null)
                    {
                        newInvoke.setStateDuring(arraycopy.stateDuring());
                    }
                    else
                    {
                        newInvoke.setStateAfter(arraycopy.stateAfter());
                    }
                    graph.replaceFixedWithFixed((InvokeNode) invoke.asNode(), newInvoke);
                }
                else if (originalNode instanceof ArrayCopyWithSlowPathNode)
                {
                    ArrayCopyWithSlowPathNode slowPath = (ArrayCopyWithSlowPathNode) replacements.get(originalNode);
                    slowPath.setBci(arraycopy.getBci());
                }
            }
            GraphUtil.killCFG(arraycopy);
        }

        private ResolvedJavaMethod originalArraycopy() throws GraalError
        {
            if (originalArraycopy == null)
            {
                Method method;
                try
                {
                    method = System.class.getDeclaredMethod("arraycopy", Object.class, int.class, Object.class, int.class, int.class);
                }
                catch (NoSuchMethodException | SecurityException e)
                {
                    throw new GraalError(e);
                }
                originalArraycopy = providers.getMetaAccess().lookupJavaMethod(method);
            }
            return originalArraycopy;
        }
    }

    // @cons
    private ArrayCopySnippets()
    {
        super();
    }
}
