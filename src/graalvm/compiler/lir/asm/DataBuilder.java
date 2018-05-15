package graalvm.compiler.lir.asm;

import graalvm.compiler.code.DataSection.Data;

import jdk.vm.ci.meta.Constant;

public abstract class DataBuilder {

    /**
     * When the method returns true, then Graal must produce detailed information that allows code
     * patching without decoding instructions, i.e., Graal must produce annotations for the machine
     * code that describe the exact locations of operands within instructions.
     */
    public abstract boolean needDetailedPatchingInformation();

    public abstract Data createDataItem(Constant c);
}
