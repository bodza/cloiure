package graalvm.compiler.phases.util;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import graalvm.compiler.core.common.spi.ArrayOffsetProvider;
import graalvm.compiler.core.common.spi.CodeGenProviders;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.nodes.spi.LoweringProvider;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.nodes.spi.StampProvider;
import graalvm.compiler.phases.tiers.PhaseContext;

/**
 * A set of providers, some of which may not be present (i.e., null).
 */
public class Providers implements CodeGenProviders
{
    private final MetaAccessProvider metaAccess;
    private final CodeCacheProvider codeCache;
    private final LoweringProvider lowerer;
    private final ConstantReflectionProvider constantReflection;
    private final ConstantFieldProvider constantFieldProvider;
    private final ForeignCallsProvider foreignCalls;
    private final Replacements replacements;
    private final StampProvider stampProvider;

    public Providers(MetaAccessProvider metaAccess, CodeCacheProvider codeCache, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, ForeignCallsProvider foreignCalls, LoweringProvider lowerer, Replacements replacements, StampProvider stampProvider)
    {
        this.metaAccess = metaAccess;
        this.codeCache = codeCache;
        this.constantReflection = constantReflection;
        this.constantFieldProvider = constantFieldProvider;
        this.foreignCalls = foreignCalls;
        this.lowerer = lowerer;
        this.replacements = replacements;
        this.stampProvider = stampProvider;
    }

    public Providers(Providers copyFrom)
    {
        this(copyFrom.getMetaAccess(), copyFrom.getCodeCache(), copyFrom.getConstantReflection(), copyFrom.getConstantFieldProvider(), copyFrom.getForeignCalls(), copyFrom.getLowerer(), copyFrom.getReplacements(), copyFrom.getStampProvider());
    }

    public Providers(PhaseContext copyFrom)
    {
        this(copyFrom.getMetaAccess(), null, copyFrom.getConstantReflection(), copyFrom.getConstantFieldProvider(), null, copyFrom.getLowerer(), copyFrom.getReplacements(), copyFrom.getStampProvider());
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

    public Providers copyWith(MetaAccessProvider substitution)
    {
        return new Providers(substitution, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(CodeCacheProvider substitution)
    {
        return new Providers(metaAccess, substitution, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(ConstantReflectionProvider substitution)
    {
        return new Providers(metaAccess, codeCache, substitution, constantFieldProvider, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(ConstantFieldProvider substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, substitution, foreignCalls, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(ForeignCallsProvider substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, substitution, lowerer, replacements, stampProvider);
    }

    public Providers copyWith(LoweringProvider substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, substitution, replacements, stampProvider);
    }

    public Providers copyWith(Replacements substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, substitution, stampProvider);
    }

    public Providers copyWith(StampProvider substitution)
    {
        return new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, substitution);
    }
}
