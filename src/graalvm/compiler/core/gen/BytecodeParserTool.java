package graalvm.compiler.core.gen;

import jdk.vm.ci.meta.Value;

/**
 * visible interface of bytecode parsers.
 */
public interface BytecodeParserTool {

    void storeLocal(int i, Value x);

}
