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
    private final MetaAccessProvider ___metaAccess;
    // @field
    private final ConstantReflectionProvider ___constantReflection;
    // @field
    private final ConstantFieldProvider ___constantFieldProvider;
    // @field
    private final LoweringProvider ___lowerer;
    // @field
    private final Replacements ___replacements;
    // @field
    private final StampProvider ___stampProvider;

    // @cons PhaseContext
    public PhaseContext(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, LoweringProvider __lowerer, Replacements __replacements, StampProvider __stampProvider)
    {
        super();
        this.___metaAccess = __metaAccess;
        this.___constantReflection = __constantReflection;
        this.___constantFieldProvider = __constantFieldProvider;
        this.___lowerer = __lowerer;
        this.___replacements = __replacements;
        this.___stampProvider = __stampProvider;
    }

    // @cons PhaseContext
    public PhaseContext(Providers __providers)
    {
        this(__providers.getMetaAccess(), __providers.getConstantReflection(), __providers.getConstantFieldProvider(), __providers.getLowerer(), __providers.getReplacements(), __providers.getStampProvider());
    }

    public MetaAccessProvider getMetaAccess()
    {
        return this.___metaAccess;
    }

    public ConstantReflectionProvider getConstantReflection()
    {
        return this.___constantReflection;
    }

    public ConstantFieldProvider getConstantFieldProvider()
    {
        return this.___constantFieldProvider;
    }

    public LoweringProvider getLowerer()
    {
        return this.___lowerer;
    }

    public Replacements getReplacements()
    {
        return this.___replacements;
    }

    public StampProvider getStampProvider()
    {
        return this.___stampProvider;
    }
}
