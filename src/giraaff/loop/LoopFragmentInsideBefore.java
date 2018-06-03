package giraaff.loop;

import giraaff.graph.NodeBitMap;
import giraaff.nodes.FixedNode;

// @class LoopFragmentInsideBefore
public final class LoopFragmentInsideBefore extends LoopFragmentInside
{
    // @field
    private final FixedNode point;

    // @cons
    public LoopFragmentInsideBefore(LoopEx __loop, FixedNode __point)
    {
        super(__loop);
        this.point = __point;
    }

    // duplicates lazily
    // @cons
    public LoopFragmentInsideBefore(LoopFragmentInsideBefore __original)
    {
        super(__original);
        this.point = __original.point();
    }

    public FixedNode point()
    {
        return point;
    }

    @Override
    public LoopFragmentInsideBefore duplicate()
    {
        return new LoopFragmentInsideBefore(this);
    }

    @Override
    public NodeBitMap nodes()
    {
        return null;
    }
}
