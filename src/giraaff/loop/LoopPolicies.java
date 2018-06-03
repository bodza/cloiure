package giraaff.loop;

import java.util.List;

import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.cfg.ControlFlowGraph;

// @iface LoopPolicies
public interface LoopPolicies
{
    boolean shouldPeel(LoopEx __loop, ControlFlowGraph __cfg, MetaAccessProvider __metaAccess);

    boolean shouldFullUnroll(LoopEx __loop);

    boolean shouldPartiallyUnroll(LoopEx __loop);

    boolean shouldTryUnswitch(LoopEx __loop);

    boolean shouldUnswitch(LoopEx __loop, List<ControlSplitNode> __controlSplits);
}
