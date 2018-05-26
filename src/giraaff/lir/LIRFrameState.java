package giraaff.lir;

/**
 * This class represents garbage collection and deoptimization information attached to a LIR instruction.
 */
public class LIRFrameState
{
    public static final LIRFrameState NO_STATE = new LIRFrameState(null);

    public final LabelRef exceptionEdge;

    public LIRFrameState(LabelRef exceptionEdge)
    {
        this.exceptionEdge = exceptionEdge;
    }
}
