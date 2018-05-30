package giraaff.nodes.cfg;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.cfg.Loop;
import giraaff.nodes.LoopBeginNode;

// @class HIRLoop
public final class HIRLoop extends Loop<Block>
{
    private LocationSet killLocations;

    // @cons
    protected HIRLoop(Loop<Block> parent, int index, Block header)
    {
        super(parent, index, header);
    }

    @Override
    public long numBackedges()
    {
        return ((LoopBeginNode) getHeader().getBeginNode()).loopEnds().count();
    }

    public LocationSet getKillLocations()
    {
        if (killLocations == null)
        {
            killLocations = new LocationSet();
            for (Block b : this.getBlocks())
            {
                if (b.getLoop() == this)
                {
                    killLocations.addAll(b.getKillLocations());
                    if (killLocations.isAny())
                    {
                        break;
                    }
                }
            }
        }
        for (Loop<Block> child : this.getChildren())
        {
            if (killLocations.isAny())
            {
                break;
            }
            killLocations.addAll(((HIRLoop) child).getKillLocations());
        }
        return killLocations;
    }

    public boolean canKill(LocationIdentity location)
    {
        return getKillLocations().contains(location);
    }
}
