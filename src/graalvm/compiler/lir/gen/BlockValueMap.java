package graalvm.compiler.lir.gen;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;

import jdk.vm.ci.meta.Value;

public interface BlockValueMap
{
    void accessOperand(Value operand, AbstractBlockBase<?> block);

    void defineOperand(Value operand, AbstractBlockBase<?> block);
}
