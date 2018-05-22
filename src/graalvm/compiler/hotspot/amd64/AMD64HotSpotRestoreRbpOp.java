package graalvm.compiler.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.AllocatableValue;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.Variable;

public interface AMD64HotSpotRestoreRbpOp
{
    /**
     * The type of location (i.e., stack or register) in which RBP is saved is not known until
     * initial LIR generation is finished. Until then, we use a placeholder variable so that LIR
     * verification is successful.
     */
    Variable PLACEHOLDER = new Variable(LIRKind.value(AMD64Kind.QWORD), Integer.MAX_VALUE);

    void setSavedRbp(AllocatableValue value);
}
