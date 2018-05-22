package giraaff.lir.phases;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

public abstract class AllocationPhase extends LIRPhase<AllocationPhase.AllocationContext>
{
    public static final class AllocationContext extends GenericContext
    {
        public final MoveFactory spillMoveFactory;
        public final RegisterAllocationConfig registerAllocationConfig;

        public AllocationContext(MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig)
        {
            this.spillMoveFactory = spillMoveFactory;
            this.registerAllocationConfig = registerAllocationConfig;
        }
    }
}
