package giraaff.hotspot;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.ValueKindFactory;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotForeignCallTarget;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicSet;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.target.Backend;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotForeignCallsProvider;
import giraaff.hotspot.stubs.Stub;
import giraaff.word.WordTypes;

///
// The details required to link a HotSpot runtime or stub call.
///
// @class HotSpotForeignCallLinkageImpl
public final class HotSpotForeignCallLinkageImpl extends HotSpotForeignCallTarget implements HotSpotForeignCallLinkage
{
    ///
    // The descriptor of the call.
    ///
    // @field
    protected final ForeignCallDescriptor ___descriptor;

    ///
    // Non-null (eventually) iff this is a call to a compiled {@linkplain Stub stub}.
    ///
    // @field
    private Stub ___stub;

    ///
    // The calling convention for this call.
    ///
    // @field
    private final CallingConvention ___outgoingCallingConvention;

    ///
    // The calling convention for incoming arguments to the stub, iff this call uses a compiled
    // {@linkplain Stub stub}.
    ///
    // @field
    private final CallingConvention ___incomingCallingConvention;

    // @field
    private final HotSpotForeignCallLinkage.RegisterEffect ___effect;

    // @field
    private final HotSpotForeignCallLinkage.Transition ___transition;

    ///
    // The registers and stack slots defined/killed by the call.
    ///
    // @field
    private Value[] ___temporaries = AllocatableValue.NONE;

    ///
    // The memory locations killed by the call.
    ///
    // @field
    private final LocationIdentity[] ___killedLocations;

    // @field
    private final boolean ___reexecutable;

    ///
    // Creates a {@link HotSpotForeignCallLinkage}.
    //
    // @param descriptor the descriptor of the call
    // @param address the address of the code to call
    // @param effect specifies if the call destroys or preserves all registers (apart from
    //            temporaries which are always destroyed)
    // @param outgoingCcType outgoing (caller) calling convention type
    // @param incomingCcType incoming (callee) calling convention type (can be null)
    // @param transition specifies if this is a {@linkplain #needsDebugInfo() leaf} call
    // @param reexecutable specifies if the call can be re-executed without (meaningful) side effects.
    //            Deoptimization will not return to a point before a call that cannot be re-executed.
    // @param killedLocations the memory locations killed by the call
    ///
    public static HotSpotForeignCallLinkage create(MetaAccessProvider __metaAccess, CodeCacheProvider __codeCache, WordTypes __wordTypes, HotSpotForeignCallsProvider __foreignCalls, ForeignCallDescriptor __descriptor, long __address, HotSpotForeignCallLinkage.RegisterEffect __effect, CallingConvention.Type __outgoingCcType, CallingConvention.Type __incomingCcType, HotSpotForeignCallLinkage.Transition __transition, boolean __reexecutable, LocationIdentity... __killedLocations)
    {
        CallingConvention __outgoingCc = createCallingConvention(__metaAccess, __codeCache, __wordTypes, __foreignCalls, __descriptor, __outgoingCcType);
        CallingConvention __incomingCc = __incomingCcType == null ? null : createCallingConvention(__metaAccess, __codeCache, __wordTypes, __foreignCalls, __descriptor, __incomingCcType);
        HotSpotForeignCallLinkageImpl __linkage = new HotSpotForeignCallLinkageImpl(__descriptor, __address, __effect, __transition, __outgoingCc, __incomingCc, __reexecutable, __killedLocations);
        if (__outgoingCcType == HotSpotCallingConventionType.NativeCall)
        {
            __linkage.___temporaries = __foreignCalls.getNativeABICallerSaveRegisters();
        }
        return __linkage;
    }

    ///
    // Gets a calling convention for a given descriptor and call type.
    ///
    public static CallingConvention createCallingConvention(MetaAccessProvider __metaAccess, CodeCacheProvider __codeCache, WordTypes __wordTypes, ValueKindFactory<?> __valueKindFactory, ForeignCallDescriptor __descriptor, CallingConvention.Type __ccType)
    {
        Class<?>[] __argumentTypes = __descriptor.getArgumentTypes();
        JavaType[] __parameterTypes = new JavaType[__argumentTypes.length];
        for (int __i = 0; __i < __parameterTypes.length; ++__i)
        {
            __parameterTypes[__i] = asJavaType(__argumentTypes[__i], __metaAccess, __wordTypes);
        }
        JavaType __returnType = asJavaType(__descriptor.getResultType(), __metaAccess, __wordTypes);
        RegisterConfig __regConfig = __codeCache.getRegisterConfig();
        return __regConfig.getCallingConvention(__ccType, __returnType, __parameterTypes, __valueKindFactory);
    }

    private static JavaType asJavaType(Class<?> __type, MetaAccessProvider __metaAccess, WordTypes __wordTypes)
    {
        ResolvedJavaType __javaType = __metaAccess.lookupJavaType(__type);
        if (__wordTypes.isWord(__javaType))
        {
            __javaType = __metaAccess.lookupJavaType(__wordTypes.getWordKind().toJavaClass());
        }
        return __javaType;
    }

    // @cons HotSpotForeignCallLinkageImpl
    public HotSpotForeignCallLinkageImpl(ForeignCallDescriptor __descriptor, long __address, HotSpotForeignCallLinkage.RegisterEffect __effect, HotSpotForeignCallLinkage.Transition __transition, CallingConvention __outgoingCallingConvention, CallingConvention __incomingCallingConvention, boolean __reexecutable, LocationIdentity... __killedLocations)
    {
        super(__address);
        this.___descriptor = __descriptor;
        this.address = __address;
        this.___effect = __effect;
        this.___transition = __transition;
        this.___outgoingCallingConvention = __outgoingCallingConvention;
        this.___incomingCallingConvention = __incomingCallingConvention != null ? __incomingCallingConvention : __outgoingCallingConvention;
        this.___reexecutable = __reexecutable;
        this.___killedLocations = __killedLocations;
    }

    @Override
    public boolean isReexecutable()
    {
        return this.___reexecutable;
    }

    @Override
    public boolean isGuaranteedSafepoint()
    {
        return this.___transition == HotSpotForeignCallLinkage.Transition.SAFEPOINT;
    }

    @Override
    public LocationIdentity[] getKilledLocations()
    {
        return this.___killedLocations;
    }

    @Override
    public CallingConvention getOutgoingCallingConvention()
    {
        return this.___outgoingCallingConvention;
    }

    @Override
    public CallingConvention getIncomingCallingConvention()
    {
        return this.___incomingCallingConvention;
    }

    @Override
    public Value[] getTemporaries()
    {
        if (this.___temporaries.length == 0)
        {
            return this.___temporaries;
        }
        return this.___temporaries.clone();
    }

    @Override
    public long getMaxCallTargetOffset()
    {
        return HotSpotRuntime.JVMCI.getHostJVMCIBackend().getCodeCache().getMaxCallTargetOffset(this.address);
    }

    @Override
    public ForeignCallDescriptor getDescriptor()
    {
        return this.___descriptor;
    }

    @Override
    public void setCompiledStub(Stub __stub)
    {
        this.___stub = __stub;
    }

    ///
    // Determines if this is a call to a compiled {@linkplain Stub stub}.
    ///
    @Override
    public boolean isCompiledStub()
    {
        return this.address == 0L || this.___stub != null;
    }

    @Override
    public Stub getStub()
    {
        return this.___stub;
    }

    private boolean checkStubCondition()
    {
        return true;
    }

    @Override
    public void finalizeAddress(Backend __backend)
    {
        if (this.address == 0)
        {
            InstalledCode __code = this.___stub.getCode(__backend);

            EconomicSet<Register> __destroyedRegisters = this.___stub.getDestroyedCallerRegisters();
            if (!__destroyedRegisters.isEmpty())
            {
                AllocatableValue[] __temporaryLocations = new AllocatableValue[__destroyedRegisters.size()];
                int __i = 0;
                for (Register __reg : __destroyedRegisters)
                {
                    __temporaryLocations[__i++] = __reg.asValue();
                }
                this.___temporaries = __temporaryLocations;
            }
            this.address = __code.getStart();
        }
    }

    @Override
    public long getAddress()
    {
        return this.address;
    }

    @Override
    public boolean destroysRegisters()
    {
        return this.___effect == HotSpotForeignCallLinkage.RegisterEffect.DESTROYS_REGISTERS;
    }

    @Override
    public boolean needsDebugInfo()
    {
        return this.___transition == HotSpotForeignCallLinkage.Transition.SAFEPOINT;
    }

    @Override
    public boolean mayContainFP()
    {
        return this.___transition != HotSpotForeignCallLinkage.Transition.LEAF_NOFP;
    }

    @Override
    public boolean needsJavaFrameAnchor()
    {
        if (this.___transition == HotSpotForeignCallLinkage.Transition.SAFEPOINT || this.___transition == HotSpotForeignCallLinkage.Transition.STACK_INSPECTABLE_LEAF)
        {
            if (this.___stub != null)
            {
                // the stub will do the JavaFrameAnchor management around the runtime call(s) it makes
                return false;
            }
            else
            {
                return true;
            }
        }
        return false;
    }
}
