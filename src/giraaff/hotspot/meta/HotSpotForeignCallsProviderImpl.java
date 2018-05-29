package giraaff.hotspot.meta;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.collections.EconomicMap;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.HotSpotForeignCallLinkageImpl;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.stubs.ForeignCallStub;
import giraaff.hotspot.stubs.Stub;
import giraaff.options.OptionValues;
import giraaff.word.Word;
import giraaff.word.WordTypes;

/**
 * HotSpot implementation of {@link HotSpotForeignCallsProvider}.
 */
// @class HotSpotForeignCallsProviderImpl
public abstract class HotSpotForeignCallsProviderImpl implements HotSpotForeignCallsProvider
{
    public static final ForeignCallDescriptor OSR_MIGRATION_END = new ForeignCallDescriptor("OSR_migration_end", void.class, long.class);
    public static final ForeignCallDescriptor IDENTITY_HASHCODE = new ForeignCallDescriptor("identity_hashcode", int.class, Object.class);
    public static final ForeignCallDescriptor LOAD_AND_CLEAR_EXCEPTION = new ForeignCallDescriptor("load_and_clear_exception", Object.class, Word.class);

    protected final HotSpotJVMCIRuntimeProvider jvmciRuntime;
    protected final HotSpotGraalRuntimeProvider runtime;

    protected final EconomicMap<ForeignCallDescriptor, HotSpotForeignCallLinkage> foreignCalls = EconomicMap.create();
    protected final MetaAccessProvider metaAccess;
    protected final CodeCacheProvider codeCache;
    protected final WordTypes wordTypes;

    // @cons
    public HotSpotForeignCallsProviderImpl(HotSpotJVMCIRuntimeProvider jvmciRuntime, HotSpotGraalRuntimeProvider runtime, MetaAccessProvider metaAccess, CodeCacheProvider codeCache, WordTypes wordTypes)
    {
        super();
        this.jvmciRuntime = jvmciRuntime;
        this.runtime = runtime;
        this.metaAccess = metaAccess;
        this.codeCache = codeCache;
        this.wordTypes = wordTypes;
    }

    /**
     * Registers the linkage for a foreign call.
     */
    public HotSpotForeignCallLinkage register(HotSpotForeignCallLinkage linkage)
    {
        foreignCalls.put(linkage.getDescriptor(), linkage);
        return linkage;
    }

    /**
     * Return true if the descriptor has already been registered.
     */
    public boolean isRegistered(ForeignCallDescriptor descriptor)
    {
        return foreignCalls.containsKey(descriptor);
    }

    /**
     * Creates and registers the details for linking a foreign call to a {@link Stub}.
     *
     * @param descriptor the signature of the call to the stub
     * @param reexecutable specifies if the stub call can be re-executed without (meaningful) side
     *            effects. Deoptimization will not return to a point before a stub call that cannot
     *            be re-executed.
     * @param transition specifies if this is a {@linkplain Transition#LEAF leaf} call
     * @param killedLocations the memory locations killed by the stub call
     */
    public HotSpotForeignCallLinkage registerStubCall(ForeignCallDescriptor descriptor, boolean reexecutable, Transition transition, LocationIdentity... killedLocations)
    {
        return register(HotSpotForeignCallLinkageImpl.create(metaAccess, codeCache, wordTypes, this, descriptor, 0L, RegisterEffect.PRESERVES_REGISTERS, HotSpotCallingConventionType.JavaCall, HotSpotCallingConventionType.JavaCallee, transition, reexecutable, killedLocations));
    }

    /**
     * Creates and registers the linkage for a foreign call.
     *
     * @param descriptor the signature of the foreign call
     * @param address the address of the code to call
     * @param outgoingCcType outgoing (caller) calling convention type
     * @param effect specifies if the call destroys or preserves all registers (apart from
     *            temporaries which are always destroyed)
     * @param transition specifies if this is a {@linkplain Transition#LEAF leaf} call
     * @param reexecutable specifies if the foreign call can be re-executed without (meaningful)
     *            side effects. Deoptimization will not return to a point before a foreign call that
     *            cannot be re-executed.
     * @param killedLocations the memory locations killed by the foreign call
     */
    public HotSpotForeignCallLinkage registerForeignCall(ForeignCallDescriptor descriptor, long address, CallingConvention.Type outgoingCcType, RegisterEffect effect, Transition transition, boolean reexecutable, LocationIdentity... killedLocations)
    {
        Class<?> resultType = descriptor.getResultType();
        return register(HotSpotForeignCallLinkageImpl.create(metaAccess, codeCache, wordTypes, this, descriptor, address, effect, outgoingCcType, null, transition, reexecutable, killedLocations));
    }

    /**
     * Creates a {@linkplain ForeignCallStub stub} for a foreign call.
     *
     * @param descriptor the signature of the call to the stub
     * @param address the address of the foreign code to call
     * @param prependThread true if the JavaThread value for the current thread is to be prepended
     *            to the arguments for the call to {@code address}
     * @param transition specifies if this is a {@linkplain Transition#LEAF leaf} call
     * @param reexecutable specifies if the foreign call can be re-executed without (meaningful)
     *            side effects. Deoptimization will not return to a point before a foreign call that
     *            cannot be re-executed.
     * @param killedLocations the memory locations killed by the foreign call
     */
    public void linkForeignCall(OptionValues options, HotSpotProviders providers, ForeignCallDescriptor descriptor, long address, boolean prependThread, Transition transition, boolean reexecutable, LocationIdentity... killedLocations)
    {
        ForeignCallStub stub = new ForeignCallStub(options, jvmciRuntime, providers, address, descriptor, prependThread, transition, reexecutable, killedLocations);
        HotSpotForeignCallLinkage linkage = stub.getLinkage();
        HotSpotForeignCallLinkage targetLinkage = stub.getTargetLinkage();
        linkage.setCompiledStub(stub);
        register(linkage);
        register(targetLinkage);
    }

    public static final boolean PREPEND_THREAD = true;
    public static final boolean DONT_PREPEND_THREAD = !PREPEND_THREAD;

    public static final boolean REEXECUTABLE = true;
    public static final boolean NOT_REEXECUTABLE = !REEXECUTABLE;

    public static final LocationIdentity[] NO_LOCATIONS = {};

    @Override
    public HotSpotForeignCallLinkage lookupForeignCall(ForeignCallDescriptor descriptor)
    {
        HotSpotForeignCallLinkage callTarget = foreignCalls.get(descriptor);
        callTarget.finalizeAddress(runtime.getBackend());
        return callTarget;
    }

    @Override
    public boolean isReexecutable(ForeignCallDescriptor descriptor)
    {
        return foreignCalls.get(descriptor).isReexecutable();
    }

    @Override
    public boolean canDeoptimize(ForeignCallDescriptor descriptor)
    {
        return foreignCalls.get(descriptor).needsDebugInfo();
    }

    @Override
    public boolean isGuaranteedSafepoint(ForeignCallDescriptor descriptor)
    {
        return foreignCalls.get(descriptor).isGuaranteedSafepoint();
    }

    @Override
    public LocationIdentity[] getKilledLocations(ForeignCallDescriptor descriptor)
    {
        return foreignCalls.get(descriptor).getKilledLocations();
    }

    @Override
    public LIRKind getValueKind(JavaKind javaKind)
    {
        return LIRKind.fromJavaKind(codeCache.getTarget().arch, javaKind);
    }

    @Override
    public List<Stub> getStubs()
    {
        List<Stub> stubs = new ArrayList<>();
        for (HotSpotForeignCallLinkage linkage : foreignCalls.getValues())
        {
            if (linkage.isCompiledStub())
            {
                Stub stub = linkage.getStub();
                stubs.add(stub);
            }
        }
        return stubs;
    }
}
