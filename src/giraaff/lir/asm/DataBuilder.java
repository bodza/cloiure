package giraaff.lir.asm;

import jdk.vm.ci.meta.Constant;

import giraaff.code.DataSection;

// @class DataBuilder
public abstract class DataBuilder
{
    public abstract DataSection.Data createDataItem(Constant __c);
}
