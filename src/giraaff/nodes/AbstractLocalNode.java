package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.calc.FloatingNode;

// @class AbstractLocalNode
public abstract class AbstractLocalNode extends FloatingNode
{
    // @def
    public static final NodeClass<AbstractLocalNode> TYPE = NodeClass.create(AbstractLocalNode.class);

    // @field
    protected final int ___index;

    // @cons
    protected AbstractLocalNode(NodeClass<? extends AbstractLocalNode> __c, int __index, Stamp __stamp)
    {
        super(__c, __stamp);
        this.___index = __index;
    }

    ///
    // Gets the index of this local in the array of parameters. This is NOT the JVM local index.
    //
    // @return the index
    ///
    public int index()
    {
        return this.___index;
    }
}
