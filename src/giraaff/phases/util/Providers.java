package giraaff.phases.util;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.core.common.spi.CodeGenProviders;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.Replacements;
import giraaff.nodes.spi.StampProvider;
import giraaff.phases.tiers.PhaseContext;

///
// A set of providers, some of which may not be present (i.e., null).
///
// @class Providers
public class Providers implements CodeGenProviders
{
    // @field
    private final MetaAccessProvider ___metaAccess;
    // @field
    private final CodeCacheProvider ___codeCache;
    // @field
    private final LoweringProvider ___lowerer;
    // @field
    private final ConstantReflectionProvider ___constantReflection;
    // @field
    private final ConstantFieldProvider ___constantFieldProvider;
    // @field
    private final ForeignCallsProvider ___foreignCalls;
    // @field
    private final Replacements ___replacements;
    // @field
    private final StampProvider ___stampProvider;

    // @cons Providers
    public Providers(MetaAccessProvider __metaAccess, CodeCacheProvider __codeCache, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, ForeignCallsProvider __foreignCalls, LoweringProvider __lowerer, Replacements __replacements, StampProvider __stampProvider)
    {
        super();
        this.___metaAccess = __metaAccess;
        this.___codeCache = __codeCache;
        this.___constantReflection = __constantReflection;
        this.___constantFieldProvider = __constantFieldProvider;
        this.___foreignCalls = __foreignCalls;
        this.___lowerer = __lowerer;
        this.___replacements = __replacements;
        this.___stampProvider = __stampProvider;
    }

    // @cons Providers
    public Providers(Providers __copyFrom)
    {
        this(__copyFrom.getMetaAccess(), __copyFrom.getCodeCache(), __copyFrom.getConstantReflection(), __copyFrom.getConstantFieldProvider(), __copyFrom.getForeignCalls(), __copyFrom.getLowerer(), __copyFrom.getReplacements(), __copyFrom.getStampProvider());
    }

    // @cons Providers
    public Providers(PhaseContext __copyFrom)
    {
        this(__copyFrom.getMetaAccess(), null, __copyFrom.getConstantReflection(), __copyFrom.getConstantFieldProvider(), null, __copyFrom.getLowerer(), __copyFrom.getReplacements(), __copyFrom.getStampProvider());
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return this.___metaAccess;
    }

    @Override
    public CodeCacheProvider getCodeCache()
    {
        return this.___codeCache;
    }

    @Override
    public ForeignCallsProvider getForeignCalls()
    {
        return this.___foreignCalls;
    }

    public LoweringProvider getLowerer()
    {
        return this.___lowerer;
    }

    @Override
    public ArrayOffsetProvider getArrayOffsetProvider()
    {
        return this.___lowerer;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return this.___constantReflection;
    }

    public ConstantFieldProvider getConstantFieldProvider()
    {
        return this.___constantFieldProvider;
    }

    public Replacements getReplacements()
    {
        return this.___replacements;
    }

    public StampProvider getStampProvider()
    {
        return this.___stampProvider;
    }

    public Providers copyWith(MetaAccessProvider __substitution)
    {
        return new Providers(__substitution, this.___codeCache, this.___constantReflection, this.___constantFieldProvider, this.___foreignCalls, this.___lowerer, this.___replacements, this.___stampProvider);
    }

    public Providers copyWith(CodeCacheProvider __substitution)
    {
        return new Providers(this.___metaAccess, __substitution, this.___constantReflection, this.___constantFieldProvider, this.___foreignCalls, this.___lowerer, this.___replacements, this.___stampProvider);
    }

    public Providers copyWith(ConstantReflectionProvider __substitution)
    {
        return new Providers(this.___metaAccess, this.___codeCache, __substitution, this.___constantFieldProvider, this.___foreignCalls, this.___lowerer, this.___replacements, this.___stampProvider);
    }

    public Providers copyWith(ConstantFieldProvider __substitution)
    {
        return new Providers(this.___metaAccess, this.___codeCache, this.___constantReflection, __substitution, this.___foreignCalls, this.___lowerer, this.___replacements, this.___stampProvider);
    }

    public Providers copyWith(ForeignCallsProvider __substitution)
    {
        return new Providers(this.___metaAccess, this.___codeCache, this.___constantReflection, this.___constantFieldProvider, __substitution, this.___lowerer, this.___replacements, this.___stampProvider);
    }

    public Providers copyWith(LoweringProvider __substitution)
    {
        return new Providers(this.___metaAccess, this.___codeCache, this.___constantReflection, this.___constantFieldProvider, this.___foreignCalls, __substitution, this.___replacements, this.___stampProvider);
    }

    public Providers copyWith(Replacements __substitution)
    {
        return new Providers(this.___metaAccess, this.___codeCache, this.___constantReflection, this.___constantFieldProvider, this.___foreignCalls, this.___lowerer, __substitution, this.___stampProvider);
    }

    public Providers copyWith(StampProvider __substitution)
    {
        return new Providers(this.___metaAccess, this.___codeCache, this.___constantReflection, this.___constantFieldProvider, this.___foreignCalls, this.___lowerer, this.___replacements, __substitution);
    }
}
