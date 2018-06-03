package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.AnchoringNode;

// @class FloatingAnchoredNode
public abstract class FloatingAnchoredNode extends FloatingNode
{
    // @def
    public static final NodeClass<FloatingAnchoredNode> TYPE = NodeClass.create(FloatingAnchoredNode.class);

    @Input(InputType.Anchor)
    // @field
    protected AnchoringNode anchor;

    // @cons
    public FloatingAnchoredNode(NodeClass<? extends FloatingAnchoredNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    // @cons
    public FloatingAnchoredNode(NodeClass<? extends FloatingAnchoredNode> __c, Stamp __stamp, AnchoringNode __anchor)
    {
        super(__c, __stamp);
        this.anchor = __anchor;
    }

    public AnchoringNode getAnchor()
    {
        return anchor;
    }

    public void setAnchor(AnchoringNode __x)
    {
        updateUsagesInterface(this.anchor, __x);
        this.anchor = __x;
    }
}
