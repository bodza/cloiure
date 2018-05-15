package graalvm.compiler.graph;

import jdk.vm.ci.meta.JavaConstant;

/**
 * Provider of {@link SourceLanguagePosition} for a constant if it represents an AST node.
 */
public interface SourceLanguagePositionProvider {
    SourceLanguagePosition getPosition(JavaConstant node);
}
