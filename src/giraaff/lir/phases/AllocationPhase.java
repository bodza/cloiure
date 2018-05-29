package giraaff.lir.phases;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

// @class AllocationPhase
public abstract class AllocationPhase extends LIRPhase<AllocationPhase.AllocationContext>
{
    // @class AllocationPhase.AllocationContext
    public static final class AllocationContext extends GenericContext
    {
        public final MoveFactory spillMoveFactory;
        public final RegisterAllocationConfig registerAllocationConfig;

        // @cons
        public AllocationContext(MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig)
        {
            super();
            this.spillMoveFactory = spillMoveFactory;
            this.registerAllocationConfig = registerAllocationConfig;
        }
    }
}
