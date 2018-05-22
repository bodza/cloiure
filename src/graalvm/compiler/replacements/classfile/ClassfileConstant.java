package graalvm.compiler.replacements.classfile;

import java.io.DataInputStream;
import java.io.IOException;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import graalvm.compiler.bytecode.Bytecodes;
import graalvm.compiler.debug.GraalError;

abstract class ClassfileConstant
{
    public static final byte CONSTANT_Utf8               = 1;
    public static final byte CONSTANT_Integer            = 3;
    public static final byte CONSTANT_Float              = 4;
    public static final byte CONSTANT_Long               = 5;
    public static final byte CONSTANT_Double             = 6;
    public static final byte CONSTANT_Class              = 7;
    public static final byte CONSTANT_Fieldref           = 9;
    public static final byte CONSTANT_String             = 8;
    public static final byte CONSTANT_Methodref          = 10;
    public static final byte CONSTANT_InterfaceMethodref = 11;
    public static final byte CONSTANT_NameAndType        = 12;
    public static final byte CONSTANT_MethodHandle       = 15;
    public static final byte CONSTANT_MethodType         = 16;
    public static final byte CONSTANT_Dynamic            = 17;
    public static final byte CONSTANT_InvokeDynamic      = 18;

    final byte tag;

    ClassfileConstant(byte tag)
    {
        this.tag = tag;
    }

    /**
     * Loads the type, if any, referenced at a specified entry.
     */
    public void loadReferencedType(ClassfileConstantPool cp, int index, int opcode)
    {
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }

    static class ClassRef extends ClassfileConstant
    {
        final int nameIndex;
        private ResolvedJavaType type;

        ClassRef(DataInputStream stream) throws IOException
        {
            super(CONSTANT_Class);
            this.nameIndex = stream.readUnsignedShort();
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool cp, int index, int opcode)
        {
            resolve(cp);
        }

        public ResolvedJavaType resolve(ClassfileConstantPool cp) throws GraalError
        {
            if (type == null)
            {
                String typeDescriptor = cp.get(Utf8.class, nameIndex).value;
                ClassfileBytecodeProvider context = cp.context;
                type = context.metaAccess.lookupJavaType(context.resolveToClass(typeDescriptor));
            }
            return type;
        }
    }

    static class MemberRef extends ClassfileConstant
    {
        final int classIndex;
        final int nameAndTypeIndex;

        MemberRef(byte tag, DataInputStream stream) throws IOException
        {
            super(tag);
            this.classIndex = stream.readUnsignedShort();
            this.nameAndTypeIndex = stream.readUnsignedShort();
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool cp, int index, int opcode)
        {
            cp.get(ClassRef.class, classIndex).loadReferencedType(cp, classIndex, opcode);
        }
    }

    static class ExecutableRef extends MemberRef
    {
        private ResolvedJavaMethod method;

        ExecutableRef(byte tag, DataInputStream stream) throws IOException
        {
            super(tag, stream);
        }

        ResolvedJavaMethod resolve(ClassfileConstantPool cp, int opcode)
        {
            if (method == null)
            {
                ResolvedJavaType cls = cp.get(ClassRef.class, classIndex).resolve(cp);
                NameAndType nameAndType = cp.get(NameAndType.class, nameAndTypeIndex);
                String name = nameAndType.getName(cp);
                String type = nameAndType.getType(cp);

                if (opcode == Bytecodes.INVOKEINTERFACE)
                {
                    method = resolveMethod(cp.context, cls, name, type, false);
                    if (method == null)
                    {
                        throw new NoSuchMethodError(cls.toJavaName() + "." + name + type);
                    }
                    if (!method.isPublic() || !(method.getDeclaringClass().isInterface() || method.getDeclaringClass().isJavaLangObject()))
                    {
                        throw new IncompatibleClassChangeError("cannot invokeinterface " + method.format("%H.%n(%P)%R"));
                    }
                }
                else if (opcode == Bytecodes.INVOKEVIRTUAL || opcode == Bytecodes.INVOKESPECIAL)
                {
                    method = resolveMethod(cp.context, cls, name, type, false);
                    if (method == null)
                    {
                        throw new NoSuchMethodError(cls.toJavaName() + "." + name + type);
                    }
                }
                else
                {
                    method = resolveMethod(cp.context, cls, name, type, true);
                    if (method == null)
                    {
                        throw new NoSuchMethodError(cls.toJavaName() + "." + name + type);
                    }
                }
            }
            return method;
        }
    }

    static class MethodRef extends ExecutableRef
    {
        MethodRef(DataInputStream stream) throws IOException
        {
            super(CONSTANT_Methodref, stream);
        }
    }

    static class InterfaceMethodRef extends ExecutableRef
    {
        InterfaceMethodRef(DataInputStream stream) throws IOException
        {
            super(CONSTANT_InterfaceMethodref, stream);
        }
    }

    static class FieldRef extends MemberRef
    {
        private ResolvedJavaField field;

        FieldRef(DataInputStream stream) throws IOException
        {
            super(CONSTANT_Fieldref, stream);
        }

        ResolvedJavaField resolve(ClassfileConstantPool cp, int opcode)
        {
            if (field == null)
            {
                ResolvedJavaType cls = cp.get(ClassRef.class, classIndex).resolve(cp);
                NameAndType nameAndType = cp.get(NameAndType.class, nameAndTypeIndex);
                String name = nameAndType.getName(cp);
                String type = nameAndType.getType(cp);
                field = resolveField(cp.context, cls, name, type, opcode == Bytecodes.GETSTATIC || opcode == Bytecodes.PUTSTATIC);
                if (field == null)
                {
                    throw new NoSuchFieldError(cls.toJavaName() + "." + name + " " + type);
                }
            }
            return field;
        }
    }

    static class Primitive extends ClassfileConstant
    {
        final JavaConstant value;

        Primitive(byte tag, JavaConstant value)
        {
            super(tag);
            this.value = value;
        }
    }

    static class StringRef extends ClassfileConstant
    {
        final int stringIndex;
        JavaConstant value;

        StringRef(DataInputStream stream) throws IOException
        {
            super(ClassfileConstant.CONSTANT_String);
            this.stringIndex = stream.readUnsignedShort();
        }

        JavaConstant getValue(ClassfileConstantPool pool)
        {
            if (value == null)
            {
                value = pool.context.snippetReflection.forObject(pool.lookupUtf8(stringIndex));
            }
            return value;
        }
    }

    static class NameAndType extends ClassfileConstant
    {
        final int nameIndex;
        final int typeIndex;
        private String name;
        private String type;

        NameAndType(DataInputStream stream) throws IOException
        {
            super(ClassfileConstant.CONSTANT_NameAndType);
            this.nameIndex = stream.readUnsignedShort();
            this.typeIndex = stream.readUnsignedShort();
        }

        public String getName(ClassfileConstantPool cp)
        {
            if (name == null)
            {
                name = cp.get(Utf8.class, nameIndex).value;
            }
            return name;
        }

        public String getType(ClassfileConstantPool cp)
        {
            if (type == null)
            {
                type = cp.get(Utf8.class, typeIndex).value;
            }
            return type;
        }
    }

    static class Utf8 extends ClassfileConstant
    {
        final String value;

        Utf8(String value)
        {
            super(CONSTANT_Utf8);
            this.value = value;
        }
    }

    static class Unsupported extends ClassfileConstant
    {
        final String name;

        Unsupported(byte tag, String name)
        {
            super(tag);
            this.name = name;
        }

        @Override
        public void loadReferencedType(ClassfileConstantPool cp, int index, int opcode)
        {
            throw new GraalError("Resolution of " + name + " constant pool entries not supported by " + ClassfileBytecodeProvider.class.getSimpleName());
        }
    }

    static ResolvedJavaMethod resolveMethod(ClassfileBytecodeProvider context, ResolvedJavaType c, String name, String descriptor, boolean isStatic)
    {
        ResolvedJavaMethod method = context.findMethod(c, name, descriptor, isStatic);
        if (method != null)
        {
            return method;
        }
        if (!c.isJavaLangObject() && !c.isInterface())
        {
            method = resolveMethod(context, c.getSuperclass(), name, descriptor, isStatic);
            if (method != null)
            {
                return method;
            }
        }
        for (ResolvedJavaType i : c.getInterfaces())
        {
            method = resolveMethod(context, i, name, descriptor, isStatic);
            if (method != null)
            {
                return method;
            }
        }
        return null;
    }

    static ResolvedJavaField resolveField(ClassfileBytecodeProvider context, ResolvedJavaType c, String name, String fieldType, boolean isStatic)
    {
        ResolvedJavaField field = context.findField(c, name, fieldType, isStatic);
        if (field != null)
        {
            return field;
        }
        if (!c.isJavaLangObject() && !c.isInterface())
        {
            field = resolveField(context, c.getSuperclass(), name, fieldType, isStatic);
            if (field != null)
            {
                return field;
            }
        }
        for (ResolvedJavaType i : c.getInterfaces())
        {
            field = resolveField(context, i, name, fieldType, isStatic);
            if (field != null)
            {
                return field;
            }
        }
        return null;
    }
}
