package graalvm.compiler.hotspot.meta;

import java.lang.reflect.Type;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.phases.AheadOfTimeVerificationPhase;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.replacements.nodes.MacroNode;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.JavaKind;
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
                assert declaringClass.equals(Integer.class) || declaringClass.equals(Long.class);
                return;
            }
        }
        super.register(plugin, declaringClass, name, argumentTypes);
    }

    @Override
    public void checkNewNodes(GraphBuilderContext b, InvocationPlugin plugin, NodeIterable<Node> newNodes)
    {
        for (Node node : newNodes)
        {
            if (node instanceof MacroNode)
            {
                // MacroNode based plugins can only be used for inlining since they
                // require a valid bci should they need to replace themselves with
                // an InvokeNode during lowering.
                assert plugin.inlineOnly() : String.format("plugin that creates a %s (%s) must return true for inlineOnly(): %s", MacroNode.class.getSimpleName(), node, plugin);
            }
        }
        if (GraalOptions.ImmutableCode.getValue(b.getOptions()))
        {
            for (Node node : newNodes)
            {
                if (node.hasUsages() && node instanceof ConstantNode)
                {
                    ConstantNode c = (ConstantNode) node;
                    if (c.getStackKind() == JavaKind.Object && AheadOfTimeVerificationPhase.isIllegalObjectConstant(c))
                    {
                        if (isClass(c))
                        {
                            // This will be handled later by LoadJavaMirrorWithKlassPhase
                        }
                        else
                        {
                            // Tolerate uses in unused FrameStates
                            if (node.usages().filter((n) -> !(n instanceof FrameState) || n.hasUsages()).isNotEmpty())
                            {
                                throw new AssertionError("illegal constant node in AOT: " + node);
                            }
                        }
                    }
                }
            }
        }
        super.checkNewNodes(b, plugin, newNodes);
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
