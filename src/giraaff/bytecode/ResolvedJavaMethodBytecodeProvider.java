package giraaff.bytecode;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * {@link BytecodeProvider} that returns {@link ResolvedJavaMethodBytecode} objects.
 */
// @class ResolvedJavaMethodBytecodeProvider
public final class ResolvedJavaMethodBytecodeProvider implements BytecodeProvider
{
    /**
     * A state-less, shared {@link ResolvedJavaMethodBytecodeProvider} instance.
     */
    public static final ResolvedJavaMethodBytecodeProvider INSTANCE = new ResolvedJavaMethodBytecodeProvider();

    // @cons
    private ResolvedJavaMethodBytecodeProvider()
    {
        super();
    }

    @Override
    public Bytecode getBytecode(ResolvedJavaMethod method)
    {
        return new ResolvedJavaMethodBytecode(method);
    }

    @Override
    public boolean supportsInvokedynamic()
    {
        return true;
    }

    @Override
    public boolean shouldRecordMethodDependencies()
    {
        return true;
    }
}
