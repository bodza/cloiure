package giraaff.hotspot;

import jdk.vm.ci.code.CallingConvention;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.hotspot.stubs.Stub;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.StandardOp;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;

// @class HotSpotLIRGenerationResult
public final class HotSpotLIRGenerationResult extends LIRGenerationResult
{
    // @field
    protected final Object ___stub;

    ///
    // Map from debug infos that need to be updated with callee save information to the operations
    // that provide the information.
    ///
    // @field
    private EconomicMap<LIRFrameState, StandardOp.SaveRegistersOp> ___calleeSaveInfo = EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);

    // @cons HotSpotLIRGenerationResult
    public HotSpotLIRGenerationResult(LIR __lir, FrameMapBuilder __frameMapBuilder, CallingConvention __callingConvention, Object __stub)
    {
        super(__lir, __frameMapBuilder, __callingConvention);
        this.___stub = __stub;
    }

    public EconomicMap<LIRFrameState, StandardOp.SaveRegistersOp> getCalleeSaveInfo()
    {
        return this.___calleeSaveInfo;
    }

    public Stub getStub()
    {
        return (Stub) this.___stub;
    }
}
