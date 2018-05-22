package giraaff.phases.tiers;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.Replacements;
import giraaff.nodes.spi.StampProvider;
import giraaff.phases.util.Providers;

public class PhaseContext
{
    private final MetaAccessProvider metaAccess;
    private final ConstantReflectionProvider constantReflection;
    private final ConstantFieldProvider constantFieldProvider;
    private final LoweringProvider lowerer;
    private final Replacements replacements;
    private final StampProvider stampProvider;

    public PhaseContext(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, LoweringProvider lowerer, Replacements replacements, StampProvider stampProvider)
    {
        this.metaAccess = metaAccess;
        this.constantReflection = constantReflection;
        this.constantFieldProvider = constantFieldProvider;
        this.lowerer = lowerer;
        this.replacements = replacements;
        this.stampProvider = stampProvider;
    }

    public PhaseContext(Providers providers)
    {
        this(providers.getMetaAccess(), providers.getConstantReflection(), providers.getConstantFieldProvider(), providers.getLowerer(), providers.getReplacements(), providers.getStampProvider());
    }

    public MetaAccessProvider getMetaAccess()
    {
        return metaAccess;
    }

    public ConstantReflectionProvider getConstantReflection()
    {
        return constantReflection;
    }

    public ConstantFieldProvider getConstantFieldProvider()
    {
        return constantFieldProvider;
    }

    public LoweringProvider getLowerer()
    {
        return lowerer;
    }

    public Replacements getReplacements()
    {
        return replacements;
    }

    public StampProvider getStampProvider()
    {
        return stampProvider;
    }
}
