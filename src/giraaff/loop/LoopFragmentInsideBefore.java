package giraaff.loop;

import giraaff.graph.NodeBitMap;
import giraaff.nodes.FixedNode;

public class LoopFragmentInsideBefore extends LoopFragmentInside
{
    private final FixedNode point;

    public LoopFragmentInsideBefore(LoopEx loop, FixedNode point)
    {
        super(loop);
        this.point = point;
    }

    // duplicates lazily
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