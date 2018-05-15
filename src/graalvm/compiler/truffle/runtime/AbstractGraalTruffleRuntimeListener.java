package graalvm.compiler.truffle.runtime;

public abstract class AbstractGraalTruffleRuntimeListener implements GraalTruffleRuntimeListener {

    protected final GraalTruffleRuntime runtime;

    protected AbstractGraalTruffleRuntimeListener(GraalTruffleRuntime runtime) {
        this.runtime = runtime;
    }
}
