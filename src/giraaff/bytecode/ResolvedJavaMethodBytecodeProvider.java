package giraaff.bytecode;

import jdk.vm.ci.meta.ResolvedJavaMethod;

///
// {@link BytecodeProvider} that returns {@link ResolvedJavaMethodBytecode} objects.
///
// @class ResolvedJavaMethodBytecodeProvider
public final class ResolvedJavaMethodBytecodeProvider implements BytecodeProvider
{
    ///
    // A state-less, shared {@link ResolvedJavaMethodBytecodeProvider} instance.
    ///
    // @def
    public static final ResolvedJavaMethodBytecodeProvider INSTANCE = new ResolvedJavaMethodBytecodeProvider();

    // @cons
    private ResolvedJavaMethodBytecodeProvider()
    {
        super();
    }

    @Override
    public Bytecode getBytecode(ResolvedJavaMethod __method)
    {
        return new ResolvedJavaMethodBytecode(__method);
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
