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

/**
 * A set of providers, some of which may not be present (i.e., null).
 */
// @class Providers
public class Providers implements CodeGenProviders
{
    // @field
    private final MetaAccessProvider metaAccess;
    // @field
    private final CodeCacheProvider codeCache;
    // @field
    private final LoweringProvider lowerer;
    // @field
    private final ConstantReflectionProvider constantReflection;
    // @field
    private final ConstantFieldProvider constantFieldProvider;
    // @field
    private final ForeignCallsProvider foreignCalls;
    // @field
    private final Replacements replacements;
    // @field
    private final StampProvider stampProvider;

    // @cons
    public Providers(MetaAccessProvider __metaAccess, CodeCacheProvider __codeCache, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, ForeignCallsProvider __foreignCalls, LoweringProvider __lowerer, Replacements __replacements, StampProvider __stampProvider)
    {
        super();
        this.metaAccess = __metaAccess;
        this.codeCache = __codeCache;
        this.constantReflection = __constantReflection;
        this.constantFieldProvider = __constantFieldProvider;
        this.foreignCalls = __foreignCalls;
        this.lowerer = __lowerer;
        this.replacements = __replacements;
        this.stampProvider = __stampProvider;
    }

    // @cons
    public Providers(Providers __copyFrom)
    {
        this(__copyFrom.getMetaAccess(), __copyFrom.getCodeCache(), __copyFrom.getConstantReflection(), __copyFrom.getConstantFieldProvider(), __copyFrom.getForeignCalls(), __copyFrom.getLowerer(), __copyFrom.getReplacements(), __copyFrom.getStampProvider());
    }

    // @cons
    public Providers(PhaseContext __copyFrom)
    {
        this(__copyFrom.getMetaAccess(), null, __copyFrom.getConstantReflection(), __copyFrom.getConstantFieldProvider(), null, __copyFrom.getLowerer(), __copyFrom.getReplacements(), __copyFrom.getStampProvider());
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return metaAccess;
    }

    @Override
    public CodeCacheProvider getCodeCache()
    {
        return codeCache;
    }

    @Override
    public ForeignCallsProvider getForeignCalls()
    {
        return foreignCalls;
    }

    public LoweringProvider getLowerer()
    {
        return lowerer;
    }

    @Override
    public ArrayOffsetProvider getArrayOffsetProvider()
    {
        return lowerer;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return constantReflection;
    }

    public ConstantFieldProvider getConstantFieldProvider()
    {
        return constantFieldProvider;
    }

    public Replacements getReplacements()
    {
        return replacements;
    }

    public StampProvider getStampProvider()
    {
        return stampProvider;
    }

    public Providers copyWith(MetaAccessProvider __substitution)
    {
        return new Providers(__substitution, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(CodeCacheProvider __substitution)
    {
        return new Providers(metaAccess, __substitution, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(ConstantReflectionProvider __substitution)
    {
        return new Providers(metaAccess, codeCache, __substitution, constantFieldProvider, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(ConstantFieldProvider __substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, __substitution, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(ForeignCallsProvider __substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, __substitution, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(LoweringProvider __substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, __substitution, replacements, stampProvider);
    }

    public Providers copyWith(Replacements __substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, __substitution, stampProvider);
    }

    public Providers copyWith(StampProvider __substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, __substitution);
    }
}
