package graalvm.compiler.hotspot.nodes.profiling;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.nodes.StructuredGraph;

public class ProfileInvokeNode extends ProfileWithNotificationNode
{
    public static final NodeClass<ProfileInvokeNode> TYPE = NodeClass.create(ProfileInvokeNode.class);

    public ProfileInvokeNode(ResolvedJavaMethod method, int freqLog, int probabilityLog)
    {
        super(TYPE, method, freqLog, probabilityLog);
    }

    @Override
    protected boolean canBeMergedWith(ProfileNode p)
    {
        if (p instanceof ProfileInvokeNode)
        {
            ProfileInvokeNode that = (ProfileInvokeNode) p;
            return this.method.equals(that.method);
        }
        return false;
    }

    /**
     * Gathers all the {@link ProfileInvokeNode}s that are inputs to the
     * {@linkplain StructuredGraph#getNodes() live nodes} in a given graph.
     */
    public static NodeIterable<ProfileInvokeNode> getProfileInvokeNodes(StructuredGraph graph)
    {
        return graph.getNodes().filter(ProfileInvokeNode.class);
    }
}
