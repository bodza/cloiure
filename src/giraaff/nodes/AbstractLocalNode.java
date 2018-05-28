package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.calc.FloatingNode;

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
