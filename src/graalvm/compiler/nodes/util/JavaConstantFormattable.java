package graalvm.compiler.nodes.util;

/**
 * Performs special formatting of values involving {@link jdk.vm.ci.meta.JavaConstant JavaConstants}
 * when they are being dumped.
 */
public interface JavaConstantFormattable {
    String format(JavaConstantFormatter formatter);
}
