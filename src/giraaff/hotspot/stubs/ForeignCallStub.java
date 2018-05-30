package giraaff.hotspot.stubs;

import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.CompilationIdentifier;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.HotSpotForeignCallLinkageImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.options.OptionValues;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.replacements.GraphKit;
import giraaff.replacements.nodes.ReadRegisterNode;
import giraaff.util.GraalError;
import giraaff.word.Word;
import giraaff.word.WordTypes;

/**
 * A {@linkplain #getGraph generated} stub for a {@link Transition non-leaf} foreign call from
 * compiled code. A stub is required for such calls as the caller may be scheduled for
 * deoptimization while the call is in progress. And since these are foreign/runtime calls on slow
 * paths, we don't want to force the register allocator to spill around the call. As such, this stub
 * saves and restores all allocatable registers. It also
 * {@linkplain StubUtil#handlePendingException(Word, boolean) handles} any exceptions raised during
 * the foreign call.
 */
// @class ForeignCallStub
public final class ForeignCallStub extends Stub
{
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
    // @cons
    public ForeignCallStub(OptionValues options, HotSpotProviders providers, long address, ForeignCallDescriptor descriptor, boolean prependThread, Transition transition, boolean reexecutable, LocationIdentity... killedLocations)
    {
        super(options, providers, HotSpotForeignCallLinkageImpl.create(providers.getMetaAccess(), providers.getCodeCache(), providers.getWordTypes(), providers.getForeignCalls(), descriptor, 0L, RegisterEffect.PRESERVES_REGISTERS, HotSpotCallingConventionType.JavaCall, HotSpotCallingConventionType.JavaCallee, transition, reexecutable, killedLocations));
        this.prependThread = prependThread;
        Class<?>[] targetParameterTypes = createTargetParameters(descriptor);
        ForeignCallDescriptor targetSig = new ForeignCallDescriptor(descriptor.getName() + ":C", descriptor.getResultType(), targetParameterTypes);
        target = HotSpotForeignCallLinkageImpl.create(providers.getMetaAccess(), providers.getCodeCache(), providers.getWordTypes(), providers.getForeignCalls(), targetSig, address, RegisterEffect.DESTROYS_REGISTERS, HotSpotCallingConventionType.NativeCall, HotSpotCallingConventionType.NativeCall, transition, reexecutable, killedLocations);
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

    /**
     * Creates a graph for this stub.
     *
     * If the stub returns an object, the graph created corresponds to this pseudo code:
     *
     * <pre>
     *     Object foreignFunctionStub(args...) {
     *         foreignFunction(currentThread,  args);
     *         if (clearPendingException(thread())) {
     *             getAndClearObjectResult(thread());
     *             DeoptimizeCallerNode.deopt(InvalidateReprofile, RuntimeConstraint);
     *         }
     *         return getAndClearObjectResult(thread());
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
    protected StructuredGraph getGraph(CompilationIdentifier compilationId)
    {
        WordTypes wordTypes = providers.getWordTypes();
        Class<?>[] args = linkage.getDescriptor().getArgumentTypes();
        boolean isObjectResult = !LIRKind.isValue(linkage.getOutgoingCallingConvention().getReturn());

        try
        {
            ResolvedJavaMethod thisMethod = providers.getMetaAccess().lookupJavaMethod(ForeignCallStub.class.getDeclaredMethod("getGraph", CompilationIdentifier.class));
            GraphKit kit = new GraphKit(thisMethod, providers, wordTypes, providers.getGraphBuilderPlugins(), compilationId, toString());
            StructuredGraph graph = kit.getGraph();
            ParameterNode[] params = createParameters(kit, args);
            ReadRegisterNode thread = kit.append(new ReadRegisterNode(providers.getRegisters().getThreadRegister(), wordTypes.getWordKind(), true, false));
            ValueNode result = createTargetCall(kit, params, thread);
            kit.createInvoke(StubUtil.class, "handlePendingException", thread, ConstantNode.forBoolean(isObjectResult, graph));
            if (isObjectResult)
            {
                result = kit.createInvoke(HotSpotReplacementsUtil.class, "getAndClearObjectResult", thread);
            }
            kit.append(new ReturnNode(linkage.getDescriptor().getResultType() == void.class ? null : result));

            kit.inlineInvokes("Foreign call stub.", "Backend");
            new RemoveValueProxyPhase().apply(graph);

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
