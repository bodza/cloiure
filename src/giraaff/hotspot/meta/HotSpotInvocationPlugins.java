package giraaff.hotspot.meta;

import java.lang.reflect.Type;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.hotspot.HotSpotRuntime;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;

///
// Extension of {@link InvocationPlugins} that disables plugins based on runtime configuration.
///
// @class HotSpotInvocationPlugins
final class HotSpotInvocationPlugins extends InvocationPlugins
{
    // @cons
    HotSpotInvocationPlugins()
    {
        super();
    }

    @Override
    public void register(InvocationPlugin __plugin, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        if (!HotSpotRuntime.usePopCountInstruction)
        {
            if (__name.equals("bitCount"))
            {
                return;
            }
        }
        super.register(__plugin, __declaringClass, __name, __argumentTypes);
    }

    @Override
    public boolean canBeIntrinsified(ResolvedJavaType __declaringClass)
    {
        if (__declaringClass instanceof HotSpotResolvedJavaType)
        {
            return true;
        }
        return false;
    }
}
