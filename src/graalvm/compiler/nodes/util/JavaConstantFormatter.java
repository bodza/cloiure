package graalvm.compiler.nodes.util;

import jdk.vm.ci.meta.JavaConstant;

public interface JavaConstantFormatter
{
    String format(JavaConstant constant);
}
