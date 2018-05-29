package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.util.GraalError;

/**
 * This stamp represents the type of the {@link JavaKind#Illegal} value in the second slot of
 * {@link JavaKind#Long} and {@link JavaKind#Double} values. It can only appear in framestates or
 * virtual objects.
 */
// @class IllegalStamp
public final class IllegalStamp extends Stamp
{
    // @cons
    private IllegalStamp()
    {
        super();
    }

    @Override
    public JavaKind getStackKind()
    {
        return JavaKind.Illegal;
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool tool)
    {
        return LIRKind.Illegal;
    }

    @Override
    public Stamp unrestricted()
    {
        return this;
    }

    @Override
    public boolean isUnrestricted()
    {
        return true;
    }

    @Override
    public Stamp empty()
    {
        return this;
    }

    @Override
    public Stamp constant(Constant c, MetaAccessProvider meta)
    {
        return this;
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider metaAccess)
    {
        throw GraalError.shouldNotReachHere("illegal stamp has no Java type");
    }

    @Override
    public Stamp meet(Stamp other)
    {
        return this;
    }

    @Override
    public Stamp join(Stamp other)
    {
        return this;
    }

    @Override
    public boolean isCompatible(Stamp stamp)
    {
        return stamp instanceof IllegalStamp;
    }

    @Override
    public boolean isCompatible(Constant constant)
    {
        if (constant instanceof PrimitiveConstant)
        {
            PrimitiveConstant prim = (PrimitiveConstant) constant;
            return prim.getJavaKind() == JavaKind.Illegal;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "ILLEGAL";
    }

    @Override
    public boolean hasValues()
    {
        return true;
    }

    @Override
    public Stamp improveWith(Stamp other)
    {
        return this;
    }

    @Override
    public Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement)
    {
        throw GraalError.shouldNotReachHere("can't read values of illegal stamp");
    }

    private static final IllegalStamp instance = new IllegalStamp();

    static IllegalStamp getInstance()
    {
        return instance;
    }
}
