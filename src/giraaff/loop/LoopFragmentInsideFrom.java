package giraaff.loop;

import giraaff.graph.NodeBitMap;
import giraaff.nodes.FixedNode;

// @class LoopFragmentInsideFrom
public final class LoopFragmentInsideFrom extends LoopFragmentInside
{
    // @field
    private final FixedNode ___point;

    // @cons LoopFragmentInsideFrom
    public LoopFragmentInsideFrom(LoopEx __loop, FixedNode __point)
    {
        super(__loop);
        this.___point = __point;
    }

    // duplicates lazily
    // @cons LoopFragmentInsideFrom
    public LoopFragmentInsideFrom(LoopFragmentInsideFrom __original)
    {
        super(__original);
        this.___point = __original.point();
    }

    public FixedNode point()
    {
        return this.___point;
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
