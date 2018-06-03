package giraaff.hotspot.meta;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
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
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.stubs.ForeignCallStub;
import giraaff.hotspot.stubs.Stub;
import giraaff.word.Word;
import giraaff.word.WordTypes;

///
// HotSpot implementation of {@link HotSpotForeignCallsProvider}.
///
// @class HotSpotForeignCallsProviderImpl
public abstract class HotSpotForeignCallsProviderImpl implements HotSpotForeignCallsProvider
{
    // @def
    public static final ForeignCallDescriptor OSR_MIGRATION_END = new ForeignCallDescriptor("OSR_migration_end", void.class, long.class);
    // @def
    public static final ForeignCallDescriptor IDENTITY_HASHCODE = new ForeignCallDescriptor("identity_hashcode", int.class, Object.class);
    // @def
    public static final ForeignCallDescriptor LOAD_AND_CLEAR_EXCEPTION = new ForeignCallDescriptor("load_and_clear_exception", Object.class, Word.class);

    // @field
    protected final HotSpotGraalRuntime ___runtime;

    // @field
    protected final EconomicMap<ForeignCallDescriptor, HotSpotForeignCallLinkage> ___foreignCalls = EconomicMap.create();
    // @field
    protected final MetaAccessProvider ___metaAccess;
    // @field
    protected final CodeCacheProvider ___codeCache;
    // @field
    protected final WordTypes ___wordTypes;

    // @cons
    public HotSpotForeignCallsProviderImpl(HotSpotGraalRuntime __runtime, MetaAccessProvider __metaAccess, CodeCacheProvider __codeCache, WordTypes __wordTypes)
    {
        super();
        this.___runtime = __runtime;
        this.___metaAccess = __metaAccess;
        this.___codeCache = __codeCache;
        this.___wordTypes = __wordTypes;
    }

    ///
    // Registers the linkage for a foreign call.
    ///
    public HotSpotForeignCallLinkage register(HotSpotForeignCallLinkage __linkage)
    {
        this.___foreignCalls.put(__linkage.getDescriptor(), __linkage);
        return __linkage;
    }

    ///
    // Return true if the descriptor has already been registered.
    ///
    public boolean isRegistered(ForeignCallDescriptor __descriptor)
    {
        return this.___foreignCalls.containsKey(__descriptor);
    }

    ///
    // Creates and registers the details for linking a foreign call to a {@link Stub}.
    //
    // @param descriptor the signature of the call to the stub
    // @param reexecutable specifies if the stub call can be re-executed without (meaningful) side
    //            effects. Deoptimization will not return to a point before a stub call that cannot
    //            be re-executed.
    // @param transition specifies if this is a {@linkplain Transition#LEAF leaf} call
    // @param killedLocations the memory locations killed by the stub call
    ///
    public HotSpotForeignCallLinkage registerStubCall(ForeignCallDescriptor __descriptor, boolean __reexecutable, Transition __transition, LocationIdentity... __killedLocations)
    {
        return register(HotSpotForeignCallLinkageImpl.create(this.___metaAccess, this.___codeCache, this.___wordTypes, this, __descriptor, 0L, RegisterEffect.PRESERVES_REGISTERS, HotSpotCallingConventionType.JavaCall, HotSpotCallingConventionType.JavaCallee, __transition, __reexecutable, __killedLocations));
    }

    ///
    // Creates and registers the linkage for a foreign call.
    //
    // @param descriptor the signature of the foreign call
    // @param address the address of the code to call
    // @param outgoingCcType outgoing (caller) calling convention type
    // @param effect specifies if the call destroys or preserves all registers (apart from
    //            temporaries which are always destroyed)
    // @param transition specifies if this is a {@linkplain Transition#LEAF leaf} call
    // @param reexecutable specifies if the foreign call can be re-executed without (meaningful)
    //            side effects. Deoptimization will not return to a point before a foreign call that
    //            cannot be re-executed.
    // @param killedLocations the memory locations killed by the foreign call
    ///
    public HotSpotForeignCallLinkage registerForeignCall(ForeignCallDescriptor __descriptor, long __address, CallingConvention.Type __outgoingCcType, RegisterEffect __effect, Transition __transition, boolean __reexecutable, LocationIdentity... __killedLocations)
    {
        Class<?> __resultType = __descriptor.getResultType();
        return register(HotSpotForeignCallLinkageImpl.create(this.___metaAccess, this.___codeCache, this.___wordTypes, this, __descriptor, __address, __effect, __outgoingCcType, null, __transition, __reexecutable, __killedLocations));
    }

    ///
    // Creates a {@linkplain ForeignCallStub stub} for a foreign call.
    //
    // @param descriptor the signature of the call to the stub
    // @param address the address of the foreign code to call
    // @param prependThread true if the JavaThread value for the current thread is to be prepended
    //            to the arguments for the call to {@code address}
    // @param transition specifies if this is a {@linkplain Transition#LEAF leaf} call
    // @param reexecutable specifies if the foreign call can be re-executed without (meaningful)
    //            side effects. Deoptimization will not return to a point before a foreign call that
    //            cannot be re-executed.
    // @param killedLocations the memory locations killed by the foreign call
    ///
    public void linkForeignCall(HotSpotProviders __providers, ForeignCallDescriptor __descriptor, long __address, boolean __prependThread, Transition __transition, boolean __reexecutable, LocationIdentity... __killedLocations)
    {
        ForeignCallStub __stub = new ForeignCallStub(__providers, __address, __descriptor, __prependThread, __transition, __reexecutable, __killedLocations);
        HotSpotForeignCallLinkage __linkage = __stub.getLinkage();
        HotSpotForeignCallLinkage __targetLinkage = __stub.getTargetLinkage();
        __linkage.setCompiledStub(__stub);
        register(__linkage);
        register(__targetLinkage);
    }

    // @def
    public static final boolean PREPEND_THREAD = true;
    // @def
    public static final boolean DONT_PREPEND_THREAD = !PREPEND_THREAD;

    // @def
    public static final boolean REEXECUTABLE = true;
    // @def
    public static final boolean NOT_REEXECUTABLE = !REEXECUTABLE;

    // @def
    public static final LocationIdentity[] NO_LOCATIONS = {};

    @Override
    public HotSpotForeignCallLinkage lookupForeignCall(ForeignCallDescriptor __descriptor)
    {
        HotSpotForeignCallLinkage __callTarget = this.___foreignCalls.get(__descriptor);
        __callTarget.finalizeAddress(this.___runtime.getBackend());
        return __callTarget;
    }

    @Override
    public boolean isReexecutable(ForeignCallDescriptor __descriptor)
    {
        return this.___foreignCalls.get(__descriptor).isReexecutable();
    }

    @Override
    public boolean canDeoptimize(ForeignCallDescriptor __descriptor)
    {
        return this.___foreignCalls.get(__descriptor).needsDebugInfo();
    }

    @Override
    public boolean isGuaranteedSafepoint(ForeignCallDescriptor __descriptor)
    {
        return this.___foreignCalls.get(__descriptor).isGuaranteedSafepoint();
    }

    @Override
    public LocationIdentity[] getKilledLocations(ForeignCallDescriptor __descriptor)
    {
        return this.___foreignCalls.get(__descriptor).getKilledLocations();
    }

    @Override
    public LIRKind getValueKind(JavaKind __javaKind)
    {
        return LIRKind.fromJavaKind(this.___codeCache.getTarget().arch, __javaKind);
    }

    @Override
    public List<Stub> getStubs()
    {
        List<Stub> __stubs = new ArrayList<>();
        for (HotSpotForeignCallLinkage __linkage : this.___foreignCalls.getValues())
        {
            if (__linkage.isCompiledStub())
            {
                Stub __stub = __linkage.getStub();
                __stubs.add(__stub);
            }
        }
        return __stubs;
    }
}
