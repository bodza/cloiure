package giraaff.hotspot.nodes.profiling;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConditionalNode;

public class ProfileBranchNode extends ProfileWithNotificationNode
{
    public static final NodeClass<ProfileBranchNode> TYPE = NodeClass.create(ProfileBranchNode.class);

    @OptionalInput ValueNode branchCondition;
    protected int bci;
    protected int targetBci;

    public ProfileBranchNode(ResolvedJavaMethod method, int freqLog, int probabilityLog, ConditionalNode branchCondition, int bci, int targetBci)
    {
        super(TYPE, method, freqLog, probabilityLog);
        this.branchCondition = branchCondition;
        this.bci = bci;
        this.targetBci = targetBci;
    }

    public ProfileBranchNode(ResolvedJavaMethod method, int freqLog, int probabilityLog, int bci, int targetBci)
    {
        super(TYPE, method, freqLog, probabilityLog);
        this.branchCondition = null;
        this.bci = bci;
        this.targetBci = targetBci;
    }

    public int bci()
    {
        return bci;
    }

    public int targetBci()
    {
        return targetBci;
    }

    public ValueNode branchCondition()
    {
        return branchCondition;
    }

    public boolean hasCondition()
    {
        return branchCondition != null;
    }

    @Override
    protected boolean canBeMergedWith(ProfileNode p)
    {
        if (p instanceof ProfileBranchNode)
        {
            ProfileBranchNode that = (ProfileBranchNode) p;
            return this.method.equals(that.method) && this.bci == that.bci;
        }
        return false;
    }

    /**
     * Gathers all the {@link ProfileBranchNode}s that are inputs to the
     * {@linkplain StructuredGraph#getNodes() live nodes} in a given graph.
     */
    public static NodeIterable<ProfileBranchNode> getProfileBranchNodes(StructuredGraph graph)
    {
        return graph.getNodes().filter(ProfileBranchNode.class);
    }
}
