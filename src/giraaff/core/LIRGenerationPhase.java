package giraaff.core;

import java.util.List;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.BlockMap;
import giraaff.graph.Node;
import giraaff.lir.LIR;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.phases.LIRPhase;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.spi.NodeLIRBuilderTool;

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
