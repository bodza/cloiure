package graalvm.compiler.core.common.alloc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.PriorityQueue;

import graalvm.compiler.core.common.alloc.TraceBuilderResult.TrivialTracePredicate;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;

/**
 * Computes traces by starting at a trace head and keep adding successors as long as possible.
 */
public final class UniDirectionalTraceBuilder
{
    public static TraceBuilderResult computeTraces(DebugContext debug, AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] blocks, TrivialTracePredicate pred)
    {
        return new UniDirectionalTraceBuilder(blocks).build(debug, startBlock, blocks, pred);
    }

    private final PriorityQueue<AbstractBlockBase<?>> worklist;
    private final BitSet processed;
    /**
     * Contains the number of unprocessed predecessors for every {@link AbstractBlockBase#getId()
     * block}.
     */
    private final int[] blocked;
    private final Trace[] blockToTrace;

    private UniDirectionalTraceBuilder(AbstractBlockBase<?>[] blocks)
    {
        processed = new BitSet(blocks.length);
        worklist = new PriorityQueue<>(UniDirectionalTraceBuilder::compare);
        assert (worklist != null);

        blocked = new int[blocks.length];
        blockToTrace = new Trace[blocks.length];
        for (AbstractBlockBase<?> block : blocks)
        {
            blocked[block.getId()] = block.getPredecessorCount();
        }
    }

    private static int compare(AbstractBlockBase<?> a, AbstractBlockBase<?> b)
    {
        return Double.compare(b.probability(), a.probability());
    }

    private boolean processed(AbstractBlockBase<?> b)
    {
        return processed.get(b.getId());
    }

    @SuppressWarnings("try")
    private TraceBuilderResult build(DebugContext debug, AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] blocks, TrivialTracePredicate pred)
    {
        try (Indent indent = debug.logAndIndent("UniDirectionalTraceBuilder: start trace building: %s", startBlock))
        {
            ArrayList<Trace> traces = buildTraces(debug, startBlock);
            return TraceBuilderResult.create(debug, blocks, traces, blockToTrace, pred);
        }
    }

    protected ArrayList<Trace> buildTraces(DebugContext debug, AbstractBlockBase<?> startBlock)
    {
        ArrayList<Trace> traces = new ArrayList<>();
        // add start block
        worklist.add(startBlock);
        // process worklist
        while (!worklist.isEmpty())
        {
            AbstractBlockBase<?> block = worklist.poll();
            assert block != null;
            if (!processed(block))
            {
                Trace trace = new Trace(findTrace(debug, block));
                for (AbstractBlockBase<?> traceBlock : trace.getBlocks())
                {
                    blockToTrace[traceBlock.getId()] = trace;
                }
                trace.setId(traces.size());
                traces.add(trace);
            }
        }
        return traces;
    }

    /**
     * Build a new trace starting at {@code block}.
     */
    @SuppressWarnings("try")
    private List<AbstractBlockBase<?>> findTrace(DebugContext debug, AbstractBlockBase<?> traceStart)
    {
        assert checkPredecessorsProcessed(traceStart);
        ArrayList<AbstractBlockBase<?>> trace = new ArrayList<>();
        int blockNumber = 0;
        try (Indent i = debug.logAndIndent("StartTrace: %s", traceStart))
        {
            for (AbstractBlockBase<?> block = traceStart; block != null; block = selectNext(block))
            {
                debug.log("add %s (prob: %f)", block, block.probability());
                processed.set(block.getId());
                trace.add(block);
                unblock(block);
                block.setLinearScanNumber(blockNumber++);
            }
        }
        return trace;
    }

    private boolean checkPredecessorsProcessed(AbstractBlockBase<?> block)
    {
        for (AbstractBlockBase<?> pred : block.getPredecessors())
        {
            assert processed(pred) : "Predecessor unscheduled: " + pred;
        }
        return true;
    }

    /**
     * Decrease the {@link #blocked} count for all predecessors and add them to the worklist once
     * the count reaches 0.
     */
    private void unblock(AbstractBlockBase<?> block)
    {
        for (AbstractBlockBase<?> successor : block.getSuccessors())
        {
            if (!processed(successor))
            {
                int blockCount = --blocked[successor.getId()];
                assert blockCount >= 0;
                if (blockCount == 0)
                {
                    worklist.add(successor);
                }
            }
        }
    }

    /**
     * @return The unprocessed predecessor with the highest probability, or {@code null}.
     */
    private AbstractBlockBase<?> selectNext(AbstractBlockBase<?> block)
    {
        AbstractBlockBase<?> next = null;
        for (AbstractBlockBase<?> successor : block.getSuccessors())
        {
            if (!processed(successor) && (next == null || successor.probability() > next.probability()))
            {
                next = successor;
            }
        }
        return next;
    }
}
