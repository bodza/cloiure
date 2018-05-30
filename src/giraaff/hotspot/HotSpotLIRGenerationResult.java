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
    protected final Object stub;

    /**
     * Map from debug infos that need to be updated with callee save information to the operations
     * that provide the information.
     */
    private EconomicMap<LIRFrameState, SaveRegistersOp> calleeSaveInfo = EconomicMap.create(Equivalence.IDENTITY_WITH_SYSTEM_HASHCODE);

    // @cons
    public HotSpotLIRGenerationResult(LIR lir, FrameMapBuilder frameMapBuilder, CallingConvention callingConvention, Object stub)
    {
        super(lir, frameMapBuilder, callingConvention);
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
}
