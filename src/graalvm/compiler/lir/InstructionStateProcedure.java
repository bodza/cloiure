package graalvm.compiler.lir;

@FunctionalInterface
public interface InstructionStateProcedure
{
    void doState(LIRInstruction instruction, LIRFrameState state);
}
