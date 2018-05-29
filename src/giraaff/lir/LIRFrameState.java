package giraaff.lir;

/**
 * This class represents garbage collection and deoptimization information attached to a LIR instruction.
 */
// @class LIRFrameState
public final class LIRFrameState
{
    public static final LIRFrameState NO_STATE = new LIRFrameState(null);

    public final LabelRef exceptionEdge;

    // @cons
    public LIRFrameState(LabelRef exceptionEdge)
    {
        super();
        this.exceptionEdge = exceptionEdge;
    }
}
