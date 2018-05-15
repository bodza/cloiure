package graalvm.compiler.truffle.runtime;

public interface PrioritizedServiceProvider {

    default int getPriority() {
        return 0;
    }

}
