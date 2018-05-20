package graalvm.compiler.core;

import java.util.List;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.graph.Node;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.lir.phases.LIRPhase;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.TargetDescription;

public class LIRGenerationPhase extends LIRPhase<LIRGenerationPhase.LIRGenerationContext>
{
    public static final class LIRGenerationContext
    {
        private final NodeLIRBuilderTool nodeLirBuilder;
        private final LIRGeneratorTool lirGen;
        private final StructuredGraph graph;
        private final ScheduleResult schedule;

        public LIRGenerationContext(LIRGeneratorTool lirGen, NodeLIRBuilderTool nodeLirBuilder, StructuredGraph graph, ScheduleResult schedule)
        {
            this.nodeLirBuilder = nodeLirBuilder;
            this.lirGen = lirGen;
            this.graph = graph;
            this.schedule = schedule;
        }
    }

    @Override
    protected final void run(TargetDescription target, LIRGenerationResult lirGenRes, LIRGenerationPhase.LIRGenerationContext context)
    {
        NodeLIRBuilderTool nodeLirBuilder = context.nodeLirBuilder;
        StructuredGraph graph = context.graph;
        ScheduleResult schedule = context.schedule;
        for (AbstractBlockBase<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks())
        {
            emitBlock(nodeLirBuilder, lirGenRes, (Block) b, graph, schedule.getBlockToNodesMap());
        }
        context.lirGen.beforeRegisterAllocation();
    }

    private static void emitBlock(NodeLIRBuilderTool nodeLirGen, LIRGenerationResult lirGenRes, Block b, StructuredGraph graph, BlockMap<List<Node>> blockMap)
    {
        nodeLirGen.doBlock(b, graph, blockMap);
        LIR lir = lirGenRes.getLIR();
    }

    private static boolean isProcessed(LIRGenerationResult lirGenRes, Block b)
    {
        return lirGenRes.getLIR().getLIRforBlock(b) != null;
    }
}
