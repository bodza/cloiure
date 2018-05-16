package graalvm.compiler.hotspot.amd64;

import static jdk.vm.ci.common.InitTimer.timer;
import static graalvm.compiler.hotspot.HotSpotBackend.Options.GraalArithmeticStubs;
import java.util.ArrayList;
import java.util.List;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.bytecode.BytecodeProvider;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.HotSpotBackendFactory;
import graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import graalvm.compiler.hotspot.HotSpotReplacementsImpl;
import graalvm.compiler.hotspot.meta.AddressLoweringHotSpotSuitesProvider;
import graalvm.compiler.hotspot.meta.HotSpotForeignCallsProvider;
import graalvm.compiler.hotspot.meta.HotSpotGraalConstantFieldProvider;
import graalvm.compiler.hotspot.meta.HotSpotGraphBuilderPlugins;
import graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider;
import graalvm.compiler.hotspot.meta.HotSpotLoweringProvider;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.meta.HotSpotRegisters;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.hotspot.meta.HotSpotSnippetReflectionProvider;
import graalvm.compiler.hotspot.meta.HotSpotStampProvider;
import graalvm.compiler.hotspot.meta.HotSpotSuitesProvider;
import graalvm.compiler.hotspot.word.HotSpotWordTypes;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.nodes.spi.LoweringProvider;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.common.AddressLoweringPhase;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.replacements.amd64.AMD64GraphBuilderPlugins;
import graalvm.compiler.replacements.classfile.ClassfileBytecodeProvider;
import graalvm.compiler.serviceprovider.ServiceProvider;
import graalvm.compiler.word.WordTypes;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.common.InitTimer;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotMetaAccessProvider;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.runtime.JVMCIBackend;

@ServiceProvider(HotSpotBackendFactory.class)
public class AMD64HotSpotBackendFactory implements HotSpotBackendFactory
{
    @Override
    public String getName()
    {
        return "community";
    }

    @Override
    public Class<? extends Architecture> getArchitecture()
    {
        return AMD64.class;
    }

    @Override
    @SuppressWarnings("try")
    public HotSpotBackend createBackend(HotSpotGraalRuntimeProvider graalRuntime, CompilerConfiguration compilerConfiguration, HotSpotJVMCIRuntimeProvider jvmciRuntime, HotSpotBackend host)
    {
        assert host == null;

        OptionValues options = graalRuntime.getOptions();
        JVMCIBackend jvmci = jvmciRuntime.getHostJVMCIBackend();
        GraalHotSpotVMConfig config = graalRuntime.getVMConfig();
        HotSpotProviders providers;
        HotSpotRegistersProvider registers;
        HotSpotCodeCacheProvider codeCache = (HotSpotCodeCacheProvider) jvmci.getCodeCache();
        TargetDescription target = codeCache.getTarget();
        HotSpotHostForeignCallsProvider foreignCalls;
        Value[] nativeABICallerSaveRegisters;
        HotSpotMetaAccessProvider metaAccess = (HotSpotMetaAccessProvider) jvmci.getMetaAccess();
        HotSpotConstantReflectionProvider constantReflection = (HotSpotConstantReflectionProvider) jvmci.getConstantReflection();
        ConstantFieldProvider constantFieldProvider = new HotSpotGraalConstantFieldProvider(config, metaAccess);
        HotSpotLoweringProvider lowerer;
        HotSpotSnippetReflectionProvider snippetReflection;
        HotSpotReplacementsImpl replacements;
        HotSpotSuitesProvider suites;
        HotSpotWordTypes wordTypes;
        Plugins plugins;
        BytecodeProvider bytecodeProvider;
        try (InitTimer t = timer("create providers"))
        {
            try (InitTimer rt = timer("create HotSpotRegisters provider"))
            {
                registers = createRegisters();
            }
            try (InitTimer rt = timer("create NativeABICallerSaveRegisters"))
            {
                nativeABICallerSaveRegisters = createNativeABICallerSaveRegisters(config, codeCache.getRegisterConfig());
            }
            try (InitTimer rt = timer("create WordTypes"))
            {
                wordTypes = new HotSpotWordTypes(metaAccess, target.wordJavaKind);
            }
            try (InitTimer rt = timer("create ForeignCalls provider"))
            {
                foreignCalls = createForeignCalls(jvmciRuntime, graalRuntime, metaAccess, codeCache, wordTypes, nativeABICallerSaveRegisters);
            }
            try (InitTimer rt = timer("create Lowerer provider"))
            {
                lowerer = createLowerer(graalRuntime, metaAccess, foreignCalls, registers, constantReflection, target);
            }
            HotSpotStampProvider stampProvider = new HotSpotStampProvider();
            Providers p = new Providers(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, null, stampProvider);

            try (InitTimer rt = timer("create SnippetReflection provider"))
            {
                snippetReflection = createSnippetReflection(graalRuntime, constantReflection, wordTypes);
            }
            try (InitTimer rt = timer("create Bytecode provider"))
            {
                bytecodeProvider = new ClassfileBytecodeProvider(metaAccess, snippetReflection);
            }
            try (InitTimer rt = timer("create Replacements provider"))
            {
                replacements = createReplacements(options, p, snippetReflection, bytecodeProvider);
            }
            try (InitTimer rt = timer("create GraphBuilderPhase plugins"))
            {
                plugins = createGraphBuilderPlugins(compilerConfiguration, config, options, target, constantReflection, foreignCalls, lowerer, metaAccess, snippetReflection, replacements, wordTypes, stampProvider);
                replacements.setGraphBuilderPlugins(plugins);
            }
            try (InitTimer rt = timer("create Suites provider"))
            {
                suites = createSuites(config, graalRuntime, compilerConfiguration, plugins, registers, replacements, options);
            }
            providers = new HotSpotProviders(metaAccess, codeCache, constantReflection, constantFieldProvider, foreignCalls, lowerer, replacements, suites, registers, snippetReflection, wordTypes, plugins);
        }
        try (InitTimer rt = timer("instantiate backend"))
        {
            return createBackend(config, graalRuntime, providers);
        }
    }

    protected Plugins createGraphBuilderPlugins(CompilerConfiguration compilerConfiguration, GraalHotSpotVMConfig config, OptionValues options, TargetDescription target, HotSpotConstantReflectionProvider constantReflection, HotSpotHostForeignCallsProvider foreignCalls, LoweringProvider lowerer, HotSpotMetaAccessProvider metaAccess, HotSpotSnippetReflectionProvider snippetReflection, HotSpotReplacementsImpl replacements, HotSpotWordTypes wordTypes, HotSpotStampProvider stampProvider)
    {
        Plugins plugins = HotSpotGraphBuilderPlugins.create(compilerConfiguration, config, wordTypes, metaAccess, constantReflection, snippetReflection, foreignCalls, lowerer, stampProvider, replacements);
        AMD64GraphBuilderPlugins.register(plugins, replacements.getDefaultReplacementBytecodeProvider(), (AMD64) target.arch, GraalArithmeticStubs.getValue(options));
        return plugins;
    }

    protected AMD64HotSpotBackend createBackend(GraalHotSpotVMConfig config, HotSpotGraalRuntimeProvider runtime, HotSpotProviders providers)
    {
        return new AMD64HotSpotBackend(config, runtime, providers);
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

    /**
     * @param replacements
     */
    protected HotSpotSuitesProvider createSuites(GraalHotSpotVMConfig config, HotSpotGraalRuntimeProvider runtime, CompilerConfiguration compilerConfiguration, Plugins plugins, HotSpotRegistersProvider registers, Replacements replacements, OptionValues options)
    {
        return new AddressLoweringHotSpotSuitesProvider(new AMD64HotSpotSuitesCreator(compilerConfiguration, plugins), config, runtime, new AddressLoweringPhase(new AMD64HotSpotAddressLowering(config, registers.getHeapBaseRegister(), options)));
    }

    protected HotSpotSnippetReflectionProvider createSnippetReflection(HotSpotGraalRuntimeProvider runtime, HotSpotConstantReflectionProvider constantReflection, WordTypes wordTypes)
    {
        return new HotSpotSnippetReflectionProvider(runtime, constantReflection, wordTypes);
    }

    protected HotSpotLoweringProvider createLowerer(HotSpotGraalRuntimeProvider runtime, HotSpotMetaAccessProvider metaAccess, HotSpotForeignCallsProvider foreignCalls, HotSpotRegistersProvider registers, HotSpotConstantReflectionProvider constantReflection, TargetDescription target)
    {
        return new AMD64HotSpotLoweringProvider(runtime, metaAccess, foreignCalls, registers, constantReflection, target);
    }

    protected Value[] createNativeABICallerSaveRegisters(GraalHotSpotVMConfig config, RegisterConfig regConfig)
    {
        List<Register> callerSave = new ArrayList<>(regConfig.getAllocatableRegisters().asList());
        if (config.windowsOs)
        {
            // http://msdn.microsoft.com/en-us/library/9z1stfyw.aspx
            callerSave.remove(AMD64.rdi);
            callerSave.remove(AMD64.rsi);
            callerSave.remove(AMD64.rbx);
            callerSave.remove(AMD64.rbp);
            callerSave.remove(AMD64.rsp);
            callerSave.remove(AMD64.r12);
            callerSave.remove(AMD64.r13);
            callerSave.remove(AMD64.r14);
            callerSave.remove(AMD64.r15);
            callerSave.remove(AMD64.xmm6);
            callerSave.remove(AMD64.xmm7);
            callerSave.remove(AMD64.xmm8);
            callerSave.remove(AMD64.xmm9);
            callerSave.remove(AMD64.xmm10);
            callerSave.remove(AMD64.xmm11);
            callerSave.remove(AMD64.xmm12);
            callerSave.remove(AMD64.xmm13);
            callerSave.remove(AMD64.xmm14);
            callerSave.remove(AMD64.xmm15);
        }
        else
        {
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
        }
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
