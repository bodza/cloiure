package giraaff.nodes.spi;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.extended.AnchoringNode;
import giraaff.nodes.extended.GuardingNode;

// @iface LoweringTool
public interface LoweringTool
{
    MetaAccessProvider getMetaAccess();

    LoweringProvider getLowerer();

    ConstantReflectionProvider getConstantReflection();

    ConstantFieldProvider getConstantFieldProvider();

    Replacements getReplacements();

    StampProvider getStampProvider();

    GuardingNode createGuard(FixedNode before, LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action);

    GuardingNode createGuard(FixedNode before, LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action, JavaConstant speculation, boolean negated);

    /**
     * Gets the closest fixed node preceding the node currently being lowered.
     */
    FixedWithNextNode lastFixedNode();

    AnchoringNode getCurrentGuardAnchor();

    /**
     * Marker interface lowering stages.
     */
    // @iface LoweringTool.LoweringStage
    interface LoweringStage
    {
    }

    /**
     * The lowering stages used in a standard Graal phase plan. Lowering is called 3 times, during
     * every tier of compilation.
     */
    // @enum LoweringTool.StandardLoweringStage implements LoweringStage
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
