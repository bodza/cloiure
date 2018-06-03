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
import giraaff.hotspot.HotSpotRuntime;
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
    // @cons
    private ArrayCopySnippets()
    {
        super();
    }

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
    public static void arraycopyZeroLengthSnippet(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantParameter ArrayCopyTypeCheck __arrayTypeCheck)
    {
        Object __nonNullSrc = GraalDirectives.guardingNonNull(__src);
        Object __nonNullDest = GraalDirectives.guardingNonNull(__dest);
        checkArrayTypes(__nonNullSrc, __nonNullDest, __arrayTypeCheck);
        checkLimits(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length);
    }

    @Snippet
    public static void arraycopyExactSnippet(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantParameter ArrayCopyTypeCheck __arrayTypeCheck, @ConstantParameter JavaKind __elementKind)
    {
        Object __nonNullSrc = GraalDirectives.guardingNonNull(__src);
        Object __nonNullDest = GraalDirectives.guardingNonNull(__dest);
        checkArrayTypes(__nonNullSrc, __nonNullDest, __arrayTypeCheck);
        checkLimits(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length);

        ArrayCopyCallNode.arraycopy(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length, __elementKind);
    }

    @Snippet
    public static void arraycopyUnrolledSnippet(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantParameter ArrayCopyTypeCheck __arrayTypeCheck, @ConstantParameter JavaKind __elementKind, @ConstantParameter int __unrolledLength)
    {
        Object __nonNullSrc = GraalDirectives.guardingNonNull(__src);
        Object __nonNullDest = GraalDirectives.guardingNonNull(__dest);
        checkArrayTypes(__nonNullSrc, __nonNullDest, __arrayTypeCheck);
        checkLimits(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length);

        unrolledArraycopyWork(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __unrolledLength, __elementKind);
    }

    @Snippet
    public static void arraycopyCheckcastSnippet(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantParameter ArrayCopyTypeCheck __arrayTypeCheck, @ConstantParameter SnippetInfo __workSnippet, @ConstantParameter JavaKind __elementKind)
    {
        Object __nonNullSrc = GraalDirectives.guardingNonNull(__src);
        Object __nonNullDest = GraalDirectives.guardingNonNull(__dest);
        checkArrayTypes(__nonNullSrc, __nonNullDest, __arrayTypeCheck);
        checkLimits(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length);

        ArrayCopyWithSlowPathNode.arraycopy(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length, __workSnippet, __elementKind);
    }

    @Snippet
    public static void arraycopyGenericSnippet(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantParameter ArrayCopyTypeCheck __arrayTypeCheck, @ConstantParameter SnippetInfo __workSnippet, @ConstantParameter JavaKind __elementKind)
    {
        Object __nonNullSrc = GraalDirectives.guardingNonNull(__src);
        Object __nonNullDest = GraalDirectives.guardingNonNull(__dest);
        checkArrayTypes(__nonNullSrc, __nonNullDest, __arrayTypeCheck);
        checkLimits(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length);

        ArrayCopyWithSlowPathNode.arraycopy(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length, __workSnippet, __elementKind);
    }

    @Snippet
    public static void arraycopyNativeSnippet(Object __src, int __srcPos, Object __dest, int __destPos, int __length)
    {
        // all checks are done in the native method, so no need to emit additional checks here
        System.arraycopy(__src, __srcPos, __dest, __destPos, __length);
    }

    // @Fold
    static LocationIdentity getArrayLocation(JavaKind __kind)
    {
        return NamedLocationIdentity.getArrayLocation(__kind);
    }

    private static void unrolledArraycopyWork(Object __nonNullSrc, int __srcPos, Object __nonNullDest, int __destPos, int __length, JavaKind __elementKind)
    {
        int __scale = HotSpotReplacementsUtil.arrayIndexScale(__elementKind);
        int __arrayBaseOffset = HotSpotReplacementsUtil.arrayBaseOffset(__elementKind);
        LocationIdentity __arrayLocation = getArrayLocation(__elementKind);

        long __sourceOffset = __arrayBaseOffset + (long) __srcPos * __scale;
        long __destOffset = __arrayBaseOffset + (long) __destPos * __scale;
        long __position = 0;
        long __delta = __scale;
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __nonNullSrc == __nonNullDest && __srcPos < __destPos))
        {
            // bad aliased case so we need to copy the array from back to front
            __position = (long) (__length - 1) * __scale;
            __delta = -__delta;
        }

        // the length was already checked before - we can emit unconditional instructions
        ExplodeLoopNode.explodeLoop();
        for (int __iteration = 0; __iteration < __length; __iteration++)
        {
            Object __value = RawLoadNode.load(__nonNullSrc, __sourceOffset + __position, __elementKind, __arrayLocation);
            RawStoreNode.storeObject(__nonNullDest, __destOffset + __position, __value, __elementKind, __arrayLocation, false);
            __position += __delta;
        }
    }

    @Snippet(allowPartialIntrinsicArgumentMismatch = true)
    public static void checkcastArraycopyWithSlowPathWork(Object __src, int __srcPos, Object __dest, int __destPos, int __length)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __length > 0))
        {
            Object __nonNullSrc = PiNode.asNonNullObject(__src);
            Object __nonNullDest = PiNode.asNonNullObject(__dest);
            KlassPointer __srcKlass = HotSpotReplacementsUtil.loadHub(__nonNullSrc);
            KlassPointer __destKlass = HotSpotReplacementsUtil.loadHub(__nonNullDest);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, __srcKlass == __destKlass))
            {
                // no storecheck required
                ArrayCopyCallNode.arraycopyObjectKillsAny(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length);
            }
            else
            {
                KlassPointer __destElemKlass = __destKlass.readKlassPointer(HotSpotRuntime.arrayClassElementOffset, HotSpotReplacementsUtil.OBJ_ARRAY_KLASS_ELEMENT_KLASS_LOCATION);
                Word __superCheckOffset = WordFactory.signed(__destElemKlass.readInt(HotSpotRuntime.superCheckOffsetOffset, HotSpotReplacementsUtil.KLASS_SUPER_CHECK_OFFSET_LOCATION));

                int __copiedElements = CheckcastArrayCopyCallNode.checkcastArraycopy(__nonNullSrc, __srcPos, __nonNullDest, __destPos, __length, __superCheckOffset, __destElemKlass, false);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __copiedElements != 0))
                {
                    // the stub doesn't throw the ArrayStoreException, but returns the number of copied elements (xor'd with -1)
                    __copiedElements ^= -1;
                    System.arraycopy(__nonNullSrc, __srcPos + __copiedElements, __nonNullDest, __destPos + __copiedElements, __length - __copiedElements);
                }
            }
        }
    }

    @Snippet(allowPartialIntrinsicArgumentMismatch = true)
    public static void genericArraycopyWithSlowPathWork(Object __src, int __srcPos, Object __dest, int __destPos, int __length)
    {
        // The length > 0 check should not be placed here because generic array copy stub should
        // enforce type check. This is fine performance-wise because this snippet is rarely used.
        int __copiedElements = GenericArrayCopyCallNode.genericArraycopy(__src, __srcPos, __dest, __destPos, __length);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __copiedElements != 0))
        {
            // the stub doesn't throw the ArrayStoreException, but returns the number of copied elements (xor'd with -1)
            __copiedElements ^= -1;
            System.arraycopy(__src, __srcPos + __copiedElements, __dest, __destPos + __copiedElements, __length - __copiedElements);
        }
    }

    private static void checkLimits(Object __src, int __srcPos, Object __dest, int __destPos, int __length)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __srcPos < 0)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __destPos < 0)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __length < 0)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __srcPos > ArrayLengthNode.arrayLength(__src) - __length)
         || BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __destPos > ArrayLengthNode.arrayLength(__dest) - __length))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
    }

    private static void checkArrayTypes(Object __nonNullSrc, Object __nonNullDest, ArrayCopyTypeCheck __arrayTypeCheck)
    {
        if (__arrayTypeCheck == ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK)
        {
            // nothing to do
        }
        else if (__arrayTypeCheck == ArrayCopyTypeCheck.HUB_BASED_ARRAY_TYPE_CHECK)
        {
            KlassPointer __srcHub = HotSpotReplacementsUtil.loadHub(__nonNullSrc);
            KlassPointer __destHub = HotSpotReplacementsUtil.loadHub(__nonNullDest);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __srcHub != __destHub))
            {
                DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
            }
        }
        else if (__arrayTypeCheck == ArrayCopyTypeCheck.LAYOUT_HELPER_BASED_ARRAY_TYPE_CHECK)
        {
            KlassPointer __srcHub = HotSpotReplacementsUtil.loadHub(__nonNullSrc);
            KlassPointer __destHub = HotSpotReplacementsUtil.loadHub(__nonNullDest);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, HotSpotReplacementsUtil.readLayoutHelper(__srcHub) != HotSpotReplacementsUtil.readLayoutHelper(__destHub)))
            {
                DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
            }
        }
    }

    // @class ArrayCopySnippets.Templates
    public static final class Templates extends SnippetTemplate.AbstractTemplates
    {
        // @field
        private final SnippetInfo ___arraycopyGenericSnippet = snippet("arraycopyGenericSnippet");
        // @field
        private final SnippetInfo ___arraycopyUnrolledSnippet = snippet("arraycopyUnrolledSnippet");
        // @field
        private final SnippetInfo ___arraycopyExactSnippet = snippet("arraycopyExactSnippet");
        // @field
        private final SnippetInfo ___arraycopyZeroLengthSnippet = snippet("arraycopyZeroLengthSnippet");
        // @field
        private final SnippetInfo ___arraycopyCheckcastSnippet = snippet("arraycopyCheckcastSnippet");
        // @field
        private final SnippetInfo ___arraycopyNativeSnippet = snippet("arraycopyNativeSnippet");

        // @field
        private final SnippetInfo ___checkcastArraycopyWithSlowPathWork = snippet("checkcastArraycopyWithSlowPathWork");
        // @field
        private final SnippetInfo ___genericArraycopyWithSlowPathWork = snippet("genericArraycopyWithSlowPathWork");

        // @field
        private ResolvedJavaMethod ___originalArraycopy;

        // @cons
        public Templates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
        }

        protected SnippetInfo snippet(String __methodName)
        {
            SnippetInfo __info = snippet(ArrayCopySnippets.class, __methodName, LocationIdentity.any());
            __info.setOriginalMethod(originalArraycopy());
            return __info;
        }

        public void lower(ArrayCopyNode __arraycopy, LoweringTool __tool)
        {
            JavaKind __elementKind = selectComponentKind(__arraycopy);
            SnippetInfo __snippetInfo;
            ArrayCopyTypeCheck __arrayTypeCheck;

            ResolvedJavaType __srcType = StampTool.typeOrNull(__arraycopy.getSource().stamp(NodeView.DEFAULT));
            ResolvedJavaType __destType = StampTool.typeOrNull(__arraycopy.getDestination().stamp(NodeView.DEFAULT));
            if (!canBeArray(__srcType) || !canBeArray(__destType))
            {
                // at least one of the objects is definitely not an array - use the native call
                // right away as the copying will fail anyways
                __snippetInfo = this.___arraycopyNativeSnippet;
                __arrayTypeCheck = ArrayCopyTypeCheck.UNDEFINED_ARRAY_TYPE_CHECK;
            }
            else
            {
                ResolvedJavaType __srcComponentType = __srcType == null ? null : __srcType.getComponentType();
                ResolvedJavaType __destComponentType = __destType == null ? null : __destType.getComponentType();

                if (__arraycopy.isExact())
                {
                    // there is a sufficient type match - we don't need any additional type checks
                    __snippetInfo = this.___arraycopyExactSnippet;
                    __arrayTypeCheck = ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK;
                }
                else if (__srcComponentType == null && __destComponentType == null)
                {
                    // we don't know anything about the types - use the generic copying
                    __snippetInfo = this.___arraycopyGenericSnippet;
                    // no need for additional type check to avoid duplicated work
                    __arrayTypeCheck = ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK;
                }
                else if (__srcComponentType != null && __destComponentType != null)
                {
                    if (!__srcComponentType.isPrimitive() && !__destComponentType.isPrimitive())
                    {
                        // it depends on the array content if the copy succeeds - we need
                        // a type check for every store
                        __snippetInfo = this.___arraycopyCheckcastSnippet;
                        __arrayTypeCheck = ArrayCopyTypeCheck.NO_ARRAY_TYPE_CHECK;
                    }
                    else
                    {
                        // one object is an object array, the other one is a primitive array:
                        // this copy will always fail - use the native call right away
                        __snippetInfo = this.___arraycopyNativeSnippet;
                        __arrayTypeCheck = ArrayCopyTypeCheck.UNDEFINED_ARRAY_TYPE_CHECK;
                    }
                }
                else
                {
                    ResolvedJavaType __nonNullComponentType = __srcComponentType != null ? __srcComponentType : __destComponentType;
                    if (__nonNullComponentType.isPrimitive())
                    {
                        // one involved object is a primitive array - it is sufficient to directly
                        // compare the hub
                        __snippetInfo = this.___arraycopyExactSnippet;
                        __arrayTypeCheck = ArrayCopyTypeCheck.HUB_BASED_ARRAY_TYPE_CHECK;
                        __elementKind = __nonNullComponentType.getJavaKind();
                    }
                    else
                    {
                        // one involved object is an object array - the other array's element type
                        // may be primitive or object, hence we compare the layout helper
                        __snippetInfo = this.___arraycopyCheckcastSnippet;
                        __arrayTypeCheck = ArrayCopyTypeCheck.LAYOUT_HELPER_BASED_ARRAY_TYPE_CHECK;
                    }
                }
            }

            // a few special cases that are easier to handle when all other variables already have a value
            if (__snippetInfo != this.___arraycopyNativeSnippet && __snippetInfo != this.___arraycopyGenericSnippet && __arraycopy.getLength().isConstant() && __arraycopy.getLength().asJavaConstant().asLong() == 0)
            {
                // Copying 0 element between object arrays with conflicting types will not throw an
                // exception - once we pass the preliminary element type checks that we are not
                // mixing arrays of different basic types, ArrayStoreException is only thrown when
                // an *astore would have thrown it. Therefore, copying null between object arrays
                // with conflicting types will also succeed (we do not optimize for such case here).
                __snippetInfo = this.___arraycopyZeroLengthSnippet;
            }
            else if (__snippetInfo == this.___arraycopyExactSnippet && shouldUnroll(__arraycopy.getLength()))
            {
                __snippetInfo = this.___arraycopyUnrolledSnippet;
            }

            // create the snippet
            Arguments __args = new Arguments(__snippetInfo, __arraycopy.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("src", __arraycopy.getSource());
            __args.add("srcPos", __arraycopy.getSourcePosition());
            __args.add("dest", __arraycopy.getDestination());
            __args.add("destPos", __arraycopy.getDestinationPosition());
            __args.add("length", __arraycopy.getLength());
            if (__snippetInfo != this.___arraycopyNativeSnippet)
            {
                __args.addConst("arrayTypeCheck", __arrayTypeCheck);
            }
            if (__snippetInfo == this.___arraycopyUnrolledSnippet)
            {
                __args.addConst("elementKind", __elementKind != null ? __elementKind : JavaKind.Illegal);
                __args.addConst("unrolledLength", __arraycopy.getLength().asJavaConstant().asInt());
            }
            if (__snippetInfo == this.___arraycopyExactSnippet)
            {
                __args.addConst("elementKind", __elementKind);
            }
            if (__snippetInfo == this.___arraycopyCheckcastSnippet)
            {
                __args.addConst("workSnippet", this.___checkcastArraycopyWithSlowPathWork);
                __args.addConst("elementKind", JavaKind.Illegal);
            }
            if (__snippetInfo == this.___arraycopyGenericSnippet)
            {
                __args.addConst("workSnippet", this.___genericArraycopyWithSlowPathWork);
                __args.addConst("elementKind", JavaKind.Illegal);
            }

            instantiate(__args, __arraycopy);
        }

        public void lower(ArrayCopyWithSlowPathNode __arraycopy, LoweringTool __tool)
        {
            StructuredGraph __graph = __arraycopy.graph();
            if (!__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                // if an arraycopy contains a slow path, we can't lower it right away
                return;
            }

            SnippetInfo __snippetInfo = __arraycopy.getSnippet();
            Arguments __args = new Arguments(__snippetInfo, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("src", __arraycopy.getSource());
            __args.add("srcPos", __arraycopy.getSourcePosition());
            __args.add("dest", __arraycopy.getDestination());
            __args.add("destPos", __arraycopy.getDestinationPosition());
            __args.add("length", __arraycopy.getLength());
            instantiate(__args, __arraycopy);
        }

        private static boolean canBeArray(ResolvedJavaType __type)
        {
            return __type == null || __type.isJavaLangObject() || __type.isArray();
        }

        public static JavaKind selectComponentKind(BasicArrayCopyNode __arraycopy)
        {
            ResolvedJavaType __srcType = StampTool.typeOrNull(__arraycopy.getSource().stamp(NodeView.DEFAULT));
            ResolvedJavaType __destType = StampTool.typeOrNull(__arraycopy.getDestination().stamp(NodeView.DEFAULT));

            if (__srcType == null || !__srcType.isArray() || __destType == null || !__destType.isArray())
            {
                return null;
            }
            if (!__destType.getComponentType().isAssignableFrom(__srcType.getComponentType()))
            {
                return null;
            }
            if (!__arraycopy.isExact())
            {
                return null;
            }
            return __srcType.getComponentType().getJavaKind();
        }

        private static boolean shouldUnroll(ValueNode __length)
        {
            return __length.isConstant() && __length.asJavaConstant().asInt() <= 8 && __length.asJavaConstant().asInt() != 0;
        }

        ///
        // Instantiate the snippet template and fix up the FrameState of any Invokes of
        // System.arraycopy and propagate the captured bci in the ArrayCopySlowPathNode.
        ///
        private void instantiate(Arguments __args, BasicArrayCopyNode __arraycopy)
        {
            StructuredGraph __graph = __arraycopy.graph();
            SnippetTemplate __template = template(__arraycopy, __args);
            UnmodifiableEconomicMap<Node, Node> __replacements = __template.instantiate(this.___providers.getMetaAccess(), __arraycopy, SnippetTemplate.DEFAULT_REPLACER, __args, false);
            for (Node __originalNode : __replacements.getKeys())
            {
                if (__originalNode instanceof Invoke)
                {
                    Invoke __invoke = (Invoke) __replacements.get(__originalNode);
                    CallTargetNode __call = __invoke.callTarget();

                    if (!__call.targetMethod().equals(this.___originalArraycopy))
                    {
                        throw new GraalError("unexpected invoke %s in snippet", __call.targetMethod());
                    }
                    // here we need to fix the bci of the invoke
                    InvokeNode __newInvoke = __graph.add(new InvokeNode(__invoke.callTarget(), __arraycopy.getBci()));
                    if (__arraycopy.stateDuring() != null)
                    {
                        __newInvoke.setStateDuring(__arraycopy.stateDuring());
                    }
                    else
                    {
                        __newInvoke.setStateAfter(__arraycopy.stateAfter());
                    }
                    __graph.replaceFixedWithFixed((InvokeNode) __invoke.asNode(), __newInvoke);
                }
                else if (__originalNode instanceof ArrayCopyWithSlowPathNode)
                {
                    ArrayCopyWithSlowPathNode __slowPath = (ArrayCopyWithSlowPathNode) __replacements.get(__originalNode);
                    __slowPath.setBci(__arraycopy.getBci());
                }
            }
            GraphUtil.killCFG(__arraycopy);
        }

        private ResolvedJavaMethod originalArraycopy()
        {
            if (this.___originalArraycopy == null)
            {
                Method __method;
                try
                {
                    __method = System.class.getDeclaredMethod("arraycopy", Object.class, int.class, Object.class, int.class, int.class);
                }
                catch (NoSuchMethodException | SecurityException __e)
                {
                    throw new GraalError(__e);
                }
                this.___originalArraycopy = this.___providers.getMetaAccess().lookupJavaMethod(__method);
            }
            return this.___originalArraycopy;
        }
    }
}
