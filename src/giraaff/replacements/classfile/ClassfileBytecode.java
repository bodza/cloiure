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

///
// The bytecode properties of a method as parsed directly from a class file without any
// {@linkplain java.lang.instrument.Instrumentation instrumentation} or other rewriting performed on
// the bytecode.
///
// @class ClassfileBytecode
public final class ClassfileBytecode implements Bytecode
{
    // @def
    private static final int EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES = 8;

    // @field
    private final ResolvedJavaMethod ___method;

    // @field
    private final ClassfileConstantPool ___constantPool;

    // @field
    private byte[] ___code;
    // @field
    private int ___maxLocals;
    // @field
    private int ___maxStack;

    // @field
    private byte[] ___exceptionTableBytes;

    // @cons ClassfileBytecode
    public ClassfileBytecode(ResolvedJavaMethod __method, DataInputStream __stream, ClassfileConstantPool __constantPool) throws IOException
    {
        super();
        this.___method = __method;
        this.___constantPool = __constantPool;
        this.___maxStack = __stream.readUnsignedShort();
        this.___maxLocals = __stream.readUnsignedShort();
        int __codeLength = __stream.readInt();
        this.___code = new byte[__codeLength];
        __stream.readFully(this.___code);
        int __exceptionTableLength = __stream.readUnsignedShort();
        this.___exceptionTableBytes = new byte[__exceptionTableLength * EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES];
        __stream.readFully(this.___exceptionTableBytes);
        skipCodeAttributes(__stream);
    }

    @Override
    public BytecodeProvider getOrigin()
    {
        return this.___constantPool.___context;
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
        return this.___code;
    }

    @Override
    public int getCodeSize()
    {
        return this.___code.length;
    }

    @Override
    public int getMaxLocals()
    {
        return this.___maxLocals;
    }

    @Override
    public int getMaxStackSize()
    {
        return this.___maxStack;
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers()
    {
        if (this.___exceptionTableBytes == null)
        {
            return new ExceptionHandler[0];
        }

        final int __exceptionTableLength = this.___exceptionTableBytes.length / EXCEPTION_HANDLER_TABLE_SIZE_IN_BYTES;
        ExceptionHandler[] __handlers = new ExceptionHandler[__exceptionTableLength];
        DataInputStream __stream = new DataInputStream(new ByteArrayInputStream(this.___exceptionTableBytes));

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
                    __catchType = this.___constantPool.lookupType(__catchTypeIndex, __opcode);

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
        return this.___constantPool;
    }

    @Override
    public ResolvedJavaMethod getMethod()
    {
        return this.___method;
    }

    @Override
    public ProfilingInfo getProfilingInfo()
    {
        return DefaultProfilingInfo.get(TriState.FALSE);
    }
}
