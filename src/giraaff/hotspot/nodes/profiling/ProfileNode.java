package giraaff.hotspot.nodes.profiling;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionKey;

public abstract class ProfileNode extends DeoptimizingFixedWithNextNode implements Simplifiable, Lowerable
{
    public static class Options
    {
        // Option "Control probabilistic profiling on AMD64."
        public static final OptionKey<Boolean> ProbabilisticProfiling = new OptionKey<>(true);
    }

    public static final NodeClass<ProfileNode> TYPE = NodeClass.create(ProfileNode.class);

    protected ResolvedJavaMethod method;

    // Only used if ProbabilisticProfiling == true and may be ignored by lowerer.
    @OptionalInput protected ValueNode random;

    // Logarithm base 2 of the profile probability.
    protected int probabilityLog;

    // Step value to add to the profile counter.
    protected int step;

    protected ProfileNode(NodeClass<? extends DeoptimizingFixedWithNextNode> c, ResolvedJavaMethod method, int probabilityLog)
    {
        super(c, StampFactory.forVoid());
        this.method = method;
        this.probabilityLog = probabilityLog;
        this.step = 1;
    }

    public ProfileNode(ResolvedJavaMethod method, int probabilityLog)
    {
        this(TYPE, method, probabilityLog);
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public ResolvedJavaMethod getProfiledMethod()
    {
        return method;
    }

    public ValueNode getRandom()
    {
        return random;
    }

    public void setRandom(ValueNode r)
    {
        updateUsages(random, r);
        this.random = r;
    }

    public int getStep()
    {
        return step;
    }

    public void setStep(int s)
    {
        step = s;
    }

    /**
     * Get the logarithm base 2 of the profile probability.
     */
    public int getProbabilityLog()
    {
        return probabilityLog;
    }

    /**
     * Gathers all the {@link ProfileNode}s that are inputs to the
     * {@linkplain StructuredGraph#getNodes() live nodes} in a given graph.
     */
    public static NodeIterable<ProfileNode> getProfileNodes(StructuredGraph graph)
    {
        return graph.getNodes().filter(ProfileNode.class);
    }

    protected abstract boolean canBeMergedWith(ProfileNode p);

    @Override
    public void simplify(SimplifierTool tool)
    {
        for (Node p = predecessor(); p != null; p = p.predecessor())
        {
            // Terminate search when we hit a control split or merge.
            if (p instanceof ControlSplitNode || p instanceof AbstractMergeNode)
            {
                break;
            }
            if (p instanceof ProfileNode)
            {
                ProfileNode that = (ProfileNode) p;
                if (this.canBeMergedWith(that))
                {
                    that.setStep(this.getStep() + that.getStep());
                    GraphUtil.removeFixedWithUnusedInputs(this);
                    tool.addToWorkList(that);
                    break;
                }
            }
        }
    }
}
