package graalvm.compiler.hotspot;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.ValueKindFactory;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotForeignCallTarget;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicSet;
import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.target.Backend;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import graalvm.compiler.hotspot.meta.HotSpotForeignCallsProvider;
import graalvm.compiler.hotspot.stubs.Stub;
import graalvm.compiler.word.WordTypes;

/**
 * The details required to link a HotSpot runtime or stub call.
 */
public class HotSpotForeignCallLinkageImpl extends HotSpotForeignCallTarget implements HotSpotForeignCallLinkage
{
    /**
     * The descriptor of the call.
     */
    protected final ForeignCallDescriptor descriptor;

    /**
     * Non-null (eventually) iff this is a call to a compiled {@linkplain Stub stub}.
     */
    private Stub stub;

    /**
     * The calling convention for this call.
     */
    private final CallingConvention outgoingCallingConvention;

    /**
     * The calling convention for incoming arguments to the stub, iff this call uses a compiled
     * {@linkplain Stub stub}.
     */
    private final CallingConvention incomingCallingConvention;

    private final RegisterEffect effect;

    private final Transition transition;

    /**
     * The registers and stack slots defined/killed by the call.
     */
    private Value[] temporaries = AllocatableValue.NONE;

    /**
     * The memory locations killed by the call.
     */
    private final LocationIdentity[] killedLocations;

    private final boolean reexecutable;

    /**
     * Creates a {@link HotSpotForeignCallLinkage}.
     *
     * @param descriptor the descriptor of the call
     * @param address the address of the code to call
     * @param effect specifies if the call destroys or preserves all registers (apart from
     *            temporaries which are always destroyed)
     * @param outgoingCcType outgoing (caller) calling convention type
     * @param incomingCcType incoming (callee) calling convention type (can be null)
     * @param transition specifies if this is a {@linkplain #needsDebugInfo() leaf} call
     * @param reexecutable specifies if the call can be re-executed without (meaningful) side
     *            effects. Deoptimization will not return to a point before a call that cannot be
     *            re-executed.
     * @param killedLocations the memory locations killed by the call
     */
    public static HotSpotForeignCallLinkage create(MetaAccessProvider metaAccess, CodeCacheProvider codeCache, WordTypes wordTypes, HotSpotForeignCallsProvider foreignCalls, ForeignCallDescriptor descriptor, long address, RegisterEffect effect, Type outgoingCcType, Type incomingCcType, Transition transition, boolean reexecutable, LocationIdentity... killedLocations)
    {
        CallingConvention outgoingCc = createCallingConvention(metaAccess, codeCache, wordTypes, foreignCalls, descriptor, outgoingCcType);
        CallingConvention incomingCc = incomingCcType == null ? null : createCallingConvention(metaAccess, codeCache, wordTypes, foreignCalls, descriptor, incomingCcType);
        HotSpotForeignCallLinkageImpl linkage = new HotSpotForeignCallLinkageImpl(descriptor, address, effect, transition, outgoingCc, incomingCc, reexecutable, killedLocations);
        if (outgoingCcType == HotSpotCallingConventionType.NativeCall)
        {
            linkage.temporaries = foreignCalls.getNativeABICallerSaveRegisters();
        }
        return linkage;
    }

    /**
     * Gets a calling convention for a given descriptor and call type.
     */
    public static CallingConvention createCallingConvention(MetaAccessProvider metaAccess, CodeCacheProvider codeCache, WordTypes wordTypes, ValueKindFactory<?> valueKindFactory, ForeignCallDescriptor descriptor, Type ccType)
    {
        Class<?>[] argumentTypes = descriptor.getArgumentTypes();
        JavaType[] parameterTypes = new JavaType[argumentTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i)
        {
            parameterTypes[i] = asJavaType(argumentTypes[i], metaAccess, wordTypes);
        }
        JavaType returnType = asJavaType(descriptor.getResultType(), metaAccess, wordTypes);
        RegisterConfig regConfig = codeCache.getRegisterConfig();
        return regConfig.getCallingConvention(ccType, returnType, parameterTypes, valueKindFactory);
    }

    private static JavaType asJavaType(Class<?> type, MetaAccessProvider metaAccess, WordTypes wordTypes)
    {
        ResolvedJavaType javaType = metaAccess.lookupJavaType(type);
        if (wordTypes.isWord(javaType))
        {
            javaType = metaAccess.lookupJavaType(wordTypes.getWordKind().toJavaClass());
        }
        return javaType;
    }

    public HotSpotForeignCallLinkageImpl(ForeignCallDescriptor descriptor, long address, RegisterEffect effect, Transition transition, CallingConvention outgoingCallingConvention, CallingConvention incomingCallingConvention, boolean reexecutable, LocationIdentity... killedLocations)
    {
        super(address);
        this.descriptor = descriptor;
        this.address = address;
        this.effect = effect;
        this.transition = transition;
        this.outgoingCallingConvention = outgoingCallingConvention;
        this.incomingCallingConvention = incomingCallingConvention != null ? incomingCallingConvention : outgoingCallingConvention;
        this.reexecutable = reexecutable;
        this.killedLocations = killedLocations;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(stub == null ? descriptor.toString() : stub.toString());
        sb.append("@0x").append(Long.toHexString(address)).append(':').append(outgoingCallingConvention).append(":").append(incomingCallingConvention);
        if (temporaries != null && temporaries.length != 0)
        {
            sb.append("; temps=");
            String sep = "";
            for (Value op : temporaries)
            {
                sb.append(sep).append(op);
                sep = ",";
            }
        }
        return sb.toString();
    }

    @Override
    public boolean isReexecutable()
    {
        return reexecutable;
    }

    @Override
    public boolean isGuaranteedSafepoint()
    {
        return transition == Transition.SAFEPOINT;
    }

    @Override
    public LocationIdentity[] getKilledLocations()
    {
        return killedLocations;
    }

    @Override
    public CallingConvention getOutgoingCallingConvention()
    {
        return outgoingCallingConvention;
    }

    @Override
    public CallingConvention getIncomingCallingConvention()
    {
        return incomingCallingConvention;
    }

    @Override
    public Value[] getTemporaries()
    {
        if (temporaries.length == 0)
        {
            return temporaries;
        }
        return temporaries.clone();
    }

    @Override
    public long getMaxCallTargetOffset()
    {
        return HotSpotJVMCIRuntime.runtime().getHostJVMCIBackend().getCodeCache().getMaxCallTargetOffset(address);
    }

    @Override
    public ForeignCallDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public void setCompiledStub(Stub stub)
    {
        this.stub = stub;
    }

    /**
     * Determines if this is a call to a compiled {@linkplain Stub stub}.
     */
    @Override
    public boolean isCompiledStub()
    {
        return address == 0L || stub != null;
    }

    @Override
    public Stub getStub()
    {
        return stub;
    }

    private boolean checkStubCondition()
    {
        return true;
    }

    @Override
    public void finalizeAddress(Backend backend)
    {
        if (address == 0)
        {
            InstalledCode code = stub.getCode(backend);

            EconomicSet<Register> destroyedRegisters = stub.getDestroyedCallerRegisters();
            if (!destroyedRegisters.isEmpty())
            {
                AllocatableValue[] temporaryLocations = new AllocatableValue[destroyedRegisters.size()];
                int i = 0;
                for (Register reg : destroyedRegisters)
                {
                    temporaryLocations[i++] = reg.asValue();
                }
                temporaries = temporaryLocations;
            }
            address = code.getStart();
        }
    }

    @Override
    public long getAddress()
    {
        return address;
    }

    @Override
    public boolean destroysRegisters()
    {
        return effect == RegisterEffect.DESTROYS_REGISTERS;
    }

    @Override
    public boolean needsDebugInfo()
    {
        return transition == Transition.SAFEPOINT;
    }

    @Override
    public boolean mayContainFP()
    {
        return transition != Transition.LEAF_NOFP;
    }

    @Override
    public boolean needsJavaFrameAnchor()
    {
        if (transition == Transition.SAFEPOINT || transition == Transition.STACK_INSPECTABLE_LEAF)
        {
            if (stub != null)
            {
                // The stub will do the JavaFrameAnchor management
                // around the runtime call(s) it makes
                return false;
            }
            else
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSymbol()
    {
        return stub == null ? null : stub.toString();
    }
}
