package giraaff.loop;

import java.util.List;

import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.cfg.ControlFlowGraph;

// @iface LoopPolicies
public interface LoopPolicies
{
    boolean shouldPeel(LoopEx loop, ControlFlowGraph cfg, MetaAccessProvider metaAccess);

    boolean shouldFullUnroll(LoopEx loop);

    boolean shouldPartiallyUnroll(LoopEx loop);

    boolean shouldTryUnswitch(LoopEx loop);

    boolean shouldUnswitch(LoopEx loop, List<ControlSplitNode> controlSplits);
}
