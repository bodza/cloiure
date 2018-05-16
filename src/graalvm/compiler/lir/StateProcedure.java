package graalvm.compiler.lir;

@FunctionalInterface
public interface StateProcedure extends InstructionStateProcedure
{
    void doState(LIRFrameState state);

    @Override
    default void doState(LIRInstruction instruction, LIRFrameState state)
    {
        doState(state);
    }
}
