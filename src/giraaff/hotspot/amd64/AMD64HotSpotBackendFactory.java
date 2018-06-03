package giraaff.hotspot.amd64;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.runtime.JVMCIBackend;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotBackendFactory;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.HotSpotReplacementsImpl;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.AddressLoweringHotSpotSuitesProvider;
import giraaff.hotspot.meta.HotSpotForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotGraalConstantFieldProvider;
import giraaff.hotspot.meta.HotSpotGraphBuilderPlugins;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotLoweringProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegisters;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.hotspot.meta.HotSpotSnippetReflectionProvider;
import giraaff.hotspot.meta.HotSpotStampProvider;
import giraaff.hotspot.meta.HotSpotSuitesProvider;
import giraaff.hotspot.word.HotSpotWordTypes;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.AddressLoweringPhase;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.phases.util.Providers;
import giraaff.replacements.amd64.AMD64GraphBuilderPlugins;
import giraaff.replacements.classfile.ClassfileBytecodeProvider;
import giraaff.word.WordTypes;

// @class AMD64HotSpotBackendFactory
public final class AMD64HotSpotBackendFactory implements HotSpotBackendFactory
{
    // @cons
    public AMD64HotSpotBackendFactory()
    {
        super();
    }

    @Override
    public HotSpotBackend createBackend(HotSpotGraalRuntime __graalRuntime, CompilerConfiguration __compilerConfiguration)
    {
        JVMCIBackend __jvmci = HotSpotRuntime.JVMCI.getHostJVMCIBackend();
        HotSpotCodeCacheProvider __codeCache = (HotSpotCodeCacheProvider) __jvmci.getCodeCache();
        TargetDescription __target = __codeCache.getTarget();
        HotSpotMetaAccessProvider __metaAccess = (HotSpotMetaAccessProvider) __jvmci.getMetaAccess();
        HotSpotConstantReflectionProvider __constantReflection = (HotSpotConstantReflectionProvider) __jvmci.getConstantReflection();
        ConstantFieldProvider __constantFieldProvider = new HotSpotGraalConstantFieldProvider(__metaAccess);
        HotSpotRegistersProvider __registers = createRegisters();
        Value[] __nativeABICallerSaveRegisters = createNativeABICallerSaveRegisters(__codeCache.getRegisterConfig());
        HotSpotWordTypes __wordTypes = new HotSpotWordTypes(__metaAccess, __target.wordJavaKind);
        HotSpotHostForeignCallsProvider __foreignCalls = createForeignCalls(__graalRuntime, __metaAccess, __codeCache, __wordTypes, __nativeABICallerSaveRegisters);
        HotSpotLoweringProvider __lowerer = createLowerer(__graalRuntime, __metaAccess, __foreignCalls, __registers, __constantReflection, __target);
        HotSpotStampProvider __stampProvider = new HotSpotStampProvider();
        Providers __p = new Providers(__metaAccess, __codeCache, __constantReflection, __constantFieldProvider, __foreignCalls, __lowerer, null, __stampProvider);

        HotSpotSnippetReflectionProvider __snippetReflection = createSnippetReflection(__graalRuntime, __constantReflection, __wordTypes);
        BytecodeProvider __bytecodeProvider = new ClassfileBytecodeProvider(__metaAccess, __snippetReflection);
        HotSpotReplacementsImpl __replacements = createReplacements(__p, __snippetReflection, __bytecodeProvider);
        Plugins __plugins = createGraphBuilderPlugins(__compilerConfiguration, __target, __constantReflection, __foreignCalls, __lowerer, __metaAccess, __snippetReflection, __replacements, __wordTypes, __stampProvider);
        __replacements.setGraphBuilderPlugins(__plugins);
        HotSpotSuitesProvider __suites = createSuites(__graalRuntime, __compilerConfiguration, __plugins, __registers, __replacements);
        HotSpotProviders __providers = new HotSpotProviders(__metaAccess, __codeCache, __constantReflection, __constantFieldProvider, __foreignCalls, __lowerer, __replacements, __suites, __registers, __snippetReflection, __wordTypes, __plugins);
        return createBackend(__graalRuntime, __providers);
    }

    protected Plugins createGraphBuilderPlugins(CompilerConfiguration __compilerConfiguration, TargetDescription __target, HotSpotConstantReflectionProvider __constantReflection, HotSpotHostForeignCallsProvider __foreignCalls, LoweringProvider __lowerer, HotSpotMetaAccessProvider __metaAccess, HotSpotSnippetReflectionProvider __snippetReflection, HotSpotReplacementsImpl __replacements, HotSpotWordTypes __wordTypes, HotSpotStampProvider __stampProvider)
    {
        Plugins __plugins = HotSpotGraphBuilderPlugins.create(__compilerConfiguration, __wordTypes, __metaAccess, __constantReflection, __snippetReflection, __foreignCalls, __lowerer, __stampProvider, __replacements);
        AMD64GraphBuilderPlugins.register(__plugins, __replacements.getDefaultReplacementBytecodeProvider(), (AMD64) __target.arch);
        return __plugins;
    }

    protected AMD64HotSpotBackend createBackend(HotSpotGraalRuntime __runtime, HotSpotProviders __providers)
    {
        return new AMD64HotSpotBackend(__runtime, __providers);
    }

    protected HotSpotRegistersProvider createRegisters()
    {
        return new HotSpotRegisters(AMD64.r15, AMD64.r12, AMD64.rsp);
    }

    protected HotSpotReplacementsImpl createReplacements(Providers __p, SnippetReflectionProvider __snippetReflection, BytecodeProvider __bytecodeProvider)
    {
        return new HotSpotReplacementsImpl(__p, __snippetReflection, __bytecodeProvider, __p.getCodeCache().getTarget());
    }

    protected AMD64HotSpotForeignCallsProvider createForeignCalls(HotSpotGraalRuntime __runtime, HotSpotMetaAccessProvider __metaAccess, HotSpotCodeCacheProvider __codeCache, WordTypes __wordTypes, Value[] __nativeABICallerSaveRegisters)
    {
        return new AMD64HotSpotForeignCallsProvider(__runtime, __metaAccess, __codeCache, __wordTypes, __nativeABICallerSaveRegisters);
    }

    protected HotSpotSuitesProvider createSuites(HotSpotGraalRuntime __runtime, CompilerConfiguration __compilerConfiguration, Plugins __plugins, HotSpotRegistersProvider __registers, Replacements __replacements)
    {
        return new AddressLoweringHotSpotSuitesProvider(new AMD64HotSpotSuitesCreator(__compilerConfiguration, __plugins), __runtime, new AddressLoweringPhase(new AMD64HotSpotAddressLowering(__registers.getHeapBaseRegister())));
    }

    protected HotSpotSnippetReflectionProvider createSnippetReflection(HotSpotGraalRuntime __runtime, HotSpotConstantReflectionProvider __constantReflection, WordTypes __wordTypes)
    {
        return new HotSpotSnippetReflectionProvider(__runtime, __constantReflection, __wordTypes);
    }

    protected HotSpotLoweringProvider createLowerer(HotSpotGraalRuntime __runtime, HotSpotMetaAccessProvider __metaAccess, HotSpotForeignCallsProvider __foreignCalls, HotSpotRegistersProvider __registers, HotSpotConstantReflectionProvider __constantReflection, TargetDescription __target)
    {
        return new AMD64HotSpotLoweringProvider(__runtime, __metaAccess, __foreignCalls, __registers, __constantReflection, __target);
    }

    protected Value[] createNativeABICallerSaveRegisters(RegisterConfig __regConfig)
    {
        List<Register> __callerSave = new ArrayList<>(__regConfig.getAllocatableRegisters().asList());

        // System V Application Binary Interface, AMD64 Architecture Processor Supplement
        //
        // Draft Version 0.96
        //
        // http://www.uclibc.org/docs/psABI-x86_64.pdf
        //
        // 3.2.1
        //
        // ...
        //
        // This subsection discusses usage of each register. Registers %rbp, %rbx and %r12
        // through %r15 "belong" to the calling function and the called function is required to
        // preserve their values. In other words, a called function must preserve these
        // registers' values for its caller. Remaining registers "belong" to the called
        // function. If a calling function wants to preserve such a register value across a
        // function call, it must save the value in its local stack frame.
        __callerSave.remove(AMD64.rbp);
        __callerSave.remove(AMD64.rbx);
        __callerSave.remove(AMD64.r12);
        __callerSave.remove(AMD64.r13);
        __callerSave.remove(AMD64.r14);
        __callerSave.remove(AMD64.r15);

        Value[] __nativeABICallerSaveRegisters = new Value[__callerSave.size()];
        for (int __i = 0; __i < __callerSave.size(); __i++)
        {
            __nativeABICallerSaveRegisters[__i] = __callerSave.get(__i).asValue();
        }
        return __nativeABICallerSaveRegisters;
    }
}
