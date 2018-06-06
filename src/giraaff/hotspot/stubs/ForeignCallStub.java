package giraaff.hotspot.stubs;

import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkageImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.replacements.GraphKit;
import giraaff.replacements.nodes.ReadRegisterNode;
import giraaff.util.GraalError;
import giraaff.word.Word;
import giraaff.word.WordTypes;

///
// A {@linkplain #getGraph generated} stub for a {@link HotSpotForeignCallLinkage.Transition non-leaf}
// foreign call from compiled code. A stub is required for such calls as the caller may be scheduled for
// deoptimization while the call is in progress. And since these are foreign/runtime calls on slow paths,
// we don't want to force the register allocator to spill around the call. As such, this stub saves and
// restores all allocatable registers. It also {@linkplain StubUtil#handlePendingException(Word, boolean) handles}
// any exceptions raised during the foreign call.
///
// @class ForeignCallStub
public final class ForeignCallStub extends Stub
{
    ///
    // The target of the call.
    ///
    // @field
    private final HotSpotForeignCallLinkage ___target;

    ///
    // Specifies if the JavaThread value for the current thread is to be prepended to the arguments
    // for the call to {@link #target}.
    ///
    // @field
    protected final boolean ___prependThread;

    ///
    // Creates a stub for a call to code at a given address.
    //
    // @param address the address of the code to call
    // @param descriptor the signature of the call to this stub
    // @param prependThread true if the JavaThread value for the current thread is to be prepended
    //            to the arguments for the call to {@code address}
    // @param reexecutable specifies if the stub call can be re-executed without (meaningful) side
    //            effects. Deoptimization will not return to a point before a stub call that cannot
    //            be re-executed.
    // @param killedLocations the memory locations killed by the stub call
    ///
    // @cons ForeignCallStub
    public ForeignCallStub(HotSpotProviders __providers, long __address, ForeignCallDescriptor __descriptor, boolean __prependThread, HotSpotForeignCallLinkage.Transition __transition, boolean __reexecutable, LocationIdentity... __killedLocations)
    {
        super(__providers, HotSpotForeignCallLinkageImpl.create(__providers.getMetaAccess(), __providers.getCodeCache(), __providers.getWordTypes(), __providers.getForeignCalls(), __descriptor, 0L, HotSpotForeignCallLinkage.RegisterEffect.PRESERVES_REGISTERS, HotSpotCallingConventionType.JavaCall, HotSpotCallingConventionType.JavaCallee, __transition, __reexecutable, __killedLocations));
        this.___prependThread = __prependThread;
        Class<?>[] __targetParameterTypes = createTargetParameters(__descriptor);
        ForeignCallDescriptor __targetSig = new ForeignCallDescriptor(__descriptor.getName() + ":C", __descriptor.getResultType(), __targetParameterTypes);
        this.___target = HotSpotForeignCallLinkageImpl.create(__providers.getMetaAccess(), __providers.getCodeCache(), __providers.getWordTypes(), __providers.getForeignCalls(), __targetSig, __address, HotSpotForeignCallLinkage.RegisterEffect.DESTROYS_REGISTERS, HotSpotCallingConventionType.NativeCall, HotSpotCallingConventionType.NativeCall, __transition, __reexecutable, __killedLocations);
    }

    ///
    // Gets the linkage information for the call from this stub.
    ///
    public HotSpotForeignCallLinkage getTargetLinkage()
    {
        return this.___target;
    }

    private Class<?>[] createTargetParameters(ForeignCallDescriptor __descriptor)
    {
        Class<?>[] __parameters = __descriptor.getArgumentTypes();
        if (this.___prependThread)
        {
            Class<?>[] __newParameters = new Class<?>[__parameters.length + 1];
            System.arraycopy(__parameters, 0, __newParameters, 1, __parameters.length);
            __newParameters[0] = Word.class;
            return __newParameters;
        }
        return __parameters;
    }

    @Override
    protected ResolvedJavaMethod getInstalledCodeOwner()
    {
        return null;
    }

    ///
    // Creates a graph for this stub.
    //
    // If the stub returns an object, the graph created corresponds to this pseudo code:
    //
    // <pre>
    //     Object foreignFunctionStub(args...) {
    //         foreignFunction(currentThread,  args);
    //         if (clearPendingException(thread())) {
    //             getAndClearObjectResult(thread());
    //             DeoptimizeCallerNode.deopt(InvalidateReprofile, RuntimeConstraint);
    //         }
    //         return getAndClearObjectResult(thread());
    //     }
    // </pre>
    //
    // If the stub returns a primitive or word, the graph created corresponds to this pseudo code
    // (using {@code int} as the primitive return type):
    //
    // <pre>
    //     int foreignFunctionStub(args...) {
    //         int result = foreignFunction(currentThread,  args);
    //         if (clearPendingException(thread())) {
    //             DeoptimizeCallerNode.deopt(InvalidateReprofile, RuntimeConstraint);
    //         }
    //         return result;
    //     }
    // </pre>
    //
    // If the stub is void, the graph created corresponds to this pseudo code:
    //
    // <pre>
    //     void foreignFunctionStub(args...) {
    //         foreignFunction(currentThread,  args);
    //         if (clearPendingException(thread())) {
    //             DeoptimizeCallerNode.deopt(InvalidateReprofile, RuntimeConstraint);
    //         }
    //     }
    // </pre>
    //
    // In each example above, the {@code currentThread} argument is the C++ JavaThread value (i.e.,
    // %r15 on AMD64) and is only prepended if {@link #prependThread} is true.
    ///
    @Override
    protected StructuredGraph getStubGraph()
    {
        WordTypes __wordTypes = this.___providers.getWordTypes();
        Class<?>[] __args = this.___linkage.getDescriptor().getArgumentTypes();
        boolean __isObjectResult = !LIRKind.isValue(this.___linkage.getOutgoingCallingConvention().getReturn());

        try
        {
            ResolvedJavaMethod __thisMethod = this.___providers.getMetaAccess().lookupJavaMethod(ForeignCallStub.class.getDeclaredMethod("getStubGraph"));
            GraphKit __kit = new GraphKit(__thisMethod, this.___providers, __wordTypes, this.___providers.getGraphBuilderPlugins());
            StructuredGraph __graph = __kit.getGraph();
            ParameterNode[] __params = createParameters(__kit, __args);
            ReadRegisterNode __thread = __kit.append(new ReadRegisterNode(this.___providers.getRegisters().getThreadRegister(), __wordTypes.getWordKind(), true, false));
            ValueNode __result = createTargetCall(__kit, __params, __thread);
            __kit.createInvoke(StubUtil.class, "handlePendingException", __thread, ConstantNode.forBoolean(__isObjectResult, __graph));
            if (__isObjectResult)
            {
                __result = __kit.createInvoke(HotSpotReplacementsUtil.class, "getAndClearObjectResult", __thread);
            }
            __kit.append(new ReturnNode(this.___linkage.getDescriptor().getResultType() == void.class ? null : __result));

            __kit.inlineInvokes("Foreign call stub.", "Backend");
            new RemoveValueProxyPhase().apply(__graph);

            return __graph;
        }
        catch (Exception __e)
        {
            throw GraalError.shouldNotReachHere(__e);
        }
    }

    private ParameterNode[] createParameters(GraphKit __kit, Class<?>[] __args)
    {
        ParameterNode[] __params = new ParameterNode[__args.length];
        ResolvedJavaType __accessingClass = this.___providers.getMetaAccess().lookupJavaType(getClass());
        for (int __i = 0; __i < __args.length; __i++)
        {
            ResolvedJavaType __type = this.___providers.getMetaAccess().lookupJavaType(__args[__i]).resolve(__accessingClass);
            StampPair __stamp = StampFactory.forDeclaredType(__kit.getGraph().getAssumptions(), __type, false);
            ParameterNode __param = __kit.unique(new ParameterNode(__i, __stamp));
            __params[__i] = __param;
        }
        return __params;
    }

    private StubForeignCallNode createTargetCall(GraphKit __kit, ParameterNode[] __params, ReadRegisterNode __thread)
    {
        Stamp __stamp = StampFactory.forKind(JavaKind.fromJavaClass(this.___target.getDescriptor().getResultType()));
        if (this.___prependThread)
        {
            ValueNode[] __targetArguments = new ValueNode[1 + __params.length];
            __targetArguments[0] = __thread;
            System.arraycopy(__params, 0, __targetArguments, 1, __params.length);
            return __kit.append(new StubForeignCallNode(this.___providers.getForeignCalls(), __stamp, this.___target.getDescriptor(), __targetArguments));
        }
        else
        {
            return __kit.append(new StubForeignCallNode(this.___providers.getForeignCalls(), __stamp, this.___target.getDescriptor(), __params));
        }
    }
}
