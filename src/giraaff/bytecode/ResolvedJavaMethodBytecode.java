package giraaff.bytecode;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.ExceptionHandler;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;

///
// Direct access to the bytecode of a {@link ResolvedJavaMethod} that will reflect any
// instrumentation and rewriting performed on the {@link ResolvedJavaMethod}.
///
// @class ResolvedJavaMethodBytecode
public final class ResolvedJavaMethodBytecode implements Bytecode
{
    // @field
    private final ResolvedJavaMethod ___method;

    // @cons ResolvedJavaMethodBytecode
    public ResolvedJavaMethodBytecode(ResolvedJavaMethod __method)
    {
        super();
        this.___method = __method;
    }

    @Override
    public BytecodeProvider getOrigin()
    {
        return ResolvedJavaMethodBytecodeProvider.INSTANCE;
    }

    @Override
    public ResolvedJavaMethod getMethod()
    {
        return this.___method;
    }

    @Override
    public byte[] getCode()
    {
        return this.___method.getCode();
    }

    @Override
    public int getCodeSize()
    {
        return this.___method.getCodeSize();
    }

    @Override
    public int getMaxStackSize()
    {
        return this.___method.getMaxStackSize();
    }

    @Override
    public int getMaxLocals()
    {
        return this.___method.getMaxLocals();
    }

    @Override
    public ConstantPool getConstantPool()
    {
        return this.___method.getConstantPool();
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers()
    {
        return this.___method.getExceptionHandlers();
    }

    @Override
    public ProfilingInfo getProfilingInfo()
    {
        return this.___method.getProfilingInfo();
    }
}
