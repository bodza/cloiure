package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.util.GraalError;

///
// Singleton stamp representing the value of type {@code void}.
///
// @class VoidStamp
public final class VoidStamp extends Stamp
{
    // @cons VoidStamp
    private VoidStamp()
    {
        super();
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
    public JavaKind getStackKind()
    {
        return JavaKind.Void;
    }

    @Override
    public Stamp improveWith(Stamp __other)
    {
        return this;
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        throw GraalError.shouldNotReachHere("void stamp has no value");
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider __metaAccess)
    {
        return __metaAccess.lookupJavaType(Void.TYPE);
    }

    @Override
    public boolean alwaysDistinct(Stamp __other)
    {
        return this != __other;
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
        return __stamp instanceof VoidStamp;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        return false;
    }

    @Override
    public Stamp empty()
    {
        // the void stamp is always empty
        return this;
    }

    @Override
    public boolean hasValues()
    {
        return false;
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        throw GraalError.shouldNotReachHere("can't read values of void stamp");
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        throw GraalError.shouldNotReachHere("void stamp has no value");
    }

    // @def
    private static final VoidStamp instance = new VoidStamp();

    static VoidStamp getInstance()
    {
        return instance;
    }
}
