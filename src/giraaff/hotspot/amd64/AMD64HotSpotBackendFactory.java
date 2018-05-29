package giraaff.hotspot.amd64;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.runtime.JVMCIBackend;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotBackendFactory;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.HotSpotReplacementsImpl;
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
import giraaff.options.OptionValues;
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
    public Class<? extends Architecture> getArchitecture()
    {
        return AMD64.class;
    }

    @Override
    public HotSpotBackend createBackend(HotSpotGraalRuntimeProvider graalRuntime, CompilerConfiguration compilerConfiguration, HotSpotJVMCIRuntimeProvider jvmciRuntime)
    {
        OptionValues options = graalRuntime.getOptions();
        JVMCIBackend jvmci = jvmciRuntime.getHostJVMCIBackend();
        HotSpotCodeCacheProvider codeCache = (HotSpotCodeCacheProvider) jvmci.getCodeCache();
        TargetDescription target = codeCache.getTarget();
        HotSpotMetaAccessProvider metaAccess = (HotSpotMetaAccessProvider) jvmci.getMetaAccess();
        HotSpotConstantReflectionProvider constantReflection = (HotSpotConstantReflectionProvider) jvmci.getConstantReflection();
        ConstantFieldProvider constantFieldProvider = new HotSpotGraalConstantFieldProvider(metaAccess);
        HotSpotRegistersProvider registers = createRegisters();
        Value[] nativeABICallerSaveRegisters = createNativeABICallerSaveRegisters(codeCache.getRegisterConfig());
        HotSpotWordTypes wordTypes = new HotSpotWordTypes(metaAccess, target.wordJavaKind);
        HotSpotHostForeignCallsProvider foreignCalls = createForeignCalls(jvmciRuntime, graalRuntime, metaAccess, codeCache, wordTypes, nativeABICallerSaveRegisters);
        HotSpotLoweringProvider lowerer = createLowerer(graalRuntime, metaAccess, foreignCalls, registers, constantReflection, target);
        HotSpotStampProvider stampProvider = new HotSpotStampProvider();
        Providers p = new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, null, stampProvider);

        HotSpotSnippetReflectionProvider snippetReflection = createSnippetReflection(graalRuntime, constantReflection, wordTypes);
        BytecodeProvider bytecodeProvider = new ClassfileBytecodeProvider(metaAccess, snippetReflection);
        HotSpotReplacementsImpl replacements = createReplacements(options, p, snippetReflection, bytecodeProvider);
        Plugins plugins = createGraphBuilderPlugins(compilerConfiguration, options, target, constantReflection, foreignCalls, lowerer, metaAccess, snippetReflection, replacements, wordTypes, stampProvider);
        replacements.setGraphBuilderPlugins(plugins);
        HotSpotSuitesProvider suites = createSuites(graalRuntime, compilerConfiguration, plugins, registers, replacements, options);
        HotSpotProviders providers = new HotSpotProviders(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, suites, registers, snippetReflection, wordTypes, plugins);
        return createBackend(graalRuntime, providers);
    }

    protected Plugins createGraphBuilderPlugins(CompilerConfiguration compilerConfiguration, OptionValues options, TargetDescription target, HotSpotConstantReflectionProvider constantReflection, HotSpotHostForeignCallsProvider foreignCalls, LoweringProvider lowerer, HotSpotMetaAccessProvider metaAccess, HotSpotSnippetReflectionProvider snippetReflection, HotSpotReplacementsImpl replacements, HotSpotWordTypes wordTypes, HotSpotStampProvider stampProvider)
    {
        Plugins plugins = HotSpotGraphBuilderPlugins.create(compilerConfiguration, wordTypes, metaAccess, constantReflection, snippetReflection, foreignCalls, lowerer, stampProvider, replacements);
        AMD64GraphBuilderPlugins.register(plugins, replacements.getDefaultReplacementBytecodeProvider(), (AMD64) target.arch);
        return plugins;
    }

    protected AMD64HotSpotBackend createBackend(HotSpotGraalRuntimeProvider runtime, HotSpotProviders providers)
    {
        return new AMD64HotSpotBackend(runtime, providers);
    }

    protected HotSpotRegistersProvider createRegisters()
    {
        return new HotSpotRegisters(AMD64.r15, AMD64.r12, AMD64.rsp);
    }

    protected HotSpotReplacementsImpl createReplacements(OptionValues options, Providers p, SnippetReflectionProvider snippetReflection, BytecodeProvider bytecodeProvider)
    {
        return new HotSpotReplacementsImpl(options, p, snippetReflection, bytecodeProvider, p.getCodeCache().getTarget());
    }

    protected AMD64HotSpotForeignCallsProvider createForeignCalls(HotSpotJVMCIRuntimeProvider jvmciRuntime, HotSpotGraalRuntimeProvider runtime, HotSpotMetaAccessProvider metaAccess, HotSpotCodeCacheProvider codeCache, WordTypes wordTypes, Value[] nativeABICallerSaveRegisters)
    {
        return new AMD64HotSpotForeignCallsProvider(jvmciRuntime, runtime, metaAccess, codeCache, wordTypes, nativeABICallerSaveRegisters);
    }

    protected HotSpotSuitesProvider createSuites(HotSpotGraalRuntimeProvider runtime, CompilerConfiguration compilerConfiguration, Plugins plugins, HotSpotRegistersProvider registers, Replacements replacements, OptionValues options)
    {
        return new AddressLoweringHotSpotSuitesProvider(new AMD64HotSpotSuitesCreator(compilerConfiguration, plugins), runtime, new AddressLoweringPhase(new AMD64HotSpotAddressLowering(registers.getHeapBaseRegister(), options)));
    }

    protected HotSpotSnippetReflectionProvider createSnippetReflection(HotSpotGraalRuntimeProvider runtime, HotSpotConstantReflectionProvider constantReflection, WordTypes wordTypes)
    {
        return new HotSpotSnippetReflectionProvider(runtime, constantReflection, wordTypes);
    }

    protected HotSpotLoweringProvider createLowerer(HotSpotGraalRuntimeProvider runtime, HotSpotMetaAccessProvider metaAccess, HotSpotForeignCallsProvider foreignCalls, HotSpotRegistersProvider registers, HotSpotConstantReflectionProvider constantReflection, TargetDescription target)
    {
        return new AMD64HotSpotLoweringProvider(runtime, metaAccess, foreignCalls, registers, constantReflection, target);
    }

    protected Value[] createNativeABICallerSaveRegisters(RegisterConfig regConfig)
    {
        List<Register> callerSave = new ArrayList<>(regConfig.getAllocatableRegisters().asList());

        /*
         * System V Application Binary Interface, AMD64 Architecture Processor Supplement
         *
         * Draft Version 0.96
         *
         * http://www.uclibc.org/docs/psABI-x86_64.pdf
         *
         * 3.2.1
         *
         * ...
         *
         * This subsection discusses usage of each register. Registers %rbp, %rbx and %r12
         * through %r15 "belong" to the calling function and the called function is required to
         * preserve their values. In other words, a called function must preserve these
         * registers' values for its caller. Remaining registers "belong" to the called
         * function. If a calling function wants to preserve such a register value across a
         * function call, it must save the value in its local stack frame.
         */
        callerSave.remove(AMD64.rbp);
        callerSave.remove(AMD64.rbx);
        callerSave.remove(AMD64.r12);
        callerSave.remove(AMD64.r13);
        callerSave.remove(AMD64.r14);
        callerSave.remove(AMD64.r15);

        Value[] nativeABICallerSaveRegisters = new Value[callerSave.size()];
        for (int i = 0; i < callerSave.size(); i++)
        {
            nativeABICallerSaveRegisters[i] = callerSave.get(i).asValue();
        }
        return nativeABICallerSaveRegisters;
    }

    @Override
    public String toString()
    {
        return "AMD64";
    }
}
