package graalvm.compiler.core.common.alloc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;

import graalvm.compiler.core.common.alloc.TraceBuilderResult.TrivialTracePredicate;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;

/**
 * Computes traces by selecting the unhandled block with the highest execution frequency and going
 * in both directions, up and down, as long as possible.
 */
public final class BiDirectionalTraceBuilder
{
    public static TraceBuilderResult computeTraces(AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] blocks, TrivialTracePredicate pred)
    {
        return new BiDirectionalTraceBuilder(blocks).build(startBlock, blocks, pred);
    }

    private final Deque<AbstractBlockBase<?>> worklist;
    private final BitSet processed;
    private final Trace[] blockToTrace;

    private BiDirectionalTraceBuilder(AbstractBlockBase<?>[] blocks)
    {
        processed = new BitSet(blocks.length);
        worklist = createQueue(blocks);
        blockToTrace = new Trace[blocks.length];
    }

    private static Deque<AbstractBlockBase<?>> createQueue(AbstractBlockBase<?>[] blocks)
    {
        ArrayList<AbstractBlockBase<?>> queue = new ArrayList<>(Arrays.asList(blocks));
        queue.sort(BiDirectionalTraceBuilder::compare);
        return new ArrayDeque<>(queue);
    }

    private static int compare(AbstractBlockBase<?> a, AbstractBlockBase<?> b)
    {
        return Double.compare(b.probability(), a.probability());
    }

    private boolean processed(AbstractBlockBase<?> b)
    {
        return processed.get(b.getId());
    }

    private TraceBuilderResult build(AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] blocks, TrivialTracePredicate pred)
    {
        ArrayList<Trace> traces = buildTraces();
        return TraceBuilderResult.create(blocks, traces, blockToTrace, pred);
    }

    protected ArrayList<Trace> buildTraces()
    {
        ArrayList<Trace> traces = new ArrayList<>();
        // process worklist
        while (!worklist.isEmpty())
        {
            AbstractBlockBase<?> block = worklist.pollFirst();
            if (!processed(block))
            {
                Trace trace = new Trace(findTrace(block));
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
    private Collection<AbstractBlockBase<?>> findTrace(AbstractBlockBase<?> initBlock)
    {
        ArrayDeque<AbstractBlockBase<?>> trace = new ArrayDeque<>();
        for (AbstractBlockBase<?> block = initBlock; block != null; block = selectPredecessor(block))
        {
            addBlockToTrace(block);
            trace.addFirst(block);
        }
        /* Number head blocks. Can not do this in the loop as we go backwards. */
        int blockNr = 0;
        for (AbstractBlockBase<?> b : trace)
        {
            b.setLinearScanNumber(blockNr++);
        }

        for (AbstractBlockBase<?> block = selectSuccessor(initBlock); block != null; block = selectSuccessor(block))
        {
            addBlockToTrace(block);
            trace.addLast(block);
            /* This time we can number the blocks immediately as we go forwards. */
            block.setLinearScanNumber(blockNr++);
        }
        return trace;
    }

    private void addBlockToTrace(AbstractBlockBase<?> block)
    {
        processed.set(block.getId());
    }

    /**
     * @return The unprocessed predecessor with the highest probability, or {@code null}.
     */
    private AbstractBlockBase<?> selectPredecessor(AbstractBlockBase<?> block)
    {
        AbstractBlockBase<?> next = null;
        for (AbstractBlockBase<?> pred : block.getPredecessors())
        {
            if (!processed(pred) && !isBackEdge(pred, block) && (next == null || pred.probability() > next.probability()))
            {
                next = pred;
            }
        }
        return next;
    }

    private static boolean isBackEdge(AbstractBlockBase<?> from, AbstractBlockBase<?> to)
    {
        return from.isLoopEnd() && to.isLoopHeader() && from.getLoop().equals(to.getLoop());
    }

    /**
     * @return The unprocessed successor with the highest probability, or {@code null}.
     */
    private AbstractBlockBase<?> selectSuccessor(AbstractBlockBase<?> block)
    {
        AbstractBlockBase<?> next = null;
        for (AbstractBlockBase<?> succ : block.getSuccessors())
        {
            if (!processed(succ) && (next == null || succ.probability() > next.probability()))
            {
                next = succ;
            }
        }
        return next;
    }
}
