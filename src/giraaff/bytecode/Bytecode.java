package giraaff.bytecode;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ExceptionHandler;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;

///
// An interface for accessing the bytecode properties of a {@link ResolvedJavaMethod} that allows
// for different properties than those returned by {@link ResolvedJavaMethod}. Since the bytecode
// accessed directly from {@link ResolvedJavaMethod} may have been subject to bytecode
// instrumentation and VM rewriting, this indirection can be used to enable access to the original
// bytecode of a method (i.e., as defined in a class file).
///
// @iface Bytecode
public interface Bytecode
{
    ///
    // Gets the method this object supplies bytecode for.
    ///
    ResolvedJavaMethod getMethod();

    byte[] getCode();

    int getCodeSize();

    int getMaxStackSize();

    int getMaxLocals();

    ConstantPool getConstantPool();

    ProfilingInfo getProfilingInfo();

    ExceptionHandler[] getExceptionHandlers();

    ///
    // Gets the {@link BytecodeProvider} from which this object was acquired.
    ///
    BytecodeProvider getOrigin();
}
