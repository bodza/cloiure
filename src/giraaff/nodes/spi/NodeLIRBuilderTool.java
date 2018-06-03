package giraaff.nodes.spi;

import java.util.Collection;
import java.util.List;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.BlockMap;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.lir.LIRFrameState;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.SafepointNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.extended.SwitchNode;

// @iface NodeLIRBuilderTool
public interface NodeLIRBuilderTool extends NodeValueMap
{
    // TODO remove and move into the Node
    LIRFrameState state(DeoptimizingNode __deopt);

    void emitIf(IfNode __i);

    void emitConditional(ConditionalNode __i);

    void emitSwitch(SwitchNode __i);

    void emitInvoke(Invoke __i);

    // Handling of block-end nodes still needs to be unified in the LIRGenerator.
    void visitMerge(AbstractMergeNode __i);

    void visitEndNode(AbstractEndNode __i);

    void visitLoopEnd(LoopEndNode __i);

    // These methods define the contract a runtime specific backend must provide.

    void visitSafepointNode(SafepointNode __i);

    LIRGeneratorTool getLIRGeneratorTool();

    void emitOverflowCheckBranch(AbstractBeginNode __overflowSuccessor, AbstractBeginNode __next, Stamp __compareStamp, double __probability);

    Value[] visitInvokeArguments(CallingConvention __cc, Collection<ValueNode> __arguments);

    void doBlock(Block __block, StructuredGraph __graph, BlockMap<List<Node>> __blockMap);
}
