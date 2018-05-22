package graalvm.compiler.nodes.spi;

import java.util.Collection;
import java.util.List;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.BreakpointNode;
import graalvm.compiler.nodes.DeoptimizingNode;
import graalvm.compiler.nodes.FullInfopointNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.SafepointNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.ConditionalNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.extended.SwitchNode;
import graalvm.compiler.options.OptionValues;

public interface NodeLIRBuilderTool extends NodeValueMap
{
    // TODO (je) remove and move into the Node
    LIRFrameState state(DeoptimizingNode deopt);

    void emitIf(IfNode i);

    void emitConditional(ConditionalNode i);

    void emitSwitch(SwitchNode i);

    void emitInvoke(Invoke i);

    // Handling of block-end nodes still needs to be unified in the LIRGenerator.
    void visitMerge(AbstractMergeNode i);

    void visitEndNode(AbstractEndNode i);

    void visitLoopEnd(LoopEndNode i);

    // These methods define the contract a runtime specific backend must provide.

    void visitSafepointNode(SafepointNode i);

    void visitBreakpointNode(BreakpointNode i);

    void visitFullInfopointNode(FullInfopointNode i);

    LIRGeneratorTool getLIRGeneratorTool();

    void emitOverflowCheckBranch(AbstractBeginNode overflowSuccessor, AbstractBeginNode next, Stamp compareStamp, double probability);

    Value[] visitInvokeArguments(CallingConvention cc, Collection<ValueNode> arguments);

    void doBlock(Block block, StructuredGraph graph, BlockMap<List<Node>> blockMap);

    default OptionValues getOptions()
    {
        return getLIRGeneratorTool().getResult().getLIR().getOptions();
    }
}
