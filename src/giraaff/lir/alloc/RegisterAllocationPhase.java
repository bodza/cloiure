package giraaff.lir.alloc;

import giraaff.lir.phases.AllocationPhase;

/**
 * Marker class for register allocation phases.
 */
// @class RegisterAllocationPhase
public abstract class RegisterAllocationPhase extends AllocationPhase
{
    // @field
    private boolean neverSpillConstants;

    public void setNeverSpillConstants(boolean __neverSpillConstants)
    {
        this.neverSpillConstants = __neverSpillConstants;
    }

    public boolean getNeverSpillConstants()
    {
        return neverSpillConstants;
    }
}
