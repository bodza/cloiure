package giraaff.lir.phases;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

// @class AllocationPhase
public abstract class AllocationPhase extends LIRPhase<AllocationPhase.AllocationContext>
{
    // @class AllocationPhase.AllocationContext
    public static final class AllocationContext extends GenericContext
    {
        // @field
        public final MoveFactory spillMoveFactory;
        // @field
        public final RegisterAllocationConfig registerAllocationConfig;

        // @cons
        public AllocationContext(MoveFactory __spillMoveFactory, RegisterAllocationConfig __registerAllocationConfig)
        {
            super();
            this.spillMoveFactory = __spillMoveFactory;
            this.registerAllocationConfig = __registerAllocationConfig;
        }
    }
}
