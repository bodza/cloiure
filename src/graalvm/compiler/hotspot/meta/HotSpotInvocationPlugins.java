package graalvm.compiler.hotspot.meta;

import java.lang.reflect.Type;

import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.phases.tiers.CompilerConfiguration;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.ResolvedJavaType;

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
