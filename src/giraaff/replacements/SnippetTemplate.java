package giraaff.replacements;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Local;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaMethod.Parameter;
import jdk.vm.ci.meta.Signature;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableEconomicMap;
import org.graalvm.word.LocationIdentity;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.api.replacements.Snippet.NonNullParameter;
import giraaff.api.replacements.Snippet.VarargsParameter;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Graph.Mark;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.Position;
import giraaff.graph.iterators.NodePredicates;
import giraaff.loop.LoopEx;
import giraaff.loop.LoopsData;
import giraaff.loop.phases.LoopTransformations;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.PiNode.Placeholder;
import giraaff.nodes.PiNode.PlaceholderStamp;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.GuardsStage;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueNodeUtil;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.java.StoreIndexedNode;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryAnchorNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.MemoryMap;
import giraaff.nodes.memory.MemoryMapNode;
import giraaff.nodes.memory.MemoryNode;
import giraaff.nodes.memory.MemoryPhiNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.MemoryProxy;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.common.FloatingReadPhase;
import giraaff.phases.common.FloatingReadPhase.MemoryMapImpl;
import giraaff.phases.common.GuardLoweringPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.tiers.PhaseContext;
import giraaff.phases.util.Providers;
import giraaff.replacements.nodes.ExplodeLoopNode;
import giraaff.replacements.nodes.LoadSnippetVarargParameterNode;
import giraaff.util.GraalError;

/**
 * A snippet template is a graph created by parsing a snippet method and then specialized by binding
 * constants to the snippet's {@link ConstantParameter} parameters.
 *
 * Snippet templates can be managed in a cache maintained by {@link AbstractTemplates}.
 */
// @class SnippetTemplate
public class SnippetTemplate
{
    // @field
    private boolean mayRemoveLocation = false;

    /**
     * Holds the {@link ResolvedJavaMethod} of the snippet, together with some information about the
     * method that needs to be computed only once. The {@link SnippetInfo} should be created once
     * per snippet and then cached.
     */
    // @class SnippetTemplate.SnippetInfo
    public abstract static class SnippetInfo
    {
        // @field
        protected final ResolvedJavaMethod method;
        // @field
        protected ResolvedJavaMethod original;
        // @field
        protected final LocationIdentity[] privateLocations;

        /**
         * Lazily constructed parts of {@link SnippetInfo}.
         */
        // @class SnippetTemplate.SnippetInfo.Lazy
        static final class Lazy
        {
            // @cons
            Lazy(ResolvedJavaMethod __method)
            {
                super();
                int __count = __method.getSignature().getParameterCount(false);
                constantParameters = new boolean[__count];
                varargsParameters = new boolean[__count];
                nonNullParameters = new boolean[__count];
                for (int __i = 0; __i < __count; __i++)
                {
                    constantParameters[__i] = __method.getParameterAnnotation(ConstantParameter.class, __i) != null;
                    varargsParameters[__i] = __method.getParameterAnnotation(VarargsParameter.class, __i) != null;
                    nonNullParameters[__i] = __method.getParameterAnnotation(NonNullParameter.class, __i) != null;
                }
            }

            // @field
            final boolean[] constantParameters;
            // @field
            final boolean[] varargsParameters;
            // @field
            final boolean[] nonNullParameters;
        }

        protected abstract Lazy lazy();

        // @cons
        protected SnippetInfo(ResolvedJavaMethod __method, LocationIdentity[] __privateLocations)
        {
            super();
            this.method = __method;
            this.privateLocations = __privateLocations;
        }

        public ResolvedJavaMethod getMethod()
        {
            return method;
        }

        public int getParameterCount()
        {
            return lazy().constantParameters.length;
        }

        public void setOriginalMethod(ResolvedJavaMethod __original)
        {
            this.original = __original;
        }

        public boolean isConstantParameter(int __paramIdx)
        {
            return lazy().constantParameters[__paramIdx];
        }

        public boolean isVarargsParameter(int __paramIdx)
        {
            return lazy().varargsParameters[__paramIdx];
        }

        public boolean isNonNullParameter(int __paramIdx)
        {
            return lazy().nonNullParameters[__paramIdx];
        }
    }

    // @class SnippetTemplate.LazySnippetInfo
    protected static final class LazySnippetInfo extends SnippetInfo
    {
        // @field
        protected final AtomicReference<Lazy> lazy = new AtomicReference<>(null);

        // @cons
        protected LazySnippetInfo(ResolvedJavaMethod __method, LocationIdentity[] __privateLocations)
        {
            super(__method, __privateLocations);
        }

        @Override
        protected Lazy lazy()
        {
            if (lazy.get() == null)
            {
                lazy.compareAndSet(null, new Lazy(method));
            }
            return lazy.get();
        }
    }

    // @class SnippetTemplate.EagerSnippetInfo
    protected static final class EagerSnippetInfo extends SnippetInfo
    {
        // @field
        protected final Lazy lazy;

        // @cons
        protected EagerSnippetInfo(ResolvedJavaMethod __method, LocationIdentity[] __privateLocations)
        {
            super(__method, __privateLocations);
            lazy = new Lazy(__method);
        }

        @Override
        protected Lazy lazy()
        {
            return lazy;
        }
    }

    /**
     * Values that are bound to the snippet method parameters. The methods {@link #add},
     * {@link #addConst}, and {@link #addVarargs} must be called in the same order as in the
     * signature of the snippet method. The parameter name is passed to the add methods for
     * assertion checking, i.e., to enforce that the order matches. Which method needs to be called
     * depends on the annotation of the snippet method parameter:
     *
     * Use {@link #add} for a parameter without an annotation. The value is bound when the
     * {@link SnippetTemplate} is {@link SnippetTemplate#instantiate instantiated}.
     *
     * Use {@link #addConst} for a parameter annotated with {@link ConstantParameter}. The value
     * is bound when the {@link SnippetTemplate} is {@link SnippetTemplate#SnippetTemplate created}.
     *
     * Use {@link #addVarargs} for an array parameter annotated with {@link VarargsParameter}. A
     * separate {@link SnippetTemplate} is {@link SnippetTemplate#SnippetTemplate created} for every
     * distinct array length. The actual values are bound when the {@link SnippetTemplate} is
     * {@link SnippetTemplate#instantiate instantiated}
     */
    // @class SnippetTemplate.Arguments
    public static final class Arguments
    {
        // @field
        protected final SnippetInfo info;
        // @field
        protected final CacheKey cacheKey;
        // @field
        protected final Object[] values;
        // @field
        protected final Stamp[] constStamps;
        // @field
        protected boolean cacheable;

        // @field
        protected int nextParamIdx;

        // @cons
        public Arguments(SnippetInfo __info, GuardsStage __guardsStage, LoweringTool.LoweringStage __loweringStage)
        {
            super();
            this.info = __info;
            this.cacheKey = new CacheKey(__info, __guardsStage, __loweringStage);
            this.values = new Object[__info.getParameterCount()];
            this.constStamps = new Stamp[__info.getParameterCount()];
            this.cacheable = true;
        }

        public Arguments add(String __name, Object __value)
        {
            values[nextParamIdx] = __value;
            nextParamIdx++;
            return this;
        }

        public Arguments addConst(String __name, Object __value)
        {
            return addConst(__name, __value, null);
        }

        public Arguments addConst(String __name, Object __value, Stamp __stamp)
        {
            values[nextParamIdx] = __value;
            constStamps[nextParamIdx] = __stamp;
            cacheKey.setParam(nextParamIdx, __value);
            nextParamIdx++;
            return this;
        }

        public Arguments addVarargs(String __name, Class<?> __componentType, Stamp __argStamp, Object __value)
        {
            Varargs __varargs = new Varargs(__componentType, __argStamp, __value);
            values[nextParamIdx] = __varargs;
            // a separate template is necessary for every distinct array length
            cacheKey.setParam(nextParamIdx, __varargs.length);
            nextParamIdx++;
            return this;
        }

        public void setCacheable(boolean __cacheable)
        {
            this.cacheable = __cacheable;
        }
    }

    /**
     * Wrapper for the prototype value of a {@linkplain VarargsParameter varargs} parameter.
     */
    // @class SnippetTemplate.Varargs
    static final class Varargs
    {
        // @field
        protected final Class<?> componentType;
        // @field
        protected final Stamp stamp;
        // @field
        protected final Object value;
        // @field
        protected final int length;

        // @cons
        protected Varargs(Class<?> __componentType, Stamp __stamp, Object __value)
        {
            super();
            this.componentType = __componentType;
            this.stamp = __stamp;
            this.value = __value;
            if (__value instanceof List)
            {
                this.length = ((List<?>) __value).size();
            }
            else
            {
                this.length = Array.getLength(__value);
            }
        }
    }

    // @class SnippetTemplate.VarargsPlaceholderNode
    static final class VarargsPlaceholderNode extends FloatingNode implements ArrayLengthProvider
    {
        // @def
        public static final NodeClass<VarargsPlaceholderNode> TYPE = NodeClass.create(VarargsPlaceholderNode.class);

        // @field
        protected final Varargs varargs;

        // @cons
        protected VarargsPlaceholderNode(Varargs __varargs, MetaAccessProvider __metaAccess)
        {
            super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(__metaAccess.lookupJavaType(__varargs.componentType).getArrayClass())));
            this.varargs = __varargs;
        }

        @Override
        public ValueNode length()
        {
            return ConstantNode.forInt(varargs.length);
        }
    }

    // @class SnippetTemplate.CacheKey
    static final class CacheKey
    {
        // @field
        private final ResolvedJavaMethod method;
        // @field
        private final Object[] values;
        // @field
        private final GuardsStage guardsStage;
        // @field
        private final LoweringTool.LoweringStage loweringStage;
        // @field
        private int hash;

        // @cons
        protected CacheKey(SnippetInfo __info, GuardsStage __guardsStage, LoweringTool.LoweringStage __loweringStage)
        {
            super();
            this.method = __info.method;
            this.guardsStage = __guardsStage;
            this.loweringStage = __loweringStage;
            this.values = new Object[__info.getParameterCount()];
            this.hash = __info.method.hashCode() + 31 * __guardsStage.ordinal();
        }

        protected void setParam(int __paramIdx, Object __value)
        {
            values[__paramIdx] = __value;
            hash = (hash * 31) ^ (__value == null ? 0 : __value.hashCode());
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (!(__obj instanceof CacheKey))
            {
                return false;
            }
            CacheKey __other = (CacheKey) __obj;
            if (!method.equals(__other.method))
            {
                return false;
            }
            if (guardsStage != __other.guardsStage || loweringStage != __other.loweringStage)
            {
                return false;
            }
            for (int __i = 0; __i < values.length; __i++)
            {
                if (values[__i] != null && !values[__i].equals(__other.values[__i]))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            return hash;
        }
    }

    /**
     * Base class for snippet classes. It provides a cache for {@link SnippetTemplate}s.
     */
    // @class SnippetTemplate.AbstractTemplates
    public abstract static class AbstractTemplates
    {
        // @field
        protected final Providers providers;
        // @field
        protected final SnippetReflectionProvider snippetReflection;
        // @field
        protected final TargetDescription target;
        // @field
        private final Map<CacheKey, SnippetTemplate> templates;

        // @cons
        protected AbstractTemplates(Providers __providers, SnippetReflectionProvider __snippetReflection, TargetDescription __target)
        {
            super();
            this.providers = __providers;
            this.snippetReflection = __snippetReflection;
            this.target = __target;
            if (GraalOptions.useSnippetTemplateCache)
            {
                int __size = GraalOptions.maxTemplatesPerSnippet;
                this.templates = Collections.synchronizedMap(new LRUCache<>(__size, __size));
            }
            else
            {
                this.templates = null;
            }
        }

        public static Method findMethod(Class<? extends Snippets> __declaringClass, String __methodName, Method __except)
        {
            for (Method __m : __declaringClass.getDeclaredMethods())
            {
                if (__m.getName().equals(__methodName) && !__m.equals(__except))
                {
                    return __m;
                }
            }
            return null;
        }

        /**
         * Finds the unique method in {@code declaringClass} named {@code methodName} annotated by
         * {@link Snippet} and returns a {@link SnippetInfo} value describing it. There must be
         * exactly one snippet method in {@code declaringClass}.
         */
        protected SnippetInfo snippet(Class<? extends Snippets> __declaringClass, String __methodName, LocationIdentity... __initialPrivateLocations)
        {
            Method __method = findMethod(__declaringClass, __methodName, null);
            ResolvedJavaMethod __javaMethod = providers.getMetaAccess().lookupJavaMethod(__method);
            if (GraalOptions.eagerSnippets)
            {
                return new EagerSnippetInfo(__javaMethod, __initialPrivateLocations);
            }
            else
            {
                return new LazySnippetInfo(__javaMethod, __initialPrivateLocations);
            }
        }

        /**
         * Gets a template for a given key, creating it first if necessary.
         */
        protected SnippetTemplate template(ValueNode __replacee, final Arguments __args)
        {
            StructuredGraph __graph = __replacee.graph();
            SnippetTemplate __template = GraalOptions.useSnippetTemplateCache && __args.cacheable ? templates.get(__args.cacheKey) : null;
            if (__template == null)
            {
                __template = new SnippetTemplate(providers, snippetReflection, __args, __replacee);
                if (GraalOptions.useSnippetTemplateCache && __args.cacheable)
                {
                    templates.put(__args.cacheKey, __template);
                }
            }
            return __template;
        }
    }

    // @class SnippetTemplate.LRUCache
    private static final class LRUCache<K, V> extends LinkedHashMap<K, V>
    {
        // @field
        private final int maxCacheSize;

        // @cons
        LRUCache(int __initialCapacity, int __maxCacheSize)
        {
            super(__initialCapacity, 0.75F, true);
            this.maxCacheSize = __maxCacheSize;
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<K, V> __eldest)
        {
            return size() > maxCacheSize;
        }
    }

    // These values must be compared with equals() not '==' to support replay compilation.
    // @def
    private static final Object UNUSED_PARAMETER = "UNUSED_PARAMETER";
    // @def
    private static final Object CONSTANT_PARAMETER = "CONSTANT_PARAMETER";

    /**
     * Determines if any parameter of a given method is annotated with {@link ConstantParameter}.
     */
    public static boolean hasConstantParameter(ResolvedJavaMethod __method)
    {
        for (ConstantParameter __p : __method.getParameterAnnotations(ConstantParameter.class))
        {
            if (__p != null)
            {
                return true;
            }
        }
        return false;
    }

    // @field
    private final SnippetReflectionProvider snippetReflection;

    /**
     * Creates a snippet template.
     */
    // @cons
    protected SnippetTemplate(final Providers __providers, SnippetReflectionProvider __snippetReflection, Arguments __args, Node __replacee)
    {
        super();
        this.snippetReflection = __snippetReflection;
        this.info = __args.info;

        Object[] __constantArgs = getConstantArgs(__args);
        StructuredGraph __snippetGraph = __providers.getReplacements().getSnippet(__args.info.method, __args.info.original, __constantArgs);

        ResolvedJavaMethod __method = __snippetGraph.method();
        Signature __signature = __method.getSignature();

        PhaseContext __phaseContext = new PhaseContext(__providers);

        // copy snippet graph replacing constant parameters with given arguments
        final StructuredGraph __snippetCopy = new StructuredGraph.Builder().method(__snippetGraph.method()).build();
        if (!__snippetGraph.isUnsafeAccessTrackingEnabled())
        {
            __snippetCopy.disableUnsafeAccessTracking();
        }

        EconomicMap<Node, Node> __nodeReplacements = EconomicMap.create(Equivalence.IDENTITY);
        __nodeReplacements.put(__snippetGraph.start(), __snippetCopy.start());

        MetaAccessProvider __metaAccess = __providers.getMetaAccess();

        int __parameterCount = __args.info.getParameterCount();
        VarargsPlaceholderNode[] __placeholders = new VarargsPlaceholderNode[__parameterCount];

        for (int __i = 0; __i < __parameterCount; __i++)
        {
            ParameterNode __parameter = __snippetGraph.getParameter(__i);
            if (__parameter != null)
            {
                if (__args.info.isConstantParameter(__i))
                {
                    Object __arg = __args.values[__i];
                    JavaKind __kind = __signature.getParameterKind(__i);
                    ConstantNode __constantNode;
                    if (__arg instanceof Constant)
                    {
                        Stamp __stamp = __args.constStamps[__i];
                        if (__stamp == null)
                        {
                            __constantNode = ConstantNode.forConstant((JavaConstant) __arg, __metaAccess, __snippetCopy);
                        }
                        else
                        {
                            __constantNode = ConstantNode.forConstant(__stamp, (Constant) __arg, __metaAccess, __snippetCopy);
                        }
                    }
                    else
                    {
                        __constantNode = ConstantNode.forConstant(__snippetReflection.forBoxed(__kind, __arg), __metaAccess, __snippetCopy);
                    }
                    __nodeReplacements.put(__parameter, __constantNode);
                }
                else if (__args.info.isVarargsParameter(__i))
                {
                    Varargs __varargs = (Varargs) __args.values[__i];
                    VarargsPlaceholderNode __placeholder = __snippetCopy.unique(new VarargsPlaceholderNode(__varargs, __providers.getMetaAccess()));
                    __nodeReplacements.put(__parameter, __placeholder);
                    __placeholders[__i] = __placeholder;
                }
                else if (__args.info.isNonNullParameter(__i))
                {
                    __parameter.setStamp(__parameter.stamp(NodeView.DEFAULT).join(StampFactory.objectNonNull()));
                }
            }
        }
        __snippetCopy.addDuplicates(__snippetGraph.getNodes(), __snippetGraph, __snippetGraph.getNodeCount(), __nodeReplacements);

        // gather the template parameters
        parameters = new Object[__parameterCount];
        for (int __i = 0; __i < __parameterCount; __i++)
        {
            if (__args.info.isConstantParameter(__i))
            {
                parameters[__i] = CONSTANT_PARAMETER;
            }
            else if (__args.info.isVarargsParameter(__i))
            {
                Varargs __varargs = (Varargs) __args.values[__i];
                int __length = __varargs.length;
                ParameterNode[] __params = new ParameterNode[__length];
                Stamp __stamp = __varargs.stamp;
                for (int __j = 0; __j < __length; __j++)
                {
                    // use a decimal friendly numbering make it more obvious how values map
                    int __idx = (__i + 1) * 10000 + __j;
                    ParameterNode __local = __snippetCopy.addOrUnique(new ParameterNode(__idx, StampPair.createSingle(__stamp)));
                    __params[__j] = __local;
                }
                parameters[__i] = __params;

                VarargsPlaceholderNode __placeholder = __placeholders[__i];
                if (__placeholder != null)
                {
                    for (Node __usage : __placeholder.usages().snapshot())
                    {
                        if (__usage instanceof LoadIndexedNode)
                        {
                            LoadIndexedNode __loadIndexed = (LoadIndexedNode) __usage;
                            LoadSnippetVarargParameterNode __loadSnippetParameter = __snippetCopy.add(new LoadSnippetVarargParameterNode(__params, __loadIndexed.index(), __loadIndexed.stamp(NodeView.DEFAULT)));
                            __snippetCopy.replaceFixedWithFixed(__loadIndexed, __loadSnippetParameter);
                        }
                        else if (__usage instanceof StoreIndexedNode)
                        {
                            /*
                             * The template lowering doesn't really treat this as an array,
                             * so you can't store back into the varargs. Allocate your own
                             * array if you really need this and EA should eliminate it.
                             */
                            throw new GraalError("Can't store into VarargsParameter array");
                        }
                    }
                }
            }
            else
            {
                ParameterNode __local = __snippetCopy.getParameter(__i);
                if (__local == null)
                {
                    // parameter value was eliminated
                    parameters[__i] = UNUSED_PARAMETER;
                }
                else
                {
                    parameters[__i] = __local;
                }
            }
        }

        explodeLoops(__snippetCopy, __phaseContext);

        GuardsStage __guardsStage = __args.cacheKey.guardsStage;
        // perform lowering on the snippet
        if (!__guardsStage.allowsFloatingGuards())
        {
            new GuardLoweringPhase().apply(__snippetCopy, null);
        }
        __snippetCopy.setGuardsStage(__guardsStage);
        new LoweringPhase(new CanonicalizerPhase(), __args.cacheKey.loweringStage).apply(__snippetCopy, __phaseContext);

        ArrayList<StateSplit> __curSideEffectNodes = new ArrayList<>();
        ArrayList<DeoptimizingNode> __curDeoptNodes = new ArrayList<>();
        ArrayList<ValueNode> __curPlaceholderStampedNodes = new ArrayList<>();
        for (Node __node : __snippetCopy.getNodes())
        {
            if (__node instanceof ValueNode)
            {
                ValueNode __valueNode = (ValueNode) __node;
                if (__valueNode.stamp(NodeView.DEFAULT) == PlaceholderStamp.singleton())
                {
                    __curPlaceholderStampedNodes.add(__valueNode);
                }
            }

            if (__node instanceof StateSplit)
            {
                StateSplit __stateSplit = (StateSplit) __node;
                FrameState __frameState = __stateSplit.stateAfter();
                if (__stateSplit.hasSideEffect())
                {
                    __curSideEffectNodes.add((StateSplit) __node);
                }
                if (__frameState != null)
                {
                    __stateSplit.setStateAfter(null);
                }
            }
            if (__node instanceof DeoptimizingNode)
            {
                DeoptimizingNode __deoptNode = (DeoptimizingNode) __node;
                if (__deoptNode.canDeoptimize())
                {
                    __curDeoptNodes.add(__deoptNode);
                }
            }
        }

        new DeadCodeEliminationPhase(Optionality.Required).apply(__snippetCopy);

        new FloatingReadPhase(true, true).apply(__snippetCopy);
        new RemoveValueProxyPhase().apply(__snippetCopy);

        MemoryAnchorNode __anchor = __snippetCopy.add(new MemoryAnchorNode());
        __snippetCopy.start().replaceAtUsages(InputType.Memory, __anchor);

        this.snippet = __snippetCopy;

        StartNode __entryPointNode = snippet.start();
        if (__anchor.hasNoUsages())
        {
            __anchor.safeDelete();
            this.memoryAnchor = null;
        }
        else
        {
            // find out if all the return memory maps point to the anchor (i.e. there's no kill anywhere)
            boolean __needsMemoryMaps = false;
            for (ReturnNode __retNode : snippet.getNodes(ReturnNode.TYPE))
            {
                MemoryMapNode __memoryMap = __retNode.getMemoryMap();
                if (__memoryMap.getLocations().size() > 1 || __memoryMap.getLastLocationAccess(LocationIdentity.any()) != __anchor)
                {
                    __needsMemoryMaps = true;
                    break;
                }
            }
            boolean __needsAnchor;
            if (__needsMemoryMaps)
            {
                __needsAnchor = true;
            }
            else
            {
                // check that all those memory maps where the only usages of the anchor
                __needsAnchor = __anchor.usages().filter(NodePredicates.isNotA(MemoryMapNode.class)).isNotEmpty();
                // remove the useless memory map
                MemoryMapNode __memoryMap = null;
                for (ReturnNode __retNode : snippet.getNodes(ReturnNode.TYPE))
                {
                    if (__memoryMap == null)
                    {
                        __memoryMap = __retNode.getMemoryMap();
                    }
                    __retNode.setMemoryMap(null);
                }
                __memoryMap.safeDelete();
            }
            if (__needsAnchor)
            {
                __snippetCopy.addAfterFixed(__snippetCopy.start(), __anchor);
                this.memoryAnchor = __anchor;
            }
            else
            {
                __anchor.safeDelete();
                this.memoryAnchor = null;
            }
        }

        List<ReturnNode> __returnNodes = snippet.getNodes(ReturnNode.TYPE).snapshot();
        if (__returnNodes.isEmpty())
        {
            this.returnNode = null;
        }
        else if (__returnNodes.size() == 1)
        {
            this.returnNode = __returnNodes.get(0);
        }
        else
        {
            AbstractMergeNode __merge = snippet.add(new MergeNode());
            List<MemoryMapNode> __memMaps = new ArrayList<>();
            for (ReturnNode __retNode : __returnNodes)
            {
                MemoryMapNode __memoryMapNode = __retNode.getMemoryMap();
                if (__memoryMapNode != null)
                {
                    __memMaps.add(__memoryMapNode);
                }
            }

            ValueNode __returnValue = InliningUtil.mergeReturns(__merge, __returnNodes);
            this.returnNode = snippet.add(new ReturnNode(__returnValue));
            if (!__memMaps.isEmpty())
            {
                MemoryMapImpl __mmap = FloatingReadPhase.mergeMemoryMaps(__merge, __memMaps);
                MemoryMapNode __memoryMap = snippet.unique(new MemoryMapNode(__mmap.getMap()));
                this.returnNode.setMemoryMap(__memoryMap);
                for (MemoryMapNode __mm : __memMaps)
                {
                    if (__mm != __memoryMap && __mm.isAlive())
                    {
                        GraphUtil.killWithUnusedFloatingInputs(__mm);
                    }
                }
            }
            __merge.setNext(this.returnNode);
        }

        this.sideEffectNodes = __curSideEffectNodes;
        this.deoptNodes = __curDeoptNodes;
        this.placeholderStampedNodes = __curPlaceholderStampedNodes;

        nodes = new ArrayList<>(snippet.getNodeCount());
        for (Node __node : snippet.getNodes())
        {
            if (__node != __entryPointNode && __node != __entryPointNode.stateAfter())
            {
                nodes.add(__node);
            }
        }

        this.snippet.freeze();
    }

    public static void explodeLoops(final StructuredGraph __snippetCopy, PhaseContext __phaseContext)
    {
        // do any required loop explosion
        boolean __exploded = false;
        do
        {
            __exploded = false;
            ExplodeLoopNode __explodeLoop = __snippetCopy.getNodes().filter(ExplodeLoopNode.class).first();
            if (__explodeLoop != null) // earlier canonicalization may have removed the loop altogether
            {
                LoopBeginNode __loopBegin = __explodeLoop.findLoopBegin();
                if (__loopBegin != null)
                {
                    LoopEx __loop = new LoopsData(__snippetCopy).loop(__loopBegin);
                    Mark __mark = __snippetCopy.getMark();
                    LoopTransformations.fullUnroll(__loop, __phaseContext, new CanonicalizerPhase());
                    new CanonicalizerPhase().applyIncremental(__snippetCopy, __phaseContext, __mark);
                    __loop.deleteUnusedNodes();
                }
                GraphUtil.removeFixedWithUnusedInputs(__explodeLoop);
                __exploded = true;
            }
        } while (__exploded);
    }

    protected Object[] getConstantArgs(Arguments __args)
    {
        Object[] __constantArgs = __args.values.clone();
        for (int __i = 0; __i < __args.info.getParameterCount(); __i++)
        {
            if (!__args.info.isConstantParameter(__i))
            {
                __constantArgs[__i] = null;
            }
        }
        return __constantArgs;
    }

    /**
     * The graph built from the snippet method.
     */
    // @field
    private final StructuredGraph snippet;

    // @field
    private final SnippetInfo info;

    /**
     * The named parameters of this template that must be bound to values during instantiation. For
     * a parameter that is still live after specialization, the value in this map is either a
     * {@link ParameterNode} instance or a {@link ParameterNode} array. For an eliminated parameter,
     * the value is identical to the key.
     */
    // @field
    private final Object[] parameters;

    /**
     * The return node (if any) of the snippet.
     */
    // @field
    private final ReturnNode returnNode;

    /**
     * The memory anchor (if any) of the snippet.
     */
    // @field
    private final MemoryAnchorNode memoryAnchor;

    /**
     * Nodes that inherit the {@link StateSplit#stateAfter()} from the replacee during instantiation.
     */
    // @field
    private final ArrayList<StateSplit> sideEffectNodes;

    /**
     * Nodes that inherit a deoptimization {@link FrameState} from the replacee during instantiation.
     */
    // @field
    private final ArrayList<DeoptimizingNode> deoptNodes;

    /**
     * Nodes that have a stamp originating from a {@link Placeholder}.
     */
    // @field
    private final ArrayList<ValueNode> placeholderStampedNodes;

    /**
     * The nodes to be inlined when this specialization is instantiated.
     */
    // @field
    private final ArrayList<Node> nodes;

    /**
     * Gets the instantiation-time bindings to this template's parameters.
     *
     * @return the map that will be used to bind arguments to parameters when inlining this template
     */
    private EconomicMap<Node, Node> bind(StructuredGraph __replaceeGraph, MetaAccessProvider __metaAccess, Arguments __args)
    {
        EconomicMap<Node, Node> __replacements = EconomicMap.create(Equivalence.IDENTITY);
        for (int __i = 0; __i < parameters.length; __i++)
        {
            Object __parameter = parameters[__i];
            Object __argument = __args.values[__i];
            if (__parameter instanceof ParameterNode)
            {
                if (__argument instanceof ValueNode)
                {
                    __replacements.put((ParameterNode) __parameter, (ValueNode) __argument);
                }
                else
                {
                    JavaKind __kind = ((ParameterNode) __parameter).getStackKind();
                    JavaConstant __constant = forBoxed(__argument, __kind);
                    __replacements.put((ParameterNode) __parameter, ConstantNode.forConstant(__constant, __metaAccess, __replaceeGraph));
                }
            }
            else if (__parameter instanceof ParameterNode[])
            {
                ParameterNode[] __params = (ParameterNode[]) __parameter;
                Varargs __varargs = (Varargs) __argument;
                int __length = __params.length;
                List<?> __list = null;
                Object __array = null;
                if (__varargs.value instanceof List)
                {
                    __list = (List<?>) __varargs.value;
                }
                else
                {
                    __array = __varargs.value;
                }

                for (int __j = 0; __j < __length; __j++)
                {
                    ParameterNode __param = __params[__j];
                    Object __value = __list != null ? __list.get(__j) : Array.get(__array, __j);
                    if (__value instanceof ValueNode)
                    {
                        __replacements.put(__param, (ValueNode) __value);
                    }
                    else
                    {
                        JavaConstant __constant = forBoxed(__value, __param.getStackKind());
                        ConstantNode __element = ConstantNode.forConstant(__constant, __metaAccess, __replaceeGraph);
                        __replacements.put(__param, __element);
                    }
                }
            }
        }
        return __replacements;
    }

    /**
     * Converts a Java boxed value to a {@link JavaConstant} of the right kind. This adjusts for the
     * limitation that a {@link Local}'s kind is a {@linkplain JavaKind#getStackKind() stack kind}
     * and so cannot be used for re-boxing primitives smaller than an int.
     *
     * @param argument a Java boxed value
     * @param localKind the kind of the {@link Local} to which {@code argument} will be bound
     */
    protected JavaConstant forBoxed(Object __argument, JavaKind __localKind)
    {
        if (__localKind == JavaKind.Int)
        {
            return JavaConstant.forBoxedPrimitive(__argument);
        }
        return snippetReflection.forBoxed(__localKind, __argument);
    }

    /**
     * Logic for replacing a snippet-lowered node at its usages with the return value of the
     * snippet. An alternative to the {@linkplain SnippetTemplate#DEFAULT_REPLACER default}
     * replacement logic can be used to handle mismatches between the stamp of the node being
     * lowered and the stamp of the snippet's return value.
     */
    // @iface SnippetTemplate.UsageReplacer
    public interface UsageReplacer
    {
        /**
         * Replaces all usages of {@code oldNode} with direct or indirect usages of {@code newNode}.
         */
        void replace(ValueNode oldNode, ValueNode newNode);
    }

    /**
     * Represents the default {@link UsageReplacer usage replacer} logic which simply delegates to
     * {@link Node#replaceAtUsages(Node)}.
     */
    // @closure
    public static final UsageReplacer DEFAULT_REPLACER = new UsageReplacer()
    {
        @Override
        public void replace(ValueNode __oldNode, ValueNode __newNode)
        {
            if (__newNode != null)
            {
                __oldNode.replaceAtUsages(__newNode);
            }
        }
    };

    private boolean assertSnippetKills(ValueNode __replacee)
    {
        if (!__replacee.graph().isAfterFloatingReadPhase())
        {
            // no floating reads yet, ignore locations created while lowering
            return true;
        }
        if (returnNode == null)
        {
            // the snippet terminates control flow
            return true;
        }
        MemoryMapNode __memoryMap = returnNode.getMemoryMap();
        if (__memoryMap == null || __memoryMap.isEmpty())
        {
            // there are no kills in the snippet graph
            return true;
        }

        EconomicSet<LocationIdentity> __kills = EconomicSet.create(Equivalence.DEFAULT);
        __kills.addAll(__memoryMap.getLocations());

        if (__replacee instanceof MemoryCheckpoint.Single)
        {
            // check if some node in snippet graph also kills the same location
            LocationIdentity __locationIdentity = ((MemoryCheckpoint.Single) __replacee).getLocationIdentity();
            if (__locationIdentity.isAny())
            {
                // if the replacee kills ANY_LOCATION, the snippet can kill arbitrary locations
                return true;
            }
            __kills.remove(__locationIdentity);
        }

        // remove ANY_LOCATION if it's just a kill by the start node
        if (__memoryMap.getLastLocationAccess(LocationIdentity.any()) instanceof MemoryAnchorNode)
        {
            __kills.remove(LocationIdentity.any());
        }

        // node can only lower to a ANY_LOCATION kill if the replacee also kills ANY_LOCATION

        /*
         * Kills to private locations are safe, since there can be no floating read to these
         * locations except reads that are introduced by the snippet itself or related snippets in
         * the same lowering round. These reads are anchored to a MemoryAnchor at the beginning of
         * their snippet, so they can not float above a kill in another instance of the same snippet.
         */
        for (LocationIdentity __p : this.info.privateLocations)
        {
            __kills.remove(__p);
        }

        return true;
    }

    // @class SnippetTemplate.MemoryInputMap
    private static class MemoryInputMap implements MemoryMap
    {
        // @field
        private final LocationIdentity locationIdentity;
        // @field
        private final MemoryNode lastLocationAccess;

        // @cons
        MemoryInputMap(ValueNode __replacee)
        {
            super();
            if (__replacee instanceof MemoryAccess)
            {
                MemoryAccess __access = (MemoryAccess) __replacee;
                locationIdentity = __access.getLocationIdentity();
                lastLocationAccess = __access.getLastLocationAccess();
            }
            else
            {
                locationIdentity = null;
                lastLocationAccess = null;
            }
        }

        @Override
        public MemoryNode getLastLocationAccess(LocationIdentity __location)
        {
            if (locationIdentity != null && locationIdentity.equals(__location))
            {
                return lastLocationAccess;
            }
            else
            {
                return null;
            }
        }

        @Override
        public Collection<LocationIdentity> getLocations()
        {
            if (locationIdentity == null)
            {
                return Collections.emptySet();
            }
            else
            {
                return Collections.singleton(locationIdentity);
            }
        }
    }

    // @class SnippetTemplate.MemoryOutputMap
    // @closure
    private final class MemoryOutputMap extends MemoryInputMap
    {
        // @field
        private final UnmodifiableEconomicMap<Node, Node> duplicates;

        // @cons
        MemoryOutputMap(ValueNode __replacee, UnmodifiableEconomicMap<Node, Node> __duplicates)
        {
            super(__replacee);
            this.duplicates = __duplicates;
        }

        @Override
        public MemoryNode getLastLocationAccess(LocationIdentity __locationIdentity)
        {
            MemoryNode __lastLocationAccess = SnippetTemplate.this.returnNode.getMemoryMap().getLastLocationAccess(__locationIdentity);
            if (__lastLocationAccess == SnippetTemplate.this.memoryAnchor)
            {
                return super.getLastLocationAccess(__locationIdentity);
            }
            else
            {
                return (MemoryNode) duplicates.get(ValueNodeUtil.asNode(__lastLocationAccess));
            }
        }

        @Override
        public Collection<LocationIdentity> getLocations()
        {
            return SnippetTemplate.this.returnNode.getMemoryMap().getLocations();
        }
    }

    private void rewireMemoryGraph(ValueNode __replacee, UnmodifiableEconomicMap<Node, Node> __duplicates)
    {
        if (__replacee.graph().isAfterFloatingReadPhase())
        {
            // rewire outgoing memory edges
            replaceMemoryUsages(__replacee, new MemoryOutputMap(__replacee, __duplicates));

            if (returnNode != null)
            {
                ReturnNode __ret = (ReturnNode) __duplicates.get(returnNode);
                if (__ret != null)
                {
                    MemoryMapNode __memoryMap = __ret.getMemoryMap();
                    if (__memoryMap != null)
                    {
                        __ret.setMemoryMap(null);
                        __memoryMap.safeDelete();
                    }
                }
            }
            if (memoryAnchor != null)
            {
                // rewire incoming memory edges
                MemoryAnchorNode __memoryDuplicate = (MemoryAnchorNode) __duplicates.get(memoryAnchor);
                replaceMemoryUsages(__memoryDuplicate, new MemoryInputMap(__replacee));

                if (__memoryDuplicate.hasNoUsages())
                {
                    if (__memoryDuplicate.next() != null)
                    {
                        __memoryDuplicate.graph().removeFixed(__memoryDuplicate);
                    }
                    else
                    {
                        // this was a dummy memory node used when instantiating pure data-flow
                        // snippets: it was not attached to the control flow.
                        __memoryDuplicate.safeDelete();
                    }
                }
            }
        }
    }

    private static LocationIdentity getLocationIdentity(Node __node)
    {
        if (__node instanceof MemoryAccess)
        {
            return ((MemoryAccess) __node).getLocationIdentity();
        }
        else if (__node instanceof MemoryProxy)
        {
            return ((MemoryProxy) __node).getLocationIdentity();
        }
        else if (__node instanceof MemoryPhiNode)
        {
            return ((MemoryPhiNode) __node).getLocationIdentity();
        }
        else
        {
            return null;
        }
    }

    private void replaceMemoryUsages(ValueNode __node, MemoryMap __map)
    {
        for (Node __usage : __node.usages().snapshot())
        {
            if (__usage instanceof MemoryMapNode)
            {
                continue;
            }

            LocationIdentity __location = getLocationIdentity(__usage);
            if (__location != null)
            {
                for (Position __pos : __usage.inputPositions())
                {
                    if (__pos.getInputType() == InputType.Memory && __pos.get(__usage) == __node)
                    {
                        MemoryNode __replacement = __map.getLastLocationAccess(__location);
                        if (__replacement != null)
                        {
                            __pos.set(__usage, __replacement.asNode());
                        }
                    }
                }
            }
        }
    }

    /**
     * Replaces a given fixed node with this specialized snippet.
     *
     * @param replacee the node that will be replaced
     * @param replacer object that replaces the usages of {@code replacee}
     * @param args the arguments to be bound to the flattened positional parameters of the snippet
     * @return the map of duplicated nodes (original -&gt; duplicate)
     */
    public UnmodifiableEconomicMap<Node, Node> instantiate(MetaAccessProvider __metaAccess, FixedNode __replacee, UsageReplacer __replacer, Arguments __args)
    {
        return instantiate(__metaAccess, __replacee, __replacer, __args, true);
    }

    /**
     * Replaces a given fixed node with this specialized snippet.
     *
     * @param replacee the node that will be replaced
     * @param replacer object that replaces the usages of {@code replacee}
     * @param args the arguments to be bound to the flattened positional parameters of the snippet
     * @param killReplacee is true, the replacee node is deleted
     * @return the map of duplicated nodes (original -&gt; duplicate)
     */
    public UnmodifiableEconomicMap<Node, Node> instantiate(MetaAccessProvider __metaAccess, FixedNode __replacee, UsageReplacer __replacer, Arguments __args, boolean __killReplacee)
    {
        // inline the snippet nodes replacing parameters with the given args in the process
        StartNode __entryPointNode = snippet.start();
        FixedNode __firstCFGNode = __entryPointNode.next();
        StructuredGraph __replaceeGraph = __replacee.graph();
        EconomicMap<Node, Node> __replacements = bind(__replaceeGraph, __metaAccess, __args);
        __replacements.put(__entryPointNode, AbstractBeginNode.prevBegin(__replacee));
        UnmodifiableEconomicMap<Node, Node> __duplicates = inlineSnippet(__replacee, __replaceeGraph, __replacements);

        // re-wire the control flow graph around the replacee
        FixedNode __firstCFGNodeDuplicate = (FixedNode) __duplicates.get(__firstCFGNode);
        __replacee.replaceAtPredecessor(__firstCFGNodeDuplicate);

        rewireFrameStates(__replacee, __duplicates);

        if (__replacee instanceof DeoptimizingNode)
        {
            DeoptimizingNode __replaceeDeopt = (DeoptimizingNode) __replacee;

            FrameState __stateBefore = null;
            FrameState __stateDuring = null;
            FrameState __stateAfter = null;
            if (__replaceeDeopt.canDeoptimize())
            {
                if (__replaceeDeopt instanceof DeoptimizingNode.DeoptBefore)
                {
                    __stateBefore = ((DeoptimizingNode.DeoptBefore) __replaceeDeopt).stateBefore();
                }
                if (__replaceeDeopt instanceof DeoptimizingNode.DeoptDuring)
                {
                    __stateDuring = ((DeoptimizingNode.DeoptDuring) __replaceeDeopt).stateDuring();
                }
                if (__replaceeDeopt instanceof DeoptimizingNode.DeoptAfter)
                {
                    __stateAfter = ((DeoptimizingNode.DeoptAfter) __replaceeDeopt).stateAfter();
                }
            }

            for (DeoptimizingNode __deoptNode : deoptNodes)
            {
                DeoptimizingNode __deoptDup = (DeoptimizingNode) __duplicates.get(__deoptNode.asNode());
                if (__deoptDup.canDeoptimize())
                {
                    if (__deoptDup instanceof DeoptimizingNode.DeoptBefore)
                    {
                        ((DeoptimizingNode.DeoptBefore) __deoptDup).setStateBefore(__stateBefore);
                    }
                    if (__deoptDup instanceof DeoptimizingNode.DeoptDuring)
                    {
                        DeoptimizingNode.DeoptDuring __deoptDupDuring = (DeoptimizingNode.DeoptDuring) __deoptDup;
                        if (__stateDuring != null)
                        {
                            __deoptDupDuring.setStateDuring(__stateDuring);
                        }
                        else if (__stateAfter != null)
                        {
                            __deoptDupDuring.computeStateDuring(__stateAfter);
                        }
                        else if (__stateBefore != null)
                        {
                            __deoptDupDuring.setStateDuring(__stateBefore);
                        }
                    }
                    if (__deoptDup instanceof DeoptimizingNode.DeoptAfter)
                    {
                        DeoptimizingNode.DeoptAfter __deoptDupAfter = (DeoptimizingNode.DeoptAfter) __deoptDup;
                        if (__stateAfter != null)
                        {
                            __deoptDupAfter.setStateAfter(__stateAfter);
                        }
                        else
                        {
                            __deoptDupAfter.setStateAfter(__stateBefore);
                        }
                    }
                }
            }
        }

        updateStamps(__replacee, __duplicates);

        rewireMemoryGraph(__replacee, __duplicates);

        // replace all usages of the replacee with the value returned by the snippet
        ValueNode __returnValue = null;
        if (returnNode != null && !(__replacee instanceof ControlSinkNode))
        {
            ReturnNode __returnDuplicate = (ReturnNode) __duplicates.get(returnNode);
            __returnValue = __returnDuplicate.result();
            if (__returnValue == null && __replacee.usages().isNotEmpty() && __replacee instanceof MemoryCheckpoint)
            {
                __replacer.replace(__replacee, null);
            }
            else
            {
                __replacer.replace(__replacee, __returnValue);
            }
            if (__returnDuplicate.isAlive())
            {
                FixedNode __next = null;
                if (__replacee instanceof FixedWithNextNode)
                {
                    FixedWithNextNode __fwn = (FixedWithNextNode) __replacee;
                    __next = __fwn.next();
                    __fwn.setNext(null);
                }
                __returnDuplicate.replaceAndDelete(__next);
            }
        }

        if (__killReplacee)
        {
            // remove the replacee from its graph
            GraphUtil.killCFG(__replacee);
        }

        return __duplicates;
    }

    private UnmodifiableEconomicMap<Node, Node> inlineSnippet(Node __replacee, StructuredGraph __replaceeGraph, EconomicMap<Node, Node> __replacements)
    {
        return __replaceeGraph.addDuplicates(nodes, snippet, snippet.getNodeCount(), __replacements);
    }

    private void propagateStamp(Node __node)
    {
        if (__node instanceof PhiNode)
        {
            PhiNode __phi = (PhiNode) __node;
            if (__phi.inferStamp())
            {
                for (Node __usage : __node.usages())
                {
                    propagateStamp(__usage);
                }
            }
        }
    }

    private void updateStamps(ValueNode __replacee, UnmodifiableEconomicMap<Node, Node> __duplicates)
    {
        for (ValueNode __node : placeholderStampedNodes)
        {
            ValueNode __dup = (ValueNode) __duplicates.get(__node);
            Stamp __replaceeStamp = __replacee.stamp(NodeView.DEFAULT);
            if (__node instanceof Placeholder)
            {
                Placeholder __placeholderDup = (Placeholder) __dup;
                __placeholderDup.makeReplacement(__replaceeStamp);
            }
            else
            {
                __dup.setStamp(__replaceeStamp);
            }
        }
        for (ParameterNode __paramNode : snippet.getNodes(ParameterNode.TYPE))
        {
            for (Node __usage : __paramNode.usages())
            {
                Node __usageDup = __duplicates.get(__usage);
                propagateStamp(__usageDup);
            }
        }
    }

    /**
     * Gets a copy of the specialized graph.
     */
    public StructuredGraph copySpecializedGraph()
    {
        return (StructuredGraph) snippet.copy();
    }

    /**
     * Replaces a given floating node with this specialized snippet.
     *
     * @param replacee the node that will be replaced
     * @param replacer object that replaces the usages of {@code replacee}
     * @param tool lowering tool used to insert the snippet into the control-flow
     * @param args the arguments to be bound to the flattened positional parameters of the snippet
     */
    public void instantiate(MetaAccessProvider __metaAccess, FloatingNode __replacee, UsageReplacer __replacer, LoweringTool __tool, Arguments __args)
    {
        // inline the snippet nodes replacing parameters with the given args in the process
        StartNode __entryPointNode = snippet.start();
        FixedNode __firstCFGNode = __entryPointNode.next();
        StructuredGraph __replaceeGraph = __replacee.graph();
        EconomicMap<Node, Node> __replacements = bind(__replaceeGraph, __metaAccess, __args);
        __replacements.put(__entryPointNode, __tool.getCurrentGuardAnchor().asNode());
        UnmodifiableEconomicMap<Node, Node> __duplicates = inlineSnippet(__replacee, __replaceeGraph, __replacements);

        FixedWithNextNode __lastFixedNode = __tool.lastFixedNode();
        FixedNode __next = __lastFixedNode.next();
        __lastFixedNode.setNext(null);
        FixedNode __firstCFGNodeDuplicate = (FixedNode) __duplicates.get(__firstCFGNode);
        __replaceeGraph.addAfterFixed(__lastFixedNode, __firstCFGNodeDuplicate);

        rewireFrameStates(__replacee, __duplicates);
        updateStamps(__replacee, __duplicates);

        rewireMemoryGraph(__replacee, __duplicates);

        // replace all usages of the replacee with the value returned by the snippet
        ReturnNode __returnDuplicate = (ReturnNode) __duplicates.get(returnNode);
        ValueNode __returnValue = __returnDuplicate.result();
        __replacer.replace(__replacee, __returnValue);

        if (__returnDuplicate.isAlive())
        {
            __returnDuplicate.replaceAndDelete(__next);
        }
    }

    /**
     * Replaces a given floating node with this specialized snippet.
     *
     * This snippet must be pure data-flow
     *
     * @param replacee the node that will be replaced
     * @param replacer object that replaces the usages of {@code replacee}
     * @param args the arguments to be bound to the flattened positional parameters of the snippet
     */
    public void instantiate(MetaAccessProvider __metaAccess, FloatingNode __replacee, UsageReplacer __replacer, Arguments __args)
    {
        // inline the snippet nodes replacing parameters with the given args in the process
        StartNode __entryPointNode = snippet.start();
        StructuredGraph __replaceeGraph = __replacee.graph();
        EconomicMap<Node, Node> __replacements = bind(__replaceeGraph, __metaAccess, __args);
        MemoryAnchorNode __anchorDuplicate = null;
        if (memoryAnchor != null)
        {
            __anchorDuplicate = __replaceeGraph.add(new MemoryAnchorNode());
            __replacements.put(memoryAnchor, __anchorDuplicate);
        }
        List<Node> __floatingNodes = new ArrayList<>(nodes.size() - 2);
        for (Node __n : nodes)
        {
            if (__n != __entryPointNode && __n != returnNode)
            {
                __floatingNodes.add(__n);
            }
        }
        UnmodifiableEconomicMap<Node, Node> __duplicates = inlineSnippet(__replacee, __replaceeGraph, __replacements);

        rewireFrameStates(__replacee, __duplicates);
        updateStamps(__replacee, __duplicates);

        rewireMemoryGraph(__replacee, __duplicates);

        // replace all usages of the replacee with the value returned by the snippet
        ValueNode __returnValue = (ValueNode) __duplicates.get(returnNode.result());
        __replacer.replace(__replacee, __returnValue);
    }

    protected void rewireFrameStates(ValueNode __replacee, UnmodifiableEconomicMap<Node, Node> __duplicates)
    {
        if (__replacee instanceof StateSplit)
        {
            for (StateSplit __sideEffectNode : sideEffectNodes)
            {
                Node __sideEffectDup = __duplicates.get(__sideEffectNode.asNode());
                ((StateSplit) __sideEffectDup).setStateAfter(((StateSplit) __replacee).stateAfter());
            }
        }
    }

    private static boolean checkTemplate(MetaAccessProvider __metaAccess, Arguments __args, ResolvedJavaMethod __method, Signature __signature)
    {
        for (int __i = 0; __i < __args.info.getParameterCount(); __i++)
        {
            if (__args.info.isConstantParameter(__i))
            {
                JavaKind __kind = __signature.getParameterKind(__i);
            }
            else if (__args.info.isVarargsParameter(__i))
            {
                Varargs __varargs = (Varargs) __args.values[__i];
            }
        }
        return true;
    }

    public void setMayRemoveLocation(boolean __mayRemoveLocation)
    {
        this.mayRemoveLocation = __mayRemoveLocation;
    }
}
