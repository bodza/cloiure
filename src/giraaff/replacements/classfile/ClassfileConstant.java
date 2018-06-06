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
    final byte ___tag;

    // @cons ClassfileConstant
    ClassfileConstant(byte __tag)
    {
        super();
        this.___tag = __tag;
    }

    ///
    // Loads the type, if any, referenced at a specified entry.
    ///
    public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
    {
    }

    // @class ClassfileConstant.ClassRef
    static final class ClassRef extends ClassfileConstant
    {
        // @field
        final int ___nameIndex;
        // @field
        private ResolvedJavaType ___type;

        // @cons ClassfileConstant.ClassRef
        ClassRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_Class);
            this.___nameIndex = __stream.readUnsignedShort();
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
        {
            resolve(__cp);
        }

        public ResolvedJavaType resolve(ClassfileConstantPool __cp)
        {
            if (this.___type == null)
            {
                String __typeDescriptor = __cp.get(ClassfileConstant.Utf8.class, this.___nameIndex).___value;
                ClassfileBytecodeProvider __context = __cp.___context;
                this.___type = __context.___metaAccess.lookupJavaType(__context.resolveToClass(__typeDescriptor));
            }
            return this.___type;
        }
    }

    // @class ClassfileConstant.MemberRef
    static class MemberRef extends ClassfileConstant
    {
        // @field
        final int ___classIndex;
        // @field
        final int ___nameAndTypeIndex;

        // @cons ClassfileConstant.MemberRef
        MemberRef(byte __tag, DataInputStream __stream) throws IOException
        {
            super(__tag);
            this.___classIndex = __stream.readUnsignedShort();
            this.___nameAndTypeIndex = __stream.readUnsignedShort();
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
        {
            __cp.get(ClassfileConstant.ClassRef.class, this.___classIndex).loadReferencedType(__cp, this.___classIndex, __opcode);
        }
    }

    // @class ClassfileConstant.ExecutableRef
    static class ExecutableRef extends ClassfileConstant.MemberRef
    {
        // @field
        private ResolvedJavaMethod ___method;

        // @cons ClassfileConstant.ExecutableRef
        ExecutableRef(byte __tag, DataInputStream __stream) throws IOException
        {
            super(__tag, __stream);
        }

        ResolvedJavaMethod resolve(ClassfileConstantPool __cp, int __opcode)
        {
            if (this.___method == null)
            {
                ResolvedJavaType __cls = __cp.get(ClassfileConstant.ClassRef.class, this.___classIndex).resolve(__cp);
                ClassfileConstant.NameAndType __nameAndType = __cp.get(ClassfileConstant.NameAndType.class, this.___nameAndTypeIndex);
                String __name = __nameAndType.getName(__cp);
                String __type = __nameAndType.getType(__cp);

                if (__opcode == Bytecodes.INVOKEINTERFACE)
                {
                    this.___method = resolveMethod(__cp.___context, __cls, __name, __type, false);
                    if (this.___method == null)
                    {
                        throw new NoSuchMethodError(__cls.toJavaName() + "." + __name + __type);
                    }
                    if (!this.___method.isPublic() || !(this.___method.getDeclaringClass().isInterface() || this.___method.getDeclaringClass().isJavaLangObject()))
                    {
                        throw new IncompatibleClassChangeError("cannot invokeinterface " + this.___method.format("%H.%n(%P)%R"));
                    }
                }
                else if (__opcode == Bytecodes.INVOKEVIRTUAL || __opcode == Bytecodes.INVOKESPECIAL)
                {
                    this.___method = resolveMethod(__cp.___context, __cls, __name, __type, false);
                    if (this.___method == null)
                    {
                        throw new NoSuchMethodError(__cls.toJavaName() + "." + __name + __type);
                    }
                }
                else
                {
                    this.___method = resolveMethod(__cp.___context, __cls, __name, __type, true);
                    if (this.___method == null)
                    {
                        throw new NoSuchMethodError(__cls.toJavaName() + "." + __name + __type);
                    }
                }
            }
            return this.___method;
        }
    }

    // @class ClassfileConstant.MethodRef
    static final class MethodRef extends ClassfileConstant.ExecutableRef
    {
        // @cons ClassfileConstant.MethodRef
        MethodRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_Methodref, __stream);
        }
    }

    // @class ClassfileConstant.InterfaceMethodRef
    static final class InterfaceMethodRef extends ClassfileConstant.ExecutableRef
    {
        // @cons ClassfileConstant.InterfaceMethodRef
        InterfaceMethodRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_InterfaceMethodref, __stream);
        }
    }

    // @class ClassfileConstant.FieldRef
    static final class FieldRef extends ClassfileConstant.MemberRef
    {
        // @field
        private ResolvedJavaField ___field;

        // @cons ClassfileConstant.FieldRef
        FieldRef(DataInputStream __stream) throws IOException
        {
            super(CONSTANT_Fieldref, __stream);
        }

        ResolvedJavaField resolve(ClassfileConstantPool __cp, int __opcode)
        {
            if (this.___field == null)
            {
                ResolvedJavaType __cls = __cp.get(ClassfileConstant.ClassRef.class, this.___classIndex).resolve(__cp);
                ClassfileConstant.NameAndType __nameAndType = __cp.get(ClassfileConstant.NameAndType.class, this.___nameAndTypeIndex);
                String __name = __nameAndType.getName(__cp);
                String __type = __nameAndType.getType(__cp);
                this.___field = resolveField(__cp.___context, __cls, __name, __type, __opcode == Bytecodes.GETSTATIC || __opcode == Bytecodes.PUTSTATIC);
                if (this.___field == null)
                {
                    throw new NoSuchFieldError(__cls.toJavaName() + "." + __name + " " + __type);
                }
            }
            return this.___field;
        }
    }

    // @class ClassfileConstant.Primitive
    static final class Primitive extends ClassfileConstant
    {
        // @field
        final JavaConstant ___value;

        // @cons ClassfileConstant.Primitive
        Primitive(byte __tag, JavaConstant __value)
        {
            super(__tag);
            this.___value = __value;
        }
    }

    // @class ClassfileConstant.StringRef
    static final class StringRef extends ClassfileConstant
    {
        // @field
        final int ___stringIndex;
        // @field
        JavaConstant ___value;

        // @cons ClassfileConstant.StringRef
        StringRef(DataInputStream __stream) throws IOException
        {
            super(ClassfileConstant.CONSTANT_String);
            this.___stringIndex = __stream.readUnsignedShort();
        }

        JavaConstant getValue(ClassfileConstantPool __pool)
        {
            if (this.___value == null)
            {
                this.___value = __pool.___context.___snippetReflection.forObject(__pool.lookupUtf8(this.___stringIndex));
            }
            return this.___value;
        }
    }

    // @class ClassfileConstant.NameAndType
    static final class NameAndType extends ClassfileConstant
    {
        // @field
        final int ___nameIndex;
        // @field
        final int ___typeIndex;
        // @field
        private String ___name;
        // @field
        private String ___type;

        // @cons ClassfileConstant.NameAndType
        NameAndType(DataInputStream __stream) throws IOException
        {
            super(ClassfileConstant.CONSTANT_NameAndType);
            this.___nameIndex = __stream.readUnsignedShort();
            this.___typeIndex = __stream.readUnsignedShort();
        }

        public String getName(ClassfileConstantPool __cp)
        {
            if (this.___name == null)
            {
                this.___name = __cp.get(ClassfileConstant.Utf8.class, this.___nameIndex).___value;
            }
            return this.___name;
        }

        public String getType(ClassfileConstantPool __cp)
        {
            if (this.___type == null)
            {
                this.___type = __cp.get(ClassfileConstant.Utf8.class, this.___typeIndex).___value;
            }
            return this.___type;
        }
    }

    // @class ClassfileConstant.Utf8
    static final class Utf8 extends ClassfileConstant
    {
        // @field
        final String ___value;

        // @cons ClassfileConstant.Utf8
        Utf8(String __value)
        {
            super(CONSTANT_Utf8);
            this.___value = __value;
        }
    }

    // @class ClassfileConstant.Unsupported
    static final class Unsupported extends ClassfileConstant
    {
        // @field
        final String ___name;

        // @cons ClassfileConstant.Unsupported
        Unsupported(byte __tag, String __name)
        {
            super(__tag);
            this.___name = __name;
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool __cp, int __index, int __opcode)
        {
            throw new GraalError("Resolution of " + this.___name + " constant pool entries not supported by " + ClassfileBytecodeProvider.class.getSimpleName());
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
