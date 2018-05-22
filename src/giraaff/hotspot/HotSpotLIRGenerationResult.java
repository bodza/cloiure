package giraaff.hotspot;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.StackSlot;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.CompilationIdentifier;
import giraaff.hotspot.stubs.Stub;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.StandardOp.SaveRegistersOp;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;

public class HotSpotLIRGenerationResult extends LIRGenerationResult
{
    /**
     * The slot reserved for storing the original return address when a frame is marked for
     * deoptimization. The return address slot in the callee is overwritten with the address of a
     * deoptimization stub.
     */
    private StackSlot deoptimizationRescueSlot;
    protected final Object stub;

    private int maxInterpreterFrameSize;

    /**
     * Map from debug infos that need to be updated with callee save information to the operations
     * that provide the information.
     */
    private EconomicMap<LIRFrameState, SaveRegistersOp> calleeSaveInfo = EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);

    public HotSpotLIRGenerationResult(CompilationIdentifier compilationId, LIR lir, FrameMapBuilder frameMapBuilder, CallingConvention callingConvention, Object stub)
    {
        super(compilationId, lir, frameMapBuilder, callingConvention);
        this.stub = stub;
    }

    public EconomicMap<LIRFrameState, SaveRegistersOp> getCalleeSaveInfo()
    {
        return calleeSaveInfo;
    }

    public Stub getStub()
    {
        return (Stub) stub;
    }

    public StackSlot getDeoptimizationRescueSlot()
    {
        return deoptimizationRescueSlot;
    }

    public final void setDeoptimizationRescueSlot(StackSlot stackSlot)
    {
        this.deoptimizationRescueSlot = stackSlot;
    }

    public void setMaxInterpreterFrameSize(int maxInterpreterFrameSize)
    {
        this.maxInterpreterFrameSize = maxInterpreterFrameSize;
    }

    public int getMaxInterpreterFrameSize()
    {
        return maxInterpreterFrameSize;
    }
}
