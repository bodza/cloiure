package graalvm.compiler.loop;

import java.util.List;

import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;

import jdk.vm.ci.meta.MetaAccessProvider;

public interface LoopPolicies
{
    boolean shouldPeel(LoopEx loop, ControlFlowGraph cfg, MetaAccessProvider metaAccess);

    boolean shouldFullUnroll(LoopEx loop);

    boolean shouldPartiallyUnroll(LoopEx loop);

    boolean shouldTryUnswitch(LoopEx loop);

    boolean shouldUnswitch(LoopEx loop, List<ControlSplitNode> controlSplits);
}
