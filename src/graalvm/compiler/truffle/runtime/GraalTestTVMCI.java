package graalvm.compiler.truffle.runtime;

import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.truffle.common.TruffleCompiler;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.common.TruffleDebugJavaMethod;

import com.oracle.truffle.api.impl.TVMCI;
import com.oracle.truffle.api.nodes.RootNode;

final class GraalTestTVMCI extends TVMCI.Test<OptimizedCallTarget> {

    private final GraalTruffleRuntime truffleRuntime;

    GraalTestTVMCI(GraalTruffleRuntime truffleRuntime) {
        this.truffleRuntime = truffleRuntime;
    }

    @Override
    public OptimizedCallTarget createTestCallTarget(RootNode testNode) {
        return (OptimizedCallTarget) truffleRuntime.createCallTarget(testNode);
    }

    @SuppressWarnings("try")
    @Override
    public void finishWarmup(OptimizedCallTarget callTarget, String testName) {
        OptionValues options = TruffleCompilerOptions.getOptions();
        DebugContext debug = DebugContext.create(options, DebugHandlersFactory.LOADER);
        TruffleCompiler compiler = truffleRuntime.getTruffleCompiler();
        TruffleInlining inliningDecision = new TruffleInlining(callTarget, new DefaultInliningPolicy());
        try (DebugContext.Scope s = debug.scope("TruffleCompilation", new TruffleDebugJavaMethod(callTarget))) {
            compiler.doCompile(debug, null, options, callTarget, inliningDecision, null, null);
        } catch (Throwable e) {
            throw debug.handle(e);
        }
    }
}
