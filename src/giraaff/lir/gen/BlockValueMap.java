package giraaff.lir.gen;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;

// @iface BlockValueMap
public interface BlockValueMap
{
    void accessOperand(Value __operand, AbstractBlockBase<?> __block);

    void defineOperand(Value __operand, AbstractBlockBase<?> __block);
}
