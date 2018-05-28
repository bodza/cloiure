package giraaff.hotspot.meta;

import java.lang.reflect.Type;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;

/**
 * Extension of {@link InvocationPlugins} that disables plugins based on runtime configuration.
 */
final class HotSpotInvocationPlugins extends InvocationPlugins
{
    HotSpotInvocationPlugins()
    {
    }

    @Override
    public void register(InvocationPlugin plugin, Type declaringClass, String name, Type... argumentTypes)
    {
        if (!GraalHotSpotVMConfig.usePopCountInstruction)
        {
            if (name.equals("bitCount"))
            {
                return;
            }
        }
        super.register(plugin, declaringClass, name, argumentTypes);
    }

    @Override
    public boolean canBeIntrinsified(ResolvedJavaType declaringClass)
    {
        if (declaringClass instanceof HotSpotResolvedJavaType)
        {
            return true;
        }
        return false;
    }
}
