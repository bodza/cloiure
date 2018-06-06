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

///
// This stamp represents the type of the {@link JavaKind#Illegal} value in the second slot
// of {@link JavaKind#Long} values. It can only appear in framestates or virtual objects.
///
// @class IllegalStamp
public final class IllegalStamp extends Stamp
{
    // @cons IllegalStamp
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
    public LIRKind getLIRKind(LIRKindTool __tool)
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
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        return this;
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider __metaAccess)
    {
        throw GraalError.shouldNotReachHere("illegal stamp has no Java type");
    }

    @Override
    public Stamp meet(Stamp __other)
    {
        return this;
    }

    @Override
    public Stamp join(Stamp __other)
    {
        return this;
    }

    @Override
    public boolean isCompatible(Stamp __stamp)
    {
        return __stamp instanceof IllegalStamp;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        if (__constant instanceof PrimitiveConstant)
        {
            PrimitiveConstant __prim = (PrimitiveConstant) __constant;
            return __prim.getJavaKind() == JavaKind.Illegal;
        }
        return false;
    }

    @Override
    public boolean hasValues()
    {
        return true;
    }

    @Override
    public Stamp improveWith(Stamp __other)
    {
        return this;
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        throw GraalError.shouldNotReachHere("can't read values of illegal stamp");
    }

    // @def
    private static final IllegalStamp instance = new IllegalStamp();

    static IllegalStamp getInstance()
    {
        return instance;
    }
}
