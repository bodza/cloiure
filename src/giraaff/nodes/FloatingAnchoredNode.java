package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.AnchoringNode;

// @class FloatingAnchoredNode
public abstract class FloatingAnchoredNode extends FloatingNode
{
    public static final NodeClass<FloatingAnchoredNode> TYPE = NodeClass.create(FloatingAnchoredNode.class);

    @Input(InputType.Anchor) protected AnchoringNode anchor;

    // @cons
    public FloatingAnchoredNode(NodeClass<? extends FloatingAnchoredNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    // @cons
    public FloatingAnchoredNode(NodeClass<? extends FloatingAnchoredNode> c, Stamp stamp, AnchoringNode anchor)
    {
        super(c, stamp);
        this.anchor = anchor;
    }

    public AnchoringNode getAnchor()
    {
        return anchor;
    }

    public void setAnchor(AnchoringNode x)
    {
        updateUsagesInterface(this.anchor, x);
        this.anchor = x;
    }
}
