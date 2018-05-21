package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.calc.FloatingNode;

public abstract class AbstractLocalNode extends FloatingNode
{
    public static final NodeClass<AbstractLocalNode> TYPE = NodeClass.create(AbstractLocalNode.class);
    protected final int index;

    protected AbstractLocalNode(NodeClass<? extends AbstractLocalNode> c, int index, Stamp stamp)
    {
        super(c, stamp);
        this.index = index;
    }

    /**
     * Gets the index of this local in the array of parameters. This is NOT the JVM local index.
     *
     * @return the index
     */
    public int index()
    {
        return index;
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Name)
        {
            return super.toString(Verbosity.Name) + "(" + index + ")";
        }
        else
        {
            return super.toString(verbosity);
        }
    }
}
