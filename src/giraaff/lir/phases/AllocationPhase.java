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
        public final MoveFactory ___spillMoveFactory;
        // @field
        public final RegisterAllocationConfig ___registerAllocationConfig;

        // @cons
        public AllocationContext(MoveFactory __spillMoveFactory, RegisterAllocationConfig __registerAllocationConfig)
        {
            super();
            this.___spillMoveFactory = __spillMoveFactory;
            this.___registerAllocationConfig = __registerAllocationConfig;
        }
    }
}
