package graalvm.compiler.lir.phases;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;

public abstract class AllocationPhase extends LIRPhase<AllocationPhase.AllocationContext> {

    public static final class AllocationContext extends GenericContext {
        public final MoveFactory spillMoveFactory;
        public final RegisterAllocationConfig registerAllocationConfig;

        public AllocationContext(MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig) {
            this.spillMoveFactory = spillMoveFactory;
            this.registerAllocationConfig = registerAllocationConfig;
        }
    }

}
