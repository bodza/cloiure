package giraaff.lir.gen;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.RegisterConfig;

import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.FrameMapBuilder;

// @class LIRGenerationResult
public class LIRGenerationResult
{
    // @field
    private final LIR lir;
    // @field
    private final FrameMapBuilder frameMapBuilder;
    // @field
    private FrameMap frameMap;
    // @field
    private final CallingConvention callingConvention;
    /**
     * Records whether the code being generated makes at least one foreign call.
     */
    // @field
    private boolean hasForeignCall;

    // @cons
    public LIRGenerationResult(LIR __lir, FrameMapBuilder __frameMapBuilder, CallingConvention __callingConvention)
    {
        super();
        this.lir = __lir;
        this.frameMapBuilder = __frameMapBuilder;
        this.callingConvention = __callingConvention;
    }

    /**
     * Returns the incoming calling convention for the parameters of the method that is compiled.
     */
    public CallingConvention getCallingConvention()
    {
        return callingConvention;
    }

    /**
     * Returns the {@link FrameMapBuilder} for collecting the information to build a
     * {@link FrameMap}.
     *
     * This method can only be used prior calling {@link #buildFrameMap}.
     */
    public final FrameMapBuilder getFrameMapBuilder()
    {
        return frameMapBuilder;
    }

    /**
     * Creates a {@link FrameMap} out of the {@link FrameMapBuilder}. This method should only be
     * called once. After calling it, {@link #getFrameMapBuilder()} can no longer be used.
     *
     * @see FrameMapBuilder#buildFrameMap
     */
    public void buildFrameMap()
    {
        frameMap = frameMapBuilder.buildFrameMap(this);
    }

    /**
     * Returns the {@link FrameMap} associated with this {@link LIRGenerationResult}.
     *
     * This method can only be called after {@link #buildFrameMap}.
     */
    public FrameMap getFrameMap()
    {
        return frameMap;
    }

    public final RegisterConfig getRegisterConfig()
    {
        return frameMapBuilder.getRegisterConfig();
    }

    public LIR getLIR()
    {
        return lir;
    }

    /**
     * Determines whether the code being generated makes at least one foreign call.
     */
    public boolean hasForeignCall()
    {
        return hasForeignCall;
    }

    public final void setForeignCall(boolean __hasForeignCall)
    {
        this.hasForeignCall = __hasForeignCall;
    }
}
