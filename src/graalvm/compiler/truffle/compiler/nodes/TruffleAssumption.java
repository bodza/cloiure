package graalvm.compiler.truffle.compiler.nodes;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaConstant;

public final class TruffleAssumption extends Assumptions.Assumption {

    private final JavaConstant assumption;

    public TruffleAssumption(JavaConstant assumption) {
        this.assumption = assumption;
    }

    public JavaConstant getAssumption() {
        return assumption;
    }

    @Override
    public int hashCode() {
        return 31 + assumption.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TruffleAssumption) {
            TruffleAssumption other = (TruffleAssumption) obj;
            return this.assumption.equals(other.assumption);
        }
        return false;
    }

    @Override
    public String toString() {
        return assumption.toString();
    }
}
