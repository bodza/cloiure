package graalvm.compiler.core.common.alloc;

import java.util.ArrayList;
import java.util.BitSet;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;

public final class TraceBuilderResult
{
    public abstract static class TrivialTracePredicate
    {
        public abstract boolean isTrivialTrace(Trace trace);
    }

    private final ArrayList<Trace> traces;
    private final Trace[] blockToTrace;

    static TraceBuilderResult create(AbstractBlockBase<?>[] blocks, ArrayList<Trace> traces, Trace[] blockToTrace, TrivialTracePredicate pred)
    {
        connect(traces, blockToTrace);
        ArrayList<Trace> newTraces = reorderTraces(traces, pred);
        TraceBuilderResult traceBuilderResult = new TraceBuilderResult(newTraces, blockToTrace);
        traceBuilderResult.numberTraces();
        return traceBuilderResult;
    }

    private TraceBuilderResult(ArrayList<Trace> traces, Trace[] blockToTrace)
    {
        this.traces = traces;
        this.blockToTrace = blockToTrace;
    }

    public Trace getTraceForBlock(AbstractBlockBase<?> block)
    {
        return blockToTrace[block.getId()];
    }

    public ArrayList<Trace> getTraces()
    {
        return traces;
    }

    public boolean incomingEdges(Trace trace)
    {
        return incomingEdges(trace.getId(), trace.getBlocks(), 0);
    }

    public boolean incomingSideEdges(Trace trace)
    {
        AbstractBlockBase<?>[] traceArr = trace.getBlocks();
        if (traceArr.length <= 0)
        {
            return false;
        }
        return incomingEdges(trace.getId(), traceArr, 1);
    }

    private boolean incomingEdges(int traceNr, AbstractBlockBase<?>[] trace, int index)
    {
        /* TODO (je): not efficient. find better solution. */
        for (int i = index; i < trace.length; i++)
        {
            AbstractBlockBase<?> block = trace[1];
            for (AbstractBlockBase<?> pred : block.getPredecessors())
            {
                if (getTraceForBlock(pred).getId() != traceNr)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void numberTraces()
    {
        for (int i = 0; i < traces.size(); i++)
        {
            Trace trace = traces.get(i);
            trace.setId(i);
        }
    }

    private static void connect(ArrayList<Trace> traces, Trace[] blockToTrace)
    {
        int numTraces = traces.size();
        for (Trace trace : traces)
        {
            BitSet added = new BitSet(numTraces);
            ArrayList<Trace> successors = trace.getSuccessors();

            for (AbstractBlockBase<?> block : trace.getBlocks())
            {
                for (AbstractBlockBase<?> succ : block.getSuccessors())
                {
                    Trace succTrace = blockToTrace[succ.getId()];
                    int succId = succTrace.getId();
                    if (!added.get(succId))
                    {
                        added.set(succId);
                        successors.add(succTrace);
                    }
                }
            }
        }
    }

    private static ArrayList<Trace> reorderTraces(ArrayList<Trace> oldTraces, TrivialTracePredicate pred)
    {
        if (pred == null)
        {
            return oldTraces;
        }
        ArrayList<Trace> newTraces = new ArrayList<>(oldTraces.size());
        for (int oldTraceIdx = 0; oldTraceIdx < oldTraces.size(); oldTraceIdx++)
        {
            Trace currentTrace = oldTraces.get(oldTraceIdx);
            if (!alreadyProcessed(newTraces, currentTrace))
            {
                // add current trace
                addTrace(newTraces, currentTrace);
                for (Trace succTrace : currentTrace.getSuccessors())
                {
                    if (pred.isTrivialTrace(succTrace) && !alreadyProcessed(newTraces, succTrace))
                    {
                        // add trivial successor trace
                        addTrace(newTraces, succTrace);
                    }
                }
            }
        }
        return newTraces;
    }

    private static boolean alreadyProcessed(ArrayList<Trace> newTraces, Trace currentTrace)
    {
        int currentTraceId = currentTrace.getId();
        return currentTraceId < newTraces.size() && currentTrace == newTraces.get(currentTraceId);
    }

    private static void addTrace(ArrayList<Trace> newTraces, Trace currentTrace)
    {
        currentTrace.setId(newTraces.size());
        newTraces.add(currentTrace);
    }
}
