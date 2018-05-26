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
import giraaff.replacements.classfile.ClassfileConstant.Utf8;
import giraaff.util.GraalError;

/**
 * The bytecode properties of a method as parsed directly from a class file without any
 * {@linkplain java.lang.instrument.Instrumentation instrumentation} or other rewriting performed on
 * the bytecode.
 */
public class ClassfileBytecode implements Bytecode
{
    private static final int EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES = 8;

    private final ResolvedJavaMethod method;

    private final ClassfileConstantPool constantPool;

    private byte[] code;
    private int maxLocals;
    private int maxStack;

    private byte[] exceptionTableBytes;

    public ClassfileBytecode(ResolvedJavaMethod method, DataInputStream stream, ClassfileConstantPool constantPool) throws IOException
    {
        this.method = method;
        this.constantPool = constantPool;
        maxStack = stream.readUnsignedShort();
        maxLocals = stream.readUnsignedShort();
        int codeLength = stream.readInt();
        code = new byte[codeLength];
        stream.readFully(code);
        int exceptionTableLength = stream.readUnsignedShort();
        exceptionTableBytes = new byte[exceptionTableLength * EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES];
        stream.readFully(exceptionTableBytes);
        skipCodeAttributes(stream);
    }

    @Override
    public BytecodeProvider getOrigin()
    {
        return constantPool.context;
    }

    private void skipCodeAttributes(DataInputStream stream) throws IOException
    {
        int count = stream.readUnsignedShort();
        for (int i = 0; i < count; i++)
        {
            stream.readUnsignedShort();
            Classfile.skipFully(stream, stream.readInt());
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

        final int exceptionTableLength = exceptionTableBytes.length / EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES;
        ExceptionHandler[] handlers = new ExceptionHandler[exceptionTableLength];
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(exceptionTableBytes));

        for (int i = 0; i < exceptionTableLength; i++)
        {
            try
            {
                final int startPc = stream.readUnsignedShort();
                final int endPc = stream.readUnsignedShort();
                final int handlerPc = stream.readUnsignedShort();
                int catchTypeIndex = stream.readUnsignedShort();

                JavaType catchType;
                if (catchTypeIndex == 0)
                {
                    catchType = null;
                }
                else
                {
                    final int opcode = -1; // opcode is not used
                    catchType = constantPool.lookupType(catchTypeIndex, opcode);

                    // Check for Throwable which catches everything.
                    if (catchType.toJavaName().equals("java.lang.Throwable"))
                    {
                        catchTypeIndex = 0;
                        catchType = null;
                    }
                }
                handlers[i] = new ExceptionHandler(startPc, endPc, handlerPc, catchTypeIndex, catchType);
            }
            catch (IOException e)
            {
                throw new GraalError(e);
            }
        }

        return handlers;
    }

    @Override
    public StackTraceElement asStackTraceElement(int bci)
    {
        return new StackTraceElement(method.getDeclaringClass().toJavaName(), method.getName(), method.getDeclaringClass().getSourceFileName(), 0);
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

    @Override
    public String toString()
    {
        return getClass().getName() + method.format("<%H.%n(%p)>");
    }
}
