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
import giraaff.nodes.cfg.Block;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class LIRGenerationPhase
public final class LIRGenerationPhase extends LIRPhase<LIRGenerationPhase.LIRGenerationContext>
{
    // @class LIRGenerationPhase.LIRGenerationContext
    public static final class LIRGenerationContext
    {
        // @field
        private final NodeLIRBuilderTool ___nodeLirBuilder;
        // @field
        private final LIRGeneratorTool ___lirGen;
        // @field
        private final StructuredGraph ___graph;
        // @field
        private final StructuredGraph.ScheduleResult ___schedule;

        // @cons LIRGenerationPhase.LIRGenerationContext
        public LIRGenerationContext(LIRGeneratorTool __lirGen, NodeLIRBuilderTool __nodeLirBuilder, StructuredGraph __graph, StructuredGraph.ScheduleResult __schedule)
        {
            super();
            this.___nodeLirBuilder = __nodeLirBuilder;
            this.___lirGen = __lirGen;
            this.___graph = __graph;
            this.___schedule = __schedule;
        }
    }

    // @cons LIRGenerationPhase
    public LIRGenerationPhase()
    {
        super();
    }

    @Override
    protected final void run(TargetDescription __target, LIRGenerationResult __lirGenRes, LIRGenerationPhase.LIRGenerationContext __context)
    {
        NodeLIRBuilderTool __nodeLirBuilder = __context.___nodeLirBuilder;
        StructuredGraph __graph = __context.___graph;
        StructuredGraph.ScheduleResult __schedule = __context.___schedule;
        for (AbstractBlockBase<?> __b : __lirGenRes.getLIR().getControlFlowGraph().getBlocks())
        {
            emitBlock(__nodeLirBuilder, __lirGenRes, (Block) __b, __graph, __schedule.getBlockToNodesMap());
        }
        __context.___lirGen.beforeRegisterAllocation();
    }

    private static void emitBlock(NodeLIRBuilderTool __nodeLirGen, LIRGenerationResult __lirGenRes, Block __b, StructuredGraph __graph, BlockMap<List<Node>> __blockMap)
    {
        __nodeLirGen.doBlock(__b, __graph, __blockMap);
        LIR __lir = __lirGenRes.getLIR();
    }

    private static boolean isProcessed(LIRGenerationResult __lirGenRes, Block __b)
    {
        return __lirGenRes.getLIR().getLIRforBlock(__b) != null;
    }
}
