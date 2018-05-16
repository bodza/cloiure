package graalvm.compiler.hotspot.stubs;

import static jdk.vm.ci.hotspot.HotSpotCallingConventionType.JavaCall;
import static jdk.vm.ci.hotspot.HotSpotCallingConventionType.JavaCallee;
import static jdk.vm.ci.hotspot.HotSpotCallingConventionType.NativeCall;
import static graalvm.compiler.hotspot.HotSpotForeignCallLinkage.RegisterEffect.DESTROYS_REGISTERS;
import static graalvm.compiler.hotspot.HotSpotForeignCallLinkage.RegisterEffect.PRESERVES_REGISTERS;

import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.debug.JavaMethodContext;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage.Transition;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkageImpl;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.StubForeignCallNode;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.ParameterNode;
import graalvm.compiler.nodes.ReturnNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.common.RemoveValueProxyPhase;
import graalvm.compiler.replacements.GraphKit;
import graalvm.compiler.replacements.nodes.ReadRegisterNode;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordTypes;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotSignature;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

/**
 * A {@linkplain #getGraph generated} stub for a {@link Transition non-leaf} foreign call from
 * compiled code. A stub is required for such calls as the caller may be scheduled for
 * deoptimization while the call is in progress. And since these are foreign/runtime calls on slow
 * paths, we don't want to force the register allocator to spill around the call. As such, this stub
 * saves and restores all allocatable registers. It also
 * {@linkplain StubUtil#handlePendingException(Word, boolean) handles} any exceptions raised during
 * the foreign call.
 */
public class ForeignCallStub extends Stub
{
    private final HotSpotJVMCIRuntimeProvider jvmciRuntime;

    /**
     * The target of the call.
     */
    private final HotSpotForeignCallLinkage target;

    /**
     * Specifies if the JavaThread value for the current thread is to be prepended to the arguments
     * for the call to {@link #target}.
     */
    protected final boolean prependThread;

    /**
     * Creates a stub for a call to code at a given address.
     *
     * @param address the address of the code to call
     * @param descriptor the signature of the call to this stub
     * @param prependThread true if the JavaThread value for the current thread is to be prepended
     *            to the arguments for the call to {@code address}
     * @param reexecutable specifies if the stub call can be re-executed without (meaningful) side
     *            effects. Deoptimization will not return to a point before a stub call that cannot
     *            be re-executed.
     * @param killedLocations the memory locations killed by the stub call
     */
    public ForeignCallStub(OptionValues options, HotSpotJVMCIRuntimeProvider runtime, HotSpotProviders providers, long address, ForeignCallDescriptor descriptor, boolean prependThread, Transition transition, boolean reexecutable, LocationIdentity... killedLocations)
    {
        super(options, providers, HotSpotForeignCallLinkageImpl.create(providers.getMetaAccess(), providers.getCodeCache(), providers.getWordTypes(), providers.getForeignCalls(), descriptor, 0L, PRESERVES_REGISTERS, JavaCall, JavaCallee, transition, reexecutable, killedLocations));
        this.jvmciRuntime = runtime;
        this.prependThread = prependThread;
        Class<?>[] targetParameterTypes = createTargetParameters(descriptor);
        ForeignCallDescriptor targetSig = new ForeignCallDescriptor(descriptor.getName() + ":C", descriptor.getResultType(), targetParameterTypes);
        target = HotSpotForeignCallLinkageImpl.create(providers.getMetaAccess(), providers.getCodeCache(), providers.getWordTypes(), providers.getForeignCalls(), targetSig, address, DESTROYS_REGISTERS, NativeCall, NativeCall, transition, reexecutable, killedLocations);
    }

    /**
     * Gets the linkage information for the call from this stub.
     */
    public HotSpotForeignCallLinkage getTargetLinkage()
    {
        return target;
    }

    private Class<?>[] createTargetParameters(ForeignCallDescriptor descriptor)
    {
        Class<?>[] parameters = descriptor.getArgumentTypes();
        if (prependThread)
        {
            Class<?>[] newParameters = new Class<?>[parameters.length + 1];
            System.arraycopy(parameters, 0, newParameters, 1, parameters.length);
            newParameters[0] = Word.class;
            return newParameters;
        }
        return parameters;
    }

    @Override
    protected ResolvedJavaMethod getInstalledCodeOwner()
    {
        return null;
    }

    private class DebugScopeContext implements JavaMethod, JavaMethodContext
    {
        @Override
        public JavaMethod asJavaMethod()
        {
            return this;
        }

        @Override
        public Signature getSignature()
        {
            ForeignCallDescriptor d = linkage.getDescriptor();
            MetaAccessProvider metaAccess = providers.getMetaAccess();
            Class<?>[] arguments = d.getArgumentTypes();
            ResolvedJavaType[] parameters = new ResolvedJavaType[arguments.length];
            for (int i = 0; i < arguments.length; i++)
            {
                parameters[i] = metaAccess.lookupJavaType(arguments[i]);
            }
            return new HotSpotSignature(jvmciRuntime, metaAccess.lookupJavaType(d.getResultType()), parameters);
        }

        @Override
        public String getName()
        {
            return linkage.getDescriptor().getName();
        }

        @Override
        public JavaType getDeclaringClass()
        {
            return providers.getMetaAccess().lookupJavaType(ForeignCallStub.class);
        }

        @Override
        public String toString()
        {
            return format("ForeignCallStub<%n(%p)>");
        }
    }

    @Override
    protected Object debugScopeContext()
    {
        return new DebugScopeContext()
        {
        };
    }

    /**
     * Creates a graph for this stub.
     * <p>
     * If the stub returns an object, the graph created corresponds to this pseudo code:
     *
     * <pre>
     *     Object foreignFunctionStub(args...) {
     *         foreignFunction(currentThread,  args);
     *         if (clearPendingException(thread())) {
     *             getAndClearObjectResult(thread());
     *             DeoptimizeCallerNode.deopt(InvalidateReprofile, RuntimeConstraint);
     *         }
     *         return verifyObject(getAndClearObjectResult(thread()));
     *     }
     * </pre>
     *
     * If the stub returns a primitive or word, the graph created corresponds to this pseudo code
     * (using {@code int} as the primitive return type):
     *
     * <pre>
     *     int foreignFunctionStub(args...) {
     *         int result = foreignFunction(currentThread,  args);
     *         if (clearPendingException(thread())) {
     *             DeoptimizeCallerNode.deopt(InvalidateReprofile, RuntimeConstraint);
     *         }
     *         return result;
     *     }
     * </pre>
     *
     * If the stub is void, the graph created corresponds to this pseudo code:
     *
     * <pre>
     *     void foreignFunctionStub(args...) {
     *         foreignFunction(currentThread,  args);
     *         if (clearPendingException(thread())) {
     *             DeoptimizeCallerNode.deopt(InvalidateReprofile, RuntimeConstraint);
     *         }
     *     }
     * </pre>
     *
     * In each example above, the {@code currentThread} argument is the C++ JavaThread value (i.e.,
     * %r15 on AMD64) and is only prepended if {@link #prependThread} is true.
     */
    @Override
    @SuppressWarnings("try")
    protected StructuredGraph getGraph(DebugContext debug, CompilationIdentifier compilationId)
    {
        WordTypes wordTypes = providers.getWordTypes();
        Class<?>[] args = linkage.getDescriptor().getArgumentTypes();
        boolean isObjectResult = !LIRKind.isValue(linkage.getOutgoingCallingConvention().getReturn());

        try
        {
            ResolvedJavaMethod thisMethod = providers.getMetaAccess().lookupJavaMethod(ForeignCallStub.class.getDeclaredMethod("getGraph", DebugContext.class, CompilationIdentifier.class));
            GraphKit kit = new GraphKit(debug, thisMethod, providers, wordTypes, providers.getGraphBuilderPlugins(), compilationId, toString());
            StructuredGraph graph = kit.getGraph();
            ParameterNode[] params = createParameters(kit, args);
            ReadRegisterNode thread = kit.append(new ReadRegisterNode(providers.getRegisters().getThreadRegister(), wordTypes.getWordKind(), true, false));
            ValueNode result = createTargetCall(kit, params, thread);
            kit.createInvoke(StubUtil.class, "handlePendingException", thread, ConstantNode.forBoolean(isObjectResult, graph));
            if (isObjectResult)
            {
                InvokeNode object = kit.createInvoke(HotSpotReplacementsUtil.class, "getAndClearObjectResult", thread);
                result = kit.createInvoke(StubUtil.class, "verifyObject", object);
            }
            kit.append(new ReturnNode(linkage.getDescriptor().getResultType() == void.class ? null : result));
            debug.dump(DebugContext.VERBOSE_LEVEL, graph, "Initial stub graph");

            kit.inlineInvokes("Foreign call stub.", "Backend");
            new RemoveValueProxyPhase().apply(graph);

            debug.dump(DebugContext.VERBOSE_LEVEL, graph, "Stub graph before compilation");
            return graph;
        }
        catch (Exception e)
        {
            throw GraalError.shouldNotReachHere(e);
        }
    }

    private ParameterNode[] createParameters(GraphKit kit, Class<?>[] args)
    {
        ParameterNode[] params = new ParameterNode[args.length];
        ResolvedJavaType accessingClass = providers.getMetaAccess().lookupJavaType(getClass());
        for (int i = 0; i < args.length; i++)
        {
            ResolvedJavaType type = providers.getMetaAccess().lookupJavaType(args[i]).resolve(accessingClass);
            StampPair stamp = StampFactory.forDeclaredType(kit.getGraph().getAssumptions(), type, false);
            ParameterNode param = kit.unique(new ParameterNode(i, stamp));
            params[i] = param;
        }
        return params;
    }

    private StubForeignCallNode createTargetCall(GraphKit kit, ParameterNode[] params, ReadRegisterNode thread)
    {
        Stamp stamp = StampFactory.forKind(JavaKind.fromJavaClass(target.getDescriptor().getResultType()));
        if (prependThread)
        {
            ValueNode[] targetArguments = new ValueNode[1 + params.length];
            targetArguments[0] = thread;
            System.arraycopy(params, 0, targetArguments, 1, params.length);
            return kit.append(new StubForeignCallNode(providers.getForeignCalls(), stamp, target.getDescriptor(), targetArguments));
        }
        else
        {
            return kit.append(new StubForeignCallNode(providers.getForeignCalls(), stamp, target.getDescriptor(), params));
        }
    }
}
