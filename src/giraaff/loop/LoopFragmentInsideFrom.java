package giraaff.loop;

import giraaff.graph.NodeBitMap;
import giraaff.nodes.FixedNode;

public class LoopFragmentInsideFrom extends LoopFragmentInside
{
    private final FixedNode point;

    public LoopFragmentInsideFrom(LoopEx loop, FixedNode point)
    {
        super(loop);
        this.point = point;
    }

    // duplicates lazily
    public LoopFragmentInsideFrom(LoopFragmentInsideFrom original)
    {
        super(original);
        this.point = original.point();
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
