package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.extended.AnchoringNode;

public abstract class FloatingAnchoredNode extends FloatingNode
{
    public static final NodeClass<FloatingAnchoredNode> TYPE = NodeClass.create(FloatingAnchoredNode.class);

    @Input(InputType.Anchor) protected AnchoringNode anchor;

    public FloatingAnchoredNode(NodeClass<? extends FloatingAnchoredNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

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
