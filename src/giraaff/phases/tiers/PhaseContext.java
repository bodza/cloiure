package giraaff.phases.tiers;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.Replacements;
import giraaff.nodes.spi.StampProvider;
import giraaff.phases.util.Providers;

// @class PhaseContext
public class PhaseContext
{
    // @field
    private final MetaAccessProvider metaAccess;
    // @field
    private final ConstantReflectionProvider constantReflection;
    // @field
    private final ConstantFieldProvider constantFieldProvider;
    // @field
    private final LoweringProvider lowerer;
    // @field
    private final Replacements replacements;
    // @field
    private final StampProvider stampProvider;

    // @cons
    public PhaseContext(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, LoweringProvider __lowerer, Replacements __replacements, StampProvider __stampProvider)
    {
        super();
        this.metaAccess = __metaAccess;
        this.constantReflection = __constantReflection;
        this.constantFieldProvider = __constantFieldProvider;
        this.lowerer = __lowerer;
        this.replacements = __replacements;
        this.stampProvider = __stampProvider;
    }

    // @cons
    public PhaseContext(Providers __providers)
    {
        this(__providers.getMetaAccess(), __providers.getConstantReflection(), __providers.getConstantFieldProvider(), __providers.getLowerer(), __providers.getReplacements(), __providers.getStampProvider());
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
