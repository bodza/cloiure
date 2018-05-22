package giraaff.lir.asm;

import jdk.vm.ci.meta.Constant;

import giraaff.code.DataSection.Data;

public abstract class DataBuilder
{
    /**
     * When the method returns true, then Graal must produce detailed information that allows code
     * patching without decoding instructions, i.e., Graal must produce annotations for the machine
     * code that describe the exact locations of operands within instructions.
     */
    public abstract boolean needDetailedPatchingInformation();

    public abstract Data createDataItem(Constant c);
}
