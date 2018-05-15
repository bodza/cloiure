package graalvm.compiler.truffle.common.hotspot;

import graalvm.compiler.truffle.common.CompilableTruffleAST;
import graalvm.compiler.truffle.common.OptimizedAssumptionDependency;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.runtime.JVMCI;

public class HotSpotTruffleInstalledCode extends InstalledCode implements OptimizedAssumptionDependency {
    private final CompilableTruffleAST compilable;

    public HotSpotTruffleInstalledCode(CompilableTruffleAST compilable) {
        super(compilable == null ? null : compilable.getName());
        this.compilable = compilable;
    }

    @Override
    public CompilableTruffleAST getCompilable() {
        return compilable;
    }

    @Override
    public void invalidate() {
        if (isValid()) {
            JVMCI.getRuntime().getHostJVMCIBackend().getCodeCache().invalidateInstalledCode(this);
        }
    }

    @Override
    public String toString() {
        return compilable == null ? super.toString() : compilable.toString();
    }
}
