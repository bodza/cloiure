package giraaff.hotspot.amd64;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.meta.DefaultHotSpotLoweringProvider;
import giraaff.hotspot.meta.HotSpotRegistersProvider;

// @class AMD64HotSpotLoweringProvider
public final class AMD64HotSpotLoweringProvider extends DefaultHotSpotLoweringProvider
{
    // @cons
    public AMD64HotSpotLoweringProvider(HotSpotGraalRuntime __runtime, MetaAccessProvider __metaAccess, ForeignCallsProvider __foreignCalls, HotSpotRegistersProvider __registers, HotSpotConstantReflectionProvider __constantReflection, TargetDescription __target)
    {
        super(__runtime, __metaAccess, __foreignCalls, __registers, __constantReflection, __target);
    }

    @Override
    public Integer smallestCompareWidth()
    {
        return 8;
    }
}
