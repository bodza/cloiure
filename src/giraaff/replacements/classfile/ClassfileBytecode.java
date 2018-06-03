package giraaff.replacements.classfile;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.DefaultProfilingInfo;
import jdk.vm.ci.meta.ExceptionHandler;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.TriState;

import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeProvider;
import giraaff.util.GraalError;

/**
 * The bytecode properties of a method as parsed directly from a class file without any
 * {@linkplain java.lang.instrument.Instrumentation instrumentation} or other rewriting performed on
 * the bytecode.
 */
// @class ClassfileBytecode
public final class ClassfileBytecode implements Bytecode
{
    // @def
    private static final int EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES = 8;

    // @field
    private final ResolvedJavaMethod method;

    // @field
    private final ClassfileConstantPool constantPool;

    // @field
    private byte[] code;
    // @field
    private int maxLocals;
    // @field
    private int maxStack;

    // @field
    private byte[] exceptionTableBytes;

    // @cons
    public ClassfileBytecode(ResolvedJavaMethod __method, DataInputStream __stream, ClassfileConstantPool __constantPool) throws IOException
    {
        super();
        this.method = __method;
        this.constantPool = __constantPool;
        maxStack = __stream.readUnsignedShort();
        maxLocals = __stream.readUnsignedShort();
        int __codeLength = __stream.readInt();
        code = new byte[__codeLength];
        __stream.readFully(code);
        int __exceptionTableLength = __stream.readUnsignedShort();
        exceptionTableBytes = new byte[__exceptionTableLength * EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES];
        __stream.readFully(exceptionTableBytes);
        skipCodeAttributes(__stream);
    }

    @Override
    public BytecodeProvider getOrigin()
    {
        return constantPool.context;
    }

    private void skipCodeAttributes(DataInputStream __stream) throws IOException
    {
        int __count = __stream.readUnsignedShort();
        for (int __i = 0; __i < __count; __i++)
        {
            __stream.readUnsignedShort();
            Classfile.skipFully(__stream, __stream.readInt());
        }
    }

    @Override
    public byte[] getCode()
    {
        return code;
    }

    @Override
    public int getCodeSize()
    {
        return code.length;
    }

    @Override
    public int getMaxLocals()
    {
        return maxLocals;
    }

    @Override
    public int getMaxStackSize()
    {
        return maxStack;
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers()
    {
        if (exceptionTableBytes == null)
        {
            return new ExceptionHandler[0];
        }

        final int __exceptionTableLength = exceptionTableBytes.length / EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES;
        ExceptionHandler[] __handlers = new ExceptionHandler[__exceptionTableLength];
        DataInputStream __stream = new DataInputStream(new ByteArrayInputStream(exceptionTableBytes));

        for (int __i = 0; __i < __exceptionTableLength; __i++)
        {
            try
            {
                final int __startPc = __stream.readUnsignedShort();
                final int __endPc = __stream.readUnsignedShort();
                final int __handlerPc = __stream.readUnsignedShort();
                int __catchTypeIndex = __stream.readUnsignedShort();

                JavaType __catchType;
                if (__catchTypeIndex == 0)
                {
                    __catchType = null;
                }
                else
                {
                    final int __opcode = -1; // opcode is not used
                    __catchType = constantPool.lookupType(__catchTypeIndex, __opcode);

                    // Check for Throwable which catches everything.
                    if (__catchType.toJavaName().equals("java.lang.Throwable"))
                    {
                        __catchTypeIndex = 0;
                        __catchType = null;
                    }
                }
                __handlers[__i] = new ExceptionHandler(__startPc, __endPc, __handlerPc, __catchTypeIndex, __catchType);
            }
            catch (IOException __e)
            {
                throw new GraalError(__e);
            }
        }

        return __handlers;
    }

    @Override
    public ConstantPool getConstantPool()
    {
        return constantPool;
    }

    @Override
    public ResolvedJavaMethod getMethod()
    {
        return method;
    }

    @Override
    public ProfilingInfo getProfilingInfo()
    {
        return DefaultProfilingInfo.get(TriState.FALSE);
    }
}
