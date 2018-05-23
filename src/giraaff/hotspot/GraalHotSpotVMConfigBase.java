package giraaff.hotspot;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotVMConfigAccess;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;

import giraaff.api.replacements.Fold;
import giraaff.api.replacements.Fold.InjectedParameter;

/**
 * This is a source with different versions for various JDKs.
 */
public abstract class GraalHotSpotVMConfigBase extends HotSpotVMConfigAccess
{
    /**
     * Sentinel value to use for an {@linkplain InjectedParameter injected}
     * {@link GraalHotSpotVMConfig} parameter to a {@linkplain Fold foldable} method.
     */
    public static final GraalHotSpotVMConfig INJECTED_VMCONFIG = null;

    GraalHotSpotVMConfigBase(HotSpotVMConfigStore store)
    {
        super(store);
    }
}
