package graalvm.compiler.phases;

import graalvm.compiler.graph.Graph.Mark;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.contract.NodeCostUtil;
import graalvm.compiler.phases.contract.PhaseSizeContract;

/**
 * Base class for all compiler phases. Subclasses should be stateless. There will be one global
 * instance for each compiler phase that is shared for all compilations. VM-, target- and
 * compilation-specific data can be passed with a context object.
 */
public abstract class BasePhase<C> implements PhaseSizeContract
{
    public static class PhaseOptions
    {
        // "Verify before - after relation of the relative, computed, code size of a graph."
        public static final OptionKey<Boolean> VerifyGraalPhasesSize = new OptionKey<>(false);
    }

    protected BasePhase()
    {
    }

    public final void apply(final StructuredGraph graph, final C context)
    {
        graph.checkCancellation();
        int sizeBefore = 0;
        Mark before = null;
        OptionValues options = graph.getOptions();
        boolean verifySizeContract = PhaseOptions.VerifyGraalPhasesSize.getValue(options) && checkContract();
        if (verifySizeContract)
        {
            sizeBefore = NodeCostUtil.computeGraphSize(graph);
            before = graph.getMark();
        }
        this.run(graph, context);
        if (verifySizeContract)
        {
            if (!before.isCurrent())
            {
                int sizeAfter = NodeCostUtil.computeGraphSize(graph);
                NodeCostUtil.phaseFulfillsSizeContract(graph, sizeBefore, sizeAfter, this);
            }
        }
    }

    protected CharSequence getName()
    {
        return new ClassTypeSequence(BasePhase.this.getClass());
    }

    protected abstract void run(StructuredGraph graph, C context);

    @Override
    public String contractorName()
    {
        return getName().toString();
    }

    @Override
    public float codeSizeIncrease()
    {
        return 1.25f;
    }
}
