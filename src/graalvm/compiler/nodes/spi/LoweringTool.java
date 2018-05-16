package graalvm.compiler.nodes.spi;

import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.extended.AnchoringNode;
import graalvm.compiler.nodes.extended.GuardingNode;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;

public interface LoweringTool
{
    MetaAccessProvider getMetaAccess();

    LoweringProvider getLowerer();

    ConstantReflectionProvider getConstantReflection();

    ConstantFieldProvider getConstantFieldProvider();

    Replacements getReplacements();

    StampProvider getStampProvider();

    GuardingNode createGuard(FixedNode before, LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action);

    GuardingNode createGuard(FixedNode before, LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action, JavaConstant speculation, boolean negated, NodeSourcePosition noDeoptSuccessorPosition);

    /**
     * Gets the closest fixed node preceding the node currently being lowered.
     */
    FixedWithNextNode lastFixedNode();

    AnchoringNode getCurrentGuardAnchor();

    /**
     * Marker interface lowering stages.
     */
    interface LoweringStage
    {
    }

    /**
     * The lowering stages used in a standard Graal phase plan. Lowering is called 3 times, during
     * every tier of compilation.
     */
    enum StandardLoweringStage implements LoweringStage
    {
        HIGH_TIER,
        MID_TIER,
        LOW_TIER
    }

    /**
     * Returns current lowering stage.
     */
    LoweringStage getLoweringStage();
}
