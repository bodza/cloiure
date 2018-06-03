package giraaff.lir;

/**
 * This class represents garbage collection and deoptimization information attached to a LIR instruction.
 */
// @class LIRFrameState
public final class LIRFrameState
{
    // @def
    public static final LIRFrameState NO_STATE = new LIRFrameState(null);

    // @field
    public final LabelRef exceptionEdge;

    // @cons
    public LIRFrameState(LabelRef __exceptionEdge)
    {
        super();
        this.exceptionEdge = __exceptionEdge;
    }
}
