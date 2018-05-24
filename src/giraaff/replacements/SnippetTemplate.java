package giraaff.replacements;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
import jdk.vm.ci.meta.LocalVariableTable;
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
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;
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
public class SnippetTemplate
{
    private boolean mayRemoveLocation = false;

    /**
     * Holds the {@link ResolvedJavaMethod} of the snippet, together with some information about the
     * method that needs to be computed only once. The {@link SnippetInfo} should be created once
     * per snippet and then cached.
     */
    public abstract static class SnippetInfo
    {
        protected final ResolvedJavaMethod method;
        protected ResolvedJavaMethod original;
        protected final LocationIdentity[] privateLocations;

        /**
         * Lazily constructed parts of {@link SnippetInfo}.
         */
        static class Lazy
        {
            Lazy(ResolvedJavaMethod method)
            {
                int count = method.getSignature().getParameterCount(false);
                constantParameters = new boolean[count];
                varargsParameters = new boolean[count];
                nonNullParameters = new boolean[count];
                for (int i = 0; i < count; i++)
                {
                    constantParameters[i] = method.getParameterAnnotation(ConstantParameter.class, i) != null;
                    varargsParameters[i] = method.getParameterAnnotation(VarargsParameter.class, i) != null;
                    nonNullParameters[i] = method.getParameterAnnotation(NonNullParameter.class, i) != null;
                }
            }

            final boolean[] constantParameters;
            final boolean[] varargsParameters;
            final boolean[] nonNullParameters;

            /**
             * The parameter names, taken from the local variables table. Only used for assertion
             * checking, so use only within an assert statement.
             */
            String[] names;

            private boolean initNames(ResolvedJavaMethod method, int parameterCount)
            {
                names = new String[parameterCount];
                Parameter[] params = method.getParameters();
                if (params != null)
                {
                    for (int i = 0; i < names.length; i++)
                    {
                        if (params[i].isNamePresent())
                        {
                            names[i] = params[i].getName();
                        }
                    }
                }
                else
                {
                    int slotIdx = 0;
                    LocalVariableTable localVariableTable = method.getLocalVariableTable();
                    if (localVariableTable != null)
                    {
                        for (int i = 0; i < names.length; i++)
                        {
                            Local local = localVariableTable.getLocal(slotIdx, 0);
                            if (local != null)
                            {
                                names[i] = local.getName();
                            }
                            JavaKind kind = method.getSignature().getParameterKind(i);
                            slotIdx += kind.getSlotCount();
                        }
                    }
                }
                return true;
            }
        }

        protected abstract Lazy lazy();

        protected SnippetInfo(ResolvedJavaMethod method, LocationIdentity[] privateLocations)
        {
            this.method = method;
            this.privateLocations = privateLocations;
        }

        public ResolvedJavaMethod getMethod()
        {
            return method;
        }

        public int getParameterCount()
        {
            return lazy().constantParameters.length;
        }

        public void setOriginalMethod(ResolvedJavaMethod original)
        {
            this.original = original;
        }

        public boolean isConstantParameter(int paramIdx)
        {
            return lazy().constantParameters[paramIdx];
        }

        public boolean isVarargsParameter(int paramIdx)
        {
            return lazy().varargsParameters[paramIdx];
        }

        public boolean isNonNullParameter(int paramIdx)
        {
            return lazy().nonNullParameters[paramIdx];
        }

        public String getParameterName(int paramIdx)
        {
            String[] names = lazy().names;
            if (names != null)
            {
                return names[paramIdx];
            }
            return null;
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + ":" + method.format("%h.%n");
        }
    }

    protected static class LazySnippetInfo extends SnippetInfo
    {
        protected final AtomicReference<Lazy> lazy = new AtomicReference<>(null);

        protected LazySnippetInfo(ResolvedJavaMethod method, LocationIdentity[] privateLocations)
        {
            super(method, privateLocations);
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

    protected static class EagerSnippetInfo extends SnippetInfo
    {
        protected final Lazy lazy;

        protected EagerSnippetInfo(ResolvedJavaMethod method, LocationIdentity[] privateLocations)
        {
            super(method, privateLocations);
            lazy = new Lazy(method);
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
    public static class Arguments
    {
        protected final SnippetInfo info;
        protected final CacheKey cacheKey;
        protected final Object[] values;
        protected final Stamp[] constStamps;
        protected boolean cacheable;

        protected int nextParamIdx;

        public Arguments(SnippetInfo info, GuardsStage guardsStage, LoweringTool.LoweringStage loweringStage)
        {
            this.info = info;
            this.cacheKey = new CacheKey(info, guardsStage, loweringStage);
            this.values = new Object[info.getParameterCount()];
            this.constStamps = new Stamp[info.getParameterCount()];
            this.cacheable = true;
        }

        public Arguments add(String name, Object value)
        {
            values[nextParamIdx] = value;
            nextParamIdx++;
            return this;
        }

        public Arguments addConst(String name, Object value)
        {
            return addConst(name, value, null);
        }

        public Arguments addConst(String name, Object value, Stamp stamp)
        {
            values[nextParamIdx] = value;
            constStamps[nextParamIdx] = stamp;
            cacheKey.setParam(nextParamIdx, value);
            nextParamIdx++;
            return this;
        }

        public Arguments addVarargs(String name, Class<?> componentType, Stamp argStamp, Object value)
        {
            Varargs varargs = new Varargs(componentType, argStamp, value);
            values[nextParamIdx] = varargs;
            // A separate template is necessary for every distinct array length
            cacheKey.setParam(nextParamIdx, varargs.length);
            nextParamIdx++;
            return this;
        }

        public void setCacheable(boolean cacheable)
        {
            this.cacheable = cacheable;
        }

        @Override
        public String toString()
        {
            StringBuilder result = new StringBuilder();
            result.append("Parameters<").append(info.method.format("%h.%n")).append(" [");
            String sep = "";
            for (int i = 0; i < info.getParameterCount(); i++)
            {
                result.append(sep);
                if (info.isConstantParameter(i))
                {
                    result.append("const ");
                }
                else if (info.isVarargsParameter(i))
                {
                    result.append("varargs ");
                }
                result.append(info.getParameterName(i)).append(" = ").append(values[i]);
                sep = ", ";
            }
            result.append(">");
            return result.toString();
        }
    }

    /**
     * Wrapper for the prototype value of a {@linkplain VarargsParameter varargs} parameter.
     */
    static class Varargs
    {
        protected final Class<?> componentType;
        protected final Stamp stamp;
        protected final Object value;
        protected final int length;

        protected Varargs(Class<?> componentType, Stamp stamp, Object value)
        {
            this.componentType = componentType;
            this.stamp = stamp;
            this.value = value;
            if (value instanceof List)
            {
                this.length = ((List<?>) value).size();
            }
            else
            {
                this.length = Array.getLength(value);
            }
        }

        @Override
        public String toString()
        {
            if (value instanceof boolean[])
            {
                return Arrays.toString((boolean[]) value);
            }
            if (value instanceof byte[])
            {
                return Arrays.toString((byte[]) value);
            }
            if (value instanceof char[])
            {
                return Arrays.toString((char[]) value);
            }
            if (value instanceof short[])
            {
                return Arrays.toString((short[]) value);
            }
            if (value instanceof int[])
            {
                return Arrays.toString((int[]) value);
            }
            if (value instanceof long[])
            {
                return Arrays.toString((long[]) value);
            }
            if (value instanceof float[])
            {
                return Arrays.toString((float[]) value);
            }
            if (value instanceof double[])
            {
                return Arrays.toString((double[]) value);
            }
            if (value instanceof Object[])
            {
                return Arrays.toString((Object[]) value);
            }
            return String.valueOf(value);
        }
    }

    static final class VarargsPlaceholderNode extends FloatingNode implements ArrayLengthProvider
    {
        public static final NodeClass<VarargsPlaceholderNode> TYPE = NodeClass.create(VarargsPlaceholderNode.class);
        protected final Varargs varargs;

        protected VarargsPlaceholderNode(Varargs varargs, MetaAccessProvider metaAccess)
        {
            super(TYPE, StampFactory.objectNonNull(TypeReference.createExactTrusted(metaAccess.lookupJavaType(varargs.componentType).getArrayClass())));
            this.varargs = varargs;
        }

        @Override
        public ValueNode length()
        {
            return ConstantNode.forInt(varargs.length);
        }
    }

    static class CacheKey
    {
        private final ResolvedJavaMethod method;
        private final Object[] values;
        private final GuardsStage guardsStage;
        private final LoweringTool.LoweringStage loweringStage;
        private int hash;

        protected CacheKey(SnippetInfo info, GuardsStage guardsStage, LoweringTool.LoweringStage loweringStage)
        {
            this.method = info.method;
            this.guardsStage = guardsStage;
            this.loweringStage = loweringStage;
            this.values = new Object[info.getParameterCount()];
            this.hash = info.method.hashCode() + 31 * guardsStage.ordinal();
        }

        protected void setParam(int paramIdx, Object value)
        {
            values[paramIdx] = value;
            hash = (hash * 31) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof CacheKey))
            {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            if (!method.equals(other.method))
            {
                return false;
            }
            if (guardsStage != other.guardsStage || loweringStage != other.loweringStage)
            {
                return false;
            }
            for (int i = 0; i < values.length; i++)
            {
                if (values[i] != null && !values[i].equals(other.values[i]))
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

    static class Options
    {
        // Option "Use a LRU cache for snippet templates."
        public static final OptionKey<Boolean> UseSnippetTemplateCache = new OptionKey<>(true);

        static final OptionKey<Integer> MaxTemplatesPerSnippet = new OptionKey<>(50);
    }

    /**
     * Base class for snippet classes. It provides a cache for {@link SnippetTemplate}s.
     */
    public abstract static class AbstractTemplates implements giraaff.api.replacements.SnippetTemplateCache
    {
        protected final OptionValues options;
        protected final Providers providers;
        protected final SnippetReflectionProvider snippetReflection;
        protected final TargetDescription target;
        private final Map<CacheKey, SnippetTemplate> templates;

        protected AbstractTemplates(OptionValues options, Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target)
        {
            this.options = options;
            this.providers = providers;
            this.snippetReflection = snippetReflection;
            this.target = target;
            if (Options.UseSnippetTemplateCache.getValue(options))
            {
                int size = Options.MaxTemplatesPerSnippet.getValue(options);
                this.templates = Collections.synchronizedMap(new LRUCache<>(size, size));
            }
            else
            {
                this.templates = null;
            }
        }

        public static Method findMethod(Class<? extends Snippets> declaringClass, String methodName, Method except)
        {
            for (Method m : declaringClass.getDeclaredMethods())
            {
                if (m.getName().equals(methodName) && !m.equals(except))
                {
                    return m;
                }
            }
            return null;
        }

        /**
         * Finds the unique method in {@code declaringClass} named {@code methodName} annotated by
         * {@link Snippet} and returns a {@link SnippetInfo} value describing it. There must be
         * exactly one snippet method in {@code declaringClass}.
         */
        protected SnippetInfo snippet(Class<? extends Snippets> declaringClass, String methodName, LocationIdentity... initialPrivateLocations)
        {
            Method method = findMethod(declaringClass, methodName, null);
            ResolvedJavaMethod javaMethod = providers.getMetaAccess().lookupJavaMethod(method);
            LocationIdentity[] privateLocations = GraalOptions.SnippetCounters.getValue(options) ? SnippetCounterNode.addSnippetCounters(initialPrivateLocations) : initialPrivateLocations;
            if (GraalOptions.EagerSnippets.getValue(options))
            {
                return new EagerSnippetInfo(javaMethod, privateLocations);
            }
            else
            {
                return new LazySnippetInfo(javaMethod, privateLocations);
            }
        }

        /**
         * Gets a template for a given key, creating it first if necessary.
         */
        protected SnippetTemplate template(ValueNode replacee, final Arguments args)
        {
            StructuredGraph graph = replacee.graph();
            SnippetTemplate template = Options.UseSnippetTemplateCache.getValue(options) && args.cacheable ? templates.get(args.cacheKey) : null;
            if (template == null)
            {
                template = new SnippetTemplate(options, providers, snippetReflection, args, replacee);
                if (Options.UseSnippetTemplateCache.getValue(options) && args.cacheable)
                {
                    templates.put(args.cacheKey, template);
                }
            }
            return template;
        }
    }

    private static final class LRUCache<K, V> extends LinkedHashMap<K, V>
    {
        private final int maxCacheSize;

        LRUCache(int initialCapacity, int maxCacheSize)
        {
            super(initialCapacity, 0.75F, true);
            this.maxCacheSize = maxCacheSize;
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest)
        {
            return size() > maxCacheSize;
        }
    }

    // These values must be compared with equals() not '==' to support replay compilation.
    private static final Object UNUSED_PARAMETER = "UNUSED_PARAMETER";
    private static final Object CONSTANT_PARAMETER = "CONSTANT_PARAMETER";

    /**
     * Determines if any parameter of a given method is annotated with {@link ConstantParameter}.
     */
    public static boolean hasConstantParameter(ResolvedJavaMethod method)
    {
        for (ConstantParameter p : method.getParameterAnnotations(ConstantParameter.class))
        {
            if (p != null)
            {
                return true;
            }
        }
        return false;
    }

    private final SnippetReflectionProvider snippetReflection;

    /**
     * Creates a snippet template.
     */
    protected SnippetTemplate(OptionValues options, final Providers providers, SnippetReflectionProvider snippetReflection, Arguments args, Node replacee)
    {
        this.snippetReflection = snippetReflection;
        this.info = args.info;

        Object[] constantArgs = getConstantArgs(args);
        StructuredGraph snippetGraph = providers.getReplacements().getSnippet(args.info.method, args.info.original, constantArgs);

        ResolvedJavaMethod method = snippetGraph.method();
        Signature signature = method.getSignature();

        PhaseContext phaseContext = new PhaseContext(providers);

        // Copy snippet graph, replacing constant parameters with given arguments
        final StructuredGraph snippetCopy = new StructuredGraph.Builder(options).name(snippetGraph.name).method(snippetGraph.method()).build();
        if (!snippetGraph.isUnsafeAccessTrackingEnabled())
        {
            snippetCopy.disableUnsafeAccessTracking();
        }

        EconomicMap<Node, Node> nodeReplacements = EconomicMap.create(Equivalence.IDENTITY);
        nodeReplacements.put(snippetGraph.start(), snippetCopy.start());

        MetaAccessProvider metaAccess = providers.getMetaAccess();

        int parameterCount = args.info.getParameterCount();
        VarargsPlaceholderNode[] placeholders = new VarargsPlaceholderNode[parameterCount];

        for (int i = 0; i < parameterCount; i++)
        {
            ParameterNode parameter = snippetGraph.getParameter(i);
            if (parameter != null)
            {
                if (args.info.isConstantParameter(i))
                {
                    Object arg = args.values[i];
                    JavaKind kind = signature.getParameterKind(i);
                    ConstantNode constantNode;
                    if (arg instanceof Constant)
                    {
                        Stamp stamp = args.constStamps[i];
                        if (stamp == null)
                        {
                            constantNode = ConstantNode.forConstant((JavaConstant) arg, metaAccess, snippetCopy);
                        }
                        else
                        {
                            constantNode = ConstantNode.forConstant(stamp, (Constant) arg, metaAccess, snippetCopy);
                        }
                    }
                    else
                    {
                        constantNode = ConstantNode.forConstant(snippetReflection.forBoxed(kind, arg), metaAccess, snippetCopy);
                    }
                    nodeReplacements.put(parameter, constantNode);
                }
                else if (args.info.isVarargsParameter(i))
                {
                    Varargs varargs = (Varargs) args.values[i];
                    VarargsPlaceholderNode placeholder = snippetCopy.unique(new VarargsPlaceholderNode(varargs, providers.getMetaAccess()));
                    nodeReplacements.put(parameter, placeholder);
                    placeholders[i] = placeholder;
                }
                else if (args.info.isNonNullParameter(i))
                {
                    parameter.setStamp(parameter.stamp(NodeView.DEFAULT).join(StampFactory.objectNonNull()));
                }
            }
        }
        snippetCopy.addDuplicates(snippetGraph.getNodes(), snippetGraph, snippetGraph.getNodeCount(), nodeReplacements);

        // Gather the template parameters
        parameters = new Object[parameterCount];
        for (int i = 0; i < parameterCount; i++)
        {
            if (args.info.isConstantParameter(i))
            {
                parameters[i] = CONSTANT_PARAMETER;
            }
            else if (args.info.isVarargsParameter(i))
            {
                Varargs varargs = (Varargs) args.values[i];
                int length = varargs.length;
                ParameterNode[] params = new ParameterNode[length];
                Stamp stamp = varargs.stamp;
                for (int j = 0; j < length; j++)
                {
                    // Use a decimal friendly numbering make it more obvious how values map
                    int idx = (i + 1) * 10000 + j;
                    ParameterNode local = snippetCopy.addOrUnique(new ParameterNode(idx, StampPair.createSingle(stamp)));
                    params[j] = local;
                }
                parameters[i] = params;

                VarargsPlaceholderNode placeholder = placeholders[i];
                if (placeholder != null)
                {
                    for (Node usage : placeholder.usages().snapshot())
                    {
                        if (usage instanceof LoadIndexedNode)
                        {
                            LoadIndexedNode loadIndexed = (LoadIndexedNode) usage;
                            LoadSnippetVarargParameterNode loadSnippetParameter = snippetCopy.add(new LoadSnippetVarargParameterNode(params, loadIndexed.index(), loadIndexed.stamp(NodeView.DEFAULT)));
                            snippetCopy.replaceFixedWithFixed(loadIndexed, loadSnippetParameter);
                        }
                        else if (usage instanceof StoreIndexedNode)
                        {
                            /*
                             * The template lowering doesn't really treat this as an array so
                             * you can't store back into the varargs. Allocate your own array if
                             * you really need this and EA should eliminate it.
                             */
                            throw new GraalError("Can't store into VarargsParameter array");
                        }
                    }
                }
            }
            else
            {
                ParameterNode local = snippetCopy.getParameter(i);
                if (local == null)
                {
                    // Parameter value was eliminated
                    parameters[i] = UNUSED_PARAMETER;
                }
                else
                {
                    parameters[i] = local;
                }
            }
        }

        explodeLoops(snippetCopy, phaseContext);

        GuardsStage guardsStage = args.cacheKey.guardsStage;
        // Perform lowering on the snippet
        if (!guardsStage.allowsFloatingGuards())
        {
            new GuardLoweringPhase().apply(snippetCopy, null);
        }
        snippetCopy.setGuardsStage(guardsStage);
        new LoweringPhase(new CanonicalizerPhase(), args.cacheKey.loweringStage).apply(snippetCopy, phaseContext);

        ArrayList<StateSplit> curSideEffectNodes = new ArrayList<>();
        ArrayList<DeoptimizingNode> curDeoptNodes = new ArrayList<>();
        ArrayList<ValueNode> curPlaceholderStampedNodes = new ArrayList<>();
        for (Node node : snippetCopy.getNodes())
        {
            if (node instanceof ValueNode)
            {
                ValueNode valueNode = (ValueNode) node;
                if (valueNode.stamp(NodeView.DEFAULT) == PlaceholderStamp.singleton())
                {
                    curPlaceholderStampedNodes.add(valueNode);
                }
            }

            if (node instanceof StateSplit)
            {
                StateSplit stateSplit = (StateSplit) node;
                FrameState frameState = stateSplit.stateAfter();
                if (stateSplit.hasSideEffect())
                {
                    curSideEffectNodes.add((StateSplit) node);
                }
                if (frameState != null)
                {
                    stateSplit.setStateAfter(null);
                }
            }
            if (node instanceof DeoptimizingNode)
            {
                DeoptimizingNode deoptNode = (DeoptimizingNode) node;
                if (deoptNode.canDeoptimize())
                {
                    curDeoptNodes.add(deoptNode);
                }
            }
        }

        new DeadCodeEliminationPhase(Optionality.Required).apply(snippetCopy);

        new FloatingReadPhase(true, true).apply(snippetCopy);
        new RemoveValueProxyPhase().apply(snippetCopy);

        MemoryAnchorNode anchor = snippetCopy.add(new MemoryAnchorNode());
        snippetCopy.start().replaceAtUsages(InputType.Memory, anchor);

        this.snippet = snippetCopy;

        StartNode entryPointNode = snippet.start();
        if (anchor.hasNoUsages())
        {
            anchor.safeDelete();
            this.memoryAnchor = null;
        }
        else
        {
            // Find out if all the return memory maps point to the anchor (i.e., there's no kill
            // anywhere)
            boolean needsMemoryMaps = false;
            for (ReturnNode retNode : snippet.getNodes(ReturnNode.TYPE))
            {
                MemoryMapNode memoryMap = retNode.getMemoryMap();
                if (memoryMap.getLocations().size() > 1 || memoryMap.getLastLocationAccess(LocationIdentity.any()) != anchor)
                {
                    needsMemoryMaps = true;
                    break;
                }
            }
            boolean needsAnchor;
            if (needsMemoryMaps)
            {
                needsAnchor = true;
            }
            else
            {
                // Check that all those memory maps where the only usages of the anchor
                needsAnchor = anchor.usages().filter(NodePredicates.isNotA(MemoryMapNode.class)).isNotEmpty();
                // Remove the useless memory map
                MemoryMapNode memoryMap = null;
                for (ReturnNode retNode : snippet.getNodes(ReturnNode.TYPE))
                {
                    if (memoryMap == null)
                    {
                        memoryMap = retNode.getMemoryMap();
                    }
                    retNode.setMemoryMap(null);
                }
                memoryMap.safeDelete();
            }
            if (needsAnchor)
            {
                snippetCopy.addAfterFixed(snippetCopy.start(), anchor);
                this.memoryAnchor = anchor;
            }
            else
            {
                anchor.safeDelete();
                this.memoryAnchor = null;
            }
        }

        List<ReturnNode> returnNodes = snippet.getNodes(ReturnNode.TYPE).snapshot();
        if (returnNodes.isEmpty())
        {
            this.returnNode = null;
        }
        else if (returnNodes.size() == 1)
        {
            this.returnNode = returnNodes.get(0);
        }
        else
        {
            AbstractMergeNode merge = snippet.add(new MergeNode());
            List<MemoryMapNode> memMaps = new ArrayList<>();
            for (ReturnNode retNode : returnNodes)
            {
                MemoryMapNode memoryMapNode = retNode.getMemoryMap();
                if (memoryMapNode != null)
                {
                    memMaps.add(memoryMapNode);
                }
            }

            ValueNode returnValue = InliningUtil.mergeReturns(merge, returnNodes);
            this.returnNode = snippet.add(new ReturnNode(returnValue));
            if (!memMaps.isEmpty())
            {
                MemoryMapImpl mmap = FloatingReadPhase.mergeMemoryMaps(merge, memMaps);
                MemoryMapNode memoryMap = snippet.unique(new MemoryMapNode(mmap.getMap()));
                this.returnNode.setMemoryMap(memoryMap);
                for (MemoryMapNode mm : memMaps)
                {
                    if (mm != memoryMap && mm.isAlive())
                    {
                        GraphUtil.killWithUnusedFloatingInputs(mm);
                    }
                }
            }
            merge.setNext(this.returnNode);
        }

        this.sideEffectNodes = curSideEffectNodes;
        this.deoptNodes = curDeoptNodes;
        this.placeholderStampedNodes = curPlaceholderStampedNodes;

        nodes = new ArrayList<>(snippet.getNodeCount());
        for (Node node : snippet.getNodes())
        {
            if (node != entryPointNode && node != entryPointNode.stateAfter())
            {
                nodes.add(node);
            }
        }

        this.snippet.freeze();
    }

    public static void explodeLoops(final StructuredGraph snippetCopy, PhaseContext phaseContext)
    {
        // Do any required loop explosion
        boolean exploded = false;
        do
        {
            exploded = false;
            ExplodeLoopNode explodeLoop = snippetCopy.getNodes().filter(ExplodeLoopNode.class).first();
            if (explodeLoop != null) { // Earlier canonicalization may have removed the loop
                // altogether
                LoopBeginNode loopBegin = explodeLoop.findLoopBegin();
                if (loopBegin != null)
                {
                    LoopEx loop = new LoopsData(snippetCopy).loop(loopBegin);
                    Mark mark = snippetCopy.getMark();
                    LoopTransformations.fullUnroll(loop, phaseContext, new CanonicalizerPhase());
                    new CanonicalizerPhase().applyIncremental(snippetCopy, phaseContext, mark);
                    loop.deleteUnusedNodes();
                }
                GraphUtil.removeFixedWithUnusedInputs(explodeLoop);
                exploded = true;
            }
        } while (exploded);
    }

    protected Object[] getConstantArgs(Arguments args)
    {
        Object[] constantArgs = args.values.clone();
        for (int i = 0; i < args.info.getParameterCount(); i++)
        {
            if (!args.info.isConstantParameter(i))
            {
                constantArgs[i] = null;
            }
        }
        return constantArgs;
    }

    /**
     * The graph built from the snippet method.
     */
    private final StructuredGraph snippet;

    private final SnippetInfo info;

    /**
     * The named parameters of this template that must be bound to values during instantiation. For
     * a parameter that is still live after specialization, the value in this map is either a
     * {@link ParameterNode} instance or a {@link ParameterNode} array. For an eliminated parameter,
     * the value is identical to the key.
     */
    private final Object[] parameters;

    /**
     * The return node (if any) of the snippet.
     */
    private final ReturnNode returnNode;

    /**
     * The memory anchor (if any) of the snippet.
     */
    private final MemoryAnchorNode memoryAnchor;

    /**
     * Nodes that inherit the {@link StateSplit#stateAfter()} from the replacee during instantiation.
     */
    private final ArrayList<StateSplit> sideEffectNodes;

    /**
     * Nodes that inherit a deoptimization {@link FrameState} from the replacee during instantiation.
     */
    private final ArrayList<DeoptimizingNode> deoptNodes;

    /**
     * Nodes that have a stamp originating from a {@link Placeholder}.
     */
    private final ArrayList<ValueNode> placeholderStampedNodes;

    /**
     * The nodes to be inlined when this specialization is instantiated.
     */
    private final ArrayList<Node> nodes;

    /**
     * Gets the instantiation-time bindings to this template's parameters.
     *
     * @return the map that will be used to bind arguments to parameters when inlining this template
     */
    private EconomicMap<Node, Node> bind(StructuredGraph replaceeGraph, MetaAccessProvider metaAccess, Arguments args)
    {
        EconomicMap<Node, Node> replacements = EconomicMap.create(Equivalence.IDENTITY);
        for (int i = 0; i < parameters.length; i++)
        {
            Object parameter = parameters[i];
            Object argument = args.values[i];
            if (parameter instanceof ParameterNode)
            {
                if (argument instanceof ValueNode)
                {
                    replacements.put((ParameterNode) parameter, (ValueNode) argument);
                }
                else
                {
                    JavaKind kind = ((ParameterNode) parameter).getStackKind();
                    JavaConstant constant = forBoxed(argument, kind);
                    replacements.put((ParameterNode) parameter, ConstantNode.forConstant(constant, metaAccess, replaceeGraph));
                }
            }
            else if (parameter instanceof ParameterNode[])
            {
                ParameterNode[] params = (ParameterNode[]) parameter;
                Varargs varargs = (Varargs) argument;
                int length = params.length;
                List<?> list = null;
                Object array = null;
                if (varargs.value instanceof List)
                {
                    list = (List<?>) varargs.value;
                }
                else
                {
                    array = varargs.value;
                }

                for (int j = 0; j < length; j++)
                {
                    ParameterNode param = params[j];
                    Object value = list != null ? list.get(j) : Array.get(array, j);
                    if (value instanceof ValueNode)
                    {
                        replacements.put(param, (ValueNode) value);
                    }
                    else
                    {
                        JavaConstant constant = forBoxed(value, param.getStackKind());
                        ConstantNode element = ConstantNode.forConstant(constant, metaAccess, replaceeGraph);
                        replacements.put(param, element);
                    }
                }
            }
        }
        return replacements;
    }

    /**
     * Converts a Java boxed value to a {@link JavaConstant} of the right kind. This adjusts for the
     * limitation that a {@link Local}'s kind is a {@linkplain JavaKind#getStackKind() stack kind}
     * and so cannot be used for re-boxing primitives smaller than an int.
     *
     * @param argument a Java boxed value
     * @param localKind the kind of the {@link Local} to which {@code argument} will be bound
     */
    protected JavaConstant forBoxed(Object argument, JavaKind localKind)
    {
        if (localKind == JavaKind.Int)
        {
            return JavaConstant.forBoxedPrimitive(argument);
        }
        return snippetReflection.forBoxed(localKind, argument);
    }

    /**
     * Logic for replacing a snippet-lowered node at its usages with the return value of the
     * snippet. An alternative to the {@linkplain SnippetTemplate#DEFAULT_REPLACER default}
     * replacement logic can be used to handle mismatches between the stamp of the node being
     * lowered and the stamp of the snippet's return value.
     */
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
    public static final UsageReplacer DEFAULT_REPLACER = new UsageReplacer()
    {
        @Override
        public void replace(ValueNode oldNode, ValueNode newNode)
        {
            if (newNode != null)
            {
                oldNode.replaceAtUsages(newNode);
            }
        }
    };

    private boolean assertSnippetKills(ValueNode replacee)
    {
        if (!replacee.graph().isAfterFloatingReadPhase())
        {
            // no floating reads yet, ignore locations created while lowering
            return true;
        }
        if (returnNode == null)
        {
            // The snippet terminates control flow
            return true;
        }
        MemoryMapNode memoryMap = returnNode.getMemoryMap();
        if (memoryMap == null || memoryMap.isEmpty())
        {
            // there are no kills in the snippet graph
            return true;
        }

        EconomicSet<LocationIdentity> kills = EconomicSet.create(Equivalence.DEFAULT);
        kills.addAll(memoryMap.getLocations());

        if (replacee instanceof MemoryCheckpoint.Single)
        {
            // check if some node in snippet graph also kills the same location
            LocationIdentity locationIdentity = ((MemoryCheckpoint.Single) replacee).getLocationIdentity();
            if (locationIdentity.isAny())
            {
                // if the replacee kills ANY_LOCATION, the snippet can kill arbitrary locations
                return true;
            }
            kills.remove(locationIdentity);
        }

        // remove ANY_LOCATION if it's just a kill by the start node
        if (memoryMap.getLastLocationAccess(LocationIdentity.any()) instanceof MemoryAnchorNode)
        {
            kills.remove(LocationIdentity.any());
        }

        // node can only lower to a ANY_LOCATION kill if the replacee also kills ANY_LOCATION

        /*
         * Kills to private locations are safe, since there can be no floating read to these
         * locations except reads that are introduced by the snippet itself or related snippets in
         * the same lowering round. These reads are anchored to a MemoryAnchor at the beginning of
         * their snippet, so they can not float above a kill in another instance of the same snippet.
         */
        for (LocationIdentity p : this.info.privateLocations)
        {
            kills.remove(p);
        }

        return true;
    }

    private static class MemoryInputMap implements MemoryMap
    {
        private final LocationIdentity locationIdentity;
        private final MemoryNode lastLocationAccess;

        MemoryInputMap(ValueNode replacee)
        {
            if (replacee instanceof MemoryAccess)
            {
                MemoryAccess access = (MemoryAccess) replacee;
                locationIdentity = access.getLocationIdentity();
                lastLocationAccess = access.getLastLocationAccess();
            }
            else
            {
                locationIdentity = null;
                lastLocationAccess = null;
            }
        }

        @Override
        public MemoryNode getLastLocationAccess(LocationIdentity location)
        {
            if (locationIdentity != null && locationIdentity.equals(location))
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

    private class MemoryOutputMap extends MemoryInputMap
    {
        private final UnmodifiableEconomicMap<Node, Node> duplicates;

        MemoryOutputMap(ValueNode replacee, UnmodifiableEconomicMap<Node, Node> duplicates)
        {
            super(replacee);
            this.duplicates = duplicates;
        }

        @Override
        public MemoryNode getLastLocationAccess(LocationIdentity locationIdentity)
        {
            MemoryMapNode memoryMap = returnNode.getMemoryMap();
            MemoryNode lastLocationAccess = memoryMap.getLastLocationAccess(locationIdentity);
            if (lastLocationAccess == memoryAnchor)
            {
                return super.getLastLocationAccess(locationIdentity);
            }
            else
            {
                return (MemoryNode) duplicates.get(ValueNodeUtil.asNode(lastLocationAccess));
            }
        }

        @Override
        public Collection<LocationIdentity> getLocations()
        {
            return returnNode.getMemoryMap().getLocations();
        }
    }

    private void rewireMemoryGraph(ValueNode replacee, UnmodifiableEconomicMap<Node, Node> duplicates)
    {
        if (replacee.graph().isAfterFloatingReadPhase())
        {
            // rewire outgoing memory edges
            replaceMemoryUsages(replacee, new MemoryOutputMap(replacee, duplicates));

            if (returnNode != null)
            {
                ReturnNode ret = (ReturnNode) duplicates.get(returnNode);
                if (ret != null)
                {
                    MemoryMapNode memoryMap = ret.getMemoryMap();
                    if (memoryMap != null)
                    {
                        ret.setMemoryMap(null);
                        memoryMap.safeDelete();
                    }
                }
            }
            if (memoryAnchor != null)
            {
                // rewire incoming memory edges
                MemoryAnchorNode memoryDuplicate = (MemoryAnchorNode) duplicates.get(memoryAnchor);
                replaceMemoryUsages(memoryDuplicate, new MemoryInputMap(replacee));

                if (memoryDuplicate.hasNoUsages())
                {
                    if (memoryDuplicate.next() != null)
                    {
                        memoryDuplicate.graph().removeFixed(memoryDuplicate);
                    }
                    else
                    {
                        // this was a dummy memory node used when instantiating pure data-flow
                        // snippets: it was not attached to the control flow.
                        memoryDuplicate.safeDelete();
                    }
                }
            }
        }
    }

    private static LocationIdentity getLocationIdentity(Node node)
    {
        if (node instanceof MemoryAccess)
        {
            return ((MemoryAccess) node).getLocationIdentity();
        }
        else if (node instanceof MemoryProxy)
        {
            return ((MemoryProxy) node).getLocationIdentity();
        }
        else if (node instanceof MemoryPhiNode)
        {
            return ((MemoryPhiNode) node).getLocationIdentity();
        }
        else
        {
            return null;
        }
    }

    private void replaceMemoryUsages(ValueNode node, MemoryMap map)
    {
        for (Node usage : node.usages().snapshot())
        {
            if (usage instanceof MemoryMapNode)
            {
                continue;
            }

            LocationIdentity location = getLocationIdentity(usage);
            if (location != null)
            {
                for (Position pos : usage.inputPositions())
                {
                    if (pos.getInputType() == InputType.Memory && pos.get(usage) == node)
                    {
                        MemoryNode replacement = map.getLastLocationAccess(location);
                        if (replacement != null)
                        {
                            pos.set(usage, replacement.asNode());
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
    public UnmodifiableEconomicMap<Node, Node> instantiate(MetaAccessProvider metaAccess, FixedNode replacee, UsageReplacer replacer, Arguments args)
    {
        return instantiate(metaAccess, replacee, replacer, args, true);
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
    public UnmodifiableEconomicMap<Node, Node> instantiate(MetaAccessProvider metaAccess, FixedNode replacee, UsageReplacer replacer, Arguments args, boolean killReplacee)
    {
        // Inline the snippet nodes, replacing parameters with the given args in the process
        StartNode entryPointNode = snippet.start();
        FixedNode firstCFGNode = entryPointNode.next();
        StructuredGraph replaceeGraph = replacee.graph();
        EconomicMap<Node, Node> replacements = bind(replaceeGraph, metaAccess, args);
        replacements.put(entryPointNode, AbstractBeginNode.prevBegin(replacee));
        UnmodifiableEconomicMap<Node, Node> duplicates = inlineSnippet(replacee, replaceeGraph, replacements);

        // Re-wire the control flow graph around the replacee
        FixedNode firstCFGNodeDuplicate = (FixedNode) duplicates.get(firstCFGNode);
        replacee.replaceAtPredecessor(firstCFGNodeDuplicate);

        rewireFrameStates(replacee, duplicates);

        if (replacee instanceof DeoptimizingNode)
        {
            DeoptimizingNode replaceeDeopt = (DeoptimizingNode) replacee;

            FrameState stateBefore = null;
            FrameState stateDuring = null;
            FrameState stateAfter = null;
            if (replaceeDeopt.canDeoptimize())
            {
                if (replaceeDeopt instanceof DeoptimizingNode.DeoptBefore)
                {
                    stateBefore = ((DeoptimizingNode.DeoptBefore) replaceeDeopt).stateBefore();
                }
                if (replaceeDeopt instanceof DeoptimizingNode.DeoptDuring)
                {
                    stateDuring = ((DeoptimizingNode.DeoptDuring) replaceeDeopt).stateDuring();
                }
                if (replaceeDeopt instanceof DeoptimizingNode.DeoptAfter)
                {
                    stateAfter = ((DeoptimizingNode.DeoptAfter) replaceeDeopt).stateAfter();
                }
            }

            for (DeoptimizingNode deoptNode : deoptNodes)
            {
                DeoptimizingNode deoptDup = (DeoptimizingNode) duplicates.get(deoptNode.asNode());
                if (deoptDup.canDeoptimize())
                {
                    if (deoptDup instanceof DeoptimizingNode.DeoptBefore)
                    {
                        ((DeoptimizingNode.DeoptBefore) deoptDup).setStateBefore(stateBefore);
                    }
                    if (deoptDup instanceof DeoptimizingNode.DeoptDuring)
                    {
                        DeoptimizingNode.DeoptDuring deoptDupDuring = (DeoptimizingNode.DeoptDuring) deoptDup;
                        if (stateDuring != null)
                        {
                            deoptDupDuring.setStateDuring(stateDuring);
                        }
                        else if (stateAfter != null)
                        {
                            deoptDupDuring.computeStateDuring(stateAfter);
                        }
                        else if (stateBefore != null)
                        {
                            deoptDupDuring.setStateDuring(stateBefore);
                        }
                    }
                    if (deoptDup instanceof DeoptimizingNode.DeoptAfter)
                    {
                        DeoptimizingNode.DeoptAfter deoptDupAfter = (DeoptimizingNode.DeoptAfter) deoptDup;
                        if (stateAfter != null)
                        {
                            deoptDupAfter.setStateAfter(stateAfter);
                        }
                        else
                        {
                            deoptDupAfter.setStateAfter(stateBefore);
                        }
                    }
                }
            }
        }

        updateStamps(replacee, duplicates);

        rewireMemoryGraph(replacee, duplicates);

        // Replace all usages of the replacee with the value returned by the snippet
        ValueNode returnValue = null;
        if (returnNode != null && !(replacee instanceof ControlSinkNode))
        {
            ReturnNode returnDuplicate = (ReturnNode) duplicates.get(returnNode);
            returnValue = returnDuplicate.result();
            if (returnValue == null && replacee.usages().isNotEmpty() && replacee instanceof MemoryCheckpoint)
            {
                replacer.replace(replacee, null);
            }
            else
            {
                replacer.replace(replacee, returnValue);
            }
            if (returnDuplicate.isAlive())
            {
                FixedNode next = null;
                if (replacee instanceof FixedWithNextNode)
                {
                    FixedWithNextNode fwn = (FixedWithNextNode) replacee;
                    next = fwn.next();
                    fwn.setNext(null);
                }
                returnDuplicate.replaceAndDelete(next);
            }
        }

        if (killReplacee)
        {
            // Remove the replacee from its graph
            GraphUtil.killCFG(replacee);
        }

        return duplicates;
    }

    private UnmodifiableEconomicMap<Node, Node> inlineSnippet(Node replacee, StructuredGraph replaceeGraph, EconomicMap<Node, Node> replacements)
    {
        return replaceeGraph.addDuplicates(nodes, snippet, snippet.getNodeCount(), replacements);
    }

    private void propagateStamp(Node node)
    {
        if (node instanceof PhiNode)
        {
            PhiNode phi = (PhiNode) node;
            if (phi.inferStamp())
            {
                for (Node usage : node.usages())
                {
                    propagateStamp(usage);
                }
            }
        }
    }

    private void updateStamps(ValueNode replacee, UnmodifiableEconomicMap<Node, Node> duplicates)
    {
        for (ValueNode node : placeholderStampedNodes)
        {
            ValueNode dup = (ValueNode) duplicates.get(node);
            Stamp replaceeStamp = replacee.stamp(NodeView.DEFAULT);
            if (node instanceof Placeholder)
            {
                Placeholder placeholderDup = (Placeholder) dup;
                placeholderDup.makeReplacement(replaceeStamp);
            }
            else
            {
                dup.setStamp(replaceeStamp);
            }
        }
        for (ParameterNode paramNode : snippet.getNodes(ParameterNode.TYPE))
        {
            for (Node usage : paramNode.usages())
            {
                Node usageDup = duplicates.get(usage);
                propagateStamp(usageDup);
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
    public void instantiate(MetaAccessProvider metaAccess, FloatingNode replacee, UsageReplacer replacer, LoweringTool tool, Arguments args)
    {
        // Inline the snippet nodes, replacing parameters with the given args in the process
        StartNode entryPointNode = snippet.start();
        FixedNode firstCFGNode = entryPointNode.next();
        StructuredGraph replaceeGraph = replacee.graph();
        EconomicMap<Node, Node> replacements = bind(replaceeGraph, metaAccess, args);
        replacements.put(entryPointNode, tool.getCurrentGuardAnchor().asNode());
        UnmodifiableEconomicMap<Node, Node> duplicates = inlineSnippet(replacee, replaceeGraph, replacements);

        FixedWithNextNode lastFixedNode = tool.lastFixedNode();
        FixedNode next = lastFixedNode.next();
        lastFixedNode.setNext(null);
        FixedNode firstCFGNodeDuplicate = (FixedNode) duplicates.get(firstCFGNode);
        replaceeGraph.addAfterFixed(lastFixedNode, firstCFGNodeDuplicate);

        rewireFrameStates(replacee, duplicates);
        updateStamps(replacee, duplicates);

        rewireMemoryGraph(replacee, duplicates);

        // Replace all usages of the replacee with the value returned by the snippet
        ReturnNode returnDuplicate = (ReturnNode) duplicates.get(returnNode);
        ValueNode returnValue = returnDuplicate.result();
        replacer.replace(replacee, returnValue);

        if (returnDuplicate.isAlive())
        {
            returnDuplicate.replaceAndDelete(next);
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
    public void instantiate(MetaAccessProvider metaAccess, FloatingNode replacee, UsageReplacer replacer, Arguments args)
    {
        // Inline the snippet nodes, replacing parameters with the given args in the process
        StartNode entryPointNode = snippet.start();
        StructuredGraph replaceeGraph = replacee.graph();
        EconomicMap<Node, Node> replacements = bind(replaceeGraph, metaAccess, args);
        MemoryAnchorNode anchorDuplicate = null;
        if (memoryAnchor != null)
        {
            anchorDuplicate = replaceeGraph.add(new MemoryAnchorNode());
            replacements.put(memoryAnchor, anchorDuplicate);
        }
        List<Node> floatingNodes = new ArrayList<>(nodes.size() - 2);
        for (Node n : nodes)
        {
            if (n != entryPointNode && n != returnNode)
            {
                floatingNodes.add(n);
            }
        }
        UnmodifiableEconomicMap<Node, Node> duplicates = inlineSnippet(replacee, replaceeGraph, replacements);

        rewireFrameStates(replacee, duplicates);
        updateStamps(replacee, duplicates);

        rewireMemoryGraph(replacee, duplicates);

        // Replace all usages of the replacee with the value returned by the snippet
        ValueNode returnValue = (ValueNode) duplicates.get(returnNode.result());
        replacer.replace(replacee, returnValue);
    }

    protected void rewireFrameStates(ValueNode replacee, UnmodifiableEconomicMap<Node, Node> duplicates)
    {
        if (replacee instanceof StateSplit)
        {
            for (StateSplit sideEffectNode : sideEffectNodes)
            {
                Node sideEffectDup = duplicates.get(sideEffectNode.asNode());
                ((StateSplit) sideEffectDup).setStateAfter(((StateSplit) replacee).stateAfter());
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(snippet.toString()).append('(');
        String sep = "";
        for (int i = 0; i < parameters.length; i++)
        {
            String name = "[" + i + "]";
            Object value = parameters[i];
            buf.append(sep);
            sep = ", ";
            if (value == null)
            {
                buf.append("<null> ").append(name);
            }
            else if (value.equals(UNUSED_PARAMETER))
            {
                buf.append("<unused> ").append(name);
            }
            else if (value.equals(CONSTANT_PARAMETER))
            {
                buf.append("<constant> ").append(name);
            }
            else if (value instanceof ParameterNode)
            {
                ParameterNode param = (ParameterNode) value;
                buf.append(param.getStackKind().getJavaName()).append(' ').append(name);
            }
            else
            {
                ParameterNode[] params = (ParameterNode[]) value;
                String kind = params.length == 0 ? "?" : params[0].getStackKind().getJavaName();
                buf.append(kind).append('[').append(params.length).append("] ").append(name);
            }
        }
        return buf.append(')').toString();
    }

    private static boolean checkTemplate(MetaAccessProvider metaAccess, Arguments args, ResolvedJavaMethod method, Signature signature)
    {
        for (int i = 0; i < args.info.getParameterCount(); i++)
        {
            if (args.info.isConstantParameter(i))
            {
                JavaKind kind = signature.getParameterKind(i);
            }
            else if (args.info.isVarargsParameter(i))
            {
                Varargs varargs = (Varargs) args.values[i];
            }
        }
        return true;
    }

    public void setMayRemoveLocation(boolean mayRemoveLocation)
    {
        this.mayRemoveLocation = mayRemoveLocation;
    }
}
