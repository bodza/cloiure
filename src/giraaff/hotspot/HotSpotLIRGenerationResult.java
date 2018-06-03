package giraaff.hotspot;

import jdk.vm.ci.code.CallingConvention;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.hotspot.stubs.Stub;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.StandardOp.SaveRegistersOp;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;

// @class HotSpotLIRGenerationResult
public final class HotSpotLIRGenerationResult extends LIRGenerationResult
{
    // @field
    protected final Object stub;

    /**
     * Map from debug infos that need to be updated with callee save information to the operations
     * that provide the information.
     */
    // @field
    private EconomicMap<LIRFrameState, SaveRegistersOp> calleeSaveInfo = EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);

    // @cons
    public HotSpotLIRGenerationResult(LIR __lir, FrameMapBuilder __frameMapBuilder, CallingConvention __callingConvention, Object __stub)
    {
        super(__lir, __frameMapBuilder, __callingConvention);
        this.stub = __stub;
    }

    public EconomicMap<LIRFrameState, SaveRegistersOp> getCalleeSaveInfo()
    {
        return calleeSaveInfo;
    }

    public Stub getStub()
    {
        return (Stub) stub;
    }
}
