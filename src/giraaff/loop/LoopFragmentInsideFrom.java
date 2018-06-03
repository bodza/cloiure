package giraaff.loop;

import giraaff.graph.NodeBitMap;
import giraaff.nodes.FixedNode;

// @class LoopFragmentInsideFrom
public final class LoopFragmentInsideFrom extends LoopFragmentInside
{
    // @field
    private final FixedNode point;

    // @cons
    public LoopFragmentInsideFrom(LoopEx __loop, FixedNode __point)
    {
        super(__loop);
        this.point = __point;
    }

    // duplicates lazily
    // @cons
    public LoopFragmentInsideFrom(LoopFragmentInsideFrom __original)
    {
        super(__original);
        this.point = __original.point();
    }

    public FixedNode point()
    {
        return point;
    }

    @Override
    public LoopFragmentInsideFrom duplicate()
    {
        return new LoopFragmentInsideFrom(this);
    }

    @Override
    public NodeBitMap nodes()
    {
        return null;
    }
}
