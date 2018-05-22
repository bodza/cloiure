package giraaff.hotspot.meta;

import java.lang.reflect.Type;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.nodes.type.StampTool;
import giraaff.phases.tiers.CompilerConfiguration;

/**
 * Extension of {@link InvocationPlugins} that disables plugins based on runtime configuration.
 */
final class HotSpotInvocationPlugins extends InvocationPlugins
{
    private final GraalHotSpotVMConfig config;
    private final IntrinsificationPredicate intrinsificationPredicate;

    HotSpotInvocationPlugins(GraalHotSpotVMConfig config, CompilerConfiguration compilerConfiguration)
    {
        this.config = config;
        intrinsificationPredicate = new IntrinsificationPredicate(compilerConfiguration);
    }

    @Override
    public void register(InvocationPlugin plugin, Type declaringClass, String name, Type... argumentTypes)
    {
        if (!config.usePopCountInstruction)
        {
            if (name.equals("bitCount"))
            {
                return;
            }
        }
        super.register(plugin, declaringClass, name, argumentTypes);
    }

    private static boolean isClass(ConstantNode node)
    {
        ResolvedJavaType type = StampTool.typeOrNull(node);
        return type != null && "Ljava/lang/Class;".equals(type.getName());
    }

    @Override
    public boolean canBeIntrinsified(ResolvedJavaType declaringClass)
    {
        if (declaringClass instanceof HotSpotResolvedJavaType)
        {
            HotSpotResolvedJavaType type = (HotSpotResolvedJavaType) declaringClass;
            return intrinsificationPredicate.apply(type.mirror());
        }
        return false;
    }
}
