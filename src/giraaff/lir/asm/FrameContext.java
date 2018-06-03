package giraaff.lir.asm;

///
// Code for managing a method's native frame.
///
// @iface FrameContext
public interface FrameContext
{
    ///
    // Emits code common to all entry points of a method. This may include:
    //
    // <li>setting up the stack frame</li>
    // <li>saving callee-saved registers</li>
    // <li>stack overflow checking</li>
    ///
    void enter(CompilationResultBuilder __crb);

    ///
    // Emits code to be executed just prior to returning from a method. This may include:
    //
    // <li>restoring callee-saved registers</li>
    // <li>performing a safepoint</li>
    // <li>destroying the stack frame</li>
    ///
    void leave(CompilationResultBuilder __crb);

    ///
    // Determines if a frame is set up and torn down by this object.
    ///
    boolean hasFrame();
}
