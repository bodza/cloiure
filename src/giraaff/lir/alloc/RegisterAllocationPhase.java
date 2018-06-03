package giraaff.lir.alloc;

import giraaff.lir.phases.AllocationPhase;

///
// Marker class for register allocation phases.
///
// @class RegisterAllocationPhase
public abstract class RegisterAllocationPhase extends AllocationPhase
{
    // @field
    private boolean ___neverSpillConstants;

    public void setNeverSpillConstants(boolean __neverSpillConstants)
    {
        this.___neverSpillConstants = __neverSpillConstants;
    }

    public boolean getNeverSpillConstants()
    {
        return this.___neverSpillConstants;
    }
}
