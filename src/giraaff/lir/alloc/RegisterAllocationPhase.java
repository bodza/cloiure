package giraaff.lir.alloc;

import giraaff.lir.phases.AllocationPhase;

/**
 * Marker class for register allocation phases.
 */
public abstract class RegisterAllocationPhase extends AllocationPhase
{
    private boolean neverSpillConstants;

    public void setNeverSpillConstants(boolean neverSpillConstants)
    {
        this.neverSpillConstants = neverSpillConstants;
    }

    public boolean getNeverSpillConstants()
    {
        return neverSpillConstants;
    }
}
