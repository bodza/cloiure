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

    GuardingNode createGuard(FixedNode __before, LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action);

    GuardingNode createGuard(FixedNode __before, LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action, JavaConstant __speculation, boolean __negated);

    ///
    // Gets the closest fixed node preceding the node currently being lowered.
    ///
    FixedWithNextNode lastFixedNode();

    AnchoringNode getCurrentGuardAnchor();

    ///
    // Marker interface lowering stages.
    ///
    // @iface LoweringTool.LoweringStage
    interface LoweringStage
    {
    }

    ///
    // The lowering stages used in a standard Graal phase plan. Lowering is called 3 times,
    // during every tier of compilation.
    ///
    // @enum LoweringTool.StandardLoweringStage
    enum StandardLoweringStage implements LoweringTool.LoweringStage
    {
        HIGH_TIER,
        MID_TIER,
        LOW_TIER
    }

    ///
    // Returns current lowering stage.
    ///
    LoweringTool.LoweringStage getLoweringStage();
}
