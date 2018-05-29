package giraaff.bytecode;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ExceptionHandler;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Direct access to the bytecode of a {@link ResolvedJavaMethod} that will reflect any
 * instrumentation and rewriting performed on the {@link ResolvedJavaMethod}.
 */
// @class ResolvedJavaMethodBytecode
public final class ResolvedJavaMethodBytecode implements Bytecode
{
    private final ResolvedJavaMethod method;

    // @cons
    public ResolvedJavaMethodBytecode(ResolvedJavaMethod method)
    {
        super();
        this.method = method;
    }

    @Override
    public BytecodeProvider getOrigin()
    {
        return ResolvedJavaMethodBytecodeProvider.INSTANCE;
    }

    @Override
    public ResolvedJavaMethod getMethod()
    {
        return method;
    }

    @Override
    public byte[] getCode()
    {
        return method.getCode();
    }

    @Override
    public int getCodeSize()
    {
        return method.getCodeSize();
    }

    @Override
    public int getMaxStackSize()
    {
        return method.getMaxStackSize();
    }

    @Override
    public int getMaxLocals()
    {
        return method.getMaxLocals();
    }

    @Override
    public ConstantPool getConstantPool()
    {
        return method.getConstantPool();
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers()
    {
        return method.getExceptionHandlers();
    }

    @Override
    public StackTraceElement asStackTraceElement(int bci)
    {
        return method.asStackTraceElement(bci);
    }

    @Override
    public ProfilingInfo getProfilingInfo()
    {
        return method.getProfilingInfo();
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + method.format("<%h.%n(%p)>");
    }
}
