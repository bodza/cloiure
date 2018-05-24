package giraaff.lir.gen;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;

public interface BlockValueMap
{
    void accessOperand(Value operand, AbstractBlockBase<?> block);

    void defineOperand(Value operand, AbstractBlockBase<?> block);
}