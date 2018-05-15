package graalvm.compiler.debug;

import java.io.PrintStream;

/**
 * Provides a {@link PrintStream} that writes to the underlying log stream of the VM.
 */
public interface TTYStreamProvider {
    PrintStream getStream();
}
