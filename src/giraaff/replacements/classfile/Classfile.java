package giraaff.replacements.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.replacements.classfile.ClassfileConstant.Utf8;

/**
 * Container for objects representing the {@code Code} attributes parsed from a class file.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.3">Code attributes</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4">Constant Pool</a>
 */
// @class Classfile
public final class Classfile
{
    // @field
    private final ResolvedJavaType type;
    // @field
    private final List<ClassfileBytecode> codeAttributes;

    // @def
    private static final int MAJOR_VERSION_JAVA_MIN = 51; // JDK7
    // @def
    private static final int MAJOR_VERSION_JAVA_MAX = 55; // JDK11
    // @def
    private static final int MAGIC = 0xCAFEBABE;

    /**
     * Creates a {@link Classfile} by parsing the class file bytes for {@code type} loadable from {@code context}.
     *
     * @throws NoClassDefFoundError if there is an IO error while parsing the class file
     */
    // @cons
    public Classfile(ResolvedJavaType __type, DataInputStream __stream, ClassfileBytecodeProvider __context) throws IOException
    {
        super();
        this.type = __type;

        // magic
        int __magic = __stream.readInt();

        int __minor = __stream.readUnsignedShort();
        int __major = __stream.readUnsignedShort();
        if (__major < MAJOR_VERSION_JAVA_MIN || __major > MAJOR_VERSION_JAVA_MAX)
        {
            throw new UnsupportedClassVersionError("Unsupported class file version: " + __major + "." + __minor);
        }

        ClassfileConstantPool __cp = new ClassfileConstantPool(__stream, __context);

        // access_flags, this_class, super_class
        skipFully(__stream, 6);

        // interfaces
        skipFully(__stream, __stream.readUnsignedShort() * 2);

        // fields
        skipFields(__stream);

        // methods
        codeAttributes = readMethods(__stream, __cp);

        // attributes
        skipAttributes(__stream);
    }

    public ClassfileBytecode getCode(String __name, String __descriptor)
    {
        for (ClassfileBytecode __code : codeAttributes)
        {
            ResolvedJavaMethod __method = __code.getMethod();
            if (__method.getName().equals(__name) && __method.getSignature().toMethodDescriptor().equals(__descriptor))
            {
                return __code;
            }
        }
        throw new NoSuchMethodError(type.toJavaName() + "." + __name + __descriptor);
    }

    private static void skipAttributes(DataInputStream __stream) throws IOException
    {
        int __attributesCount;
        __attributesCount = __stream.readUnsignedShort();
        for (int __i = 0; __i < __attributesCount; __i++)
        {
            skipFully(__stream, 2); // name_index
            int __attributeLength = __stream.readInt();
            skipFully(__stream, __attributeLength);
        }
    }

    static void skipFully(DataInputStream __stream, int __n) throws IOException
    {
        long __skipped = 0;
        do
        {
            long __s = __stream.skip(__n - __skipped);
            __skipped += __s;
            if (__s == 0 && __skipped != __n)
            {
                // check for EOF (i.e. truncated class file)
                if (__stream.read() == -1)
                {
                    throw new IOException("truncated stream");
                }
                __skipped++;
            }
        } while (__skipped != __n);
    }

    private ClassfileBytecode findCodeAttribute(DataInputStream __stream, ClassfileConstantPool __cp, String __name, String __descriptor, boolean __isStatic) throws IOException
    {
        int __attributesCount;
        __attributesCount = __stream.readUnsignedShort();
        ClassfileBytecode __code = null;
        for (int __i = 0; __i < __attributesCount; __i++)
        {
            String __attributeName = __cp.get(Utf8.class, __stream.readUnsignedShort()).value;
            int __attributeLength = __stream.readInt();
            if (__code == null && __attributeName.equals("Code"))
            {
                ResolvedJavaMethod __method = __cp.context.findMethod(type, __name, __descriptor, __isStatic);
                // Even if we will discard the Code attribute (see below), we still need to parse it
                // to reach the following class file content.
                __code = new ClassfileBytecode(__method, __stream, __cp);
                if (__method == null)
                {
                    // this is a method hidden from reflection (see sun.reflect.Reflection.filterMethods)
                    __code = null;
                }
            }
            else
            {
                skipFully(__stream, __attributeLength);
            }
        }
        return __code;
    }

    private static void skipFields(DataInputStream __stream) throws IOException
    {
        int __count = __stream.readUnsignedShort();
        for (int __i = 0; __i < __count; __i++)
        {
            skipFully(__stream, 6); // access_flags, name_index, descriptor_index
            skipAttributes(__stream);
        }
    }

    private List<ClassfileBytecode> readMethods(DataInputStream __stream, ClassfileConstantPool __cp) throws IOException
    {
        int __count = __stream.readUnsignedShort();
        List<ClassfileBytecode> __result = new ArrayList<>(__count);
        for (int __i = 0; __i < __count; __i++)
        {
            int __accessFlags = __stream.readUnsignedShort();
            boolean __isStatic = Modifier.isStatic(__accessFlags);
            String __name = __cp.get(Utf8.class, __stream.readUnsignedShort()).value;
            String __descriptor = __cp.get(Utf8.class, __stream.readUnsignedShort()).value;
            ClassfileBytecode __code = findCodeAttribute(__stream, __cp, __name, __descriptor, __isStatic);
            if (__code != null)
            {
                __result.add(__code);
            }
        }
        return __result;
    }
}
