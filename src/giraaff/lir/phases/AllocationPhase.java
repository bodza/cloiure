package giraaff.lir.phases;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.lir.gen.LIRGeneratorTool;

// @class AllocationPhase
public abstract class AllocationPhase extends LIRPhase<AllocationPhase.AllocationContext>
{
    // @class AllocationPhase.AllocationContext
    public static final class AllocationContext extends GenericContext
    {
        // @field
        public final LIRGeneratorTool.MoveFactory ___spillMoveFactory;
        // @field
        public final RegisterAllocationConfig ___registerAllocationConfig;

        // @cons AllocationPhase.AllocationContext
        public AllocationContext(LIRGeneratorTool.MoveFactory __spillMoveFactory, RegisterAllocationConfig __registerAllocationConfig)
        {
            super();
            this.___spillMoveFactory = __spillMoveFactory;
            this.___registerAllocationConfig = __registerAllocationConfig;
        }
    }
}
