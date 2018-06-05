package giraaff.core.target;

import java.util.ArrayList;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompiledCode;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueKindFactory;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.SpeculationLog;

import org.graalvm.collections.EconomicSet;

import giraaff.asm.Assembler;
import giraaff.code.CompilationResult;
import giraaff.core.common.LIRKind;
import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.lir.LIR;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.asm.CompilationResultBuilderFactory;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.GraphSpeculationLog;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.phases.tiers.SuitesProvider;
import giraaff.phases.tiers.TargetProvider;
import giraaff.phases.util.Providers;

///
// Represents a compiler backend for Graal.
///
// @class Backend
public abstract class Backend implements TargetProvider, ValueKindFactory<LIRKind>
{
    // @field
    private final Providers ___providers;
    // @field
    private final ArrayList<CodeInstallationTaskFactory> ___codeInstallationTaskFactories;

    // @cons
    protected Backend(Providers __providers)
    {
        super();
        this.___providers = __providers;
        this.___codeInstallationTaskFactories = new ArrayList<>();
    }

    public synchronized void addCodeInstallationTask(CodeInstallationTaskFactory __factory)
    {
        this.___codeInstallationTaskFactories.add(__factory);
    }

    public Providers getProviders()
    {
        return this.___providers;
    }

    public CodeCacheProvider getCodeCache()
    {
        return this.___providers.getCodeCache();
    }

    public MetaAccessProvider getMetaAccess()
    {
        return this.___providers.getMetaAccess();
    }

    public ConstantReflectionProvider getConstantReflection()
    {
        return this.___providers.getConstantReflection();
    }

    public ForeignCallsProvider getForeignCalls()
    {
        return this.___providers.getForeignCalls();
    }

    public abstract SuitesProvider getSuites();

    @Override
    public TargetDescription getTarget()
    {
        return this.___providers.getCodeCache().getTarget();
    }

    @Override
    public LIRKind getValueKind(JavaKind __javaKind)
    {
        return LIRKind.fromJavaKind(getTarget().arch, __javaKind);
    }

    ///
    // The given registerConfig is optional, in case null is passed the default RegisterConfig from
    // the CodeCacheProvider will be used.
    ///
    public abstract FrameMapBuilder newFrameMapBuilder(RegisterConfig __registerConfig);

    ///
    // Creates a new configuration for register allocation.
    ///
    public abstract RegisterAllocationConfig newRegisterAllocationConfig(RegisterConfig __registerConfig);

    public abstract FrameMap newFrameMap(RegisterConfig __registerConfig);

    public abstract LIRGeneratorTool newLIRGenerator(LIRGenerationResult __lirGenRes);

    public abstract LIRGenerationResult newLIRGenerationResult(LIR __lir, FrameMapBuilder __frameMapBuilder, StructuredGraph __graph, Object __stub);

    public abstract NodeLIRBuilderTool newNodeLIRBuilder(StructuredGraph __graph, LIRGeneratorTool __lirGen);

    ///
    // Creates the assembler used to emit the machine code.
    ///
    protected abstract Assembler createAssembler(FrameMap __frameMap);

    ///
    // Creates the object used to fill in the details of a given compilation result.
    ///
    public abstract CompilationResultBuilder newCompilationResultBuilder(LIRGenerationResult __lirGenResult, FrameMap __frameMap, CompilationResult __compilationResult, CompilationResultBuilderFactory __factory);

    ///
    // Turns a Graal {@link CompilationResult} into a {@link CompiledCode} object that can be passed
    // to the VM for code installation.
    ///
    protected abstract CompiledCode createCompiledCode(ResolvedJavaMethod __method, CompilationRequest __compilationRequest, CompilationResult __compilationResult);

    ///
    // @see #createInstalledCode(ResolvedJavaMethod, CompilationRequest, CompilationResult, SpeculationLog, InstalledCode, boolean)
    ///
    public InstalledCode createInstalledCode(ResolvedJavaMethod __method, CompilationResult __compilationResult, SpeculationLog __speculationLog, InstalledCode __predefinedInstalledCode, boolean __isDefault)
    {
        return createInstalledCode(__method, null, __compilationResult, __speculationLog, __predefinedInstalledCode, __isDefault);
    }

    ///
    // Installs code based on a given compilation result.
    //
    // @param method the method compiled to produce {@code compiledCode} or {@code null} if the
    //            input to {@code compResult} was not a {@link ResolvedJavaMethod}
    // @param compilationRequest the compilation request or {@code null}
    // @param compilationResult the code to be installed
    // @param predefinedInstalledCode a pre-allocated {@link InstalledCode} object to use as a
    //            reference to the installed code. If {@code null}, a new {@link InstalledCode}
    //            object will be created.
    // @param speculationLog the speculation log to be used
    // @param isDefault specifies if the installed code should be made the default implementation of
    //            {@code compRequest.getMethod()}. The default implementation for a method is the
    //            code executed for standard calls to the method. This argument is ignored if
    //            {@code compRequest == null}.
    // @return a reference to the compiled and ready-to-run installed code
    // @throws BailoutException if the code installation failed
    ///
    public InstalledCode createInstalledCode(ResolvedJavaMethod __method, CompilationRequest __compilationRequest, CompilationResult __compilationResult, SpeculationLog __speculationLog, InstalledCode __predefinedInstalledCode, boolean __isDefault)
    {
        CodeInstallationTask[] __tasks;
        synchronized (this)
        {
            __tasks = new CodeInstallationTask[this.___codeInstallationTaskFactories.size()];
            for (int __i = 0; __i < this.___codeInstallationTaskFactories.size(); __i++)
            {
                __tasks[__i] = this.___codeInstallationTaskFactories.get(__i).create();
            }
        }
        InstalledCode __installedCode;
        try
        {
            preCodeInstallationTasks(__tasks, __compilationResult, __predefinedInstalledCode);
            CompiledCode __compiledCode = createCompiledCode(__method, __compilationRequest, __compilationResult);
            __installedCode = getProviders().getCodeCache().installCode(__method, __compiledCode, __predefinedInstalledCode, GraphSpeculationLog.unwrap(__speculationLog), __isDefault);
        }
        catch (Throwable __t)
        {
            failCodeInstallationTasks(__tasks, __t);
            throw __t;
        }

        postCodeInstallationTasks(__tasks, __installedCode);

        return __installedCode;
    }

    private static void failCodeInstallationTasks(CodeInstallationTask[] __tasks, Throwable __t)
    {
        for (CodeInstallationTask __task : __tasks)
        {
            __task.installFailed(__t);
        }
    }

    private static void preCodeInstallationTasks(CodeInstallationTask[] __tasks, CompilationResult __compilationResult, InstalledCode __predefinedInstalledCode)
    {
        for (CodeInstallationTask __task : __tasks)
        {
            __task.preProcess(__compilationResult, __predefinedInstalledCode);
        }
    }

    private static void postCodeInstallationTasks(CodeInstallationTask[] __tasks, InstalledCode __installedCode)
    {
        try
        {
            for (CodeInstallationTask __task : __tasks)
            {
                __task.postProcess(__installedCode);
            }
        }
        catch (Throwable __t)
        {
            __installedCode.invalidate();
            throw __t;
        }
    }

    ///
    // Installs code based on a given compilation result.
    //
    // @param method the method compiled to produce {@code compiledCode} or {@code null} if the
    //            input to {@code compResult} was not a {@link ResolvedJavaMethod}
    // @param compilationRequest the request or {@code null}
    // @param compilationResult the code to be compiled
    // @return a reference to the compiled and ready-to-run installed code
    // @throws BailoutException if the code installation failed
    ///
    public InstalledCode addInstalledCode(ResolvedJavaMethod __method, CompilationRequest __compilationRequest, CompilationResult __compilationResult)
    {
        return createInstalledCode(__method, __compilationRequest, __compilationResult, null, null, false);
    }

    ///
    // Installs code based on a given compilation result and sets it as the default code to be used
    // when {@code method} is invoked.
    //
    // @param method the method compiled to produce {@code compiledCode} or {@code null} if the
    //            input to {@code compResult} was not a {@link ResolvedJavaMethod}
    // @param compilationResult the code to be compiled
    // @return a reference to the compiled and ready-to-run installed code
    // @throws BailoutException if the code installation failed
    ///
    public InstalledCode createDefaultInstalledCode(ResolvedJavaMethod __method, CompilationResult __compilationResult)
    {
        return createInstalledCode(__method, __compilationResult, null, null, true);
    }

    ///
    // Emits the code for a given graph.
    //
    // @param installedCodeOwner the method the compiled code will be associated with once
    //            installed. This argument can be null.
    ///
    public abstract void emitCode(CompilationResultBuilder __crb, LIR __lir, ResolvedJavaMethod __installedCodeOwner);

    ///
    // Translates a set of registers from the callee's perspective to the caller's perspective. This
    // is needed for architectures where input/output registers are renamed during a call (e.g.
    // register windows on SPARC). Registers which are not visible by the caller are removed.
    ///
    public abstract EconomicSet<Register> translateToCallerRegisters(EconomicSet<Register> __calleeRegisters);

    ///
    // Encapsulates custom tasks done before and after code installation.
    ///
    // @class Backend.CodeInstallationTask
    public abstract static class CodeInstallationTask
    {
        ///
        // Task to run before code installation.
        //
        // @param compilationResult the code about to be installed
        // @param predefinedInstalledCode a pre-allocated {@link InstalledCode} object that will be
        //            used as a reference to the installed code. May be {@code null}.
        ///
        public void preProcess(CompilationResult __compilationResult, InstalledCode __predefinedInstalledCode)
        {
        }

        ///
        // Task to run after the code is installed.
        //
        // @param installedCode a reference to the installed code
        ///
        public void postProcess(InstalledCode __installedCode)
        {
        }

        ///
        // Invoked after {@link #preProcess} when code installation fails.
        //
        // @param cause the cause of the installation failure
        ///
        public void installFailed(Throwable __cause)
        {
        }
    }

    ///
    // Creates code installation tasks.
    ///
    // @class Backend.CodeInstallationTaskFactory
    public abstract static class CodeInstallationTaskFactory
    {
        public abstract CodeInstallationTask create();
    }
}
