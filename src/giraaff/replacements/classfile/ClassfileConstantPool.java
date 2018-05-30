package giraaff.replacements.classfile;

import java.io.DataInputStream;
import java.io.IOException;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaField;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Signature;

import giraaff.replacements.classfile.Classfile;
import giraaff.replacements.classfile.ClassfileConstant;
import giraaff.replacements.classfile.ClassfileConstant.ClassRef;
import giraaff.replacements.classfile.ClassfileConstant.ExecutableRef;
import giraaff.replacements.classfile.ClassfileConstant.FieldRef;
import giraaff.replacements.classfile.ClassfileConstant.Primitive;
import giraaff.replacements.classfile.ClassfileConstant.Utf8;
import giraaff.util.GraalError;

// @class ClassfileConstantPool
final class ClassfileConstantPool implements ConstantPool
{
    final ClassfileConstant[] entries;
    final ClassfileBytecodeProvider context;

    // @class ClassfileConstantPool.Bytecodes
    public static final class Bytecodes
    {
        // @cons
        private Bytecodes()
        {
            super();
        }
    
        public static final int
            GETSTATIC       = 178, // 0xB2
            PUTSTATIC       = 179, // 0xB3
            GETFIELD        = 180, // 0xB4
            PUTFIELD        = 181, // 0xB5
            INVOKEVIRTUAL   = 182, // 0xB6
            INVOKESPECIAL   = 183, // 0xB7
            INVOKESTATIC    = 184, // 0xB8
            INVOKEINTERFACE = 185, // 0xB9
            INVOKEDYNAMIC   = 186; // 0xBA
    }

    // @cons
    ClassfileConstantPool(DataInputStream stream, ClassfileBytecodeProvider context) throws IOException
    {
        super();
        this.context = context;
        byte tag;

        int count = stream.readUnsignedShort();
        entries = new ClassfileConstant[count];

        int i = 1;
        while (i < count)
        {
            entries[i] = readConstant(stream);
            tag = entries[i].tag;

            if ((tag == ClassfileConstant.CONSTANT_Double) || (tag == ClassfileConstant.CONSTANT_Long))
            {
                i += 2;
            }
            else
            {
                i += 1;
            }
        }
    }

    static final ClassfileConstant readConstant(DataInputStream stream) throws IOException
    {
        byte tag = stream.readByte();

        switch (tag)
        {
            case ClassfileConstant.CONSTANT_Class:
                return new ClassfileConstant.ClassRef(stream);
            case ClassfileConstant.CONSTANT_Fieldref:
                return new ClassfileConstant.FieldRef(stream);
            case ClassfileConstant.CONSTANT_Methodref:
                return new ClassfileConstant.MethodRef(stream);
            case ClassfileConstant.CONSTANT_InterfaceMethodref:
                return new ClassfileConstant.InterfaceMethodRef(stream);
            case ClassfileConstant.CONSTANT_String:
                return new ClassfileConstant.StringRef(stream);
            case ClassfileConstant.CONSTANT_Integer:
                return new ClassfileConstant.Primitive(tag, JavaConstant.forInt(stream.readInt()));
            case ClassfileConstant.CONSTANT_Float:
                return new ClassfileConstant.Primitive(tag, JavaConstant.forFloat(stream.readFloat()));
            case ClassfileConstant.CONSTANT_Long:
                return new ClassfileConstant.Primitive(tag, JavaConstant.forLong(stream.readLong()));
            case ClassfileConstant.CONSTANT_Double:
                return new ClassfileConstant.Primitive(tag, JavaConstant.forDouble(stream.readDouble()));
            case ClassfileConstant.CONSTANT_NameAndType:
                return new ClassfileConstant.NameAndType(stream);
            case ClassfileConstant.CONSTANT_Utf8:
                return new ClassfileConstant.Utf8(stream.readUTF());
            case ClassfileConstant.CONSTANT_MethodHandle:
                Classfile.skipFully(stream, 3); // reference_kind, reference_index
                return new ClassfileConstant.Unsupported(tag, "CONSTANT_MethodHandle_info");
            case ClassfileConstant.CONSTANT_MethodType:
                Classfile.skipFully(stream, 2); // descriptor_index
                return new ClassfileConstant.Unsupported(tag, "CONSTANT_MethodType_info");
            case ClassfileConstant.CONSTANT_Dynamic:
                Classfile.skipFully(stream, 4); // bootstrap_method_attr_index, name_and_type_index
                return new ClassfileConstant.Unsupported(tag, "CONSTANT_Dynamic_info");
            case ClassfileConstant.CONSTANT_InvokeDynamic:
                Classfile.skipFully(stream, 4); // bootstrap_method_attr_index, name_and_type_index
                return new ClassfileConstant.Unsupported(tag, "CONSTANT_InvokeDynamic_info");
            default:
                throw new GraalError("Invalid constant pool tag: " + tag);
        }
    }

    @Override
    public int length()
    {
        return entries.length;
    }

    <T extends ClassfileConstant> T get(Class<T> c, int index)
    {
        return c.cast(entries[index]);
    }

    @Override
    public void loadReferencedType(int index, int opcode)
    {
        if (opcode == Bytecodes.INVOKEDYNAMIC)
        {
            throw new GraalError("INVOKEDYNAMIC not supported by " + ClassfileBytecodeProvider.class.getSimpleName());
        }
        entries[index].loadReferencedType(this, index, opcode);
    }

    @Override
    public JavaField lookupField(int index, ResolvedJavaMethod method, int opcode)
    {
        return get(FieldRef.class, index).resolve(this, opcode);
    }

    @Override
    public JavaMethod lookupMethod(int index, int opcode)
    {
        if (opcode == Bytecodes.INVOKEDYNAMIC)
        {
            throw new GraalError("INVOKEDYNAMIC not supported by" + ClassfileBytecodeProvider.class.getSimpleName());
        }
        return get(ExecutableRef.class, index).resolve(this, opcode);
    }

    @Override
    public JavaType lookupType(int index, int opcode)
    {
        return get(ClassRef.class, index).resolve(this);
    }

    @Override
    public String lookupUtf8(int index)
    {
        return ((Utf8) entries[index]).value;
    }

    @Override
    public Signature lookupSignature(int index)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public Object lookupConstant(int index)
    {
        ClassfileConstant c = entries[index];
        if (c instanceof Primitive)
        {
            Primitive p = (Primitive) c;
            return p.value;
        }
        switch (c.tag)
        {
            case ClassfileConstant.CONSTANT_Class:
                final int opcode = -1;
                return lookupType(index, opcode);
            case ClassfileConstant.CONSTANT_String:
                return ((ClassfileConstant.StringRef) c).getValue(this);
            default:
                throw new GraalError("Unexpected constant pool tag %s", c.tag);
        }
    }

    @Override
    public JavaConstant lookupAppendix(int index, int opcode)
    {
        if (opcode == Bytecodes.INVOKEVIRTUAL)
        {
            return null;
        }
        throw GraalError.shouldNotReachHere();
    }
}
