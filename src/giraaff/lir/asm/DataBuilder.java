package giraaff.lir.asm;

import jdk.vm.ci.meta.Constant;

import giraaff.code.DataSection.Data;

public abstract class DataBuilder
{
    public abstract Data createDataItem(Constant c);
}
