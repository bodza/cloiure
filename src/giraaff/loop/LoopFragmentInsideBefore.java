package giraaff.loop;

import giraaff.graph.NodeBitMap;
import giraaff.nodes.FixedNode;

// @class LoopFragmentInsideBefore
public final class LoopFragmentInsideBefore extends LoopFragmentInside
{
    // @field
    private final FixedNode ___point;

    // @cons LoopFragmentInsideBefore
    public LoopFragmentInsideBefore(LoopEx __loop, FixedNode __point)
    {
        super(__loop);
        this.___point = __point;
    }

    // duplicates lazily
    // @cons LoopFragmentInsideBefore
    public LoopFragmentInsideBefore(LoopFragmentInsideBefore __original)
    {
        super(__original);
        this.___point = __original.point();
    }

    public FixedNode point()
    {
        return this.___point;
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
