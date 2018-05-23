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
import giraaff.nodes.FullInfopointNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.SafepointNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.extended.SwitchNode;
import giraaff.options.OptionValues;

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
