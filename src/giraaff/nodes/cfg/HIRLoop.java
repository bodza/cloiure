package giraaff.nodes.cfg;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.cfg.Loop;
import giraaff.nodes.LoopBeginNode;

// @class HIRLoop
public final class HIRLoop extends Loop<Block>
{
    // @field
    private LocationSet ___killLocations;

    // @cons
    protected HIRLoop(Loop<Block> __parent, int __index, Block __header)
    {
        super(__parent, __index, __header);
    }

    @Override
    public long numBackedges()
    {
        return ((LoopBeginNode) getHeader().getBeginNode()).loopEnds().count();
    }

    public LocationSet getKillLocations()
    {
        if (this.___killLocations == null)
        {
            this.___killLocations = new LocationSet();
            for (Block __b : this.getBlocks())
            {
                if (__b.getLoop() == this)
                {
                    this.___killLocations.addAll(__b.getKillLocations());
                    if (this.___killLocations.isAny())
                    {
                        break;
                    }
                }
            }
        }
        for (Loop<Block> __child : this.getChildren())
        {
            if (this.___killLocations.isAny())
            {
                break;
            }
            this.___killLocations.addAll(((HIRLoop) __child).getKillLocations());
        }
        return this.___killLocations;
    }

    public boolean canKill(LocationIdentity __location)
    {
        return getKillLocations().contains(__location);
    }
}
