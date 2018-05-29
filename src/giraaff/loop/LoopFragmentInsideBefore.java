package giraaff.loop;

import giraaff.graph.NodeBitMap;
import giraaff.nodes.FixedNode;

// @class LoopFragmentInsideBefore
public final class LoopFragmentInsideBefore extends LoopFragmentInside
{
    private final FixedNode point;

    // @cons
    public LoopFragmentInsideBefore(LoopEx loop, FixedNode point)
    {
        super(loop);
        this.point = point;
    }

    // duplicates lazily
    // @cons
    public LoopFragmentInsideBefore(LoopFragmentInsideBefore original)
    {
        super(original);
        this.point = original.point();
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
