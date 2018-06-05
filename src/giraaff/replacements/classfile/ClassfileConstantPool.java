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
    // @field
    final ClassfileConstant[] ___entries;
    // @field
    final ClassfileBytecodeProvider ___context;

    // @class ClassfileConstantPool.Bytecodes
    public static final class Bytecodes
    {
        // @cons
        private Bytecodes()
        {
            super();
        }

        // @defs
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
    ClassfileConstantPool(DataInputStream __stream, ClassfileBytecodeProvider __context) throws IOException
    {
        super();
        this.___context = __context;
        byte __tag;

        int __count = __stream.readUnsignedShort();
        this.___entries = new ClassfileConstant[__count];

        int __i = 1;
        while (__i < __count)
        {
            this.___entries[__i] = readConstant(__stream);
            __tag = this.___entries[__i].___tag;

            if ((__tag == ClassfileConstant.CONSTANT_Double) || (__tag == ClassfileConstant.CONSTANT_Long))
            {
                __i += 2;
            }
            else
            {
                __i += 1;
            }
        }
    }

    static final ClassfileConstant readConstant(DataInputStream __stream) throws IOException
    {
        byte __tag = __stream.readByte();

        switch (__tag)
        {
            case ClassfileConstant.CONSTANT_Class:
                return new ClassfileConstant.ClassRef(__stream);
            case ClassfileConstant.CONSTANT_Fieldref:
                return new ClassfileConstant.FieldRef(__stream);
            case ClassfileConstant.CONSTANT_Methodref:
                return new ClassfileConstant.MethodRef(__stream);
            case ClassfileConstant.CONSTANT_InterfaceMethodref:
                return new ClassfileConstant.InterfaceMethodRef(__stream);
            case ClassfileConstant.CONSTANT_String:
                return new ClassfileConstant.StringRef(__stream);
            case ClassfileConstant.CONSTANT_Integer:
                return new ClassfileConstant.Primitive(__tag, JavaConstant.forInt(__stream.readInt()));
            case ClassfileConstant.CONSTANT_Float:
                throw GraalError.shouldNotReachHere();
            case ClassfileConstant.CONSTANT_Long:
                return new ClassfileConstant.Primitive(__tag, JavaConstant.forLong(__stream.readLong()));
            case ClassfileConstant.CONSTANT_Double:
                throw GraalError.shouldNotReachHere();
            case ClassfileConstant.CONSTANT_NameAndType:
                return new ClassfileConstant.NameAndType(__stream);
            case ClassfileConstant.CONSTANT_Utf8:
                return new ClassfileConstant.Utf8(__stream.readUTF());
            case ClassfileConstant.CONSTANT_MethodHandle:
            {
                Classfile.skipFully(__stream, 3); // reference_kind, reference_index
                return new ClassfileConstant.Unsupported(__tag, "CONSTANT_MethodHandle_info");
            }
            case ClassfileConstant.CONSTANT_MethodType:
            {
                Classfile.skipFully(__stream, 2); // descriptor_index
                return new ClassfileConstant.Unsupported(__tag, "CONSTANT_MethodType_info");
            }
            case ClassfileConstant.CONSTANT_Dynamic:
            {
                Classfile.skipFully(__stream, 4); // bootstrap_method_attr_index, name_and_type_index
                return new ClassfileConstant.Unsupported(__tag, "CONSTANT_Dynamic_info");
            }
            case ClassfileConstant.CONSTANT_InvokeDynamic:
            {
                Classfile.skipFully(__stream, 4); // bootstrap_method_attr_index, name_and_type_index
                return new ClassfileConstant.Unsupported(__tag, "CONSTANT_InvokeDynamic_info");
            }
            default:
                throw new GraalError("Invalid constant pool tag: " + __tag);
        }
    }

    @Override
    public int length()
    {
        return this.___entries.length;
    }

    <T extends ClassfileConstant> T get(Class<T> __c, int __index)
    {
        return __c.cast(this.___entries[__index]);
    }

    @Override
    public void loadReferencedType(int __index, int __opcode)
    {
        if (__opcode == Bytecodes.INVOKEDYNAMIC)
        {
            throw new GraalError("INVOKEDYNAMIC not supported by " + ClassfileBytecodeProvider.class.getSimpleName());
        }
        this.___entries[__index].loadReferencedType(this, __index, __opcode);
    }

    @Override
    public JavaField lookupField(int __index, ResolvedJavaMethod __method, int __opcode)
    {
        return get(FieldRef.class, __index).resolve(this, __opcode);
    }

    @Override
    public JavaMethod lookupMethod(int __index, int __opcode)
    {
        if (__opcode == Bytecodes.INVOKEDYNAMIC)
        {
            throw new GraalError("INVOKEDYNAMIC not supported by" + ClassfileBytecodeProvider.class.getSimpleName());
        }
        return get(ExecutableRef.class, __index).resolve(this, __opcode);
    }

    @Override
    public JavaType lookupType(int __index, int __opcode)
    {
        return get(ClassRef.class, __index).resolve(this);
    }

    @Override
    public String lookupUtf8(int __index)
    {
        return ((Utf8) this.___entries[__index]).___value;
    }

    @Override
    public Signature lookupSignature(int __index)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public Object lookupConstant(int __index)
    {
        ClassfileConstant __c = this.___entries[__index];
        if (__c instanceof Primitive)
        {
            Primitive __p = (Primitive) __c;
            return __p.___value;
        }
        switch (__c.___tag)
        {
            case ClassfileConstant.CONSTANT_Class:
                return lookupType(__index, -1);
            case ClassfileConstant.CONSTANT_String:
                return ((ClassfileConstant.StringRef) __c).getValue(this);
            default:
                throw new GraalError("unexpected constant pool tag %s", __c.___tag);
        }
    }

    @Override
    public JavaConstant lookupAppendix(int __index, int __opcode)
    {
        if (__opcode == Bytecodes.INVOKEVIRTUAL)
        {
            return null;
        }
        throw GraalError.shouldNotReachHere();
    }
}
