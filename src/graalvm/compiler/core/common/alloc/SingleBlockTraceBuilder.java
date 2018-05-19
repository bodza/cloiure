package graalvm.compiler.core.common.alloc;

import java.util.ArrayList;

import graalvm.compiler.core.common.alloc.TraceBuilderResult.TrivialTracePredicate;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;

/**
 * Builds traces consisting of a single basic block.
 */
public final class SingleBlockTraceBuilder
{
    public static TraceBuilderResult computeTraces(AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] blocks, TrivialTracePredicate pred)
    {
        return build(startBlock, blocks, pred);
    }

    private static TraceBuilderResult build(AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] blocks, TrivialTracePredicate pred)
    {
        Trace[] blockToTrace = new Trace[blocks.length];
        ArrayList<Trace> traces = new ArrayList<>(blocks.length);

        for (AbstractBlockBase<?> block : blocks)
        {
            Trace trace = new Trace(new AbstractBlockBase<?>[]{block});
            blockToTrace[block.getId()] = trace;
            block.setLinearScanNumber(0);
            trace.setId(traces.size());
            traces.add(trace);
        }

        return TraceBuilderResult.create(blocks, traces, blockToTrace, pred);
    }
}
