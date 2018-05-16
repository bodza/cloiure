package graalvm.compiler.lir.gen;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.core.common.CompilationIdentifier.Verbosity;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.FrameMapBuilder;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.RegisterConfig;

public class LIRGenerationResult
{
    private final LIR lir;
    private final FrameMapBuilder frameMapBuilder;
    private FrameMap frameMap;
    private final CallingConvention callingConvention;
    /**
     * Records whether the code being generated makes at least one foreign call.
     */
    private boolean hasForeignCall;
    /**
     * Unique identifier of this compilation.
     */
    private CompilationIdentifier compilationId;

    /**
     * Stores comments about a {@link LIRInstruction} , e.g., which phase created it.
     */
    private EconomicMap<LIRInstruction, String> comments;

    public LIRGenerationResult(CompilationIdentifier compilationId, LIR lir, FrameMapBuilder frameMapBuilder, CallingConvention callingConvention)
    {
        this.lir = lir;
        this.frameMapBuilder = frameMapBuilder;
        this.callingConvention = callingConvention;
        this.compilationId = compilationId;
    }

    /**
     * Adds a comment to a {@link LIRInstruction}. Existing comments are replaced.
     */
    public final void setComment(LIRInstruction op, String comment)
    {
        DebugContext debug = lir.getDebug();
        if (debug.isDumpEnabled(DebugContext.BASIC_LEVEL))
        {
            if (comments == null)
            {
                comments = EconomicMap.create(Equivalence.IDENTITY);
            }
            comments.put(op, comment);
        }
    }

    /**
     * Gets the comment attached to a {@link LIRInstruction}.
     */
    public final String getComment(LIRInstruction op)
    {
        if (comments == null)
        {
            return null;
        }
        return comments.get(op);
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
        assert frameMap == null : "getFrameMapBuilder() can only be used before calling buildFrameMap()!";
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
        assert frameMap == null : "buildFrameMap() can only be called once!";
        frameMap = frameMapBuilder.buildFrameMap(this);
    }

    /**
     * Returns the {@link FrameMap} associated with this {@link LIRGenerationResult}.
     *
     * This method can only be called after {@link #buildFrameMap}.
     */
    public FrameMap getFrameMap()
    {
        assert frameMap != null : "getFrameMap() can only be used after calling buildFrameMap()!";
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

    public final void setForeignCall(boolean hasForeignCall)
    {
        this.hasForeignCall = hasForeignCall;
    }

    public String getCompilationUnitName()
    {
        if (compilationId == null || compilationId == CompilationIdentifier.INVALID_COMPILATION_ID)
        {
            return "<unknown>";
        }
        return compilationId.toString(Verbosity.NAME);
    }

    /**
     * Returns a unique identifier of the current compilation.
     */
    public CompilationIdentifier getCompilationId()
    {
        return compilationId;
    }
}
