package giraaff.replacements.classfile;

import java.io.DataInputStream;
import java.io.IOException;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.bytecode.Bytecodes;
import giraaff.util.GraalError;

// @class ClassfileConstant
abstract class ClassfileConstant
{
    // @def
    public static final byte CONSTANT_Utf8               = 1;
    // @def
    public static final byte CONSTANT_Integer            = 3;
    // @def
    public static final byte CONSTANT_Float              = 4;
    // @def
    public static final byte CONSTANT_Long               = 5;
    // @def
    public static final byte CONSTANT_Double             = 6;
    // @def
    public static final byte CONSTANT_Class              = 7;
    // @def
    public static final byte CONSTANT_Fieldref           = 9;
    // @def
    public static final byte CONSTANT_String             = 8;
    // @def
    public static final byte CONSTANT_Methodref          = 10;
    // @def
    public static final byte CONSTANT_InterfaceMethodref = 11;
    // @def
    public static final byte CONSTANT_NameAndType        = 12;
    // @def
    public static final byte CONSTANT_MethodHandle       = 15;
    // @def
    public static final byte CONSTANT_MethodType         = 16;
    // @def
    public static final byte CONSTANT_Dynamic            = 17;
    // @def
    public static final byte CONSTANT_InvokeDynamic      = 18;

    // @field
    final byte tag;

    // @cons
    ClassfileConstant(byte __tag)
    {
        super();
        this.tag = __tag;
    }

    /**
     * Loads the type, if any, referenced at a specified entry.
     */
    public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
    {
    }

    // @class ClassfileConstant.ClassRef
    static final class ClassRef extends ClassfileConstant
    {
        // @field
        final int nameIndex;
        // @field
        private ResolvedJavaType type;

        // @cons
        ClassRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_Class);
            this.nameIndex = __stream.readUnsignedShort();
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
        {
            resolve(__cp);
        }

        public ResolvedJavaType resolve(ClassfileConstantPool __cp)
        {
            if (type == null)
            {
                String __typeDescriptor = __cp.get(Utf8.class, nameIndex).value;
                ClassfileBytecodeProvider __context = __cp.context;
                type = __context.metaAccess.lookupJavaType(__context.resolveToClass(__typeDescriptor));
            }
            return type;
        }
    }

    // @class ClassfileConstant.MemberRef
    static class MemberRef extends ClassfileConstant
    {
        // @field
        final int classIndex;
        // @field
        final int nameAndTypeIndex;

        // @cons
        MemberRef(byte __tag, DataInputStream __stream) throws IOException
        {
            super(__tag);
            this.classIndex = __stream.readUnsignedShort();
            this.nameAndTypeIndex = __stream.readUnsignedShort();
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
        {
            __cp.get(ClassRef.class, classIndex).loadReferencedType(__cp, classIndex, __opcode);
        }
    }

    // @class ClassfileConstant.ExecutableRef
    static class ExecutableRef extends MemberRef
    {
        // @field
        private ResolvedJavaMethod method;

        // @cons
        ExecutableRef(byte __tag, DataInputStream __stream) throws IOException
        {
            super(__tag, __stream);
        }

        ResolvedJavaMethod resolve(ClassfileConstantPool __cp, int __opcode)
        {
            if (method == null)
            {
                ResolvedJavaType __cls = __cp.get(ClassRef.class, classIndex).resolve(__cp);
                NameAndType __nameAndType = __cp.get(NameAndType.class, nameAndTypeIndex);
                String __name = __nameAndType.getName(__cp);
                String __type = __nameAndType.getType(__cp);

                if (__opcode == Bytecodes.INVOKEINTERFACE)
                {
                    method = resolveMethod(__cp.context, __cls, __name, __type, false);
                    if (method == null)
                    {
                        throw new NoSuchMethodError(__cls.toJavaName() + "." + __name + __type);
                    }
                    if (!method.isPublic() || !(method.getDeclaringClass().isInterface() || method.getDeclaringClass().isJavaLangObject()))
                    {
                        throw new IncompatibleClassChangeError("cannot invokeinterface " + method.format("%H.%n(%P)%R"));
                    }
                }
                else if (__opcode == Bytecodes.INVOKEVIRTUAL || __opcode == Bytecodes.INVOKESPECIAL)
                {
                    method = resolveMethod(__cp.context, __cls, __name, __type, false);
                    if (method == null)
                    {
                        throw new NoSuchMethodError(__cls.toJavaName() + "." + __name + __type);
                    }
                }
                else
                {
                    method = resolveMethod(__cp.context, __cls, __name, __type, true);
                    if (method == null)
                    {
                        throw new NoSuchMethodError(__cls.toJavaName() + "." + __name + __type);
                    }
                }
            }
            return method;
        }
    }

    // @class ClassfileConstant.MethodRef
    static final class MethodRef extends ExecutableRef
    {
        // @cons
        MethodRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_Methodref, __stream);
        }
    }

    // @class ClassfileConstant.InterfaceMethodRef
    static final class InterfaceMethodRef extends ExecutableRef
    {
        // @cons
        InterfaceMethodRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_InterfaceMethodref, __stream);
        }
    }

    // @class ClassfileConstant.FieldRef
    static final class FieldRef extends MemberRef
    {
        // @field
        private ResolvedJavaField field;

        // @cons
        FieldRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_Fieldref, __stream);
        }

        ResolvedJavaField resolve(ClassfileConstantPool __cp, int __opcode)
        {
            if (field == null)
            {
                ResolvedJavaType __cls = __cp.get(ClassRef.class, classIndex).resolve(__cp);
                NameAndType __nameAndType = __cp.get(NameAndType.class, nameAndTypeIndex);
                String __name = __nameAndType.getName(__cp);
                String __type = __nameAndType.getType(__cp);
                field = resolveField(__cp.context, __cls, __name, __type, __opcode == Bytecodes.GETSTATIC || __opcode == Bytecodes.PUTSTATIC);
                if (field == null)
                {
                    throw new NoSuchFieldError(__cls.toJavaName() + "." + __name + " " + __type);
                }
            }
            return field;
        }
    }

    // @class ClassfileConstant.Primitive
    static final class Primitive extends ClassfileConstant
    {
        // @field
        final JavaConstant value;

        // @cons
        Primitive(byte __tag, JavaConstant __value)
        {
            super(__tag);
            this.value = __value;
        }
    }

    // @class ClassfileConstant.StringRef
    static final class StringRef extends ClassfileConstant
    {
        // @field
        final int stringIndex;
        // @field
        JavaConstant value;

        // @cons
        StringRef(DataInputStream __stream) throws IOException
        {
            super(ClassfileConstant.CONSTANT_String);
            this.stringIndex = __stream.readUnsignedShort();
        }

        JavaConstant getValue(ClassfileConstantPool __pool)
        {
            if (value == null)
            {
                value = __pool.context.snippetReflection.forObject(__pool.lookupUtf8(stringIndex));
            }
            return value;
        }
    }

    // @class ClassfileConstant.NameAndType
    static final class NameAndType extends ClassfileConstant
    {
        // @field
        final int nameIndex;
        // @field
        final int typeIndex;
        // @field
        private String name;
        // @field
        private String type;

        // @cons
        NameAndType(DataInputStream __stream) throws IOException
        {
            super(ClassfileConstant.CONSTANT_NameAndType);
            this.nameIndex = __stream.readUnsignedShort();
            this.typeIndex = __stream.readUnsignedShort();
        }

        public String getName(ClassfileConstantPool __cp)
        {
            if (name == null)
            {
                name = __cp.get(Utf8.class, nameIndex).value;
            }
            return name;
        }

        public String getType(ClassfileConstantPool __cp)
        {
            if (type == null)
            {
                type = __cp.get(Utf8.class, typeIndex).value;
            }
            return type;
        }
    }

    // @class ClassfileConstant.Utf8
    static final class Utf8 extends ClassfileConstant
    {
        // @field
        final String value;

        // @cons
        Utf8(String __value)
        {
            super(CONSTANT_Utf8);
            this.value = __value;
        }
    }

    // @class ClassfileConstant.Unsupported
    static final class Unsupported extends ClassfileConstant
    {
        // @field
        final String name;

        // @cons
        Unsupported(byte __tag, String __name)
        {
            super(__tag);
            this.name = __name;
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
        {
            throw new GraalError("Resolution of " + name + " constant pool entries not supported by " + ClassfileBytecodeProvider.class.getSimpleName());
        }
    }

    static ResolvedJavaMethod resolveMethod(ClassfileBytecodeProvider __context, ResolvedJavaType __c, String __name, String __descriptor, boolean __isStatic)
    {
        ResolvedJavaMethod __method = __context.findMethod(__c, __name, __descriptor, __isStatic);
        if (__method != null)
        {
            return __method;
        }
        if (!__c.isJavaLangObject() && !__c.isInterface())
        {
            __method = resolveMethod(__context, __c.getSuperclass(), __name, __descriptor, __isStatic);
            if (__method != null)
            {
                return __method;
            }
        }
        for (ResolvedJavaType __i : __c.getInterfaces())
        {
            __method = resolveMethod(__context, __i, __name, __descriptor, __isStatic);
            if (__method != null)
            {
                return __method;
            }
        }
        return null;
    }

    static ResolvedJavaField resolveField(ClassfileBytecodeProvider __context, ResolvedJavaType __c, String __name, String __fieldType, boolean __isStatic)
    {
        ResolvedJavaField __field = __context.findField(__c, __name, __fieldType, __isStatic);
        if (__field != null)
        {
            return __field;
        }
        if (!__c.isJavaLangObject() && !__c.isInterface())
        {
            __field = resolveField(__context, __c.getSuperclass(), __name, __fieldType, __isStatic);
            if (__field != null)
            {
                return __field;
            }
        }
        for (ResolvedJavaType __i : __c.getInterfaces())
        {
            __field = resolveField(__context, __i, __name, __fieldType, __isStatic);
            if (__field != null)
            {
                return __field;
            }
        }
        return null;
    }
}
